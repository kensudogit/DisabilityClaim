package com.disabilityclaim.service.importing;

import com.disabilityclaim.domain.entity.*;
import com.disabilityclaim.domain.enums.BeneficiaryCategory;
import com.disabilityclaim.domain.enums.BeneficiaryStatus;
import com.disabilityclaim.repository.*;
import com.disabilityclaim.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    private final ImportJobRepository importJobRepository;
    private final ImportStagingRowRepository stagingRowRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final MunicipalityRepository municipalityRepository;
    private final OfficeProfileRepository officeProfileRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ImportResult importBeneficiaries(MultipartFile file, UUID officeId, boolean allowPartial,
                                            ColumnMapping mapping, UUID actorId, String actorName) {
        OfficeProfile office = officeProfileRepository.findById(officeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "office not found"));

        ImportJob job = ImportJob.builder()
                .jobType("EXCEL_BENEFICIARY")
                .fileName(file.getOriginalFilename())
                .status("PROCESSING")
                .allowPartial(allowPartial)
                .committed(false)
                .build();
        job = importJobRepository.save(job);

        List<ImportResult.RowError> errors = new ArrayList<>();
        int total = 0;
        int valid = 0;
        Set<String> seenRecipientNumbers = new HashSet<>();

        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerIndex = buildHeaderIndex(sheet.getRow(0));

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row)) {
                    continue;
                }
                total++;
                int rowNum = r + 1;
                Map<String, String> raw = new LinkedHashMap<>();
                Map<String, String> mapped = new LinkedHashMap<>();
                String errorColumn = null;
                String errorReason = null;
                boolean rowValid = true;

                for (Map.Entry<String, String> entry : mapping.asMap().entrySet()) {
                    String field = entry.getKey();
                    String header = entry.getValue();
                    Integer colIdx = headerIndex.get(header);
                    String cellValue = colIdx == null ? "" : getCellString(row.getCell(colIdx));
                    raw.put(header, cellValue);
                    mapped.put(field, cellValue);
                }

                try {
                    validateRequired(mapped, "familyName", "姓");
                    validateRequired(mapped, "givenName", "名");
                    validateRequired(mapped, "category", "区分");
                    parseCategory(mapped.get("category"));
                    if (mapped.get("birthDate") != null && !mapped.get("birthDate").isBlank()) {
                        parseDate(mapped.get("birthDate"), "生年月日");
                    }
                    if (mapped.get("serviceStartDate") != null && !mapped.get("serviceStartDate").isBlank()) {
                        parseDate(mapped.get("serviceStartDate"), "利用開始日");
                    }
                    String recipientNumber = mapped.getOrDefault("recipientNumber", "");
                    if (!recipientNumber.isBlank()) {
                        if (!seenRecipientNumbers.add(recipientNumber)) {
                            throw new ImportValidationException("受給者番号", "duplicate recipient number in file");
                        }
                    }
                    String muniCode = mapped.getOrDefault("municipalityCode", "");
                    if (!muniCode.isBlank() && municipalityRepository.findByCode(muniCode).isEmpty()) {
                        throw new ImportValidationException("市町村コード", "unknown municipality code: " + muniCode);
                    }
                } catch (ImportValidationException ex) {
                    rowValid = false;
                    errorColumn = ex.column;
                    errorReason = ex.getMessage();
                    errors.add(new ImportResult.RowError(rowNum, errorColumn, errorReason));
                }

                stagingRowRepository.save(ImportStagingRow.builder()
                        .importJob(job)
                        .rowNumber(rowNum)
                        .rawJson(toJson(raw))
                        .mappedJson(toJson(mapped))
                        .valid(rowValid)
                        .errorColumn(errorColumn)
                        .errorReason(errorReason)
                        .build());
                if (rowValid) {
                    valid++;
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setCompletedAt(Instant.now());
            importJobRepository.save(job);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel parse failed: " + e.getMessage());
        }

        int errorRows = total - valid;
        job.setTotalRows(total);
        job.setValidRows(valid);
        job.setErrorRows(errorRows);

        boolean canCommit = errorRows == 0 || (allowPartial && valid > 0);
        if (canCommit && (errorRows == 0 || allowPartial)) {
            commitValidRows(job, office);
            job.setCommitted(true);
            job.setStatus(errorRows == 0 ? "COMMITTED" : "PARTIAL_COMMITTED");
        } else {
            job.setCommitted(false);
            job.setStatus(errorRows > 0 ? "VALIDATION_FAILED" : "EMPTY");
        }
        job.setCompletedAt(Instant.now());
        importJobRepository.save(job);
        auditService.record(actorId, actorName, "IMPORT_EXCEL", "ImportJob", job.getId().toString(),
                null, "total=" + total + ",valid=" + valid, null);

        return new ImportResult(job.getId(), job.getStatus(), total, valid, errorRows, job.isCommitted(), errors);
    }

    private void commitValidRows(ImportJob job, OfficeProfile office) {
        List<ImportStagingRow> rows = stagingRowRepository.findByImportJobIdAndValidTrue(job.getId());
        for (ImportStagingRow row : rows) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> mapped = objectMapper.readValue(row.getMappedJson(), Map.class);
                BeneficiaryCategory category = parseCategory(mapped.get("category"));
                Municipality municipality = null;
                String muniCode = mapped.get("municipalityCode");
                if (muniCode != null && !muniCode.isBlank()) {
                    municipality = municipalityRepository.findByCode(muniCode).orElse(null);
                }
                Beneficiary beneficiary = Beneficiary.builder()
                        .office(office)
                        .category(category)
                        .recipientNumber(blankToNull(mapped.get("recipientNumber")))
                        .familyName(mapped.get("familyName"))
                        .givenName(mapped.get("givenName"))
                        .familyNameKana(blankToNull(mapped.get("familyNameKana")))
                        .givenNameKana(blankToNull(mapped.get("givenNameKana")))
                        .birthDate(optionalDate(mapped.get("birthDate")))
                        .serviceStartDate(optionalDate(mapped.get("serviceStartDate")))
                        .status(BeneficiaryStatus.ACTIVE)
                        .municipality(municipality)
                        .build();
                beneficiaryRepository.save(beneficiary);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "commit failed at row " + row.getRowNumber() + ": " + e.getMessage());
            }
        }
    }

    private Map<String, Integer> buildHeaderIndex(Row headerRow) {
        if (headerRow == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel header row missing");
        }
        Map<String, Integer> index = new HashMap<>();
        for (Cell cell : headerRow) {
            index.put(getCellString(cell).trim(), cell.getColumnIndex());
        }
        return index;
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (!getCellString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double n = cell.getNumericCellValue();
                if (n == Math.rint(n)) {
                    yield String.valueOf((long) n);
                }
                yield String.valueOf(n);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private void validateRequired(Map<String, String> mapped, String field, String column) {
        String v = mapped.get(field);
        if (v == null || v.isBlank()) {
            throw new ImportValidationException(column, "required");
        }
    }

    private BeneficiaryCategory parseCategory(String raw) {
        if (raw == null) {
            throw new ImportValidationException("区分", "required");
        }
        String v = raw.trim().toUpperCase(Locale.ROOT);
        if (v.contains("児") || v.equals("CHILD")) {
            return BeneficiaryCategory.CHILD;
        }
        if (v.contains("障") || v.equals("ADULT") || v.contains("成人")) {
            return BeneficiaryCategory.ADULT;
        }
        try {
            return BeneficiaryCategory.valueOf(v);
        } catch (Exception e) {
            throw new ImportValidationException("区分", "invalid category: " + raw);
        }
    }

    private LocalDate parseDate(String raw, String column) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw.trim(), fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new ImportValidationException(column, "invalid date: " + raw);
    }

    private LocalDate optionalDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseDate(raw, "date");
    }

    private String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static class ImportValidationException extends RuntimeException {
        private final String column;

        ImportValidationException(String column, String message) {
            super(message);
            this.column = column;
        }
    }
}
