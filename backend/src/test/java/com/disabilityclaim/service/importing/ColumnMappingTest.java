package com.disabilityclaim.service.importing;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ColumnMappingTest {

    @Test
    void defaultBeneficiaryMappingHasJapaneseHeaders() {
        ColumnMapping mapping = ColumnMapping.defaultBeneficiaryMapping();
        assertThat(mapping.headerFor("familyName")).isEqualTo("姓");
        assertThat(mapping.headerFor("givenName")).isEqualTo("名");
        assertThat(mapping.headerFor("category")).isEqualTo("区分");
        assertThat(mapping.headerFor("recipientNumber")).isEqualTo("受給者番号");
        assertThat(mapping.headerFor("municipalityCode")).isEqualTo("市町村コード");
        assertThat(mapping.headerFor("birthDate")).isEqualTo("生年月日");
        assertThat(mapping.headerFor("serviceStartDate")).isEqualTo("利用開始日");
    }

    @Test
    void unknownFieldReturnsNull() {
        ColumnMapping mapping = ColumnMapping.defaultBeneficiaryMapping();
        assertThat(mapping.headerFor("unknown")).isNull();
    }

    @Test
    void asMapIsUnmodifiableCopy() {
        ColumnMapping mapping = new ColumnMapping(Map.of("a", "A"));
        assertThat(mapping.asMap()).containsEntry("a", "A");
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> mapping.asMap().put("b", "B"));
    }
}
