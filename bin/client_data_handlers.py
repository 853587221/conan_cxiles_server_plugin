import time
import re
from bin.edit_db import DatabaseEditor
from bin.user_db_manager import UserDBManager
from bin.judge import Judge
from bin.rcon_manager import RconManager
from bin.sse_handlers import (
    update_spawn_point, update_char_name_2, update_guild_id,
    get_user_spawn_point, get_user_char_name_2, get_user_guild_id,
    broadcast_event, execute_command_with_variables,
    create_command_event, create_gold_event, create_tag_event,
    set_inventory_response, set_rcon_response, set_thrall_response
)

_processed_new_players = {}
_processed_lock = None

def _get_lock():
    global _processed_lock
    if _processed_lock is None:
        import threading
        _processed_lock = threading.Lock()
    return _processed_lock


def handle_client_data(username, data):
    """处理客户端发送的数据"""
    print(f"[DEBUG] 用户 [{username}] 发送的数据: {data}")
    
    spawn_point = data.get('spawn_point')
    if spawn_point:
        update_spawn_point(username, spawn_point)
        print(f"[DEBUG] 更新用户 [{username}] 的复活点: {spawn_point}")
    
    player_info_2 = data.get('player_info_2', {})
    char_name_2 = player_info_2.get('Char_name_2')
    if char_name_2:
        update_char_name_2(username, char_name_2)
        print(f"[DEBUG] 更新用户 [{username}] 的角色名: {char_name_2}")
    
    guild_id = data.get('guild_id')
    if guild_id:
        update_guild_id(username, guild_id)
        print(f"[DEBUG] 更新用户 [{username}] 的部落ID: {guild_id}")
    
    if data.get('type') == 'new_player':
        _handle_new_player(username, data)
    else:
        if 'log_in' in data:
            _handle_player_login(username, data)
        
        if 'log_out' in data:
            _handle_player_logout(username, data)
        
        if data.get('type') == 'chat_message':
            _handle_chat_message(username, data)
        
        if data.get('type') == 'online_player':
            _handle_online_player(username, data)
    
    if data.get('type') == 'server_stats':
        _handle_server_stats(username, data)
    
    if data.get('type') == 'player_respawn':
        _handle_player_respawn(username, data)
    
    if data.get('type') == 'inventory_response':
        _handle_inventory_response(username, data)
    
    if data.get('type') == 'rcon_response':
        _handle_rcon_response(username, data)
    
    if data.get('type') == 'thrall_response':
        _handle_thrall_response(username, data)


def _handle_player_login(username, data):
    """处理玩家登录事件"""
    player_info = data.get('player_info', {})
    if player_info:
        user_db = UserDBManager(username)
        user_db.update_or_create_player(
            player_info, 
            level=data.get('level', 1),
            online_time=data.get('online_time', 0),
            spawn_point=data.get('spawn_point'),
            guild_name=data.get('guild_name')
        )
        user_db.update_player_status(
            player_info.get('User_ID', ''),
            player_info.get('Platform_ID', ''),
            'online'
        )
    
    event_data = {
        'type': 'player_join',
        'data': data,
        'timestamp': time.time()
    }
    broadcast_event(username, event_data)


def _handle_player_logout(username, data):
    """处理玩家登出事件"""
    player_info = data.get('player_info', {})
    if player_info:
        user_db = UserDBManager(username)
        user_db.update_player_status(
            player_info.get('User_ID', ''),
            player_info.get('Platform_ID', ''),
            'offline'
        )
    
    event_data = {
        'type': 'player_leave',
        'data': data,
        'timestamp': time.time()
    }
    broadcast_event(username, event_data)


