import http.server
import socketserver
import os
import webbrowser
import sys
import json
import urllib.parse
import hashlib
import sqlite3
import time
import threading
import queue
import signal
from pathlib import Path
from bin.edit_db import DatabaseEditor
from bin.user_db_manager import UserDBManager
from bin.rcon_manager import RconManager
from bin.token_manager import TokenManager
from bin.captcha_manager import CaptchaManager
from bin.response_utils import send_json_response, send_success_response, send_error_response
from bin.auth_handlers import (
    handle_login, handle_desktop_login, handle_register,
    handle_change_password, handle_verify_session, handle_logout
)
from bin.shop_handlers import (
    handle_shop_query, handle_shop_categories, handle_shop_products,
    handle_shop_search, handle_shop_image,
    handle_shop_admin_category_add, handle_shop_admin_category_update,
    handle_shop_admin_category_delete, handle_shop_admin_product_add,
    handle_shop_admin_product_update, handle_shop_admin_product_delete,
    handle_shop_admin_product_upload
)
from bin.api_handlers import (
    handle_version, handle_categories, handle_commands,
    handle_commands_by_category, handle_search_commands,
    handle_auto_trigger_rules, handle_auto_trigger_rule,
    handle_auto_trigger_rule_toggle,
    handle_rcon_connection_info, handle_player_join,
    handle_players, handle_chat_messages, handle_sse_events, handle_users_update,
    handle_category_create, handle_category_update, handle_category_delete,
    handle_command_create, handle_command_update, handle_command_delete,
    handle_auto_trigger_rule_create, handle_auto_trigger_rule_update,
    handle_auto_trigger_rule_delete, handle_players_update,
    handle_players_delete, handle_players_reset_signin,
    handle_rcon_connect, handle_rcon_send, handle_rcon_send_via_sse,
    handle_save_rcon_mode,
    handle_ai_service_get, handle_ai_service_save, handle_ai_models_fetch,
    handle_migration_verify, handle_migration_execute,
    handle_cleanup_execute,
    handle_qq_bot_settings_get, handle_qq_bot_settings_save
)
from bin.sse_handlers import (
    update_spawn_point, update_char_name_2, update_guild_id,
    get_user_spawn_point, get_user_char_name_2, get_user_guild_id,
    add_sse_client, remove_sse_client, broadcast_event,
    execute_command_with_variables, create_command_event,
    create_gold_event, create_tag_event, create_player_event,
    register_inventory_request, get_inventory_request, set_inventory_response,
    register_rcon_request, get_rcon_request, set_rcon_response,
    register_thrall_request, get_thrall_request, set_thrall_response,
    cleanup_old_requests, sse_clients
)
from bin.item_specifics import create_inventory_request_event, create_inventory_response_event
from bin.client_data_handlers import handle_client_data
from bin.sse_handlers import LATEST_PLAYER_JOIN
from shop.query import query_player_gold
from bin.qq_bot import start_qq_bot

PORT = 8000
DIRECTORY = Path(__file__).parent

