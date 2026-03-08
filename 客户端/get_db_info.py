import sqlite3
import os
import struct

def get_player_info_from_db(db_path="game.db"):
    """
    从 game.db 数据库中获取 characters 表的所有信息
    
    Args:
        db_path (str): 数据库文件路径，默认为当前目录下的 game.db
    
    Returns:
        dict: 包含查询结果的字典，格式为：
            {
                "success": bool,
                "data": list,
                "columns": list,
                "message": str
            }
            其中 data 列表包含所有角色信息，columns 列表包含列名
    """
    if not os.path.exists(db_path):
        return {
            "success": False,
            "data": [],
            "columns": [],
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 检查表是否存在
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='characters'")
        if not cursor.fetchone():
            conn.close()
            return {
                "success": False,
                "data": [],
                "columns": [],
                "message": "characters 表不存在"
            }
        
        # 获取列名
        cursor.execute("PRAGMA table_info(characters)")
        columns_info = cursor.fetchall()
        columns = [col[1] for col in columns_info]
        
        # 查询所有数据
        cursor.execute("SELECT * FROM characters")
        rows = cursor.fetchall()
        
        # 将结果转换为字典列表
        data = []
        for row in rows:
            player_dict = {}
            for idx, col in enumerate(columns):
                player_dict[col] = row[idx]
            data.append(player_dict)
        
        conn.close()
        
        return {
            "success": True,
            "data": data,
            "columns": columns,
            "message": f"成功获取 {len(data)} 条角色信息"
        }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "data": [],
            "columns": [],
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "data": [],
            "columns": [],
            "message": f"发生错误: {str(e)}"
        }

