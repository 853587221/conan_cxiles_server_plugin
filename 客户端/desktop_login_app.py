import tkinter as tk
from tkinter import messagebox, scrolledtext
import requests
import json
import os
import sys
import time
import winreg
import webbrowser
from monitoring_log import MonitoringLog
from sse_client import SSEClient
from inventory_request_handler import InventoryRequestHandler

class DesktopLoginApp:
    CURRENT_VERSION = "1.1.1"  # 当前版本号
    
    def __init__(self, root):
        self.root = root
        self.root.title(f"RCON 桌面客户端登录 v{self.CURRENT_VERSION}")
        self.root.geometry("400x450")
        self.root.resizable(False, False)
        
        # 设置窗口图标
        icon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon_sandstorm_mask.ico")
        if os.path.exists(icon_path):
            self.root.iconbitmap(icon_path)
        
        check_result, error_message = self.check_directory()
        if not check_result:
            messagebox.showerror("错误", error_message)
            self.root.destroy()
            return
        
        self.config_file = "login_config.json"
        self.server_url = "https://www.xiaolang.icu"
        self.token = None
        self.console_window = None
        self.console_text = None
        self.monitoring = None
        self.sse_client = None
        self.inventory_handler = None
        
        self.load_config()
        self.create_widgets()
        
        # 检查版本更新
        self.check_version_update()
        
        if self.auto_login and self.last_username and self.last_password:
            self.root.after(500, self.login)
    
    def check_version_update(self):
        """检查版本更新"""
        try:
            version_url = f"{self.server_url}/api/version"
            print(f"正在检查版本更新: {version_url}")
            response = requests.get(version_url, timeout=5)
            print(f"服务器响应状态码: {response.status_code}")
            print(f"服务器响应内容: {response.text}")
            result = response.json()
            
            if result.get("success"):
                latest_version = result.get("version", "")
                print(f"当前版本: {self.CURRENT_VERSION}, 最新版本: {latest_version}")
                if latest_version and latest_version != self.CURRENT_VERSION:
                    # 发现新版本
                    download_url = f"{self.server_url}/plug-in/RCONDesktopClient.exe"
                    
                    # 创建更新提示标签
                    update_label = tk.Label(
                        self.update_frame,
                        text=f"发现新版本 {latest_version}！点击此处下载最新版",
                        fg="red",
                        cursor="hand2",
                        font=("Microsoft YaHei UI", 10, "bold")
                    )
                    update_label.pack(pady=2)
                    
                    # 绑定点击事件
                    update_label.bind("<Button-1>", lambda e: webbrowser.open(download_url))
                    print("已显示更新提示")
                else:
                    print("当前已是最新版本")
            else:
                print(f"版本检查失败: {result}")
        except Exception as e:
            # 版本检查失败不影响主程序运行
            print(f"版本检查出错: {str(e)}")
    
    def load_config(self):
        if os.path.exists(self.config_file):
            try:
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    config = json.load(f)
                    self.last_username = config.get('username', '')
                    self.last_password = config.get('password', '')
                    self.auto_login = config.get('auto_login', False)
                    self.server_url = config.get('server_url', 'https://www.xiaolang.icu')
                    self.autostart = config.get('autostart', False)
            except:
                self.last_username = ''
                self.last_password = ''
                self.auto_login = False
                self.server_url = 'https://www.xiaolang.icu'
                self.autostart = False
        else:
            self.last_username = ''
            self.last_password = ''
            self.auto_login = False
            self.server_url = 'https://www.xiaolang.icu'
            self.autostart = False
    
    def save_config(self, username, password, auto_login=False):
        config = {
            'username': username,
            'password': password,
            'auto_login': auto_login,
            'server_url': self.server_url,
            'autostart': self.autostart_var.get()
        }
        with open(self.config_file, 'w', encoding='utf-8') as f:
            json.dump(config, f, ensure_ascii=False, indent=2)
    
    def save_auto_login_only(self):
        config = {
            'username': self.last_username,
            'password': self.last_password,
            'auto_login': self.auto_login_var.get(),
            'server_url': self.server_url,
            'autostart': self.autostart_var.get()
        }
        with open(self.config_file, 'w', encoding='utf-8') as f:
            json.dump(config, f, ensure_ascii=False, indent=2)
    
    def set_autostart(self, enable):
        try:
            key_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
            app_name = "RCONDesktopClient"
            
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, key_path, 0, winreg.KEY_SET_VALUE) as key:
                if enable:
                    python_exe = os.path.abspath(sys.executable)
                    script_path = os.path.abspath(__file__)
                    command = f'"{python_exe}" "{script_path}"'
                    winreg.SetValueEx(key, app_name, 0, winreg.REG_SZ, command)
                else:
                    try:
                        winreg.DeleteValue(key, app_name)
                    except FileNotFoundError:
                        pass
        except Exception as e:
            messagebox.showerror("错误", f"设置开机自启动失败: {str(e)}")
    
    def check_directory(self):
        current_dir = os.getcwd()
        
        # 检查是否在 Saved 目录下
        if not current_dir.endswith('Saved'):
            return False, "请把此插件置于游戏服务端的ConanSandbox\\Saved目录下！"
        
        # 检查 Logs 目录是否存在
        logs_dir = os.path.join(current_dir, 'Logs')
        if not os.path.exists(logs_dir):
            return False, "未检测到Logs日志文件，如果是首次运行游戏服务端，请运行一次让服务端生成日志文件！"
        
        return True, ""
    
    def open_web_console(self, event):
        webbrowser.open(self.server_url)
    
    def create_widgets(self):
        title_label = tk.Label(
            self.root,
            text="RCON 桌面客户端",
            font=("Microsoft YaHei UI", 16)
        )
        title_label.pack(pady=20)
        
        # 版本更新提示容器
        self.update_frame = tk.Frame(self.root)
        self.update_frame.pack(pady=5)
        
        tk.Label(self.root, text="用户名:", font=("Microsoft YaHei UI", 10)).pack(pady=5)
        self.username_entry = tk.Entry(self.root, width=30, font=("Microsoft YaHei UI", 10))
        if self.last_username:
            self.username_entry.insert(0, self.last_username)
        self.username_entry.pack(pady=5)
        
        tk.Label(self.root, text="密码:", font=("Microsoft YaHei UI", 10)).pack(pady=5)
        self.password_entry = tk.Entry(self.root, width=30, show="*", font=("Microsoft YaHei UI", 10))
        if self.last_password:
            self.password_entry.insert(0, self.last_password)
        self.password_entry.pack(pady=5)
        
        tk.Label(self.root, text="登录地址:", font=("Microsoft YaHei UI", 10)).pack(pady=5)
        self.server_url_entry = tk.Entry(self.root, width=30, font=("Microsoft YaHei UI", 10))
        self.server_url_entry.insert(0, self.server_url)
        self.server_url_entry.pack(pady=5)
        
        checkbox_frame = tk.Frame(self.root)
        checkbox_frame.pack(pady=5)
        
        self.auto_login_var = tk.BooleanVar(value=self.auto_login)
        self.auto_login_check = tk.Checkbutton(
            checkbox_frame,
            text="自动登录",
            variable=self.auto_login_var,
            command=self.save_auto_login_only,
            font=("Microsoft YaHei UI", 10)
        )
        self.auto_login_check.pack(side=tk.LEFT, padx=10)
        
        self.autostart_var = tk.BooleanVar(value=self.autostart)
        self.autostart_check = tk.Checkbutton(
            checkbox_frame,
            text="开机自启动",
            variable=self.autostart_var,
            command=lambda: self.set_autostart(self.autostart_var.get()),
            font=("Microsoft YaHei UI", 10)
        )
        self.autostart_check.pack(side=tk.LEFT, padx=10)
        
        self.login_button = tk.Button(
            self.root,
            text="登录",
            command=self.login,
            width=20,
            height=2,
            font=("Microsoft YaHei UI", 12)
        )
        self.login_button.pack(pady=20)
        
        self.open_web_label = tk.Label(
            self.root,
            text="打开网页操作台或者注册",
            fg="blue",
            cursor="hand2",
            font=("Microsoft YaHei UI", 10)
        )
        self.open_web_label.pack(pady=5)
        self.open_web_label.bind("<Button-1>", self.open_web_console)
        
        self.status_label = tk.Label(self.root, text="", fg="blue", font=("Microsoft YaHei UI", 10))
        self.status_label.pack(pady=10)
    
    def create_console_window(self):
        self.console_window = tk.Toplevel(self.root)
        self.console_window.title(f"监控日志控制台 v{self.CURRENT_VERSION}")
        self.console_window.geometry("800x600")
        self.console_window.protocol("WM_DELETE_WINDOW", self.on_console_close)
        
        # 设置窗口图标
        icon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon_sandstorm_mask.ico")
        if os.path.exists(icon_path):
            self.console_window.iconbitmap(icon_path)
        
        self.console_text = scrolledtext.ScrolledText(
            self.console_window,
            wrap=tk.WORD,
            font=("Consolas", 10)
        )
        self.console_text.pack(expand=True, fill='both', padx=10, pady=10)
        
        stop_button = tk.Button(
            self.console_window,
            text="停止监控",
            command=self.stop_monitoring,
            font=("Microsoft YaHei UI", 10)
        )
        stop_button.pack(pady=5)
    
    def on_console_close(self):
        self.stop_monitoring()
        self.stop_sse_monitoring()
        self.console_window.destroy()
        self.root.deiconify()
        self.login_button.config(state="normal")
    
    def log_to_console(self, message):
        if self.console_text:
            self.console_text.insert(tk.END, message + '\n')
            self.console_text.see(tk.END)
            self.console_window.update()
    
    def send_data_to_server(self, data, max_retries=5, retry_delay=1):
        for attempt in range(max_retries):
            try:
                url = f"{self.server_url}/api/client/data"
                headers = {
                    "Content-Type": "application/json",
                    "Authorization": f"Bearer {self.token}"
                }
                response = requests.post(url, headers=headers, json=data, timeout=5)
                return response.status_code == 200
            except Exception as e:
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                    continue
                else:
                    self.log_to_console(f"发送数据失败（已重试{max_retries}次）: {str(e)}")
                    return False
    
    def handle_monitoring_data(self, data):
        if data.get("type") == "log":
            self.log_to_console(data.get("message", ""))
        elif data.get("type") == "server_stats":
            pass
        else:
            self.send_data_to_server(data)
    
    def handle_sse_event(self, event_data):
        event_type = event_data.get('type')
        
        if event_type == 'inventory_request':
            if self.inventory_handler:
                self.inventory_handler.handle_inventory_request(event_data)
        elif event_type == 'rcon_command':
            self.handle_rcon_command(event_data)
    
    def handle_rcon_command(self, event_data):
        """处理通过SSE发送的RCON命令"""
        try:
            from get_player_info import execute_rcon_command
            
            request_id = event_data.get('request_id')
            command = event_data.get('command')
            
            if not command:
                self.log_to_console(f"[SSE RCON] 收到空命令")
                return
            
            self.log_to_console(f"[SSE RCON] 收到服务器执行命令请求")
            
            result = execute_rcon_command(command)
            
            response_data = {
                'type': 'rcon_response',
                'request_id': request_id,
                'command': command,
                'success': result.get('success', False),
                'response': result.get('response', ''),
                'message': result.get('message', '')
            }
            
            self.send_data_to_server(response_data)
            
            if result['success']:
                self.log_to_console(f"[SSE RCON] 执行服务器命令成功")
            else:
                self.log_to_console(f"[SSE RCON] 命令执行失败: {result.get('message', '')}")
        except Exception as e:
            self.log_to_console(f"[SSE RCON] 处理命令时出错: {str(e)}")
            import traceback
            traceback.print_exc()
    
    def start_sse_monitoring(self):
        if self.sse_client:
            self.sse_client.stop()
        
        self.sse_client = SSEClient(
            self.server_url,
            self.token,
            event_callback=self.handle_sse_event
        )
        self.sse_client.start()
        self.log_to_console("SSE监听已启动")
    
    def stop_sse_monitoring(self):
        if self.sse_client:
            self.sse_client.stop()
            self.sse_client = None
            self.log_to_console("SSE监听已停止")
    
    def start_monitoring(self):
        self.monitoring = MonitoringLog(data_callback=self.handle_monitoring_data)
        self.monitoring.start()
    
    def stop_monitoring(self):
        if self.monitoring:
            self.monitoring.stop()
    
    def login(self):
        username = self.username_entry.get().strip()
        password = self.password_entry.get().strip()
        server_url = self.server_url_entry.get().strip()
        
        if not username or not password:
            messagebox.showerror("错误", "请填写用户名和密码")
            return
        
        if not server_url:
            messagebox.showerror("错误", "请填写服务器地址")
            return
        
        self.server_url = server_url
        
        self.status_label.config(text="正在登录...", fg="blue")
        self.login_button.config(state="disabled")
        self.root.update()
        
        try:
            login_url = f"{self.server_url}/api/desktop/login"
            
            response = requests.post(
                login_url,
                headers={"Content-Type": "application/json"},
                json={"username": username, "password": password},
                timeout=10
            )
            
            result = response.json()
            
            if result.get("success"):
                self.save_config(username, password, self.auto_login_var.get())
                self.token = result.get("token")
                self.status_label.config(text="登录成功！", fg="green")
                
                self.inventory_handler = InventoryRequestHandler(self.server_url, self.token)
                
                self.create_console_window()
                self.root.iconify()
                self.start_monitoring()
                self.start_sse_monitoring()
            else:
                self.status_label.config(text="登录失败", fg="red")
                messagebox.showerror("登录失败", result.get('message', '未知错误'))
                self.login_button.config(state="normal")
                
        except requests.exceptions.ConnectionError:
            self.status_label.config(text="无法连接到服务器", fg="red")
            messagebox.showerror("连接错误", f"无法连接到服务器: {self.server_url}\n请检查服务器是否运行")
            self.login_button.config(state="normal")
        except requests.exceptions.Timeout:
            self.status_label.config(text="连接超时", fg="red")
            messagebox.showerror("超时错误", "连接超时，请检查网络或增加超时时间")
            self.login_button.config(state="normal")
        except json.JSONDecodeError:
            self.status_label.config(text="服务器响应错误", fg="red")
            messagebox.showerror("响应错误", "服务器响应格式错误")
            self.login_button.config(state="normal")
        except Exception as e:
            self.status_label.config(text="发生错误", fg="red")
            messagebox.showerror("错误", f"发生错误: {str(e)}")
            self.login_button.config(state="normal")

if __name__ == "__main__":
    import os
    import sys
    
    # 获取程序所在目录（兼容打包后的exe和脚本）
    if getattr(sys, 'frozen', False):
        # 打包后的exe文件
        script_dir = os.path.dirname(os.path.abspath(sys.executable))
    else:
        # 脚本运行
        script_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 确保工作目录是程序所在目录
    if script_dir:
        os.chdir(script_dir)
    
    root = tk.Tk()
    app = DesktopLoginApp(root)
    root.mainloop()
