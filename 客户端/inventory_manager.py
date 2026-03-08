import sqlite3
import os
import struct

def parse_item_data(data):
    """
    解析物品的十六进制数据，提取物品路径、实例名称和数量
    
    Args:
        data (bytes): 物品的十六进制数据
    
    Returns:
        dict: 包含物品信息的字典，格式为：
            {
                "item_path": str,      # 物品路径
                "instance_name": str,  # 实例名称
                "quantity": int,        # 物品数量
                "raw_data": bytes      # 原始数据
            }
    """
    if not data or len(data) < 32:
        return {
            "item_path": None,
            "instance_name": None,
            "quantity": 1,
            "light_attack_damage": 0,
            "heavy_attack_damage": 0,
            "armor_penetration": 0.0,
            "raw_data": data
        }
    
    try:
        offset = 24
        
        if offset + 4 > len(data):
            return {
                "item_path": None,
                "instance_name": None,
                "quantity": 1,
                "light_attack_damage": 0,
                "heavy_attack_damage": 0,
                "armor_penetration": 0.0,
                "raw_data": data
            }
        str1_len = struct.unpack('<I', data[offset:offset+4])[0]
        offset += 4
        
        if str1_len > 0:
            offset += str1_len
        
        if offset + 4 > len(data):
            return {
                "item_path": None,
                "instance_name": None,
                "quantity": 1,
                "light_attack_damage": 0,
                "heavy_attack_damage": 0,
                "armor_penetration": 0.0,
                "raw_data": data
            }
        item_path_len = struct.unpack('<I', data[offset:offset+4])[0]
        offset += 4
        
        item_path = None
        if item_path_len > 0 and offset + item_path_len <= len(data):
            item_path = data[offset:offset+item_path_len].decode('utf-8', errors='ignore').rstrip('\x00')
            offset += item_path_len
        
        if offset + 4 > len(data):
            return {
                "item_path": item_path,
                "instance_name": None,
                "quantity": 1,
                "light_attack_damage": 0,
                "heavy_attack_damage": 0,
                "armor_penetration": 0.0,
                "raw_data": data
            }
        instance_name_len = struct.unpack('<I', data[offset:offset+4])[0]
        offset += 4
        
        instance_name = None
        if instance_name_len > 0 and offset + instance_name_len <= len(data):
            instance_name = data[offset:offset+instance_name_len].decode('utf-8', errors='ignore').rstrip('\x00')
            offset += instance_name_len
        
        light_attack_damage = 0
        heavy_attack_damage = 0
        armor_penetration = 0.0
        
        if item_path:
            item_path_lower = item_path.lower()
            
            if 'weapon' in item_path_lower:
                if offset + 13 <= len(data):
                    try:
                        light_attack_damage = struct.unpack('<H', data[offset+13:offset+15])[0]
                    except:
                        light_attack_damage = 0
                
                if offset + 23 <= len(data):
                    try:
                        heavy_attack_damage = struct.unpack('<H', data[offset+21:offset+23])[0]
                    except:
                        heavy_attack_damage = 0
                
                if offset + 77 <= len(data):
                    try:
                        armor_penetration = struct.unpack('<f', data[offset+73:offset+77])[0]
                    except:
                        armor_penetration = 0.0
            
            if offset + 17 <= len(data):
                try:
                    parsed_quantity = struct.unpack('<I', data[offset+13:offset+17])[0]
                    
                    if 'weapon' in item_path_lower or 'armor' in item_path_lower:
                        quantity = 1
                    elif parsed_quantity < 10000:
                        quantity = parsed_quantity
                    else:
                        quantity = 1
                except:
                    quantity = 1
            else:
                quantity = 1
        else:
            if offset + 17 <= len(data):
                try:
                    parsed_quantity = struct.unpack('<I', data[offset+13:offset+17])[0]
                    if parsed_quantity < 10000:
                        quantity = parsed_quantity
                    else:
                        quantity = 1
                except:
                    quantity = 1
            else:
                quantity = 1
        
        return {
            "item_path": item_path,
            "instance_name": instance_name,
            "quantity": quantity,
            "light_attack_damage": light_attack_damage,
            "heavy_attack_damage": heavy_attack_damage,
            "armor_penetration": armor_penetration,
            "raw_data": data
        }
    except:
        return {
            "item_path": None,
            "instance_name": None,
            "quantity": 1,
            "light_attack_damage": 0,
            "heavy_attack_damage": 0,
            "armor_penetration": 0.0,
            "raw_data": data
        }

def get_inventory_type_name(inv_type):
    """
    获取背包类型的名称
    
    Args:
        inv_type (int): 背包类型编号
    
    Returns:
        str: 背包类型名称
    """
    inv_types = {
        0: '背包',
        1: '装备栏',
        2: '快捷栏',
        6: '技能点/知识',
        7: '表情动作'
    }
    return inv_types.get(inv_type, f'未知({inv_type})')

