#!/bin/bash

# RCON客户端Systemd服务卸载脚本

echo "============================================================"
echo "🗑️  RCON客户端Systemd服务卸载"
echo "============================================================"

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    echo "❌ 请使用root权限运行此脚本"
    echo "使用: sudo bash uninstall_service.sh"
    exit 1
fi

# 停止服务
echo "📍 停止服务..."
systemctl stop rcon-client.service 2>/dev/null

# 禁用开机自启动
echo "📍 禁用开机自启动..."
systemctl disable rcon-client.service 2>/dev/null

# 删除服务文件
echo "📍 删除服务文件..."
rm -f /etc/systemd/system/rcon-client.service

if [ $? -eq 0 ]; then
    echo "✅ 服务文件删除成功"
else
    echo "⚠️  服务文件删除失败或不存在"
fi

# 重新加载systemd配置
echo "📍 重新加载systemd配置..."
systemctl daemon-reload

echo ""
echo "============================================================"
echo "✅ 服务卸载完成！"
echo "============================================================"
