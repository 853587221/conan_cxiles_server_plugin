function openPlayerThrallsModal(playerId, playerName) {
    const template = document.getElementById('playerThrallsModalTemplate');
    const modal = template.content.cloneNode(true).querySelector('.modal');
    document.body.appendChild(modal);
    
    modal.querySelector('#playerThrallName').textContent = playerName;
    modal.playerId = playerId;
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
    
    loadPlayerThralls(modal, playerName);
    
    const thrallSearchInput = modal.querySelector('#thrallSearch');
    if (thrallSearchInput) {
        thrallSearchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase().trim();
            filterThralls(modal, searchTerm);
        });
    }
    
    const refreshBtn = modal.querySelector('#refreshThrallsBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            loadPlayerThralls(modal, playerName);
        });
    }
}

function loadPlayerThralls(modal, playerName) {
    const loadingIndicator = modal.querySelector('#thrallLoadingIndicator');
    if (loadingIndicator) {
        loadingIndicator.style.display = 'block';
    }
    
    fetch(`/api/inventory/${encodeURIComponent(playerName)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                let thralls = data.thralls || data.inventory?.thralls || [];
                if (thralls && typeof thralls === 'object' && !Array.isArray(thralls)) {
                    thralls = Object.values(thralls);
                }
                modal.thrallsData = thralls;
                renderThrallsList(modal, thralls);
            } else {
                console.error('获取奴隶数据失败:', data.message);
                showThrallsError(modal, data.message || '获取奴隶数据失败');
            }
        })
        .catch(error => {
            console.error('加载奴隶数据出错:', error);
            showThrallsError(modal, '加载奴隶数据失败，请确保桌面客户端正在运行');
        })
        .finally(() => {
            if (loadingIndicator) {
                loadingIndicator.style.display = 'none';
            }
        });
}

function renderThrallsList(modal, thralls) {
    const thrallsList = modal.querySelector('#thrallsList');
    
    if (!thralls || thralls.length === 0) {
        thrallsList.innerHTML = `
            <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 40px; grid-column: 1 / -1;">
                <div style="font-size: 48px; margin-bottom: 15px;">👤</div>
                <div>该玩家暂无奴隶</div>
            </div>
        `;
        return;
    }
    
    let html = '';
    
    thralls.forEach((thrall, index) => {
        const thrallName = thrall.thrall_name || formatThrallType(thrall.thrall_type);
        const level = thrall.level || 1;
        const health = thrall.health || 0;
        const food = thrall.stats?.food || 0;
        
        html += `
            <div class="thrall-card" data-thrall-index="${index}" style="background: rgba(0,0,0,0.3); border: 2px solid #e91e63; border-radius: 8px; padding: 15px; cursor: pointer; transition: all 0.3s;" 
                 onmouseover="this.style.borderColor='#f06292'; this.style.background='rgba(233, 30, 99, 0.2)';"
                 onmouseout="this.style.borderColor='#e91e63'; this.style.background='rgba(0,0,0,0.3)';"
                 onclick="openThrallDetailModal(${index})">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                    <div style="color: #e91e63; font-weight: bold; font-size: 14px; word-break: break-word;">
                        👤 ${thrallName}
                    </div>
                    <div style="color: #b0bec5; font-size: 11px;">
                        ID: ${thrall.thrall_id}
                    </div>
                </div>
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 12px;">
                    <div style="color: #4caf50;">
                        ⚔️ 等级: ${level}
                    </div>
                    <div style="color: #f44336;">
                        ❤️ 生命: ${health}
                    </div>
                    <div style="color: #ff9800;">
                        🍖 饱食: ${food}%
                    </div>
                    <div style="color: #2196f3;">
                        📍 坐标: ${thrall.position ? thrall.position.split(' ').slice(0, 2).map(n => parseFloat(n).toFixed(0)).join(', ') : '未知'}
                    </div>
                </div>
            </div>
        `;
    });
    
    thrallsList.innerHTML = html;
    
    modal.openThrallDetailModal = function(index) {
        const thrall = modal.thrallsData[index];
        if (thrall) {
            showThrallDetailModal(thrall);
        }
    };
    
    window.openThrallDetailModal = function(index) {
        const thrall = modal.thrallsData[index];
        if (thrall) {
            showThrallDetailModal(thrall);
        }
    };
}

function showThrallDetailModal(thrall) {
    const template = document.getElementById('thrallDetailModalTemplate');
    const modal = template.content.cloneNode(true).querySelector('.modal');
    document.body.appendChild(modal);
    
    const thrallName = thrall.thrall_name || formatThrallType(thrall.thrall_type);
    modal.querySelector('#thrallDetailName').textContent = thrallName;
    
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
    
    renderThrallDetail(modal, thrall);
}

function renderThrallDetail(modal, thrall) {
    const content = modal.querySelector('#thrallDetailContent');
    
    const stats = thrall.stats || {};
    const perks = thrall.perks || {};
    const inventory = thrall.inventory || {};
    const backpack = inventory.backpack || [];
    const equipment = inventory.equipment || {};
    const thrallName = thrall.thrall_name || formatThrallType(thrall.thrall_type);
    
    let html = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
            <div>
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                    <div style="color: #e91e63; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #e91e63; padding-bottom: 5px;">
                        📋 基本信息
                    </div>
                    <div style="color: #ff9800; font-size: 11px; margin-bottom: 10px; padding: 5px; background: rgba(255, 152, 0, 0.1); border-radius: 4px;">
                        ⚠️ 提示：暂仅支持英文昵称显示
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;">
                        <div><span style="color: #b0bec5;">奴隶ID:</span> <span style="color: #fff;">${thrall.thrall_id}</span></div>
                        <div><span style="color: #b0bec5;">奴隶名称:</span> <span style="color: #fff;">${thrallName}</span></div>
                        <div><span style="color: #b0bec5;">等级:</span> <span style="color: #4caf50;">${thrall.level || 1}</span></div>
                        <div><span style="color: #b0bec5;">生命值:</span> <span style="color: #f44336;">${thrall.health || 0}</span></div>
                        <div><span style="color: #b0bec5;">坐标:</span> <span style="color: #2196f3;">${thrall.position || '未知'}</span></div>
                        <div style="margin-top: 5px;">
                            <button id="teleportPlayerBtn" class="btn" style="background: linear-gradient(45deg, #9c27b0, #ba68c8); color: #fff; padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">
                                📍 传送指定玩家到该处
                            </button>
                        </div>
                        <div id="teleportSelectContainer" style="display: none; margin-top: 5px;">
                            <select id="teleportPlayerSelect" class="form-control" style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; color: #fff; font-size: 12px; padding: 5px; border-radius: 4px; width: 100%;">
                                <option value="">-- 选择在线玩家 --</option>
                            </select>
                        </div>
                        <div id="teleportResult" style="display: none; margin-top: 5px; padding: 8px; border-radius: 4px; font-size: 12px;"></div>
                    </div>
                </div>
                
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                    <div style="color: #e91e63; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #e91e63; padding-bottom: 5px;">
                        👤 归属信息
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;">
                        <div><span style="color: #b0bec5;">归属玩家:</span> <span style="color: #fff;">${thrall.owner_char_name || '未知'}</span></div>
                        <div><span style="color: #b0bec5;">所属部落:</span> <span style="color: #fff;">${thrall.owner_guild_name || '无部落'}</span></div>
                        <div><span style="color: #b0bec5;">部落ID:</span> <span style="color: #fff;">${thrall.owner_guild_id || '无'}</span></div>
                    </div>
                </div>
                
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                    <div style="color: #e91e63; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #e91e63; padding-bottom: 5px;">
                        📊 属性信息
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 13px;">
                        <div><span style="color: #b0bec5;">饱食度:</span> <span style="color: #ff9800;">${stats.food || 0}%</span></div>
                        <div><span style="color: #b0bec5;">力量:</span> <span style="color: #f44336;">${stats.strength || 0}</span></div>
                        <div><span style="color: #b0bec5;">灵活:</span> <span style="color: #4caf50;">${stats.agility || 0}</span></div>
                        <div><span style="color: #b0bec5;">活力:</span> <span style="color: #2196f3;">${stats.vitality || 0}</span></div>
                        <div><span style="color: #b0bec5;">毅力:</span> <span style="color: #9c27b0;">${stats.grit || 0}</span></div>
                    </div>
                </div>
                
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; border-radius: 8px; padding: 15px;">
                    <div style="color: #e91e63; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #e91e63; padding-bottom: 5px;">
                        🎒 背包物品 (${backpack.filter(i => i !== null).length})
                    </div>
                    <div style="display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; max-height: 200px; overflow-y: auto;">
                        ${renderThrallBackpack(backpack)}
                    </div>
                </div>
            </div>
            
            <div>
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                    <div style="color: #e91e63; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #e91e63; padding-bottom: 5px;">
                        ⭐ 特权信息
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;">
                        <div><span style="color: #b0bec5;">奴隶原名:</span> <span style="color: #fff;">${formatThrallType(thrall.thrall_type)}</span></div>
                        <div><span style="color: #b0bec5;">奴隶类型:</span> <span style="color: #fff;">${perks.perk_type || '未知'}</span></div>
                        <div><span style="color: #b0bec5;">特权1:</span> <span style="color: #ff9800;">${perks.perk_1 || '无'}</span></div>
                        <div><span style="color: #b0bec5;">特权2:</span> <span style="color: #ff9800;">${perks.perk_2 || '无'}</span></div>
                        <div><span style="color: #b0bec5;">特权3:</span> <span style="color: #ff9800;">${perks.perk_3 || '无'}</span></div>
                    </div>
                </div>
                
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; border-radius: 8px; padding: 15px;">
                    <div style="color: #e91e63; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #e91e63; padding-bottom: 5px;">
                        🛡️ 装备栏
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;">
                        ${renderThrallEquipment(equipment)}
                    </div>
                </div>
            </div>
        </div>
    `;
    
    content.innerHTML = html;
    
    const teleportBtn = modal.querySelector('#teleportPlayerBtn');
    const teleportSelectContainer = modal.querySelector('#teleportSelectContainer');
    const teleportSelect = modal.querySelector('#teleportPlayerSelect');
    const teleportResult = modal.querySelector('#teleportResult');
    
    if (teleportBtn) {
        teleportBtn.addEventListener('click', () => {
            teleportSelectContainer.style.display = 'block';
            teleportBtn.disabled = true;
            teleportBtn.textContent = '⏳ 获取在线玩家...';
            
            const rconModeToggle = document.getElementById('rconModeToggle');
            const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
            const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
            
            fetch(apiUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command: 'listplayers' })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success && data.response) {
                    const onlinePlayers = parseListPlayersForTeleport(data.response);
                    teleportSelect.innerHTML = '<option value="">-- 选择在线玩家 --</option>';
                    onlinePlayers.forEach(player => {
                        const option = document.createElement('option');
                        option.value = player.idx;
                        option.textContent = player.char_name;
                        option.dataset.idx = player.idx;
                        teleportSelect.appendChild(option);
                    });
                    teleportBtn.textContent = '📍 传送指定玩家到该处';
                    teleportBtn.disabled = false;
                } else {
                    teleportBtn.textContent = '❌ 获取失败';
                    teleportBtn.disabled = false;
                }
            })
            .catch(() => {
                teleportBtn.textContent = '❌ 获取失败';
                teleportBtn.disabled = false;
            });
        });
    }
    
    if (teleportSelect) {
        teleportSelect.addEventListener('change', (e) => {
            const selectedIdx = e.target.value;
            if (!selectedIdx) return;
            
            const position = thrall.position;
            if (!position) {
                showTeleportResult(teleportResult, false, '奴隶坐标未知');
                return;
            }
            
            const coords = position.split(' ');
            if (coords.length < 3) {
                showTeleportResult(teleportResult, false, '坐标格式错误');
                return;
            }
            
            const x = parseFloat(coords[0]);
            const y = parseFloat(coords[1]);
            const z = parseFloat(coords[2]);
            
            const teleportCmd = `con ${selectedIdx} TeleportPlayer ${x} ${y} ${z}`;
            
            teleportResult.style.display = 'block';
            teleportResult.style.background = 'rgba(33, 150, 243, 0.2)';
            teleportResult.style.color = '#2196f3';
            teleportResult.innerHTML = `⏳ 正在传送玩家...`;
            
            const rconModeToggle = document.getElementById('rconModeToggle');
            const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
            const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
            
            fetch(apiUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command: teleportCmd })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showTeleportResult(teleportResult, true, `传送成功！玩家已传送到 (${x.toFixed(0)}, ${y.toFixed(0)}, ${z.toFixed(0)})`);
                } else {
                    showTeleportResult(teleportResult, false, `传送失败: ${data.message || '未知错误'}`);
                }
            })
            .catch(() => {
                showTeleportResult(teleportResult, false, '传送失败: 网络错误');
            });
        });
    }
}

