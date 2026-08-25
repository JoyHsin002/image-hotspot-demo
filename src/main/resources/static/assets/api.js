export class ApiError extends Error {
    constructor(message, status, payload) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.payload = payload;
    }
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            Accept: "application/json",
            ...(options.headers || {})
        }
    });

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const message = typeof payload === "object" && payload?.message
            ? payload.message
            : `请求失败（HTTP ${response.status}）`;
        throw new ApiError(message, response.status, payload);
    }

    return payload;
}

export const api = {
    listScenes: () => request("/api/scenes"),
    getScene: sceneId => request(`/api/scenes/${encodeURIComponent(sceneId)}`),
    createScene: formData => request("/api/scenes", { method: "POST", body: formData }),
    createSample: () => request("/api/scenes/sample", { method: "POST" }),
    saveDraft: (sceneId, hotspots) => request(`/api/scenes/${encodeURIComponent(sceneId)}/draft`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ hotspots })
    }),
    publish: sceneId => request(`/api/scenes/${encodeURIComponent(sceneId)}/publish`, { method: "POST" }),
    deleteScene: sceneId => request(`/api/scenes/${encodeURIComponent(sceneId)}`, { method: "DELETE" }),
    searchProducts: query => request(`/api/products?q=${encodeURIComponent(query || "")}`)
};
