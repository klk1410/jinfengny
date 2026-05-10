# 环保油项目（第一阶段骨架）

已按当前文档先落地 4 个工程，全部放在 `环保油` 文件夹下：

- `admin-backend`：管理系统后端（Spring Boot）
- `admin-frontend`：管理系统前端（Vue3 + Vite）
- `app-backend`：服务号/客户端后端（Spring Boot）
- `app-frontend`：服务号/客户端前端（Vue3 + Vite）

## 目录结构

```text
环保油/
├─ admin-backend
├─ admin-frontend
├─ app-backend
├─ app-frontend
├─ 环保油管理系统.md
└─ 环保油数据库设计.sql
```

## 运行方式

### 1) 管理系统后端

```bash
cd admin-backend
mvn spring-boot:run
```

- 地址：`http://127.0.0.1:7266/prod-api`
- 示例接口：
  - `GET /admin/dashboard/summary`
  - `GET /admin/merchant/list`
  - `GET /admin/order/list`
  - `GET /admin/permission/share-template`
  - `POST /admin/permission/share-grant`

### 2) 服务号后端

```bash
cd app-backend
mvn spring-boot:run
```

- 地址：`http://127.0.0.1:7267/app-api`
- 示例接口：
  - `POST /auth/wechat-login`
  - `GET /portal/modules?openid=...`
  - `POST /order/create`
  - `GET /order/list?openid=...`
  - `POST /order/cancel/{orderNo}?openid=...`

### 3) 管理系统前端

```bash
cd admin-frontend
npm install --cache .npm-cache
npm run dev
```

- 默认端口：`8081`
- 已代理 `/prod-api` -> `http://127.0.0.1:7266`

### 4) 服务号前端

```bash
cd app-frontend
npm install --cache .npm-cache
npm run dev
```

- 默认端口：`8082`
- 已代理 `/app-api` -> `http://127.0.0.1:7267`

## 当前实现说明

1. 当前为联调用首版，后端数据为内存 mock，已按文档结构预留角色/权限/共享子权限逻辑。
2. 共享账号遵循“部分权限白名单”模型，不是完整继承。
3. 下一阶段建议接入 `环保油数据库设计.sql`，替换 mock 为 MyBatis/JPA 持久化。
4. 若本机 npm 缓存目录有权限问题，统一使用 `npm install --cache .npm-cache`。
