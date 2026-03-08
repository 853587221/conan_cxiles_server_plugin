import threading
import json
import queue
import re
import collections
import time
from bin.judge import Judge
from bin.rcon_manager import RconManager


LATEST_PLAYER_JOIN = None

user_spawn_points = {}
spawn_point_lock = threading.RLock()

user_char_name_2 = {}
user_guild_id = {}
user_data_lock = threading.RLock()

sse_clients = collections.defaultdict(list)

inventory_requests = collections.defaultdict(dict)
inventory_request_lock = threading.RLock()

rcon_requests = collections.defaultdict(dict)
rcon_request_lock = threading.RLock()

thrall_requests = collections.defaultdict(dict)
thrall_request_lock = threading.RLock()


def evaluate_expression(value_str, base_value):
    """计算数学表达式
    
    支持格式: 值+1, 值-1, 值*2, 值/2 等
    例如: @玩家等级+1 -> 如果等级是60，返回61
    """
    if not value_str:
        return base_value
    
    try:
        import re
        match = re.match(r'^([+\-*/])(.+)$', value_str.strip())
        if match:
            operator = match.group(1)
            operand = float(match.group(2))
            if operator == '+':
                return base_value + operand
            elif operator == '-':
                return base_value - operand
            elif operator == '*':
                return base_value * operand
            elif operator == '/':
                if operand != 0:
                    return base_value / operand
                return base_value
    except Exception as e:
        print(f"[DEBUG] 计算表达式失败: {e}")
    
    return base_value


