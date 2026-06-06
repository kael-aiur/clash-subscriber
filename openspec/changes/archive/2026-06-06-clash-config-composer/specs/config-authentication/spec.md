## ADDED Requirements

### Requirement: Basic Auth 认证

系统 SHALL 支持 Basic Auth 认证方式保护配置访问。

#### Scenario: 认证成功

- **WHEN** 客户端请求配置，携带正确的 Basic Auth 头（用户名和密码匹配配置组合的认证信息）
- **THEN** 系统返回配置内容

#### Scenario: 认证失败 - 用户名密码错误

- **WHEN** 客户端请求配置，携带的 Basic Auth 头用户名或密码错误
- **THEN** 系统返回 401 Unauthorized 错误

#### Scenario: 认证失败 - 缺少认证头

- **WHEN** 客户端请求配置，未携带 Authorization 头
- **THEN** 系统返回 401 Unauthorized 错误

---

### Requirement: 配置访问 URL

系统 SHALL 提供基于配置名称的 URL 路径访问配置。

#### Scenario: 通过名称访问配置

- **WHEN** 客户端请求 GET /api/config/{name}
- **THEN** 系统根据名称查找配置组合，验证认证，返回配置内容

#### Scenario: 配置不存在

- **WHEN** 客户端请求的配置名称不存在
- **THEN** 系统返回 404 Not Found 错误

---

### Requirement: 认证信息存储

系统 SHALL 在配置组合中存储认证信息（用户名和密码）。

#### Scenario: 存储认证信息

- **WHEN** 用户创建或编辑配置组合，设置认证信息
- **THEN** 系统将认证信息存储在配置组合中

#### Scenario: 认证信息加密存储

- **WHEN** 系统存储认证信息
- **THEN** 系统对密码进行加密存储（建议）

---

### Requirement: Clash 客户端兼容

系统 SHALL 确保 Basic Auth 认证方式与 Clash Verge 等客户端兼容。

#### Scenario: Clash Verge 访问

- **WHEN** Clash Verge 客户端请求配置，使用 Basic Auth 认证
- **THEN** 系统正确返回配置内容，客户端可正常使用

#### Scenario: 其他 Clash 客户端访问

- **WHEN** 其他支持 Basic Auth 的 Clash 客户端请求配置
- **THEN** 系统正确返回配置内容，客户端可正常使用
