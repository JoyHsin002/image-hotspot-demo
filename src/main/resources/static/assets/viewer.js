import { api } from "./api.js";
import { formatDate, sceneIdFromUrl, setSceneIdInUrl, toast } from "./ui.js";

const $ = id => document.getElementById(id);
const el = {
    sceneSelect: $("viewerSceneSelect"), modeSelect: $("viewerModeSelect"), title: $("viewerTitle"),
    subtitle: $("viewerSubtitle"), editLink: $("editSceneLink"), stage: $("viewerStageSection"),
    canvas: $("viewerCanvas"), image: $("viewerImage"), markerLayer: $("viewerMarkerLayer"),
    version: $("viewerVersion"), pointCount: $("viewerPointCount"), empty: $("viewerEmpty"),
    emptyText: $("viewerEmptyText"), dialog: $("productDialog"), close: $("dialogCloseButton"),
    productImage: $("dialogProductImage"), imageFallback: $("dialogImageFallback"),
    markerLabel: $("dialogMarkerLabel"), productCode: $("dialogProductCode"), productName: $("dialogProductName"),
    productPrice: $("dialogProductPrice"), productDescription: $("dialogProductDescription"), detailLink: $("dialogDetailLink")
};
const state = { scenes: [], scene: null, mode: new URLSearchParams(location.search).get("mode") === "draft" ? "draft" : "published" };

function populateScenes(selectedId) {
    el.sceneSelect.replaceChildren();
    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = state.scenes.length ? "请选择场景" : "暂无场景";
    el.sceneSelect.appendChild(placeholder);
    for (const scene of state.scenes) {
        const option = document.createElement("option");
        option.value = String(scene.id);
        option.textContent = scene.name;
        el.sceneSelect.appendChild(option);
    }
    el.sceneSelect.value = selectedId ? String(selectedId) : "";
}

function currentHotspots() {
    if (!state.scene) return [];
    return state.mode === "draft" ? state.scene.draftHotspots : state.scene.publishedHotspots;
}

function renderMarkers() {
    el.markerLayer.replaceChildren();
    for (const hotspot of currentHotspots()) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "viewer-marker";
        button.style.left = `${hotspot.xRatio * 100}%`;
        button.style.top = `${hotspot.yRatio * 100}%`;
        button.style.setProperty("--marker-color", hotspot.markerColor || "#2563EB");
        button.setAttribute("aria-label", `查看商品：${hotspot.product.name}`);
        const dot = document.createElement("span");
        dot.className = "marker-dot";
        const caption = document.createElement("span");
        caption.className = "marker-caption";
        caption.textContent = hotspot.label || hotspot.product.name;
        button.append(dot, caption);
        button.addEventListener("click", () => openProduct(hotspot));
        el.markerLayer.appendChild(button);
    }
}

function renderScene() {
    if (!state.scene) return showEmpty("还没有可以展示的场景。", false);
    const hotspots = currentHotspots();
    el.title.textContent = state.scene.name;
    el.subtitle.textContent = state.mode === "draft" ? "草稿预览：仅供后台配置检查。" : "已发布版本：点击图片中的点位查看商品详情。";
    el.image.src = state.scene.imageUrl;
    el.image.alt = state.scene.name;
    el.editLink.href = `/editor.html?scene=${state.scene.id}`;
    el.sceneSelect.value = String(state.scene.id);
    el.modeSelect.value = state.mode;
    el.version.textContent = state.mode === "draft"
        ? `草稿 v${state.scene.draftVersion} · 更新 ${formatDate(state.scene.updatedAt)}`
        : `线上 v${state.scene.publishedVersion} · 发布 ${formatDate(state.scene.publishedAt)}`;
    el.pointCount.textContent = `${hotspots.length} 个点位`;
    el.stage.hidden = false;
    el.empty.hidden = true;
    setSceneIdInUrl(state.scene.id, { mode: state.mode });
    renderMarkers();

    if (!hotspots.length) {
        el.empty.hidden = false;
        el.emptyText.textContent = state.mode === "draft" ? "这个场景的草稿还没有点位。" : "这个场景还没有发布点位，可切换到草稿预览。";
    }
}

function showEmpty(message, clearScene = true) {
    if (clearScene) state.scene = null;
    el.stage.hidden = true;
    el.empty.hidden = false;
    el.emptyText.textContent = message;
    el.title.textContent = "交互场景预览";
    el.subtitle.textContent = "点击图片中的点位查看商品详情。";
    el.editLink.href = "/editor.html";
    setSceneIdInUrl(null, { mode: state.mode });
}

async function loadScene(id) {
    if (!id) return showEmpty("还没有可以展示的场景。");
    el.sceneSelect.disabled = true;
    try {
        state.scene = await api.getScene(id);
        renderScene();
    } catch (error) {
        showEmpty("场景读取失败，请返回配置后台检查。", true);
        toast(error.message || "读取场景失败", "error", 5000);
    } finally {
        el.sceneSelect.disabled = false;
    }
}

function openProduct(hotspot) {
    const product = hotspot.product;
    el.markerLabel.textContent = hotspot.label || "商品点位";
    el.productCode.textContent = product.code || "";
    el.productCode.hidden = !product.code;
    el.productName.textContent = product.name;
    el.productPrice.textContent = product.price || "";
    el.productPrice.hidden = !product.price;
    el.productDescription.textContent = product.description || "暂无更多商品介绍。";
    if (product.imageUrl) {
        el.productImage.src = product.imageUrl;
        el.productImage.alt = product.name;
        el.productImage.hidden = false;
        el.imageFallback.hidden = true;
    } else {
        el.productImage.removeAttribute("src");
        el.productImage.hidden = true;
        el.imageFallback.hidden = false;
    }
    if (product.detailUrl) {
        el.detailLink.href = product.detailUrl;
        el.detailLink.hidden = false;
    } else {
        el.detailLink.removeAttribute("href");
        el.detailLink.hidden = true;
    }
    if (typeof el.dialog.showModal === "function") el.dialog.showModal(); else el.dialog.setAttribute("open", "");
}

function closeDialog() {
    if (typeof el.dialog.close === "function") el.dialog.close(); else el.dialog.removeAttribute("open");
}

el.productImage.addEventListener("error", () => { el.productImage.hidden = true; el.imageFallback.hidden = false; });
el.image.addEventListener("error", () => toast("场景图片加载失败", "error", 5000));
el.close.addEventListener("click", closeDialog);
el.dialog.addEventListener("click", event => {
    const rect = el.dialog.getBoundingClientRect();
    if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeDialog();
});
el.sceneSelect.addEventListener("change", event => { const id = Number(event.target.value); if (id) loadScene(id); });
el.modeSelect.addEventListener("change", event => { state.mode = event.target.value === "draft" ? "draft" : "published"; renderScene(); });

async function initialize() {
    el.modeSelect.value = state.mode;
    try {
        state.scenes = await api.listScenes();
        const id = sceneIdFromUrl() || state.scenes[0]?.id;
        populateScenes(id);
        if (id) await loadScene(id); else showEmpty("还没有场景，请先前往配置后台创建。", true);
    } catch (error) {
        populateScenes(null);
        showEmpty("无法连接 Spring Boot 后端，请确认应用已经启动。", true);
        toast(error.message || "无法连接后端", "error", 6000);
    }
}

await initialize();
