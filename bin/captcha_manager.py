import random
import string
import base64
from io import BytesIO
from datetime import datetime, timedelta
from pathlib import Path
import sqlite3

class CaptchaManager:
    def __init__(self):
        self.db_path = Path(__file__).parent.parent / 'user_database.db'
        self._ensure_database_structure()
    
    def _get_connection(self):
        return sqlite3.connect(self.db_path, timeout=30.0)
    
    def _ensure_database_structure(self):
        conn = self._get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS captcha_codes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                code TEXT NOT NULL,
                session_id TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NOT NULL,
                used INTEGER NOT NULL DEFAULT 0,
                UNIQUE(session_id)
            )
        ''')
        
        conn.commit()
        conn.close()
    
    def generate_captcha(self, session_id, length=4, expire_minutes=5):
        code = ''.join(random.choices(string.digits, k=length))
        created_at = datetime.now()
        expires_at = created_at + timedelta(minutes=expire_minutes)
        
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('''
                INSERT OR REPLACE INTO captcha_codes (code, session_id, created_at, expires_at, used)
                VALUES (?, ?, ?, ?, 0)
            ''', (code, session_id, created_at.strftime('%Y-%m-%d %H:%M:%S'), expires_at.strftime('%Y-%m-%d %H:%M:%S')))
            conn.commit()
            return code
        finally:
            conn.close()
    
    def verify_captcha(self, session_id, user_input):
        if not user_input:
            return False, '请输入验证码'
        
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('''
                SELECT code, expires_at, used FROM captcha_codes 
                WHERE session_id = ? 
                ORDER BY created_at DESC LIMIT 1
            ''', (session_id,))
            
            row = cursor.fetchone()
            
            if not row:
                return False, '验证码不存在或已过期'
            
            code, expires_at, used = row
            
            if used:
                return False, '验证码已使用'
            
            expires_time = datetime.strptime(expires_at, '%Y-%m-%d %H:%M:%S')
            if datetime.now() > expires_time:
                return False, '验证码已过期'
            
            if code != user_input.strip():
                return False, '验证码错误'
            
            cursor.execute('''
                UPDATE captcha_codes SET used = 1 WHERE session_id = ?
            ''', (session_id,))
            conn.commit()
            
            return True, '验证码正确'
        finally:
            conn.close()
    
    def cleanup_expired(self):
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
            cursor.execute('''
                DELETE FROM captcha_codes WHERE expires_at < ?
            ''', (current_time,))
            conn.commit()
        finally:
            conn.close()
