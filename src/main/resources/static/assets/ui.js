export function clamp(value, min = 0, max = 1) {
    return Math.min(max, Math.max(min, value));
}

export function sceneIdFromUrl() {
    const raw = new URLSearchParams(location.search).get("scene");
    const value = Number(raw);
    return Number.isInteger(value) && value > 0 ? value : null;
}

export function setSceneIdInUrl(sceneId, extra = {}) {
    const url = new URL(location.href);
    if (sceneId) {
        url.searchParams.set("scene", String(sceneId));
    } else {
        url.searchParams.delete("scene");
    }
    for (const [key, value] of Object.entries(extra)) {
        if (value == null || value === "") {
            url.searchParams.delete(key);
        } else {
            url.searchParams.set(key, String(value));
        }
    }
    history.replaceState(null, "", url);
}

export function formatDate(value) {
    if (!value) return "未发布";
    return new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

export function toast(message, type = "success", timeout = 3200) {
    let region = document.getElementById("toastRegion");
    if (!region) {
        region = document.createElement("div");
        region.id = "toastRegion";
        region.className = "toast-region";
        region.setAttribute("aria-live", "polite");
        document.body.appendChild(region);
    }
    const item = document.createElement("div");
    item.className = `toast ${type}`;
    item.textContent = message;
    region.appendChild(item);
    requestAnimationFrame(() => item.classList.add("visible"));
    window.setTimeout(() => {
        item.classList.remove("visible");
        window.setTimeout(() => item.remove(), 180);
    }, timeout);
}

export function setBusy(button, busy, busyText = "处理中…") {
    if (!button.dataset.label) button.dataset.label = button.textContent;
    button.disabled = busy;
    button.textContent = busy ? busyText : button.dataset.label;
}

export function escapeText(value) {
    return value == null ? "" : String(value);
}
