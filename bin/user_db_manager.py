import sqlite3
import time
from pathlib import Path
from typing import Dict, List, Optional, Any

class UserDBManager:
    def __init__(self, username: str):
        self.username = username
        self.db_path = self._get_user_db_path()
        self._ensure_database_structure()
    
    def _get_user_db_path(self) -> Path:
        """获取用户数据库路径"""
        return Path(__file__).parent.parent / f"data/{self.username}/database.db"
    
    def _ensure_database_structure(self) -> None:
        """确保数据库结构存在"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        # 创建分类表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                description TEXT
            )
        ''')
        
        # 创建命令表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS commands (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT,
                category TEXT NOT NULL,
                example TEXT NOT NULL,
                UNIQUE(name, category)
            )
        ''')
        
        # 创建索引
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_commands_name ON commands(name)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_commands_category ON commands(category)')
        
        # 创建玩家表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                idx TEXT,
                char_name TEXT NOT NULL,
                player_name TEXT,
                user_id TEXT,
                platform_id TEXT,
                platform_name TEXT,
                level INTEGER DEFAULT 1,
                status TEXT DEFAULT 'offline',
                last_seen REAL,
                created_at REAL,
                online_time INTEGER DEFAULT 0,
                permission_level INTEGER DEFAULT 0,
                gold REAL DEFAULT 0.0,
                UNIQUE(user_id, platform_id)
            )
        ''')
        
        # 创建聊天消息表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_id INTEGER NOT NULL,
                char_name TEXT,
                message TEXT NOT NULL,
                timestamp REAL,
                FOREIGN KEY (player_id) REFERENCES players(id)
            )
        ''')
        
        # 创建自动触发规则表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS auto_trigger_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                rule_name TEXT,
                conditions TEXT NOT NULL,
                execute_type TEXT NOT NULL,
                execute_data TEXT NOT NULL,
                after_execute TEXT,
                enabled BOOLEAN DEFAULT 1,
                created_at REAL DEFAULT CURRENT_TIMESTAMP,
                updated_at REAL DEFAULT CURRENT_TIMESTAMP
            )
        ''')
        
        # 创建商品分类表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS product_categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key TEXT NOT NULL UNIQUE,
                name TEXT NOT NULL,
                icon TEXT,
                sort_order INTEGER DEFAULT 0
            )
        ''')
        
        # 创建商品表
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                image TEXT,
                description TEXT,
                category_key TEXT NOT NULL,
                price INTEGER NOT NULL,
                sort_order INTEGER DEFAULT 0,
                created_at REAL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (category_key) REFERENCES product_categories(key)
            )
        ''')
        
        # 创建商品索引
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_key)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_products_name ON products(name)')
        
        # 创建索引
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_players_user_id ON players(user_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_players_platform_id ON players(platform_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_players_char_name ON players(char_name)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_chat_messages_player_id ON chat_messages(player_id)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_chat_messages_timestamp ON chat_messages(timestamp)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_auto_trigger_rules_enabled ON auto_trigger_rules(enabled)')
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_product_categories_key ON product_categories(key)')
        
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS ai_service (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT,
                key TEXT,
                name TEXT,
                Prompter TEXT,
                keyword TEXT,
                enabled BOOLEAN DEFAULT 0
            )
        ''')
        
        cursor.execute('CREATE INDEX IF NOT EXISTS idx_ai_service_enabled ON ai_service(enabled)')
        
        # 数据库迁移：添加 online_time 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN online_time INTEGER DEFAULT 0')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 permission_level 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN permission_level INTEGER DEFAULT 0')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 gold 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN gold REAL DEFAULT 0.0')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：将 gold 列转换为 REAL（SQLite 动态类型，无需显式转换）
        # 注意：SQLite 实际上允许将 REAL 存储到 INTEGER 列，读取时会自动转换
        try:
            cursor.execute('UPDATE players SET gold = CAST(gold AS REAL)')
            conn.commit()
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 rule_name 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE auto_trigger_rules ADD COLUMN rule_name TEXT')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：将 products 表的 icon 列重命名为 image 列
        try:
            cursor.execute('ALTER TABLE products RENAME COLUMN icon TO image')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 spawn_point 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN spawn_point TEXT')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 guild_name 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN guild_name TEXT')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 month_card 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN month_card INTEGER DEFAULT 0')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 secondary_conditions 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE auto_trigger_rules ADD COLUMN secondary_conditions TEXT')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 monthly_card_expiry 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN monthly_card_expiry REAL DEFAULT 0')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 qq_member 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN qq_member TEXT')
        except sqlite3.OperationalError:
            pass
        
        # 数据库迁移：添加 qq_binding_time 列（如果不存在）
        try:
            cursor.execute('ALTER TABLE players ADD COLUMN qq_binding_time REAL')
        except sqlite3.OperationalError:
            pass
        
        conn.commit()
        conn.close()
    
    def _get_connection(self) -> sqlite3.Connection:
        """获取数据库连接"""
        return sqlite3.connect(self.db_path)
    
    # 分类管理方法
    def get_categories(self) -> List[Dict[str, Any]]:
        """获取所有分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, name, description FROM categories ORDER BY id')
        categories = cursor.fetchall()
        conn.close()
        return [{'id': cat[0], 'name': cat[1], 'description': cat[2]} for cat in categories]
    
    def get_category(self, category_id: int) -> Optional[Dict[str, Any]]:
        """根据ID获取分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, name, description FROM categories WHERE id = ?', (category_id,))
        category = cursor.fetchone()
        conn.close()
        if category:
            return {'id': category[0], 'name': category[1], 'description': category[2]}
        return None
    
    def create_category(self, name: str, description: str = '') -> Dict[str, Any]:
        """创建新分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('INSERT INTO categories (name, description) VALUES (?, ?)', (name, description))
            conn.commit()
            category_id = cursor.lastrowid
            return {'success': True, 'message': '分类创建成功', 'id': category_id}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '分类名称已存在'}
        finally:
            conn.close()
    
    def update_category(self, category_id: int, name: str, description: str = '') -> Dict[str, Any]:
        """更新分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('UPDATE categories SET name = ?, description = ? WHERE id = ?', (name, description, category_id))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '分类更新成功'}
            return {'success': False, 'message': '分类不存在'}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '分类名称已存在'}
        finally:
            conn.close()
    
    def delete_category(self, category_id: int) -> Dict[str, Any]:
        """删除分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            # 先获取分类名称，用于删除该分类下的所有命令
            cursor.execute('SELECT name FROM categories WHERE id = ?', (category_id,))
            category = cursor.fetchone()
            if not category:
                return {'success': False, 'message': '分类不存在'}
            
            category_name = category[0]
            
            # 删除该分类下的所有命令
            cursor.execute('DELETE FROM commands WHERE category = ?', (category_name,))
            
            # 删除分类
            cursor.execute('DELETE FROM categories WHERE id = ?', (category_id,))
            conn.commit()
            
            return {'success': True, 'message': '分类和相关命令已删除'}
        finally:
            conn.close()
    
    # 命令管理方法
    def get_commands(self, category: Optional[str] = None) -> List[Dict[str, Any]]:
        """获取命令，可以按分类过滤"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        if category:
            cursor.execute('SELECT id, name, description, category, example FROM commands WHERE category = ? ORDER BY name', (category,))
        else:
            cursor.execute('SELECT id, name, description, category, example FROM commands ORDER BY category, name')
        
        commands = cursor.fetchall()
        conn.close()
        return [{
            'id': cmd[0],
            'name': cmd[1],
            'description': cmd[2],
            'category': cmd[3],
            'example': cmd[4]
        } for cmd in commands]
    
    def get_command(self, command_id: int) -> Optional[Dict[str, Any]]:
        """根据ID获取命令"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, name, description, category, example FROM commands WHERE id = ?', (command_id,))
        command = cursor.fetchone()
        conn.close()
        if command:
            return {
                'id': command[0],
                'name': command[1],
                'description': command[2],
                'category': command[3],
                'example': command[4]
            }
        return None
    
    def get_command_by_example(self, example: str) -> Optional[Dict[str, Any]]:
        """根据example获取命令"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, name, description, category, example FROM commands WHERE example = ?', (example,))
        command = cursor.fetchone()
        conn.close()
        if command:
            return {
                'id': command[0],
                'name': command[1],
                'description': command[2],
                'category': command[3],
                'example': command[4]
            }
        return None
    
    def create_command(self, name: str, description: str, category: str, example: str) -> Dict[str, Any]:
        """创建新命令"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''
                INSERT INTO commands (name, description, category, example)
                VALUES (?, ?, ?, ?)
            ''', (name, description, category, example))
            conn.commit()
            command_id = cursor.lastrowid
            return {'success': True, 'message': '命令创建成功', 'id': command_id}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '该分类下已存在同名命令'}
        finally:
            conn.close()
    
    def update_command(self, command_id: int, name: str, description: str, category: str, example: str) -> Dict[str, Any]:
        """更新命令"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''
                UPDATE commands 
                SET name = ?, description = ?, category = ?, example = ? 
                WHERE id = ?
            ''', (name, description, category, example, command_id))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '命令更新成功'}
            return {'success': False, 'message': '命令不存在'}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '该分类下已存在同名命令'}
        finally:
            conn.close()
    
    def delete_command(self, command_id: int) -> Dict[str, Any]:
        """删除命令"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('DELETE FROM commands WHERE id = ?', (command_id,))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '命令已删除'}
            return {'success': False, 'message': '命令不存在'}
        finally:
            conn.close()
    
    def search_commands(self, keyword: str) -> List[Dict[str, Any]]:
        """根据关键词搜索命令"""
        conn = self._get_connection()
        cursor = conn.cursor()
        search_term = f'%{keyword}%'
        cursor.execute('''
            SELECT id, name, description, category, example 
            FROM commands 
            WHERE name LIKE ? OR description LIKE ? OR example LIKE ? 
            ORDER BY category, name
        ''', (search_term, search_term, search_term))
        commands = cursor.fetchall()
        conn.close()
        return [{
            'id': cmd[0],
            'name': cmd[1],
            'description': cmd[2],
            'category': cmd[3],
            'example': cmd[4]
        } for cmd in commands]
    
    def get_commands_by_category(self, category: str) -> List[Dict[str, Any]]:
        """根据分类获取命令"""
        return self.get_commands(category)
    
    # 玩家管理方法
    def update_or_create_player(self, player_info: Dict[str, Any], level: int = 1, online_time: int = 0, spawn_point: str = None, guild_name: str = None) -> Dict[str, Any]:
        """更新或创建玩家记录"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        idx = player_info.get('Idx', '')
        char_name = player_info.get('Char_name', '')
        player_name = player_info.get('Player_name', '')
        user_id = player_info.get('User_ID', '')
        platform_id = player_info.get('Platform_ID', '')
        platform_name = player_info.get('Platform_Name', '')
        
        current_time = time.time()
        
        try:
            # 先查询现有记录的在线时间
            cursor.execute('''
                SELECT online_time FROM players WHERE user_id = ? AND platform_id = ?
            ''', (user_id, platform_id))
            existing_player = cursor.fetchone()
            
            # 如果传入的在线时间为0或无效，使用数据库中的现有值
            if online_time <= 0 and existing_player:
                online_time = existing_player[0] or 0
            
            # 尝试更新现有记录
            cursor.execute('''
                UPDATE players 
                SET idx = ?, char_name = ?, player_name = ?, platform_name = ?, level = ?, last_seen = ?, online_time = ?, spawn_point = ?, guild_name = ?
                WHERE user_id = ? AND platform_id = ?
            ''', (idx, char_name, player_name, platform_name, level, current_time, online_time, spawn_point, guild_name, user_id, platform_id))
            
            if cursor.rowcount > 0:
                conn.commit()
                return {'success': True, 'message': '玩家信息已更新', 'action': 'updated'}
            
            # 如果没有找到记录，创建新记录
            cursor.execute('''
                INSERT INTO players (idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, spawn_point, guild_name)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'online', ?, ?, ?, ?, ?)
            ''', (idx, char_name, player_name, user_id, platform_id, platform_name, level, current_time, current_time, online_time, spawn_point, guild_name))
            
            conn.commit()
            return {'success': True, 'message': '玩家记录已创建', 'action': 'created'}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '玩家记录已存在'}
        finally:
            conn.close()
    
    def update_player_status(self, user_id: str, platform_id: str, status: str) -> Dict[str, Any]:
        """更新玩家状态"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        current_time = time.time()
        
        try:
            cursor.execute('''
                UPDATE players 
                SET status = ?, last_seen = ?
                WHERE user_id = ? AND platform_id = ?
            ''', (status, current_time, user_id, platform_id))
            
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '玩家状态已更新'}
            return {'success': False, 'message': '玩家不存在'}
        finally:
            conn.close()
    
    def add_chat_message(self, player_info: Dict[str, Any], message: str) -> Dict[str, Any]:
        """添加聊天消息"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        user_id = player_info.get('User_ID', '')
        platform_id = player_info.get('Platform_ID', '')
        char_name = player_info.get('Char_name', '')
        current_time = time.time()
        
        try:
            # 获取玩家ID
            cursor.execute('''
                SELECT id FROM players WHERE user_id = ? AND platform_id = ?
            ''', (user_id, platform_id))
            
            player = cursor.fetchone()
            if not player:
                return {'success': False, 'message': '玩家不存在'}
            
            player_id = player[0]
            
            # 添加聊天消息
            cursor.execute('''
                INSERT INTO chat_messages (player_id, char_name, message, timestamp)
                VALUES (?, ?, ?, ?)
            ''', (player_id, char_name, message, current_time))
            
            conn.commit()
            return {'success': True, 'message': '聊天消息已添加'}
        finally:
            conn.close()
    
    def get_player_by_user_id(self, user_id: str) -> Optional[Dict[str, Any]]:
        """根据user_id获取玩家"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, permission_level, gold, spawn_point, guild_name, monthly_card_expiry, qq_member, qq_binding_time
            FROM players WHERE user_id = ?
        ''', (user_id,))
        player = cursor.fetchone()
        conn.close()
        if player:
            return {
                'id': player[0],
                'idx': player[1],
                'char_name': player[2],
                'player_name': player[3],
                'user_id': player[4],
                'platform_id': player[5],
                'platform_name': player[6],
                'level': player[7],
                'status': player[8],
                'last_seen': player[9],
                'created_at': player[10],
                'online_time': player[11],
                'permission_level': player[12],
                'gold': player[13],
                'spawn_point': player[14],
                'guild_name': player[15],
                'monthly_card_expiry': player[16] if player[16] else 0,
                'qq_member': player[17],
                'qq_binding_time': player[18]
            }
        return None
    
    def get_player_by_ids(self, user_id: str = None, platform_id: str = None) -> Optional[Dict[str, Any]]:
        """根据user_id或platform_id获取玩家"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        if user_id:
            cursor.execute('''
                SELECT id, idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, permission_level, gold, spawn_point, guild_name, monthly_card_expiry, qq_member, qq_binding_time
                FROM players WHERE user_id = ?
            ''', (user_id,))
        elif platform_id:
            cursor.execute('''
                SELECT id, idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, permission_level, gold, spawn_point, guild_name, monthly_card_expiry, qq_member, qq_binding_time
                FROM players WHERE platform_id = ?
            ''', (platform_id,))
        else:
            conn.close()
            return None
        
        player = cursor.fetchone()
        conn.close()
        if player:
            return {
                'id': player[0],
                'idx': player[1],
                'char_name': player[2],
                'player_name': player[3],
                'user_id': player[4],
                'platform_id': player[5],
                'platform_name': player[6],
                'level': player[7],
                'status': player[8],
                'last_seen': player[9],
                'created_at': player[10],
                'online_time': player[11],
                'permission_level': player[12],
                'gold': player[13],
                'spawn_point': player[14],
                'guild_name': player[15],
                'monthly_card_expiry': player[16] if player[16] else 0,
                'qq_member': player[17],
                'qq_binding_time': player[18]
            }
        return None
    
    def get_player_by_platform_id(self, platform_id: str) -> Optional[Dict[str, Any]]:
        """根据platform_id获取玩家"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, permission_level, gold, spawn_point, guild_name, monthly_card_expiry, qq_member, qq_binding_time
            FROM players WHERE platform_id = ?
        ''', (platform_id,))
        player = cursor.fetchone()
        conn.close()
        if player:
            return {
                'id': player[0],
                'idx': player[1],
                'char_name': player[2],
                'player_name': player[3],
                'user_id': player[4],
                'platform_id': player[5],
                'platform_name': player[6],
                'level': player[7],
                'status': player[8],
                'last_seen': player[9],
                'created_at': player[10],
                'online_time': player[11],
                'permission_level': player[12],
                'gold': player[13],
                'spawn_point': player[14],
                'guild_name': player[15],
                'monthly_card_expiry': player[16] if player[16] else 0,
                'qq_member': player[17],
                'qq_binding_time': player[18]
            }
        return None
    
    def get_player_by_char_name(self, char_name: str) -> Optional[Dict[str, Any]]:
        """根据角色名获取玩家"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, permission_level, gold, spawn_point, guild_name, monthly_card_expiry, qq_member, qq_binding_time
            FROM players WHERE char_name = ?
        ''', (char_name,))
        player = cursor.fetchone()
        conn.close()
        if player:
            return {
                'id': player[0],
                'idx': player[1],
                'char_name': player[2],
                'player_name': player[3],
                'user_id': player[4],
                'platform_id': player[5],
                'platform_name': player[6],
                'level': player[7],
                'status': player[8],
                'last_seen': player[9],
                'created_at': player[10],
                'online_time': player[11],
                'permission_level': player[12],
                'gold': player[13],
                'spawn_point': player[14],
                'guild_name': player[15],
                'monthly_card_expiry': player[16] if player[16] else 0,
                'qq_member': player[17],
                'qq_binding_time': player[18]
            }
        return None
    
    def get_player_by_qq_member(self, qq_member: str) -> Optional[Dict[str, Any]]:
        """根据QQ成员ID获取玩家"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, permission_level, gold, tag, amount, spawn_point, guild_name, month_card, monthly_card_expiry, qq_member, qq_binding_time, sign_time, sign_records
            FROM players WHERE qq_member = ?
        ''', (qq_member,))
        player = cursor.fetchone()
        conn.close()
        if player:
            return {
                'id': player[0],
                'idx': player[1],
                'char_name': player[2],
                'player_name': player[3],
                'user_id': player[4],
                'platform_id': player[5],
                'platform_name': player[6],
                'level': player[7],
                'status': player[8],
                'last_seen': player[9],
                'created_at': player[10],
                'online_time': player[11],
                'permission_level': player[12],
                'gold': player[13],
                'tag': player[14],
                'amount': player[15],
                'spawn_point': player[16],
                'guild_name': player[17],
                'month_card': player[18],
                'monthly_card_expiry': player[19] if player[19] else 0,
                'qq_member': player[20],
                'qq_binding_time': player[21],
                'sign_time': player[22] if len(player) > 22 and player[22] else 0,
                'sign_records': player[23] if len(player) > 23 else ''
            }
        return None
    
    def get_all_players(self) -> List[Dict[str, Any]]:
        """获取所有玩家"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, idx, char_name, player_name, user_id, platform_id, platform_name, level, status, last_seen, created_at, online_time, permission_level, gold, tag, amount, spawn_point, guild_name, month_card, monthly_card_expiry, qq_member, qq_binding_time, sign_time, sign_records
            FROM players ORDER BY last_seen DESC
        ''')
        players = cursor.fetchall()
        conn.close()
        return [{
            'id': p[0],
            'idx': p[1],
            'char_name': p[2],
            'player_name': p[3],
            'user_id': p[4],
            'platform_id': p[5],
            'platform_name': p[6],
            'level': p[7],
            'status': p[8],
            'last_seen': p[9],
            'created_at': p[10],
            'online_time': p[11],
            'permission_level': p[12],
            'gold': p[13],
            'tag': p[14],
            'amount': p[15],
            'spawn_point': p[16],
            'guild_name': p[17],
            'month_card': p[18],
            'monthly_card_expiry': p[19] if p[19] else 0,
            'qq_member': p[20],
            'qq_binding_time': p[21],
            'sign_time': p[22] if len(p) > 22 else 0,
            'sign_records': p[23] if len(p) > 23 else ''
        } for p in players]
    
    def update_player_permission_level(self, user_id: str, platform_id: str, permission_level: int) -> Dict[str, Any]:
        """更新玩家权限等级"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('''
                UPDATE players 
                SET permission_level = ?
                WHERE user_id = ? AND platform_id = ?
            ''', (permission_level, user_id, platform_id))
            
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '玩家权限等级已更新'}
            return {'success': False, 'message': '玩家不存在'}
        finally:
            conn.close()
    
    def update_player_gold(self, user_id: str, platform_id: str, gold: float) -> Dict[str, Any]:
        """更新玩家金币"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('''
                UPDATE players 
                SET gold = ?
                WHERE user_id = ? AND platform_id = ?
            ''', (round(float(gold), 1), user_id, platform_id))
            
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '玩家金币已更新'}
            return {'success': False, 'message': '玩家不存在'}
        finally:
            conn.close()
    
    def update_player_fields(self, player_id: int, permission_level: int = None, gold: float = None, spawn_point: str = None, guild_name: str = None, monthly_card_expiry: float = None, qq_member: str = None, qq_binding_time: float = None, sign_time: float = None, sign_records: str = None) -> Dict[str, Any]:
        """更新玩家字段（根据player_id）"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            update_fields = []
            update_values = []
            
            if permission_level is not None:
                update_fields.append('permission_level = ?')
                update_values.append(permission_level)
            
            if gold is not None:
                update_fields.append('gold = ?')
                update_values.append(round(float(gold), 1))
            
            if spawn_point is not None:
                update_fields.append('spawn_point = ?')
                update_values.append(spawn_point)
            
            if guild_name is not None:
                update_fields.append('guild_name = ?')
                update_values.append(guild_name)
            
            if monthly_card_expiry is not None:
                update_fields.append('monthly_card_expiry = ?')
                update_values.append(monthly_card_expiry)
            
            if qq_member is not None:
                update_fields.append('qq_member = ?')
                update_values.append(qq_member)
            
            if qq_binding_time is not None:
                update_fields.append('qq_binding_time = ?')
                update_values.append(qq_binding_time)
            
            if sign_time is not None:
                update_fields.append('sign_time = ?')
                update_values.append(sign_time)
            
            if sign_records is not None:
                update_fields.append('sign_records = ?')
                update_values.append(sign_records)
            
            if not update_fields:
                return {'success': False, 'message': '没有需要更新的字段'}
            
            update_values.append(player_id)
            
            cursor.execute(f'''
                UPDATE players 
                SET {', '.join(update_fields)}
                WHERE id = ?
            ''', update_values)
            
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '玩家信息已更新'}
            return {'success': False, 'message': '玩家不存在'}
        finally:
            conn.close()
    
    def delete_player(self, player_id: int) -> Dict[str, Any]:
        """删除玩家记录（仅删除players表中的记录）"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('SELECT char_name FROM players WHERE id = ?', (player_id,))
            player = cursor.fetchone()
            
            if not player:
                return {'success': False, 'message': '玩家不存在'}
            
            char_name = player[0]
            
            cursor.execute('DELETE FROM players WHERE id = ?', (player_id,))
            
            conn.commit()
            return {'success': True, 'message': f'玩家 "{char_name}" 已删除'}
        except Exception as e:
            conn.rollback()
            return {'success': False, 'message': f'删除失败: {str(e)}'}
        finally:
            conn.close()
    
    def get_chat_messages(self, offset: int = 0, limit: int = 10) -> List[Dict[str, Any]]:
        """获取聊天消息（分页，从最新开始）"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, player_id, char_name, message, timestamp
            FROM chat_messages 
            ORDER BY timestamp DESC 
            LIMIT ? OFFSET ?
        ''', (limit, offset))
        messages = cursor.fetchall()
        conn.close()
        return [{
            'id': m[0],
            'player_id': m[1],
            'char_name': m[2],
            'message': m[3],
            'timestamp': m[4]
        } for m in messages]
    
    def get_chat_messages_count(self) -> int:
        """获取聊天消息总数"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT COUNT(*) FROM chat_messages')
        count = cursor.fetchone()[0]
        conn.close()
        return count
    
    def cleanup_old_chat_messages(self, keep_count: int = 500) -> Dict[str, Any]:
        """清理旧的聊天记录，只保留最新的指定数量
        
        Args:
            keep_count: 保留的最新消息数量，默认500条
            
        Returns:
            dict: 包含 success, message, deleted_count 等信息
        """
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('SELECT COUNT(*) FROM chat_messages')
            total_count = cursor.fetchone()[0]
            
            if total_count <= keep_count:
                return {
                    'success': True,
                    'message': f'聊天记录数量({total_count})未超过保留数量({keep_count})，无需清理',
                    'deleted_count': 0,
                    'total_count': total_count
                }
            
            cursor.execute('''
                DELETE FROM chat_messages 
                WHERE id NOT IN (
                    SELECT id FROM chat_messages 
                    ORDER BY timestamp DESC 
                    LIMIT ?
                )
            ''', (keep_count,))
            
            deleted_count = cursor.rowcount
            conn.commit()
            
            cursor.execute('SELECT COUNT(*) FROM chat_messages')
            remaining_count = cursor.fetchone()[0]
            
            cursor.execute('VACUUM')
            
            return {
                'success': True,
                'message': f'已清理 {deleted_count} 条旧聊天记录，保留最新 {remaining_count} 条',
                'deleted_count': deleted_count,
                'total_count': remaining_count
            }
        except Exception as e:
            conn.rollback()
            return {
                'success': False,
                'message': f'清理聊天记录失败: {str(e)}',
                'deleted_count': 0
            }
        finally:
            conn.close()
    
    # 自动触发规则管理方法
    def create_auto_trigger_rule(self, rule_name: str, conditions: List[Dict[str, Any]], 
                                   execute_type: str, execute_data: Dict[str, Any],
                                   after_execute: Optional[Dict[str, Any]] = None,
                                   secondary_conditions: Optional[List[Dict[str, Any]]] = None,
                                   enabled: bool = True) -> Dict[str, Any]:
        """创建自动触发规则"""
        import json
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            current_time = time.time()
            cursor.execute('''
                INSERT INTO auto_trigger_rules (rule_name, conditions, execute_type, execute_data, after_execute, secondary_conditions, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                rule_name,
                json.dumps(conditions, ensure_ascii=False),
                execute_type,
                json.dumps(execute_data, ensure_ascii=False),
                json.dumps(after_execute, ensure_ascii=False) if after_execute else None,
                json.dumps(secondary_conditions, ensure_ascii=False) if secondary_conditions else None,
                enabled,
                current_time,
                current_time
            ))
            conn.commit()
            rule_id = cursor.lastrowid
            return {'success': True, 'message': '自动触发规则创建成功', 'id': rule_id}
        except Exception as e:
            return {'success': False, 'message': f'创建失败: {str(e)}'}
        finally:
            conn.close()
    
    def get_auto_trigger_rules(self, enabled_only: bool = False) -> List[Dict[str, Any]]:
        """获取自动触发规则"""
        import json
        conn = self._get_connection()
        cursor = conn.cursor()
        
        if enabled_only:
            cursor.execute('''
                SELECT id, rule_name, conditions, execute_type, execute_data, after_execute, secondary_conditions, enabled, created_at, updated_at
                FROM auto_trigger_rules 
                WHERE enabled = 1
                ORDER BY created_at DESC
            ''')
        else:
            cursor.execute('''
                SELECT id, rule_name, conditions, execute_type, execute_data, after_execute, secondary_conditions, enabled, created_at, updated_at
                FROM auto_trigger_rules 
                ORDER BY enabled DESC, created_at DESC
            ''')
        
        rules = cursor.fetchall()
        conn.close()
        
        return [{
            'id': rule[0],
            'rule_name': rule[1],
            'conditions': json.loads(rule[2]),
            'execute_type': rule[3],
            'execute_data': json.loads(rule[4]),
            'after_execute': json.loads(rule[5]) if rule[5] else None,
            'secondary_conditions': json.loads(rule[6]) if rule[6] else None,
            'enabled': bool(rule[7]),
            'created_at': rule[8],
            'updated_at': rule[9]
        } for rule in rules]
    
    def get_auto_trigger_rule(self, rule_id: int) -> Optional[Dict[str, Any]]:
        """根据ID获取自动触发规则"""
        import json
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT id, rule_name, conditions, execute_type, execute_data, after_execute, secondary_conditions, enabled, created_at, updated_at
            FROM auto_trigger_rules 
            WHERE id = ?
        ''', (rule_id,))
        rule = cursor.fetchone()
        conn.close()
        
        if rule:
            return {
                'id': rule[0],
                'rule_name': rule[1],
                'conditions': json.loads(rule[2]),
                'execute_type': rule[3],
                'execute_data': json.loads(rule[4]),
                'after_execute': json.loads(rule[5]) if rule[5] else None,
                'secondary_conditions': json.loads(rule[6]) if rule[6] else None,
                'enabled': bool(rule[7]),
                'created_at': rule[8],
                'updated_at': rule[9]
            }
        return None
    
    def update_auto_trigger_rule(self, rule_id: int, rule_name: str, conditions: List[Dict[str, Any]],
                                  execute_type: str, execute_data: Dict[str, Any],
                                  after_execute: Optional[Dict[str, Any]] = None,
                                  secondary_conditions: Optional[List[Dict[str, Any]]] = None,
                                  enabled: bool = True) -> Dict[str, Any]:
        """更新自动触发规则"""
        import json
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            current_time = time.time()
            cursor.execute('''
                UPDATE auto_trigger_rules 
                SET rule_name = ?, conditions = ?, execute_type = ?, execute_data = ?, after_execute = ?, secondary_conditions = ?, enabled = ?, updated_at = ?
                WHERE id = ?
            ''', (
                rule_name,
                json.dumps(conditions, ensure_ascii=False),
                execute_type,
                json.dumps(execute_data, ensure_ascii=False),
                json.dumps(after_execute, ensure_ascii=False) if after_execute else None,
                json.dumps(secondary_conditions, ensure_ascii=False) if secondary_conditions else None,
                enabled,
                current_time,
                rule_id
            ))
            conn.commit()
            
            if cursor.rowcount > 0:
                return {'success': True, 'message': '自动触发规则更新成功'}
            return {'success': False, 'message': '规则不存在'}
        except Exception as e:
            return {'success': False, 'message': f'更新失败: {str(e)}'}
        finally:
            conn.close()
    
    def delete_auto_trigger_rule(self, rule_id: int) -> Dict[str, Any]:
        """删除自动触发规则"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('DELETE FROM auto_trigger_rules WHERE id = ?', (rule_id,))
            conn.commit()
            
            if cursor.rowcount > 0:
                return {'success': True, 'message': '自动触发规则已删除'}
            return {'success': False, 'message': '规则不存在'}
        finally:
            conn.close()
    
    def toggle_auto_trigger_rule(self, rule_id: int) -> Dict[str, Any]:
        """启用/禁用自动触发规则"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            # 先查询当前状态
            cursor.execute('SELECT enabled FROM auto_trigger_rules WHERE id = ?', (rule_id,))
            result = cursor.fetchone()
            
            if not result:
                return {'success': False, 'message': '规则不存在'}
            
            current_enabled = bool(result[0])
            new_enabled = not current_enabled
            
            # 更新状态
            cursor.execute('''
                UPDATE auto_trigger_rules 
                SET enabled = ?, updated_at = ?
                WHERE id = ?
            ''', (1 if new_enabled else 0, time.time(), rule_id))
            conn.commit()
            
            return {'success': True, 'message': '规则状态已更新'}
        finally:
            conn.close()
    
    def get_product_categories(self) -> List[Dict[str, Any]]:
        """获取所有商品分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''
            SELECT c.id, c.key, c.name, c.icon, c.sort_order, COUNT(p.id) as product_count
            FROM product_categories c
            LEFT JOIN products p ON c.key = p.category_key
            GROUP BY c.id, c.key, c.name, c.icon, c.sort_order
            ORDER BY c.sort_order, c.id
        ''')
        categories = cursor.fetchall()
        conn.close()
        return [{'id': cat[0], 'key': cat[1], 'name': cat[2], 'icon': cat[3], 'sort_order': cat[4], 'product_count': cat[5]} for cat in categories]
    
    def get_product_category(self, category_key: str) -> Optional[Dict[str, Any]]:
        """根据key获取商品分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, key, name, icon, sort_order FROM product_categories WHERE key = ?', (category_key,))
        category = cursor.fetchone()
        conn.close()
        if category:
            return {'id': category[0], 'key': category[1], 'name': category[2], 'icon': category[3], 'sort_order': category[4]}
        return None
    
    def create_product_category(self, key: str, name: str, icon: str = '', sort_order: int = 0) -> Dict[str, Any]:
        """创建商品分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('INSERT INTO product_categories (key, name, icon, sort_order) VALUES (?, ?, ?, ?)', 
                          (key, name, icon, sort_order))
            conn.commit()
            category_id = cursor.lastrowid
            return {'success': True, 'message': '商品分类创建成功', 'id': category_id}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '分类key已存在'}
        finally:
            conn.close()
    
    def update_product_category(self, category_id: int, key: str, name: str, icon: str = '', sort_order: int = 0) -> Dict[str, Any]:
        """更新商品分类"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT key FROM product_categories WHERE id = ?', (category_id,))
            old_category = cursor.fetchone()
            if not old_category:
                return {'success': False, 'message': '商品分类不存在'}
            
            old_key = old_category[0]
            
            cursor.execute('UPDATE product_categories SET key = ?, name = ?, icon = ?, sort_order = ? WHERE id = ?',
                          (key, name, icon, sort_order, category_id))
            
            if old_key != key:
                cursor.execute('UPDATE products SET category_key = ? WHERE category_key = ?', (key, old_key))
            
            conn.commit()
            return {'success': True, 'message': '商品分类更新成功'}
        except sqlite3.IntegrityError:
            return {'success': False, 'message': '分类key已存在'}
        finally:
            conn.close()
    
    def delete_product_category(self, category_id: int) -> Dict[str, Any]:
        """删除商品分类及其所有商品"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('SELECT key FROM product_categories WHERE id = ?', (category_id,))
            category = cursor.fetchone()
            if not category:
                return {'success': False, 'message': '商品分类不存在'}
            
            category_key = category[0]
            cursor.execute('DELETE FROM products WHERE category_key = ?', (category_key,))
            cursor.execute('DELETE FROM product_categories WHERE id = ?', (category_id,))
            conn.commit()
            
            return {'success': True, 'message': '商品分类和相关商品已删除'}
        finally:
            conn.close()
    
    def get_products(self, category_key: Optional[str] = None) -> List[Dict[str, Any]]:
        """获取商品，可以按分类过滤"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        if category_key:
            cursor.execute('''SELECT id, name, image, description, category_key, price, sort_order, created_at 
                            FROM products WHERE category_key = ? ORDER BY sort_order, id''', (category_key,))
        else:
            cursor.execute('''SELECT id, name, image, description, category_key, price, sort_order, created_at 
                            FROM products ORDER BY category_key, sort_order, id''')
        
        products = cursor.fetchall()
        conn.close()
        return [{
            'id': p[0],
            'name': p[1],
            'image': p[2],
            'description': p[3],
            'category_key': p[4],
            'price': p[5],
            'sort_order': p[6],
            'created_at': p[7]
        } for p in products]
    
    def get_ai_service(self) -> Optional[Dict[str, Any]]:
        """获取AI服务配置"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, url, key, name, Prompter, keyword, enabled FROM ai_service LIMIT 1')
        service = cursor.fetchone()
        conn.close()
        if service:
            return {
                'id': service[0],
                'url': service[1],
                'key': service[2],
                'name': service[3],
                'Prompter': service[4],
                'keyword': service[5],
                'enabled': bool(service[6])
            }
        return None
    
    def check_keyword_conflict(self, keyword: str) -> Optional[str]:
        """检查关键词是否与自动触发规则冲突
        
        Args:
            keyword: 要检查的关键词
            
        Returns:
            如果有冲突，返回冲突的规则名称；否则返回None
        """
        import json
        
        if not keyword:
            return None
        
        keywords = [k.strip().lower() for k in keyword.split(',') if k.strip()]
        
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('SELECT id, rule_name, conditions, enabled FROM auto_trigger_rules')
        rules = cursor.fetchall()
        conn.close()
        
        for rule in rules:
            rule_id, rule_name, conditions_str, enabled = rule
            try:
                conditions = json.loads(conditions_str)
                for condition in conditions:
                    if condition.get('type') == 'keyword':
                        rule_keyword = str(condition.get('value', '')).lower()
                        if rule_keyword in keywords:
                            status = '' if enabled else '（已禁用）'
                            return f'{rule_name or f"规则#{rule_id}"}{status}'
            except (json.JSONDecodeError, TypeError):
                continue
        
        return None
    
    def save_ai_service(self, url: str, key: str, name: str, Prompter: str, keyword: str, enabled: bool = True) -> Dict[str, Any]:
        """保存AI服务配置（更新或创建）"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('SELECT id FROM ai_service LIMIT 1')
            existing = cursor.fetchone()
            
            if existing:
                cursor.execute('''
                    UPDATE ai_service 
                    SET url = ?, key = ?, name = ?, Prompter = ?, keyword = ?, enabled = ?
                    WHERE id = ?
                ''', (url, key, name, Prompter, keyword, 1 if enabled else 0, existing[0]))
                conn.commit()
                return {'success': True, 'message': 'AI服务配置已更新'}
            else:
                cursor.execute('''
                    INSERT INTO ai_service (url, key, name, Prompter, keyword, enabled)
                    VALUES (?, ?, ?, ?, ?, ?)
                ''', (url, key, name, Prompter, keyword, 1 if enabled else 0))
                conn.commit()
                return {'success': True, 'message': 'AI服务配置已创建'}
        except Exception as e:
            return {'success': False, 'message': f'保存失败: {str(e)}'}
        finally:
            conn.close()
    
    def update_ai_service_enabled(self, enabled: bool) -> Dict[str, Any]:
        """更新AI服务启用状态"""
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            cursor.execute('UPDATE ai_service SET enabled = ?', (1 if enabled else 0,))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': 'AI服务状态已更新'}
            return {'success': False, 'message': 'AI服务配置不存在'}
        finally:
            conn.close()
    
    def get_product(self, product_id: int) -> Optional[Dict[str, Any]]:
        """根据ID获取商品"""
        conn = self._get_connection()
        cursor = conn.cursor()
        cursor.execute('''SELECT id, name, image, description, category_key, price, sort_order, created_at 
                        FROM products WHERE id = ?''', (product_id,))
        product = cursor.fetchone()
        conn.close()
        if product:
            return {
                'id': product[0],
                'name': product[1],
                'image': product[2],
                'description': product[3],
                'category_key': product[4],
                'price': product[5],
                'sort_order': product[6],
                'created_at': product[7]
            }
        return None
    
    def create_product(self, name: str, image: str, description: str, category_key: str, price: int, sort_order: int = 0) -> Dict[str, Any]:
        """创建商品"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''INSERT INTO products (name, image, description, category_key, price, sort_order) 
                            VALUES (?, ?, ?, ?, ?, ?)''', 
                          (name, image, description, category_key, price, sort_order))
            conn.commit()
            product_id = cursor.lastrowid
            return {'success': True, 'message': '商品创建成功', 'id': product_id}
        except Exception as e:
            return {'success': False, 'message': f'创建失败: {str(e)}'}
        finally:
            conn.close()
    
    def update_product(self, product_id: int, name: str, image: str, description: str, category_key: str, price: int, sort_order: int = 0) -> Dict[str, Any]:
        """更新商品"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('''UPDATE products SET name = ?, image = ?, description = ?, category_key = ?, price = ?, sort_order = ? WHERE id = ?''',
                          (name, image, description, category_key, price, sort_order, product_id))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '商品更新成功'}
            return {'success': False, 'message': '商品不存在'}
        except Exception as e:
            return {'success': False, 'message': f'更新失败: {str(e)}'}
        finally:
            conn.close()
    
    def delete_product(self, product_id: int) -> Dict[str, Any]:
        """删除商品"""
        conn = self._get_connection()
        cursor = conn.cursor()
        try:
            cursor.execute('DELETE FROM products WHERE id = ?', (product_id,))
            conn.commit()
            if cursor.rowcount > 0:
                return {'success': True, 'message': '商品已删除'}
            return {'success': False, 'message': '商品不存在'}
        finally:
            conn.close()
    
    def add_product(self, name: str, image: str, description: str, category_key: str, price: int, sort_order: int = 0) -> Dict[str, Any]:
        """添加商品（create_product的别名）"""
        return self.create_product(name, image, description, category_key, price, sort_order)
    
    def search_products(self, keyword: str) -> List[Dict[str, Any]]:
        """根据关键词搜索商品"""
        conn = self._get_connection()
        cursor = conn.cursor()
        search_term = f'%{keyword}%'
        cursor.execute('''SELECT id, name, image, description, category_key, price, sort_order, created_at 
                        FROM products WHERE name LIKE ? OR description LIKE ? ORDER BY category_key, sort_order, id''',
                      (search_term, search_term))
        products = cursor.fetchall()
        conn.close()
        return [{
            'id': p[0],
            'name': p[1],
            'image': p[2],
            'description': p[3],
            'category_key': p[4],
            'price': p[5],
            'sort_order': p[6],
            'created_at': p[7]
        } for p in products]
    