def get_inventory_by_owner_id(db_path="game.db", owner_id=None, inv_type=None):
    """
    通过 owner_id 直接获取背包物品（支持玩家角色和奴隶）
    
    Args:
        db_path (str): 数据库文件路径
        owner_id (int): 所有者ID（角色ID或奴隶ID）
        inv_type (int): 背包类型（可选），0=背包，1=装备栏，2=快捷栏
    
    Returns:
        dict: 包含背包物品信息的字典
    """
    if owner_id is None:
        return {
            "success": False,
            "data": [],
            "message": "请提供 owner_id"
        }
    
    if not os.path.exists(db_path):
        return {
            "success": False,
            "data": [],
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        query = "SELECT item_id, owner_id, inv_type, template_id, data FROM item_inventory WHERE owner_id = ?"
        params = [owner_id]
        
        if inv_type is not None:
            query += " AND inv_type = ?"
            params.append(inv_type)
        
        query += " ORDER BY inv_type, item_id"
        
        cursor.execute(query, params)
        items = cursor.fetchall()
        
        inventory_data = []
        for item in items:
            item_id, owner_id, item_inv_type, template_id, data = item
            
            parsed_data = parse_item_data(data)
            
            inventory_item = {
                "item_id": item_id,
                "template_id": template_id,
                "inv_type": item_inv_type,
                "inv_type_name": get_inventory_type_name(item_inv_type),
                "item_path": parsed_data.get("item_path"),
                "instance_name": parsed_data.get("instance_name"),
                "quantity": parsed_data.get("quantity", 1),
                "owner_id": owner_id
            }
            
            inventory_data.append(inventory_item)
        
        conn.close()
        
        return {
            "success": True,
            "data": inventory_data,
            "message": f"找到 {len(inventory_data)} 个物品"
        }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "data": [],
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "data": [],
            "message": f"发生错误: {str(e)}"
        }

def get_player_inventory(db_path="game.db", player_id=None, char_name=None, inv_type=None):
    """
    获取玩家的背包物品
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
        inv_type (int): 背包类型（可选），0=背包，1=装备栏，2=快捷栏，6=技能点/知识，7=表情动作
                         如果不指定，则返回所有类型的物品
    
    Returns:
        dict: 包含背包物品信息的字典，格式为：
            {
                "success": bool,
                "data": list,
                "message": str
            }
            其中 data 列表包含物品信息，每个物品包含：
            {
                "item_id": int,           # 物品在背包中的位置ID
                "template_id": int,        # 物品模板ID
                "inv_type": int,           # 背包类型
                "inv_type_name": str,      # 背包类型名称
                "item_path": str,          # 物品路径
                "instance_name": str,      # 实例名称
                "char_name": str,          # 角色名称
                "player_id": str,          # 玩家ID
                "properties": list         # 物品属性列表
            }
    """
    if not player_id and not char_name:
        return {
            "success": False,
            "data": [],
            "message": "请提供 player_id 或 char_name"
        }
    
    if not os.path.exists(db_path):
        return {
            "success": False,
            "data": [],
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        if char_name and not player_id:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE char_name = ?", (char_name,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": f"未找到角色名为 '{char_name}' 的角色"
                }
            char_id, player_id, char_name = result
        else:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE playerId = ?", (player_id,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": f"未找到玩家ID为 '{player_id}' 的角色"
                }
            char_id, player_id, char_name = result
        
        query = "SELECT item_id, owner_id, inv_type, template_id, data FROM item_inventory WHERE owner_id = ?"
        params = [char_id]
        
        if inv_type is not None:
            query += " AND inv_type = ?"
            params.append(inv_type)
        
        query += " ORDER BY inv_type, item_id"
        
        cursor.execute(query, params)
        items = cursor.fetchall()
        
        inventory_data = []
        for item in items:
            item_id, owner_id, item_inv_type, template_id, data = item
            
            parsed_data = parse_item_data(data)
            
            item_info = {
                "item_id": item_id,
                "template_id": template_id,
                "inv_type": item_inv_type,
                "inv_type_name": get_inventory_type_name(item_inv_type),
                "item_path": parsed_data["item_path"],
                "instance_name": parsed_data["instance_name"],
                "quantity": parsed_data["quantity"],
                "light_attack_damage": parsed_data["light_attack_damage"],
                "heavy_attack_damage": parsed_data["heavy_attack_damage"],
                "armor_penetration": parsed_data["armor_penetration"],
                "char_name": char_name,
                "player_id": player_id
            }
            inventory_data.append(item_info)
        
        conn.close()
        
        inv_type_msg = f"（类型: {get_inventory_type_name(inv_type)}）" if inv_type is not None else ""
        return {
            "success": True,
            "data": inventory_data,
            "message": f"找到角色 '{char_name}' 的 {len(inventory_data)} 个物品{inv_type_msg}"
        }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "data": [],
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "data": [],
            "message": f"发生错误: {str(e)}"
        }

def get_player_inventory_by_type(db_path="game.db", player_id=None, char_name=None):
    """
    按背包类型分组获取玩家的背包物品
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
    
    Returns:
        dict: 包含按类型分组的背包物品信息的字典，格式为：
            {
                "success": bool,
                "data": dict,
                "message": str
            }
            其中 data 字典按背包类型分组，每个类型包含一个物品列表
    """
    if not player_id and not char_name:
        return {
            "success": False,
            "data": {},
            "message": "请提供 player_id 或 char_name"
        }
    
    result = get_player_inventory(db_path, player_id, char_name, inv_type=None)
    
    if not result["success"]:
        return result
    
    grouped_data = {}
    for item in result["data"]:
        inv_type_name = item["inv_type_name"]
        if inv_type_name not in grouped_data:
            grouped_data[inv_type_name] = []
        grouped_data[inv_type_name].append(item)
    
    return {
        "success": True,
        "data": grouped_data,
        "message": f"找到角色 '{result['data'][0]['char_name']}' 的物品，共 {len(result['data'])} 个，分为 {len(grouped_data)} 个背包类型"
    }

