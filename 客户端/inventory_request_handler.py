import requests
import json
from inventory_manager import get_player_inventory_by_type, get_player_thralls, get_player_info


class InventoryRequestHandler:
    def __init__(self, server_url, token):
        self.server_url = server_url
        self.token = token
    
    def handle_inventory_request(self, event_data):
        request_id = event_data.get('request_id')
        char_name = event_data.get('data', {}).get('char_name')
        
        if not request_id or not char_name:
            print(f"[Inventory] 无效的背包请求: request_id={request_id}, char_name={char_name}")
            return
        
        print(f"[Inventory] 收到背包数据请求: request_id={request_id}, char_name={char_name}")
        
        try:
            result = get_player_inventory_by_type(char_name=char_name)
            thralls_result = get_player_thralls(char_name=char_name)
            player_info_result = get_player_info(char_name=char_name)
            
            print(f"[Inventory] 背包查询结果: success={result.get('success')}")
            print(f"[Inventory] 奴隶查询结果: success={thralls_result.get('success')}")
            print(f"[Inventory] 玩家信息查询结果: success={player_info_result.get('success')}")
            
            if result.get('success'):
                grouped_data = result.get('data', {})
                
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
                            'light_attack_damage': item.get('light_attack_damage', 0),
                            'heavy_attack_damage': item.get('heavy_attack_damage', 0),
                            'armor_penetration': item.get('armor_penetration', 0.0)
                        }
                        formatted_inventory[target_type][item_id] = formatted_item
                
                formatted_thralls = {}
                if thralls_result.get('success'):
                    for thrall in thralls_result.get('data', []):
                        thrall_id = thrall.get('thrall_id')
                        formatted_thrall = {
                            'thrall_id': thrall.get('thrall_id'),
                            'thrall_name': thrall.get('thrall_name'),
                            'thrall_type': thrall.get('thrall_type'),
                            'level': thrall.get('level'),
                            'health': thrall.get('health'),
                            'owner_guid': thrall.get('owner_guid'),
                            'owner_char_name': thrall.get('owner_char_name'),
                            'owner_guild_id': thrall.get('owner_guild_id'),
                            'owner_guild_name': thrall.get('owner_guild_name'),
                            'position': thrall.get('position'),
                            'stats': thrall.get('stats', {}),
                            'perks': thrall.get('perks', {}),
                            'inventory': thrall.get('inventory', {})
                        }
                        formatted_thralls[thrall_id] = formatted_thrall
                
                response_data = {
                    'type': 'inventory_response',
                    'request_id': request_id,
                    'char_name': char_name,
                    'player_info': player_info_result.get('data') if player_info_result.get('success') else None,
                    'inventory': formatted_inventory,
                    'thralls': formatted_thralls
                }
                
                print(f"[Inventory] 准备发送响应: request_id={request_id}, 背包物品数={sum(len(v) for v in formatted_inventory.values())}, 奴隶数={len(formatted_thralls)}")
                self._send_response(response_data)
                print(f"[Inventory] 背包数据响应已发送: request_id={request_id}")
            else:
                print(f"[Inventory] 获取背包数据失败: {result.get('message')}")
                
        except Exception as e:
            print(f"[Inventory] 处理背包请求时出错: {e}")
            import traceback
            traceback.print_exc()
    
    def _send_response(self, data):
        try:
            url = f"{self.server_url}/api/client/data"
            headers = {
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.token}"
            }
            
            response = requests.post(url, headers=headers, json=data, timeout=10)
            
            if response.status_code == 200:
                print("背包数据响应发送成功")
            else:
                print(f"背包数据响应发送失败: HTTP {response.status_code}")
                
        except requests.exceptions.RequestException as e:
            print(f"发送背包数据响应时出错: {e}")