def replace_command_variables(command, spawn_point=None, char_name_2=None, guild_id=None, player_info=None):
    """替换命令中的变量
    
    支持以下变量:
    - @复活点: 玩家的床铺坐标
    - @角色名: 玩家的角色名 (Char_name_2)
    - @同部落角色名: 如果 guild_id 和 guild_id_2 一致则返回 guild_id_2，否则返回空
    - @金额: 玩家的金额
    - @等级: 玩家的游戏等级
    - @在线时间: 玩家的在线时间（分钟）
    - @权限标签: 玩家的权限标签
    - @会员剩余时间: 会员剩余天数
    - @玩家等级: 玩家的游戏等级（支持数学运算，如 @玩家等级+1）
    - @玩家力量: 玩家的力量属性（支持数学运算）
    - @玩家灵活: 玩家的灵活属性（支持数学运算）
    - @玩家活力: 玩家的活力属性（支持数学运算）
    - @玩家毅力: 玩家的毅力属性（支持数学运算）
    - @玩家权威: 玩家的权威属性（支持数学运算）
    - @玩家专长: 玩家的专长属性（支持数学运算）
    - @玩家当前坐标: 玩家的当前坐标（X Y Z格式）
    """
    if not command:
        return command
    
    result = command
    
    if spawn_point:
        result = result.replace('@复活点', str(spawn_point))
    
    if char_name_2:
        result = result.replace('@角色名', str(char_name_2))
    
    if guild_id:
        result = result.replace('@同部落角色名', str(guild_id))
    else:
        result = result.replace('@同部落角色名', '')
    
    if player_info:
        gold = player_info.get('gold', 0)
        level = player_info.get('level', 1)
        online_time = player_info.get('online_time', 0)
        permission_level = player_info.get('permission_level', 0)
        spawn_point_db = player_info.get('spawn_point', '未设置')
        char_name = player_info.get('Char_name', '')
        guild_name = player_info.get('guild_name', '未加入部落')
        monthly_card_expiry = player_info.get('monthly_card_expiry', 0)
        position = player_info.get('position', '')
        
        stats = player_info.get('stats', {})
        strength = stats.get('strength', 0) if stats else 0
        agility = stats.get('agility', 0) if stats else 0
        vitality = stats.get('vitality', 0) if stats else 0
        grit = stats.get('grit', 0) if stats else 0
        authority = stats.get('authority', 0) if stats else 0
        expertise = stats.get('expertise', 0) if stats else 0
        
        result = result.replace('@角色名', str(char_name))
        result = result.replace('@部落名', str(guild_name))
        result = result.replace('@金额', str(round(float(gold), 1)))
        result = result.replace('@等级', str(level))
        result = result.replace('@在线时间', str(online_time))
        result = result.replace('@权限标签', str(permission_level))
        result = result.replace('@复活点', str(spawn_point_db))
        result = result.replace('@玩家当前坐标', str(position) if position else '未知')
        
        import re
        
        player_level_pattern = r'@玩家等级([+\-*/]\d+(?:\.\d+)?)?'
        for match in re.finditer(player_level_pattern, result):
            full_match = match.group(0)
            operator_part = match.group(1) or ''
            calculated_value = evaluate_expression(operator_part, level)
            if calculated_value != level or operator_part:
                calculated_value = int(calculated_value) if calculated_value == int(calculated_value) else calculated_value
            result = result.replace(full_match, str(int(calculated_value) if isinstance(calculated_value, float) and calculated_value.is_integer() else calculated_value), 1)
        
        strength_pattern = r'@玩家力量([+\-*/]\d+(?:\.\d+)?)?'
        for match in re.finditer(strength_pattern, result):
            full_match = match.group(0)
            operator_part = match.group(1) or ''
            calculated_value = evaluate_expression(operator_part, strength)
            result = result.replace(full_match, str(int(calculated_value) if isinstance(calculated_value, float) and calculated_value.is_integer() else calculated_value), 1)
        
        agility_pattern = r'@玩家灵活([+\-*/]\d+(?:\.\d+)?)?'
        for match in re.finditer(agility_pattern, result):
            full_match = match.group(0)
            operator_part = match.group(1) or ''
            calculated_value = evaluate_expression(operator_part, agility)
            result = result.replace(full_match, str(int(calculated_value) if isinstance(calculated_value, float) and calculated_value.is_integer() else calculated_value), 1)
        
        vitality_pattern = r'@玩家活力([+\-*/]\d+(?:\.\d+)?)?'
        for match in re.finditer(vitality_pattern, result):
            full_match = match.group(0)
            operator_part = match.group(1) or ''
            calculated_value = evaluate_expression(operator_part, vitality)
            result = result.replace(full_match, str(int(calculated_value) if isinstance(calculated_value, float) and calculated_value.is_integer() else calculated_value), 1)
        
        grit_pattern = r'@玩家毅力([+\-*/]\d+(?:\.\d+)?)?'
        for match in re.finditer(grit_pattern, result):
            full_match = match.group(0)
            operator_part = match.group(1) or ''
            calculated_value = evaluate_expression(operator_part, grit)
            result = result.replace(full_match, str(int(calculated_value) if isinstance(calculated_value, float) and calculated_value.is_integer() else calculated_value), 1)
        
        authority_pattern = r'@玩家权威([+\-*/]\d+(?:\.\d+)?)?'
        for match in re.finditer(authority_pattern, result):
            full_match = match.group(0)
            operator_part = match.group(1) or ''
            calculated_value = evaluate_expression(operator_part, authority)
            result = result.replace(full_match, str(int(calculated_value) if isinstance(calculated_value, float) and calculated_value.is_integer() else calculated_value), 1)
        
        expertise_pattern = r'@玩家专长([+\-*/]\d+(?:\.\d+)?)?'
        for match in re.finditer(expertise_pattern, result):
            full_match = match.group(0)
            operator_part = match.group(1) or ''
            calculated_value = evaluate_expression(operator_part, expertise)
            result = result.replace(full_match, str(int(calculated_value) if isinstance(calculated_value, float) and calculated_value.is_integer() else calculated_value), 1)
        
        if monthly_card_expiry and monthly_card_expiry > 0:
            now = time.time()
            if monthly_card_expiry > now:
                remaining_days = int((monthly_card_expiry - now) / 86400) + 1
                vip_remaining = str(remaining_days)
            else:
                vip_remaining = "0"
        else:
            vip_remaining = "0"
        result = result.replace('@会员剩余时间', vip_remaining)
    
    return result


def update_spawn_point(username, spawn_point):
    """更新用户的spawn_point"""
    with spawn_point_lock:
        user_spawn_points[username] = spawn_point


def update_char_name_2(username, char_name_2):
    """更新用户的char_name_2"""
    with user_data_lock:
        user_char_name_2[username] = char_name_2


def update_guild_id(username, guild_id):
    """更新用户的guild_id"""
    with user_data_lock:
        user_guild_id[username] = guild_id


def get_user_spawn_point(username):
    """获取用户的spawn_point"""
    with spawn_point_lock:
        return user_spawn_points.get(username)