function renderThrallBackpack(backpack) {
    const validItems = backpack.filter(i => i !== null);
    const maxSlots = Math.max(5, validItems.length);
    
    let html = '';
    for (let i = 0; i < maxSlots; i++) {
        const item = backpack[i];
        if (item) {
            const iconPath = getItemIconPath ? getItemIconPath(item.item_id) : null;
            const itemName = getItemName ? getItemName(item.item_id) : null;
            
            if (iconPath) {
                html += `
                    <div class="thrall-backpack-slot" style="aspect-ratio: 1; background: rgba(15, 52, 96, 0.3); border: 2px solid #0f3460; border-radius: 8px; display: flex; align-items: center; justify-content: center; position: relative; cursor: pointer;" title="${itemName || '物品'} (ID: ${item.item_id})">
                        <img src="${iconPath}" alt="${itemName || '物品'}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                        <div style="display: none; width: 100%; height: 100%; align-items: center; justify-content: center; font-size: 20px;">📦</div>
                        <span style="position: absolute; bottom: 2px; right: 2px; background: rgba(0,0,0,0.8); color: #fff; padding: 1px 3px; border-radius: 3px; font-size: 9px;">${item.quantity || 1}</span>
                    </div>
                `;
            } else {
                html += `
                    <div class="thrall-backpack-slot" style="aspect-ratio: 1; background: rgba(15, 52, 96, 0.3); border: 2px solid #0f3460; border-radius: 8px; display: flex; align-items: center; justify-content: center; position: relative; cursor: pointer;" title="物品ID: ${item.item_id}">
                        <span style="font-size: 20px;">📦</span>
                        <span style="position: absolute; bottom: 2px; right: 2px; background: rgba(0,0,0,0.8); color: #fff; padding: 1px 3px; border-radius: 3px; font-size: 9px;">${item.quantity || 1}</span>
                    </div>
                `;
            }
        } else {
            html += `
                <div class="thrall-backpack-slot" style="aspect-ratio: 1; background: rgba(15, 52, 96, 0.3); border: 2px solid #0f3460; border-radius: 8px; display: flex; align-items: center; justify-content: center;">
                </div>
            `;
        }
    }
    return html;
}