def query_player_info_by_field(db_path="game.db", field=None, value=None):
    """
    根据字段和值查询 characters 表
    
    Args:
        db_path (str): 数据库文件路径
        field (str): 要查询的字段名
        value: 要查询的值
    
    Returns:
        dict: 包含查询结果的字典
    """
    if not field or value is None:
        return {
            "success": False,
            "data": [],
            "message": "请提供字段名和查询值"
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
        
        # 检查表是否存在
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='characters'")
        if not cursor.fetchone():
            conn.close()
            return {
                "success": False,
                "data": [],
                "message": "characters 表不存在"
            }
        
        # 检查字段是否存在
        cursor.execute("PRAGMA table_info(characters)")
        columns_info = cursor.fetchall()
        columns = [col[1] for col in columns_info]
        
        if field not in columns:
            conn.close()
            return {
                "success": False,
                "data": [],
                "message": f"字段 '{field}' 不存在，可用字段: {', '.join(columns)}"
            }
        
        # 执行查询
        query = f"SELECT * FROM characters WHERE {field} = ?"
        cursor.execute(query, (value,))
        rows = cursor.fetchall()
        
        # 转换为字典列表
        data = []
        for row in rows:
            player_dict = {}
            for idx, col in enumerate(columns):
                player_dict[col] = row[idx]
            data.append(player_dict)
        
        conn.close()
        
        return {
            "success": True,
            "data": data,
            "message": f"找到 {len(data)} 条匹配记录"
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

def get_player_info_columns(db_path="game.db"):
    """
    获取 characters 表的列名
    
    Args:
        db_path (str): 数据库文件路径
    
    Returns:
        dict: 包含列名的字典
    """
    if not os.path.exists(db_path):
        return {
            "success": False,
            "columns": [],
            "message": f"数据库文件不存在: {db_path}"
        }
    
    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 检查表是否存在
        cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name='characters'")
        if not cursor.fetchone():
            conn.close()
            return {
                "success": False,
                "columns": [],
                "message": "characters 表不存在"
            }
        
        # 获取列名
        cursor.execute("PRAGMA table_info(characters)")
        columns_info = cursor.fetchall()
        columns = [col[1] for col in columns_info]
        
        conn.close()
        
        return {
            "success": True,
            "columns": columns,
            "message": f"成功获取 {len(columns)} 个字段"
        }
        
    except sqlite3.Error as e:
        return {
            "success": False,
            "columns": [],
            "message": f"数据库错误: {str(e)}"
        }
    except Exception as e:
        return {
            "success": False,
            "columns": [],
            "message": f"发生错误: {str(e)}"
        }

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

def get_player_spawn_points(db_path="game.db", player_id=None, char_name=None):
    """
    获取角色的床铺/复活点坐标（从 properties 表中解析）
    
    Args:
        db_path (str): 数据库文件路径
        player_id (str): 玩家ID（可选）
        char_name (str): 角色名称（可选）
    
    Returns:
        dict: 包含床铺坐标信息的字典，格式为：
            {
                "success": bool,
                "data": list,
                "message": str
            }
            其中 data 列表包含床铺信息，每个床铺包含：
            {
                "id": int,
                "bedroll_id": str,
                "x": float,
                "y": float,
                "z": float,
                "char_name": str,
                "player_id": str,
                "guild_id": int,
                "guild_name": str
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
        
        # 查询角色信息（包含部落ID）
        if char_name and not player_id:
            cursor.execute("SELECT id, playerId, char_name, guild FROM characters WHERE char_name = ?", (char_name,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": f"未找到角色名为 '{char_name}' 的角色"
                }
            char_id, player_id, char_name, guild_id = result
        else:
            cursor.execute("SELECT id, playerId, char_name, guild FROM characters WHERE playerId = ?", (player_id,))
            result = cursor.fetchone()
            if not result:
                conn.close()
                return {
                    "success": False,
                    "data": [],
                    "message": f"未找到玩家ID为 '{player_id}' 的角色"
                }
            char_id, player_id, char_name, guild_id = result
        
        # 获取部落名称
        guild_name = get_guild_name(db_path, guild_id)
        
        # 查询床铺坐标
        cursor.execute("SELECT name, value FROM properties WHERE object_id = ? AND name LIKE '%BedSpawnTransform%'", (char_id,))
        spawn_props = cursor.fetchall()
        
        # 查询床铺ID
        cursor.execute("SELECT name, value FROM properties WHERE object_id = ? AND name LIKE '%bedrollID%'", (char_id,))
        bedroll_props = cursor.fetchall()
        
        # 解析床铺ID
        bedroll_id = None
        for prop_name, prop_value in bedroll_props:
            try:
                decoded = prop_value.decode('utf-8', errors='ignore')
                if 'UniqueID' in decoded:
                    bedroll_id = decoded.split('UniqueID')[-1].strip('\x00')
                    break
            except:
                pass
        
        # 解析床铺坐标
        spawn_points = []
        for prop_name, prop_value in spawn_props:
            # 步骤1: 查找 "Vector" 字符串位置
            vector_pos = prop_value.find(b'Vector')
            
            if vector_pos > 0:
                # 步骤2: 计算坐标数据的起始位置
                # + 6: 跳过 "Vector" 字符串本身（6字节）
                # + 18: 跳过 StructProperty 头部（18字节）
                vector_data_start = vector_pos + 6 + 18
                
                # 步骤3: 解析 3 个 float 值（X, Y, Z 坐标）
                try:
                    translation_values = struct.unpack('<fff', prop_value[vector_data_start:vector_data_start+12])
                    x, y, z = translation_values
                    
                    spawn_point = {
                        "id": char_id,
                        "bedroll_id": bedroll_id,
                        "x": x,
                        "y": y,
                        "z": z,
                        "char_name": char_name,
                        "player_id": player_id,
                        "guild_id": guild_id,
                        "guild_name": guild_name
                    }
                    spawn_points.append(spawn_point)
                except:
                    pass
        
        conn.close()
        
        if spawn_points:
            return {
                "success": True,
                "data": spawn_points,
                "message": f"找到 {len(spawn_points)} 个床铺/复活点"
            }
        else:
            return {
                "success": True,
                "data": [],
                "message": f"该角色没有床铺/复活点"
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

def get_all_spawn_points(db_path="game.db"):
    """
    获取所有床铺/复活点坐标（从 properties 表中解析）
    
    Args:
        db_path (str): 数据库文件路径
    
    Returns:
        dict: 包含所有床铺坐标信息的字典，每个床铺包含：
            {
                "id": int,
                "bedroll_id": str,
                "x": float,
                "y": float,
                "z": float,
                "char_name": str,
                "player_id": str,
                "guild_id": int,
                "guild_name": str
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
        
        # 获取所有角色（包含部落ID）
        cursor.execute("SELECT id, playerId, char_name, guild FROM characters")
        characters = cursor.fetchall()
        
        all_spawn_points = []
        for char_id, player_id, char_name, guild_id in characters:
            # 获取部落名称
            guild_name = get_guild_name(db_path, guild_id)
            
            # 查询床铺坐标
            cursor.execute("SELECT name, value FROM properties WHERE object_id = ? AND name LIKE '%BedSpawnTransform%'", (char_id,))
            spawn_props = cursor.fetchall()
            
            # 查询床铺ID
            cursor.execute("SELECT name, value FROM properties WHERE object_id = ? AND name LIKE '%bedrollID%'", (char_id,))
            bedroll_props = cursor.fetchall()
            
            # 解析床铺ID
            bedroll_id = None
            for prop_name, prop_value in bedroll_props:
                try:
                    decoded = prop_value.decode('utf-8', errors='ignore')
                    if 'UniqueID' in decoded:
                        bedroll_id = decoded.split('UniqueID')[-1].strip('\x00')
                        break
                except:
                    pass
            
            # 解析床铺坐标
            for prop_name, prop_value in spawn_props:
                # 步骤1: 查找 "Vector" 字符串位置
                vector_pos = prop_value.find(b'Vector')
                
                if vector_pos > 0:
                    # 步骤2: 计算坐标数据的起始位置
                    vector_data_start = vector_pos + 6 + 18
                    
                    # 步骤3: 解析 3 个 float 值（X, Y, Z 坐标）
                    try:
                        translation_values = struct.unpack('<fff', prop_value[vector_data_start:vector_data_start+12])
                        x, y, z = translation_values
                        
                        spawn_point = {
                            "id": char_id,
                            "bedroll_id": bedroll_id,
                            "x": x,
                            "y": y,
                            "z": z,
                            "char_name": char_name,
                            "player_id": player_id,
                            "guild_id": guild_id,
                            "guild_name": guild_name
                        }
                        all_spawn_points.append(spawn_point)
                    except:
                        pass
        
        conn.close()
        
        return {
            "success": True,
            "data": all_spawn_points,
            "message": f"找到 {len(all_spawn_points)} 个床铺/复活点"
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

if __name__ == "__main__":
    print("=== 获取所有角色信息 ===")
    result = get_player_info_from_db()
    if result["success"]:
        print(f"{result['message']}")
        print(f"字段: {', '.join(result['columns'])}")
        for idx, player in enumerate(result["data"][:3], 1):
            print(f"{idx}. {player}")
    else:
        print(f"错误: {result['message']}")
    
    print("\n=== 获取列名 ===")
    columns_result = get_player_info_columns()
    if columns_result["success"]:
        print(f"{columns_result['message']}")
        print(f"字段: {', '.join(columns_result['columns'])}")
    else:
        print(f"错误: {columns_result['message']}")
    
    print("\n=== 获取所有床铺/复活点 ===")
    spawn_result = get_all_spawn_points()
    if spawn_result["success"]:
        print(f"{spawn_result['message']}")
        for idx, bed in enumerate(spawn_result["data"][:5], 1):
            print(f"{idx}. 角色: {bed['char_name']} (ID: {bed['player_id']})")
            print(f"   部落: {bed['guild_name'] if bed['guild_name'] else '无部落'}")
            print(f"   床铺ID: {bed['bedroll_id']}")
            print(f"   坐标: {bed['x']:.2f}, {bed['y']:.2f}, {bed['z']:.2f}")
    else:
        print(f"错误: {spawn_result['message']}")