def get_user_char_name_2(username):
    """获取用户的char_name_2"""
    with user_data_lock:
        return user_char_name_2.get(username)


def get_user_guild_id(username):
    """获取用户的guild_id"""
    with user_data_lock:
        return user_guild_id.get(username)


def add_sse_client(username, message_queue):
    """添加SSE客户端"""
    sse_clients[username].append(message_queue)


def remove_sse_client(username, message_queue):
    """移除SSE客户端"""
    if username in sse_clients and message_queue in sse_clients[username]:
        sse_clients[username].remove(message_queue)
        if not sse_clients[username]:
            del sse_clients[username]


def broadcast_event(username, event):
    """向指定用户的所有SSE客户端广播事件"""
    print(f"[SSE Broadcast] 用户: {username}, 事件类型: {event.get('type')}, 客户端数: {len(sse_clients.get(username, []))}")
    if username in sse_clients:
        for idx, client_queue in enumerate(sse_clients[username]):
            try:
                client_queue.put_nowait(event)
                print(f"[SSE Broadcast] 事件已发送到客户端 {idx}")
            except Exception as e:
                print(f"[SSE Broadcast] 发送到客户端 {idx} 失败: {e}")
    else:
        print(f"[SSE Broadcast] 用户 {username} 没有SSE客户端连接")


def execute_command_with_variables(username, command, data, host, password, port, use_sse=False):
    """执行命令并替换变量"""
    spawn_point = get_user_spawn_point(username)
    char_name_2 = get_user_char_name_2(username)
    guild_id = get_user_guild_id(username)
    
    same_guild_char_name = None
    guild_id_2 = data.get('guild_id_2')
    if guild_id and guild_id_2:
        judge_guild = Judge(username)
        same_guild_char_name = judge_guild.judge_guild_id_match(data)
    
    player_info = data.get('player_info', {})
    idx = player_info.get('Idx', '0')
    if command.strip().lower().startswith('con ') and 'playermessage' not in command.lower():
        command = re.sub(r'^con\s+\S+', f'con {idx}', command, flags=re.IGNORECASE)
    
    user_id = player_info.get('User_ID')
    platform_id = player_info.get('Platform_ID')
    if user_id or platform_id:
        from bin.user_db_manager import UserDBManager
        user_db = UserDBManager(username)
        db_player = user_db.get_player_by_ids(user_id, platform_id)
        if db_player:
            player_info['monthly_card_expiry'] = db_player.get('monthly_card_expiry', 0)
    
    command = replace_command_variables(command, spawn_point, char_name_2, same_guild_char_name, player_info)
    
    if use_sse:
        import uuid
        import time
        from bin.sse_handlers import register_rcon_request, get_rcon_request, broadcast_event
        
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
                return {
                    'success': response_data.get('success', False),
                    'message': response_data.get('message', ''),
                    'response': response_data.get('response', ''),
                    'attempts': 1
                }
        
        return {
            'success': False,
            'message': '请求超时，桌面客户端未响应',
            'response': '',
            'attempts': 1
        }
    else:
        rcon_manager = RconManager()
        send_result = rcon_manager.send_command(host, password, port, command)
        
        return send_result, command


def create_command_event(player_name, command_name, command, success, response, rule_name):
    """创建命令执行事件"""
    return {
        'type': 'command_executed',
        'data': {
            'player_name': player_name,
            'command_name': command_name,
            'command': command,
            'success': success,
            'response': response,
            'rule_name': rule_name
        },
        'timestamp': __import__('time').time()
    }


def create_gold_event(player_name, operation, operation_text, amount, old_gold, new_gold, rule_name):
    """创建金币变更事件"""
    return {
        'type': 'gold_changed',
        'data': {
            'player_name': player_name,
            'operation': operation,
            'operation_text': operation_text,
            'amount': amount,
            'old_gold': old_gold,
            'new_gold': new_gold,
            'rule_name': rule_name
        },
        'timestamp': __import__('time').time()
    }


def create_tag_event(player_name, old_tag, new_tag, rule_name):
    """创建标签变更事件"""
    return {
        'type': 'tag_changed',
        'data': {
            'player_name': player_name,
            'old_tag': old_tag,
            'new_tag': new_tag,
            'rule_name': rule_name
        },
        'timestamp': __import__('time').time()
    }


