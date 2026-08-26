import { api } from "./api.js";
import { clamp, sceneIdFromUrl, setBusy, setSceneIdInUrl, toast } from "./ui.js";

const $ = id => document.getElementById(id);
const el = {
    uploadForm: $("uploadForm"), uploadButton: $("uploadButton"), sampleButton: $("sampleButton"),
    sceneName: $("sceneName"), sceneImage: $("sceneImage"), sceneSelect: $("sceneSelect"),
    deleteSceneButton: $("deleteSceneButton"), connectionStatus: $("connectionStatus"), workspace: $("workspace"),
    noSceneState: $("noSceneState"), currentSceneName: $("currentSceneName"), dirtyBadge: $("dirtyBadge"),
    draftVersion: $("draftVersion"), publishedVersion: $("publishedVersion"), pointCount: $("pointCount"),
    pointListCount: $("pointListCount"), saveButton: $("saveButton"), publishButton: $("publishButton"),
    previewDraftLink: $("previewDraftLink"), viewPublishedLink: $("viewPublishedLink"),
    sceneCanvas: $("sceneCanvas"), sceneImagePreview: $("sceneImagePreview"), markerLayer: $("markerLayer"),
    zoomOutButton: $("zoomOutButton"), zoomRange: $("zoomRange"), zoomValue: $("zoomValue"),
    zoomInButton: $("zoomInButton"), zoomFitButton: $("zoomFitButton"),
    inspectorEmpty: $("inspectorEmpty"), pointForm: $("pointForm"), coordinateBadge: $("coordinateBadge"),
    productSearch: $("productSearch"), productResults: $("productResults"), markerLabel: $("markerLabel"),
    markerColor: $("markerColor"), productCode: $("productCode"), productName: $("productName"),
    productPrice: $("productPrice"), productImageUrl: $("productImageUrl"), productDetailUrl: $("productDetailUrl"),
    productDescription: $("productDescription"), descriptionCount: $("descriptionCount"),
    duplicatePointButton: $("duplicatePointButton"), deletePointButton: $("deletePointButton"), pointList: $("pointList")
};

const state = {
    scenes: [], scene: null, hotspots: [], selectedClientId: null, dirty: false,
    loadingScene: false, zoom: 100, catalog: [], catalogRequest: 0
};

