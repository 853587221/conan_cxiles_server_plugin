import sqlite3
import time
from pathlib import Path
from typing import Dict, Any, Optional
from datetime import datetime


class Judge:
    def __init__(self, username: str):
        self.username = username
        self.db_path = self._get_user_db_path()
    
    def _get_user_db_path(self) -> Path:
        return Path(__file__).parent.parent / f"data/{self.username}/database.db"
    
    def _get_connection(self):
        return sqlite3.connect(str(self.db_path))
    
    def judge_keyword(self, data: Dict[str, Any], operator: str, value: str) -> bool:
        message = data.get('said', '').lower()
        value = value.lower()
        
        if operator == 'eq':
            return message == value
        elif operator == 'startsWith':
            return message.startswith(value)
        elif operator == 'contains':
            return value in message
        elif operator == 'notContains':
            return value not in message
        elif operator == 'endsWith':
            return message.endswith(value)
        
        return False
    
    def judge_amount(self, data: Dict[str, Any], operator: str, value: float) -> bool:
        player_info = data.get('player_info', {})
        user_id = player_info.get('User_ID')
        platform_id = player_info.get('Platform_ID')
        
        if not user_id and not platform_id:
            return False
        
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            if user_id:
                cursor.execute('SELECT gold FROM players WHERE user_id = ?', (user_id,))
            else:
                cursor.execute('SELECT gold FROM players WHERE platform_id = ?', (platform_id,))
            
            result = cursor.fetchone()
            if not result:
                return False
            
            player_gold = float(result[0])
            
            if operator == 'eq':
                return player_gold == value
            elif operator == 'gt':
                return player_gold > value
            elif operator == 'lt':
                return player_gold < value
            elif operator == 'gte':
                return player_gold >= value
            elif operator == 'lte':
                return player_gold <= value
            
            return False
        finally:
            conn.close()
    
    def judge_tag(self, data: Dict[str, Any], operator: str, value: int) -> bool:
        player_info = data.get('player_info', {})
        user_id = player_info.get('User_ID')
        platform_id = player_info.get('Platform_ID')
        
        if not user_id and not platform_id:
            return False
        
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            if user_id:
                cursor.execute('SELECT permission_level FROM players WHERE user_id = ?', (user_id,))
            else:
                cursor.execute('SELECT permission_level FROM players WHERE platform_id = ?', (platform_id,))
            
            result = cursor.fetchone()
            if not result:
                return False
            
            player_tag = int(result[0])
            
            if operator == 'eq':
                return player_tag == value
            elif operator == 'gt':
                return player_tag > value
            elif operator == 'lt':
                return player_tag < value
            elif operator == 'gte':
                return player_tag >= value
            elif operator == 'lte':
                return player_tag <= value
            
            return False
        finally:
            conn.close()
    
    def judge_level(self, data: Dict[str, Any], operator: str, value: int) -> bool:
        level = data.get('level', 0)
        
        if operator == 'eq':
            return level == value
        elif operator == 'gt':
            return level > value
        elif operator == 'lt':
            return level < value
        elif operator == 'gte':
            return level >= value
        elif operator == 'lte':
            return level <= value
        
        return False
    
    def judge_playtime(self, data: Dict[str, Any], operator: str, value: int) -> bool:
        player_online_time = data.get('online_time', None)
        
        if player_online_time is None:
            player_info = data.get('player_info', {})
            user_id = player_info.get('User_ID')
            platform_id = player_info.get('Platform_ID')
            
            if not user_id and not platform_id:
                return False
            
            conn = self._get_connection()
            cursor = conn.cursor()
            
            try:
                if user_id:
                    cursor.execute('SELECT online_time FROM players WHERE user_id = ?', (user_id,))
                else:
                    cursor.execute('SELECT online_time FROM players WHERE platform_id = ?', (platform_id,))
                
                result = cursor.fetchone()
                if not result:
                    return False
                
                player_online_time = int(result[0])
            finally:
                conn.close()
        else:
            player_online_time = int(player_online_time)
        
        if operator == 'eq':
            return player_online_time == value
        elif operator == 'gt':
            return player_online_time > value
        elif operator == 'lt':
            return player_online_time < value
        elif operator == 'gte':
            return player_online_time >= value
        elif operator == 'lte':
            return player_online_time <= value
        elif operator == 'interval':
            return player_online_time > 0 and player_online_time % value == 0
        
        return False
    
    def judge_new_player(self, data: Dict[str, Any], operator: str, value: str) -> bool:
        new_player = data.get('new_player', '')
        return new_player == 'yes'
    
    def judge_vip(self, data: Dict[str, Any], operator: str, value: str) -> bool:
        player_info = data.get('player_info', {})
        user_id = player_info.get('User_ID')
        platform_id = player_info.get('Platform_ID')
        
        if not user_id and not platform_id:
            return False
        
        conn = self._get_connection()
        cursor = conn.cursor()
        
        try:
            if user_id:
                cursor.execute('SELECT monthly_card_expiry FROM players WHERE user_id = ?', (user_id,))
            else:
                cursor.execute('SELECT monthly_card_expiry FROM players WHERE platform_id = ?', (platform_id,))
            
            result = cursor.fetchone()
            if not result or result[0] is None:
                is_vip = False
            else:
                expiry_timestamp = float(result[0])
                now = time.time()
                is_vip = expiry_timestamp > now
            
            if operator == 'eq':
                if value == 'yes':
                    return is_vip
                elif value == 'no':
                    return not is_vip
            
            return False
        finally:
            conn.close()
    
    def judge_server_time(self, data: Dict[str, Any], operator: str, value: str) -> bool:
        now = datetime.now()
        
        if operator == 'date_range':
            parts = value.split('|')
            if len(parts) != 2:
                return False
            
            try:
                start_time = datetime.strptime(parts[0], '%Y-%m-%d %H:%M')
                end_time = datetime.strptime(parts[1], '%Y-%m-%d %H:%M')
                
                return start_time <= now <= end_time
            except ValueError:
                return False
        
        elif operator == 'weekday':
            parts = value.split('|')
            if len(parts) != 3:
                return False
            
            try:
                selected_weekdays = [int(d) for d in parts[0].split(',')]
                start_time_str = parts[1]
                end_time_str = parts[2]
                
                current_weekday = now.weekday()
                if current_weekday == 6:
                    current_weekday = 0
                else:
                    current_weekday += 1
                
                if current_weekday not in selected_weekdays:
                    return False
                
                current_time = now.strftime('%H:%M')
                
                return start_time_str <= current_time <= end_time_str
            except (ValueError, IndexError):
                return False
        
        return False
    
    def judge_item(self, data: Dict[str, Any], operator: str, value: str) -> bool:
        """判断背包物品
        
        Args:
            data: 包含inventory的数据
            operator: 操作符 (eq, gte)
            value: 物品ID (template_id) 或 "物品ID:数量" 格式，或多个物品 "物品ID1:数量1;物品ID2:数量2;..."
        
        Returns:
            bool: 是否满足条件（所有物品都必须满足条件）
        """
        print(f"[Judge] judge_item 开始: operator={operator}, value={value}")
        
        inventory = data.get('inventory', {})
        if not inventory:
            print(f"[Judge] inventory 为空，返回 False")
            return False
        
        backpack = inventory.get('backpack', {})
        equipment = inventory.get('equipment', {})
        quickbar = inventory.get('quickbar', {})
        
        print(f"[Judge] backpack 物品数: {len(backpack)}, equipment 物品数: {len(equipment)}, quickbar 物品数: {len(quickbar)}")
        
        # 合并所有背包区域的物品
        all_items = {}
        for key, item in backpack.items():
            all_items[f'backpack_{key}'] = item
        for key, item in equipment.items():
            all_items[f'equipment_{key}'] = item
        for key, item in quickbar.items():
            all_items[f'quickbar_{key}'] = item
        
        print(f"[Judge] 合并后的物品总数: {len(all_items)}")
        if len(all_items) > 0:
            first_item_key = list(all_items.keys())[0]
            first_item = all_items[first_item_key]
            print(f"[Judge] 第一个物品示例: key={first_item_key}, template_id={first_item.get('template_id')}, quantity={first_item.get('quantity')}")
        
        # 解析value，支持多个物品（用分号分隔）
        items_to_check = []
        
        if ';' in value:
            # 多个物品
            item_strings = value.split(';')
            for item_str in item_strings:
                item_str = item_str.strip()
                if not item_str:
                    continue
                
                template_id = item_str
                required_quantity = 1
                
                if ':' in item_str:
                    parts = item_str.split(':')
                    if len(parts) == 2:
                        template_id = parts[0]
                        required_quantity = int(parts[1])
                
                items_to_check.append({
                    'template_id': template_id,
                    'required_quantity': required_quantity
                })
        else:
            # 单个物品
            template_id = value
            required_quantity = 1
            
            if ':' in value:
                parts = value.split(':')
                if len(parts) == 2:
                    template_id = parts[0]
                    required_quantity = int(parts[1])
            
            items_to_check.append({
                'template_id': template_id,
                'required_quantity': required_quantity
            })
        
        print(f"[Judge] 需要检查的物品数量: {len(items_to_check)}")
        
        # 对每个物品进行判断
        for item_check in items_to_check:
            template_id = item_check['template_id']
            required_quantity = item_check['required_quantity']
            
            # 将template_id转换为整数（SSE返回的template_id是整数类型）
            try:
                template_id = int(template_id)
            except (ValueError, TypeError):
                print(f"[Judge] 无法将template_id转换为整数: {template_id}")
                return False
            
            print(f"[Judge] 检查物品: template_id={template_id}, required_quantity={required_quantity}")
            
            # 统计指定物品的数量
            total_quantity = 0
            for item_id, item in all_items.items():
                item_template_id = item.get('template_id')
                item_quantity = item.get('quantity', 0)
                
                if item_template_id == template_id:
                    total_quantity += item_quantity
                    print(f"[Judge] 找到匹配物品: item_id={item_id}, template_id={item_template_id}, quantity={item_quantity}, 累计={total_quantity}")
            
            print(f"[Judge] 物品判断: template_id={template_id}, operator={operator}, required_quantity={required_quantity}, total_quantity={total_quantity}")
            
            # 判断当前物品是否满足条件
            if operator == 'eq':
                result = total_quantity == required_quantity
                print(f"[Judge] 等于判断: {total_quantity} == {required_quantity} = {result}")
                if not result:
                    return False
            elif operator == 'gt':
                result = total_quantity > required_quantity
                print(f"[Judge] 大于判断: {total_quantity} > {required_quantity} = {result}")
                if not result:
                    return False
            elif operator == 'lt':
                result = total_quantity < required_quantity
                print(f"[Judge] 小于判断: {total_quantity} < {required_quantity} = {result}")
                if not result:
                    return False
            elif operator == 'gte':
                result = total_quantity >= required_quantity
                print(f"[Judge] 大于等于判断: {total_quantity} >= {required_quantity} = {result}")
                if not result:
                    return False
            elif operator == 'lte':
                result = total_quantity <= required_quantity
                print(f"[Judge] 小于等于判断: {total_quantity} <= {required_quantity} = {result}")
                if not result:
                    return False
            else:
                print(f"[Judge] 未知的操作符: {operator}")
                return False
        
        # 所有物品都满足条件
        print(f"[Judge] 所有物品都满足条件")
        return True
    
    def judge_condition(self, condition_type: str, data: Dict[str, Any], operator: str, value: Any) -> bool:
        if condition_type == 'keyword':
            return self.judge_keyword(data, operator, str(value))
        elif condition_type == 'amount':
            return self.judge_amount(data, operator, float(value))
        elif condition_type == 'tag':
            return self.judge_tag(data, operator, int(value))
        elif condition_type == 'level':
            return self.judge_level(data, operator, int(value))
        elif condition_type == 'playtime':
            return self.judge_playtime(data, operator, int(value))
        elif condition_type == 'new_player':
            return self.judge_new_player(data, operator, str(value))
        elif condition_type == 'vip':
            return self.judge_vip(data, operator, str(value))
        elif condition_type == 'server_time':
            return self.judge_server_time(data, operator, str(value))
        elif condition_type == 'item':
            return self.judge_item(data, operator, str(value))
        
        return False
    
    def judge_all_conditions(self, data: Dict[str, Any], conditions: list) -> bool:
        for condition in conditions:
            condition_type = condition.get('type')
            operator = condition.get('operator')
            value = condition.get('value')
            
            if not self.judge_condition(condition_type, data, operator, value):
                return False
        
        return True
    
    def judge_guild_id_match(self, data: Dict[str, Any]) -> Optional[str]:
        guild_id = data.get('guild_id')
        guild_id_2 = data.get('guild_id_2')
        
        if guild_id and guild_id_2 and str(guild_id) == str(guild_id_2):
            player_info_2 = data.get('player_info_2', {})
            return player_info_2.get('Char_name_2')
        
        return None