function renderThrallEquipment(equipment) {
    const slots = [
        { key: 'head', name: '头盔', icon: '🪖' },
        { key: 'body', name: '胸甲', icon: '👕' },
        { key: 'hands', name: '手套', icon: '🧤' },
        { key: 'legs', name: '护腿', icon: '👖' },
        { key: 'feet', name: '靴子', icon: '👢' },
        { key: 'main_hand', name: '主手', icon: '⚔️' },
        { key: 'off_hand', name: '副手', icon: '🛡️' }
    ];
    
    let html = '';
    slots.forEach(slot => {
        const item = equipment[slot.key];
        if (item) {
            const templateId = item.item_id;
            const iconPath = getItemIconPath ? getItemIconPath(templateId) : null;
            const itemName = getItemName ? getItemName(templateId) : null;
            const displayName = itemName || `物品 #${templateId}`;
            
            let iconHtml = '';
            if (iconPath) {
                iconHtml = `<img src="${iconPath}" alt="${displayName}" style="width: 32px; height: 32px; object-fit: contain;" onerror="this.style.display='none'; this.nextElementSibling.style.display='inline';"><span style="display: none; font-size: 18px;">${slot.icon}</span>`;
            } else {
                iconHtml = `<span style="font-size: 18px;">${slot.icon}</span>`;
            }
            
            html += `
                <div style="display: flex; align-items: center; gap: 10px; padding: 8px; background: rgba(15, 52, 96, 0.3); border-radius: 4px; border: 1px solid #0f3460; min-height: 50px;">
                    ${iconHtml}
                    <div style="flex: 1;">
                        <div style="color: #e91e63; font-size: 12px; font-weight: bold;">${slot.name}</div>
                        <div style="color: #fff; font-size: 11px;">${displayName}</div>
                        <div style="color: #b0bec5; font-size: 10px;">模板ID: ${templateId}</div>
                    </div>
                </div>
            `;
        } else {
            html += `
                <div style="display: flex; align-items: center; gap: 10px; padding: 8px; background: rgba(15, 52, 96, 0.3); border-radius: 4px; border: 1px solid #0f3460; min-height: 50px;">
                    <div style="flex: 1;">
                        <div style="color: #b0bec5; font-size: 12px;">${slot.name}</div>
                    </div>
                </div>
            `;
        }
    });
    return html;
}

