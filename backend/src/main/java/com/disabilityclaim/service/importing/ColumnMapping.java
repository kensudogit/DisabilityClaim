package com.disabilityclaim.service.importing;

import java.util.HashMap;
import java.util.Map;

/**
 * Configurable Excel column mapping. Keys are domain fields; values are header names.
 */
public class ColumnMapping {

    private final Map<String, String> fieldToHeader;

    public ColumnMapping(Map<String, String> fieldToHeader) {
        this.fieldToHeader = Map.copyOf(fieldToHeader);
    }

    public static ColumnMapping defaultBeneficiaryMapping() {
        Map<String, String> map = new HashMap<>();
        map.put("familyName", "姓");
        map.put("givenName", "名");
        map.put("familyNameKana", "セイ");
        map.put("givenNameKana", "メイ");
        map.put("category", "区分");
        map.put("recipientNumber", "受給者番号");
        map.put("municipalityCode", "市町村コード");
        map.put("birthDate", "生年月日");
        map.put("serviceStartDate", "利用開始日");
        return new ColumnMapping(map);
    }

    public String headerFor(String field) {
        return fieldToHeader.get(field);
    }

    public Map<String, String> asMap() {
        return fieldToHeader;
    }
}
