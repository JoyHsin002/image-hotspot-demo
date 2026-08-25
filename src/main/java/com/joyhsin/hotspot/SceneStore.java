package com.joyhsin.hotspot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.joyhsin.hotspot.SceneData.Hotspot;
import com.joyhsin.hotspot.SceneData.HotspotDraft;
import com.joyhsin.hotspot.SceneData.Scene;
import com.joyhsin.hotspot.SceneData.StoredImage;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class SceneStore {

    private final Path storeFile;
    private final ObjectMapper objectMapper;
    private final Map<Long, Scene> scenes = new LinkedHashMap<>();

    private long nextSceneId = 1;
    private long nextHotspotId = 1;

    public SceneStore(AppProperties properties, ObjectMapper objectMapper) {
        this.storeFile = properties.sceneStoreFile().toAbsolutePath().normalize();
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    synchronized void initialize() throws IOException {
        Files.createDirectories(storeFile.getParent());
        if (!Files.exists(storeFile)) {
            persist();
            return;
        }
        if (Files.size(storeFile) == 0) {
            return;
        }

        List<Scene> loaded = objectMapper.readValue(storeFile.toFile(), new TypeReference<>() {
        });
        for (Scene scene : loaded) {
            scenes.put(scene.id(), scene);
            nextSceneId = Math.max(nextSceneId, scene.id() + 1);
            for (Hotspot hotspot : scene.draftHotspots()) {
                nextHotspotId = Math.max(nextHotspotId, hotspot.id() + 1);
            }
            for (Hotspot hotspot : scene.publishedHotspots()) {
                nextHotspotId = Math.max(nextHotspotId, hotspot.id() + 1);
            }
        }
    }

    public synchronized List<Scene> findAll() {
        return scenes.values().stream()
                .sorted(Comparator.comparing(Scene::updatedAt).reversed())
                .toList();
    }

    public synchronized Optional<Scene> findById(long sceneId) {
        return Optional.ofNullable(scenes.get(sceneId));
    }

    public synchronized Scene create(String name, StoredImage image) {
        Instant now = Instant.now();
        Scene scene = new Scene(
                nextSceneId++,
                name,
                image.imageUrl(),
                image.storedFileName(),
                image.originalFileName(),
                List.of(),
                List.of(),
                0,
                0,
                now,
                now,
                null
        );
        scenes.put(scene.id(), scene);
        persistUnchecked();
        return scene;
    }

    public synchronized Scene replaceDraft(long sceneId, List<HotspotDraft> drafts) {
        Scene current = requireScene(sceneId);
        Set<Long> reusableIds = new HashSet<>();
        for (Hotspot hotspot : current.draftHotspots()) {
            reusableIds.add(hotspot.id());
        }

        Set<Long> usedIds = new HashSet<>();
        List<Hotspot> hotspots = new ArrayList<>(drafts.size());
        for (HotspotDraft draft : drafts) {
            long hotspotId;
            if (draft.id() != null
                    && reusableIds.contains(draft.id())
                    && usedIds.add(draft.id())) {
                hotspotId = draft.id();
            } else {
                hotspotId = nextHotspotId++;
                usedIds.add(hotspotId);
            }
            hotspots.add(new Hotspot(
                    hotspotId,
                    draft.xRatio(),
                    draft.yRatio(),
                    draft.label(),
                    draft.markerColor(),
                    draft.product()
            ));
        }

        Scene updated = new Scene(
                current.id(),
                current.name(),
                current.imageUrl(),
                current.storedFileName(),
                current.originalFileName(),
                hotspots,
                current.publishedHotspots(),
                current.draftVersion() + 1,
                current.publishedVersion(),
                current.createdAt(),
                Instant.now(),
                current.publishedAt()
        );
        scenes.put(sceneId, updated);
        persistUnchecked();
        return updated;
    }

    public synchronized Scene publish(long sceneId) {
        Scene current = requireScene(sceneId);
        Instant now = Instant.now();
        Scene published = new Scene(
                current.id(),
                current.name(),
                current.imageUrl(),
                current.storedFileName(),
                current.originalFileName(),
                current.draftHotspots(),
                current.draftHotspots(),
                current.draftVersion(),
                current.draftVersion(),
                current.createdAt(),
                now,
                now
        );
        scenes.put(sceneId, published);
        persistUnchecked();
        return published;
    }

    public synchronized Optional<Scene> delete(long sceneId) {
        Scene removed = scenes.remove(sceneId);
        if (removed != null) {
            persistUnchecked();
        }
        return Optional.ofNullable(removed);
    }

    private Scene requireScene(long sceneId) {
        Scene scene = scenes.get(sceneId);
        if (scene == null) {
            throw new SceneNotFoundException(sceneId);
        }
        return scene;
    }

    private void persistUnchecked() {
        try {
            persist();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not persist scene data", ex);
        }
    }

    private void persist() throws IOException {
        Path tempFile = storeFile.resolveSibling(storeFile.getFileName() + ".tmp");
        String json = objectMapper.writeValueAsString(scenes.values());
        Files.writeString(
                tempFile,
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        try {
            Files.move(
                    tempFile,
                    storeFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempFile, storeFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}

final class SceneNotFoundException extends RuntimeException {
    SceneNotFoundException(long sceneId) {
        super("Scene " + sceneId + " was not found");
    }
}
