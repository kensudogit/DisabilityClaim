package com.disabilityclaim.web;

import com.disabilityclaim.domain.entity.UserAccount;
import com.disabilityclaim.repository.UserAccountRepository;
import com.disabilityclaim.service.importing.ColumnMapping;
import com.disabilityclaim.service.importing.ExcelImportService;
import com.disabilityclaim.service.importing.ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService excelImportService;
    private final UserAccountRepository userAccountRepository;

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','BILLING_MANAGER','BILLING_OPERATOR')")
    public ImportResult importExcel(
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID officeId,
            @RequestParam(defaultValue = "false") boolean allowPartial,
            Authentication authentication) {
        UserAccount actor = userAccountRepository.findByUsername(authentication.getName()).orElseThrow();
        return excelImportService.importBeneficiaries(
                file, officeId, allowPartial, ColumnMapping.defaultBeneficiaryMapping(),
                actor.getId(), actor.getUsername());
    }
}
