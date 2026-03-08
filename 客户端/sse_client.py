import requests
import json
import threading
import time
import queue


class SSEClient:
    def __init__(self, server_url, token, event_callback=None):
        self.server_url = server_url
        self.token = token
        self.event_callback = event_callback
        self.running = False
        self.thread = None
        self.event_queue = queue.Queue()
        self.worker_thread = None
    
    def start(self):
        if self.running:
            return
        
        self.running = True
        
        self.thread = threading.Thread(target=self._listen, daemon=True)
        self.thread.start()
        
        self.worker_thread = threading.Thread(target=self._process_events, daemon=True)
        self.worker_thread.start()
    
    def stop(self):
        self.running = False
        if self.thread:
            self.thread.join(timeout=5)
        if self.worker_thread:
            self.worker_thread.join(timeout=5)
    
    def _process_events(self):
        while self.running:
            try:
                event_data = self.event_queue.get(timeout=1)
                if self.event_callback:
                    self.event_callback(event_data)
            except queue.Empty:
                continue
            except Exception as e:
                print(f"[SSE Worker] 处理事件时出错: {e}")
    
    def _listen(self):
        while self.running:
            try:
                url = f"{self.server_url}/api/events"
                headers = {
                    "Authorization": f"Bearer {self.token}"
                }
                
                print(f"[SSE] 尝试连接到: {url}")
                print(f"[SSE] Token: {self.token[:20]}..." if len(self.token) > 20 else f"[SSE] Token: {self.token}")
                
                response = requests.get(url, headers=headers, stream=True, timeout=60)
                
                print(f"[SSE] 连接状态: {response.status_code}")
                
                if response.status_code == 200:
                    print(f"[SSE] 开始接收事件流...")
                    event_count = 0
                    buffer = b""
                    
                    for chunk in response.iter_content(chunk_size=1):
                        if not self.running:
                            break
                        
                        buffer += chunk
                        
                        if b"\n\n" in buffer:
                            messages = buffer.split(b"\n\n")
                            buffer = messages.pop()
                            
                            for message in messages:
                                message = message.strip()
                                if not message:
                                    continue
                                
                                try:
                                    message_str = message.decode('utf-8')
                                except UnicodeDecodeError as e:
                                    print(f"[SSE] 解码错误: {e}")
                                    continue
                                
                                for line in message_str.split("\n"):
                                    line = line.strip()
                                    if not line:
                                        continue
                                    
                                    if line.startswith("data:"):
                                        data_str = line[5:].strip()
                                        try:
                                            event_data = json.loads(data_str)
                                            event_count += 1
                                            self.event_queue.put_nowait(event_data)
                                        except json.JSONDecodeError as e:
                                            print(f"[SSE] JSON解析错误: {e}, 数据: {data_str[:100]}")
                                    elif line.startswith(":"):
                                        print(f"[SSE] 收到控制消息: {line}")
                else:
                    print(f"[SSE] 连接失败，状态码: {response.status_code}")
                    if self.running:
                        time.sleep(5)
                        
            except requests.exceptions.RequestException as e:
                if self.running:
                    print(f"[SSE] 连接错误: {e}")
                    time.sleep(5)
            except Exception as e:
                if self.running:
                    print(f"[SSE] 监听错误: {e}")
                    import traceback
                    traceback.print_exc()
                    time.sleep(5)
