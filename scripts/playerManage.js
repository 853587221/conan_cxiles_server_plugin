function openPlayerManageModal() {
    const template = document.getElementById('playerManageModalTemplate');
    const modalClone = template.content.cloneNode(true);
    const modal = modalClone.querySelector('.modal');
    
    document.body.appendChild(modal);
    
    modal.onlinePlayers = [];
    
    executeListPlayersCommand(modal);
    
    const searchInput = modal.querySelector('#playerSearch');
    searchInput.addEventListener('input', function() {
        filterPlayers(modal, this.value);
    });
    
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', function() {
        modal.remove();
    });

    const escapeHandler = function(e) {
        if (e.key === 'Escape') {
            modal.remove();
            document.removeEventListener('keydown', escapeHandler);
        }
    };
    document.addEventListener('keydown', escapeHandler);
}

function executeListPlayersCommand(modal) {
    const rconModeToggle = document.getElementById('rconModeToggle');
    const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
    const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
    
    const fetchPromise = fetch(apiUrl, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            command: 'listplayers'
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success && data.response) {
            modal.onlinePlayers = parseListPlayersResponse(data.response);
        }
    })
    .catch(error => {
    });
    
    const timeoutPromise = new Promise((resolve) => {
        setTimeout(() => {
            resolve();
        }, 5000);
    });
    
    Promise.race([fetchPromise, timeoutPromise])
        .finally(() => {
            loadPlayersList(modal);
        });
}

function parseListPlayersResponse(response) {
    const onlinePlayers = [];
    
    if (!response) return onlinePlayers;
    
    const lines = response.split('\n');
    
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line || line.startsWith('Idx') || line.startsWith('---') || line.startsWith('There are') || line.startsWith('No players') || line.startsWith('Total')) continue;
        
        const parts = line.split('|').map(p => p.trim());
        if (parts.length >= 3) {
            const idx = parts[0] || '';
            const characterName = parts[1] || '';
            const playerName = parts[2] || '';
            const userId = parts[3] || '';
            
            onlinePlayers.push({
                idx: idx,
                player_name: playerName,
                char_name: characterName,
                steam_id: userId
            });
        }
    }
    
    return onlinePlayers;
}

function loadPlayersList(modal) {
    const playersList = modal.querySelector('#playersList');
    playersList.innerHTML = `
        <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
            加载中...
        </div>
    `;
    
    fetch('/api/players')
        .then(response => response.json())
        .then(data => {
            if (data.success && data.players && data.players.length > 0) {
                modal.allPlayers = data.players;
                modal.serverDate = data.server_date || new Date().toISOString().split('T')[0];
                renderPlayersList(modal, data.players);
            } else {
                playersList.innerHTML = `
                    <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
                        暂无玩家数据
                    </div>
                `;
            }
        })
        .catch(error => {
            console.error('加载玩家列表失败:', error);
            playersList.innerHTML = `
                <div style="color: #ef5350; font-style: italic; text-align: center; padding: 20px;">
                    加载失败，请刷新页面重试
                </div>
            `;
        });
}

