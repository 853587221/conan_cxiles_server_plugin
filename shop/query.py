import sqlite3
import sys
import json
from pathlib import Path


def query_player_gold(username: str) -> dict:
    """
    查询指定用户的数据库中所有玩家的 char_name 和 gold

    Args:
        username: 用户名

    Returns:
        dict: 包含成功状态和玩家列表的字典
    """
    db_path = Path(__file__).parent.parent / f"data/{username}/database.db"

    if not db_path.exists():
        return {'success': False, 'message': '用户数据库不存在'}

    try:
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        cursor.execute('SELECT char_name, gold FROM players ORDER BY char_name')

        players = cursor.fetchall()

        conn.close()

        player_list = [
            {'char_name': player[0], 'gold': player[1]}
            for player in players
        ]

        return {
            'success': True,
            'count': len(player_list),
            'players': player_list
        }

    except Exception as e:
        return {'success': False, 'message': f'查询失败: {str(e)}'}


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print("用法: python shop/query.py <用户名>")
        print("示例: python shop/query.py test_user")
        sys.exit(1)

    username = sys.argv[1]
    result = query_player_gold(username)

    if result['success']:
        print(f"\n查询成功！共找到 {result['count']} 个玩家:\n")
        print(f"{'角色名':<20} {'金币':>10}")
        print("-" * 32)
        for player in result['players']:
            print(f"{player['char_name']:<20} {player['gold']:>10}")
        print("-" * 32)
    else:
        print(f"查询失败: {result['message']}")
