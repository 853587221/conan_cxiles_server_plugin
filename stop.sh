#!/bin/bash

# RCON客户端一键停止脚本

echo "============================================================"
echo "⏹️  RCON客户端停止脚本"
echo "============================================================"

# 从PID文件读取进程ID
if [ -f ".main_server.pid" ]; then
    MAIN_PID=$(cat .main_server.pid)
else
    MAIN_PID=""
fi

if [ -f ".admin_server.pid" ]; then
    ADMIN_PID=$(cat .admin_server.pid)
else
    ADMIN_PID=""
fi

# 如果PID文件不存在，尝试通过端口查找进程
if [ -z "$MAIN_PID" ]; then
    MAIN_PID=$(lsof -ti:8000 2>/dev/null)
fi

if [ -z "$ADMIN_PID" ]; then
    ADMIN_PID=$(lsof -ti:8001 2>/dev/null)
fi

# 停止主服务器
stop_process() {
    local pid=$1
    local name=$2
    local timeout=3
    
    if [ -n "$pid" ] && kill -0 $pid 2>/dev/null; then
        echo "📍 正在停止 $name (PID: $pid)..."
        
        # 首先尝试优雅停止 (SIGTERM)
        kill $pid 2>/dev/null
        
        # 等待进程停止，最多等待 timeout 秒
        for i in $(seq 1 $timeout); do
            if ! kill -0 $pid 2>/dev/null; then
                echo "✅ $name 已停止"
                return 0
            fi
            sleep 1
        done
        
        # 如果进程还在运行，强制杀死 (SIGKILL)
        if kill -0 $pid 2>/dev/null; then
            echo "⚠️  $name 无法正常响应，强制杀死..."
            kill -9 $pid 2>/dev/null
            sleep 1
            
            if ! kill -0 $pid 2>/dev/null; then
                echo "✅ $name 已强制停止"
                return 0
            fi
        fi
        
        echo "❌ $name 停止失败"
        return 1
    else
        echo "ℹ️  $name 未运行"
        return 0
    fi
}

stop_process "$MAIN_PID" "主服务器"
stop_process "$ADMIN_PID" "管理员后台"

# 删除PID文件
rm -f .main_server.pid .admin_server.pid

echo ""
echo "============================================================"
echo "✅ 所有服务已停止"
echo "============================================================"
