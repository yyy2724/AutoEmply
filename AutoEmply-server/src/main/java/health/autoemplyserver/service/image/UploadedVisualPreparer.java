package health.autoemplyserver.service.image;

import health.autoemplyserver.support.exception.BadRequestException;
import java.io.IOException;
import java.util.Base64;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class UploadedVisualPreparer {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    public PreparedVisual prepare(String formName, MultipartFile image) {
        String normalizedFormName = normalizeFormName(formName);
        validateImagePresence(image);
        validateImageSize(image);

        String mediaType = resolveMediaType(image.getOriginalFilename(), image.getContentType());
        if (mediaType == null) {
            throw new BadRequestException("Unsupported image or document type.");
        }

        try {
            return new PreparedVisual(
                normalizedFormName,
                mediaType,
                Base64.getEncoder().encodeToString(image.getBytes())
            );
        } catch (IOException exception) {
            throw new BadRequestException("Failed to read uploaded image.", exception);
        }
    }

    private String normalizeFormName(String formName) {
        if (formName == null || formName.isBlank()) {
            throw new BadRequestException("formName is required.");
        }
        return formName.trim();
    }

    private void validateImagePresence(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("image is required.");
        }
    }

    private void validateImageSize(MultipartFile image) {
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("image exceeds 5MB limit.");
        }
    }

    private String resolveMediaType(String fileName, String contentType) {
        String byExtension = mapExtension(fileName == null ? "" : fileName);
        String byContentType = normalizeContentType(contentType);
        if (byExtension != null && byContentType != null && !byExtension.equals(byContentType)) {
            return null;
        }
        return byExtension != null ? byExtension : byContentType;
    }

    private String mapExtension(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return null;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return switch (contentType.trim().toLowerCase()) {
            case "image/jpg", "image/jpeg" -> "image/jpeg";
            case "image/png" -> "image/png";
            case "image/gif" -> "image/gif";
            case "image/webp" -> "image/webp";
            case "application/pdf" -> "application/pdf";
            default -> null;
        };
    }
}
