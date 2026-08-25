package com.joyhsin.hotspot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SceneApiIntegrationTest {

    private static final Path TEST_DATA = Path.of("target", "hotspot-test-" + UUID.randomUUID());

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("hotspot.data-directory", () -> TEST_DATA.toString());
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createsSampleSavesDraftAndPublishes() throws Exception {
        String createBody = mockMvc.perform(post("/api/scenes/sample"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("智慧餐饮门店示例"))
                .andExpect(jsonPath("$.draftHotspots.length()").value(4))
                .andExpect(jsonPath("$.publishedHotspots.length()").value(4))
                .andExpect(jsonPath("$.draftVersion").value(1))
                .andExpect(jsonPath("$.publishedVersion").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = objectMapper.readTree(createBody);
        long sceneId = created.path("id").asLong();
        String imageUrl = created.path("imageUrl").asText();

        mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("image/svg+xml")));

        String draftRequest = """
                {
                  "hotspots": [
                    {
                      "xRatio": 0.25,
                      "yRatio": 0.75,
                      "label": "Checkout",
                      "markerColor": "#2563EB",
                      "product": {
                        "code": "POS-TEST",
                        "name": "Smart POS",
                        "price": "CNY 2999",
                        "imageUrl": "/sample/products/pos.svg",
                        "description": "A demo product",
                        "detailUrl": "https://example.com/products/pos"
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/scenes/{sceneId}/draft", sceneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draftHotspots.length()").value(1))
                .andExpect(jsonPath("$.publishedHotspots.length()").value(4))
                .andExpect(jsonPath("$.draftVersion").value(2))
                .andExpect(jsonPath("$.publishedVersion").value(1));

        mockMvc.perform(post("/api/scenes/{sceneId}/publish", sceneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedHotspots.length()").value(1))
                .andExpect(jsonPath("$.publishedHotspots[0].product.code").value("POS-TEST"))
                .andExpect(jsonPath("$.publishedVersion").value(2));
    }

    @Test
    void uploadsRealImageAndRejectsInvalidHotspotUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "floor-plan.png",
                MediaType.IMAGE_PNG_VALUE,
                pngBytes()
        );

        String createBody = mockMvc.perform(multipart("/api/scenes")
                        .file(image)
                        .param("name", "Integration Test Scene"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Test Scene"))
                .andExpect(jsonPath("$.draftHotspots.length()").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long sceneId = objectMapper.readTree(createBody).path("id").asLong();
        String invalidRequest = """
                {
                  "hotspots": [
                    {
                      "xRatio": 0.5,
                      "yRatio": 0.5,
                      "markerColor": "#123456",
                      "product": {
                        "name": "Unsafe URL",
                        "detailUrl": "javascript:alert(1)"
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/api/scenes/{sceneId}/draft", sceneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product detail URL must be an http(s) URL or root-relative path"));
    }

    @Test
    void returnsSearchableMockProductCatalog() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "wifi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NET-06"))
                .andExpect(jsonPath("$[0].name").value("Wi-Fi 6 企业级接入点"));
    }

    private static byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        image.setRGB(5, 5, Color.BLUE.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    @AfterAll
    static void cleanUp() throws IOException {
        if (!Files.exists(TEST_DATA)) {
            return;
        }
        try (var paths = Files.walk(TEST_DATA)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