function renderPlayersList(modal, players) {
    const playersList = modal.querySelector('#playersList');
    
    if (!players || players.length === 0) {
        playersList.innerHTML = `
            <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
                未找到匹配的玩家
            </div>
        `;
        return;
    }
    
    const sortedPlayers = [...players].sort((a, b) => {
        const aIsOnline = modal.onlinePlayers && modal.onlinePlayers.some(online => 
            (online.player_name && online.player_name === a.player_name) ||
            (online.char_name && online.char_name === a.char_name) ||
            (online.steam_id && online.steam_id === a.steam_id)
        );
        const bIsOnline = modal.onlinePlayers && modal.onlinePlayers.some(online => 
            (online.player_name && online.player_name === b.player_name) ||
            (online.char_name && online.char_name === b.char_name) ||
            (online.steam_id && online.steam_id === b.steam_id)
        );
        
        if (aIsOnline && !bIsOnline) return -1;
        if (!aIsOnline && bIsOnline) return 1;
        return 0;
    });
    
    let html = '<div style="border: 1px solid #6a1b9a; border-radius: 8px; padding: 15px; background: linear-gradient(145deg, #1a1a1a, #101010);">';
    
    sortedPlayers.forEach(player => {
        const isOnline = modal.onlinePlayers && modal.onlinePlayers.some(online => 
            (online.player_name && online.player_name === player.player_name) ||
            (online.char_name && online.char_name === player.char_name) ||
            (online.steam_id && online.steam_id === player.steam_id)
        );
        
        const onlineStatus = isOnline 
            ? `<span style="background: #4caf50; color: #fff; padding: 2px 8px; border-radius: 3px; font-size: 11px; margin-left: 8px;">在线</span>`
            : '';
        
        const vipExpiry = player.monthly_card_expiry || 0;
        const now = Date.now() / 1000;
        let vipStatus = '';
        let vipColor = '#b0bec5';
        
        if (vipExpiry > 0) {
            if (vipExpiry > now) {
                const remainingDays = Math.ceil((vipExpiry - now) / 86400);
                vipStatus = `👑 会员: 有效期剩余 ${remainingDays} 天`;
                vipColor = '#4caf50';
            } else {
                vipStatus = '👑 会员: 已过期';
                vipColor = '#f44336';
            }
        }
        
        const vipStatusHtml = vipStatus ? `<span style="color: ${vipColor}; font-size: 12px; margin-left: 8px;">${vipStatus}</span>` : '';
        
        let signRecords = [];
        try {
            if (player.sign_records) {
                signRecords = JSON.parse(player.sign_records);
            }
        } catch (e) {
            console.error('解析签到记录失败:', e, player.sign_records);
            signRecords = [];
        }
        
        const signTime = player.sign_time || 0;
        const lastSignDate = signTime > 0 ? new Date(signTime * 1000).toLocaleDateString('zh-CN') : '从未签到';
        const totalSignDays = signRecords.length;
        
        const today = modal.serverDate || new Date().toISOString().split('T')[0];
        const signedToday = signRecords.includes(today);
        const todaySignStatus = signedToday 
            ? '<span style="background: #4caf50; color: #fff; padding: 2px 6px; border-radius: 3px; font-size: 10px; margin-left: 5px;">今日已签到</span>'
            : '';
        
        html += `
            <div style="margin-bottom: 15px; padding: 15px; border: 1px solid #333; border-radius: 5px; background: #0a0a0a;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                    <div>
                        <strong style="color: #9c27b0; font-size: 16px;">${player.char_name || '未知'}</strong>${onlineStatus}${vipStatusHtml}${todaySignStatus}
                        <div style="color: #b0bec5; font-size: 12px; margin-top: 5px;">
                            玩家名: ${player.player_name || '未知'} | 等级: ${player.level || 1} | 复活点: ${player.spawn_point || '未设置'} | 部落: ${player.guild_name || '无部落'}
                        </div>
                        <div style="color: #b0bec5; font-size: 12px; margin-top: 3px;">
                            📅 签到: 累计 ${totalSignDays} 天 | 最后签到: ${lastSignDate}
                        </div>
                    </div>
                    <div style="text-align: right;">
                        <div style="color: #b0bec5; font-size: 11px;">ID: ${player.id}</div>
                    </div>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 10px;">
                    <div>
                        <label style="color: #b0bec5; font-size: 12px;">权限等级:</label>
                        <input type="number" 
                               id="permission_level_${player.id}" 
                               class="form-control" 
                               value="${player.permission_level || 0}" 
                               min="0" 
                               style="margin-top: 5px;">
                    </div>
                    <div>
                        <label style="color: #b0bec5; font-size: 12px;">金额:</label>
                        <input type="number" 
                               id="gold_${player.id}" 
                               class="form-control" 
                               value="${player.gold || 0}" 
                               min="0" 
                               step="0.01"
                               style="margin-top: 5px;">
                    </div>
                    <input type="hidden" 
                           id="monthly_card_expiry_${player.id}" 
                           value="${vipExpiry}">
                </div>
                <div style="margin-top: 10px; text-align: right; display: flex; gap: 10px; justify-content: flex-end; flex-wrap: wrap;">
                    ${isOnline ? `
                    <button type="button" 
                            class="btn btn-info" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #ff9800, #ffb74d);"
                            onclick="openSendNotificationModal(${player.id}, '${player.char_name || '未知'}', '${player.player_name || ''}')">
                        📢 发送通知
                    </button>
                    ` : ''}
                    <button type="button" 
                            class="btn btn-info" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #ff5722, #ff8a65);"
                            onclick="openVipModal(${player.id}, '${player.char_name || '未知'}', ${vipExpiry})">
                        👑 会员
                    </button>
                    <button type="button" 
                            class="btn btn-info" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #00bcd4, #4dd0e1);"
                            onclick="openPlayerInfoModal(${player.id}, '${player.char_name || '未知'}')">
                        📋 玩家信息
                    </button>
                    <button type="button" 
                            class="btn btn-info" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #2196f3, #42a5f5);"
                            onclick="openPlayerItemsModal(${player.id}, '${player.char_name || '未知'}')">
                        🎒 物品信息
                    </button>
                    <button type="button" 
                            class="btn btn-info" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #e91e63, #f06292);"
                            onclick="openPlayerThrallsModal(${player.id}, '${player.char_name || '未知'}')">
                        👤 奴隶信息
                    </button>
                    <button type="button" 
                            class="btn btn-warning" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #ff9800, #ffc107);"
                            onclick="resetPlayerSignin(${player.id}, '${player.char_name || '未知'}', this)">
                        🔄 重置签到
                    </button>
                    <button type="button" 
                            class="btn btn-primary" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #4caf50, #66bb6a);"
                            onclick="updatePlayer(${player.id}, this)">
                        💾 保存
                    </button>
                    <button type="button" 
                            class="btn btn-danger" 
                            style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #f44336, #d32f2f);"
                            onclick="deletePlayer(${player.id}, '${player.char_name || '未知'}', this)">
                        🗑️ 删除
                    </button>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    playersList.innerHTML = html;
}

function filterPlayers(modal, searchTerm) {
    if (!modal.allPlayers) return;
    
    const term = searchTerm.toLowerCase().trim();
    
    if (!term) {
        renderPlayersList(modal, modal.allPlayers);
        return;
    }
    
    const filteredPlayers = modal.allPlayers.filter(player => {
        return (player.char_name && player.char_name.toLowerCase().includes(term)) ||
               (player.player_name && player.player_name.toLowerCase().includes(term)) ||
               (player.user_id && player.user_id.toLowerCase().includes(term)) ||
               String(player.id).includes(term);
    });
    
    renderPlayersList(modal, filteredPlayers);
}

function updatePlayer(playerId, buttonElement) {
    const permissionLevelInput = document.getElementById(`permission_level_${playerId}`);
    const goldInput = document.getElementById(`gold_${playerId}`);
    
    const permissionLevel = parseInt(permissionLevelInput.value) || 0;
    const gold = parseFloat(goldInput.value) || 0;
    
    const originalButtonText = buttonElement.textContent;
    buttonElement.textContent = '保存中...';
    buttonElement.disabled = true;
    
    fetch('/api/players/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            id: playerId,
            permission_level: permissionLevel,
            gold: gold
        })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            alert('更新成功: ' + result.message);
        } else {
            alert('更新失败: ' + result.message);
        }
    })
    .catch(error => {
        console.error('更新玩家信息失败:', error);
        alert('更新失败，请检查网络连接');
    })
    .finally(() => {
        buttonElement.textContent = originalButtonText;
        buttonElement.disabled = false;
    });
}

function deletePlayer(playerId, playerName, buttonElement) {
    if (!confirm(`确定要删除玩家 "${playerName}" 吗？\n\n此操作将删除该玩家的记录信息。\n\n删除后玩家重新登录时会重新同步数据！`)) {
        return;
    }
    
    const originalButtonText = buttonElement.textContent;
    buttonElement.textContent = '删除中...';
    buttonElement.disabled = true;
    
    fetch('/api/players/delete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            id: playerId
        })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            alert('删除成功: ' + result.message);
            
            const modal = buttonElement.closest('.modal');
            if (modal) {
                loadPlayersList(modal);
            }
        } else {
            alert('删除失败: ' + result.message);
        }
    })
    .catch(error => {
        console.error('删除玩家失败:', error);
        alert('删除失败，请检查网络连接');
    })
    .finally(() => {
        buttonElement.textContent = originalButtonText;
        buttonElement.disabled = false;
    });
}

function resetPlayerSignin(playerId, playerName, buttonElement) {
    if (!confirm(`确定要重置玩家 "${playerName}" 的签到记录吗？\n这将清除签到时间和签到日期记录。`)) {
        return;
    }
    
    const originalButtonText = buttonElement.textContent;
    buttonElement.textContent = '重置中...';
    buttonElement.disabled = true;
    
    fetch('/api/players/reset-signin', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            id: playerId
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            alert('签到记录已重置');
            const modal = buttonElement.closest('.modal');
            if (modal && modal.allPlayers) {
                const player = modal.allPlayers.find(p => p.id === playerId);
                if (player) {
                    player.sign_time = 0;
                    player.sign_records = '[]';
                }
                renderPlayersList(modal, modal.allPlayers);
            }
        } else {
            alert('重置失败: ' + (data.message || '未知错误'));
            buttonElement.textContent = originalButtonText;
            buttonElement.disabled = false;
        }
    })
    .catch(error => {
        console.error('重置签到记录失败:', error);
        alert('重置失败，请重试');
        buttonElement.textContent = originalButtonText;
        buttonElement.disabled = false;
    });
}

function openSendNotificationModal(playerId, charName, playerName) {
    const template = document.getElementById('sendNotificationModalTemplate');
    const modal = template.content.cloneNode(true).querySelector('.modal');
    document.body.appendChild(modal);
    
    modal.querySelector('#notificationPlayerName').value = charName;
    modal.playerId = playerId;
    modal.charName = charName;
    modal.playerName = playerName;
    
    modal.style.display = 'flex';
    
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', () => {
        modal.remove();
    });
    
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.remove();
        }
    });
    
    const escapeHandler = (e) => {
        if (e.key === 'Escape') {
            modal.remove();
            document.removeEventListener('keydown', escapeHandler);
        }
    };
    document.addEventListener('keydown', escapeHandler);
    
    const sendBtn = modal.querySelector('#sendNotificationBtn');
    sendBtn.addEventListener('click', () => {
        const message = modal.querySelector('#notificationMessage').value.trim();
        
        if (!message) {
            alert('请输入要发送的消息内容');
            return;
        }
        
        const targetName = (playerName && charName.includes(' ')) ? playerName : charName;
        
        const command = `con 0 playermessage "${targetName}" "${message}"`;
        
        const rconModeToggle = document.getElementById('rconModeToggle');
        const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
        
        const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
        
        fetch(apiUrl, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                command: command
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('通知发送成功！');
                modal.remove();
            } else {
                alert('通知发送失败: ' + data.message);
            }
        })
        .catch(error => {
            console.error('发送通知失败:', error);
            alert('发送通知失败，请检查网络连接');
        });
    });
}

window.openPlayerManageModal = openPlayerManageModal;
window.updatePlayer = updatePlayer;
window.deletePlayer = deletePlayer;
window.openSendNotificationModal = openSendNotificationModal;
window.openVipModal = openVipModal;
window.setVip = setVip;

function openVipModal(playerId, charName, currentExpiry) {
    const now = Date.now() / 1000;
    const isExpired = currentExpiry > 0 && currentExpiry <= now;
    const isActive = currentExpiry > now;
    
    let statusText = '未开通';
    let statusColor = '#b0bec5';
    
    if (isActive) {
        const remainingDays = Math.ceil((currentExpiry - now) / 86400);
        statusText = `有效（剩余 ${remainingDays} 天）`;
        statusColor = '#4caf50';
    } else if (isExpired) {
        statusText = '已过期';
        statusColor = '#f44336';
    }
    
    const modalHtml = `
        <div class="modal" id="vipModal" style="display: flex; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); z-index: 10000; justify-content: center; align-items: center;">
            <div class="modal-content" style="max-width: 450px; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border: 2px solid #ff5722; border-radius: 10px; padding: 0;">
                <div class="modal-header" style="background: linear-gradient(90deg, #ff5722 0%, #ff8a65 100%); padding: 15px 20px; border-radius: 8px 8px 0 0; display: flex; justify-content: space-between; align-items: center;">
                    <h3 style="color: #fff; margin: 0;">👑 会员管理 - ${charName}</h3>
                    <button class="modal-close" style="color: #fff; font-size: 24px; background: none; border: none; cursor: pointer;">&times;</button>
                </div>
                <div class="modal-body" style="padding: 20px;">
                    <div style="margin-bottom: 20px; padding: 15px; background: rgba(0,0,0,0.3); border-radius: 8px;">
                        <div style="color: #b0bec5; font-size: 14px; margin-bottom: 10px;">当前状态：</div>
                        <div style="color: ${statusColor}; font-size: 16px; font-weight: bold;">${statusText}</div>
                    </div>
                    
                    <div class="form-group" style="margin-bottom: 15px;">
                        <label style="color: #b0bec5; font-size: 14px; display: block; margin-bottom: 8px;">设置有效时间：</label>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
                            <button type="button" class="btn vip-btn" data-days="7" style="padding: 12px; background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff; border: none; border-radius: 5px; cursor: pointer; font-size: 14px;">
                                7 天
                            </button>
                            <button type="button" class="btn vip-btn" data-days="30" style="padding: 12px; background: linear-gradient(45deg, #2196f3, #42a5f5); color: #fff; border: none; border-radius: 5px; cursor: pointer; font-size: 14px;">
                                30 天
                            </button>
                            <button type="button" class="btn vip-btn" data-days="90" style="padding: 12px; background: linear-gradient(45deg, #9c27b0, #ab47bc); color: #fff; border: none; border-radius: 5px; cursor: pointer; font-size: 14px;">
                                90 天
                            </button>
                            <button type="button" class="btn vip-btn" data-days="365" style="padding: 12px; background: linear-gradient(45deg, #ff9800, #ffb74d); color: #fff; border: none; border-radius: 5px; cursor: pointer; font-size: 14px;">
                                365 天
                            </button>
                        </div>
                    </div>
                    
                    <div class="form-group" style="margin-bottom: 15px;">
                        <label style="color: #b0bec5; font-size: 14px; display: block; margin-bottom: 8px;">自定义天数：</label>
                        <div style="display: flex; gap: 10px;">
                            <input type="number" id="customDays" class="form-control" placeholder="输入天数" min="1" max="3650" style="flex: 1; background: rgba(255,255,255,0.1); border: 1px solid #ff5722; color: #fff; padding: 10px; border-radius: 5px;">
                            <button type="button" id="applyCustomDays" class="btn" style="padding: 10px 20px; background: linear-gradient(45deg, #ff5722, #ff8a65); color: #fff; border: none; border-radius: 5px; cursor: pointer;">
                                应用
                            </button>
                        </div>
                    </div>
                    
                    <div class="form-group" style="margin-bottom: 15px;">
                        <label style="color: #b0bec5; font-size: 14px; display: block; margin-bottom: 8px;">或设置具体过期时间：</label>
                        <input type="datetime-local" id="expiryDatetime" class="form-control" style="width: 100%; background: rgba(255,255,255,0.1); border: 1px solid #ff5722; color: #fff; padding: 10px; border-radius: 5px;">
                    </div>
                    
                    <div style="display: flex; gap: 10px; margin-top: 20px;">
                        <button type="button" id="cancelVip" class="btn" style="flex: 1; padding: 12px; background: linear-gradient(45deg, #f44336, #d32f2f); color: #fff; border: none; border-radius: 5px; cursor: pointer;">
                            取消会员
                        </button>
                        <button type="button" id="closeVipModal" class="btn" style="flex: 1; padding: 12px; background: linear-gradient(45deg, #607d8b, #78909c); color: #fff; border: none; border-radius: 5px; cursor: pointer;">
                            关闭
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    const modal = document.getElementById('vipModal');
    const closeBtn = modal.querySelector('.modal-close');
    const closeBtn2 = document.getElementById('closeVipModal');
    const cancelBtn = document.getElementById('cancelVip');
    const customDaysBtn = document.getElementById('applyCustomDays');
    const expiryDatetimeInput = document.getElementById('expiryDatetime');
    
    if (currentExpiry > 0) {
        const date = new Date(currentExpiry * 1000);
        const localDatetime = date.toISOString().slice(0, 16);
        expiryDatetimeInput.value = localDatetime;
    }
    
    const closeModal = () => modal.remove();
    
    closeBtn.addEventListener('click', closeModal);
    closeBtn2.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });
    
    document.querySelectorAll('.vip-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const days = parseInt(btn.dataset.days);
            const expiryTimestamp = Math.floor(Date.now() / 1000) + (days * 86400);
            setVip(playerId, expiryTimestamp, modal);
        });
    });
    
    customDaysBtn.addEventListener('click', () => {
        const days = parseInt(document.getElementById('customDays').value);
        if (days && days > 0) {
            const expiryTimestamp = Math.floor(Date.now() / 1000) + (days * 86400);
            setVip(playerId, expiryTimestamp, modal);
        } else {
            alert('请输入有效的天数');
        }
    });
    
    expiryDatetimeInput.addEventListener('change', () => {
        const datetime = expiryDatetimeInput.value;
        if (datetime) {
            const expiryTimestamp = Math.floor(new Date(datetime).getTime() / 1000);
            setVip(playerId, expiryTimestamp, modal);
        }
    });
    
    cancelBtn.addEventListener('click', () => {
        if (confirm('确定要取消该玩家的会员吗？')) {
            setVip(playerId, 0, modal);
        }
    });
    
    const escapeHandler = (e) => {
        if (e.key === 'Escape') {
            closeModal();
            document.removeEventListener('keydown', escapeHandler);
        }
    };
    document.addEventListener('keydown', escapeHandler);
}

