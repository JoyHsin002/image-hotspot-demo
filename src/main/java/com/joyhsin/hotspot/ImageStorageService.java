package com.joyhsin.hotspot;

import com.joyhsin.hotspot.SceneData.StoredImage;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ImageStorageService {

    private final AppProperties properties;
    private Path uploadDirectory;

    public ImageStorageService(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void initialize() throws IOException {
        uploadDirectory = properties.uploadDirectory().toAbsolutePath().normalize();
        Files.createDirectories(uploadDirectory);
    }

    public StoredImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select an image to upload");
        }
        if (file.getSize() > properties.maxImageBytes()) {
            throw new IllegalArgumentException("Image is larger than the configured upload limit");
        }

        try {
            return store(file.getBytes(), Objects.requireNonNullElse(file.getOriginalFilename(), "uploaded-image"));
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read the uploaded image", ex);
        }
    }

    public StoredImage store(byte[] bytes, String originalFileName) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Image data is empty");
        }
        if (bytes.length > properties.maxImageBytes()) {
            throw new IllegalArgumentException("Image is larger than the configured upload limit");
        }

        ImageFormat format = detectFormat(bytes);
        String storedFileName = UUID.randomUUID() + "." + format.extension();
        Path target = resolveSafe(storedFileName);
        try {
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store the uploaded image", ex);
        }

        return new StoredImage(
                storedFileName,
                cleanOriginalFileName(originalFileName, format.extension()),
                "/uploads/" + storedFileName,
                format.contentType()
        );
    }

    public StoredResource load(String fileName) {
        try {
            Path target = resolveSafe(fileName);
            if (!Files.isRegularFile(target)) {
                throw new IllegalArgumentException("Image was not found");
            }
            String contentType = Files.probeContentType(target);
            if (contentType == null) {
                contentType = contentTypeFromName(fileName);
            }
            return new StoredResource(new PathResource(target), contentType);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read the image", ex);
        }
    }

    public void deleteQuietly(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(fileName));
        } catch (IOException ignored) {
            // Metadata deletion should still succeed in this local demo.
        }
    }

    private Path resolveSafe(String fileName) {
        if (fileName == null || !fileName.matches("[a-fA-F0-9-]+\\.(png|jpg|gif|webp)")) {
            throw new IllegalArgumentException("Invalid image file name");
        }
        Path resolved = uploadDirectory.resolve(fileName).normalize();
        if (!resolved.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid image path");
        }
        return resolved;
    }

    private static String cleanOriginalFileName(String value, String extension) {
        String fallback = "uploaded-image." + extension;
        String cleaned = StringUtils.cleanPath(Objects.requireNonNullElse(value, fallback));
        int slash = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        String fileName = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        return fileName.isBlank() ? fallback : fileName;
    }

    private static ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 8
                && unsigned(bytes[0]) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {
            return new ImageFormat("png", "image/png");
        }
        if (bytes.length >= 3
                && unsigned(bytes[0]) == 0xFF
                && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF) {
            return new ImageFormat("jpg", "image/jpeg");
        }
        if (bytes.length >= 6) {
            String header = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
            if (header.equals("GIF87a") || header.equals("GIF89a")) {
                return new ImageFormat("gif", "image/gif");
            }
        }
        if (bytes.length >= 12) {
            String riff = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
            String webp = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
            if (riff.equals("RIFF") && webp.equals("WEBP")) {
                return new ImageFormat("webp", "image/webp");
            }
        }
        throw new IllegalArgumentException("Only PNG, JPEG, GIF, and WebP images are supported");
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }

    private static String contentTypeFromName(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/webp";
    }

    private record ImageFormat(String extension, String contentType) {
    }

    public record StoredResource(Resource resource, String contentType) {
    }
}
