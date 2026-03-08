#!/usr/bin/env python3
import sqlite3
from pathlib import Path

def migrate_add_valid_until():
    db_path = Path(__file__).parent.parent / 'user_database.db'
    
    print(f"数据库路径: {db_path}")
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    try:
        cursor.execute("PRAGMA table_info(users)")
        columns = [column[1] for column in cursor.fetchall()]
        
        if 'valid_until' in columns:
            print("✓ valid_until 字段已存在，无需迁移")
            return
        
        print("添加 valid_until 字段...")
        cursor.execute('''
            ALTER TABLE users ADD COLUMN valid_until TEXT
        ''')
        
        conn.commit()
        print("✓ 成功添加 valid_until 字段")
        
        cursor.execute("SELECT COUNT(*) FROM users")
        user_count = cursor.fetchone()[0]
        print(f"✓ 数据库中有 {user_count} 个用户")
        
    except Exception as e:
        print(f"✗ 迁移失败: {str(e)}")
        conn.rollback()
        raise
    finally:
        conn.close()

if __name__ == '__main__':
    migrate_add_valid_until()