function clientId() {
    return crypto.randomUUID?.() || `p-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function toClient(hotspot) {
    return {
        clientId: clientId(), id: hotspot.id ?? null,
        xRatio: Number(hotspot.xRatio), yRatio: Number(hotspot.yRatio),
        label: hotspot.label || "", markerColor: hotspot.markerColor || "#2563EB",
        product: {
            code: hotspot.product?.code || "", name: hotspot.product?.name || "",
            price: hotspot.product?.price || "", imageUrl: hotspot.product?.imageUrl || "",
            description: hotspot.product?.description || "", detailUrl: hotspot.product?.detailUrl || ""
        }
    };
}

function selectedPoint() {
    return state.hotspots.find(point => point.clientId === state.selectedClientId) || null;
}

function caption(point) {
    return point.label.trim() || point.product.name.trim() || "新点位";
}

function setConnection(ok, text) {
    el.connectionStatus.textContent = text;
    el.connectionStatus.classList.toggle("error", !ok);
}

function markDirty() {
    if (!state.scene) return;
    state.dirty = true;
    renderStatus();
}

function renderStatus() {
    const hasScene = Boolean(state.scene);
    el.dirtyBadge.textContent = state.dirty ? "草稿未保存" : "草稿已保存";
    el.dirtyBadge.className = `badge ${state.dirty ? "warning" : "success"}`;
    el.saveButton.disabled = !hasScene || !state.dirty;
    el.publishButton.disabled = !hasScene;
    el.deleteSceneButton.disabled = !hasScene;
    el.draftVersion.textContent = String(state.scene?.draftVersion ?? 0);
    el.publishedVersion.textContent = String(state.scene?.publishedVersion ?? 0);
    el.pointCount.textContent = `${state.hotspots.length} 个点位`;
    el.pointListCount.textContent = String(state.hotspots.length);
}

function setZoom(raw) {
    const value = Math.min(200, Math.max(50, Math.round(Number(raw) / 10) * 10));
    state.zoom = Number.isFinite(value) ? value : 100;
    el.sceneCanvas.style.width = `${state.zoom}%`;
    el.zoomRange.value = String(state.zoom);
    el.zoomValue.textContent = `${state.zoom}%`;
    el.zoomOutButton.disabled = state.zoom <= 50;
    el.zoomInButton.disabled = state.zoom >= 200;
}

function updateCoordinate(point = selectedPoint()) {
    el.coordinateBadge.textContent = point
        ? `${(point.xRatio * 100).toFixed(1)}%, ${(point.yRatio * 100).toFixed(1)}%`
        : "未选择";
}

function renderMarkers() {
    el.markerLayer.replaceChildren();
    for (const point of state.hotspots) {
        const marker = document.createElement("button");
        marker.type = "button";
        marker.className = "editor-marker";
        marker.classList.toggle("selected", point.clientId === state.selectedClientId);
        marker.style.left = `${point.xRatio * 100}%`;
        marker.style.top = `${point.yRatio * 100}%`;
        marker.style.setProperty("--marker-color", point.markerColor);
        marker.setAttribute("aria-label", `编辑点位：${caption(point)}`);

        const dot = document.createElement("span");
        dot.className = "marker-dot";
        const label = document.createElement("span");
        label.className = "marker-caption";
        label.textContent = caption(point);
        marker.append(dot, label);

        marker.addEventListener("click", event => {
            event.stopPropagation();
            selectPoint(point.clientId);
        });
        marker.addEventListener("pointerdown", event => beginDrag(event, marker, point));
        el.markerLayer.appendChild(marker);
    }
}

function beginDrag(event, marker, point) {
    if (event.button !== 0 && event.pointerType !== "touch") return;
    event.preventDefault();
    event.stopPropagation();
    state.selectedClientId = point.clientId;
    renderInspector();
    renderPointList();
    marker.classList.add("selected");
    marker.setPointerCapture(event.pointerId);
    let moved = false;
    const startX = event.clientX;
    const startY = event.clientY;

    const move = moveEvent => {
        const rect = el.sceneImagePreview.getBoundingClientRect();
        if (!rect.width || !rect.height) return;
        moved ||= Math.abs(moveEvent.clientX - startX) > 2 || Math.abs(moveEvent.clientY - startY) > 2;
        point.xRatio = clamp((moveEvent.clientX - rect.left) / rect.width);
        point.yRatio = clamp((moveEvent.clientY - rect.top) / rect.height);
        marker.style.left = `${point.xRatio * 100}%`;
        marker.style.top = `${point.yRatio * 100}%`;
        updateCoordinate(point);
        markDirty();
    };
    const end = endEvent => {
        marker.removeEventListener("pointermove", move);
        marker.removeEventListener("pointerup", end);
        marker.removeEventListener("pointercancel", end);
        if (marker.hasPointerCapture(endEvent.pointerId)) marker.releasePointerCapture(endEvent.pointerId);
        renderAll();
        if (moved) toast("点位位置已调整，记得保存草稿", "success", 1800);
    };
    marker.addEventListener("pointermove", move);
    marker.addEventListener("pointerup", end);
    marker.addEventListener("pointercancel", end);
}

function renderPointList() {
    el.pointList.replaceChildren();
    if (!state.hotspots.length) {
        el.pointList.innerHTML = '<p class="muted center">暂无点位</p>';
        return;
    }
    state.hotspots.forEach((point, index) => {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "point-list-item";
        button.classList.toggle("active", point.clientId === state.selectedClientId);
        button.addEventListener("click", () => selectPoint(point.clientId));
        const dot = document.createElement("span");
        dot.className = "point-list-dot";
        dot.style.background = point.markerColor;
        const text = document.createElement("span");
        text.className = "point-list-copy";
        const strong = document.createElement("strong");
        strong.textContent = caption(point);
        const small = document.createElement("small");
        small.textContent = point.product.name || `点位 ${index + 1} · 未关联商品`;
        text.append(strong, small);
        button.append(dot, text);
        el.pointList.appendChild(button);
    });
}

function renderInspector() {
    const point = selectedPoint();
    el.inspectorEmpty.hidden = Boolean(point);
    el.pointForm.hidden = !point;
    updateCoordinate(point);
    if (!point) return;
    el.markerLabel.value = point.label;
    el.markerColor.value = point.markerColor;
    el.productCode.value = point.product.code;
    el.productName.value = point.product.name;
    el.productPrice.value = point.product.price;
    el.productImageUrl.value = point.product.imageUrl;
    el.productDetailUrl.value = point.product.detailUrl;
    el.productDescription.value = point.product.description;
    el.descriptionCount.textContent = String(point.product.description.length);
    renderCatalog();
}

function renderCatalog() {
    el.productResults.replaceChildren();
    if (!state.catalog.length) {
        el.productResults.innerHTML = '<p class="catalog-empty">没有匹配的商品</p>';
        return;
    }
    const current = selectedPoint();
    for (const product of state.catalog) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "catalog-item";
        button.classList.toggle("active", current?.product.code === product.code);
        button.addEventListener("click", () => applyProduct(product));
        const thumb = document.createElement("img");
        thumb.src = product.imageUrl || "/sample/products/pos.svg";
        thumb.alt = "";
        const copy = document.createElement("span");
        const name = document.createElement("strong");
        name.textContent = product.name;
        const meta = document.createElement("small");
        meta.textContent = `${product.code || "无编码"} · ${product.price || "暂无价格"}`;
        copy.append(name, meta);
        button.append(thumb, copy);
        el.productResults.appendChild(button);
    }
}

async function searchCatalog(query = "") {
    const requestId = ++state.catalogRequest;
    try {
        const products = await api.searchProducts(query);
        if (requestId !== state.catalogRequest) return;
        state.catalog = products;
        renderCatalog();
    } catch (error) {
        if (requestId === state.catalogRequest) toast(error.message || "商品库读取失败", "error");
    }
}

function applyProduct(product) {
    const point = selectedPoint();
    if (!point) return;
    point.label = product.name;
    point.product = { ...product, code: product.code || "", price: product.price || "", imageUrl: product.imageUrl || "", description: product.description || "", detailUrl: product.detailUrl || "" };
    markDirty();
    renderMarkers();
    renderPointList();
    renderInspector();
    toast(`已关联：${product.name}`, "success", 1800);
}

function renderAll() {
    renderMarkers();
    renderPointList();
    renderInspector();
    renderStatus();
}

function selectPoint(id, focusName = false) {
    state.selectedClientId = id;
    renderAll();
    if (focusName) el.productName.focus();
}

function addPoint(event) {
    if (!state.scene || event.target.closest(".editor-marker")) return;
    if (state.hotspots.length >= 100) {
        toast("单个场景最多支持 100 个点位", "error");
        return;
    }
    const rect = el.sceneImagePreview.getBoundingClientRect();
    if (!rect.width || !rect.height) return;
    const point = {
        clientId: clientId(), id: null,
        xRatio: clamp((event.clientX - rect.left) / rect.width),
        yRatio: clamp((event.clientY - rect.top) / rect.height),
        label: "新点位", markerColor: "#2563EB",
        product: { code: "", name: "", price: "", imageUrl: "", description: "", detailUrl: "" }
    };
    state.hotspots.push(point);
    state.selectedClientId = point.clientId;
    markDirty();
    renderAll();
    el.productName.focus();
}

function updateFromForm() {
    const point = selectedPoint();
    if (!point) return;
    point.label = el.markerLabel.value;
    point.markerColor = el.markerColor.value;
    point.product.code = el.productCode.value;
    point.product.name = el.productName.value;
    point.product.price = el.productPrice.value;
    point.product.imageUrl = el.productImageUrl.value;
    point.product.detailUrl = el.productDetailUrl.value;
    point.product.description = el.productDescription.value;
    el.descriptionCount.textContent = String(point.product.description.length);
    markDirty();
    renderMarkers();
    renderPointList();
}

function duplicatePoint() {
    const source = selectedPoint();
    if (!source) return;
    if (state.hotspots.length >= 100) return toast("单个场景最多支持 100 个点位", "error");
    const copy = {
        ...source, clientId: clientId(), id: null,
        xRatio: clamp(source.xRatio + 0.025), yRatio: clamp(source.yRatio + 0.025),
        label: source.label ? `${source.label} 副本` : "复制点位", product: { ...source.product }
    };
    state.hotspots.push(copy);
    state.selectedClientId = copy.clientId;
    markDirty();
    renderAll();
    toast("点位已复制", "success", 1600);
}

function deletePoint() {
    const index = state.hotspots.findIndex(point => point.clientId === state.selectedClientId);
    if (index < 0) return;
    state.hotspots.splice(index, 1);
    state.selectedClientId = state.hotspots[index]?.clientId || state.hotspots[index - 1]?.clientId || null;
    markDirty();
    renderAll();
    toast("点位已从草稿删除，保存后生效", "success");
}

function nudge(dx, dy) {
    const point = selectedPoint();
    if (!point) return;
    point.xRatio = clamp(point.xRatio + dx);
    point.yRatio = clamp(point.yRatio + dy);
    markDirty();
    renderMarkers();
    updateCoordinate(point);
}

function populateSceneSelect(selectedId = state.scene?.id) {
    el.sceneSelect.replaceChildren();
    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = state.scenes.length ? "请选择场景" : "暂无场景";
    el.sceneSelect.appendChild(placeholder);
    for (const scene of state.scenes) {
        const option = document.createElement("option");
        option.value = String(scene.id);
        option.textContent = `${scene.name}（草稿 ${scene.draftCount} / 线上 ${scene.publishedCount}）`;
        el.sceneSelect.appendChild(option);
    }
    el.sceneSelect.value = selectedId ? String(selectedId) : "";
}

async function refreshScenes(selectedId = state.scene?.id) {
    state.scenes = await api.listScenes();
    populateSceneSelect(selectedId);
}

function applyScene(scene) {
    state.scene = scene;
    state.hotspots = (scene.draftHotspots || []).map(toClient);
    state.selectedClientId = null;
    state.dirty = false;
    el.workspace.hidden = false;
    el.noSceneState.hidden = true;
    el.currentSceneName.textContent = scene.name;
    el.sceneImagePreview.src = scene.imageUrl;
    el.sceneImagePreview.alt = scene.name;
    el.previewDraftLink.href = `/viewer.html?scene=${scene.id}&mode=draft`;
    el.viewPublishedLink.href = `/viewer.html?scene=${scene.id}&mode=published`;
    el.sceneSelect.value = String(scene.id);
    setSceneIdInUrl(scene.id);
    setZoom(100);
    renderAll();
}

function clearScene() {
    state.scene = null;
    state.hotspots = [];
    state.selectedClientId = null;
    state.dirty = false;
    el.workspace.hidden = true;
    el.noSceneState.hidden = false;
    el.sceneSelect.value = "";
    setSceneIdInUrl(null);
    setZoom(100);
    renderAll();
}

async function loadScene(sceneId, force = false) {
    if (!sceneId || state.loadingScene) return false;
    if (state.dirty && !force && !confirm("当前场景有未保存修改，切换后会丢失。是否继续？")) {
        el.sceneSelect.value = state.scene ? String(state.scene.id) : "";
        return false;
    }
    state.loadingScene = true;
    el.sceneSelect.disabled = true;
    try {
        applyScene(await api.getScene(sceneId));
        setConnection(true, "后端已连接");
        return true;
    } catch (error) {
        toast(error.message || "读取场景失败", "error", 5000);
        setConnection(false, "后端连接异常");
        return false;
    } finally {
        state.loadingScene = false;
        el.sceneSelect.disabled = false;
    }
}

async function handleUpload(event) {
    event.preventDefault();
    if (state.dirty && !confirm("当前场景有未保存修改，创建新场景会放弃这些修改。是否继续？")) return;
    if (!el.uploadForm.reportValidity()) return;
    const file = el.sceneImage.files[0];
    if (!file) return toast("请选择图片", "error");
    const data = new FormData();
    data.append("name", el.sceneName.value.trim());
    data.append("image", file);
    setBusy(el.uploadButton, true, "上传中…");
    try {
        const created = await api.createScene(data);
        await refreshScenes(created.id);
        applyScene(created);
        el.uploadForm.reset();
        toast("场景已创建，现在可以点击图片添加点位");
    } catch (error) {
        toast(error.message || "上传失败", "error", 5000);
    } finally {
        setBusy(el.uploadButton, false);
    }
}

async function createSample() {
    if (state.dirty && !confirm("当前场景有未保存修改，创建示例会放弃这些修改。是否继续？")) return;
    setBusy(el.sampleButton, true, "创建中…");
    try {
        const created = await api.createSample();
        await refreshScenes(created.id);
        applyScene(created);
        toast("示例场景已创建并自动发布");
    } catch (error) {
        toast(error.message || "创建示例失败", "error", 5000);
    } finally {
        setBusy(el.sampleButton, false);
    }
}

function payload() {
    return state.hotspots.map(point => ({
        id: point.id,
        xRatio: Number(point.xRatio.toFixed(6)), yRatio: Number(point.yRatio.toFixed(6)),
        label: point.label, markerColor: point.markerColor,
        product: { ...point.product }
    }));
}

async function saveDraft(silent = false) {
    if (!state.scene) return false;
    const invalid = state.hotspots.findIndex(point => !point.product.name.trim());
    if (invalid >= 0) {
        selectPoint(state.hotspots[invalid].clientId, true);
        toast(`第 ${invalid + 1} 个点位没有填写商品名称`, "error");
        return false;
    }
    if (!state.dirty) return true;
    const selectedIndex = state.hotspots.findIndex(point => point.clientId === state.selectedClientId);
    setBusy(el.saveButton, true, "保存中…");
    try {
        const saved = await api.saveDraft(state.scene.id, payload());
        state.scene = saved;
        state.hotspots = saved.draftHotspots.map(toClient);
        state.selectedClientId = selectedIndex >= 0 ? state.hotspots[selectedIndex]?.clientId || null : null;
        state.dirty = false;
        await refreshScenes(saved.id);
        renderAll();
        if (!silent) toast("草稿已保存到后端");
        return true;
    } catch (error) {
        toast(error.message || "保存失败", "error", 5000);
        return false;
    } finally {
        setBusy(el.saveButton, false);
        renderStatus();
    }
}

async function publishScene() {
    if (!state.scene) return;
    setBusy(el.publishButton, true, "发布中…");
    try {
        if (!(await saveDraft(true))) return;
        const published = await api.publish(state.scene.id);
        applyScene(published);
        await refreshScenes(published.id);
        toast(`发布成功：线上版本 v${published.publishedVersion}`);
    } catch (error) {
        toast(error.message || "发布失败", "error", 5000);
    } finally {
        setBusy(el.publishButton, false);
        renderStatus();
    }
}

async function deleteScene() {
    if (!state.scene || !confirm(`确定删除场景“${state.scene.name}”吗？图片、草稿和发布数据都会删除。`)) return;
    setBusy(el.deleteSceneButton, true, "删除中…");
    try {
        await api.deleteScene(state.scene.id);
        clearScene();
        await refreshScenes(null);
        toast("场景已删除");
    } catch (error) {
        toast(error.message || "删除失败", "error", 5000);
    } finally {
        setBusy(el.deleteSceneButton, false);
        renderStatus();
    }
}

for (const input of [el.markerLabel, el.markerColor, el.productCode, el.productName, el.productPrice, el.productImageUrl, el.productDetailUrl, el.productDescription]) {
    input.addEventListener("input", updateFromForm);
}

let searchTimer;
el.productSearch.addEventListener("input", () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => searchCatalog(el.productSearch.value), 180);
});
el.sceneCanvas.addEventListener("click", addPoint);
el.uploadForm.addEventListener("submit", handleUpload);
el.sampleButton.addEventListener("click", createSample);
el.saveButton.addEventListener("click", () => saveDraft());
el.publishButton.addEventListener("click", publishScene);
el.duplicatePointButton.addEventListener("click", duplicatePoint);
el.deletePointButton.addEventListener("click", deletePoint);
el.deleteSceneButton.addEventListener("click", deleteScene);
el.sceneSelect.addEventListener("change", event => {
    const id = Number(event.target.value);
    if (id) loadScene(id); else if (state.scene) event.target.value = String(state.scene.id);
});
el.zoomRange.addEventListener("input", event => setZoom(event.target.value));
el.zoomOutButton.addEventListener("click", () => setZoom(state.zoom - 10));
el.zoomInButton.addEventListener("click", () => setZoom(state.zoom + 10));
el.zoomFitButton.addEventListener("click", () => setZoom(100));
el.sceneImagePreview.addEventListener("error", () => toast("场景图片加载失败，请检查 data/uploads", "error", 5000));

window.addEventListener("keydown", event => {
    const target = event.target;
    const editing = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement || target instanceof HTMLSelectElement || target?.isContentEditable;
    if (editing || !selectedPoint()) return;
    if (event.key === "Delete" || event.key === "Backspace") {
        event.preventDefault(); deletePoint(); return;
    }
    const step = event.shiftKey ? 0.01 : 0.002;
    const delta = { ArrowLeft: [-step, 0], ArrowRight: [step, 0], ArrowUp: [0, -step], ArrowDown: [0, step] }[event.key];
    if (delta) { event.preventDefault(); nudge(delta[0], delta[1]); }
});
window.addEventListener("beforeunload", event => {
    if (!state.dirty) return;
    event.preventDefault();
    event.returnValue = "";
});

async function initialize() {
    setZoom(100);
    renderStatus();
    await searchCatalog();
    try {
        await refreshScenes();
        const id = sceneIdFromUrl() || state.scenes[0]?.id;
        if (id) await loadScene(id, true); else clearScene();
        setConnection(true, "后端已连接");
    } catch (error) {
        clearScene();
        setConnection(false, "无法连接后端");
        toast("无法连接 Spring Boot 后端，请确认应用已启动", "error", 6000);
    }
}

await initialize();
