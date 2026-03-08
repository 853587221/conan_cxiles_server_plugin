#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
QQ群机器人模块
用于接收QQ群消息并回复
"""

import botpy
from botpy import logging
from typing import Optional, Dict, Any
from datetime import datetime
import threading
import asyncio
import time
import json
from pathlib import Path

LOG = logging.get_logger()

CONFIG = {
    "appid": "222",
    "token": "111",
    "command_prefix": "/",
    "allowed_groups": [],
    "commands": {
        "签到": "签到成功",
        "余额查询": "查询余额成功",
        "在线玩家": "在线玩家查询成功",
        "绑定": "绑定玩家成功",
        "资源分布图": "http://42.194.169.146:8090/%E8%B5%84%E6%BA%90%E5%88%86%E5%B8%83%E5%9B%BE.jpg"
    }
}

_bot_instance = None
_bot_thread = None


def _get_db_editor():
    from bin.edit_db import DatabaseEditor
    return DatabaseEditor()


def _get_user_db_manager(username):
    from bin.user_db_manager import UserDBManager
    return UserDBManager(username)


class QQGroupBot(botpy.Client):
    def __init__(self, config: Dict[str, Any], **kwargs):
        super().__init__(**kwargs)
        self.config = config
        self.command_prefix = config.get('command_prefix', '/')
        self.allowed_groups = set(config.get('allowed_groups', []))
        self.commands = config.get('commands', {})
    
    async def on_ready(self):
        LOG.info("QQ机器人已就绪")
    
    async def on_group_at_message_create(self, message):
        group_openid = message.group_openid
        author_member_openid = message.author.member_openid
        content = message.content.strip()
        
        LOG.info(f"收到群消息 - 群: {group_openid}, 成员: {author_member_openid}, 内容: {content}")
        
        if self.allowed_groups and group_openid not in self.allowed_groups:
            return
        
        command = self._parse_command(content)
        if not command:
            await self._reply_message(message, "请使用命令，输入 /help 查看帮助")
            return
        
        response = await self._handle_command(command, group_openid, author_member_openid)
        if response:
            await self._reply_message(message, response)
    
    async def on_c2c_message_create(self, message):
        author_openid = message.author.user_openid
        content = message.content.strip()
        
        LOG.info(f"收到私聊消息 - 用户: {author_openid}, 内容: {content}")
        
        command = self._parse_command(content)
        if not command:
            await self._reply_c2c_message(message, "请使用命令，输入 /help 查看帮助")
            return
        
        response = self._handle_command(command, None, author_openid)
        if response:
            await self._reply_c2c_message(message, response)
    
    def _parse_command(self, content: str) -> Optional[str]:
        content = content.strip()
        if content.startswith(self.command_prefix):
            return content[len(self.command_prefix):].strip()
        return None
    
    async def _handle_command(self, command: str, group_openid: str = None, member_openid: str = None) -> str:
        parts = command.split(maxsplit=1)
        cmd = parts[0].lower() if parts else ''
        args = parts[1].strip() if len(parts) > 1 else ''
        
        if cmd == 'help':
            return self._get_help_text()
        
        if cmd == 'hello':
            return "你好！我是机器人，很高兴为你服务！"
        
        if cmd == 'time':
            now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            return f"当前时间: {now}"
        
        if cmd == '绑定':
            return await self._handle_bind_command(group_openid, member_openid, args)
        
        if cmd == '签到':
            return await self._handle_signin_command(group_openid, member_openid)
        
        if cmd == '余额查询':
            return await self._handle_balance_command(group_openid, member_openid)
        
        if cmd == '在线玩家':
            return await self._handle_online_players_command(group_openid)
        
        if cmd in self.commands:
            return self.commands.get(cmd, '无描述')
        
        return f"未知命令: {cmd}，输入 /help 查看帮助"
    
    async def _handle_bind_command(self, group_openid: str, member_openid: str, char_name: str) -> str:
        if not group_openid or not member_openid or not char_name:
            return "绑定命令格式错误，请使用: /绑定 角色名"
        
        try:
            db_editor = _get_db_editor()
            
            qq_bot_result = db_editor.get_qq_bot_by_group(group_openid)
            if not qq_bot_result['success']:
                LOG.info(f"QQ群 {group_openid} 未在系统中绑定，跳过处理")
                return ""
            
            qq_bot_settings = qq_bot_result['settings']
            user_id = qq_bot_settings['user_id']
            
            username_result = db_editor.get_username_by_id(user_id)
            if not username_result['success']:
                return "系统错误：无法找到绑定用户"
            
            username = username_result['username']
            
            user_db = _get_user_db_manager(username)
            
            existing_player = user_db.get_player_by_qq_member(member_openid)
            if existing_player:
                fail_msg = qq_bot_settings.get('Binding_message_3', '您的QQ已绑定其他角色，请先解绑后再绑定新角色')
                fail_msg = self._replace_variables(fail_msg, {
                    '角色名': existing_player.get('char_name', '')
                })
                return fail_msg
            
            player = user_db.get_player_by_char_name(char_name)
            
            if not player:
                return f"未找到角色: {char_name}"
            
            if player.get('qq_member'):
                fail_msg = qq_bot_settings.get('Binding_message_2', '该角色已被其他QQ绑定')
                fail_msg = self._replace_variables(fail_msg, {
                    '角色名': char_name
                })
                return fail_msg
            
            result = user_db.update_player_fields(
                player_id=player['id'],
                qq_member=member_openid,
                qq_binding_time=time.time()
            )
            
            if result['success']:
                success_msg = qq_bot_settings.get('Binding_message_1', '绑定成功！')
                success_msg = self._replace_variables(success_msg, {
                    '角色名': char_name
                })
                return success_msg
            else:
                return f"绑定失败: {result.get('message', '未知错误')}"
                
        except Exception as e:
            LOG.error(f"处理绑定命令失败: {e}")
            return f"系统错误: {str(e)}"
    
    def _replace_variables(self, message: str, variables: Dict[str, str]) -> str:
        if not message:
            return message
        for key, value in variables.items():
            message = message.replace(f'@{key}', str(value) if value else '')
        return message
    
    def _weighted_random(self, min_val: int, max_val: int, weight: float) -> int:
        """加权随机算法，数值越大越难中
        
        Args:
            min_val: 最小值
            max_val: 最大值
            weight: 难度系数，越大越难中高金额
            
        Returns:
            随机结果
        """
        import random
        import math
        
        if min_val >= max_val:
            return min_val
        
        values = list(range(min_val, max_val + 1))
        weights = []
        
        for val in values:
            distance = val - min_val
            w = 1.0 / (1.0 + distance * weight)
            weights.append(w)
        
        total_weight = sum(weights)
        rand_val = random.uniform(0, total_weight)
        
        cumulative = 0
        for i, w in enumerate(weights):
            cumulative += w
            if rand_val <= cumulative:
                return values[i]
        
        return values[-1]
    
    def _calculate_consecutive_days(self, sign_records: list) -> int:
        """计算连续签到天数"""
        if not sign_records:
            return 0
        
        from datetime import datetime, timedelta
        
        try:
            records = sorted(sign_records, reverse=True)
            today = datetime.now().date()
            consecutive = 0
            
            for i, date_str in enumerate(records):
                record_date = datetime.strptime(date_str, '%Y-%m-%d').date()
                expected_date = today - timedelta(days=i)
                
                if record_date == expected_date:
                    consecutive += 1
                else:
                    break
            
            return consecutive
        except Exception:
            return len(sign_records)
    
    async def _handle_signin_command(self, group_openid: str, member_openid: str) -> str:
        if not group_openid or not member_openid:
            return "签到命令错误"
        
        try:
            import random
            db_editor = _get_db_editor()
            
            qq_bot_result = db_editor.get_qq_bot_by_group(group_openid)
            if not qq_bot_result['success']:
                LOG.info(f"QQ群 {group_openid} 未在系统中绑定，跳过处理")
                return ""
            
            qq_bot_settings = qq_bot_result['settings']
            user_id = qq_bot_settings['user_id']
            
            username_result = db_editor.get_username_by_id(user_id)
            if not username_result['success']:
                return "系统错误：无法找到绑定用户"
            
            username = username_result['username']
            
            user_db = _get_user_db_manager(username)
            
            player = user_db.get_player_by_qq_member(member_openid)
            if not player:
                not_bound_msg = qq_bot_settings.get('sign_message_3', '您还未绑定游戏角色，请先使用"绑定 角色名"进行绑定')
                return not_bound_msg
            
            last_sign_time = player.get('sign_time')
            reset_type = qq_bot_settings.get('sign_reset_type', 'daily')
            reset_hour = qq_bot_settings.get('sign_reset_hour', 0)
            interval_hours = qq_bot_settings.get('sign_interval_hours', 24)
            
            can_sign = True
            now = time.time()
            
            if last_sign_time and last_sign_time > 0:
                if reset_type == 'daily':
                    now_dt = datetime.now()
                    today_reset = now_dt.replace(hour=reset_hour, minute=0, second=0, microsecond=0)
                    if now_dt.hour < reset_hour:
                        from datetime import timedelta
                        today_reset = today_reset - timedelta(days=1)
                    
                    last_sign_dt = datetime.fromtimestamp(last_sign_time)
                    if last_sign_dt >= today_reset:
                        can_sign = False
                else:
                    elapsed_hours = (now - last_sign_time) / 3600
                    if elapsed_hours < interval_hours:
                        can_sign = False
            
            if not can_sign:
                already_msg = qq_bot_settings.get('sign_message_2', '您今天已经签到过了，明天再来吧！')
                already_msg = self._replace_variables(already_msg, {
                    '角色名': player.get('char_name', '')
                })
                return already_msg
            
            gold_type = qq_bot_settings.get('sign_gold_type', 'fixed')
            if gold_type == 'random':
                gold_min = int(qq_bot_settings.get('sign_gold_min', 1))
                gold_max = int(qq_bot_settings.get('sign_gold_max', 50))
                gold_weight = float(qq_bot_settings.get('sign_gold_weight', 1.0))
                reward_gold = self._weighted_random(gold_min, gold_max, gold_weight)
            else:
                reward_gold = int(qq_bot_settings.get('sign_gold_fixed', 100))
            
            current_gold = player.get('gold', 0) or 0
            new_gold = round(float(current_gold) + float(reward_gold), 1)
            
            sign_records = player.get('sign_records', '') or ''
            if sign_records:
                records = json.loads(sign_records)
            else:
                records = []
            today_str = datetime.now().strftime('%Y-%m-%d')
            if today_str not in records:
                records.append(today_str)
            sign_records_json = json.dumps(records)
            
            result = user_db.update_player_fields(
                player_id=player['id'],
                sign_time=now,
                gold=new_gold,
                sign_records=sign_records_json
            )
            
            if result['success']:
                consecutive_days = self._calculate_consecutive_days(records)
                success_msg = qq_bot_settings.get('sign_message_1', '签到成功！')
                success_msg = self._replace_variables(success_msg, {
                    '角色名': player.get('char_name', ''),
                    '金额': str(int(reward_gold)),
                    '连续天数': str(consecutive_days)
                })
                return success_msg
            else:
                return f"签到失败: {result.get('message', '未知错误')}"
                
        except Exception as e:
            LOG.error(f"处理签到命令失败: {e}")
            return f"系统错误: {str(e)}"
    
    async def _handle_balance_command(self, group_openid: str, member_openid: str) -> str:
        if not group_openid or not member_openid:
            return "余额查询命令错误"
        
        try:
            db_editor = _get_db_editor()
            
            qq_bot_result = db_editor.get_qq_bot_by_group(group_openid)
            if not qq_bot_result['success']:
                LOG.info(f"QQ群 {group_openid} 未在系统中绑定，跳过处理")
                return ""
            
            qq_bot_settings = qq_bot_result['settings']
            user_id = qq_bot_settings['user_id']
            
            username_result = db_editor.get_username_by_id(user_id)
            if not username_result['success']:
                return "系统错误：无法找到绑定用户"
            
            username = username_result['username']
            
            user_db = _get_user_db_manager(username)
            
            player = user_db.get_player_by_qq_member(member_openid)
            if not player:
                not_bound_msg = qq_bot_settings.get('balance_message_2', '您还未绑定游戏角色，请先使用"绑定 角色名"进行绑定')
                return not_bound_msg
            
            current_gold = player.get('gold', 0) or 0
            gold_display = round(float(current_gold), 1)
            
            success_msg = qq_bot_settings.get('balance_message_1', '您的当前余额为：@金额 金币')
            success_msg = self._replace_variables(success_msg, {
                '角色名': player.get('char_name', ''),
                '金额': str(gold_display)
            })
            return success_msg
                
        except Exception as e:
            LOG.error(f"处理余额查询命令失败: {e}")
            return f"系统错误: {str(e)}"
    
    async def _handle_online_players_command(self, group_openid: str) -> str:
        if not group_openid:
            return "在线玩家查询命令错误"
        
        try:
            from bin.rcon_manager import RconManager
            
            db_editor = _get_db_editor()
            
            qq_bot_result = db_editor.get_qq_bot_by_group(group_openid)
            if not qq_bot_result['success']:
                LOG.info(f"QQ群 {group_openid} 未在系统中绑定，跳过处理")
                return ""
            
            qq_bot_settings = qq_bot_result['settings']
            user_id = qq_bot_settings['user_id']
            
            username_result = db_editor.get_username_by_id(user_id)
            if not username_result['success']:
                return "系统错误：无法找到绑定用户"
            
            username = username_result['username']
            
            rcon_manager = RconManager()
            result = rcon_manager.send_command_with_saved_info(username, 'listplayers')
            
            if not result['success']:
                return f"获取在线玩家失败: {result.get('message', '未知错误')}"
            
            response = result.get('response', '')
            online_count, player_list = self._parse_list_players(response)
            
            header_msg = qq_bot_settings.get('online_players_message', '📊 当前在线玩家 (@在线人数 人)：')
            header_msg = self._replace_variables(header_msg, {
                '在线人数': str(online_count),
                '玩家列表': '\n' + player_list if player_list else '暂无'
            })
            
            return header_msg
                
        except Exception as e:
            LOG.error(f"处理在线玩家查询命令失败: {e}")
            return f"系统错误: {str(e)}"
    
    def _parse_list_players(self, response: str):
        """解析listplayers命令的响应，返回在线人数和玩家列表"""
        online_count = 0
        player_names = []
        
        if not response:
            return 0, ''
        
        lines = response.split('\n')
        
        for line in lines:
            line = line.strip()
            if not line:
                continue
            if line.startswith('Idx') or line.startswith('---') or line.startswith('There are') or line.startswith('No players') or line.startswith('Total'):
                continue
            
            parts = line.split('|')
            if len(parts) >= 2:
                char_name = parts[1].strip()
                if char_name:
                    player_names.append(char_name)
                    online_count += 1
        
        player_list = '\n'.join(player_names)
        return online_count, player_list
    
    def _get_help_text(self) -> str:
        help_lines = ["可用命令:"]
        for cmd, desc in self.commands.items():
            help_lines.append(f"  {self.command_prefix}{cmd} - {desc}")
        help_lines.append(f"  {self.command_prefix}help - 显示帮助信息")
        return "\n".join(help_lines)
    
    async def _reply_message(self, message, content: str):
        try:
            if content.startswith('http://') or content.startswith('https://'):
                await self._send_image_message(message, content)
            else:
                await self.api.post_group_message(
                    group_openid=message.group_openid,
                    msg_type=0,
                    content=content,
                    msg_id=message.id
                )
        except Exception as e:
            LOG.error(f"发送群消息失败: {e}")
    
    async def _send_image_message(self, message, image_url: str):
        """发送图片消息"""
        try:
            result = await self.api.post_group_file(
                group_openid=message.group_openid,
                file_type=1,
                url=image_url,
                srv_send_msg=False
            )
            
            if result and hasattr(result, 'file_info'):
                await self.api.post_group_message(
                    group_openid=message.group_openid,
                    msg_type=7,
                    media={'file_info': result.file_info},
                    msg_id=message.id
                )
            elif result and isinstance(result, dict) and 'file_info' in result:
                await self.api.post_group_message(
                    group_openid=message.group_openid,
                    msg_type=7,
                    media={'file_info': result['file_info']},
                    msg_id=message.id
                )
            else:
                await self.api.post_group_message(
                    group_openid=message.group_openid,
                    msg_type=0,
                    content=image_url,
                    msg_id=message.id
                )
        except Exception as e:
            LOG.error(f"发送图片消息失败: {e}")
            try:
                await self.api.post_group_message(
                    group_openid=message.group_openid,
                    msg_type=0,
                    content=image_url,
                    msg_id=message.id
                )
            except:
                pass
    
    async def _reply_c2c_message(self, message, content: str):
        try:
            await self.api.post_c2c_message(
                openid=message.author.user_openid,
                msg_type=0,
                content=content,
                msg_id=message.id
            )
        except Exception as e:
            LOG.error(f"发送私聊消息失败: {e}")


def start_qq_bot():
    global _bot_instance, _bot_thread
    
    appid = CONFIG.get('appid', '')
    secret = CONFIG.get('token', '')
    
    if not appid or not secret:
        LOG.error("请在CONFIG中填写 appid 和 token")
        return
    
    intents = botpy.Intents(public_messages=True)
    
    _bot_instance = QQGroupBot(config=CONFIG, intents=intents)
    
    def run_bot():
        loop = asyncio.new_event_loop()
        try:
            LOG.info("启动QQ群机器人...")
            LOG.info(f"命令前缀: {CONFIG.get('command_prefix', '/')}")
            _bot_instance.run(appid=appid, secret=secret)
        except Exception as e:
            LOG.error(f"QQ机器人启动失败: {e}")
    
    _bot_thread = threading.Thread(target=run_bot, daemon=True)
    _bot_thread.start()


def stop_qq_bot():
    global _bot_instance, _bot_thread
    if _bot_instance:
        LOG.info("停止QQ群机器人...")
        _bot_instance = None
    if _bot_thread and _bot_thread.is_alive():
        _bot_thread = None


if __name__ == '__main__':
    start_qq_bot()
