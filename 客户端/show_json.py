import sys
sys.path.insert(0, r'd:\LFZKN\DedicatedServerLauncher\ConanExilesDedicatedServer\ConanSandbox\Saved')
from inventory_manager import get_player_thralls
import json

result = get_player_thralls(char_name='与额')

for thrall in result['data']:
    if thrall['thrall_id'] == 37:
        print(json.dumps(thrall, indent=2, ensure_ascii=False))
