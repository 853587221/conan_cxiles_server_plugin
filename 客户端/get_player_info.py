import configparser
import os
import time
from mcrcon import MCRcon

def read_ini_file(file_path):
    """尝试使用多种编码读取 INI 文件"""
    encodings = ['utf-8', 'utf-16-le', 'utf-16', 'utf-16-be', 'gbk', 'gb2312', 'latin-1']
    
    for encoding in encodings:
        try:
            config = configparser.ConfigParser()
            config.read(file_path, encoding=encoding)
            return config
        except UnicodeDecodeError:
            continue
        except Exception:
            continue
    
    return None

def get_server_config():
    """从配置文件中读取服务器配置"""
    engine_ini_path = os.path.join("Config", "WindowsServer", "Engine.ini")
    game_ini_path = os.path.join("Config", "WindowsServer", "Game.ini")
    
    config = {
        "host": "127.0.0.1",
        "port": 25575,
        "password": ""
    }
    
    # 读取 Engine.ini 获取 IP 地址
    if os.path.exists(engine_ini_path):
        engine_config = read_ini_file(engine_ini_path)
        if engine_config and 'OnlineSubsystem' in engine_config:
            ip = engine_config['OnlineSubsystem'].get('DedicatedServerLauncherMultihomeIP', '').strip()
            if ip:
                config["host"] = ip
    
    # 读取 Game.ini 获取 RCON 端口和密码
    if os.path.exists(game_ini_path):
        game_config = read_ini_file(game_ini_path)
        if game_config and 'RconPlugin' in game_config:
            port = game_config['RconPlugin'].get('RconPort', '').strip()
            if port:
                config["port"] = int(port)
            
            password = game_config['RconPlugin'].get('RconPassword', '').strip()
            if password:
                config["password"] = password
    
    return config

def get_online_players_info():
    """
    使用 mcrcon 库获取在线玩家信息
    
    Returns:
        dict: 包含在线玩家信息的字典，格式为：
            {
                "success": bool,
                "players": list,
                "message": str
            }
            其中 players 列表包含玩家信息，每个玩家是一个字典
    """
    result = execute_rcon_command("listplayers")
    
    if not result["success"]:
        return {
            "success": False,
            "players": [],
            "message": result["message"]
        }
    
    response = result["response"]
    
    players = []
    lines = response.split('\n')
    
    for line in lines:
        line = line.strip()
        
        if not line:
            continue
        if line.startswith("There are") or line.startswith("No players") or line.startswith("Total"):
            continue
        if "Idx" in line or "Char name" in line or "Player name" in line:
            continue
        
        parts = [part.strip() for part in line.split('|')]
        
        if len(parts) >= 6:
            idx = parts[0]
            char_name = parts[1]
            player_name = parts[2]
            user_id = parts[3]
            platform_id = parts[4]
            platform_name = parts[5]
            
            players.append({
                "Idx": idx,
                "Char_name": char_name,
                "Player_name": player_name,
                "User_ID": user_id,
                "Platform_ID": platform_id,
                "Platform_Name": platform_name
            })
    
    return {
        "success": True,
        "players": players,
        "message": f"成功获取 {len(players)} 名在线玩家"
    }

def execute_rcon_command(command, max_retries=6, retry_delay=2):
    """
    执行 RCON 命令，支持自动重试和IP回退
    
    Args:
        command (str): 要执行的 RCON 命令
        max_retries (int): 最大重试次数，默认6次
        retry_delay (int): 重试间隔（秒），默认2秒
    
    Returns:
        dict: 包含执行结果的字典，格式为：
            {
                "success": bool,
                "response": str,
                "message": str,
                "attempts": int  # 尝试次数
            }
    """
    config = get_server_config()
    
    if not config["password"]:
        return {
            "success": False,
            "response": "",
            "message": "未找到 RCON 密码配置",
            "attempts": 0
        }
    
    hosts_to_try = ["127.0.0.1"]
    
    if config["host"] != "127.0.0.1" and config["host"] != "localhost":
        hosts_to_try.append(config["host"])
    
    last_error = None
    total_attempts = 0
    
    for host in hosts_to_try:
        for attempt in range(max_retries):
            total_attempts += 1
            try:
                with MCRcon(host, config["password"], config["port"]) as mcr:
                    response = mcr.command(command)
                    message = f"命令执行成功: {command}"
                    if host != config["host"]:
                        message += f" (使用回退IP: {host})"
                    return {
                        "success": True,
                        "response": response,
                        "message": message,
                        "attempts": total_attempts
                    }
            except ConnectionRefusedError as e:
                last_error = f"无法连接到 RCON 服务器: {host}:{config['port']}"
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                    continue
                else:
                    break
            except Exception as e:
                last_error = str(e)
                error_msg = str(e).lower()
                
                if any(keyword in error_msg for keyword in ['connection', 'timeout', 'refused', 'broken pipe']):
                    if attempt < max_retries - 1:
                        time.sleep(retry_delay)
                        continue
                    else:
                        break
                else:
                    break
    
    return {
        "success": False,
        "response": "",
        "message": f"命令执行失败（已重试{total_attempts}次）: {last_error}",
        "attempts": total_attempts
    }

def activate_thrall_spawn():
    """
    激活奴隶生成功能
    
    此命令用于首次激活奴隶生成，否则第一次执行命令的时候是无效的。
    执行命令: con 0 datacmd spawn exact Accursed_Alchemist_1_Cimmerian thrall
    
    Returns:
        dict: 包含执行结果的字典
    """
    command = "con 0 datacmd spawn exact Accursed_Alchemist_1_Cimmerian thrall"
    return execute_rcon_command(command)

if __name__ == "__main__":
    result = get_online_players_info()
    if result["success"]:
        print(f"{result['message']}")
        for idx, player in enumerate(result["players"], 1):
            print(f"{idx}. {player}")
    else:
        print(f"错误: {result['message']}")
