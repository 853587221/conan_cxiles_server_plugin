import json
import urllib.parse
import queue
import threading
import shutil
from pathlib import Path
from bin.response_utils import send_success_response, send_error_response
from bin.user_db_manager import UserDBManager
from bin.rcon_manager import RconManager
from bin.sse_handlers import LATEST_PLAYER_JOIN, sse_clients, replace_command_variables, get_user_spawn_point, get_user_char_name_2, get_user_guild_id


def handle_version(handler, directory):
    """处理版本号API"""
    try:
        version_file = directory / 'plug-in' / 'versions.txt'
        version = '1.0.0'
        if version_file.exists():
            with open(version_file, 'r', encoding='utf-8') as f:
                version = f.read().strip()
        
        send_success_response(handler, {'version': version})
    except Exception as e:
        send_error_response(handler, f'读取版本号失败: {str(e)}', 500)


def handle_categories(handler, user_db):
    """处理获取分类API"""
    categories = user_db.get_categories()
    send_success_response(handler, {'categories': categories})


def handle_commands(handler, user_db):
    """处理获取命令API"""
    commands = user_db.get_commands()
    send_success_response(handler, {'commands': commands})


def handle_commands_by_category(handler, user_db):
    """处理按分类获取命令API"""
    category = handler.path.split('/')[-1]
    category = urllib.parse.unquote(category)
    commands = user_db.get_commands_by_category(category)
    send_success_response(handler, {'commands': commands})


def handle_search_commands(handler, user_db):
    """处理搜索命令API"""
    keyword = handler.path.split('/')[-1]
    keyword = urllib.parse.unquote(keyword)
    commands = user_db.search_commands(keyword)
    send_success_response(handler, {'commands': commands})


def handle_auto_trigger_rules(handler, user_db):
    """处理获取自动触发规则API"""
    rules = user_db.get_auto_trigger_rules()
    send_success_response(handler, {'rules': rules})


def handle_auto_trigger_rule(handler, user_db):
    """处理获取单个自动触发规则API"""
    rule_id = int(handler.path.split('/')[-1])
    rule = user_db.get_auto_trigger_rule(rule_id)
    if rule:
        send_success_response(handler, {'rule': rule})
    else:
        send_error_response(handler, '规则不存在', 404)


def handle_auto_trigger_rule_toggle(handler, user_db):
    """处理切换自动触发规则启用状态API"""
    rule_id = int(handler.path.split('/')[-2])
    result = user_db.toggle_auto_trigger_rule(rule_id)
    if result['success']:
        send_success_response(handler, {'message': result['message']})
    else:
        send_error_response(handler, result['message'])


def handle_rcon_connection_info(handler, username):
    """处理RCON连接信息API"""
    rcon_manager = RconManager()
    user_info = rcon_manager.get_connection_info(username)
    
    if user_info['success']:
        send_success_response(handler, user_info)
    else:
        send_error_response(handler, user_info.get('message', '获取失败'))


def handle_player_join(handler):
    """处理获取最新玩家加入信息API"""
    send_success_response(handler, {'data': LATEST_PLAYER_JOIN})


def handle_players(handler, user_db):
    """处理获取玩家列表API"""
    from datetime import datetime
    players = user_db.get_all_players()
    server_date = datetime.now().strftime('%Y-%m-%d')
    send_success_response(handler, {'players': players, 'server_date': server_date})


def handle_chat_messages(handler, user_db):
    """处理获取聊天消息API"""
    import urllib.parse
    
    query_params = urllib.parse.parse_qs(handler.path.split('?')[1]) if '?' in handler.path else {}
    offset = int(query_params.get('offset', [0])[0])
    limit = int(query_params.get('limit', [10])[0])
    
    print(f"[DEBUG] 获取聊天消息: offset={offset}, limit={limit}")
    
    messages = user_db.get_chat_messages(offset, limit)
    total_count = user_db.get_chat_messages_count()
    
    print(f"[DEBUG] 聊天消息总数: {total_count}, 返回消息数: {len(messages)}, has_more: {offset + limit < total_count}")
    
    send_success_response(handler, {
        'messages': messages,
        'total': total_count,
        'offset': offset,
        'limit': limit,
        'has_more': offset + limit < total_count
    })


