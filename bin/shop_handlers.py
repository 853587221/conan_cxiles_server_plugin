import sqlite3
import urllib.parse
import re
import time
import sys
from pathlib import Path
from bin.response_utils import send_success_response, send_error_response
from bin.user_db_manager import UserDBManager


def handle_shop_query(handler, directory):
    """处理商城查询金币API"""
    query_params = urllib.parse.parse_qs(handler.path.split('?')[1]) if '?' in handler.path else {}
    target_username = query_params.get('username', [''])[0]

    if not target_username:
        send_error_response(handler, '缺少用户名参数')
        return

    db_path = directory / f"data/{target_username}/database.db"

    if not db_path.exists():
        send_error_response(handler, '用户数据库不存在')
        return

    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute('SELECT char_name, gold FROM players ORDER BY char_name')
        players = cursor.fetchall()
        conn.close()

        total_gold = sum(player[1] or 0 for player in players)
        player_list = [
            {'char_name': player[0], 'gold': player[1] or 0}
            for player in players
        ]

        send_success_response(handler, {
            'username': target_username,
            'total_gold': total_gold,
            'player_count': len(player_list),
            'players': player_list
        })
    except Exception as e:
        send_error_response(handler, f'查询失败: {str(e)}')


def handle_shop_categories(handler, username):
    """处理商城商品分类API"""
    user_db = UserDBManager(username)
    categories = user_db.get_product_categories()
    
    all_products = user_db.get_products()
    all_count = len(all_products)
    
    all_category = {
        'id': 0,
        'key': 'all',
        'name': '全部',
        'icon': '🏪',
        'sort_order': -1,
        'product_count': all_count
    }
    
    send_success_response(handler, {'categories': [all_category] + categories})


def handle_shop_products(handler, username):
    """处理商城商品API"""
    query_params = urllib.parse.parse_qs(handler.path.split('?')[1]) if '?' in handler.path else {}
    category = query_params.get('category', [None])[0]

    user_db = UserDBManager(username)
    
    if category and category != 'all':
        products = user_db.get_products(category)
    else:
        products = user_db.get_products()
    
    send_success_response(handler, {'products': products})


def handle_shop_search(handler, username):
    """处理商城搜索商品API"""
    query_params = urllib.parse.parse_qs(handler.path.split('?')[1]) if '?' in handler.path else {}
    keyword = query_params.get('keyword', [''])[0]

    if not keyword:
        send_error_response(handler, '缺少搜索关键词')
        return

    user_db = UserDBManager(username)
    products = user_db.search_products(keyword)
    send_success_response(handler, {'products': products})


def handle_shop_image(handler, directory, cookies, validate_token_func):
    """处理商城图片API"""
    path_parts = handler.path.split('/')
    username = cookies.get('username')
    
    if not username:
        auth_header = handler.headers.get('Authorization')
        if auth_header and auth_header.startswith('Bearer '):
            token = auth_header.split(' ')[1]
            username = validate_token_func(token)
    
    if len(path_parts) >= 5:
        url_username = path_parts[4]
        if url_username and url_username != 'image':
            username = url_username
    
    if not username:
        referer = handler.headers.get('Referer', '')
        match = re.search(r'/shop/(\w+)', referer)
        if match:
            username = match.group(1)
    
    if not username:
        send_error_response(handler, '无法确定用户身份', 401)
        return
    
    raw_filename = path_parts[-1]
    filename = raw_filename.split('?')[0] if '?' in raw_filename else raw_filename
    
    query_params = {}
    if '?' in handler.path:
        query_str = handler.path.split('?')[1]
        query_params = dict(urllib.parse.parse_qs(query_str))
    
    if not filename:
        handler.send_response(400)
        handler.end_headers()
        return
    
    size_param = query_params.get('size', [''])[0]
    image_path = directory / f'data/{username}/images/{filename}'
    
    if not image_path.exists():
        handler.send_response(404)
        handler.end_headers()
        return
    
    try:
        from PIL import Image
        import io
        
        size = None
        if size_param:
            try:
                size = tuple(map(int, size_param.split('x')))
            except:
                pass
        
        if size:
            with Image.open(image_path) as img:
                original_format = img.format if img.format else 'PNG'
                img.thumbnail(size, Image.Resampling.LANCZOS)
                output = io.BytesIO()
                
                if original_format == 'WEBP':
                    img.save(output, format='WEBP', quality=85)
                    content_type = 'image/webp'
                elif original_format == 'JPEG':
                    img.save(output, format='JPEG', quality=85)
                    content_type = 'image/jpeg'
                elif original_format == 'GIF':
                    img.save(output, format='PNG')
                    content_type = 'image/png'
                elif original_format == 'BMP':
                    img.save(output, format='PNG')
                    content_type = 'image/png'
                else:
                    img.save(output, format='PNG', quality=85)
                    content_type = 'image/png'
                
                image_data = output.getvalue()
        else:
            with open(image_path, 'rb') as f:
                image_data = f.read()
            
            file_ext = Path(filename).suffix.lower()
            if file_ext == '.webp':
                content_type = 'image/webp'
            elif file_ext in ['.jpg', '.jpeg']:
                content_type = 'image/jpeg'
            elif file_ext == '.gif':
                content_type = 'image/gif'
            elif file_ext == '.bmp':
                content_type = 'image/bmp'
            else:
                content_type = 'image/png'
        
        handler.send_response(200)
        handler.send_header('Content-Type', content_type)
        handler.send_header('Cache-Control', 'public, max-age=31536000')
        handler.end_headers()
        handler.wfile.write(image_data)
    except Exception as e:
        print(f"[ERROR] 图片处理失败: {e}")
        handler.send_response(500)
        handler.end_headers()


