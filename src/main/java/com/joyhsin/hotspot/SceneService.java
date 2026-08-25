package com.joyhsin.hotspot;

import com.joyhsin.hotspot.SceneData.HotspotDraft;
import com.joyhsin.hotspot.SceneData.HotspotInput;
import com.joyhsin.hotspot.SceneData.Product;
import com.joyhsin.hotspot.SceneData.ProductInput;
import com.joyhsin.hotspot.SceneData.Scene;
import com.joyhsin.hotspot.SceneData.SceneSummary;
import com.joyhsin.hotspot.SceneData.SceneView;
import com.joyhsin.hotspot.SceneData.StoredImage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

@Service
public class SceneService {

    private static final String DEFAULT_MARKER_COLOR = "#2563EB";

    private static final List<Product> CATALOG = List.of(
            new Product(
                    "POS-01",
                    "双屏智能 POS",
                    "¥2,999",
                    "/sample/products/pos.svg",
                    "支持收银、会员、营销和订单聚合，适合连锁餐饮门店。",
                    null
            ),
            new Product(
                    "NET-06",
                    "Wi-Fi 6 企业级接入点",
                    "到店咨询",
                    "/sample/products/wifi.svg",
                    "为点餐、收银、后厨设备和顾客网络提供稳定无线覆盖。",
                    null
            ),
            new Product(
                    "KDS-02",
                    "厨房信息显示屏",
                    "¥1,899",
                    "/sample/products/display.svg",
                    "实时展示订单与出餐状态，减少漏单并提升后厨协同效率。",
                    null
            ),
            new Product(
                    "TABLE-08",
                    "桌边扫码点餐牌",
                    "¥99 / 套",
                    "/sample/products/table-order.svg",
                    "顾客扫码即可浏览菜单、自助下单，适用于餐桌和包间。",
                    null
            ),
            new Product(
                    "SENSOR-03",
                    "客流感知传感器",
                    "¥699",
                    "/sample/products/sensor.svg",
                    "统计客流趋势与区域热度，为门店运营分析提供数据。",
                    null
            ),
            new Product(
                    "SIGN-05",
                    "门店数字标牌",
                    "¥1,299",
                    "/sample/products/display.svg",
                    "循环展示新品、优惠活动和品牌内容，可远程更新素材。",
                    null
            )
    );

    private final SceneStore sceneStore;
    private final ImageStorageService imageStorageService;

    public SceneService(SceneStore sceneStore, ImageStorageService imageStorageService) {
        this.sceneStore = sceneStore;
        this.imageStorageService = imageStorageService;
    }

    public List<SceneSummary> listScenes() {
        return sceneStore.findAll().stream()
                .map(scene -> new SceneSummary(
                        scene.id(),
                        scene.name(),
                        scene.imageUrl(),
                        scene.draftHotspots().size(),
                        scene.publishedHotspots().size(),
                        scene.draftVersion(),
                        scene.publishedVersion(),
                        scene.updatedAt(),
                        scene.publishedAt()
                ))
                .toList();
    }

    public SceneView getScene(long sceneId) {
        return toView(findScene(sceneId));
    }

    public SceneView createScene(String rawName, MultipartFile image) {
        String name = requiredText(rawName, "Scene name", 120);
        StoredImage storedImage = imageStorageService.store(image);
        try {
            return toView(sceneStore.create(name, storedImage));
        } catch (RuntimeException ex) {
            imageStorageService.deleteQuietly(storedImage.storedFileName());
            throw ex;
        }
    }

    public SceneView createSampleScene() {
        StoredImage sampleImage = new StoredImage(
                null,
                "restaurant-layout.svg",
                "/sample/restaurant-layout.svg",
                "image/svg+xml"
        );
        Scene created = null;
        try {
            created = sceneStore.create("智慧餐饮门店示例", sampleImage);
            Scene draft = sceneStore.replaceDraft(created.id(), sampleHotspots());
            return toView(sceneStore.publish(draft.id()));
        } catch (RuntimeException ex) {
            if (created != null) {
                sceneStore.delete(created.id());
            }
            throw ex;
        }
    }