def handle_sse_events(handler, username):
    """处理SSE事件流端点"""
    handler.send_response(200)
    handler.send_header('Content-Type', 'text/event-stream')
    handler.send_header('Cache-Control', 'no-cache')
    handler.send_header('Connection', 'keep-alive')
    handler.send_header('Access-Control-Allow-Origin', '*')
    handler.end_headers()
    
    message_queue = queue.Queue()
    
    # 确保用户名存在于sse_clients中
    if username not in sse_clients:
        sse_clients[username] = []
    sse_clients[username].append(message_queue)
    
    try:
        while True:
            try:
                message = message_queue.get(timeout=15)
                if message is None:
                    break
                try:
                    handler.wfile.write(f"data: {json.dumps(message, ensure_ascii=False)}\n\n".encode('utf-8'))
                    handler.wfile.flush()
                except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
                    break
            except queue.Empty:
                try:
                    handler.wfile.write(": keepalive\n\n".encode('utf-8'))
                    handler.wfile.flush()
                except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
                    break
    except (ConnectionResetError, BrokenPipeError, ConnectionAbortedError):
        pass
    finally:
        if username in sse_clients and message_queue in sse_clients[username]:
            sse_clients[username].remove(message_queue)
            if not sse_clients[username]:
                del sse_clients[username]


def handle_users_update(handler, db, data):
    """处理更新用户信息API"""
    original_username = data.get('original_username', '').strip()
    username = data.get('username', '').strip()
    password = data.get('password', '').strip()
    rcon_ip = data.get('rcon_ip', '').strip()
    rcon_password = data.get('rcon_password', '').strip()
    rcon_port = data.get('rcon_port', 0)
    invite_code = data.get('invite_code', '').strip() or None
    is_admin = data.get('is_admin', False)
    
    if not original_username:
        send_error_response(handler, '原始用户名不能为空')
        return
    
    if username != original_username:
        send_error_response(handler, '不允许修改用户名')
        return
    
    if password:
        result = db.update_password(original_username, password)
        if not result['success']:
            send_error_response(handler, result['message'])
            return
    
    result = db.update_user_info(
        username=original_username,
        rcon_ip=rcon_ip,
        rcon_password=rcon_password,
        rcon_port=int(rcon_port) if rcon_port else 0,
        is_admin=is_admin
    )
    
    if result['success']:
        send_success_response(handler, {'message': '用户信息更新成功'})
    else:
        send_error_response(handler, result.get('message', '更新失败'))


