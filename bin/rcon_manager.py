import sqlite3
import sys
import json
import time
from pathlib import Path
from .edit_db import DatabaseEditor

# 直接使用本地的mcrcon.py文件
from . import mcrcon
MCRCON_AVAILABLE = True

class RconManager:
    def __init__(self, db_path=None):
        self.db_path = db_path or Path(__file__).parent.parent / 'user_database.db'
        self.db_editor = DatabaseEditor(str(self.db_path))
    
    def check_connection(self, host, password, port):
        """检查RCON连接是否正常"""
        try:
            with mcrcon.MCRcon(host, password, port, timeout=3) as mcr:
                mcr.command('help')
                return {
                    "success": True,
                    "message": "连接正常"
                }
        except Exception as e:
            return {
                "success": False,
                "message": f"连接失败: {str(e)}"
            }
    
    def connect_rcon(self, host, password, port, username=None):
        """连接到RCON服务器"""
        try:
            # 使用本地mcrcon模块连接到RCON服务器
            with mcrcon.MCRcon(host, password, port) as mcr:
                # 发送测试命令验证连接
                response = mcr.command('help')
                return {
                    "success": True,
                    "message": "RCON连接成功",
                    "response": response
                }
        except Exception as e:
            return {
                "success": False,
                "message": f"RCON连接失败: {str(e)}"
            }
    
    def send_command(self, host, password, port, command, max_retries=3, retry_delay=2):
        """发送RCON命令到服务器，支持自动重试和连接状态检查"""
        last_error = None
        
        for attempt in range(max_retries):
            try:
                # 每次尝试前先检查连接状态
                if attempt > 0:
                    check_result = self.check_connection(host, password, port)
                    if not check_result['success']:
                        # 连接失败，尝试重新连接
                        connect_result = self.connect_rcon(host, password, port)
                        if not connect_result['success']:
                            last_error = f"重连失败: {connect_result['message']}"
                            time.sleep(retry_delay)
                            continue
                
                # 使用本地mcrcon模块连接到RCON服务器
                with mcrcon.MCRcon(host, password, port) as mcr:
                    # 发送命令
                    response = mcr.command(command)
                    return {
                        "success": True,
                        "message": "命令执行成功",
                        "response": response,
                        "attempts": attempt + 1
                    }
            except Exception as e:
                last_error = str(e)
                error_msg = str(e).lower()
                
                # 如果是连接相关错误，尝试重连
                if any(keyword in error_msg for keyword in ['login failed', 'connection', 'timeout', 'refused']):
                    if attempt < max_retries - 1:
                        time.sleep(retry_delay)
                        continue
                
                # 如果是其他错误，也尝试重试
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                    continue
        
        # 所有重试都失败了
        return {
            "success": False,
            "message": f"命令执行失败（已重试{max_retries}次）: {last_error}",
            "attempts": max_retries
        }
    
    def save_connection_info(self, username, host, password, port, rcon_mode='direct'):
        """保存连接信息到数据库"""
        try:
            result = self.db_editor.update_user_info(
                username=username,
                rcon_ip=host,
                rcon_password=password,
                rcon_port=port,
                rcon_mode=rcon_mode
            )
            return result
        except Exception as e:
            return {
                "success": False,
                "message": f"保存连接信息失败: {str(e)}"
            }
    
    def get_connection_info(self, username):
        """获取用户的连接信息"""
        try:
            result = self.db_editor.get_user(username)
            if result['success']:
                user = result['user']
                return {
                    "success": True,
                    "connection_info": {
                        "host": user['rcon_ip'],
                        "password": user['rcon_password'],
                        "port": user['rcon_port'],
                        "rcon_mode": user.get('rcon_mode', 'direct')
                    }
                }
            return result
        except Exception as e:
            return {
                "success": False,
                "message": f"获取连接信息失败: {str(e)}"
            }
    
    def send_command_with_saved_info(self, username, command):
        """使用保存的连接信息发送命令"""
        try:
            conn_info = self.get_connection_info(username)
            if not conn_info['success']:
                return {
                    "success": False,
                    "message": f"获取连接信息失败: {conn_info.get('message', '未知错误')}"
                }
            
            info = conn_info['connection_info']
            host = info['host']
            password = info['password']
            port = info['port']
            
            if not host or not password or not port:
                return {
                    "success": False,
                    "message": "连接信息不完整，请先配置RCON连接"
                }
            
            return self.send_command(host, password, port, command)
        except Exception as e:
            return {
                "success": False,
                "message": f"发送命令失败: {str(e)}"
            }

def main():
    """主函数，处理命令行参数"""
    if len(sys.argv) < 2:
        print(json.dumps({
            "success": False,
            "message": "缺少命令参数"
        }))
        sys.exit(1)
    
    command = sys.argv[1]
    rcon_manager = RconManager()
    
    try:
        if command == 'connect':
            # 解析连接参数
            host = sys.argv[2] if len(sys.argv) > 2 else ''
            password = sys.argv[3] if len(sys.argv) > 3 else ''
            port = int(sys.argv[4]) if len(sys.argv) > 4 else 25575
            username = sys.argv[5] if len(sys.argv) > 5 else None
            
            # 连接RCON
            result = rcon_manager.connect_rcon(host, password, port, username)
            print(json.dumps(result))
        
        elif command == 'save':
            # 解析保存参数
            if len(sys.argv) < 6:
                print(json.dumps({
                    "success": False,
                    "message": "保存命令缺少必要参数: username host password port"
                }))
                sys.exit(1)
            
            username = sys.argv[2]
            host = sys.argv[3]
            password = sys.argv[4]
            port = int(sys.argv[5])
            
            # 保存连接信息
            result = rcon_manager.save_connection_info(username, host, password, port)
            print(json.dumps(result))
        
        elif command == 'get':
            # 解析获取参数
            if len(sys.argv) < 3:
                print(json.dumps({
                    "success": False,
                    "message": "获取命令缺少必要参数: username"
                }))
                sys.exit(1)
            
            username = sys.argv[2]
            
            # 获取连接信息
            result = rcon_manager.get_connection_info(username)
            print(json.dumps(result))
        
        else:
            print(json.dumps({
                "success": False,
                "message": f"未知命令: {command}"
            }))
            sys.exit(1)
    
    except Exception as e:
        print(json.dumps({
            "success": False,
            "message": f"执行命令失败: {str(e)}"
        }))
        sys.exit(1)

if __name__ == '__main__':
    main()