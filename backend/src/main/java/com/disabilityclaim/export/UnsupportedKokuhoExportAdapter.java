package com.disabilityclaim.export;

import com.disabilityclaim.domain.entity.BillingBatch;
import org.springframework.stereotype.Component;

@Component
public class UnsupportedKokuhoExportAdapter implements KokuhoExportAdapter {

    @Override
    public String name() {
        return "UnsupportedKokuhoExportAdapter";
    }

    @Override
    public ExportResult export(BillingBatch batch) {
        throw new IllegalStateException("公式I/F仕様未提供");
    }
}
