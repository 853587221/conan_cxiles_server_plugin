import urllib.parse
from datetime import datetime, timezone, timedelta
from bin.response_utils import send_success_response, send_error_response, send_json_response_with_cookie


def handle_login(handler, db, captcha_manager):
    """处理登录请求"""
    content_length = int(handler.headers['Content-Length'])
    post_data = handler.rfile.read(content_length).decode('utf-8')
    params = urllib.parse.parse_qs(post_data)
    
    username = params.get('username', [''])[0].strip()
    password = params.get('password', [''])[0].strip()
    captcha = params.get('captcha', [''])[0].strip()
    
    if not username or not password:
        send_error_response(handler, '用户名和密码不能为空')
        return
    
    lock_status = db.check_login_locked(username)
    if lock_status['locked']:
        send_error_response(handler, '账户已被锁定，请30分钟后再试')
        return
    
    fail_count = lock_status.get('fail_count', 0)
    if fail_count >= 3:
        if not captcha:
            send_error_response(handler, '请输入验证码', require_captcha=True)
            return
        
        session_id = handler.headers.get('User-Agent', '') + username
        captcha_valid, captcha_msg = captcha_manager.verify_captcha(session_id, captcha)
        if not captcha_valid:
            send_error_response(handler, captcha_msg, require_captcha=True)
            return
    
    user_info = db.get_user(username)
    if not user_info.get('success'):
        send_error_response(handler, '用户名或密码错误')
        return
    
    valid_until = user_info['user'].get('valid_until')
    if valid_until:
        shanghai_tz = timezone(timedelta(hours=8))
        current_time = datetime.now(shanghai_tz)
        valid_time = datetime.strptime(valid_until, '%Y-%m-%d %H:%M:%S').replace(tzinfo=shanghai_tz)
        if current_time > valid_time:
            send_error_response(handler, '账户已过期，请联系管理员')
            return
    
    user = db.verify_user(username, password)
    
    if user:
        cookie_expires = (datetime.now(timezone.utc) + timedelta(days=7)).strftime('%a, %d %b %Y %H:%M:%S GMT')
        send_json_response_with_cookie(
            handler, 
            200, 
            {'success': True, 'message': '登录成功', 'username': username},
            f'username={username}; Path=/; HttpOnly; SameSite=Lax; Expires={cookie_expires}'
        )
    else:
        failure_result = db.record_login_failure(username)
        require_captcha = failure_result.get('fail_count', 0) >= 3
        send_error_response(handler, failure_result['message'], require_captcha=require_captcha)


def handle_desktop_login(handler, db, token_manager):
    """处理桌面端登录请求"""
    content_length = int(handler.headers['Content-Length'])
    post_data = handler.rfile.read(content_length).decode('utf-8')
    
    content_type = handler.headers.get('Content-Type', '')
    
    if 'application/json' in content_type:
        import json
        data = json.loads(post_data)
    else:
        data = eval(post_data)
    
    username = data.get('username', '').strip()
    password = data.get('password', '').strip()
    
    if not username or not password:
        send_error_response(handler, '用户名和密码不能为空')
        return
    
    lock_status = db.check_login_locked(username)
    if lock_status['locked']:
        send_error_response(handler, '账户已被锁定，请30分钟后再试')
        return
    
    user = db.verify_user(username, password)
    
    if user:
        token = token_manager.generate_token(username)
        send_success_response(handler, {'token': token, 'username': username})
    else:
        failure_result = db.record_login_failure(username)
        send_error_response(handler, failure_result['message'])


def handle_register(handler, db):
    """处理注册请求"""
    content_length = int(handler.headers['Content-Length'])
    post_data = handler.rfile.read(content_length).decode('utf-8')
    params = urllib.parse.parse_qs(post_data)
    
    username = params.get('username', [''])[0].strip()
    password = params.get('password', [''])[0].strip()
    invite_code = params.get('invite_code', [''])[0].strip()
    
    if not username or not password:
        send_error_response(handler, '用户名和密码不能为空')
        return
    
    if not invite_code:
        send_error_response(handler, '邀请码不能为空')
        return
    
    result = db.create_user(username, password, invite_code=invite_code)
    if result['success']:
        cookie_expires = (datetime.now(timezone.utc) + timedelta(days=7)).strftime('%a, %d %b %Y %H:%M:%S GMT')
        send_json_response_with_cookie(
            handler, 
            200, 
            {'success': True, 'message': '注册成功', 'username': username},
            f'username={username}; Path=/; HttpOnly; SameSite=Lax; Expires={cookie_expires}'
        )
    else:
        send_error_response(handler, result['message'])


def handle_change_password(handler, db):
    """处理修改密码请求"""
    content_length = int(handler.headers['Content-Length'])
    post_data = handler.rfile.read(content_length).decode('utf-8')
    params = urllib.parse.parse_qs(post_data)
    
    username = params.get('username', [''])[0].strip()
    old_password = params.get('old_password', [''])[0].strip()
    new_password = params.get('new_password', [''])[0].strip()
    
    if not username or not old_password or not new_password:
        send_error_response(handler, '用户名、旧密码和新密码不能为空')
        return
    
    result = db.change_password(username, old_password, new_password)
    if result['success']:
        send_success_response(handler, {'message': '密码修改成功'})
    else:
        send_error_response(handler, result['message'])


def handle_verify_session(handler, cookies, validate_token_func):
    """处理会话验证请求"""
    username = cookies.get('username')
    
    if username:
        send_success_response(handler, {'username': username})
        return
    
    auth_header = handler.headers.get('Authorization')
    if auth_header and auth_header.startswith('Bearer '):
        token = auth_header.split(' ')[1]
        username = validate_token_func(token)
        if username:
            send_success_response(handler, {'username': username})
            return
    
    send_error_response(handler, '无效令牌')


def handle_logout(handler):
    """处理退出登录请求"""
    send_json_response_with_cookie(
        handler, 
        200, 
        {'success': True, 'message': '退出登录成功'},
        'username=; expires=Thu, 01 Jan 1970 00:00:00 UTC; Path=/; HttpOnly; SameSite=Lax'
    )
