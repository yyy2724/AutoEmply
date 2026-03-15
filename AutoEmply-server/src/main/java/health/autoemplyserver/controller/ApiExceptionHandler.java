package health.autoemplyserver.controller;

import health.autoemplyserver.dto.common.ApiErrorResponse;
import health.autoemplyserver.support.exception.BadRequestException;
import health.autoemplyserver.support.exception.ExternalServiceException;
import health.autoemplyserver.support.exception.NotFoundException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .toList();
        return ResponseEntity.badRequest().body(new ApiErrorResponse("Validation failed", details));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException exception) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(exception.getMessage(), exception.getDetails()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(404).body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalService(ExternalServiceException exception) {
        return ResponseEntity.status(502).body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupported(UnsupportedOperationException exception) {
        return ResponseEntity.status(501).body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return ResponseEntity.status(415).body(new ApiErrorResponse(exception.getMessage()));
    }
}
