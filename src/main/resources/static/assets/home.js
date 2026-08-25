import { api } from "./api.js";
import { formatDate, toast } from "./ui.js";

const container = document.getElementById("recentScenes");

function renderEmpty() {
    container.replaceChildren();
    const card = document.createElement("div");
    card.className = "card loading-card";
    const title = document.createElement("strong");
    title.textContent = "还没有场景";
    const text = document.createElement("p");
    text.textContent = "进入配置后台上传第一张图片，或一键创建内置示例。";
    const link = document.createElement("a");
    link.className = "button primary";
    link.href = "/editor.html";
    link.textContent = "创建第一个场景";
    card.append(title, text, link);
    container.appendChild(card);
}

function renderScenes(scenes) {
    container.replaceChildren();
    if (!scenes.length) {
        renderEmpty();
        return;
    }

    for (const scene of scenes.slice(0, 6)) {
        const article = document.createElement("article");
        article.className = "card scene-card";

        const imageLink = document.createElement("a");
        imageLink.className = "scene-card-image";
        imageLink.href = `/viewer.html?scene=${scene.id}`;
        const image = document.createElement("img");
        image.src = scene.imageUrl;
        image.alt = scene.name;
        image.loading = "lazy";
        imageLink.appendChild(image);

        const body = document.createElement("div");
        body.className = "scene-card-body";
        const heading = document.createElement("h3");
        heading.textContent = scene.name;
        const meta = document.createElement("div");
        meta.className = "scene-card-meta";
        meta.innerHTML = `<span>草稿 ${scene.draftCount} 点</span><span>线上 ${scene.publishedCount} 点</span><span>${formatDate(scene.updatedAt)}</span>`;

        const actions = document.createElement("div");
        actions.className = "scene-card-actions";
        const edit = document.createElement("a");
        edit.className = "button secondary";
        edit.href = `/editor.html?scene=${scene.id}`;
        edit.textContent = "编辑";
        const preview = document.createElement("a");
        preview.className = "button primary";
        preview.href = `/viewer.html?scene=${scene.id}`;
        preview.textContent = "查看";
        actions.append(edit, preview);
        body.append(heading, meta, actions);
        article.append(imageLink, body);
        container.appendChild(article);
    }
}

try {
    renderScenes(await api.listScenes());
} catch (error) {
    container.innerHTML = '<div class="card loading-card">无法连接 Spring Boot 后端，请确认应用已经启动。</div>';
    toast(error.message || "读取场景失败", "error", 5000);
}
