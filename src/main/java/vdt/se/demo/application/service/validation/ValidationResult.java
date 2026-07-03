package vdt.se.demo.application.service.validation;

import java.util.List;

public record ValidationResult(List<String> errors, List<String> warnings) {
    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
    public boolean ok() { return errors.isEmpty(); }
}
