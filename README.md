# 酒店辅助订购系统 · Hotel Management

[![CI](https://github.com/phr170638/HotelManegement/actions/workflows/ci.yml/badge.svg)](https://github.com/phr170638/HotelManegement/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/phr170638/HotelManegement/branch/main/graph/badge.svg)](https://codecov.io/gh/phr170638/HotelManegement)

基于 Spring Boot 3 + MyBatis-Plus + MySQL + Redis 的酒店预订后端服务。

---

## 技术栈

| 层面 | 技术 |
|------|------|
| 框架 | Spring Boot 3.3.5, MyBatis-Plus 3.5.5 |
| 安全 | Spring Security + JWT |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 7 |
| 邮件 | JavaMailSender (QQ SMTP) |
| 支付 | 支付宝沙箱 (RSA2) |
| 文档 | SpringDoc OpenAPI |
| 测试 | JUnit 5, MockMvc, Mockito |
| 覆盖率 | JaCoCo → Codecov |

---

## 快速开始

### 环境要求

- JDK 21
- MySQL 8.0
- Redis 7
- Maven 3.9+

### 启动

```bash
# 1. 创建数据库（application-dev.yml 中会自动建表）
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS hotel_db"

# 2. 配置本地密钥（邮件 + 支付宝）
cp application-local.yml.example application-local.yml
# 编辑填写真实密钥

# 3. 启动
cd hotel-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local
```

启动后访问：
- API 文档: http://localhost:8080/doc.html
- Swagger: http://localhost:8080/swagger-ui.html

### 运行测试

```bash
mvn test
```

覆盖率报告：`hotel-server/target/site/jacoco/index.html`

---

## 项目结构

```
hotel-server/src/main/java/com/hotel/
├── HotelApplication.java              # 启动类
├── common/
│   ├── config/                        # Security, JWT, MyBatis, Redis, CORS 配置
│   ├── exception/                     # BusinessException, GlobalExceptionHandler
│   ├── result/                        # R<T>, PageResult<T> 统一响应
│   └── util/                          # JwtUtil
└── module/
    ├── user/                          # 用户模块
    │   ├── controller/                # UserController, AdminController
    │   ├── dto/                       # LoginRequest, RegisterRequest, UpdateUserRequest
    │   ├── entity/                    # User, Role, Permission, UserRole, RolePermission
    │   ├── mapper/                    # UserMapper et al.
    │   ├── service/                   # UserService, UserDetailsServiceImpl
    │   └── vo/                        # LoginVO, UserVO, OrderListVO
    ├── order/                         # 订单模块
    │   ├── controller/                # OrderController
    │   ├── dto/                       # OrderCreateRequest
    │   ├── entity/                    # Order, OrderItem
    │   ├── mapper/                    # OrderMapper, OrderItemMapper
    │   ├── service/                   # OrderService
    │   └── vo/                        # OrderVO
    ├── payment/                       # 支付模块
    │   ├── config/                    # AlipayConfig
    │   ├── entity/                    # Payment
    │   ├── mapper/                    # PaymentMapper
    │   └── service/                   # AlipayService (支付宝SDK封装)
    ├── resource/                      # 资源模块（酒店、房型、城市等）
    │   ├── controller/                # HotelController, RoomController, ...
    │   ├── entity/                    # Hotel, Room, City, Country, ...
    │   ├── mapper/
    │   └── vo/                        # HotelVO, RoomVO
    ├── review/                        # 评价模块
    │   ├── controller/                # ReviewController
    │   ├── entity/                    # Review
    │   └── service/
    └── search/                        # 搜索模块
        ├── controller/                # SearchController
        └── service/
```

---

## API 总览

### 用户模块 `/api/user`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | 否 |
| POST | `/api/user/login` | 用户登录 | 否 |
| POST | `/api/user/send-code` | 发送邮箱验证码 | 否 |
| GET | `/api/user/info` | 获取当前用户信息 | 是 |
| PUT | `/api/user/update` | 更新个人信息 | 是 |
| GET | `/api/user/orders` | 我的订单列表 | 是 |
| GET | `/api/user/orders/{id}` | 订单详情 | 是 |

### 管理端 `/api/admin`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/admin/orders` | 订单列表 | ADMIN |
| PUT | `/api/admin/orders/{id}/refund` | 退款处理 | ADMIN |
| GET | `/api/admin/users` | 用户列表 | ADMIN |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用用户 | ADMIN |

### 订单模块 `/api/order`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/order/create` | 创建订单 | 是 |
| POST | `/api/order/{id}/pay` | 发起支付（返回支付宝表单）| 是 |
| PUT | `/api/order/{id}/cancel` | 取消订单 | 是 |
| POST | `/api/order/{id}/pre-cancel` | 退房申请（计算手续费）| 是 |
| POST | `/api/order/{id}/confirm-cancel` | 确认退房 | 是 |
| POST | `/api/order/pay-notify` | 支付宝异步回调 | 否 |

### 评价模块 `/api/review`

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/review/hotel/{hotelId}` | 酒店评价列表 | 否 |
| POST | `/api/review/create` | 发表评价 | 是 |
| PUT | `/api/review/{id}/reply` | 商家回复 | ADMIN |

---

## 退房手续费规则

| 距入住时间 | 手续费 |
|-----------|--------|
| 7 天以上 | 0% |
| 3~7 天 | 50% |
| 24小时~3天 | 80% |
| 24小时内 | 100%（不退款）|

---

## Codecov 覆盖率徽章

### 设置步骤

1. 去 [codecov.io](https://codecov.io) 用 GitHub 登录
2. 添加 `phr170638/HotelManegement` 仓库
3. 复制 **Repository Upload Token**
4. 在 GitHub 仓库 → Settings → Secrets and variables → Actions → 添加
   - Name: `CODECOV_TOKEN`
   - Value: 粘贴刚才复制的 token
5. 之后每次 CI 运行，覆盖率徽章就会自动更新

徽章地址（替换 `SETUP_LATER` 为你的 token 后生效）：
```
[![codecov](https://codecov.io/gh/phr170638/HotelManegement/branch/main/graph/badge.svg)](https://codecov.io/gh/phr170638/HotelManegement)
```

---

## 许可证

MIT
