package health.autoemplyserver.support.exception;

import java.util.List;

public class BadRequestException extends RuntimeException {

    private final List<String> details;

    public BadRequestException(String message) {
        super(message);
        this.details = List.of();
    }

    public BadRequestException(String message, List<String> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.details = List.of();
    }

    public List<String> getDetails() {
        return details;
    }
}