function setVip(playerId, expiryTimestamp, modal) {
    fetch('/api/players/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            id: playerId,
            monthly_card_expiry: expiryTimestamp
        })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            alert('会员设置成功！');
            modal.remove();
            const playerManageModal = document.querySelector('.modal:not(#vipModal)');
            if (playerManageModal) {
                loadPlayersList({ 
                    querySelector: (sel) => playerManageModal.querySelector(sel),
                    onlinePlayers: playerManageModal.onlinePlayers,
                    allPlayers: playerManageModal.allPlayers
                });
            }
        } else {
            alert('会员设置失败: ' + result.message);
        }
    })
    .catch(error => {
        console.error('设置会员失败:', error);
        alert('设置会员失败，请检查网络连接');
    });
}

function loadPlayersList(modal) {
    const playersList = modal.querySelector('#playersList');
    playersList.innerHTML = `
        <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
            加载中...
        </div>
    `;
    
    fetch('/api/players')
        .then(response => response.json())
        .then(data => {
            if (data.success && data.players && data.players.length > 0) {
                modal.allPlayers = data.players;
                renderPlayersList(modal, data.players);
            } else {
                playersList.innerHTML = `
                    <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
                        暂无玩家数据
                    </div>
                `;
            }
        })
        .catch(error => {
            console.error('加载玩家列表失败:', error);
            playersList.innerHTML = `
                <div style="color: #ef5350; font-style: italic; text-align: center; padding: 20px;">
                    加载失败，请刷新页面重试
                </div>
            `;
        });
}