def handle_category_create(handler, user_db, data):
    """处理创建分类API"""
    name = data.get('name', '').strip()
    description = data.get('description', '').strip()
    
    if not name:
        send_error_response(handler, '分类名称不能为空')
        return
    
    result = user_db.create_category(name, description)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_category_update(handler, user_db, data):
    """处理更新分类API"""
    category_id = data.get('id', 0)
    name = data.get('name', '').strip()
    description = data.get('description', '').strip()
    
    if not category_id or not name:
        send_error_response(handler, '分类ID和名称不能为空')
        return
    
    result = user_db.update_category(category_id, name, description)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_category_delete(handler, user_db, data):
    """处理删除分类API"""
    category_id = data.get('id', 0)
    
    if not category_id:
        send_error_response(handler, '分类ID不能为空')
        return
    
    result = user_db.delete_category(category_id)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_command_create(handler, user_db, data):
    """处理创建命令API"""
    name = data.get('name', '').strip()
    description = data.get('description', '').strip()
    category = data.get('category', '').strip()
    example = data.get('example', '').strip()
    
    if not name or not category or not example:
        send_error_response(handler, '命令名称、分类和示例不能为空')
        return
    
    result = user_db.create_command(name, description, category, example)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_command_update(handler, user_db, data):
    """处理更新命令API"""
    command_id = data.get('id', 0)
    name = data.get('name', '').strip()
    description = data.get('description', '').strip()
    category = data.get('category', '').strip()
    example = data.get('example', '').strip()
    
    if not command_id or not name or not category or not example:
        send_error_response(handler, '命令ID、名称、分类和示例不能为空')
        return
    
    result = user_db.update_command(command_id, name, description, category, example)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_command_delete(handler, user_db, data):
    """处理删除命令API"""
    command_id = data.get('id', 0)
    
    if not command_id:
        send_error_response(handler, '命令ID不能为空')
        return
    
    result = user_db.delete_command(command_id)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_auto_trigger_rule_create(handler, user_db, data):
    """处理创建自动触发规则API"""
    rule_name = data.get('rule_name', '').strip()
    conditions = data.get('conditions', [])
    secondary_conditions = data.get('secondary_conditions')
    execute_type = data.get('execute_type', '').strip()
    execute_data = data.get('execute_data', {})
    after_execute = data.get('after_execute', {})
    enabled = data.get('enabled', True)
    
    if not rule_name or not execute_type:
        send_error_response(handler, '规则名称和执行类型不能为空')
        return
    
    result = user_db.create_auto_trigger_rule(
        rule_name, conditions,
        execute_type, execute_data, after_execute, secondary_conditions, enabled
    )
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_auto_trigger_rule_update(handler, user_db, data):
    """处理更新自动触发规则API"""
    rule_id = data.get('id', 0)
    rule_name = data.get('rule_name', '').strip()
    conditions = data.get('conditions', [])
    secondary_conditions = data.get('secondary_conditions')
    execute_type = data.get('execute_type', '').strip()
    execute_data = data.get('execute_data', {})
    after_execute = data.get('after_execute', {})
    enabled = data.get('enabled', True)
    
    if not rule_id or not rule_name or not execute_type:
        send_error_response(handler, '规则ID、名称和执行类型不能为空')
        return
    
    result = user_db.update_auto_trigger_rule(
        rule_id, rule_name, conditions,
        execute_type, execute_data, after_execute, secondary_conditions, enabled
    )
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_auto_trigger_rule_delete(handler, user_db, data):
    """处理删除自动触发规则API"""
    rule_id = data.get('id', 0)
    
    if not rule_id:
        send_error_response(handler, '规则ID不能为空')
        return
    
    result = user_db.delete_auto_trigger_rule(rule_id)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_players_update(handler, user_db, data):
    """处理更新玩家信息API"""
    player_id = data.get('id', 0)
    permission_level = data.get('permission_level', 0)
    gold = round(float(data.get('gold', 0)), 1)
    spawn_point = data.get('spawn_point')
    guild_name = data.get('guild_name')
    monthly_card_expiry = data.get('monthly_card_expiry')
    
    if not player_id:
        send_error_response(handler, '玩家ID不能为空')
        return
    
    result = user_db.update_player_fields(player_id, permission_level, gold, spawn_point, guild_name, monthly_card_expiry)
    if result['success']:
        send_success_response(handler, {'message': result['message']})
    else:
        send_error_response(handler, result['message'])


def handle_players_delete(handler, user_db, data):
    """处理删除玩家API"""
    player_id = data.get('id', 0)
    
    if not player_id:
        send_error_response(handler, '玩家ID不能为空')
        return
    
    result = user_db.delete_player(player_id)
    if result['success']:
        send_success_response(handler, {'message': result['message']})
    else:
        send_error_response(handler, result['message'])


def handle_players_reset_signin(handler, user_db, data):
    """处理重置玩家签到记录API"""
    player_id = data.get('id', 0)
    
    if not player_id:
        send_error_response(handler, '玩家ID不能为空')
        return
    
    result = user_db.update_player_fields(player_id, sign_time=0, sign_records='[]')
    if result['success']:
        send_success_response(handler, {'message': '签到记录已重置'})
    else:
        send_error_response(handler, result['message'])


def handle_rcon_connect(handler, username, data):
    """处理RCON连接API"""
    host = data.get('host', '').strip()
    password = data.get('password', '').strip()
    port = int(data.get('port', 0))
    rcon_mode = data.get('rcon_mode', 'direct').strip()
    
    if not host or not password or not port:
        send_error_response(handler, '主机、密码和端口不能为空')
        return
    
    rcon_manager = RconManager()
    result = rcon_manager.check_connection(host, password, port)
    
    if result['success']:
        rcon_manager.save_connection_info(username, host, password, port, rcon_mode)
        send_success_response(handler, {'message': '连接成功'})
    else:
        send_error_response(handler, result.get('message', '连接失败'))