def _handle_chat_message(username, data):
    """处理聊天消息"""
    player_info = data.get('player_info', {})
    if player_info:
        user_db = UserDBManager(username)
        user_db.update_or_create_player(
            player_info, 
            level=data.get('level', 1),
            online_time=data.get('online_time', 0),
            spawn_point=data.get('spawn_point'),
            guild_name=data.get('guild_name')
        )
        user_db.add_chat_message(player_info, data.get('said', ''))
    
    try:
        judge = Judge(username)
        user_db = UserDBManager(username)
        rules = user_db.get_auto_trigger_rules(enabled_only=True)
        
        for rule in rules:
            conditions = rule.get('conditions', [])
            keyword_condition = None
            other_conditions = []
            
            for cond in conditions:
                if cond.get('type') == 'keyword':
                    keyword_condition = cond
                else:
                    other_conditions.append(cond)
            
            if keyword_condition:
                if judge.judge_condition('keyword', data, keyword_condition.get('operator'), keyword_condition.get('value')):
                    if other_conditions:
                        all_other_pass = True
                        for cond in other_conditions:
                            if not judge.judge_condition(cond.get('type'), data, cond.get('operator'), cond.get('value')):
                                all_other_pass = False
                                break
                        
                        if all_other_pass:
                            if rule.get('secondary_conditions'):
                                _execute_with_secondary_conditions(username, data, rule, player_info)
                            else:
                                _execute_auto_trigger_rule(username, data, rule, player_info)
                        else:
                            print(f"[DEBUG] 关键词匹配但其他条件不满足，发送失败通知")
                            _send_fail_notification(username, data, rule, player_info)
                    else:
                        if rule.get('secondary_conditions'):
                            _execute_with_secondary_conditions(username, data, rule, player_info)
                        else:
                            _execute_auto_trigger_rule(username, data, rule, player_info)
            else:
                has_playtime_condition = any(cond.get('type') == 'playtime' for cond in conditions)
                if has_playtime_condition:
                    continue
                if judge.judge_all_conditions(data, rule['conditions']):
                    if rule.get('secondary_conditions'):
                        _execute_with_secondary_conditions(username, data, rule, player_info)
                    else:
                        _execute_auto_trigger_rule(username, data, rule, player_info)
    except Exception as e:
        print(f"[ERROR] 处理自动触发命令时出错: {e}")
        import traceback
        traceback.print_exc()
    
    try:
        user_db = UserDBManager(username)
        ai_service = user_db.get_ai_service()
        
        if ai_service and ai_service.get('enabled'):
            keywords_str = ai_service.get('keyword', '')
            if keywords_str:
                keywords = [k.strip() for k in keywords_str.split(',') if k.strip()]
                message = data.get('said', '')
                
                message_lower = message.lower()
                for keyword in keywords:
                    if message_lower.startswith(keyword.lower()):
                        query_text = message[len(keyword):].strip()
                        if query_text:
                            _handle_ai_query(username, data, ai_service, query_text, keyword, player_info)
                        break
    except Exception as e:
        print(f"[ERROR] 处理AI客服查询时出错: {e}")
        import traceback
        traceback.print_exc()
    
    event_data = {
        'type': 'chat_message',
        'data': data,
        'timestamp': time.time()
    }
    broadcast_event(username, event_data)


def _handle_online_player(username, data):
    """处理在线玩家监控"""
    player_info = data.get('player_info', {})
    if player_info:
        user_db = UserDBManager(username)
        user_db.update_or_create_player(
            player_info, 
            level=data.get('level', 1),
            online_time=data.get('online_time', 0),
            spawn_point=data.get('spawn_point'),
            guild_name=data.get('guild_name')
        )
        
        spawn_point = data.get('spawn_point')
        if spawn_point:
            update_spawn_point(username, spawn_point)
            print(f"[DEBUG] 更新用户 [{username}] 的复活点: {spawn_point}")
    
    try:
        judge = Judge(username)
        user_db = UserDBManager(username)
        rules = user_db.get_auto_trigger_rules(enabled_only=True)
        
        for rule in rules:
            if judge.judge_all_conditions(data, rule['conditions']):
                if rule.get('secondary_conditions'):
                    _execute_with_secondary_conditions(username, data, rule, player_info)
                else:
                    _execute_auto_trigger_rule(username, data, rule, player_info)
    except Exception as e:
        print(f"[ERROR] 处理自动触发命令时出错: {e}")
        import traceback
        traceback.print_exc()


