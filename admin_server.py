import http.server
import socketserver
import json
import sqlite3
from urllib.parse import urlparse, parse_qs
from pathlib import Path
import sys
import webbrowser
import threading
import time

sys.path.insert(0, str(Path(__file__).parent))
from bin.edit_db import DatabaseEditor

class AdminHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        self.db = DatabaseEditor()
        super().__init__(*args, **kwargs)
    
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        super().end_headers()
    
    def do_OPTIONS(self):
        self.send_response(200)
        self.end_headers()
    
    def do_GET(self):
        parsed_path = urlparse(self.path)
        
        if parsed_path.path == '/':
            self.serve_file('admin/index.html')
        elif parsed_path.path == '/api/login':
            self.handle_login(parsed_path)
        elif parsed_path.path == '/api/verify-session':
            self.handle_verify_session(parsed_path)
        elif parsed_path.path == '/api/users':
            self.handle_get_users()
        elif parsed_path.path.startswith('/api/users/'):
            username = parsed_path.path.split('/')[-1]
            self.handle_get_user(username)
        elif parsed_path.path == '/api/invite-codes':
            self.handle_get_invite_codes()
        elif parsed_path.path.startswith('/static/'):
            self.serve_static_file(parsed_path.path)
        else:
            self.serve_file('admin/index.html')
    
    def do_POST(self):
        parsed_path = urlparse(self.path)
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        try:
            data = json.loads(post_data.decode('utf-8'))
        except:
            data = {}
        
        if parsed_path.path == '/api/users':
            self.handle_create_user(data)
        elif parsed_path.path == '/api/users/update':
            self.handle_update_user(data)
        elif parsed_path.path == '/api/users/delete':
            self.handle_delete_user(data)
        elif parsed_path.path == '/api/users/set-admin':
            self.handle_set_admin(data)
        elif parsed_path.path == '/api/users/unlock':
            self.handle_unlock_user(data)
        elif parsed_path.path == '/api/invite-codes':
            self.handle_create_invite_code(data)
        elif parsed_path.path == '/api/invite-codes/delete':
            self.handle_delete_invite_code(data)
        elif parsed_path.path == '/api/invite-codes/activate':
            self.handle_activate_invite_code(data)
        elif parsed_path.path == '/api/invite-codes/deactivate':
            self.handle_deactivate_invite_code(data)
        else:
            self.send_json_response({'success': False, 'message': '未知的请求'}, 404)
    
    def serve_file(self, file_path):
        full_path = Path(__file__).parent / file_path
        if full_path.exists():
            self.send_response(200)
            if file_path.endswith('.html'):
                self.send_header('Content-type', 'text/html; charset=utf-8')
            elif file_path.endswith('.css'):
                self.send_header('Content-type', 'text/css; charset=utf-8')
            elif file_path.endswith('.js'):
                self.send_header('Content-type', 'application/javascript; charset=utf-8')
            self.end_headers()
            with open(full_path, 'rb') as f:
                self.wfile.write(f.read())
        else:
            self.send_response(404)
            self.end_headers()
    
    def serve_static_file(self, path):
        file_name = path.replace('/static/', '')
        full_path = Path(__file__).parent / 'admin' / file_name
        if full_path.exists():
            self.send_response(200)
            if file_name.endswith('.css'):
                self.send_header('Content-type', 'text/css; charset=utf-8')
            elif file_name.endswith('.js'):
                self.send_header('Content-type', 'application/javascript; charset=utf-8')
            self.end_headers()
            with open(full_path, 'rb') as f:
                self.wfile.write(f.read())
        else:
            self.send_response(404)
            self.end_headers()
    
    def handle_login(self, parsed_path):
        params = parse_qs(parsed_path.query)
        username = params.get('username', [''])[0]
        password = params.get('password', [''])[0]
        
        user = self.db.verify_user(username, password)
        if user:
            if len(user) > 9 and user[9] == 1:
                self.send_json_response({'success': True, 'message': '登录成功', 'username': username})
            else:
                self.send_json_response({'success': False, 'message': '您没有管理员权限'})
        else:
            self.send_json_response({'success': False, 'message': '用户名或密码错误'})
    
    def handle_verify_session(self, parsed_path):
        params = parse_qs(parsed_path.query)
        username = params.get('username', [''])[0]
        
        user = self.db.get_user(username)
        if user.get('success') and 'user' in user:
            user_data = user['user']
            is_admin = user_data.get('is_admin', False)
            self.send_json_response({'success': True, 'is_admin': is_admin})
        else:
            self.send_json_response({'success': False, 'message': '用户不存在'})
    
    def handle_get_users(self):
        result = self.db.get_all_users()
        self.send_json_response(result)
    
    def handle_get_user(self, username):
        result = self.db.get_user(username)
        print(f"获取用户 {username} 的信息: {result}")
        if result.get('success') and 'user' in result:
            print(f"用户 valid_until 值: {result['user'].get('valid_until')}")
        self.send_json_response(result)
    
    def handle_create_user(self, data):
        result = self.db.create_user(
            username=data.get('username'),
            password=data.get('password'),
            rcon_ip=data.get('rcon_ip'),
            rcon_password=data.get('rcon_password'),
            rcon_port=data.get('rcon_port'),
            invite_code=data.get('invite_code'),
            is_admin=data.get('is_admin', False),
            valid_until=data.get('valid_until')
        )
        self.send_json_response(result)
    
    def handle_update_user(self, data):
        print(f"收到的更新数据: {data}")
        original_username = data.get('original_username')
        username = data.get('username')
        password = data.get('password')
        valid_until = data.get('valid_until')
        
        print(f"有效期值: {valid_until}, 类型: {type(valid_until)}")
        
        if not original_username:
            self.send_json_response({'success': False, 'message': '原始用户名不能为空'})
            return
        
        if username != original_username:
            self.send_json_response({'success': False, 'message': '不允许修改用户名'})
            return
        
        if password:
            result = self.db.update_password(original_username, password)
            if not result['success']:
                self.send_json_response(result)
                return
        
        result = self.db.update_user_info(
            username=original_username,
            rcon_ip=data.get('rcon_ip'),
            rcon_password=data.get('rcon_password'),
            rcon_port=data.get('rcon_port'),
            is_admin=data.get('is_admin'),
            valid_until=valid_until
        )
        self.send_json_response(result)
    
    def handle_delete_user(self, data):
        result = self.db.delete_user(data.get('username'))
        self.send_json_response(result)
    
    def handle_set_admin(self, data):
        result = self.db.set_admin(
            username=data.get('username'),
            is_admin=data.get('is_admin', True)
        )
        self.send_json_response(result)
    
    def handle_unlock_user(self, data):
        result = self.db.reset_login_failures(data.get('username'))
        self.send_json_response(result)
    
    def handle_get_invite_codes(self):
        result = self.db.get_all_invite_codes()
        self.send_json_response(result)
    
    def handle_create_invite_code(self, data):
        result = self.db.create_invite_code(
            code=data.get('code'),
            creator_id=data.get('creator_id'),
            max_uses=data.get('max_uses', 1),
            expire_days=data.get('expire_days')
        )
        self.send_json_response(result)
    
    def handle_delete_invite_code(self, data):
        result = self.db.delete_invite_code(data.get('code'))
        self.send_json_response(result)
    
    def handle_activate_invite_code(self, data):
        result = self.db.activate_invite_code(data.get('code'))
        self.send_json_response(result)
    
    def handle_deactivate_invite_code(self, data):
        result = self.db.deactivate_invite_code(data.get('code'))
        self.send_json_response(result)
    
    def send_json_response(self, data, status_code=200):
        self.send_response(status_code)
        self.send_header('Content-type', 'application/json; charset=utf-8')
        self.end_headers()
        self.wfile.write(json.dumps(data, ensure_ascii=False).encode('utf-8'))

def start_server(port=8001):
    handler = AdminHandler
    
    with socketserver.TCPServer(('', port), handler) as httpd:
        print(f'管理员后台服务器运行在 http://localhost:{port}')
        print(f'请在浏览器中打开 http://localhost:{port}')
        
        def open_browser():
            time.sleep(1)
            webbrowser.open(f'http://localhost:{port}')
        
        threading.Thread(target=open_browser, daemon=True).start()
        
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print('\n服务器已停止')

if __name__ == '__main__':
    import argparse
    
    parser = argparse.ArgumentParser(description='Conan Exiles 管理员后台服务器')
    parser.add_argument('--port', type=int, default=8001, help='服务器端口（默认：8001）')
    args = parser.parse_args()
    
    start_server(args.port)