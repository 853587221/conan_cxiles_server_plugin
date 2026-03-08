# Conan Exiles 管理员后台系统

这是一个用于管理 Conan Exiles 用户数据库和邀请码的 Web 管理后台。

## 功能特性

### 用户管理
- 查看所有用户列表
- 添加新用户（支持设置管理员权限）
- 设置/取消用户管理员权限
- 删除用户
- 查看用户详细信息（RCON 配置、注册时间、登录时间、使用的邀请码）

### 邀请码管理
- 查看所有邀请码列表
- 添加自定义邀请码
- 生成随机邀请码
- 设置邀请码使用次数限制
- 设置邀请码有效期
- 激活/停用邀请码
- 删除邀请码
- 查看邀请码使用状态

## 使用方法

### 1. 启动服务器

在命令行中运行：

```bash
python admin_server.py
```

默认端口为 8001，可以通过 `--port` 参数指定其他端口：

```bash
python admin_server.py --port 9000
```

### 2. 访问管理后台

服务器启动后会自动在浏览器中打开管理后台页面，或者手动访问：

```
http://localhost:8001
```

### 3. 登录系统

使用管理员账号登录系统。注意：只有 `is_admin` 字段为 `true` 的用户才能登录管理后台。

如果还没有管理员账号，可以使用以下方法创建：

**方法 1：使用 Python 脚本创建**

```python
from bin.edit_db import DatabaseEditor

db = DatabaseEditor()
db.create_user(
    username='admin',
    password='your_password',
    rcon_ip='127.0.0.1',
    rcon_password='rcon_password',
    rcon_port=25575,
    is_admin=True
)
```

**方法 2：直接修改数据库**

```sql
UPDATE users SET is_admin = 1 WHERE username = 'your_username';
```

## 文件结构

```
RconClient/
├── admin_server.py          # 管理后台服务器主脚本
├── admin/                   # 管理后台前端文件
│   ├── index.html          # 管理界面 HTML
│   ├── admin.css           # 样式文件
│   └── admin.js            # 交互脚本
└── bin/
    ├── edit_db.py          # 数据库编辑模块
    └── user_database.db    # 用户数据库
```

## API 接口

### 用户管理
- `GET /api/users` - 获取所有用户列表
- `POST /api/users` - 创建新用户
- `POST /api/users/update` - 更新用户信息
- `POST /api/users/delete` - 删除用户
- `POST /api/users/set-admin` - 设置/取消管理员权限

### 邀请码管理
- `GET /api/invite-codes` - 获取所有邀请码列表
- `POST /api/invite-codes` - 创建邀请码
- `POST /api/invite-codes/delete` - 删除邀请码
- `POST /api/invite-codes/activate` - 激活邀请码
- `POST /api/invite-codes/deactivate` - 停用邀请码

### 认证
- `GET /api/login` - 管理员登录

## 安全建议

1. 使用强密码作为管理员账号密码
2. 不要将管理后台暴露在公网
3. 定期备份数据库文件
4. 限制管理员账号数量

## 技术栈

- **后端**: Python + http.server
- **前端**: HTML + CSS + JavaScript
- **数据库**: SQLite
- **密码加密**: SHA256

## 注意事项

- 管理后台需要管理员权限才能登录
- 删除用户时，相关的邀请码使用记录会保留在数据库中
- 邀请码过期后会自动失效
- 邀请码使用次数达到上限后会自动失效