def _handle_new_player(username, data):
    """处理新玩家事件"""
    player_info = data.get('player_info', {})
    char_name = player_info.get('Char_name', '')
    
    if char_name:
        lock = _get_lock()
        with lock:
            if username not in _processed_new_players:
                _processed_new_players[username] = {}
            
            now = time.time()
            last_time = _processed_new_players[username].get(char_name, 0)
            
            if now - last_time < 60:
                print(f"[DEBUG] 新玩家 {char_name} 已在60秒内处理过，跳过")
                return
            
            _processed_new_players[username][char_name] = now
    
    if player_info:
        user_db = UserDBManager(username)
        user_db.update_or_create_player(
            player_info, 
            level=data.get('level', 1),
            online_time=data.get('online_time', 0),
            spawn_point=data.get('spawn_point'),
            guild_name=data.get('guild_name')
        )
    
    try:
        judge = Judge(username)
        user_db = UserDBManager(username)
        rules = user_db.get_auto_trigger_rules(enabled_only=True)
        
        for rule in rules:
            if judge.judge_all_conditions(data, rule['conditions']):
                if rule.get('secondary_conditions'):
                    _execute_with_secondary_conditions(username, data, rule, player_info)
                else:
                    _execute_auto_trigger_rule(username, data, rule, player_info)
    except Exception as e:
        print(f"[ERROR] 处理新玩家自动触发命令时出错: {e}")
        import traceback
        traceback.print_exc()
    
    event_data = {
        'type': 'new_player',
        'data': data,
        'timestamp': time.time()
    }
    broadcast_event(username, event_data)


def _handle_server_stats(username, data):
    """处理服务器状态"""
    event_data = {
        'type': 'server_stats',
        'data': data,
        'timestamp': time.time()
    }
    broadcast_event(username, event_data)


def _handle_player_respawn(username, data):
    """处理玩家重生"""
    event_data = {
        'type': 'player_respawn',
        'data': data,
        'timestamp': time.time()
    }
    broadcast_event(username, event_data)


def _execute_with_secondary_conditions(username, data, rule, player_info):
    """执行带二级条件的自动触发规则"""
    print(f"[DEBUG] 执行带二级条件的规则: {rule.get('rule_name')}")
    
    secondary_conditions = rule.get('secondary_conditions', [])
    print(f"[DEBUG] 二级条件: {secondary_conditions}")
    
    if not secondary_conditions:
        print(f"[DEBUG] 没有二级条件，直接执行命令")
        _execute_auto_trigger_rule(username, data, rule, player_info)
        return
    
    needs_inventory = any(cond.get('type') == 'item' for cond in secondary_conditions)
    
    data_with_inventory = data.copy()
    
    if needs_inventory:
        char_name = player_info.get('Char_name')
        if not char_name:
            print(f"[ERROR] 无法获取角色名称")
            return
        
        print(f"[DEBUG] 二级条件包含物品检查，准备请求背包数据: char_name={char_name}")
        
        from bin.item_specifics import create_inventory_request_event
        from bin.sse_handlers import register_inventory_request, get_inventory_request
        
        request_id = str(int(time.time() * 1000))
        register_inventory_request(username, request_id)
        
        request_event = create_inventory_request_event(char_name)
        request_event['request_id'] = request_id
        
        print(f"[DEBUG] 广播背包请求事件: request_id={request_id}, char_name={char_name}")
        broadcast_event(username, request_event)
        
        max_wait_time = 10
        wait_interval = 0.1
        waited_time = 0
        
        inventory_data = None
        while waited_time < max_wait_time:
            request = get_inventory_request(username, request_id)
            if request and request.get('response'):
                inventory_data = request['response'].get('inventory', {})
                print(f"[DEBUG] 收到背包数据: request_id={request_id}, inventory_keys={list(inventory_data.keys()) if inventory_data else 'None'}")
                break
            
            time.sleep(wait_interval)
            waited_time += wait_interval
        
        if not inventory_data:
            print(f"[ERROR] 背包数据请求超时: request_id={request_id}, 等待时间={waited_time}秒")
            return
        
        data_with_inventory['inventory'] = inventory_data
    else:
        print(f"[DEBUG] 二级条件不需要背包数据，直接判断")
    
    judge = Judge(username)
    result = judge.judge_all_conditions(data_with_inventory, secondary_conditions)
    print(f"[DEBUG] 二级条件判断结果: {result}")
    
    if result:
        print(f"[DEBUG] 二级条件通过，执行命令")
        _execute_auto_trigger_rule(username, data, rule, player_info)
    else:
        print(f"[DEBUG] 二级条件不满足，发送失败通知")
        _send_fail_notification(username, data, rule, player_info)


