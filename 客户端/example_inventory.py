from inventory_manager import get_player_inventory
import json
from datetime import datetime

char_name = "怎么"
output_file = f"inventory_{char_name.replace(' ', '_')}_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"

print(f"=== 查询角色 '{char_name}' 的背包物品 ===\n")

result = get_player_inventory(char_name=char_name)

if result["success"]:
    inventory_data = {
        "char_name": char_name,
        "player_id": result["data"][0]["player_id"] if result["data"] else None,
        "total_items": len(result["data"]),
        "items": result["data"]
    }
    
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(inventory_data, f, ensure_ascii=False, indent=2)
    
    print(f"成功导出 {len(result['data'])} 个物品到文件: {output_file}\n")
    
    for item in result["data"]:
        print(f"位置ID: {item['item_id']}, 模板ID: {item['template_id']}, 背包类型: {item['inv_type_name']} ({item['inv_type']})")
        print(f"  物品路径: {item['item_path']}")
        print(f"  实例名称: {item['instance_name']}")
        print(f"  数量: {item['quantity']}")
        if item["properties"]:
            print(f"  属性 ({len(item['properties'])} 个):")
            for prop in item["properties"]:
                print(f"    {prop['name']}: {prop['value_hex']}")
        print()
else:
    print(f"错误: {result['message']}")