    public SceneView saveDraft(long sceneId, List<HotspotInput> inputs) {
        findScene(sceneId);
        List<HotspotDraft> drafts = inputs.stream().map(this::toDraft).toList();
        return toView(sceneStore.replaceDraft(sceneId, drafts));
    }

    public SceneView publish(long sceneId) {
        findScene(sceneId);
        return toView(sceneStore.publish(sceneId));
    }

    public void deleteScene(long sceneId) {
        Scene removed = sceneStore.delete(sceneId).orElseThrow(() -> new SceneNotFoundException(sceneId));
        imageStorageService.deleteQuietly(removed.storedFileName());
    }

    public List<Product> searchProducts(String rawQuery) {
        String query = normalizeSearch(rawQuery);
        if (query.isEmpty()) {
            return CATALOG;
        }
        return CATALOG.stream()
                .filter(product -> normalizeSearch(
                        product.code() + " " + product.name() + " " + product.description()
                ).contains(query))
                .toList();
    }

    private static String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", "");
    }

    private List<HotspotDraft> sampleHotspots() {
        return List.of(
                sampleHotspot(0.185, 0.430, "桌边点餐", CATALOG.get(3), "#2563EB"),
                sampleHotspot(0.735, 0.535, "智慧收银", CATALOG.get(0), "#7C3AED"),
                sampleHotspot(0.505, 0.770, "企业 Wi-Fi", CATALOG.get(1), "#0891B2"),
                sampleHotspot(0.795, 0.285, "后厨看板", CATALOG.get(2), "#059669")
        );
    }

    private static HotspotDraft sampleHotspot(
            double x,
            double y,
            String label,
            Product product,
            String markerColor
    ) {
        return new HotspotDraft(null, x, y, label, markerColor, product);
    }

    private HotspotDraft toDraft(HotspotInput input) {
        double x = ratio(input.xRatio(), "xRatio");
        double y = ratio(input.yRatio(), "yRatio");
        ProductInput productInput = input.product();
        String productName = requiredText(productInput.name(), "Product name", 120);
        String label = optionalText(input.label(), 80);
        if (label == null) {
            label = productName;
        }
        String markerColor = optionalText(input.markerColor(), 7);
        if (markerColor == null) {
            markerColor = DEFAULT_MARKER_COLOR;
        }

        Product product = new Product(
                optionalText(productInput.code(), 80),
                productName,
                optionalText(productInput.price(), 80),
                safeUrl(productInput.imageUrl(), "Product image URL"),
                optionalText(productInput.description(), 1000),
                safeUrl(productInput.detailUrl(), "Product detail URL")
        );
        return new HotspotDraft(input.id(), x, y, label, markerColor, product);
    }

    private Scene findScene(long sceneId) {
        return sceneStore.findById(sceneId).orElseThrow(() -> new SceneNotFoundException(sceneId));
    }

    private static SceneView toView(Scene scene) {
        return new SceneView(
                scene.id(),
                scene.name(),
                scene.imageUrl(),
                scene.originalFileName(),
                scene.draftHotspots(),
                scene.publishedHotspots(),
                scene.draftVersion(),
                scene.publishedVersion(),
                scene.createdAt(),
                scene.updatedAt(),
                scene.publishedAt()
        );
    }

    private static double ratio(Double value, String field) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
        return value;
    }

    private static String requiredText(String value, String field, int maxLength) {
        String cleaned = optionalText(value, maxLength);
        if (cleaned == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return cleaned;
    }

    private static String optionalText(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException("Text is longer than " + maxLength + " characters");
        }
        return cleaned;
    }

    private static String safeUrl(String value, String field) {
        String cleaned = optionalText(value, 1000);
        if (cleaned == null) {
            return null;
        }
        if (cleaned.startsWith("/") && !cleaned.startsWith("//")) {
            return cleaned;
        }
        try {
            URI uri = new URI(cleaned);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                throw new IllegalArgumentException(field + " must be an http(s) URL or root-relative path");
            }
            String normalized = scheme.toLowerCase(Locale.ROOT);
            if (!normalized.equals("http") && !normalized.equals("https")) {
                throw new IllegalArgumentException(field + " only supports http and https URLs");
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(field + " is not a valid URL");
        }
    }
}