def _execute_auto_trigger_rule(username, data, rule, player_info):
    """执行自动触发规则"""
    db_editor = DatabaseEditor()
    conn_result = db_editor.get_user(username)
    if not conn_result['success']:
        return
    
    user = conn_result['user']
    host = user['rcon_ip']
    password = user['rcon_password']
    port = user['rcon_port']
    rcon_mode = user.get('rcon_mode', 'direct')
    
    user_db = UserDBManager(username)
    
    if rule['execute_type'] == 'single':
        command = rule['execute_data']['command']
        idx = player_info.get('Idx', '0')
        if command.strip().lower().startswith('con '):
            command = re.sub(r'^con\s+\S+', f'con {idx}', command, flags=re.IGNORECASE)
        
        stats_variables = ['@玩家等级', '@玩家力量', '@玩家灵活', '@玩家活力', '@玩家毅力', '@玩家权威', '@玩家专长', '@玩家当前坐标']
        needs_stats = any(var in command for var in stats_variables)
        
        current_data = data.copy()
        if needs_stats:
            char_name = player_info.get('Char_name')
            if char_name:
                from bin.sse_handlers import request_player_stats
                stats_response = request_player_stats(username, char_name, timeout=5)
                
                if stats_response and stats_response.get('stats'):
                    if 'player_info' not in current_data:
                        current_data['player_info'] = player_info.copy()
                    current_data['player_info']['stats'] = stats_response['stats']
                    if stats_response.get('position'):
                        current_data['player_info']['position'] = stats_response['position']
                    print(f"[DEBUG] 已获取最新玩家属性数据: {stats_response['stats']}")
                else:
                    print(f"[WARN] 获取玩家属性数据超时，使用缓存数据")
        
        send_result = execute_command_with_variables(
            username, command, current_data, host, password, port, use_sse=(rcon_mode == 'sse')
        )
        
        if isinstance(send_result, tuple):
            send_result, final_command = send_result
        else:
            final_command = command
        
        attempts = send_result.get('attempts', 1)
        if send_result['success']:
            print(f"[DEBUG] 自动触发命令成功: {final_command}, 尝试次数: {attempts}, 结果: {send_result.get('response', '')}")
        else:
            print(f"[ERROR] 自动触发命令失败: {final_command}, 尝试次数: {attempts}, 错误: {send_result.get('message', '')}")
        
        original_command = rule['execute_data']['command']
        command_info = user_db.get_command_by_example(original_command)
        command_name = command_info['name'] if command_info else original_command
        
        char_name = player_info.get('Char_name', '未知')
        command_event = create_command_event(
            char_name, command_name, final_command,
            send_result.get('success', False),
            send_result.get('response', ''),
            rule.get('rule_name', '自动触发规则')
        )
        broadcast_event(username, command_event)
    
    elif rule['execute_type'] == 'category':
        category = rule['execute_data']['category']
        commands = user_db.get_commands_by_category(category)
        
        stats_variables = ['@玩家等级', '@玩家力量', '@玩家灵活', '@玩家活力', '@玩家毅力', '@玩家权威', '@玩家专长', '@玩家当前坐标']
        
        for cmd in commands:
            command = cmd['example']
            idx = player_info.get('Idx', '0')
            if command.strip().lower().startswith('con '):
                command = re.sub(r'^con\s+\S+', f'con {idx}', command, flags=re.IGNORECASE)
            
            needs_stats = any(var in command for var in stats_variables)
            
            current_data = data.copy()
            if needs_stats:
                char_name = player_info.get('Char_name')
                if char_name:
                    from bin.sse_handlers import request_player_stats
                    stats_response = request_player_stats(username, char_name, timeout=5)
                    
                    if stats_response and stats_response.get('stats'):
                        if 'player_info' not in current_data:
                            current_data['player_info'] = player_info.copy()
                        current_data['player_info']['stats'] = stats_response['stats']
                        if stats_response.get('position'):
                            current_data['player_info']['position'] = stats_response['position']
                        print(f"[DEBUG] 已获取最新玩家属性数据: {stats_response['stats']}")
                    else:
                        print(f"[WARN] 获取玩家属性数据超时，使用缓存数据")
            
            send_result = execute_command_with_variables(
                username, command, current_data, host, password, port, use_sse=(rcon_mode == 'sse')
            )
            
            if isinstance(send_result, tuple):
                send_result, final_command = send_result
            else:
                final_command = command
            
            attempts = send_result.get('attempts', 1)
            if send_result['success']:
                print(f"[DEBUG] 自动触发命令成功: {final_command}, 尝试次数: {attempts}, 结果: {send_result.get('response', '')}")
            else:
                print(f"[ERROR] 自动触发命令失败: {final_command}, 尝试次数: {attempts}, 错误: {send_result.get('message', '')}")
            
            char_name = player_info.get('Char_name', '未知')
            command_event = create_command_event(
                char_name, cmd['name'], final_command,
                send_result.get('success', False),
                send_result.get('response', ''),
                rule.get('rule_name', '自动触发规则')
            )
            broadcast_event(username, command_event)
            time.sleep(1)
    
    elif rule['execute_type'] == 'no_operation':
        print(f"[DEBUG] 不操作模式：跳过命令执行，直接执行后操作")
    
    print(f"[DEBUG] 规则after_execute: {rule.get('after_execute')}")
    
    after_execute = rule.get('after_execute')
    if after_execute:
        _execute_after_operations(username, data, rule, player_info, user_db)


