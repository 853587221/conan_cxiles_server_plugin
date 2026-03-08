import sqlite3
import hashlib
import shutil
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

class DatabaseEditor:
    def __init__(self, db_path=None):
        if db_path is None:
            db_path = Path(__file__).parent.parent / 'user_database.db'
        self.db_path = str(db_path)
        self._enable_wal_mode()
        self._migrate_database()
        
    def _enable_wal_mode(self):
        conn = sqlite3.connect(self.db_path, timeout=30.0)
        try:
            conn.execute('PRAGMA journal_mode=WAL')
            conn.commit()
        finally:
            conn.close()
    
    def _migrate_database(self):
        conn = sqlite3.connect(self.db_path, timeout=30.0)
        try:
            cursor = conn.cursor()
            cursor.execute("PRAGMA table_info(users)")
            columns = [row[1] for row in cursor.fetchall()]
            
            if 'rcon_mode' not in columns:
                cursor.execute("ALTER TABLE users ADD COLUMN rcon_mode TEXT DEFAULT 'direct'")
                conn.commit()
            
            cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='qq_bot'")
            qq_bot_exists = cursor.fetchone() is not None
            
            if not qq_bot_exists:
                cursor.execute('''
                    CREATE TABLE qq_bot (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        qq_group TEXT DEFAULT '',
                        group_binding_time INTEGER DEFAULT 0,
                        FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                ''')
                conn.commit()
            
            if 'qq_group' in columns or 'group_binding_time' in columns:
                cursor.execute('''
                    SELECT id, qq_group, group_binding_time FROM users 
                    WHERE qq_group IS NOT NULL AND qq_group != ''
                ''')
                existing_bindings = cursor.fetchall()
                
                for user_id, qq_group, group_binding_time in existing_bindings:
                    if qq_group:
                        cursor.execute('''
                            INSERT OR IGNORE INTO qq_bot (user_id, qq_group, group_binding_time)
                            VALUES (?, ?, ?)
                        ''', (user_id, qq_group, group_binding_time or 0))
                conn.commit()
                
                self._remove_qq_columns_from_users(conn)
        finally:
            conn.close()
    
    def _remove_qq_columns_from_users(self, conn):
        cursor = conn.cursor()
        cursor.execute("PRAGMA table_info(users)")
        columns_info = cursor.fetchall()
        columns = [row[1] for row in columns_info]
        
        if 'qq_group' not in columns and 'group_binding_time' not in columns:
            return
        
        cursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='users'")
        result = cursor.fetchone()
        if not result:
            return
        
        new_columns = []
        for col in columns_info:
            col_name = col[1]
            if col_name not in ('qq_group', 'group_binding_time'):
                new_columns.append(col_name)
        
        columns_def = ', '.join(new_columns)
        cursor.execute(f"CREATE TABLE users_new AS SELECT {columns_def} FROM users")
        cursor.execute("DROP TABLE users")
        cursor.execute("ALTER TABLE users_new RENAME TO users")
        
        cursor.execute('''
            CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)
        ''')
        conn.commit()
    
    def _retry_on_lock(self, func, max_retries=3, initial_delay=0.1):
        for attempt in range(max_retries):
            try:
                return func()
            except sqlite3.OperationalError as e:
                if 'database is locked' in str(e) and attempt < max_retries - 1:
                    delay = initial_delay * (2 ** attempt)
                    time.sleep(delay)
                    continue
                raise
        return None
    
    def get_connection(self):
        return sqlite3.connect(self.db_path, timeout=30.0)
    
    def get_shanghai_time(self):
        shanghai_tz = timezone(timedelta(hours=8))
        shanghai_time = datetime.now(shanghai_tz)
        return shanghai_time.strftime('%Y-%m-%d %H:%M:%S')
    
    def hash_password(self, password):
        return hashlib.sha256(password.encode('utf-8')).hexdigest()
    
    def create_user(self, username, password, rcon_ip='', rcon_password='', rcon_port='', invite_code=None, is_admin=False, valid_until=None):
        def _create_user_impl():
            conn = self.get_connection()
            cursor = conn.cursor()
            try:
                register_time = self.get_shanghai_time()
                hashed_password = self.hash_password(password)
                is_admin_value = 1 if is_admin else 0
                
                user_path = f"data/{username}"
                data_dir = Path(__file__).parent.parent / user_path
                data_dir.mkdir(parents=True, exist_ok=True)
                
                template_db = Path(__file__).parent.parent / "data" / "database.db"
                user_db = data_dir / "database.db"
                shutil.copy2(template_db, user_db)
                
                cursor.execute('''
                    INSERT INTO users (username, password, rcon_ip, rcon_password, rcon_port, invite_code, is_admin, register_time, user_path, valid_until)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (username, hashed_password, rcon_ip, rcon_password, rcon_port, invite_code, is_admin_value, register_time, user_path, valid_until))
                user_id = cursor.lastrowid
                
                conn.commit()
                return {'success': True, 'message': '用户创建成功', 'user_id': user_id}
            except sqlite3.IntegrityError:
                return {'success': False, 'message': '用户名已存在'}
            finally:
                conn.close()
        
        try:
            if invite_code and not self.validate_invite_code(invite_code):
                return {'success': False, 'message': '邀请码无效或已过期'}
            
            result = self._retry_on_lock(_create_user_impl)
            
            if result and result['success'] and invite_code:
                self._retry_on_lock(lambda: self.use_invite_code(invite_code))
            
            return result if result else {'success': False, 'message': '创建用户失败: 未知错误'}
        except Exception as e:
            return {'success': False, 'message': f'创建用户失败: {str(e)}'}
    
    def verify_user(self, username, password):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            hashed_password = self.hash_password(password)
            cursor.execute('''
                SELECT * FROM users WHERE username = ? AND password = ?
            ''', (username, hashed_password))
            user = cursor.fetchone()
            if user:
                valid_until = user[14] if len(user) > 14 else None
                if valid_until:
                    from datetime import datetime
                    shanghai_tz = timezone(timedelta(hours=8))
                    current_time = datetime.now(shanghai_tz)
                    valid_time = datetime.strptime(valid_until, '%Y-%m-%d %H:%M:%S').replace(tzinfo=shanghai_tz)
                    if current_time > valid_time:
                        return None
                
                login_time = self.get_shanghai_time()
                cursor.execute('''
                    UPDATE users SET login_time = ?, login_fail_count = 0, last_fail_time = NULL, locked_until = NULL WHERE username = ?
                ''', (login_time, username))
                conn.commit()
            return user
        finally:
            conn.close()
    
    def update_user_info(self, username, rcon_ip=None, rcon_password=None, rcon_port=None, is_admin=None, valid_until=None, rcon_mode=None):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            updates = []
            params = []
            if rcon_ip is not None:
                updates.append('rcon_ip = ?')
                params.append(rcon_ip)
            if rcon_password is not None:
                updates.append('rcon_password = ?')
                params.append(rcon_password)
            if rcon_port is not None:
                updates.append('rcon_port = ?')
                params.append(rcon_port)
            if is_admin is not None:
                updates.append('is_admin = ?')
                params.append(1 if is_admin else 0)
            if valid_until is not None:
                updates.append('valid_until = ?')
                params.append(valid_until)
            if rcon_mode is not None:
                updates.append('rcon_mode = ?')
                params.append(rcon_mode)
            
            if updates:
                params.append(username)
                query = f"UPDATE users SET {', '.join(updates)} WHERE username = ?"
                cursor.execute(query, params)
                conn.commit()
                return {'success': True, 'message': '用户信息更新成功'}
            return {'success': False, 'message': '没有需要更新的字段'}
        finally:
            conn.close()
    
    def update_password(self, username, new_password):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            hashed_password = self.hash_password(new_password)
            cursor.execute('''
                UPDATE users SET password = ? WHERE username = ?
            ''', (hashed_password, username))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '密码更新成功'}
            return {'success': False, 'message': '用户不存在'}
        finally:
            conn.close()
    
    def set_admin(self, username, is_admin=True):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            is_admin_value = 1 if is_admin else 0
            cursor.execute('''
                UPDATE users SET is_admin = ? WHERE username = ?
            ''', (is_admin_value, username))
            conn.commit()
            if cursor.rowcount > 0:
                action = '设置为管理员' if is_admin else '取消管理员'
                return {'success': True, 'message': f'用户{action}成功'}
            return {'success': False, 'message': '用户不存在'}
        finally:
            conn.close()
    
    def delete_user(self, username):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('DELETE FROM users WHERE username = ?', (username,))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '用户删除成功'}
            return {'success': False, 'message': '用户不存在'}
        finally:
            conn.close()
    
    def get_user(self, username):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT * FROM users WHERE username = ?', (username,))
            user = cursor.fetchone()
            if user:
                return {
                    'success': True,
                    'user': {
                        'id': user[0],
                        'username': user[1],
                        'rcon_ip': user[3],
                        'rcon_password': user[4],
                        'rcon_port': user[5],
                        'invite_code': user[8] if len(user) > 8 else None,
                        'is_admin': bool(user[9] if len(user) > 9 else 0),
                        'register_time': user[6] if len(user) > 6 else None,
                        'login_time': user[7] if len(user) > 7 else None,
                        'user_path': user[10] if len(user) > 10 else None,
                        'login_fail_count': user[11] if len(user) > 11 else 0,
                        'last_fail_time': user[12] if len(user) > 12 else None,
                        'locked_until': user[13] if len(user) > 13 else None,
                        'valid_until': user[14] if len(user) > 14 else None,
                        'rcon_mode': user[15] if len(user) > 15 else 'direct'
                    }
                }
            return {'success': False, 'message': '用户不存在'}
        finally:
            conn.close()
    
    def get_all_users(self):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT * FROM users')
            users = cursor.fetchall()
            return {
                'success': True,
                'users': [
                    {
                        'id': u[0],
                        'username': u[1],
                        'rcon_ip': u[3],
                        'rcon_port': u[5],
                        'invite_code': u[8] if len(u) > 8 else None,
                        'is_admin': bool(u[9] if len(u) > 9 else 0),
                        'register_time': u[6] if len(u) > 6 else None,
                        'login_time': u[7] if len(u) > 7 else None,
                        'user_path': u[10] if len(u) > 10 else None,
                        'login_fail_count': u[11] if len(u) > 11 else 0,
                        'last_fail_time': u[12] if len(u) > 12 else None,
                        'locked_until': u[13] if len(u) > 13 else None,
                        'valid_until': u[14] if len(u) > 14 else None
                    }
                    for u in users
                ]
            }
        finally:
            conn.close()
    
    def create_invite_code(self, code, creator_id=None, max_uses=1, expire_days=None):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            create_time = self.get_shanghai_time()
            expire_time = None
            if expire_days:
                shanghai_tz = timezone(timedelta(hours=8))
                expire_datetime = datetime.now(shanghai_tz) + timedelta(days=expire_days)
                expire_time = expire_datetime.strftime('%Y-%m-%d %H:%M:%S')
            
            cursor.execute('''
                INSERT INTO invite_codes (code, creator_id, max_uses, create_time, expire_time)
                VALUES (?, ?, ?, ?, ?)
            ''', (code, creator_id, max_uses, create_time, expire_time))
            conn.commit()
            return {'success': True, 'message': '邀请码创建成功', 'code_id': cursor.lastrowid}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '邀请码已存在'}
        finally:
            conn.close()
    
    def validate_invite_code(self, code):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''
                SELECT * FROM invite_codes 
                WHERE code = ? AND is_active = 1 AND used_count < max_uses
            ''', (code,))
            invite_code = cursor.fetchone()
            
            if not invite_code:
                return False
            
            if invite_code[7]:
                current_time = self.get_shanghai_time()
                if current_time > invite_code[7]:
                    return False
            
            return True
        finally:
            conn.close()
    
    def use_invite_code(self, code):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''
                UPDATE invite_codes 
                SET used_count = used_count + 1 
                WHERE code = ?
            ''', (code,))
            conn.commit()
            return cursor.rowcount > 0
        finally:
            conn.close()
    
    def get_invite_code(self, code):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT * FROM invite_codes WHERE code = ?', (code,))
            invite = cursor.fetchone()
            if invite:
                return {
                    'success': True,
                    'invite_code': {
                        'id': invite[0],
                        'code': invite[1],
                        'creator_id': invite[2],
                        'max_uses': invite[3],
                        'used_count': invite[4],
                        'is_active': invite[5],
                        'create_time': invite[6],
                        'expire_time': invite[7]
                    }
                }
            return {'success': False, 'message': '邀请码不存在'}
        finally:
            conn.close()
    
    def get_all_invite_codes(self):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT * FROM invite_codes')
            codes = cursor.fetchall()
            return {
                'success': True,
                'invite_codes': [
                    {
                        'id': c[0],
                        'code': c[1],
                        'creator_id': c[2],
                        'max_uses': c[3],
                        'used_count': c[4],
                        'is_active': c[5],
                        'create_time': c[6],
                        'expire_time': c[7]
                    }
                    for c in codes
                ]
            }
        finally:
            conn.close()
    
    def deactivate_invite_code(self, code):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('UPDATE invite_codes SET is_active = 0 WHERE code = ?', (code,))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '邀请码已停用'}
            return {'success': False, 'message': '邀请码不存在'}
        finally:
            conn.close()
    
    def activate_invite_code(self, code):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('UPDATE invite_codes SET is_active = 1 WHERE code = ?', (code,))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '邀请码已激活'}
            return {'success': False, 'message': '邀请码不存在'}
        finally:
            conn.close()
    
    def delete_invite_code(self, code):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('DELETE FROM invite_codes WHERE code = ?', (code,))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '邀请码删除成功'}
            return {'success': False, 'message': '邀请码不存在'}
        finally:
            conn.close()
    
    def generate_invite_code(self, length=8):
        import random
        import string
        characters = string.ascii_uppercase + string.digits
        return ''.join(random.choice(characters) for _ in range(length))
    
    def get_user_count(self):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT COUNT(*) FROM users')
            count = cursor.fetchone()[0]
            return {'success': True, 'count': count}
        finally:
            conn.close()
    
    def get_invite_code_count(self):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT COUNT(*) FROM invite_codes')
            count = cursor.fetchone()[0]
            return {'success': True, 'count': count}
        finally:
            conn.close()
    
    def record_login_failure(self, username):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            current_time = self.get_shanghai_time()
            
            cursor.execute('''
                SELECT login_fail_count, locked_until FROM users WHERE username = ?
            ''', (username,))
            result = cursor.fetchone()
            
            if not result:
                return {'success': False, 'message': '用户不存在'}
            
            fail_count, locked_until = result
            
            if locked_until:
                locked_time = datetime.strptime(locked_until, '%Y-%m-%d %H:%M:%S')
                if datetime.now() < locked_time:
                    return {'success': False, 'message': '账户已被锁定'}
            
            new_fail_count = fail_count + 1
            
            if new_fail_count >= 5:
                lock_until = datetime.now() + timedelta(minutes=30)
                cursor.execute('''
                    UPDATE users 
                    SET login_fail_count = ?, last_fail_time = ?, locked_until = ?
                    WHERE username = ?
                ''', (new_fail_count, current_time, lock_until.strftime('%Y-%m-%d %H:%M:%S'), username))
                conn.commit()
                return {'success': False, 'message': '登录失败次数过多，账户已被锁定30分钟'}
            else:
                cursor.execute('''
                    UPDATE users 
                    SET login_fail_count = ?, last_fail_time = ?
                    WHERE username = ?
                ''', (new_fail_count, current_time, username))
                conn.commit()
                remaining = 5 - new_fail_count
                return {'success': False, 'message': f'用户名或密码错误，剩余尝试次数: {remaining}'}
        finally:
            conn.close()
    
    def reset_login_failures(self, username):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''
                UPDATE users 
                SET login_fail_count = 0, last_fail_time = NULL, locked_until = NULL
                WHERE username = ?
            ''', (username,))
            conn.commit()
            return {'success': True}
        finally:
            conn.close()
    
    def check_login_locked(self, username):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''
                SELECT locked_until, login_fail_count FROM users WHERE username = ?
            ''', (username,))
            result = cursor.fetchone()
            
            if not result:
                return {'locked': False}
            
            locked_until, fail_count = result
            
            if locked_until:
                locked_time = datetime.strptime(locked_until, '%Y-%m-%d %H:%M:%S')
                if datetime.now() < locked_time:
                    return {'locked': True, 'fail_count': fail_count}
                else:
                    cursor.execute('''
                        UPDATE users 
                        SET locked_until = NULL, login_fail_count = 0
                        WHERE username = ?
                    ''', (username,))
                    conn.commit()
                    return {'locked': False}
            
            return {'locked': False, 'fail_count': fail_count}
        finally:
            conn.close()
    
    def get_user_id(self, username):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT id FROM users WHERE username = ?', (username,))
            result = cursor.fetchone()
            if result:
                return {'success': True, 'user_id': result[0]}
            return {'success': False, 'message': '用户不存在'}
        finally:
            conn.close()
    
    def get_qq_bot_settings(self, user_id):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT * FROM qq_bot WHERE user_id = ?', (user_id,))
            result = cursor.fetchone()
            if result:
                return {
                    'success': True,
                    'settings': {
                        'id': result[0],
                        'user_id': result[1],
                        'qq_group': result[2],
                        'group_binding_time': result[3],
                        'Binding_message_1': result[4] if len(result) > 4 else '',
                        'Binding_message_2': result[5] if len(result) > 5 else '',
                        'Binding_message_3': result[12] if len(result) > 12 else '',
                        'sign_message_1': result[6] if len(result) > 6 else '',
                        'sign_message_2': result[7] if len(result) > 7 else '',
                        'sign_message_3': result[8] if len(result) > 8 else '',
                        'balance_message_1': result[9] if len(result) > 9 else '',
                        'balance_message_2': result[10] if len(result) > 10 else '',
                        'online_players_message': result[11] if len(result) > 11 else '',
                        'sign_reset_type': result[13] if len(result) > 13 else 'daily',
                        'sign_reset_hour': result[14] if len(result) > 14 else 0,
                        'sign_interval_hours': result[15] if len(result) > 15 else 24,
                        'sign_gold_type': result[16] if len(result) > 16 else 'fixed',
                        'sign_gold_fixed': result[17] if len(result) > 17 else 100,
                        'sign_gold_min': result[18] if len(result) > 18 else 1,
                        'sign_gold_max': result[19] if len(result) > 19 else 50,
                        'sign_gold_weight': result[20] if len(result) > 20 else 1.0
                    }
                }
            return {'success': False, 'message': '未找到QQ机器人设置'}
        finally:
            conn.close()
    
    def save_qq_bot_settings(self, user_id, settings):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT id, Binding_message_1, Binding_message_2, Binding_message_3, sign_message_1, sign_message_2, sign_message_3, balance_message_1, balance_message_2, online_players_message, sign_reset_type, sign_reset_hour, sign_interval_hours, sign_gold_type, sign_gold_fixed, sign_gold_min, sign_gold_max, sign_gold_weight FROM qq_bot WHERE user_id = ?', (user_id,))
            existing = cursor.fetchone()
            
            if existing:
                current_settings = {
                    'Binding_message_1': existing[1] or '',
                    'Binding_message_2': existing[2] or '',
                    'Binding_message_3': existing[3] or '',
                    'sign_message_1': existing[4] or '',
                    'sign_message_2': existing[5] or '',
                    'sign_message_3': existing[6] or '',
                    'balance_message_1': existing[7] or '',
                    'balance_message_2': existing[8] or '',
                    'online_players_message': existing[9] or '',
                    'sign_reset_type': existing[10] or 'daily',
                    'sign_reset_hour': existing[11] or 0,
                    'sign_interval_hours': existing[12] or 24,
                    'sign_gold_type': existing[13] or 'fixed',
                    'sign_gold_fixed': existing[14] or 100,
                    'sign_gold_min': existing[15] or 1,
                    'sign_gold_max': existing[16] or 50,
                    'sign_gold_weight': existing[17] or 1.0
                }
                for key, value in settings.items():
                    if value is not None:
                        current_settings[key] = value
                
                cursor.execute('''
                    UPDATE qq_bot SET
                        Binding_message_1 = ?,
                        Binding_message_2 = ?,
                        Binding_message_3 = ?,
                        sign_message_1 = ?,
                        sign_message_2 = ?,
                        sign_message_3 = ?,
                        balance_message_1 = ?,
                        balance_message_2 = ?,
                        online_players_message = ?,
                        sign_reset_type = ?,
                        sign_reset_hour = ?,
                        sign_interval_hours = ?,
                        sign_gold_type = ?,
                        sign_gold_fixed = ?,
                        sign_gold_min = ?,
                        sign_gold_max = ?,
                        sign_gold_weight = ?
                    WHERE user_id = ?
                ''', (
                    current_settings['Binding_message_1'],
                    current_settings['Binding_message_2'],
                    current_settings['Binding_message_3'],
                    current_settings['sign_message_1'],
                    current_settings['sign_message_2'],
                    current_settings['sign_message_3'],
                    current_settings['balance_message_1'],
                    current_settings['balance_message_2'],
                    current_settings['online_players_message'],
                    current_settings['sign_reset_type'],
                    current_settings['sign_reset_hour'],
                    current_settings['sign_interval_hours'],
                    current_settings['sign_gold_type'],
                    current_settings['sign_gold_fixed'],
                    current_settings['sign_gold_min'],
                    current_settings['sign_gold_max'],
                    current_settings['sign_gold_weight'],
                    user_id
                ))
            else:
                cursor.execute('''
                    INSERT INTO qq_bot (user_id, Binding_message_1, Binding_message_2, Binding_message_3,
                        sign_message_1, sign_message_2, sign_message_3,
                        balance_message_1, balance_message_2, online_players_message,
                        sign_reset_type, sign_reset_hour, sign_interval_hours,
                        sign_gold_type, sign_gold_fixed, sign_gold_min, sign_gold_max, sign_gold_weight)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (
                    user_id,
                    settings.get('Binding_message_1', ''),
                    settings.get('Binding_message_2', ''),
                    settings.get('Binding_message_3', ''),
                    settings.get('sign_message_1', ''),
                    settings.get('sign_message_2', ''),
                    settings.get('sign_message_3', ''),
                    settings.get('balance_message_1', ''),
                    settings.get('balance_message_2', ''),
                    settings.get('online_players_message', ''),
                    settings.get('sign_reset_type', 'daily'),
                    settings.get('sign_reset_hour', 0),
                    settings.get('sign_interval_hours', 24),
                    settings.get('sign_gold_type', 'fixed'),
                    settings.get('sign_gold_fixed', 100),
                    settings.get('sign_gold_min', 1),
                    settings.get('sign_gold_max', 50),
                    settings.get('sign_gold_weight', 1.0)
                ))
            
            conn.commit()
            return {'success': True, 'message': 'QQ机器人设置保存成功'}
        except Exception as e:
            return {'success': False, 'message': f'保存失败: {str(e)}'}
        finally:
            conn.close()
    
    def get_qq_bot_by_group(self, qq_group):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT * FROM qq_bot WHERE qq_group = ?', (qq_group,))
            result = cursor.fetchone()
            if result:
                return {
                    'success': True,
                    'settings': {
                        'id': result[0],
                        'user_id': result[1],
                        'qq_group': result[2],
                        'group_binding_time': result[3],
                        'Binding_message_1': result[4] if len(result) > 4 else '',
                        'Binding_message_2': result[5] if len(result) > 5 else '',
                        'Binding_message_3': result[12] if len(result) > 12 else '',
                        'sign_message_1': result[6] if len(result) > 6 else '',
                        'sign_message_2': result[7] if len(result) > 7 else '',
                        'sign_message_3': result[8] if len(result) > 8 else '',
                        'balance_message_1': result[9] if len(result) > 9 else '',
                        'balance_message_2': result[10] if len(result) > 10 else '',
                        'online_players_message': result[11] if len(result) > 11 else '',
                        'sign_reset_type': result[13] if len(result) > 13 else 'daily',
                        'sign_reset_hour': result[14] if len(result) > 14 else 0,
                        'sign_interval_hours': result[15] if len(result) > 15 else 24,
                        'sign_gold_type': result[16] if len(result) > 16 else 'fixed',
                        'sign_gold_fixed': result[17] if len(result) > 17 else 100,
                        'sign_gold_min': result[18] if len(result) > 18 else 1,
                        'sign_gold_max': result[19] if len(result) > 19 else 50,
                        'sign_gold_weight': result[20] if len(result) > 20 else 1.0
                    }
                }
            return {'success': False, 'message': '未找到该QQ群的绑定设置'}
        finally:
            conn.close()
    
    def get_username_by_id(self, user_id):
        conn = self.get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT username FROM users WHERE id = ?', (user_id,))
            result = cursor.fetchone()
            if result:
                return {'success': True, 'username': result[0]}
            return {'success': False, 'message': '用户不存在'}
        finally:
            conn.close()


