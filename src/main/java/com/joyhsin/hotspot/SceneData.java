package com.joyhsin.hotspot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SceneData {

    private SceneData() {
    }

    public record Product(
            String code,
            String name,
            String price,
            String imageUrl,
            String description,
            String detailUrl
    ) {
    }

    public record Hotspot(
            long id,
            double xRatio,
            double yRatio,
            String label,
            String markerColor,
            Product product
    ) {
    }

    public record Scene(
            long id,
            String name,
            String imageUrl,
            String storedFileName,
            String originalFileName,
            List<Hotspot> draftHotspots,
            List<Hotspot> publishedHotspots,
            int draftVersion,
            int publishedVersion,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt
    ) {
        public Scene {
            draftHotspots = draftHotspots == null ? List.of() : List.copyOf(draftHotspots);
            publishedHotspots = publishedHotspots == null ? List.of() : List.copyOf(publishedHotspots);
        }
    }

    public record StoredImage(
            String storedFileName,
            String originalFileName,
            String imageUrl,
            String contentType
    ) {
    }

    public record HotspotDraft(
            Long id,
            double xRatio,
            double yRatio,
            String label,
            String markerColor,
            Product product
    ) {
    }

    public record SceneSummary(
            long id,
            String name,
            String imageUrl,
            int draftCount,
            int publishedCount,
            int draftVersion,
            int publishedVersion,
            Instant updatedAt,
            Instant publishedAt
    ) {
    }

    public record SceneView(
            long id,
            String name,
            String imageUrl,
            String originalFileName,
            List<Hotspot> draftHotspots,
            List<Hotspot> publishedHotspots,
            int draftVersion,
            int publishedVersion,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt
    ) {
    }

    public record ProductInput(
            @Size(max = 80, message = "Product code must be 80 characters or fewer")
            String code,

            @NotBlank(message = "Product name is required")
            @Size(max = 120, message = "Product name must be 120 characters or fewer")
            String name,

            @Size(max = 80, message = "Price must be 80 characters or fewer")
            String price,

            @Size(max = 1000, message = "Product image URL is too long")
            String imageUrl,

            @Size(max = 1000, message = "Description must be 1000 characters or fewer")
            String description,

            @Size(max = 1000, message = "Detail URL is too long")
            String detailUrl
    ) {
    }

    public record HotspotInput(
            Long id,

            @NotNull(message = "xRatio is required")
            @DecimalMin(value = "0.0", message = "xRatio must be at least 0")
            @DecimalMax(value = "1.0", message = "xRatio must be at most 1")
            Double xRatio,

            @NotNull(message = "yRatio is required")
            @DecimalMin(value = "0.0", message = "yRatio must be at least 0")
            @DecimalMax(value = "1.0", message = "yRatio must be at most 1")
            Double yRatio,

            @Size(max = 80, message = "Marker label must be 80 characters or fewer")
            String label,

            @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Marker color must use #RRGGBB")
            String markerColor,

            @Valid
            @NotNull(message = "Product information is required")
            ProductInput product
    ) {
    }

    public record SaveDraftRequest(
            @NotNull(message = "hotspots is required")
            @Size(max = 100, message = "A scene can contain at most 100 hotspots")
            List<@Valid HotspotInput> hotspots
    ) {
    }
}