def _execute_after_operations(username, data, rule, player_info, user_db):
    """执行后操作：金额、标签和通知"""
    after_execute = rule.get('after_execute')
    if not after_execute:
        return
    
    char_name = player_info.get('Char_name', '未知')
    
    if after_execute.get('amountOperation') and after_execute.get('amountValue'):
        operation = after_execute['amountOperation']
        amount = after_execute['amountValue']
        
        player_data = user_db.get_player_by_char_name(char_name)
        if player_data:
            current_gold = round(float(player_data.get('gold', 0)), 1)
            if operation == 'add':
                new_gold = current_gold + float(amount)
            elif operation == 'deduct':
                new_gold = current_gold - float(amount)
            elif operation == 'set':
                new_gold = float(amount)
            else:
                new_gold = current_gold
            new_gold = max(0, round(new_gold, 1))
            
            user_id = player_data.get('user_id')
            platform_id = player_data.get('platform_id')
            result = user_db.update_player_gold(user_id, platform_id, new_gold)
            print(f"[DEBUG] 金额操作: {operation} {amount}, 新金币: {new_gold}, 结果: {result}")
            
            operation_text = {'add': '增加', 'deduct': '扣除', 'set': '设置为'}.get(operation, '操作')
            gold_event = create_gold_event(
                char_name, operation, operation_text,
                amount, current_gold, new_gold,
                rule.get('rule_name', '自动触发规则')
            )
            broadcast_event(username, gold_event)
    
    if after_execute.get('setTag'):
        tag_value = after_execute['setTag']
        try:
            permission_level = int(tag_value)
            
            player_data = user_db.get_player_by_char_name(char_name)
            old_permission_level = player_data.get('permission_level', 0) if player_data else 0
            
            if player_data:
                user_id = player_data.get('user_id')
                platform_id = player_data.get('platform_id')
                result = user_db.update_player_permission_level(user_id, platform_id, permission_level)
                print(f"[DEBUG] 设置标签: {tag_value}, 结果: {result}")
                
                tag_event = create_tag_event(
                    char_name,
                    old_permission_level,
                    permission_level,
                    rule.get('rule_name', '自动触发规则')
                )
                broadcast_event(username, tag_event)
        except ValueError:
            print(f"[DEBUG] 标签值无效: {tag_value}，必须是整数")
    
    if after_execute.get('notificationMessage'):
        notification_message = after_execute['notificationMessage']
        char_name = player_info.get('Char_name', '未知')
        
        player_data = user_db.get_player_by_char_name(char_name)
        if player_data:
            updated_player_info = data.get('player_info', {}).copy()
            updated_player_info.update({
                'gold': player_data.get('gold', 0),
                'level': player_data.get('level', 1),
                'online_time': player_data.get('online_time', 0),
                'permission_level': player_data.get('permission_level', 0),
                'spawn_point': player_data.get('spawn_point', '未设置'),
                'guild_name': player_data.get('guild_name', '未加入部落'),
                'monthly_card_expiry': player_data.get('monthly_card_expiry', 0)
            })
            data['player_info'] = updated_player_info
            print(f"[DEBUG] 更新player_info数据: {updated_player_info}")
        
        db_editor = DatabaseEditor()
        conn_result = db_editor.get_user(username)
        if conn_result['success']:
            user = conn_result['user']
            host = user['rcon_ip']
            password = user['rcon_password']
            port = user['rcon_port']
            rcon_mode = user.get('rcon_mode', 'direct')
            
            # 如果char_name包含空格，使用数据库中的player_name字段
            target_name = player_data.get('player_name', char_name) if player_data and ' ' in char_name else char_name
            
            command = f'con 0 playermessage "{target_name}" "{notification_message}"'
            
            send_result = execute_command_with_variables(
                username, command, data, host, password, port, use_sse=(rcon_mode == 'sse')
            )
            
            if isinstance(send_result, tuple):
                send_result, final_command = send_result
            else:
                final_command = command
            
            if send_result['success']:
                print(f"[DEBUG] 发送通知成功: {final_command}, 结果: {send_result.get('response', '')}")
            else:
                print(f"[ERROR] 发送通知失败: {final_command}, 错误: {send_result.get('message', '')}")


