#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
打包脚本：将 desktop_login_app.py 打包成 exe 可执行文件
使用 PyInstaller 打包
"""

import PyInstaller.__main__
import os
import sys

def build_exe():
    """打包程序为 exe"""
    
    # 确保在当前目录
    current_dir = os.path.dirname(os.path.abspath(__file__))
    os.chdir(current_dir)
    
    # PyInstaller 参数
    args = [
        'desktop_login_app.py',  # 主程序文件
        '--name=RCONDesktopClient',  # 生成的 exe 名称
        '--windowed',  # 使用窗口模式（不显示控制台）
        '--onefile',  # 打包成单个文件
        '--icon=icon_sandstorm_mask.ico',  # 使用自定义图标
        '--add-data=monitoring_log.py;.',  # 包含 monitoring_log.py
        '--add-data=get_player_info.py;.',  # 包含 get_player_info.py
        '--add-data=get_db_info.py;.',  # 包含 get_db_info.py
        '--add-data=sse_client.py;.',  # 包含 sse_client.py
        '--add-data=inventory_request_handler.py;.',  # 包含 inventory_request_handler.py
        '--add-data=inventory_manager.py;.',  # 包含 inventory_manager.py
        '--hidden-import=mcrcon',  # 隐藏导入 mcrcon
        '--hidden-import=configparser',  # 隐藏导入 configparser
        '--hidden-import=requests',  # 隐藏导入 requests
        '--hidden-import=tkinter',  # 隐藏导入 tkinter
        '--clean',  # 清理临时文件
        '--noconfirm',  # 不确认覆盖
    ]
    
    print("开始打包...")
    print(f"工作目录: {current_dir}")
    print(f"打包参数: {' '.join(args)}")
    
    try:
        PyInstaller.__main__.run(args)
        print("\n打包完成！")
        print(f"生成的 exe 文件在: {os.path.join(current_dir, 'dist', 'RCONDesktopClient.exe')}")
    except Exception as e:
        print(f"打包失败: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    build_exe()
