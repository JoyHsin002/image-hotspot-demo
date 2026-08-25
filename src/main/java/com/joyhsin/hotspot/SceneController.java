package com.joyhsin.hotspot;

import com.joyhsin.hotspot.SceneData.Product;
import com.joyhsin.hotspot.SceneData.SaveDraftRequest;
import com.joyhsin.hotspot.SceneData.SceneSummary;
import com.joyhsin.hotspot.SceneData.SceneView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@Validated
public class SceneController {

    private final SceneService sceneService;
    private final ImageStorageService imageStorageService;

    public SceneController(SceneService sceneService, ImageStorageService imageStorageService) {
        this.sceneService = sceneService;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping("/api/scenes")
    public List<SceneSummary> listScenes() {
        return sceneService.listScenes();
    }

    @GetMapping("/api/scenes/{sceneId}")
    public SceneView getScene(@PathVariable long sceneId) {
        return sceneService.getScene(sceneId);
    }

    @PostMapping(value = "/api/scenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SceneView> createScene(
            @RequestParam("name")
            @NotBlank(message = "Scene name is required")
            @Size(max = 120, message = "Scene name must be 120 characters or fewer")
            String name,
            @RequestPart("image") MultipartFile image
    ) {
        SceneView created = sceneService.createScene(name, image);
        return ResponseEntity
                .created(URI.create("/api/scenes/" + created.id()))
                .body(created);
    }

    @PostMapping("/api/scenes/sample")
    public ResponseEntity<SceneView> createSampleScene() {
        SceneView created = sceneService.createSampleScene();
        return ResponseEntity
                .created(URI.create("/api/scenes/" + created.id()))
                .body(created);
    }

    @PutMapping("/api/scenes/{sceneId}/draft")
    public SceneView saveDraft(
            @PathVariable long sceneId,
            @RequestBody @Valid SaveDraftRequest request
    ) {
        return sceneService.saveDraft(sceneId, request.hotspots());
    }

    @PostMapping("/api/scenes/{sceneId}/publish")
    public SceneView publish(@PathVariable long sceneId) {
        return sceneService.publish(sceneId);
    }

    @DeleteMapping("/api/scenes/{sceneId}")
    public ResponseEntity<Void> deleteScene(@PathVariable long sceneId) {
        sceneService.deleteScene(sceneId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/api/products")
    public List<Product> searchProducts(@RequestParam(name = "q", required = false) String query) {
        return sceneService.searchProducts(query);
    }

    @GetMapping("/uploads/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> uploadedImage(@PathVariable String fileName) {
        ImageStorageService.StoredResource stored = imageStorageService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.contentType()))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .body(stored.resource());
    }
}
