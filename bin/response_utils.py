import json


def send_json_response(handler, status_code, data):
    """发送JSON响应"""
    handler.send_response(status_code)
    handler.send_header('Content-Type', 'application/json')
    handler.end_headers()
    handler.wfile.write(json.dumps(data).encode('utf-8'))


def send_success_response(handler, data=None):
    """发送成功响应"""
    response = {'success': True}
    if data is not None:
        response.update(data)
    send_json_response(handler, 200, response)


def send_error_response(handler, message, status_code=400):
    """发送错误响应"""
    send_json_response(handler, status_code, {'success': False, 'message': message})


def send_json_response_with_cookie(handler, status_code, data, cookie):
    """发送JSON响应并设置Cookie"""
    handler.send_response(status_code)
    handler.send_header('Content-Type', 'application/json')
    handler.send_header('Set-Cookie', cookie)
    handler.end_headers()
    handler.wfile.write(json.dumps(data).encode('utf-8'))
