package health.autoemplyserver.service.image;

public record PreparedVisual(
    String formName,
    String mediaType,
    String base64Data
) {
}
