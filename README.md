# 流放者柯南 RCON 管理器

一个功能完整的流放者柯南（Conan Exiles）服务器 RCON 管理工具，支持 Web 端、桌面客户端和 Android 移动端。

## 功能特性

### 核心功能
- **RCON 连接管理** - 支持标准 RCON 和无公网模式连接游戏服务器
- **命令快捷执行** - 预设命令分类，一键执行常用管理命令
- **命令分类管理** - 自定义命令分类和命令，支持礼包配置

### 玩家管理
- **在线玩家监控** - 实时查看在线玩家列表
- **玩家信息查询** - 查看玩家等级、在线时间、部落等信息
- **背包管理** - 查看和管理玩家背包物品
- **奴隶管理** - 查看玩家拥有的奴隶/随从

### 自动触发系统
- **关键词触发** - 玩家聊天包含特定关键词时触发命令
- **条件触发** - 支持等级、在线时间、金额、权限标签等条件
- **新玩家礼包** - 自动为新玩家发放礼包
- **传送点系统** - 玩家输入关键词即可传送到预设位置

### 商店系统
- **游戏内商店** - 玩家可使用游戏内货币购买物品
- **商品管理** - 支持分类、搜索、图片上传
- **金额查询** - 玩家可查询自己的余额

### 其他功能
- **QQ 机器人集成** - 通过 QQ 群管理服务器
- **多端支持** - Web、桌面客户端、Android 应用
- **SSE 实时通信** - 实时接收服务器事件

## 项目结构

```
RconClient/
├── index.html              # Web 主页面
├── styles.css              # 样式文件
├── scripts/                # 前端 JavaScript
│   ├── main.js
│   ├── auth.js
│   ├── autoTrigger.js
│   ├── playerManage.js
│   ├── playerInventory.js
│   └── ...
├── bin/                    # 后端 Python 模块
│   ├── rcon_manager.py
│   ├── user_db_manager.py
│   ├── sse_handlers.py
│   └── ...
├── 客户端/                  # 桌面客户端
│   ├── desktop_login_app.py
│   ├── sse_client.py
│   └── ...
├── android/                # Android 应用
│   └── app/src/main/
├── shop/                   # 商店系统
│   ├── index.html
│   └── query.py
├── admin/                  # 管理后台
├── Icons_PNG/              # 游戏物品图标
├── start_server.py         # 服务器启动脚本
├── start.sh                # Linux 启动脚本
└── stop.sh                 # Linux 停止脚本
```

## 环境要求

### 服务端
- Python 3.8+
- SQLite3

### 桌面客户端
- Python 3.8+
- tkinter (通常随 Python 安装)

### Android 客户端
- Android 5.0+

## 快速开始

### 1. 服务端部署

```bash
# 克隆项目
git clone https://github.com/853587221/conan_cxiles_server_plugin.git
cd conan_cxiles_server_plugin

# 安装依赖
pip install -r requirements.txt

# 启动服务器
python start_server.py
```

或使用 systemd 服务（Linux）：
```bash
# 查看状态
systemctl status rcon-client.service
# 启动服务
systemctl start rcon-client.service
# 停止服务
systemctl stop rcon-client.service
# 查看日志
journalctl -u rcon-client.service -f
```

### 2. 桌面客户端

将桌面客户端程序放到游戏服务器的 `ConanSandbox/Saved` 目录下，使用与 Web 端相同的账号登录。

桌面客户端用于：
- 接收 SSE 事件推送
- 读取游戏日志
- 执行需要本地环境的操作

### 3. 游戏服务器配置

确保游戏服务器已开启 RCON 功能，在服务器配置文件中添加：

```ini
[Rcon]
RconEnabled=1
RconPassword=your_password
RconPort=25575
```

### 4. 注册说明

系统使用邀请码注册机制，邀请码存储在 `user_database.db` 数据库的 `invite_codes` 表中。

**测试邀请码**：`TY8Y2MWL`

你可以使用此邀请码注册测试账号，或在数据库中添加新的邀请码。

## 使用说明

### 连接 RCON
1. 输入服务器 IP 地址
2. 输入 RCON 密码
3. 输入 RCON 端口（默认 25575）
4. 点击"连接"

### 自动触发命令配置
1. 点击"自动命令"按钮
2. 添加触发条件（关键词、等级、金额等）
3. 选择要执行的命令或命令分类
4. 设置执行后的操作（金额增减、标签设置等）

### 新玩家礼包配置
1. 创建一个命令分类，添加礼包物品命令
2. 在自动触发中添加条件：标签=0 且 新玩家
3. 执行后设置标签为 1，防止重复领取

### 传送点配置
1. 游戏内按 `Ctrl+Alt+Shift+L` 获取当前坐标
2. 复制坐标创建传送命令：`con 索引 TeleportPlayer x y z`
3. 添加自动触发规则，关键词如"传送沉洞"

## API 接口

### 认证
- `POST /api/login` - 登录
- `POST /api/register` - 注册
- `POST /api/logout` - 登出
- `GET /api/verify-session` - 验证会话

### RCON
- `POST /api/rcon/connect` - 连接 RCON
- `POST /api/rcon/send` - 发送命令

### 玩家管理
- `GET /api/players` - 获取玩家列表
- `GET /api/inventory/{char_name}` - 获取玩家背包
- `GET /api/thralls/{char_name}` - 获取玩家奴隶

### 自动触发
- `GET /api/auto-trigger-rules` - 获取规则列表
- `POST /api/auto-trigger-rules/create` - 创建规则
- `PUT /api/auto-trigger-rules/update` - 更新规则
- `DELETE /api/auto-trigger-rules/delete` - 删除规则

### SSE 事件
- `GET /api/events` - SSE 事件流

## 技术栈

- **前端**: HTML5, CSS3, JavaScript (原生)
- **后端**: Python, http.server, SQLite
- **实时通信**: Server-Sent Events (SSE)
- **桌面客户端**: Python, tkinter
- **移动端**: Android (Java)

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- QQ群：1067304378
- 有问题加群反馈建议和获取插件更新最新动态

## 致谢

感谢所有为本项目做出贡献的开发者和用户！
