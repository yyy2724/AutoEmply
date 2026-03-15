package health.autoemplyserver.dto.common;

import java.util.List;

public record ApiErrorResponse(String error, List<String> details) {

    public ApiErrorResponse(String error) {
        this(error, List.of());
    }
}