def create_player_event(data):
    """创建玩家事件"""
    return {
        'type': 'new_player',
        'data': data,
        'timestamp': __import__('time').time()
    }


def register_inventory_request(username, request_id):
    """注册背包数据请求，等待桌面客户端响应"""
    with inventory_request_lock:
        inventory_requests[username][request_id] = {
            'timestamp': __import__('time').time(),
            'response': None
        }


def get_inventory_request(username, request_id):
    """获取背包数据请求状态"""
    with inventory_request_lock:
        return inventory_requests.get(username, {}).get(request_id)


def set_inventory_response(username, request_id, response_data):
    """设置背包数据响应"""
    with inventory_request_lock:
        if username in inventory_requests and request_id in inventory_requests[username]:
            inventory_requests[username][request_id]['response'] = response_data


def cleanup_old_requests():
    """清理过期的请求（超过30秒的请求）"""
    current_time = __import__('time').time()
    with inventory_request_lock:
        for username in list(inventory_requests.keys()):
            for request_id in list(inventory_requests[username].keys()):
                if current_time - inventory_requests[username][request_id]['timestamp'] > 30:
                    del inventory_requests[username][request_id]
            if not inventory_requests[username]:
                del inventory_requests[username]
    
    with rcon_request_lock:
        for username in list(rcon_requests.keys()):
            for request_id in list(rcon_requests[username].keys()):
                if current_time - rcon_requests[username][request_id]['timestamp'] > 30:
                    del rcon_requests[username][request_id]
            if not rcon_requests[username]:
                del rcon_requests[username]
    
    with thrall_request_lock:
        for username in list(thrall_requests.keys()):
            for request_id in list(thrall_requests[username].keys()):
                if current_time - thrall_requests[username][request_id]['timestamp'] > 30:
                    del thrall_requests[username][request_id]
            if not thrall_requests[username]:
                del thrall_requests[username]


def register_rcon_request(username, request_id):
    """注册RCON命令请求，等待桌面客户端响应"""
    with rcon_request_lock:
        rcon_requests[username][request_id] = {
            'timestamp': __import__('time').time(),
            'response': None
        }


def get_rcon_request(username, request_id):
    """获取RCON命令请求状态"""
    with rcon_request_lock:
        return rcon_requests.get(username, {}).get(request_id)


def set_rcon_response(username, request_id, response_data):
    """设置RCON命令响应"""
    with rcon_request_lock:
        if username in rcon_requests and request_id in rcon_requests[username]:
            rcon_requests[username][request_id]['response'] = response_data


def register_thrall_request(username, request_id):
    """注册奴隶数据请求，等待桌面客户端响应"""
    with thrall_request_lock:
        thrall_requests[username][request_id] = {
            'timestamp': __import__('time').time(),
            'response': None
        }


def get_thrall_request(username, request_id):
    """获取奴隶数据请求状态"""
    with thrall_request_lock:
        return thrall_requests.get(username, {}).get(request_id)


def set_thrall_response(username, request_id, response_data):
    """设置奴隶数据响应"""
    with thrall_request_lock:
        if username in thrall_requests and request_id in thrall_requests[username]:
            thrall_requests[username][request_id]['response'] = response_data


def request_player_stats(username, char_name, timeout=10):
    """请求玩家属性数据并等待响应
    
    使用现有的 inventory_request 机制获取玩家属性
    
    Args:
        username: 用户名
        char_name: 角色名
        timeout: 超时时间（秒）
    
    Returns:
        dict: 玩家属性数据，包含 stats 和 position 字段，如果超时则返回 None
    """
    from bin.item_specifics import create_inventory_request_event
    
    request_id = str(int(time.time() * 1000))
    
    register_inventory_request(username, request_id)
    
    request_event = create_inventory_request_event(char_name)
    request_event['request_id'] = request_id
    
    broadcast_event(username, request_event)
    
    max_wait_time = timeout
    wait_interval = 0.1
    waited_time = 0
    
    while waited_time < max_wait_time:
        request = get_inventory_request(username, request_id)
        if request and request.get('response'):
            response = request['response']
            player_info = response.get('player_info', {})
            stats = player_info.get('stats', {})
            position = player_info.get('position', '')
            if stats:
                return {'stats': stats, 'position': position}
            return None
        
        time.sleep(wait_interval)
        waited_time += wait_interval
    
    return None
