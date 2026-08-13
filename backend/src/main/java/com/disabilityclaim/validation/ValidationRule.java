package com.disabilityclaim.validation;

import java.util.List;

public interface ValidationRule {
    String code();

    List<ValidationFinding> validate(ValidationContext context);
}