def handle_rcon_send(handler, username, data):
    """处理RCON发送命令API"""
    command = data.get('command', '').strip()
    
    if not command:
        send_error_response(handler, '命令不能为空')
        return
    
    spawn_point = get_user_spawn_point(username)
    char_name_2 = get_user_char_name_2(username)
    guild_id = get_user_guild_id(username)
    
    command = replace_command_variables(command, spawn_point, char_name_2, guild_id)
    
    rcon_manager = RconManager()
    result = rcon_manager.send_command_with_saved_info(username, command)
    
    if result['success']:
        send_success_response(handler, {'response': result['response']})
    else:
        send_error_response(handler, result.get('message', '发送失败'))


def handle_rcon_send_via_sse(handler, username, data):
    """处理通过SSE发送RCON命令API"""
    from bin.sse_handlers import register_rcon_request, get_rcon_request, broadcast_event
    import uuid
    import time
    
    command = data.get('command', '').strip()
    
    if not command:
        send_error_response(handler, '命令不能为空')
        return
    
    spawn_point = get_user_spawn_point(username)
    char_name_2 = get_user_char_name_2(username)
    guild_id = get_user_guild_id(username)
    
    command = replace_command_variables(command, spawn_point, char_name_2, guild_id)
    
    request_id = str(uuid.uuid4())
    
    register_rcon_request(username, request_id)
    
    event_data = {
        'type': 'rcon_command',
        'request_id': request_id,
        'command': command,
        'timestamp': time.time()
    }
    
    broadcast_event(username, event_data)
    
    max_wait_time = 30
    check_interval = 0.5
    total_wait = 0
    
    while total_wait < max_wait_time:
        time.sleep(check_interval)
        total_wait += check_interval
        
        request = get_rcon_request(username, request_id)
        if request and request.get('response'):
            response_data = request['response']
            if response_data.get('success'):
                send_success_response(handler, {'response': response_data.get('response', '')})
            else:
                send_error_response(handler, response_data.get('message', '命令执行失败'))
            return
    
    send_error_response(handler, '请求超时，桌面客户端未响应')


def handle_save_rcon_mode(handler, username, data):
    """保存RCON模式"""
    from bin.rcon_manager import RconManager
    
    rcon_mode = data.get('rcon_mode', 'direct').strip()
    
    if rcon_mode not in ['direct', 'sse']:
        send_error_response(handler, '无效的RCON模式')
        return
    
    rcon_manager = RconManager()
    db_editor = rcon_manager.db_editor
    
    result = db_editor.update_user_info(
        username=username,
        rcon_mode=rcon_mode
    )
    
    if result['success']:
        send_success_response(handler, {'message': 'RCON模式已保存', 'rcon_mode': rcon_mode})
    else:
        send_error_response(handler, result.get('message', '保存失败'))


def handle_ai_service_get(handler, user_db):
    """处理获取AI服务配置API"""
    service = user_db.get_ai_service()
    send_success_response(handler, {'service': service})


def handle_ai_service_save(handler, user_db, data):
    """处理保存AI服务配置API"""
    url = data.get('url', '').strip()
    key = data.get('key', '').strip()
    name = data.get('name', '').strip()
    Prompter = data.get('Prompter', '').strip()
    keyword = data.get('keyword', '').strip()
    enabled = data.get('enabled', True)
    
    conflict_rule = user_db.check_keyword_conflict(keyword)
    if conflict_rule:
        send_error_response(handler, f'您的规则「{conflict_rule}」里面已经包含该关键词，请修改其它关键词避免冲突')
        return
    
    result = user_db.save_ai_service(url, key, name, Prompter, keyword, enabled)
    if result['success']:
        send_success_response(handler, {'message': result['message']})
    else:
        send_error_response(handler, result['message'])


