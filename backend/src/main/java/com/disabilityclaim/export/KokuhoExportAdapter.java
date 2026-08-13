package com.disabilityclaim.export;

import com.disabilityclaim.domain.entity.BillingBatch;

/**
 * Adapter for 国保連 / 電子請求受付システム file generation.
 * Real layouts must not be invented — wait for official I/F specifications.
 */
public interface KokuhoExportAdapter {

    String name();

    /**
     * Generates export artifacts for a confirmed billing batch.
     *
     * @throws IllegalStateException when official I/F specification is not provided
     */
    ExportResult export(BillingBatch batch);

    record ExportResult(String adapterName, String message) {
    }
}
