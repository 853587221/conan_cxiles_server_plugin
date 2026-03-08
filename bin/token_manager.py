import sqlite3
import hashlib
import os
from pathlib import Path
from typing import Optional, Dict, Any

class TokenManager:
    def __init__(self):
        self.db_path = Path(__file__).parent.parent / "user_database.db"
        self._ensure_database_structure()
    
    def _ensure_database_structure(self):
        """确保数据库结构存在"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS desktop_tokens (
                token TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP,
                FOREIGN KEY (username) REFERENCES users(username)
            )
        ''')
        
        conn.commit()
        conn.close()
    
    def _get_connection(self) -> sqlite3.Connection:
        """获取数据库连接"""
        return sqlite3.connect(self.db_path)
    
    def generate_token(self, username: str) -> str:
        """生成永久有效的token"""
        token_data = f"{username}:{os.urandom(32).hex()}"
        token = hashlib.sha256(token_data.encode()).hexdigest()
        
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('''
                INSERT INTO desktop_tokens (token, username, expires_at)
                VALUES (?, ?, NULL)
            ''', (token, username))
            conn.commit()
            return token
        except sqlite3.IntegrityError:
            return None
        finally:
            conn.close()
    
    def validate_token(self, token: str) -> Optional[str]:
        """验证token并返回用户名，如果token无效返回None"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT username, expires_at 
            FROM desktop_tokens 
            WHERE token = ?
        ''', (token,))
        
        result = cursor.fetchone()
        conn.close()
        
        if result:
            username, expires_at = result
            if expires_at is None:
                return username
        return None
    
    def get_user_tokens(self, username: str) -> list:
        """获取用户的所有token"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        cursor.execute('''
            SELECT token, created_at, expires_at 
            FROM desktop_tokens 
            WHERE username = ?
        ''', (username,))
        
        tokens = cursor.fetchall()
        conn.close()
        
        return [{
            'token': t[0],
            'created_at': t[1],
            'expires_at': t[2]
        } for t in tokens]
    
    def revoke_token(self, token: str) -> bool:
        """撤销token"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        cursor.execute('DELETE FROM desktop_tokens WHERE token = ?', (token,))
        conn.commit()
        affected = cursor.rowcount
        conn.close()
        
        return affected > 0
    
    def revoke_all_user_tokens(self, username: str) -> int:
        """撤销用户的所有token"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        cursor.execute('DELETE FROM desktop_tokens WHERE username = ?', (username,))
        conn.commit()
        affected = cursor.rowcount
        conn.close()
        
        return affected
