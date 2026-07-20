# 酒店辅助订购系统 · Hotel Management

[![CI](https://github.com/phr170638/HotelManegement/actions/workflows/ci.yml/badge.svg)](https://github.com/phr170638/HotelManegement/actions/workflows/ci.yml)

基于 Spring Boot 3、MyBatis-Plus、MySQL、Redis 与 Vue 3 的酒店预订与后台管理系统。当前仓库包含住客端预订流程、后台资源管理、订单与评价模块，以及前后端基础测试与 CI 配置。

## 技术栈

### 后端

| 层面 | 技术 |
| --- | --- |
| 框架 | Spring Boot 3.3.5, MyBatis-Plus 3.5.5 |
| 安全 | Spring Security + JWT |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 接口文档 | SpringDoc OpenAPI |
| 支付 | 支付宝沙箱接入骨架（需本地补齐密钥后可用） |
| 测试 | JUnit 5 + MockMvc + Mockito |
| 覆盖率 | JaCoCo（CI 可选上传 Codecov） |

### 前端

| 层面 | 技术 |
| --- | --- |
| 框架 | Vue 3 Composition API |
| UI | Element Plus |
| 状态管理 | Pinia |
| 请求 | Axios |
| 构建 | Vite |
| 测试 | Vitest + @vue/test-utils |

## 当前实现范围

- 用户注册、登录、个人信息维护
- 基于手机号的验证码发送与注册校验
- 酒店、城市、房型、早餐、床型等资源接口
- 酒店搜索、酒店详情、住客评价
- 订单创建、订单详情、取消订单、退房申请
- 后台订单管理、用户状态管理、酒店新增/编辑/删除
- 支付宝支付发起与异步回调处理骨架

## 环境要求

- JDK 17+
- MySQL 8.0
- Redis 7
- Maven 3.9+
- Node.js 20+（前端）

## 快速开始

### 1. 创建数据库

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hotel_db"
```

### 2. 配置本地敏感信息

在 `hotel-server` 目录下复制示例文件：

```bash
cp application-local.yml.example application-local.yml
```

Windows PowerShell 也可以这样：

```powershell
Copy-Item .\application-local.yml.example .\application-local.yml
```

然后填写：

- MySQL 密码
- Redis 密码（如有）
- JWT 密钥
- 支付宝沙箱参数

### 3. 启动后端

```bash
cd hotel-server
mvn spring-boot:run
```

启动后可访问：

- Swagger UI: `http://localhost:8090/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8090/api-docs`

### 4. 启动前端

```bash
cd hotel-web
npm install
npm run dev
```

默认前端地址：

- `http://127.0.0.1:5173`

### 5. 后台入口

- 后台地址：`http://127.0.0.1:5173/admin/hotels`
- 默认管理员账号：`13800000000`
- 默认管理员密码：`admin123`
- 兼容保留管理员：`17727974960 / ycj20050908`

说明：

- 后台页面本质上仍然由前端 Vite 服务承载，不存在单独的“后台端口”
- 只要前端与后端都启动，并使用管理员账号登录，即可进入后台

## 测试

### 后端

```bash
cd hotel-server
mvn test
```

JaCoCo 报告输出位置：

- `hotel-server/target/site/jacoco/index.html`

### 前端

```bash
cd hotel-web
npm test
```

## 项目结构

```text
HotelManegement-main/
├── hotel-server/
│   ├── application-local.yml.example
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/hotel/
│       │   ├── HotelApplication.java
│       │   ├── common/
│       │   │   ├── config/          # Security、JWT、SpringDoc 等配置
│       │   │   ├── exception/       # 全局异常处理
│       │   │   ├── result/          # 统一响应体
│       │   │   └── util/            # JWT 工具等
│       │   └── module/
│       │       ├── user/            # 用户、登录、后台用户管理
│       │       ├── order/           # 订单创建、详情、取消、退房
│       │       ├── payment/         # 支付配置、支付记录、支付宝服务
│       │       ├── resource/        # 酒店、城市、房型、早餐、床型
│       │       ├── review/          # 评价与商家回复
│       │       └── search/          # 搜索、联想、附近搜索骨架
│       └── test/java/               # JUnit / MockMvc / Mockito
├── hotel-web/
│   ├── package.json
│   └── src/
│       ├── api/
│       ├── components/
│       ├── router/
│       ├── store/
│       ├── utils/
│       └── views/
└── .github/workflows/ci.yml
```

## API 总览

### 用户模块 `/api/user`

| 方法 | 路径 | 说明 | 认证 |
| --- | --- | --- | --- |
| POST | `/api/user/register` | 用户注册 | 否 |
| POST | `/api/user/login` | 用户登录 | 否 |
| POST | `/api/user/send-code` | 发送手机号验证码 | 否 |
| GET | `/api/user/info` | 获取当前用户信息 | 是 |
| PUT | `/api/user/update` | 更新个人信息 | 是 |
| GET | `/api/user/orders` | 我的订单列表 | 是 |
| GET | `/api/user/orders/{id}` | 我的订单详情 | 是 |

### 管理端 `/api/admin`

| 方法 | 路径 | 说明 | 认证 |
| --- | --- | --- | --- |
| GET | `/api/admin/orders` | 订单列表 | ADMIN |
| PUT | `/api/admin/orders/{id}/refund` | 退款处理 | ADMIN |
| GET | `/api/admin/users` | 用户列表 | ADMIN |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用用户 | ADMIN |

### 订单模块 `/api/order`

| 方法 | 路径 | 说明 | 认证 |
| --- | --- | --- | --- |
| POST | `/api/order/create` | 创建订单 | 是 |
| GET | `/api/order/{id}` | 订单详情 | 是 |
| POST | `/api/order/{id}/pay` | 发起支付，返回支付宝表单 HTML | 是 |
| PUT | `/api/order/{id}/cancel` | 取消订单 | 是 |
| POST | `/api/order/{id}/pre-cancel` | 退房申请第一步 | 是 |
| POST | `/api/order/{id}/confirm-cancel` | 确认退房 | 是 |
| POST | `/api/order/pay-notify` | 支付宝异步回调 | 否 |

### 评价模块 `/api/review`

| 方法 | 路径 | 说明 | 认证 |
| --- | --- | --- | --- |
| GET | `/api/review/hotel/{hotelId}` | 酒店评价列表 | 否 |
| POST | `/api/review/create` | 发表评价 | 是 |
| PUT | `/api/review/{id}/reply` | 商家回复 | ADMIN |

## 支付与验证码说明

- 当前注册验证码为**手机号验证码流程**，用于本地开发与接口联调
- 当前已接入支付宝沙箱调用与回调处理骨架，但必须先在 `application-local.yml` 中补齐真实沙箱密钥
- 若支付宝参数未配置，支付接口会明确返回配置未完成错误，不会伪造成功

## CI 与覆盖率

- GitHub Actions 会执行后端测试与前端构建
- 已配置 JaCoCo 生成覆盖率报告
- 如仓库中配置了 `CODECOV_TOKEN`，CI 会自动上传 Codecov

## 说明

- 本仓库不提交 `application-local.yml`、数据库密码等敏感信息
- `search/nearby`、`search/suggest` 等接口目前仍属于预留能力，返回占位结果，不应在 README 中误写为已完整上线
