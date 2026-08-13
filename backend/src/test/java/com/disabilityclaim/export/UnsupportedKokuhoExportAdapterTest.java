package com.disabilityclaim.export;

import com.disabilityclaim.domain.entity.BillingBatch;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnsupportedKokuhoExportAdapterTest {

    private final UnsupportedKokuhoExportAdapter adapter = new UnsupportedKokuhoExportAdapter();

    @Test
    void nameIsStable() {
        assertThat(adapter.name()).isEqualTo("UnsupportedKokuhoExportAdapter");
    }

    @Test
    void exportThrowsOfficialSpecMissing() {
        BillingBatch batch = BillingBatch.builder().id(UUID.randomUUID()).billingMonth("2026-08").build();
        assertThatThrownBy(() -> adapter.export(batch))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("公式I/F仕様未提供");
    }
}