def update_item_position(db_path="game.db", player_id=None, char_name=None, old_item_id=None, old_inv_type=None, new_item_id=None, new_inv_type=None):
    """
    修改物品的位置ID和/或背包类型
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
        old_item_id (int): 原物品位置ID
        old_inv_type (int): 原背包类型（可选）
        new_item_id (int): 新物品位置ID（可选）
        new_inv_type (int): 新背包类型（可选）
    
    Returns:
        dict: 包含操作结果的字典
    """
    if not player_id and not char_name:
        return {
            "success": False,
            "message": "请提供 player_id 或 char_name"
        }
    
    if old_item_id is None:
        return {
            "success": False,
            "message": "请提供原物品位置ID (old_item_id)"
        }
    
    if new_item_id is None and new_inv_type is None:
        return {
            "success": False,
            "message": "请提供至少一个修改项：新物品位置ID (new_item_id) 或新背包类型 (new_inv_type)"
        }
    
    if not os.path.exists(db_path):
        return {
            "success": False,
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        if char_name and not player_id:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE char_name = ?", (char_name,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "message": f"未找到角色名为 '{char_name}' 的角色"
                }
            char_id, player_id, char_name = result
        else:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE playerId = ?", (player_id,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "message": f"未找到玩家ID为 '{player_id}' 的角色"
                }
            char_id, player_id, char_name = result
        
        query = "UPDATE item_inventory SET "
        updates = []
        params = []
        
        if new_item_id is not None:
            updates.append("item_id = ?")
            params.append(new_item_id)
        
        if new_inv_type is not None:
            updates.append("inv_type = ?")
            params.append(new_inv_type)
        
        query += ", ".join(updates)
        query += " WHERE owner_id = ? AND item_id = ?"
        params.extend([char_id, old_item_id])
        
        if old_inv_type is not None:
            query += " AND inv_type = ?"
            params.append(old_inv_type)
        
        cursor.execute(query, params)
        affected_rows = cursor.rowcount
        conn.commit()
        conn.close()
        
        if affected_rows > 0:
            changes = []
            if new_item_id is not None:
                changes.append(f"位置ID从 {old_item_id} 改为 {new_item_id}")
            if new_inv_type is not None:
                old_type = get_inventory_type_name(old_inv_type) if old_inv_type is not None else "未知"
                new_type = get_inventory_type_name(new_inv_type)
                changes.append(f"背包类型从 {old_type} 改为 {new_type}")
            
            return {
                "success": True,
                "affected_rows": affected_rows,
                "message": f"成功修改 {affected_rows} 个物品：{', '.join(changes)}"
            }
        else:
            return {
                "success": False,
                "message": "未找到匹配的物品，请检查原物品位置ID和背包类型是否正确"
            }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "message": f"发生错误: {str(e)}"
        }

def update_item_template(db_path="game.db", player_id=None, char_name=None, item_id=None, inv_type=None, new_template_id=None):
    """
    修改物品的模板ID
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
        item_id (int): 物品位置ID
        inv_type (int): 背包类型（可选）
        new_template_id (int): 新物品模板ID
    
    Returns:
        dict: 包含操作结果的字典
    """
    if not player_id and not char_name:
        return {
            "success": False,
            "message": "请提供 player_id 或 char_name"
        }
    
    if item_id is None:
        return {
            "success": False,
            "message": "请提供物品位置ID (item_id)"
        }
    
    if new_template_id is None:
        return {
            "success": False,
            "message": "请提供新物品模板ID (new_template_id)"
        }
    
    if not os.path.exists(db_path):
        return {
            "success": False,
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        if char_name and not player_id:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE char_name = ?", (char_name,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "message": f"未找到角色名为 '{char_name}' 的角色"
                }
            char_id, player_id, char_name = result
        else:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE playerId = ?", (player_id,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": f"未找到玩家ID为 '{player_id}' 的角色"
                }
            char_id, player_id, char_name = result
        
        query = "UPDATE item_inventory SET template_id = ? WHERE owner_id = ? AND item_id = ?"
        params = [new_template_id, char_id, item_id]
        
        if inv_type is not None:
            query += " AND inv_type = ?"
            params.append(inv_type)
        
        cursor.execute(query, params)
        affected_rows = cursor.rowcount
        conn.commit()
        conn.close()
        
        if affected_rows > 0:
            return {
                "success": True,
                "affected_rows": affected_rows,
                "message": f"成功修改 {affected_rows} 个物品的模板ID为 {new_template_id}"
            }
        else:
            return {
                "success": False,
                "message": "未找到匹配的物品，请检查物品位置ID和背包类型是否正确"
            }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "data": [],
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "data": [],
            "message": f"发生错误: {str(e)}"
        }

