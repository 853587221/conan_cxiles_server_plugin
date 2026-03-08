#!/usr/bin/env python3
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from bin.mcrcon import MCRcon

def test_rcon_connection():
    host = '172.22.219.248'
    password = '4896489'
    port = 25575
    
    print(f"正在测试RCON连接...")
    print(f"主机: {host}")
    print(f"端口: {port}")
    print(f"密码: {password}")
    print("-" * 50)
    
    try:
        with MCRcon(host, password, port, timeout=10) as mcr:
            print("✓ RCON连接成功！")
            
            # 发送测试命令
            response = mcr.command('help')
            print(f"\n测试命令 'help' 的响应:")
            print(response[:500] if len(response) > 500 else response)
            
            return True
            
    except Exception as e:
        print(f"✗ RCON连接失败: {str(e)}")
        return False

if __name__ == '__main__':
    success = test_rcon_connection()
    sys.exit(0 if success else 1)