def handle_shop_admin_category_add(handler, user_db, data):
    """处理添加商品分类"""
    key = data.get('key', '').strip()
    name = data.get('name', '').strip()
    icon = data.get('icon', '').strip()
    sort_order = int(data.get('sort_order', 0))
    
    if not key or not name:
        send_error_response(handler, '分类key和名称不能为空')
        return
    
    result = user_db.create_product_category(key, name, icon, sort_order)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_shop_admin_category_update(handler, user_db, data):
    """处理更新商品分类"""
    category_id = data.get('id')
    key = data.get('key', '').strip()
    name = data.get('name', '').strip()
    icon = data.get('icon', '').strip()
    sort_order = int(data.get('sort_order', 0))
    
    if not category_id:
        send_error_response(handler, '分类ID不能为空')
        return
    
    if not key:
        send_error_response(handler, '分类key不能为空')
        return
    
    result = user_db.update_product_category(int(category_id), key, name, icon, sort_order)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_shop_admin_category_delete(handler, user_db, data):
    """处理删除商品分类"""
    path_parts = handler.path.split('/')
    category_id = None
    
    for i, part in enumerate(path_parts):
        if part == 'delete' and i + 1 < len(path_parts):
            try:
                category_id = int(path_parts[i + 1].split('?')[0])
            except (ValueError, IndexError):
                pass
            break
    
    if not category_id:
        category_id = data.get('id')
    
    if not category_id:
        send_error_response(handler, '分类ID不能为空')
        return
    
    result = user_db.delete_product_category(int(category_id))
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_shop_admin_product_add(handler, user_db, data):
    """处理添加商品"""
    name = data.get('name', '').strip()
    category = data.get('category', '').strip()
    price = int(data.get('price', 0))
    description = data.get('description', '').strip()
    image = data.get('image', '').strip()
    sortOrder = int(data.get('sort_order', 0))
    
    if not name or not category:
        send_error_response(handler, '商品名称和分类不能为空')
        return
    
    result = user_db.create_product(name, image, description, category, price, sortOrder)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_shop_admin_product_update(handler, user_db, data):
    """处理更新商品"""
    product_id = data.get('id')
    name = data.get('name', '').strip()
    category = data.get('category', '').strip()
    price = int(data.get('price', 0))
    description = data.get('description', '').strip()
    image = data.get('image', '').strip()
    sortOrder = int(data.get('sort_order', 0))
    
    if not product_id:
        send_error_response(handler, '商品ID不能为空')
        return
    
    result = user_db.update_product(product_id, name, image, description, category, price, sortOrder)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_shop_admin_product_delete(handler, user_db, data):
    """处理删除商品"""
    product_id = data.get('id')
    
    if not product_id:
        send_error_response(handler, '商品ID不能为空')
        return
    
    result = user_db.delete_product(product_id)
    if result['success']:
        send_success_response(handler)
    else:
        send_error_response(handler, result['message'])


def handle_shop_admin_product_upload(handler, username, directory, content_length, content_type, post_data):
    """处理商品图片上传"""
    if 'multipart/form-data' not in content_type:
        send_error_response(handler, '请使用multipart/form-data上传文件')
        return
    
    boundary = content_type.split('boundary=')[-1].encode()
    parts = post_data.split(b'--' + boundary)
    
    image_data = None
    filename = None
    
    for part in parts:
        if b'Content-Disposition' in part and b'filename=' in part:
            try:
                lines = part.split(b'\r\n')
                for i, line in enumerate(lines):
                    if b'filename=' in line:
                        filename = line.split(b'filename=')[-1].decode().strip('"')
                    elif line == b'' and i > 0:
                        data_start = i + 1
                        image_data = b'\r\n'.join(lines[data_start:]).rstrip(b'\r\n')
                        break
            except Exception as e:
                print(f"[DEBUG] 解析part失败: {e}")
                continue
    
    print(f"[DEBUG] filename: {filename}, image_data length: {len(image_data) if image_data else 0}")
    
    if not image_data or not filename:
        send_error_response(handler, '未找到图片文件')
        return
    
    if len(image_data) > 2 * 1024 * 1024:
        send_error_response(handler, '图片大小不能超过2MB')
        return
    
    allowed_extensions = ['.png', '.jpg', '.jpeg', '.gif', '.webp', '.bmp']
    file_ext = Path(filename).suffix.lower()
    if file_ext not in allowed_extensions:
        send_error_response(handler, f'只支持以下格式的图片: {", ".join(allowed_extensions)}')
        return
    
    images_dir = directory / f'data/{username}/images'
    images_dir.mkdir(parents=True, exist_ok=True)
    
    timestamp = int(time.time())
    image_filename = f'product_{timestamp}{file_ext}'
    image_path = images_dir / image_filename
    
    with open(image_path, 'wb') as f:
        f.write(image_data)
    
    print(f"[DEBUG] 图片上传成功: {image_filename}")
    
    try:
        sys.path.insert(0, str(directory / 'shop'))
        from check_nsfw import check_and_process_image
        
        print(f"[DEBUG] 开始NSFW检查: {image_path}")
        nsfw_result = check_and_process_image(str(image_path))
        
        if nsfw_result['is_nsfw']:
            print(f"[DEBUG] 图片包含违规内容，已处理: {nsfw_result['violations']}")
    except Exception as e:
        print(f"[DEBUG] NSFW检查失败，继续上传: {e}")
    
    send_success_response(handler, {'filename': image_filename})
