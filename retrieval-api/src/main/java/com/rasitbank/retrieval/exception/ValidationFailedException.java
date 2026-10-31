package com.rasitbank.retrieval.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Bean Validation'la denetlenemeyen istek parçalarının (ör. multipart yükleme) ihlallerini bildirir; 400 döner.
 */
public class ValidationFailedException extends BaseException {

    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";

    private final List<Violation> violations;

    public ValidationFailedException(List<Violation> violations) {
        super(VALIDATION_FAILED, "Request validation failed", HttpStatus.BAD_REQUEST);
        this.violations = List.copyOf(violations);
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
