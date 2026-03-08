#!/bin/bash

# RCON客户端一键启动脚本

echo "============================================================"
echo "🚀 RCON客户端启动脚本"
echo "============================================================"

# 检查虚拟环境是否存在
if [ ! -d "venv" ]; then
    echo "❌ 错误：虚拟环境 venv 不存在"
    echo "请先创建虚拟环境：python3 -m venv venv"
    exit 1
fi

# 检查start_server.py是否存在
if [ ! -f "start_server.py" ]; then
    echo "❌ 错误：start_server.py 不存在"
    exit 1
fi

# 检查admin_server.py是否存在
if [ ! -f "admin_server.py" ]; then
    echo "❌ 错误：admin_server.py 不存在"
    exit 1
fi

# 检查端口是否被占用
check_port() {
    local port=$1
    local service_name=$2
    # 使用 ss 命令检测，更可靠
    if ss -tlnp 2>/dev/null | grep -q ":$port "; then
        echo "⚠️  警告：端口 $port 已被占用 ($service_name)"
        echo "请先关闭占用该端口的进程"
        echo "可以使用: kill -9 \$(lsof -ti:$port)"
        return 1
    fi
    return 0
}

# 检查端口8000和8001
if ! check_port 8000 "主服务器"; then
    exit 1
fi

if ! check_port 8001 "管理员后台"; then
    exit 1
fi

echo ""
echo "✅ 检查通过，正在启动服务..."
echo ""

# 启动主服务器（后台运行）
echo "📍 启动主服务器 (端口 8000)..."
./venv/bin/python -u start_server.py &
MAIN_PID=$!

# 等待主服务器启动
sleep 2

# 检查主服务器是否成功启动
if ps -p $MAIN_PID > /dev/null; then
    echo "✅ 主服务器启动成功 (PID: $MAIN_PID)"
else
    echo "❌ 主服务器启动失败"
    exit 1
fi

# 启动管理员后台（后台运行）
echo "📍 启动管理员后台 (端口 8001)..."
./venv/bin/python -u admin_server.py &
ADMIN_PID=$!

# 等待管理员后台启动
sleep 2

# 检查管理员后台是否成功启动
if ps -p $ADMIN_PID > /dev/null; then
    echo "✅ 管理员后台启动成功 (PID: $ADMIN_PID)"
else
    echo "❌ 管理员后台启动失败"
    exit 1
fi

echo ""
echo "============================================================"
echo "✅ 所有服务启动成功！"
echo "============================================================"
echo "📍 主服务器地址: http://localhost:8000"
echo "📍 管理员后台: http://localhost:8001"
echo ""
echo "📝 进程信息:"
echo "   主服务器 PID: $MAIN_PID"
echo "   管理员后台 PID: $ADMIN_PID"
echo ""
echo "⏹️  停止服务请使用: ./stop.sh"
echo "⏹️  或者使用: kill $MAIN_PID $ADMIN_PID"
echo "============================================================"
echo ""

# 将PID保存到文件，方便后续停止
echo $MAIN_PID > .main_server.pid
echo $ADMIN_PID > .admin_server.pid

echo "💡 提示：PID已保存到 .main_server.pid 和 .admin_server.pid"
echo ""