function formatThrallType(thrallType) {
    if (!thrallType) return '未知奴隶';
    
    return thrallType
        .replace(/([A-Z])/g, ' $1')
        .replace(/\s+/g, ' ')
        .trim();
}

function parseListPlayersForTeleport(response) {
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
            
            onlinePlayers.push({
                idx: idx,
                char_name: characterName
            });
        }
    }
    
    return onlinePlayers;
}

function showTeleportResult(element, success, message) {
    element.style.display = 'block';
    if (success) {
        element.style.background = 'rgba(76, 175, 80, 0.2)';
        element.style.color = '#4caf50';
        element.innerHTML = `✅ ${message}`;
    } else {
        element.style.background = 'rgba(244, 67, 54, 0.2)';
        element.style.color = '#f44336';
        element.innerHTML = `❌ ${message}`;
    }
}

function filterThralls(modal, searchTerm) {
    if (!modal.thrallsData) return;
    
    if (!searchTerm) {
        renderThrallsList(modal, modal.thrallsData);
        return;
    }
    
    const filteredThralls = modal.thrallsData.filter(thrall => {
        const thrallType = formatThrallType(thrall.thrall_type).toLowerCase();
        const thrallId = String(thrall.thrall_id);
        const thrallName = (thrall.thrall_name || '').toLowerCase();
        
        return thrallType.includes(searchTerm) || thrallId.includes(searchTerm) || thrallName.includes(searchTerm);
    });
    
    renderThrallsList(modal, filteredThralls);
}

function showThrallsError(modal, errorMessage) {
    const thrallsList = modal.querySelector('#thrallsList');
    thrallsList.innerHTML = `
        <div style="color: #ef5350; text-align: center; padding: 40px; grid-column: 1 / -1;">
            <div style="font-size: 48px; margin-bottom: 15px;">❌</div>
            <div style="font-size: 14px;">${errorMessage}</div>
        </div>
    `;
}

window.openPlayerThrallsModal = openPlayerThrallsModal;
