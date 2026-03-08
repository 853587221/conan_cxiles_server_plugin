#!/bin/bash

# RCON客户端Systemd服务安装脚本

echo "============================================================"
echo "📦 RCON客户端Systemd服务安装"
echo "============================================================"

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
    echo "❌ 请使用root权限运行此脚本"
    echo "使用: sudo bash install_service.sh"
    exit 1
fi

# 检查服务文件是否存在
if [ ! -f "rcon-client.service" ]; then
    echo "❌ 错误：rcon-client.service 不存在"
    exit 1
fi

# 复制服务文件到systemd目录
echo "📍 复制服务文件到 /etc/systemd/system/..."
cp rcon-client.service /etc/systemd/system/

if [ $? -ne 0 ]; then
    echo "❌ 复制服务文件失败"
    exit 1
fi

echo "✅ 服务文件复制成功"

# 重新加载systemd配置
echo "📍 重新加载systemd配置..."
systemctl daemon-reload

if [ $? -ne 0 ]; then
    echo "❌ 重新加载systemd配置失败"
    exit 1
fi

echo "✅ systemd配置重新加载成功"

# 启用开机自启动
echo "📍 启用开机自启动..."
systemctl enable rcon-client.service

if [ $? -ne 0 ]; then
    echo "❌ 启用开机自启动失败"
    exit 1
fi

echo "✅ 开机自启动已启用"

# 启动服务
echo "📍 启动服务..."
systemctl start rcon-client.service

if [ $? -ne 0 ]; then
    echo "❌ 启动服务失败"
    echo "请检查日志: journalctl -u rcon-client.service -f"
    exit 1
fi

echo "✅ 服务启动成功"

echo ""
echo "============================================================"
echo "✅ 服务安装完成！"
echo "============================================================"
echo ""
echo "📝 常用命令:"
echo "   查看状态: systemctl status rcon-client.service"
echo "   启动服务: systemctl start rcon-client.service"
echo "   停止服务: systemctl stop rcon-client.service"
echo "   重启服务: systemctl restart rcon-client.service"
echo "   查看日志: journalctl -u rcon-client.service -f"
echo ""
echo "📍 服务地址:"
echo "   主服务器: http://localhost:8000"
echo "   管理员后台: http://localhost:8001"
echo ""
echo "============================================================"