def _send_fail_notification(username, data, rule, player_info):
    """发送条件不通过的通知"""
    after_execute = rule.get('after_execute')
    if not after_execute or not after_execute.get('failNotificationMessage'):
        return
    
    fail_notification_message = after_execute['failNotificationMessage']
    char_name = player_info.get('Char_name', '未知')
    
    user_db = UserDBManager(username)
    player_data = user_db.get_player_by_char_name(char_name)
    
    if player_data:
        updated_player_info = data.get('player_info', {}).copy()
        updated_player_info.update({
            'gold': player_data.get('gold', 0),
            'level': player_data.get('level', 1),
            'online_time': player_data.get('online_time', 0),
            'permission_level': player_data.get('permission_level', 0),
            'spawn_point': player_data.get('spawn_point', '未设置'),
            'guild_name': player_data.get('guild_name', '未加入部落'),
            'monthly_card_expiry': player_data.get('monthly_card_expiry', 0)
        })
        data['player_info'] = updated_player_info
        print(f"[DEBUG] 更新player_info数据: {updated_player_info}")
    
    db_editor = DatabaseEditor()
    conn_result = db_editor.get_user(username)
    if conn_result['success']:
        user = conn_result['user']
        host = user['rcon_ip']
        password = user['rcon_password']
        port = user['rcon_port']
        rcon_mode = user.get('rcon_mode', 'direct')
        
        target_name = player_data.get('player_name', char_name) if player_data and ' ' in char_name else char_name
        
        command = f'con 0 playermessage "{target_name}" "{fail_notification_message}"'
        
        send_result = execute_command_with_variables(
            username, command, data, host, password, port, use_sse=(rcon_mode == 'sse')
        )
        
        if isinstance(send_result, tuple):
            send_result, final_command = send_result
        else:
            final_command = command
        
        if send_result['success']:
            print(f"[DEBUG] 发送条件不通过通知成功: {final_command}, 结果: {send_result.get('response', '')}")
        else:
            print(f"[ERROR] 发送条件不通过通知失败: {final_command}, 错误: {send_result.get('message', '')}")