def delete_item(db_path="game.db", player_id=None, char_name=None, item_id=None, inv_type=None):
    """
    删除物品
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
        item_id (int): 物品位置ID
        inv_type (int): 背包类型（可选）
    
    Returns:
        dict: 包含操作结果的字典
    """
    if not player_id and not char_name:
        return {
            "success": False,
            "message": "请提供 player_id 或 char_name"
        }
    
    if item_id is None:
        return {
            "success": False,
            "message": "请提供物品位置ID (item_id)"
        }
    
    if not os.path.exists(db_path):
        return {
            "success": False,
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        if char_name and not player_id:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE char_name = ?", (char_name,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "message": f"未找到角色名为 '{char_name}' 的角色"
                }
            char_id, player_id, char_name = result
        else:
            cursor.execute("SELECT id, playerId, char_name FROM characters WHERE playerId = ?", (player_id,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": f"未找到玩家ID为 '{player_id}' 的角色"
                }
            char_id, player_id, char_name = result
        
        query = "DELETE FROM item_inventory WHERE owner_id = ? AND item_id = ?"
        params = [char_id, item_id]
        
        if inv_type is not None:
            query += " AND inv_type = ?"
            params.append(inv_type)
        
        cursor.execute(query, params)
        affected_rows = cursor.rowcount
        
        query_props = "DELETE FROM item_properties WHERE owner_id = ? AND item_id = ?"
        params_props = [char_id, item_id]
        
        if inv_type is not None:
            query_props += " AND inv_type = ?"
            params_props.append(inv_type)
        
        cursor.execute(query_props, params_props)
        
        conn.commit()
        conn.close()
        
        if affected_rows > 0:
            return {
                "success": True,
                "affected_rows": affected_rows,
                "message": f"成功删除 {affected_rows} 个物品"
            }
        else:
            return {
                "success": False,
                "message": "未找到匹配的物品，请检查物品位置ID和背包类型是否正确"
            }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "message": f"发生错误: {str(e)}"
        }

def extract_guid_from_uniqueid(value):
    """
    从 UniqueID 二进制数据中提取 GUID
    
    Args:
        value (bytes): UniqueID 二进制数据
    
    Returns:
        str: GUID 字符串 (如 "efbeadde0fcafeba")，如果提取失败返回 None
    """
    if not value or len(value) < 12:
        return None
    try:
        guid1 = value[4:8].hex()
        guid2 = value[8:12].hex()
        return f"{guid1}{guid2}"
    except:
        return None

def get_guild_name(db_path="game.db", guild_id=None):
    """
    根据部落ID获取部落名称
    
    Args:
        db_path (str): 数据库文件路径
        guild_id (int): 部落ID
    
    Returns:
        str: 部落名称，如果不存在则返回 None
    """
    if not guild_id:
        return None
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        cursor.execute("SELECT name FROM guilds WHERE guildId = ?", (guild_id,))
        result = cursor.fetchone()
        
        conn.close()
        
        if result:
            return result[0]
        return None
        
    except sqlite3.Error as e:
        return None
    except Exception as e:
        return None

