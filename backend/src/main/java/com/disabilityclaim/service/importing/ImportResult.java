package com.disabilityclaim.service.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ImportResult(
        UUID jobId,
        String status,
        int totalRows,
        int validRows,
        int errorRows,
        boolean committed,
        List<RowError> errors
) {
    public record RowError(int row, String column, String reason) {
    }

    public static ImportResult empty(UUID jobId) {
        return new ImportResult(jobId, "EMPTY", 0, 0, 0, false, List.of());
    }

    public ImportResult withErrors(List<RowError> more) {
        List<RowError> all = new ArrayList<>(errors);
        all.addAll(more);
        return new ImportResult(jobId, status, totalRows, validRows, errorRows, committed, all);
    }
}