def _handle_inventory_response(username, data):
    """处理桌面客户端返回的背包数据"""
    request_id = data.get('request_id')
    char_name = data.get('char_name')
    inventory_data = data.get('inventory')
    thralls_data = data.get('thralls')
    player_info_data = data.get('player_info')
    
    if not request_id or not char_name:
        print(f"[ERROR] 背包响应缺少必要参数: request_id={request_id}, char_name={char_name}")
        return
    
    print(f"[DEBUG] 收到背包数据响应: request_id={request_id}, char_name={char_name}")
    
    response_data = {
        'char_name': char_name,
        'inventory': inventory_data,
        'timestamp': time.time()
    }
    
    if thralls_data is not None:
        response_data['thralls'] = thralls_data
        print(f"[DEBUG] 包含奴隶数据: 奴隶数量={len(thralls_data) if isinstance(thralls_data, (list, dict)) else 0}")
    
    if player_info_data is not None:
        response_data['player_info'] = player_info_data
        print(f"[DEBUG] 包含玩家信息: player_id={player_info_data.get('player_id')}, level={player_info_data.get('level')}")
    
    set_inventory_response(username, request_id, response_data)


def _handle_rcon_response(username, data):
    """处理桌面客户端返回的RCON命令响应"""
    request_id = data.get('request_id')
    command = data.get('command')
    success = data.get('success', False)
    response = data.get('response', '')
    message = data.get('message', '')
    
    if not request_id:
        print(f"[ERROR] RCON响应缺少必要参数: request_id={request_id}")
        return
    
    print(f"[DEBUG] 收到RCON命令响应: request_id={request_id}, command={command}, success={success}")
    
    set_rcon_response(username, request_id, {
        'command': command,
        'success': success,
        'response': response,
        'message': message,
        'timestamp': time.time()
    })


def _handle_thrall_response(username, data):
    """处理桌面客户端返回的奴隶数据"""
    request_id = data.get('request_id')
    char_name = data.get('char_name')
    thralls_data = data.get('thralls')
    
    if not request_id or not char_name:
        print(f"[ERROR] 奴隶响应缺少必要参数: request_id={request_id}, char_name={char_name}")
        return
    
    print(f"[DEBUG] 收到奴隶数据响应: request_id={request_id}, char_name={char_name}, thralls_count={len(thralls_data) if thralls_data else 0}")
    
    set_thrall_response(username, request_id, {
        'char_name': char_name,
        'thralls': thralls_data,
        'timestamp': time.time()
    })