def get_player_guid(db_path="game.db", player_id=None, char_name=None):
    """
    获取玩家的 GUID（部落ID或个人ID）
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID
        char_name (str): 角色名称
    
    Returns:
        dict: 包含 GUID 信息的字典
    """
    if not player_id and not char_name:
        return {
            "success": False,
            "guid": None,
            "message": "请提供 player_id 或 char_name"
        }
    
    if not os.path.exists(db_path):
        return {
            "success": False,
            "guid": None,
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        if char_name:
            cursor.execute("SELECT id FROM characters WHERE char_name = ?", (char_name,))
        else:
            cursor.execute("SELECT id FROM characters WHERE playerId = ?", (player_id,))
        
        result = cursor.fetchone()
        if not result:
            conn.close()
            return {
                "success": False,
                "guid": None,
                "message": "未找到角色"
            }
        
        char_id = result[0]
        
        cursor.execute("SELECT value FROM properties WHERE object_id = ? AND name = 'BasePlayerChar_C.bedID'", (char_id,))
        bed_id_result = cursor.fetchone()
        
        conn.close()
        
        if bed_id_result and bed_id_result[0]:
            guid = extract_guid_from_uniqueid(bed_id_result[0])
            if guid:
                return {
                    "success": True,
                    "guid": guid,
                    "message": f"成功获取 GUID: {guid}"
                }
        
        return {
            "success": False,
            "guid": None,
            "message": "无法从角色属性中提取 GUID"
        }
        
    except Exception as e:
        return {
            "success": False,
            "guid": None,
            "message": f"发生错误: {str(e)}"
        }

def extract_thrall_perks(data):
    """
    从 m_OwnedPerks 二进制数据中提取特权ID
    
    Args:
        data (bytes): m_OwnedPerks 的二进制数据
    
    Returns:
        dict: 包含特权ID的字典 {perk_1: id, perk_2: id, perk_3: id}
    """
    import re
    
    if not data:
        return {'perk_1': None, 'perk_2': None, 'perk_3': None}
    
    decoded = data.decode('utf-8', errors='ignore')
    numbers = re.findall(r'\d+', decoded)
    
    perks = {
        'perk_1': int(numbers[0]) if len(numbers) > 0 else None,
        'perk_2': int(numbers[1]) if len(numbers) > 1 else None,
        'perk_3': int(numbers[2]) if len(numbers) > 2 else None
    }
    
    return perks

def get_thrall_stats(db_path, cursor, thrall_id):
    """
    获取奴隶的详细属性数据
    
    Args:
        db_path (str): 数据库路径
        cursor: 数据库游标
        thrall_id (int): 奴隶ID
    
    Returns:
        dict: 包含奴隶属性的字典
    """
    stats = {}
    
    stat_mapping = {
        (0, 1): 'health',
        (0, 4): 'level',
        (0, 6): 'food',
        (0, 7): 'xp',
        (0, 8): 'xp_max',
        (0, 9): 'strength_base',
        (0, 10): 'agility_base',
        (0, 13): 'vitality_base',
        (0, 14): 'vitality',
        (0, 15): 'grit',
        (0, 16): 'strength_bonus',
        (0, 17): 'strength',
        (0, 18): 'agility_bonus',
        (0, 19): 'agility',
        (0, 20): 'encumbrance',
    }
    
    cursor.execute('''
        SELECT stat_type, stat_id, stat_value 
        FROM character_stats 
        WHERE char_id = ?
    ''', (thrall_id,))
    
    for stat_type, stat_id, stat_value in cursor.fetchall():
        key = (stat_type, stat_id)
        if key in stat_mapping:
            stats[stat_mapping[key]] = stat_value
    
    return stats

def get_thrall_perks(db_path, cursor, thrall_id):
    """
    获取奴隶的特权数据
    
    Args:
        db_path (str): 数据库路径
        cursor: 数据库游标
        thrall_id (int): 奴隶ID
    
    Returns:
        dict: 包含特权信息的字典
    """
    perks = {}
    
    cursor.execute('''
        SELECT name, value FROM properties 
        WHERE object_id = ? AND name LIKE 'BP_NPCProgressionSystem_C.%'
    ''', (thrall_id,))
    
    for name, value in cursor.fetchall():
        if value:
            if name == 'BP_NPCProgressionSystem_C.m_GrowthID':
                decoded = value.decode('utf-8', errors='ignore')
                cleaned = ''.join(c for c in decoded if c.isprintable() or c in ' _-')
                perks['growth_id'] = cleaned.strip()
            elif name == 'BP_NPCProgressionSystem_C.m_PerkType':
                decoded = value.decode('utf-8', errors='ignore')
                cleaned = ''.join(c for c in decoded if c.isprintable() or c in ' _-')
                perks['perk_type'] = cleaned.strip()
            elif name == 'BP_NPCProgressionSystem_C.m_XPCurveID':
                decoded = value.decode('utf-8', errors='ignore')
                cleaned = ''.join(c for c in decoded if c.isprintable() or c in ' _-')
                perks['xp_curve'] = cleaned.strip()
            elif name == 'BP_NPCProgressionSystem_C.m_OwnedPerks':
                perks.update(extract_thrall_perks(value))
    
    return perks

def get_thrall_inventory(db_path, cursor, thrall_id):
    """
    获取奴隶的物品栏和装备栏
    
    Args:
        db_path (str): 数据库路径
        cursor: 数据库游标
        thrall_id (int): 奴隶ID
    
    Returns:
        dict: 包含物品栏和装备栏的字典
    """
    inventory = {
        'backpack': [],
        'equipment': {
            'head': None,
            'body': None,
            'hands': None,
            'legs': None,
            'feet': None,
            'main_hand': None,
            'off_hand': None
        }
    }
    
    equipment_slot_mapping = {
        0: 'head',
        4: 'body',
        5: 'hands',
        6: 'legs',
        7: 'feet',
        9: 'main_hand',
        10: 'off_hand'
    }
    
    cursor.execute('''
        SELECT item_id, inv_type, template_id, data 
        FROM item_inventory 
        WHERE owner_id = ?
        ORDER BY inv_type, item_id
    ''', (thrall_id,))
    
    for slot_index, inv_type, template_id, data in cursor.fetchall():
        item_path = None
        instance_name = None
        quantity = 1
        
        if data:
            parsed = parse_item_data(data)
            item_path = parsed.get("item_path")
            instance_name = parsed.get("instance_name")
            quantity = parsed.get("quantity", 1)
        
        item_info = {
            'item_id': template_id,
            'inv_type': inv_type,
            'slot_index': slot_index,
            'item_path': item_path,
            'instance_name': instance_name,
            'quantity': quantity
        }
        
        if inv_type == 0:
            inventory['backpack'].append(item_info)
        elif inv_type == 1:
            slot = equipment_slot_mapping.get(slot_index)
            if slot:
                inventory['equipment'][slot] = item_info
    
    return inventory

def get_all_thralls(db_path="game.db"):
    """
    获取所有奴隶信息（通过 follower_markers 表匹配归属）
    
    Args:
        db_path (str): 数据库文件路径
    
    Returns:
        dict: 包含奴隶信息的字典，格式为：
            {
                "success": bool,
                "data": list,
                "message": str
            }
            其中 data 列表包含奴隶信息，每个奴隶包含：
            {
                "thrall_id": int,
                "thrall_type": str,
                "level": int,
                "health": float,
                "owner_id": int,
                "owner_char_name": str,
                "owner_guild_name": str,
                "x": float,
                "y": float,
                "z": float
            }
    """
    if not os.path.exists(db_path):
        return {
            "success": False,
            "data": [],
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        cursor.execute("SELECT object_id FROM properties WHERE name = 'BP_ThrallComponent_C.ThrallInfo'")
        thrall_ids = cursor.fetchall()
        
        follower_owners = {}
        cursor.execute("SELECT follower_id, owner_id FROM follower_markers")
        for follower_id, owner_id in cursor.fetchall():
            follower_owners[follower_id] = owner_id
        
        cursor.execute("SELECT id, char_name, guild FROM characters")
        chars = cursor.fetchall()
        player_info = {}
        for char_id, char_name, guild_id in chars:
            guild_name = get_guild_name(db_path, guild_id)
            player_info[char_id] = {
                'char_name': char_name,
                'guild_id': guild_id,
                'guild_name': guild_name
            }
        
        cursor.execute("SELECT guildId, name FROM guilds")
        guild_info = {r[0]: r[1] for r in cursor.fetchall()}
        
        cursor.execute('''
            SELECT b.object_id, b.owner_id, a.x, a.y, a.z
            FROM buildings b
            JOIN actor_position a ON b.object_id = a.id
            WHERE b.owner_id != 0
        ''')
        all_buildings = cursor.fetchall()
        
        thralls = []
        for (thrall_id,) in thrall_ids:
            cursor.execute("SELECT value FROM properties WHERE object_id = ? AND name = 'BP_ThrallComponent_C.ThrallInfo'", (thrall_id,))
            thrall_info_result = cursor.fetchone()
            
            thrall_type = "未知"
            if thrall_info_result and thrall_info_result[0]:
                try:
                    decoded = thrall_info_result[0].decode('utf-8', errors='ignore')
                    parts = decoded.split('\x00')
                    for part in parts:
                        if part and len(part) > 3 and not part.startswith('\x00'):
                            if '_' in part or part[0].isupper():
                                thrall_type = part
                                break
                except:
                    pass
            
            owner_id = follower_owners.get(thrall_id)
            owner_info = None
            
            if owner_id and owner_id in player_info:
                owner_info = player_info[owner_id]
            else:
                cursor.execute("SELECT x, y, z FROM actor_position WHERE id = ?", (thrall_id,))
                thrall_pos = cursor.fetchone()
                
                if thrall_pos and all_buildings:
                    min_dist = float('inf')
                    nearest_owner = None
                    for b_id, b_owner, bx, by, bz in all_buildings:
                        dist = (bx - thrall_pos[0])**2 + (by - thrall_pos[1])**2 + (bz - thrall_pos[2])**2
                        if dist < min_dist:
                            min_dist = dist
                            nearest_owner = b_owner
                    
                    if nearest_owner and min_dist < 5000**2:
                        owner_id = nearest_owner
                        if nearest_owner in player_info:
                            owner_info = player_info[nearest_owner]
                        elif nearest_owner in guild_info:
                            owner_info = {
                                'char_name': None,
                                'guild_id': nearest_owner,
                                'guild_name': guild_info[nearest_owner]
                            }
            
            if not owner_info:
                owner_info = {
                    'char_name': '未知',
                    'guild_id': None,
                    'guild_name': None
                }
            
            cursor.execute("SELECT x, y, z FROM actor_position WHERE id = ?", (thrall_id,))
            pos_result = cursor.fetchone()
            x, y, z = pos_result if pos_result else (0, 0, 0)
            
            stats = get_thrall_stats(db_path, cursor, thrall_id)
            perks = get_thrall_perks(db_path, cursor, thrall_id)
            inventory = get_thrall_inventory(db_path, cursor, thrall_id)
            
            thrall_name = None
            cursor.execute("SELECT value FROM properties WHERE object_id = ? AND name = 'PersistentHumanoidNPC_C.ThrallName'", (thrall_id,))
            name_result = cursor.fetchone()
            if name_result and name_result[0]:
                try:
                    value = name_result[0]
                    for start in range(16, min(30, len(value))):
                        try:
                            decoded = value[start:].decode('utf-16-le', errors='ignore')
                            name_parts = []
                            for c in decoded:
                                if '\u4e00' <= c <= '\u9fff' or (c.isalpha() and ord(c) < 128):
                                    name_parts.append(c)
                                elif name_parts and len(name_parts) >= 2:
                                    break
                            if len(name_parts) >= 2:
                                thrall_name = ''.join(name_parts)
                                break
                        except:
                            continue
                except:
                    pass
            
            if not thrall_name:
                thrall_name = None
            
            thrall = {
                "thrall_id": thrall_id,
                "thrall_name": thrall_name,
                "thrall_type": thrall_type,
                "level": stats.get('level', 0),
                "health": stats.get('health', 0),
                "owner_id": owner_id,
                "owner_char_name": owner_info['char_name'],
                "owner_guild_id": owner_info['guild_id'],
                "owner_guild_name": owner_info['guild_name'],
                "position": f"{x:.2f} {y:.2f} {z:.2f}",
                "stats": {
                    "food": stats.get('food', 0),
                    "strength": stats.get('strength', 0),
                    "agility": stats.get('agility', 0),
                    "vitality": stats.get('vitality', 0),
                    "grit": stats.get('grit', 0)
                },
                "perks": {
                    "growth_id": perks.get('growth_id'),
                    "perk_type": perks.get('perk_type'),
                    "xp_curve": perks.get('xp_curve'),
                    "perk_1": perks.get('perk_1'),
                    "perk_2": perks.get('perk_2'),
                    "perk_3": perks.get('perk_3')
                },
                "inventory": inventory
            }
            thralls.append(thrall)
        
        conn.close()
        
        return {
            "success": True,
            "data": thralls,
            "message": f"找到 {len(thralls)} 个奴隶"
        }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "data": [],
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "data": [],
            "message": f"发生错误: {str(e)}"
        }

def get_player_info(db_path="game.db", char_name=None, char_id=None):
    """
    获取玩家角色信息
    
    Args:
        db_path (str): 数据库文件路径
        char_name (str): 角色名称
        char_id (int): 角色ID
    
    Returns:
        dict: 包含玩家角色信息的字典
    """
    if not os.path.exists(db_path):
        return {
            "success": False,
            "data": None,
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        if char_id:
            cursor.execute("SELECT id, char_name, guild FROM characters WHERE id = ?", (char_id,))
        elif char_name:
            cursor.execute("SELECT id, char_name, guild FROM characters WHERE char_name = ?", (char_name,))
        else:
            conn.close()
            return {
                "success": False,
                "data": None,
                "message": "请提供 char_name 或 char_id"
            }
        
        result = cursor.fetchone()
        if not result:
            conn.close()
            return {
                "success": False,
                "data": None,
                "message": "未找到角色"
            }
        
        player_id, player_char_name, guild_id = result
        
        guild_name = get_guild_name(db_path, guild_id) if guild_id else None
        
        cursor.execute('''
            SELECT stat_id, stat_value FROM character_stats 
            WHERE char_id = ? AND stat_type = 0
        ''', (player_id,))
        stats = {stat_id: stat_value for stat_id, stat_value in cursor.fetchall()}
        
        attribute_points = 0
        cursor.execute('SELECT value FROM properties WHERE object_id = ? AND name = ?', 
                      (player_id, 'BP_ProgressionSystem_C.AttributePointsUndistributed'))
        ap_result = cursor.fetchone()
        if ap_result and ap_result[0] and len(ap_result[0]) >= 8:
            attribute_points = struct.unpack('<i', ap_result[0][4:8])[0]
        
        food_current = 0
        cursor.execute('SELECT value FROM properties WHERE object_id = ? AND name = ?', 
                      (player_id, 'BP_HungerSystem_C.FoodCurrent'))
        food_result = cursor.fetchone()
        if food_result and food_result[0] and len(food_result[0]) >= 8:
            food_current = struct.unpack('<f', food_result[0][4:8])[0]
        
        cursor.execute('SELECT x, y, z FROM actor_position WHERE id = ?', (player_id,))
        pos_result = cursor.fetchone()
        x, y, z = pos_result if pos_result else (0, 0, 0)
        
        spawn_point = None
        cursor.execute('SELECT value FROM properties WHERE object_id = ? AND name LIKE ?', (player_id, '%BedSpawnTransform%'))
        spawn_result = cursor.fetchone()
        if spawn_result and spawn_result[0]:
            prop_value = spawn_result[0]
            vector_pos = prop_value.find(b'Vector')
            if vector_pos > 0:
                vector_data_start = vector_pos + 6 + 18
                try:
                    translation_values = struct.unpack('<fff', prop_value[vector_data_start:vector_data_start+12])
                    spawn_x, spawn_y, spawn_z = translation_values
                    spawn_point = f"{spawn_x:.2f} {spawn_y:.2f} {spawn_z:.2f}"
                except:
                    pass
        
        player_info = {
            "player_id": player_id,
            "char_name": player_char_name,
            "guild_id": guild_id,
            "guild_name": guild_name,
            "level": int(stats.get(4, 0)),
            "health": stats.get(1, 0),
            "food": food_current,
            "attribute_points": attribute_points,
            "position": f"{x:.2f} {y:.2f} {z:.2f}",
            "spawn_point": spawn_point,
            "stats": {
                "strength": int(stats.get(17, 0)),
                "agility": int(stats.get(19, 0)),
                "vitality": int(stats.get(14, 0)),
                "grit": int(stats.get(15, 0)),
                "authority": int(stats.get(27, 0)),
                "expertise": int(stats.get(16, 0))
            }
        }
        
        conn.close()
        
        return {
            "success": True,
            "data": player_info,
            "message": f"找到角色: {player_char_name}"
        }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "data": None,
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "data": None,
            "message": f"发生错误: {str(e)}"
        }

def get_player_thralls(db_path="game.db", player_id=None, char_name=None, char_id=None):
    """
    获取指定玩家的奴隶信息（通过 follower_markers 表和建筑物匹配）
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
        char_id (int): 角色ID（可选）
    
    Returns:
        dict: 包含奴隶信息的字典
    """
    if not os.path.exists(db_path):
        return {
            "success": False,
            "data": [],
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        if char_id:
            target_char_id = char_id
        elif char_name:
            cursor.execute("SELECT id FROM characters WHERE char_name = ?", (char_name,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": "未找到角色"
                }
            target_char_id = result[0]
        elif player_id:
            cursor.execute("SELECT id FROM characters WHERE playerId = ?", (player_id,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": "未找到角色"
                }
            target_char_id = result[0]
        else:
            conn.close()
            return {
                "success": False,
                "data": [],
                "message": "请提供 player_id、char_name 或 char_id"
            }
        
        cursor.execute("SELECT guild FROM characters WHERE id = ?", (target_char_id,))
        guild_result = cursor.fetchone()
        target_guild_id = guild_result[0] if guild_result else None
        
        conn.close()
        
        all_thralls = get_all_thralls(db_path)
        if not all_thralls["success"]:
            return all_thralls
        
        player_thralls = []
        for t in all_thralls["data"]:
            if t["owner_id"] == target_char_id:
                player_thralls.append(t)
            elif t["owner_guild_id"] == target_guild_id and target_guild_id:
                player_thralls.append(t)
        
        return {
            "success": True,
            "data": player_thralls,
            "message": f"找到 {len(player_thralls)} 个奴隶属于该玩家或其部落"
        }
    
    except sqlite3.Error as e:
        return {
            "success": False,
            "data": [],
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "data": [],
            "message": f"发生错误: {str(e)}"
        }

def get_player_inventory_with_thralls(db_path="game.db", player_id=None, char_name=None, inv_type=None):
    """
    获取玩家的背包物品和奴隶信息（用于服务端查询一起发送）
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
        inv_type (int): 背包类型（可选）
    
    Returns:
        dict: 包含背包物品和奴隶信息的字典，格式为：
            {
                "success": bool,
                "data": {
                    "inventory": list,
                    "thralls": list
                },
                "message": str
            }
    """
    inventory_result = get_player_inventory(db_path, player_id, char_name, inv_type)
    thralls_result = get_player_thralls(db_path, player_id, char_name)
    
    if not inventory_result["success"]:
        return {
            "success": False,
            "data": {
                "inventory": [],
                "thralls": []
            },
            "message": inventory_result["message"]
        }
    
    return {
        "success": True,
        "data": {
            "inventory": inventory_result["data"],
            "thralls": thralls_result["data"] if thralls_result["success"] else []
        },
        "message": f"找到 {len(inventory_result['data'])} 个物品和 {len(thralls_result['data']) if thralls_result['success'] else 0} 个奴隶"
    }

if __name__ == "__main__":
    print("=== Inventory Management Test ===\n")
    
    # Test 1: Query all inventory items for character "离开"
    print("1. Query all inventory items for character '离开'")
    result = get_player_inventory(char_name="离开")
    if result["success"]:
        print(f"{result['message']}\n")
    else:
        print(f"Error: {result['message']}\n")
    
    # Test 2: Query inventory for character "离开" (backpack only)
    print("2. Query inventory for character '离开' (backpack only)")
    result = get_player_inventory(char_name="离开", inv_type=0)
    if result["success"]:
        print(f"{result['message']}")
        for item in result["data"]:
            print(f"  Position ID: {item['item_id']}, Template ID: {item['template_id']}")
        print()
    else:
        print(f"Error: {result['message']}\n")
    
    # Test 3: Update item position ID
    print("3. Update item position ID (change position ID 0 to 100)")
    result = update_item_position(char_name="离开", old_item_id=0, old_inv_type=0, new_item_id=100)
    if result["success"]:
        print(f"{result['message']}\n")
    else:
        print(f"Error: {result['message']}\n")
    
    # Test 4: Update item template ID
    print("4. Update item template ID (change template ID of position 100 to 99999)")
    result = update_item_template(char_name="离开", item_id=100, inv_type=0, new_template_id=99999)
    if result["success"]:
        print(f"{result['message']}\n")
    else:
        print(f"Error: {result['message']}\n")
    
    # Test 5: Update inventory type
    print("5. Update inventory type (change position 100 from backpack to hotbar)")
    result = update_item_position(char_name="离开", old_item_id=100, old_inv_type=0, new_inv_type=2)
    if result["success"]:
        print(f"{result['message']}\n")
    else:
        print(f"Error: {result['message']}\n")
    
    # Test 6: Query modified item
    print("6. Query modified item")
    result = get_player_inventory(char_name="离开")
    if result["success"]:
        print(f"{result['message']}")
        for item in result["data"]:
            if item['item_id'] == 100:
                print(f"  [{item['inv_type_name']}] Position ID: {item['item_id']}, Template ID: {item['template_id']}")
        print()
    else:
        print(f"Error: {result['message']}\n")
    
    # Test 7: Delete item
    print("7. Delete item (delete position 100)")
    result = delete_item(char_name="离开", item_id=100, inv_type=2)
    if result["success"]:
        print(f"{result['message']}\n")
    else:
        print(f"Error: {result['message']}\n")
    
    print("=== Test Complete ===")