if __name__ == '__main__':
    db = DatabaseEditor()
    
    print("=== 数据库编辑器测试 ===\n")
    
    print("1. 创建管理员用户")
    result = db.create_user(
        username='admin',
        password='admin123',
        rcon_ip='127.0.0.1',
        rcon_password='rconpass',
        rcon_port=25575,
        is_admin=True
    )
    print(f"结果: {result}")
    
    print("\n2. 创建普通用户")
    result = db.create_user(
        username='user1',
        password='user123',
        rcon_ip='127.0.0.1',
        rcon_password='rconpass',
        rcon_port=25575
    )
    print(f"结果: {result}")
    
    print("\n3. 获取所有用户")
    result = db.get_all_users()
    print(f"用户数量: {result.get('count', len(result.get('users', [])))}")
    for user in result.get('users', []):
        print(f"  - {user['username']} (管理员: {user['is_admin']})")
    
    print("\n4. 创建邀请码")
    code = db.generate_invite_code()
    result = db.create_invite_code(code, max_uses=5, expire_days=30)
    print(f"结果: {result}")
    print(f"邀请码: {code}")
    
    print("\n5. 验证邀请码")
    is_valid = db.validate_invite_code(code)
    print(f"邀请码有效: {is_valid}")
    
    print("\n6. 设置普通用户为管理员")
    result = db.set_admin('user1', is_admin=True)
    print(f"结果: {result}")
    
    print("\n7. 再次获取用户信息")
    result = db.get_user('user1')
    if result['success']:
        print(f"用户: {result['user']['username']}, 管理员: {result['user']['is_admin']}")
    
    print("\n测试完成！")
