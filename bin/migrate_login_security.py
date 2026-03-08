import sqlite3
from pathlib import Path

def migrate_database():
    db_path = Path(__file__).parent.parent / 'user_database.db'
    
    if not db_path.exists():
        print(f"数据库文件不存在: {db_path}")
        return
    
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    try:
        cursor.execute("PRAGMA table_info(users)")
        columns = [column[1] for column in cursor.fetchall()]
        
        if 'login_fail_count' not in columns:
            print("添加登录失败相关字段...")
            cursor.execute('ALTER TABLE users ADD COLUMN login_fail_count INTEGER NOT NULL DEFAULT 0')
            cursor.execute('ALTER TABLE users ADD COLUMN last_fail_time TEXT')
            cursor.execute('ALTER TABLE users ADD COLUMN locked_until TEXT')
            conn.commit()
            print("数据库迁移成功!")
        else:
            print("数据库已经是最新版本，无需迁移")
            
    except Exception as e:
        conn.rollback()
        print(f"数据库迁移失败: {str(e)}")
    finally:
        conn.close()

if __name__ == '__main__':
    migrate_database()
