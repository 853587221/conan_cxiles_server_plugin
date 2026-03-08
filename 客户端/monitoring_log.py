import re
import time
import os
import threading
from get_player_info import get_online_players_info, activate_thrall_spawn
from get_db_info import query_player_info_by_field, get_player_spawn_points

class MonitoringLog:
    def __init__(self, data_callback=None):
        self.data_callback = data_callback
        self.monitoring_running = False
        self.player_online_time = {}
        self.tracking_players = {}
        self.player_info_cache = {}
        self.thrall_spawn_needed = False
        self.processed_new_players = set()
        self.lock = threading.Lock()
    
    def set_data_callback(self, callback):
        self.data_callback = callback
    
    def send_data(self, data):
        if self.data_callback:
            self.data_callback(data)
    
    def log_message(self, message):
        if self.data_callback:
            self.data_callback({"type": "log", "message": message})
    
    def record_player_online_time(self, player_info):
        char_name = player_info["Char_name"]
        db_result = query_player_info_by_field(field="char_name", value=char_name)
        
        level = None
        if db_result["success"] and db_result["data"]:
            level = db_result["data"][0].get("level")
        
        spawn_result = get_player_spawn_points(char_name=char_name)
        spawn_info = None
        guild_id = None
        guild_name = None
        if spawn_result["success"] and spawn_result["data"]:
            spawn = spawn_result["data"][0]
            spawn_info = f"{spawn['x']:.2f}, {spawn['y']:.2f}, {spawn['z']:.2f}"
            guild_id = spawn.get('guild_id')
            guild_name = spawn.get('guild_name')
        
        player_name = player_info["Player_name"]
        current_time = time.time()
        
        if player_name not in self.player_online_time:
            self.player_online_time[player_name] = current_time
        
        online_minutes = int((current_time - self.player_online_time[player_name]) / 60)
        
        data = {
            "type": "online_player",
            "player_info": player_info,
            "level": level,
            "spawn_point": spawn_info,
            "guild_id": guild_id,
            "guild_name": guild_name,
            "online_time": online_minutes
        }
        self.send_data(data)
    
    def track_new_player(self, player_name, platform_id):
        max_attempts = 1200
        attempt = 0
        
        while attempt < max_attempts and self.monitoring_running:
            attempt += 1
            time.sleep(3)
            
            players_info = get_online_players_info()
            
            if not players_info["success"]:
                continue
            
            player_info = None
            for player in players_info["players"]:
                if player["Player_name"] == player_name or player["Platform_ID"] == platform_id:
                    player_info = player
                    break
            
            if not player_info:
                self.log_message(f"\n玩家 {player_name} 在角色创建前退出游戏，停止追踪\n")
                if player_name in self.tracking_players:
                    del self.tracking_players[player_name]
                return
            
            if player_info["Char_name"]:
                char_name = player_info["Char_name"]
                
                with self.lock:
                    if player_name in self.processed_new_players:
                        if player_name in self.tracking_players:
                            del self.tracking_players[player_name]
                        return
                    
                    self.processed_new_players.add(player_name)
                    if player_name in self.tracking_players:
                        del self.tracking_players[player_name]
                
                self.player_info_cache[player_name] = player_info.copy()
                
                db_result = query_player_info_by_field(field="char_name", value=char_name)
                
                level = None
                if db_result["success"] and db_result["data"]:
                    level = db_result["data"][0].get("level")
                
                spawn_result = get_player_spawn_points(char_name=char_name)
                spawn_info = None
                guild_id = None
                guild_name = None
                if spawn_result["success"] and spawn_result["data"]:
                    spawn = spawn_result["data"][0]
                    spawn_info = f"{spawn['x']:.2f}, {spawn['y']:.2f}, {spawn['z']:.2f}"
                    guild_id = spawn.get('guild_id')
                    guild_name = spawn.get('guild_name')
                
                output = f"\n{'='*60}\n"
                output += f"新玩家角色创建完成:\n"
                output += f"{'='*60}\n"
                for key, value in player_info.items():
                    output += f"{key}: {value}\n"
                if level is not None:
                    output += f"\"level\": {level}\n"
                if spawn_info is not None:
                    output += f"\"spawn_point\": {spawn_info}\n"
                if guild_id is not None:
                    output += f"\"guild_id\": {guild_id}\n"
                if guild_name is not None:
                    output += f"\"guild_name\": {guild_name}\n"
                output += f"\"log_in\": \"{char_name}\"\n"
                output += f"\"new_player\": \"yes\"\n"
                output += f"{'='*60}\n"
                
                self.log_message(output)
                
                data = {
                    "type": "new_player",
                    "player_info": player_info,
                    "level": level,
                    "spawn_point": spawn_info,
                    "guild_id": guild_id,
                    "guild_name": guild_name,
                    "log_in": char_name,
                    "new_player": "yes"
                }
                self.send_data(data)
                return
    
    def record_online_players(self):
        while self.monitoring_running:
            players_info = get_online_players_info()
            
            if not players_info["success"]:
                time.sleep(60)
                continue
            
            for player in players_info["players"]:
                if not player["Char_name"]:
                    continue
                
                self.record_player_online_time(player)
            
            online_player_names = {p["Player_name"] for p in players_info["players"] if p["Char_name"]}
            for player_name in list(self.player_online_time.keys()):
                if player_name not in online_player_names:
                    del self.player_online_time[player_name]
            
            time.sleep(60)
    
    def monitor_chat_messages(self, log_file_path="Logs\\ConanSandbox.log"):
        chat_pattern = re.compile(
            r'\[.*?\]\[.*?\]ChatWindow: Character .*? \(uid \d+, player (\d+)\) said: (.*)'
        )
        
        chat_pattern_pippi = re.compile(
            r'\[.*?\]\[.*?\]ChatWindow: Character (.+?) said: (.*)'
        )
        
        join_pattern = re.compile(
            r'\[.*?\]\[.*?\]LogNet: Join succeeded: (.+)'
        )
        
        disconnect_pattern = re.compile(
            r'\[.*?\]\[.*?\]LogNet: Player disconnected: (.+)'
        )
        
        respawn_pattern = re.compile(
            r'\[.*?\]\[.*?\]ConanSandbox:Display: Character ID (\d+) has name (.+) and guild ID (\d+)\.'
        )
        
        server_startup_pattern = re.compile(
            r'LogServerStats: Startup report\. StartupTime=(\d+)'
        )
        
        f = None
        
        if not os.path.exists(log_file_path):
            self.log_message(f"等待日志文件生成... (日志文件: {log_file_path})")
        
        while self.monitoring_running and not os.path.exists(log_file_path):
            time.sleep(1)
        
        if not self.monitoring_running:
            return
        
        f = open(log_file_path, 'r', encoding='utf-8', errors='replace')
        f.seek(0, 2)
        
        self.log_message(f"等待日志文件稳定... (日志文件: {log_file_path})")
        time.sleep(3)
        
        f.seek(0, 2)
        
        self.log_message(f"开始监听聊天信息... (日志文件: {log_file_path})")
        self.log_message("按停止监控按钮停止监听\n")
        
        record_thread = threading.Thread(target=self.record_online_players)
        record_thread.daemon = True
        record_thread.start()
        
        self.log_message(f"开始监控日志文件... (日志文件: {log_file_path})\n")
        
        last_line_count = 0
        last_check_time = time.time()
        last_change_time = time.time()
        
        try:
            while self.monitoring_running:
                current_time = time.time()
                
                if current_time - last_check_time >= 5:
                    last_check_time = current_time
                    
                    f_check = open(log_file_path, 'r', encoding='utf-8', errors='replace')
                    current_line_count = len(f_check.readlines())
                    f_check.close()
                    
                    if current_line_count < last_line_count:
                        self.log_message(f"\n检测到日志文件已重置（行数从 {last_line_count} 减少到 {current_line_count}）\n")
                        self.log_message(f"重新开始监控日志...\n")
                        
                        if f:
                            f.close()
                        f = open(log_file_path, 'r', encoding='utf-8', errors='replace')
                        f.seek(0, 2)
                        self.player_online_time = {}
                        self.player_info_cache = {}
                        self.processed_new_players = set()
                        self.tracking_players = {}
                        last_line_count = current_line_count
                        last_change_time = current_time
                    elif current_line_count > last_line_count:
                        last_line_count = current_line_count
                        last_change_time = current_time
                    elif last_line_count == 0:
                        last_line_count = current_line_count
                        last_change_time = current_time
                        if f:
                            f.close()
                        f = open(log_file_path, 'r', encoding='utf-8', errors='replace')
                        f.seek(0, 2)
                    elif current_time - last_change_time >= 120:
                        self.log_message(f"\n检测到日志文件超过2分钟无变化（当前行数: {current_line_count}）\n")
                        self.log_message(f"重新开始监控日志...\n")
                        
                        if f:
                            f.close()
                        f = open(log_file_path, 'r', encoding='utf-8', errors='replace')
                        f.seek(0, 2)
                        self.player_online_time = {}
                        self.player_info_cache = {}
                        self.processed_new_players = set()
                        self.tracking_players = {}
                        last_change_time = current_time
                
                content = f.read()
                
                if content:
                    # 批量匹配所有事件
                    lines = content.splitlines()
                    
                    # 1. 检查服务器启动
                    for line in lines:
                        server_startup_match = server_startup_pattern.search(line)
                        if server_startup_match:
                            startup_time = server_startup_match.group(1)
                            
                            output = f"\n{'='*60}\n"
                            output += f"服务器启动成功！\n"
                            output += f"{'='*60}\n"
                            output += f"启动时间: {startup_time} 秒\n"
                            output += f"{'='*60}\n"
                            
                            self.log_message(output)
                            
                            data = {
                                "type": "server_startup",
                                "startup_time": startup_time
                            }
                            self.send_data(data)
                            
                            self.thrall_spawn_needed = True
                            break
                    
                    # 2. 批量匹配所有玩家加入
                    join_matches = []
                    for line in lines:
                        join_match = join_pattern.search(line)
                        if join_match:
                            join_matches.append(join_match.group(1))
                    
                    if join_matches:
                        join_matches = list(set(join_matches))
                        
                        players_info = get_online_players_info()
                        if players_info["success"]:
                            for player_name in join_matches:
                                player_info = None
                                for player in players_info["players"]:
                                    if player["Player_name"] == player_name:
                                        player_info = player
                                        break
                                
                                if player_info:
                                    should_send_join = False
                                    
                                    with self.lock:
                                        if player_name in self.tracking_players or player_name in self.processed_new_players:
                                            continue
                                        
                                        if not player_info["Char_name"]:
                                            self.tracking_players[player_name] = True
                                            self.log_message(f"\n检测到新玩家 {player_name} 正在创建角色，开始追踪...\n")
                                            thread = threading.Thread(
                                                target=self.track_new_player,
                                                args=(player_name, player_info["Platform_ID"])
                                            )
                                            thread.daemon = True
                                            thread.start()
                                        else:
                                            self.processed_new_players.add(player_name)
                                            self.record_player_online_time(player_info)
                                            self.player_info_cache[player_name] = player_info.copy()
                                            should_send_join = True
                                    
                                    if should_send_join:
                                        char_name = player_info["Char_name"]
                                        
                                        db_result = query_player_info_by_field(field="char_name", value=char_name)
                                        
                                        level = None
                                        if db_result["success"] and db_result["data"]:
                                            level = db_result["data"][0].get("level")
                                        
                                        spawn_result = get_player_spawn_points(char_name=char_name)
                                        spawn_info = None
                                        guild_id = None
                                        guild_name = None
                                        if spawn_result["success"] and spawn_result["data"]:
                                            spawn = spawn_result["data"][0]
                                            spawn_info = f"{spawn['x']:.2f}, {spawn['y']:.2f}, {spawn['z']:.2f}"
                                            guild_id = spawn.get('guild_id')
                                            guild_name = spawn.get('guild_name')
                                        
                                        output = f"\n{'='*60}\n"
                                        output += f"检测到玩家加入游戏:\n"
                                        output += f"{'='*60}\n"
                                        for key, value in player_info.items():
                                            output += f"{key}: {value}\n"
                                        if level is not None:
                                            output += f"\"level\": {level}\n"
                                        if spawn_info is not None:
                                            output += f"\"spawn_point\": {spawn_info}\n"
                                        if guild_id is not None:
                                            output += f"\"guild_id\": {guild_id}\n"
                                        if guild_name is not None:
                                            output += f"\"guild_name\": {guild_name}\n"
                                        output += f"\"log_in\": \"{char_name}\"\n"
                                        output += f"{'='*60}\n"
                                        
                                        self.log_message(output)
                                        
                                        data = {
                                            "type": "player_join",
                                            "player_info": player_info,
                                            "level": level,
                                            "spawn_point": spawn_info,
                                            "guild_id": guild_id,
                                            "guild_name": guild_name,
                                            "log_in": char_name
                                        }
                                        self.send_data(data)
                                    
                                    if self.thrall_spawn_needed:
                                        self.log_message("\n正在激活奴隶生成功能...\n")
                                        result = activate_thrall_spawn()
                                        if result["success"]:
                                            self.log_message(f"奴隶生成功能激活成功！\n")
                                            self.thrall_spawn_needed = False
                                        else:
                                            self.log_message(f"奴隶生成功能激活失败: {result['message']}\n")
                            
                            if self.thrall_spawn_needed:
                                self.log_message("\n正在激活奴隶生成功能...\n")
                                result = activate_thrall_spawn()
                                if result["success"]:
                                    self.log_message(f"奴隶生成功能激活成功！\n")
                                    self.thrall_spawn_needed = False
                                else:
                                    self.log_message(f"奴隶生成功能激活失败: {result['message']}\n")
                    
                    # 3. 批量匹配所有玩家退出
                    disconnect_matches = []
                    for line in lines:
                        disconnect_match = disconnect_pattern.search(line)
                        if disconnect_match:
                            disconnect_matches.append(disconnect_match.group(1))
                    
                    if disconnect_matches:
                        for player_name in disconnect_matches:
                            player_info = None
                            
                            if player_name in self.player_info_cache:
                                player_info = self.player_info_cache[player_name]
                                self.log_message(f"\n从缓存中获取玩家 {player_name} 的信息")
                            else:
                                self.log_message(f"\n缓存中未找到玩家 {player_name} 的信息")
                                continue
                            
                            if player_info and player_info["Char_name"]:
                                char_name = player_info["Char_name"]
                                db_result = query_player_info_by_field(field="char_name", value=char_name)
                                
                                level = None
                                if db_result["success"] and db_result["data"]:
                                    level = db_result["data"][0].get("level")
                                
                                spawn_result = get_player_spawn_points(char_name=char_name)
                                spawn_info = None
                                guild_id = None
                                guild_name = None
                                if spawn_result["success"] and spawn_result["data"]:
                                    spawn = spawn_result["data"][0]
                                    spawn_info = f"{spawn['x']:.2f}, {spawn['y']:.2f}, {spawn['z']:.2f}"
                                    guild_id = spawn.get('guild_id')
                                    guild_name = spawn.get('guild_name')
                                
                                output = f"\n{'='*60}\n"
                                output += f"检测到玩家退出游戏:\n"
                                output += f"{'='*60}\n"
                                for key, value in player_info.items():
                                    output += f"{key}: {value}\n"
                                if level is not None:
                                    output += f"\"level\": {level}\n"
                                if spawn_info is not None:
                                    output += f"\"spawn_point\": {spawn_info}\n"
                                if guild_id is not None:
                                    output += f"\"guild_id\": {guild_id}\n"
                                if guild_name is not None:
                                    output += f"\"guild_name\": {guild_name}\n"
                                output += f"\"log_out\": \"{char_name}\"\n"
                                output += f"{'='*60}\n"
                                
                                self.log_message(output)
                                
                                data = {
                                    "type": "player_disconnect",
                                    "player_info": player_info,
                                    "level": level,
                                    "spawn_point": spawn_info,
                                    "guild_id": guild_id,
                                    "guild_name": guild_name,
                                    "log_out": char_name
                                }
                                self.send_data(data)
                                
                                if player_name in self.player_online_time:
                                    del self.player_online_time[player_name]
                                
                                if player_name in self.player_info_cache:
                                    del self.player_info_cache[player_name]
                                
                                if player_name in self.processed_new_players:
                                    self.processed_new_players.discard(player_name)
                    
                    # 4. 批量匹配所有聊天消息
                    chat_matches = []
                    for line in lines:
                        match = chat_pattern.search(line)
                        if match:
                            chat_matches.append(('platform_id', match.group(1), match.group(2)))
                    
                    # 4.1 批量匹配 Pippi 模组聊天消息
                    for line in lines:
                        match = chat_pattern_pippi.search(line)
                        if match:
                            char_name_pippi = match.group(1)
                            chat_message_pippi = match.group(2)
                            already_matched = False
                            for m in chat_matches:
                                if m[2] == chat_message_pippi:
                                    already_matched = True
                                    break
                            if not already_matched:
                                chat_matches.append(('char_name', char_name_pippi, chat_message_pippi))
                    
                    if chat_matches:
                        players_info = get_online_players_info()
                        if players_info["success"]:
                            for match_type, match_value, chat_message in chat_matches:
                                player_info = None
                                
                                if match_type == 'platform_id':
                                    for player in players_info["players"]:
                                        if player["Platform_ID"] == match_value:
                                            player_info = player
                                            break
                                else:
                                    for player in players_info["players"]:
                                        if player["Char_name"] == match_value:
                                            player_info = player
                                            break
                                
                                if player_info:
                                    char_name = player_info["Char_name"]
                                    self.player_info_cache[player_info["Player_name"]] = player_info.copy()
                                    
                                    db_result = query_player_info_by_field(field="char_name", value=char_name)
                                    
                                    level = None
                                    if db_result["success"] and db_result["data"]:
                                        level = db_result["data"][0].get("level")
                                    
                                    spawn_result = get_player_spawn_points(char_name=char_name)
                                    spawn_info = None
                                    guild_id = None
                                    guild_name = None
                                    if spawn_result["success"] and spawn_result["data"]:
                                        spawn = spawn_result["data"][0]
                                        spawn_info = f"{spawn['x']:.2f}, {spawn['y']:.2f}, {spawn['z']:.2f}"
                                        guild_id = spawn.get('guild_id')
                                        guild_name = spawn.get('guild_name')
                                    
                                    output = f"\n{'='*60}\n"
                                    output += f"检测到聊天信息:\n"
                                    output += f"{'='*60}\n"
                                    for key, value in player_info.items():
                                        output += f"{key}: {value}\n"
                                    if level is not None:
                                        output += f"\"level\": {level}\n"
                                    if spawn_info is not None:
                                        output += f"\"spawn_point\": {spawn_info}\n"
                                    if guild_id is not None:
                                        output += f"\"guild_id\": {guild_id}\n"
                                    if guild_name is not None:
                                        output += f"\"guild_name\": {guild_name}\n"
                                    output += f"\"said\": \"{chat_message}\"\n"
                                    
                                    data = {
                                        "type": "chat_message",
                                        "player_info": player_info,
                                        "level": level,
                                        "spawn_point": spawn_info,
                                        "guild_id": guild_id,
                                        "guild_name": guild_name,
                                        "said": chat_message
                                    }
                                    
                                    mentioned_char_name = None
                                    if '@' in chat_message:
                                        mentioned_char_name = chat_message.split('@')[-1].strip()
                                        if mentioned_char_name:
                                            db_result_2 = query_player_info_by_field(field="char_name", value=mentioned_char_name)
                                            
                                            level_2 = None
                                            spawn_info_2 = None
                                            guild_id_2 = None
                                            guild_name_2 = None
                                            player_info_2 = None
                                            
                                            if db_result_2["success"] and db_result_2["data"]:
                                                player_data_2 = db_result_2["data"][0]
                                                level_2 = player_data_2.get("level")
                                                player_info_2 = {
                                                    "Idx_2": 0,
                                                    "Char_name_2": player_data_2.get("char_name"),
                                                    "Player_name_2": player_data_2.get("playerId"),
                                                    "User_ID_2": "",
                                                    "Platform_ID_2": player_data_2.get("playerId"),
                                                    "Platform_Name_2": "Steam"
                                                }
                                                
                                                spawn_result_2 = get_player_spawn_points(char_name=mentioned_char_name)
                                                if spawn_result_2["success"] and spawn_result_2["data"]:
                                                    spawn_2 = spawn_result_2["data"][0]
                                                    spawn_info_2 = f"{spawn_2['x']:.2f}, {spawn_2['y']:.2f}, {spawn_2['z']:.2f}"
                                                    guild_id_2 = spawn_2.get('guild_id')
                                                    guild_name_2 = spawn_2.get('guild_name')
                                            
                                            output += f"\n--- 被@的角色信息 ---\n"
                                            if player_info_2:
                                                for key, value in player_info_2.items():
                                                    output += f"{key}: {value}\n"
                                            if level_2 is not None:
                                                output += f"\"level_2\": {level_2}\n"
                                            else:
                                                output += f"\"level_2\": None\n"
                                            if spawn_info_2 is not None:
                                                output += f"\"spawn_point_2\": {spawn_info_2}\n"
                                            else:
                                                output += f"\"spawn_point_2\": None\n"
                                            if guild_id_2 is not None:
                                                output += f"\"guild_id_2\": {guild_id_2}\n"
                                            else:
                                                output += f"\"guild_id_2\": None\n"
                                            if guild_name_2 is not None:
                                                output += f"\"guild_name_2\": {guild_name_2}\n"
                                            else:
                                                output += f"\"guild_name_2\": None\n"
                                            
                                            data["player_info_2"] = player_info_2
                                            data["level_2"] = level_2
                                            data["spawn_point_2"] = spawn_info_2
                                            data["guild_id_2"] = guild_id_2
                                            data["guild_name_2"] = guild_name_2
                                    
                                    output += f"{'='*60}\n"
                                    
                                    self.log_message(output)
                                    
                                    self.send_data(data)
                
                time.sleep(1)
        
        except Exception as e:
            import traceback
            self.log_message(f"\n监听出错: {str(e)}")
            self.log_message(f"错误详情: {traceback.format_exc()}")
            if f:
                f.close()
    
    def start(self, log_file_path="Logs\\ConanSandbox.log"):
        self.monitoring_running = True
        monitor_thread = threading.Thread(target=self.monitor_chat_messages, args=(log_file_path,))
        monitor_thread.daemon = True
        monitor_thread.start()
    
    def stop(self):
        self.monitoring_running = False
