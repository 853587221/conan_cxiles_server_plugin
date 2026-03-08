import sqlite3

db_path = r'd:\LFZKN\Conan Exiles_web_server\RconClient\data\853587221\database.db'

conn = sqlite3.connect(db_path)
cursor = conn.cursor()

print("=== 查询auto_trigger_rules表 ===")
cursor.execute('SELECT * FROM auto_trigger_rules')
rules = cursor.fetchall()

for rule in rules:
    print(f"\n规则ID: {rule[0]}")
    print(f"规则名称: {rule[1]}")
    print(f"是否启用: {rule[2]}")
    print(f"执行类型: {rule[3]}")
    print(f"命令: {rule[4]}")
    print(f"分类ID: {rule[5]}")
    print(f"条件: {rule[6]}")
    print(f"后操作: {rule[7]}")

conn.close()