def _handle_ai_query(username, data, ai_service, query_text, keyword, player_info):
    """处理AI客服查询
    
    Args:
        username: 用户名
        data: 原始消息数据
        ai_service: AI服务配置
        query_text: 提取的查询文本（去除关键词后的内容）
        keyword: 匹配的关键词
        player_info: 玩家信息
    """
    import requests
    import json
    
    url = ai_service.get('url', '')
    api_key = ai_service.get('key', '')
    model_name = ai_service.get('name', '')
    system_prompt = ai_service.get('Prompter', '')
    
    if not url or not api_key or not model_name:
        print(f"[ERROR] AI服务配置不完整: url={url}, model={model_name}")
        return
    
    char_name = player_info.get('Char_name', '未知玩家')
    
    print(f"[DEBUG] AI客服查询: 玩家={char_name}, 关键词={keyword}, 查询内容={query_text}")
    
    try:
        if re.search(r'/v\d+$', url.rstrip('/')):
            api_url = url.rstrip('/') + '/chat/completions'
        else:
            api_url = url.rstrip('/') + '/v1/chat/completions'
        
        headers = {
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {api_key}'
        }
        
        messages = []
        if system_prompt:
            messages.append({'role': 'system', 'content': system_prompt})
        messages.append({'role': 'user', 'content': query_text})
        
        payload = {
            'model': model_name,
            'messages': messages,
            'stream': False
        }
        
        response = requests.post(api_url, headers=headers, json=payload, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            message_data = result.get('choices', [{}])[0].get('message', {})
            
            reasoning_content = message_data.get('reasoning_content', '')
            if reasoning_content:
                print(f"[DEBUG] AI深度思考内容(不发送给玩家): {reasoning_content[:200]}...")
            
            ai_response = message_data.get('content', '')
            
            if ai_response:
                ai_response = re.sub(r'<think[\s\S]*?</think\s*>', '', ai_response)
                ai_response = re.sub(r'思索\n[\s\S]*?\n\n\n', '', ai_response)
                ai_response = ai_response.strip()
            
            if ai_response:
                print(f"[DEBUG] AI客服响应: {ai_response[:100]}...")
                _handle_ai_response(username, data, ai_response, player_info, ai_service)
            else:
                print(f"[ERROR] AI返回空响应")
        else:
            print(f"[ERROR] AI API请求失败: status={response.status_code}, response={response.text}")
            
    except requests.exceptions.Timeout:
        print(f"[ERROR] AI API请求超时")
    except requests.exceptions.RequestException as e:
        print(f"[ERROR] AI API请求异常: {e}")
    except Exception as e:
        print(f"[ERROR] 处理AI查询时出错: {e}")
        import traceback
        traceback.print_exc()


def _handle_ai_response(username, data, ai_response, player_info, ai_service):
    """处理AI返回的消息，通过RCON私发给玩家
    
    Args:
        username: 用户名
        data: 原始消息数据
        ai_response: AI返回的消息
        player_info: 玩家信息
        ai_service: AI服务配置
    """
    char_name = player_info.get('Char_name', '未知玩家')
    
    user_db = UserDBManager(username)
    player_data = user_db.get_player_by_char_name(char_name)
    
    if player_data:
        updated_player_info = data.get('player_info', {}).copy()
        updated_player_info.update({
            'gold': player_data.get('gold', 0),
            'level': player_data.get('level', 1),
            'online_time': player_data.get('online_time', 0),
            'permission_level': player_data.get('permission_level', 0),
            'spawn_point': player_data.get('spawn_point', '未设置'),
            'guild_name': player_data.get('guild_name', '未加入部落'),
            'monthly_card_expiry': player_data.get('monthly_card_expiry', 0)
        })
        data['player_info'] = updated_player_info
    
    db_editor = DatabaseEditor()
    conn_result = db_editor.get_user(username)
    
    if not conn_result['success']:
        print(f"[ERROR] 获取用户RCON连接信息失败")
        return
    
    user = conn_result['user']
    host = user['rcon_ip']
    password = user['rcon_password']
    port = user['rcon_port']
    rcon_mode = user.get('rcon_mode', 'direct')
    
    if player_data and ' ' in char_name:
        target_name = player_data.get('player_name', char_name)
    else:
        target_name = char_name
    
    command = f'con 0 playermessage "{target_name}" "{ai_response}"'
    
    send_result = execute_command_with_variables(
        username, command, data, host, password, port, use_sse=(rcon_mode == 'sse')
    )
    
    if isinstance(send_result, tuple):
        send_result, final_command = send_result
    else:
        final_command = command
    
    if send_result['success']:
        print(f"[DEBUG] AI客服回复发送成功: {final_command}, 结果: {send_result.get('response', '')}")
    else:
        print(f"[ERROR] AI客服回复发送失败: {final_command}, 错误: {send_result.get('message', '')}")