token_manager = TokenManager()
captcha_manager = CaptchaManager()


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        self.db = DatabaseEditor()
        super().__init__(*args, directory=DIRECTORY, **kwargs)
    
    def generate_token(self, username):
        return token_manager.generate_token(username)
    
    def validate_token(self, token):
        return token_manager.validate_token(token)
    
    def end_headers(self):
        self.send_header('Cache-Control', 'no-store, no-cache, must-revalidate')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        super().end_headers()
    
    def log_message(self, format, *args):
        pass
    
    def get_cookies(self):
        cookies = {}
        cookie_header = self.headers.get('Cookie')
        if cookie_header:
            for cookie in cookie_header.split(';'):
                if '=' in cookie:
                    key, value = cookie.strip().split('=', 1)
                    cookies[key] = value
        return cookies
    
    def is_logged_in(self):
        cookies = self.get_cookies()
        if 'username' in cookies:
            return True
        
        auth_header = self.headers.get('Authorization')
        if auth_header and auth_header.startswith('Bearer '):
            token = auth_header.split(' ')[1]
            username = self.validate_token(token)
            if username:
                return True
        
        return False
    
    def get_username(self):
        cookies = self.get_cookies()
        username = cookies.get('username')
        
        if not username:
            auth_header = self.headers.get('Authorization')
            if auth_header and auth_header.startswith('Bearer '):
                token = auth_header.split(' ')[1]
                username = self.validate_token(token)
        
        return username
    
    def do_GET(self):
        print(f"[DEBUG] 收到GET请求: {self.path}")
        
        if self.path.startswith('/api/captcha'):
            username = urllib.parse.parse_qs(self.path.split('?')[1]).get('username', [''])[0] if '?' in self.path else ''
            session_id = self.headers.get('User-Agent', '') + username
            captcha_code = captcha_manager.generate_captcha(session_id)
            send_success_response(self, {'captcha': captcha_code})
            return
        
        if self.path == '/api/version':
            handle_version(self, DIRECTORY)
            return
        
        if self.path.startswith('/api/'):
            if self.path == '/api/verify-session':
                handle_verify_session(self, self.get_cookies(), self.validate_token)
                return
            
            if self.path.startswith('/api/shop/query'):
                handle_shop_query(self, DIRECTORY)
                return
            
            if self.path.startswith('/api/shop/categories'):
                query_params = urllib.parse.parse_qs(self.path.split('?')[1]) if '?' in self.path else {}
                username = query_params.get('username', [''])[0]
                if not username:
                    send_error_response(self, '缺少用户名参数')
                    return
                handle_shop_categories(self, username)
                return
            
            if self.path.startswith('/api/shop/products'):
                query_params = urllib.parse.parse_qs(self.path.split('?')[1]) if '?' in self.path else {}
                username = query_params.get('username', [''])[0]
                if not username:
                    send_error_response(self, '缺少用户名参数')
                    return
                handle_shop_products(self, username)
                return
            
            if self.path.startswith('/api/shop/search'):
                query_params = urllib.parse.parse_qs(self.path.split('?')[1]) if '?' in self.path else {}
                username = query_params.get('username', [''])[0]
                if not username:
                    send_error_response(self, '缺少用户名参数')
                    return
                handle_shop_search(self, username)
                return
            
            if self.path.startswith('/api/shop/image/'):
                handle_shop_image(self, DIRECTORY, self.get_cookies(), self.validate_token)
                return
            
            if not self.is_logged_in():
                send_error_response(self, '未登录', 401)
                return
            
            username = self.get_username()
            if not username:
                send_error_response(self, '未登录', 401)
                return
            
            user_db = UserDBManager(username)
            
            if self.path == '/api/categories':
                handle_categories(self, user_db)
                return
            
            if self.path == '/api/commands':
                handle_commands(self, user_db)
                return
            
            if self.path.startswith('/api/commands/category/'):
                handle_commands_by_category(self, user_db)
                return
            
            if self.path.startswith('/api/commands/search/'):
                handle_search_commands(self, user_db)
                return
            
            if self.path == '/api/auto-trigger-rules':
                handle_auto_trigger_rules(self, user_db)
                return
            
            if self.path.startswith('/api/auto-trigger-rules/') and self.path.endswith('/toggle'):
                handle_auto_trigger_rule_toggle(self, user_db)
                return
            
            if self.path.startswith('/api/auto-trigger-rules/') and self.path.count('/') == 3:
                handle_auto_trigger_rule(self, user_db)
                return
            
            if self.path == '/api/rcon/connection-info':
                handle_rcon_connection_info(self, username)
                return
            
            if self.path == '/api/player-join':
                handle_player_join(self)
                return
            
            if self.path == '/api/players':
                handle_players(self, user_db)
                return
            
            if self.path.startswith('/api/chat-messages'):
                handle_chat_messages(self, user_db)
                return
            
            if self.path == '/api/events':
                handle_sse_events(self, username)
                return
            
            if self.path.startswith('/api/inventory/'):
                char_name = urllib.parse.unquote(self.path.split('/')[-1])
                request_id = str(int(time.time() * 1000))
                
                print(f"[DEBUG] 收到背包请求: username={username}, char_name={char_name}, request_id={request_id}")
                
                register_inventory_request(username, request_id)
                
                request_event = create_inventory_request_event(char_name)
                request_event['request_id'] = request_id
                
                print(f"[DEBUG] 广播事件到SSE客户端: {username}, 当前SSE客户端数: {len(sse_clients.get(username, []))}")
                broadcast_event(username, request_event)
                
                max_wait_time = 15
                wait_interval = 0.1
                waited_time = 0
                
                while waited_time < max_wait_time:
                    request = get_inventory_request(username, request_id)
                    if request and request.get('response'):
                        print(f"[DEBUG] 收到背包响应: request_id={request_id}")
                        response_data = {
                            'char_name': char_name,
                            'inventory': request['response'].get('inventory', {}),
                            'message': '获取背包数据成功'
                        }
                        if request['response'].get('thralls'):
                            response_data['thralls'] = request['response'].get('thralls')
                            print(f"[DEBUG] 包含奴隶数据: {len(request['response'].get('thralls')) if isinstance(request['response'].get('thralls'), (list, dict)) else 0} 个奴隶")
                        if request['response'].get('player_info'):
                            response_data['player_info'] = request['response'].get('player_info')
                            print(f"[DEBUG] 包含玩家信息: player_id={request['response'].get('player_info', {}).get('player_id')}")
                        send_success_response(self, response_data)
                        return
                    
                    time.sleep(wait_interval)
                    waited_time += wait_interval
                
                print(f"[DEBUG] 背包请求超时: request_id={request_id}, 等待时间={waited_time}秒")
                send_error_response(self, '获取背包数据超时，请确保桌面客户端正在运行', 408)
                return
            
            if self.path.startswith('/api/thralls/'):
                char_name = urllib.parse.unquote(self.path.split('/')[-1])
                request_id = str(int(time.time() * 1000))
                
                print(f"[DEBUG] 收到奴隶请求: username={username}, char_name={char_name}, request_id={request_id}")
                
                register_thrall_request(username, request_id)
                
                request_event = {
                    'type': 'thrall_request',
                    'data': {
                        'char_name': char_name
                    },
                    'request_id': request_id
                }
                
                print(f"[DEBUG] 广播奴隶请求事件到SSE客户端: {username}, 当前SSE客户端数: {len(sse_clients.get(username, []))}")
                broadcast_event(username, request_event)
                
                max_wait_time = 15
                wait_interval = 0.1
                waited_time = 0
                
                while waited_time < max_wait_time:
                    request = get_thrall_request(username, request_id)
                    if request and request.get('response'):
                        print(f"[DEBUG] 收到奴隶响应: request_id={request_id}")
                        send_success_response(self, {
                            'char_name': char_name,
                            'thralls': request['response'].get('thralls', []),
                            'message': '获取奴隶数据成功'
                        })
                        return
                    
                    time.sleep(wait_interval)
                    waited_time += wait_interval
                
                print(f"[DEBUG] 奴隶请求超时: request_id={request_id}, 等待时间={waited_time}秒")
                send_error_response(self, '获取奴隶数据超时，请确保桌面客户端正在运行', 408)
                return
            
            if self.path == '/api/ai-service':
                handle_ai_service_get(self, user_db)
                return
            
            if self.path == '/api/qq-bot/settings':
                handle_qq_bot_settings_get(self, self.db, username)
                return
            
            send_error_response(self, '未找到请求的资源', 404)
            return
        
        if self.path == '/login.html' or self.path.endswith('/login.html') or self.path == '/register.html' or self.path.endswith('/register.html'):
            super().do_GET()
            return
        
        if self.path.startswith('/shop/query'):
            query_params = urllib.parse.parse_qs(self.path.split('?')[1]) if '?' in self.path else {}
            target_username = query_params.get('username', [''])[0]
            
            if not target_username:
                send_error_response(self, '缺少用户名参数')
                return
            
            result = query_player_gold(target_username)
            send_json_response(self, 200 if result['success'] else 400, result)
            return
        
        if self.path == '/shop/check_gold.html':
            super().do_GET()
            return
        
        if self.path.startswith('/shop'):
            if self.path == '/shop' or self.path == '/shop/':
                cookies = self.get_cookies()
                target_username = cookies.get('username')
                
                if not target_username:
                    auth_header = self.headers.get('Authorization')
                    if auth_header and auth_header.startswith('Bearer '):
                        token = auth_header.split(' ')[1]
                        target_username = self.validate_token(token)
                
                if target_username:
                    self.send_response(302)
                    self.send_header('Location', f'/shop/{urllib.parse.quote(target_username)}')
                    self.end_headers()
                    return
                
                self.path = '/shop/index.html'
            elif self.path.startswith('/shop/') and len(self.path.split('/')) == 3:
                path_parts = self.path.split('/')
                potential_username = path_parts[2]
                
                if potential_username and potential_username not in ['image', 'scripts.js', 'styles.css', 'index.html', 'check_gold.html']:
                    self.send_response(200)
                    self.send_header('Content-Type', 'text/html; charset=utf-8')
                    self.send_header('X-Shop-Username', potential_username)
                    self.end_headers()
                    
                    with open('/www/wwwroot/LFZKN/RconClient/shop/index.html', 'r', encoding='utf-8') as f:
                        self.wfile.write(f.read().encode('utf-8'))
                    return
            
            super().do_GET()
            return
        
        super().do_GET()
    
    def do_POST(self):
        print(f"[DEBUG] 收到POST请求: {self.path}")
        
        if self.path == '/api/login':
            handle_login(self, self.db, captcha_manager)
            return
        
        if self.path == '/api/desktop/login':
            handle_desktop_login(self, self.db, token_manager)
            return
        
        if self.path == '/api/register':
            handle_register(self, self.db)
            return
        
        if self.path == '/api/change-password':
            handle_change_password(self, self.db)
            return
        
        if self.path == '/api/logout':
            handle_logout(self)
            return
        
        if not self.is_logged_in():
            send_error_response(self, '未登录', 401)
            return
        
        username = self.get_username()
        if not username:
            send_error_response(self, '未登录', 401)
            return
        
        user_db = UserDBManager(username)
        
        content_length = int(self.headers.get('Content-Length', 0))
        
        if self.path == '/api/shop/admin/product/upload':
            content_type = self.headers.get('Content-Type', '')
            post_data = self.rfile.read(content_length)
            handle_shop_admin_product_upload(self, username, DIRECTORY, content_length, content_type, post_data)
            return
        
        if self.path.startswith('/api/auto-trigger-rules/') and self.path.endswith('/toggle'):
            handle_auto_trigger_rule_toggle(self, user_db)
            return
        
        post_data = self.rfile.read(content_length)
        
        try:
            data = json.loads(post_data.decode('utf-8')) if post_data else {}
        except json.JSONDecodeError:
            data = {}
        
        if self.path.startswith('/api/shop/admin/'):
            if self.path.startswith('/api/shop/admin/category/add'):
                handle_shop_admin_category_add(self, user_db, data)
                return
            elif self.path.startswith('/api/shop/admin/category/update'):
                handle_shop_admin_category_update(self, user_db, data)
                return
            elif self.path.startswith('/api/shop/admin/category/delete'):
                handle_shop_admin_category_delete(self, user_db, data)
                return
            elif self.path.startswith('/api/shop/admin/product/add'):
                handle_shop_admin_product_add(self, user_db, data)
                return
            elif self.path.startswith('/api/shop/admin/product/update'):
                handle_shop_admin_product_update(self, user_db, data)
                return
            elif self.path.startswith('/api/shop/admin/product/delete'):
                handle_shop_admin_product_delete(self, user_db, data)
                return
        
        if self.path == '/api/users/update':
            handle_users_update(self, self.db, data)
            return
        
        if self.path == '/api/categories/create':
            handle_category_create(self, user_db, data)
            return
        
        if self.path == '/api/categories/update':
            handle_category_update(self, user_db, data)
            return
        
        if self.path == '/api/categories/delete':
            handle_category_delete(self, user_db, data)
            return
        
        if self.path == '/api/commands/create':
            handle_command_create(self, user_db, data)
            return
        
        if self.path == '/api/commands/update':
            handle_command_update(self, user_db, data)
            return
        
        if self.path == '/api/commands/delete':
            handle_command_delete(self, user_db, data)
            return
        
        if self.path == '/api/auto-trigger-rules/create':
            handle_auto_trigger_rule_create(self, user_db, data)
            return
        
        if self.path == '/api/players/update':
            handle_players_update(self, user_db, data)
            return
        
        if self.path == '/api/players/delete':
            handle_players_delete(self, user_db, data)
            return
        
        if self.path == '/api/players/reset-signin':
            handle_players_reset_signin(self, user_db, data)
            return
        
        if self.path == '/api/auto-trigger-rules/update':
            handle_auto_trigger_rule_update(self, user_db, data)
            return
        
        if self.path == '/api/auto-trigger-rules/delete':
            handle_auto_trigger_rule_delete(self, user_db, data)
            return
        
        if self.path == '/api/rcon/connect':
            handle_rcon_connect(self, username, data)
            return
        
        if self.path == '/api/rcon/send':
            handle_rcon_send(self, username, data)
            return
        
        if self.path == '/api/rcon/send-via-sse':
            handle_rcon_send_via_sse(self, username, data)
            return
        
        if self.path == '/api/rcon/save-mode':
            handle_save_rcon_mode(self, username, data)
            return
        
        if self.path == '/api/ai-service/save':
            handle_ai_service_save(self, user_db, data)
            return
        
        if self.path == '/api/ai-service/models':
            handle_ai_models_fetch(self, data)
            return
        
        if self.path == '/api/client/data':
            send_success_response(self, {'message': '数据已接收'})
            threading.Thread(target=handle_client_data, args=(username, data), daemon=True).start()
            return
        
        if self.path == '/api/migration/verify':
            handle_migration_verify(self, self.db, username, data)
            return
        
        if self.path == '/api/migration/execute':
            handle_migration_execute(self, username, data)
            return
        
        if self.path == '/api/cleanup/execute':
            handle_cleanup_execute(self, user_db, data)
            return
        
        if self.path == '/api/qq-bot/settings':
            handle_qq_bot_settings_save(self, self.db, username, data)
            return
        
        send_error_response(self, '未找到请求的资源', 404)


class ReuseAddressTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True
    daemon_threads = True


def start_server(port=PORT):
    httpd = ReuseAddressTCPServer(("", port), Handler)
    print(f"Server running at http://localhost:{port}")
    
    def signal_handler(signum, frame):
        print(f"\n[DEBUG] Signal {signum} received")
        print("\nStopping server...")
        
        import os
        
        print("[DEBUG] Calling os._exit(0)")
        os._exit(0)
    
    signal.signal(signal.SIGTERM, signal_handler)
    signal.signal(signal.SIGINT, signal_handler)
    
    def cleanup_requests():
        """定期清理过期的背包请求"""
        while True:
            time.sleep(60)
            cleanup_old_requests()
    
    def cleanup_chat_messages():
        """定期清理聊天记录，每5天清理一次，保留最新500条"""
        CLEANUP_INTERVAL = 5 * 24 * 60 * 60  # 5天（秒）
        KEEP_COUNT = 500  # 保留最新500条
        
        while True:
            time.sleep(CLEANUP_INTERVAL)
            try:
                print("[DEBUG] 开始清理聊天记录...")
                data_dir = Path(__file__).parent / 'data'
                if data_dir.exists():
                    for user_dir in data_dir.iterdir():
                        if user_dir.is_dir():
                            db_path = user_dir / 'database.db'
                            if db_path.exists():
                                from bin.user_db_manager import UserDBManager
                                username = user_dir.name
                                user_db = UserDBManager(username)
                                result = user_db.cleanup_old_chat_messages(KEEP_COUNT)
                                if result.get('deleted_count', 0) > 0:
                                    print(f"[DEBUG] 用户 [{username}] {result['message']}")
                print("[DEBUG] 聊天记录清理完成")
            except Exception as e:
                print(f"[ERROR] 清理聊天记录失败: {e}")
    
    cleanup_thread = threading.Thread(target=cleanup_requests, daemon=True)
    cleanup_thread.start()
    
    chat_cleanup_thread = threading.Thread(target=cleanup_chat_messages, daemon=True)
    chat_cleanup_thread.start()
    
    start_qq_bot()
    
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        signal_handler(None, None)


if __name__ == '__main__':
    start_server()
