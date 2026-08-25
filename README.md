# Image Hotspot Demo

一个可以本地直接启动的“图片打点 + 商品详情”完整 Demo。

- 后端：Spring Boot 3.4、Java 17
- 前端：原生 HTML / CSS / JavaScript，无 Node.js 构建步骤
- 存储：本地 JSON 文件 + 本地图片目录
- 运行方式：Maven Wrapper 或 Docker Compose

[![CI](https://github.com/JoyHsin002/image-hotspot-demo/actions/workflows/ci.yml/badge.svg)](https://github.com/JoyHsin002/image-hotspot-demo/actions/workflows/ci.yml)

![内置测试平面图](src/main/resources/static/sample/restaurant-layout.png)

## 已实现功能

### 配置后台

- 上传 PNG、JPEG、GIF、WebP 场景图，最大 10 MB
- 一键创建内置智慧餐饮示例场景
- 点击图片新增点位
- 拖拽点位调整位置
- 使用方向键微调，按住 `Shift` 可加大步长
- 50%～200% 画布缩放，不影响相对坐标
- 点位复制、删除和列表选择
- Mock 商品库搜索与快速关联
- 编辑商品名称、价格、图片、简介、详情链接
- 未保存状态提示和离开页面保护
- 最多 100 个点位

### 展示页面

- 独立场景预览页
- 根据 0～1 相对坐标响应式渲染点位
- 点击点位打开商品详情弹窗
- 支持商品图片、价格、介绍和详情链接
- 手机、平板和桌面端自适应

### 后端

- REST API
- 图片文件头校验，避免仅依赖扩展名
- URL 协议白名单校验
- 原子写入 `scenes.json`
- 统一 JSON 错误响应
- 集成测试
- Dockerfile、Docker Compose 和 GitHub Actions CI

## 环境要求

本地 Maven 方式：

- JDK 17
- 可访问 Maven Central，用于首次下载 Maven 和依赖

检查 Java：

```bash
java -version
```

输出中应包含 `17`。

## 本地启动

### macOS / Linux

```bash
git clone https://github.com/JoyHsin002/image-hotspot-demo.git
cd image-hotspot-demo
chmod +x mvnw
./mvnw spring-boot:run
```

### Windows

```powershell
git clone https://github.com/JoyHsin002/image-hotspot-demo.git
cd image-hotspot-demo
mvnw.cmd spring-boot:run
```

启动后打开：

| 页面 | 地址 |
|---|---|
| 首页 | `http://localhost:8080/` |
| 配置后台 | `http://localhost:8080/editor.html` |
| 效果预览 | `http://localhost:8080/viewer.html` |

第一次体验时，可以在配置后台点击 **“一键创建示例”**，无需手动上传图片。

## Docker 启动

```bash
docker compose up --build
```

然后打开：

```text
http://localhost:8080/editor.html
```

Docker Compose 会把运行数据挂载到项目根目录的 `data/`。

## 数据保存位置

默认目录：

```text
data/
├── scenes.json
└── uploads/
```

可以通过环境变量修改：

```bash
HOTSPOT_DATA_DIR=/your/path ./mvnw spring-boot:run
```

本地 JSON 存储是为了让 Demo 零依赖启动。正式项目建议替换为 MySQL/PostgreSQL，并把图片存入 OSS、S3 或 MinIO。

## API

### 查询场景列表

```http
GET /api/scenes
```

### 创建上传场景

```http
POST /api/scenes
Content-Type: multipart/form-data

name=<场景名称>
image=<图片文件>
```

### 创建内置示例

```http
POST /api/scenes/sample
```

### 查询场景详情

```http
GET /api/scenes/{sceneId}
```

### 覆盖保存点位

```http
PUT /api/scenes/{sceneId}/hotspots
Content-Type: application/json
```

示例：

```json
{
  "hotspots": [
    {
      "xRatio": 0.5274,
      "yRatio": 0.3118,
      "label": "智慧收银",
      "product": {
        "name": "双屏智能 POS",
        "price": "¥2,999",
        "imageUrl": "/sample/products/pos.svg",
        "description": "支持收银、会员、营销和订单聚合。",
        "detailUrl": "https://example.com/products/pos"
      }
    }
  ]
}
```

### 删除场景

```http
DELETE /api/scenes/{sceneId}
```

## 坐标设计

后端不保存像素值，而是保存图片宽高上的相对坐标：

```text
xRatio = 点击位置相对图片左侧的距离 / 图片显示宽度
yRatio = 点击位置相对图片顶部的距离 / 图片显示高度
```

渲染时转换为百分比：

```javascript
marker.style.left = `${hotspot.xRatio * 100}%`;
marker.style.top = `${hotspot.yRatio * 100}%`;
```

因此图片在不同屏幕宽度下缩放后，点位仍然保持在正确位置。

## 运行测试

```bash
./mvnw verify
```

## 项目结构

```text
src/main/java/com/example/hotspot/
├── api/                 REST 接口、DTO、异常处理
├── config/              本地存储配置
├── domain/              场景、点位、商品模型
├── service/             业务逻辑与 JSON 持久化
└── storage/             图片上传和读取

src/main/resources/static/
├── index.html           首页
├── editor.html          配置后台
├── viewer.html          效果预览
├── assets/              CSS 和 JavaScript
└── sample/              内置平面图及商品示意图
```

## 生产化改造建议

这个仓库是可运行 Demo。接入 Spring Cloud 业务系统时，建议继续补充：

1. MySQL 表结构与乐观锁版本号。
2. 草稿、发布、回滚流程。
3. 商品中心真实搜索接口，只保存 `productId`。
4. 租户、角色和数据权限。
5. OSS/MinIO 图片存储和 CDN。
6. 商品上下架后的点位兜底策略。
7. 点击曝光、转化等埋点统计。

## License

MIT
