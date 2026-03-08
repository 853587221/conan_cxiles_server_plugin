import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), '..', '客户端'))

from inventory_manager import get_player_inventory_by_type


def create_inventory_request_event(char_name):
    """创建背包数据请求事件"""
    return {
        'type': 'inventory_request',
        'data': {
            'char_name': char_name
        }
    }


def create_inventory_response_event(char_name, inventory_data):
    """创建背包数据响应事件"""
    return {
        'type': 'inventory_response',
        'data': {
            'char_name': char_name,
            'inventory': inventory_data
        }
    }


def format_inventory_for_display(inventory_result):
    """格式化背包数据用于前端显示
    
    Args:
        inventory_result: get_player_inventory_by_type 的返回结果
    
    Returns:
        dict: 格式化后的背包数据，包含按背包类型和位置ID组织的物品
    """
    if not inventory_result.get('success'):
        return {
            'success': False,
            'message': inventory_result.get('message', '获取背包数据失败'),
            'inventory': {}
        }
    
    grouped_data = inventory_result.get('data', {})
    
    formatted_inventory = {
        'backpack': {},
        'equipment': {},
        'quickbar': {}
    }
    
    inv_type_mapping = {
        '背包': 'backpack',
        '装备栏': 'equipment',
        '快捷栏': 'quickbar'
    }
    
    for inv_type_name, items in grouped_data.items():
        if inv_type_name in ['技能点/知识', '表情动作']:
            continue
        
        target_type = inv_type_mapping.get(inv_type_name)
        if not target_type:
            continue
        
        for item in items:
            item_id = item.get('item_id')
            formatted_item = {
                'item_id': item.get('item_id'),
                'template_id': item.get('template_id'),
                'inv_type': item.get('inv_type'),
                'quantity': item.get('quantity'),
                'item_path': item.get('item_path'),
                'instance_name': item.get('instance_name'),
                'char_name': item.get('char_name'),
                'player_id': item.get('player_id'),
                'properties': item.get('properties', [])
            }
            formatted_inventory[target_type][item_id] = formatted_item
    
    return {
        'success': True,
        'message': inventory_result.get('message', ''),
        'inventory': formatted_inventory
    }


def get_player_inventory_data(char_name, db_path="game.db"):
    """获取玩家背包数据
    
    Args:
        char_name: 角色名称
        db_path: 数据库路径
    
    Returns:
        dict: 格式化后的背包数据
    """
    result = get_player_inventory_by_type(db_path=db_path, char_name=char_name)
    return format_inventory_for_display(result)