def handle_ai_models_fetch(handler, data):
    """处理获取AI模型列表API"""
    import requests
    
    url = data.get('url', '').strip()
    api_key = data.get('key', '').strip()
    
    if not url or not api_key:
        send_error_response(handler, '模型地址和密钥不能为空')
        return
    
    try:
        import re
        if re.search(r'/v\d+$', url.rstrip('/')):
            models_url = url.rstrip('/') + '/models'
        else:
            models_url = url.rstrip('/') + '/v1/models'
        
        headers = {
            'Authorization': f'Bearer {api_key}'
        }
        
        response = requests.get(models_url, headers=headers, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            models = result.get('data', [])
            model_list = [{'id': m.get('id', ''), 'name': m.get('id', '')} for m in models]
            send_success_response(handler, {'models': model_list})
        else:
            send_error_response(handler, f'获取模型列表失败: HTTP {response.status_code}')
    except requests.exceptions.Timeout:
        send_error_response(handler, '请求超时')
    except requests.exceptions.RequestException as e:
        send_error_response(handler, f'请求失败: {str(e)}')
    except Exception as e:
        send_error_response(handler, f'获取模型列表失败: {str(e)}')


def handle_migration_verify(handler, db, username, data):
    """处理数据迁移验证API"""
    target_username = data.get('target_username', '').strip()
    target_password = data.get('target_password', '').strip()
    
    if not target_username or not target_password:
        send_error_response(handler, '目标账号和密码不能为空')
        return
    
    if target_username == username:
        send_error_response(handler, '不能迁移到当前账号')
        return
    
    from bin.edit_db import DatabaseEditor
    db_editor = DatabaseEditor()
    
    target_user = db_editor.verify_user(target_username, target_password)
    
    if target_user:
        send_success_response(handler, {
            'current_username': username,
            'target_username': target_username,
            'message': '验证成功'
        })
    else:
        send_error_response(handler, '目标账号或密码错误')


def handle_migration_execute(handler, username, data):
    """处理数据迁移执行API"""
    target_username = data.get('target_username', '').strip()
    tables = data.get('tables', [])
    
    if not target_username:
        send_error_response(handler, '目标账号不能为空')
        return
    
    if target_username == username:
        send_error_response(handler, '不能迁移到当前账号')
        return
    
    if not tables or len(tables) == 0:
        send_error_response(handler, '请至少选择一项要迁移的数据')
        return
    
    base_dir = Path(__file__).parent.parent
    source_db_path = base_dir / 'data' / username / 'database.db'
    target_db_path = base_dir / 'data' / target_username / 'database.db'
    
    if not source_db_path.exists():
        send_error_response(handler, '当前账号数据库不存在')
        return
    
    if not target_db_path.exists():
        send_error_response(handler, '目标账号数据库不存在')
        return
    
    import sqlite3
    
    valid_tables = ['ai_service', 'auto_trigger_rules', 'commands', 'categories', 
                    'players', 'product_categories', 'products', 'chat_messages']
    
    for table in tables:
        if table not in valid_tables:
            send_error_response(handler, '无效的数据项')
            return
    
    try:
        source_conn = sqlite3.connect(str(source_db_path))
        target_conn = sqlite3.connect(str(target_db_path))
        
        source_cursor = source_conn.cursor()
        target_cursor = target_conn.cursor()
        
        migrated_tables = []
        
        table_display_names = {
            'ai_service': 'AI模型数据',
            'auto_trigger_rules': '自动命令规则',
            'commands': '命令',
            'categories': '分类',
            'players': '玩家数据',
            'product_categories': '商品分类',
            'products': '商品',
            'chat_messages': '聊天记录'
        }
        
        for table in tables:
            try:
                source_cursor.execute(f"SELECT * FROM {table}")
                rows = source_cursor.fetchall()
                
                if not rows:
                    continue
                
                source_cursor.execute(f"PRAGMA table_info({table})")
                columns_info = source_cursor.fetchall()
                columns = [col[1] for col in columns_info]
                
                target_cursor.execute(f"DELETE FROM {table}")
                
                placeholders = ', '.join(['?' for _ in columns])
                column_names = ', '.join(columns)
                
                target_cursor.executemany(
                    f"INSERT INTO {table} ({column_names}) VALUES ({placeholders})",
                    rows
                )
                
                display_name = table_display_names.get(table, table)
                migrated_tables.append(display_name)
            except Exception as e:
                print(f"[ERROR] 迁移表 {table} 失败: {e}")
        
        target_conn.commit()
        source_conn.close()
        target_conn.close()
        
        migrate_images = 'product_categories' in tables or 'products' in tables
        images_migrated = False
        
        if migrate_images:
            source_images_dir = base_dir / 'data' / username / 'images'
            target_images_dir = base_dir / 'data' / target_username / 'images'
            
            if source_images_dir.exists():
                if target_images_dir.exists():
                    shutil.rmtree(str(target_images_dir))
                
                shutil.copytree(str(source_images_dir), str(target_images_dir))
                images_migrated = True
        
        if migrated_tables:
            message = f"已迁移: {', '.join(migrated_tables)}"
            if images_migrated:
                message += ", 商品图片"
        else:
            message = "没有数据需要迁移"
        
        send_success_response(handler, {'message': message})
    except Exception as e:
        send_error_response(handler, f'数据迁移失败: {str(e)}')


def handle_cleanup_execute(handler, user_db, data):
    """处理数据清理执行API"""
    items = data.get('items', [])
    
    if not items or len(items) == 0:
        send_error_response(handler, '请至少选择一项要清理的数据')
        return
    
    valid_items = ['all_players', 'gold', 'permission_level', 'spawn_point', 'month_card']
    
    for item in items:
        if item not in valid_items:
            send_error_response(handler, '无效的清理项')
            return
    
    try:
        import sqlite3
        conn = user_db._get_connection()
        cursor = conn.cursor()
        
        cleaned_items = []
        
        if 'all_players' in items:
            cursor.execute('DELETE FROM players')
            cleaned_items.append('所有玩家数据')
        else:
            if 'gold' in items:
                cursor.execute('UPDATE players SET gold = 0')
                cleaned_items.append('玩家金额')
            
            if 'permission_level' in items:
                cursor.execute('UPDATE players SET permission_level = 0')
                cleaned_items.append('玩家权限标签')
            
            if 'spawn_point' in items:
                cursor.execute('UPDATE players SET spawn_point = NULL')
                cleaned_items.append('玩家复活点')
            
            if 'month_card' in items:
                cursor.execute('UPDATE players SET month_card = 0, monthly_card_expiry = 0')
                cleaned_items.append('玩家会员')
        
        conn.commit()
        conn.close()
        
        if cleaned_items:
            message = f"已清理: {', '.join(cleaned_items)}"
        else:
            message = "没有数据需要清理"
        
        send_success_response(handler, {'message': message})
    except Exception as e:
        send_error_response(handler, f'数据清理失败: {str(e)}')


def handle_qq_bot_settings_get(handler, db_editor, username):
    """处理获取QQ机器人设置API"""
    user_result = db_editor.get_user_id(username)
    if not user_result['success']:
        send_error_response(handler, user_result.get('message', '获取用户ID失败'))
        return
    
    user_id = user_result['user_id']
    result = db_editor.get_qq_bot_settings(user_id)
    
    if result['success']:
        send_success_response(handler, {'settings': result['settings']})
    else:
        send_success_response(handler, {'settings': None})


def handle_qq_bot_settings_save(handler, db_editor, username, data):
    """处理保存QQ机器人设置API"""
    user_result = db_editor.get_user_id(username)
    if not user_result['success']:
        send_error_response(handler, user_result.get('message', '获取用户ID失败'))
        return
    
    user_id = user_result['user_id']
    
    settings = {}
    valid_keys = ['Binding_message_1', 'Binding_message_2', 'Binding_message_3', 'sign_message_1', 
                  'sign_message_2', 'sign_message_3', 'balance_message_1', 
                  'balance_message_2', 'online_players_message',
                  'sign_reset_type', 'sign_reset_hour', 'sign_interval_hours',
                  'sign_gold_type', 'sign_gold_fixed', 'sign_gold_min', 'sign_gold_max', 'sign_gold_weight']
    
    for key in valid_keys:
        if key in data:
            settings[key] = data[key]
    
    result = db_editor.save_qq_bot_settings(user_id, settings)
    
    if result['success']:
        send_success_response(handler, {'message': result['message']})
    else:
        send_error_response(handler, result.get('message', '保存失败'))
