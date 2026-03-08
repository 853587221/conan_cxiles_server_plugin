#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import json
import sqlite3

PLAYER_INFO_DIR = '/www/wwwroot/LFZKN/RconClient/player_info'
DATABASE_PATH = '/www/wwwroot/LFZKN/RconClient/data/853587221/database.db'

def sync_players():
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()
    
    json_files = [f for f in os.listdir(PLAYER_INFO_DIR) if f.endswith('.json')]
    
    inserted_count = 0
    updated_count = 0
    error_count = 0
    
    for json_file in json_files:
        json_path = os.path.join(PLAYER_INFO_DIR, json_file)
        
        try:
            with open(json_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            player_name = data.get('Player_name', '')
            if not player_name:
                print(f"跳过 {json_file}: 缺少 Player_name")
                continue
            
            idx = data.get('idx', '')
            char_name = data.get('char_name', '')
            user_id = data.get('user_id', '')
            platform_id = data.get('platform_id', '')
            platform_name = data.get('platform_name', '')
            level = data.get('level', 1)
            gold = round(float(data.get('gold', 0)), 1)
            qq_member = data.get('member_openid', '')
            
            cursor.execute("SELECT id FROM players WHERE player_name = ?", (player_name,))
            existing = cursor.fetchone()
            
            if existing:
                cursor.execute("""
                    UPDATE players SET 
                        idx = ?,
                        char_name = ?,
                        user_id = ?,
                        platform_id = ?,
                        platform_name = ?,
                        level = ?,
                        gold = ?,
                        qq_member = ?
                    WHERE player_name = ?
                """, (idx, char_name, user_id, platform_id, platform_name, level, gold, qq_member, player_name))
                updated_count += 1
                print(f"更新: {player_name}")
            else:
                cursor.execute("""
                    INSERT INTO players (idx, char_name, player_name, user_id, platform_id, platform_name, level, gold, qq_member)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, (idx, char_name, player_name, user_id, platform_id, platform_name, level, gold, qq_member))
                inserted_count += 1
                print(f"新增: {player_name}")
                
        except Exception as e:
            error_count += 1
            print(f"处理 {json_file} 时出错: {e}")
    
    conn.commit()
    conn.close()
    
    print(f"\n同步完成!")
    print(f"新增: {inserted_count} 条")
    print(f"更新: {updated_count} 条")
    print(f"错误: {error_count} 条")

if __name__ == '__main__':
    sync_players()
