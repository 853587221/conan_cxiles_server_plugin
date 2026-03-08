# 数据库编辑器使用说明

## 概述

`edit_db.py` 是一个专门用于修改 `user_database.db` 数据库的模块，提供了完整的用户和邀请码管理功能。

## 文件位置

- 数据库编辑器: `RconClient/bin/edit_db.py`
- 数据库文件: `RconClient/user_database.db`

## 导入方式

```python
from bin.edit_db import DatabaseEditor

# 创建编辑器实例
db = DatabaseEditor()

# 或者指定数据库路径
db = DatabaseEditor('/path/to/database.db')
```

## 用户管理功能

### 1. 创建用户

```python
result = db.create_user(
    username='testuser',
    password='password123',
    rcon_ip='127.0.0.1',
    rcon_password='rconpass',
    rcon_port=25575,
    invite_code=None  # 可选
)

# 返回值: {'success': True/False, 'message': '...', 'user_id': ...}
```

### 2. 验证用户登录

```python
user = db.verify_user('testuser', 'password123')

# 返回值: 用户元组或 None
```

### 3. 更新用户信息

```python
result = db.update_user_info(
    username='testuser',
    rcon_ip='192.168.1.100',  # 可选
    rcon_password='newpass',  # 可选
    rcon_port=25576  # 可选
)

# 返回值: {'success': True/False, 'message': '...'}
```

### 4. 更新密码

```python
result = db.update_password('testuser', 'newpassword123')

# 返回值: {'success': True/False, 'message': '...'}
```

### 5. 删除用户

```python
result = db.delete_user('testuser')

# 返回值: {'success': True/False, 'message': '...'}
```

### 6. 获取用户信息

```python
result = db.get_user('testuser')

# 返回值: {'success': True/False, 'user': {...}}
```

### 7. 获取所有用户

```python
result = db.get_all_users()

# 返回值: {'success': True, 'users': [...]}
```

## 邀请码管理功能

### 1. 创建邀请码

```python
result = db.create_invite_code(
    code='ABC12345',  # 或使用 db.generate_invite_code() 生成
    creator_id=1,     # 可选，创建者ID
    max_uses=5,       # 最大使用次数，默认1
    expire_days=7     # 有效期（天），可选
)

# 返回值: {'success': True/False, 'message': '...', 'code_id': ...}
```

### 2. 生成随机邀请码

```python
code = db.generate_invite_code(length=8)  # 默认8位

# 返回值: 随机邀请码字符串
```

### 3. 验证邀请码

```python
is_valid = db.validate_invite_code('ABC12345')

# 返回值: True/False
```

### 4. 获取邀请码信息

```python
result = db.get_invite_code('ABC12345')

# 返回值: {'success': True/False, 'invite_code': {...}}
```

### 5. 获取所有邀请码

```python
result = db.get_all_invite_codes()

# 返回值: {'success': True, 'invite_codes': [...]}
```

### 6. 停用邀请码

```python
result = db.deactivate_invite_code('ABC12345')

# 返回值: {'success': True/False, 'message': '...'}
```

### 7. 激活邀请码

```python
result = db.activate_invite_code('ABC12345')

# 返回值: {'success': True/False, 'message': '...'}
```

### 8. 删除邀请码

```python
result = db.delete_invite_code('ABC12345')

# 返回值: {'success': True/False, 'message': '...'}
```

## 统计功能

### 1. 获取用户数量

```python
result = db.get_user_count()

# 返回值: {'success': True, 'count': ...}
```

### 2. 获取邀请码数量

```python
result = db.get_invite_code_count()

# 返回值: {'success': True, 'count': ...}
```

## 完整示例

```python
from bin.edit_db import DatabaseEditor

# 创建编辑器实例
db = DatabaseEditor()

# 创建管理员用户
result = db.create_user(
    username='admin',
    password='admin123',
    rcon_ip='127.0.0.1',
    rcon_password='rconpass',
    rcon_port=25575
)

if result['success']:
    print(f"用户创建成功，ID: {result['user_id']}")
else:
    print(f"创建失败: {result['message']}")

# 创建邀请码
code = db.generate_invite_code()
result = db.create_invite_code(code, max_uses=10, expire_days=30)

if result['success']:
    print(f"邀请码创建成功: {code}")

# 使用邀请码创建用户
result = db.create_user(
    username='newuser',
    password='user123',
    rcon_ip='192.168.1.1',
    rcon_password='userpass',
    rcon_port=25576,
    invite_code=code
)

# 获取所有用户
result = db.get_all_users()
if result['success']:
    for user in result['users']:
        print(f"用户: {user['username']} (ID: {user['id']})")
```

## 注意事项

1. 所有密码都会自动使用 SHA256 加密存储
2. 所有时间都使用上海时区
3. 创建用户时如果提供邀请码，会自动验证并使用
4. 邀请码验证会检查：是否存在、是否激活、使用次数是否超限、是否过期
5. 所有操作都会返回包含 `success` 字段的字典，便于判断操作结果

## 测试

运行测试脚本：

```bash
python RconClient/bin/edit_db.py
```

查看更多使用示例：

```bash
python RconClient/example_usage.py
```
