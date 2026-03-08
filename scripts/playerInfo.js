function openPlayerInfoModal(playerId, playerName) {
    const template = document.getElementById('playerInfoModalTemplate');
    const modal = template.content.cloneNode(true).querySelector('.modal');
    document.body.appendChild(modal);
    
    modal.querySelector('#playerInfoName').textContent = playerName;
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
    
    loadPlayerInfo(modal, playerName);
    
    const refreshBtn = modal.querySelector('#refreshPlayerInfoBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            loadPlayerInfo(modal, playerName);
        });
    }
}

function loadPlayerInfo(modal, playerName) {
    const loadingIndicator = modal.querySelector('#playerInfoLoadingIndicator');
    if (loadingIndicator) {
        loadingIndicator.style.display = 'block';
    }
    
    const maxRetries = 2;
    let retryCount = 0;
    
    function makeRequest() {
        fetch(`/api/inventory/${encodeURIComponent(playerName)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                if (loadingIndicator) {
                    loadingIndicator.style.display = 'none';
                }
                if (data.success) {
                    const playerInfo = data.player_info || data.inventory?.player_info;
                    const inventory = data.inventory || {};
                    let thralls = data.thralls || [];
                    if (thralls && typeof thralls === 'object' && !Array.isArray(thralls)) {
                        thralls = Object.values(thralls);
                    }
                    
                    renderPlayerInfo(modal, playerInfo, inventory, thralls);
                } else {
                    console.error('获取玩家数据失败:', data.message);
                    showPlayerInfoError(modal, data.message || '获取玩家数据失败');
                }
            })
            .catch(error => {
                console.error('加载玩家数据出错:', error);
                retryCount++;
                if (retryCount < maxRetries) {
                    console.log(`重试请求 (${retryCount}/${maxRetries})...`);
                    setTimeout(makeRequest, 1000);
                } else {
                    if (loadingIndicator) {
                        loadingIndicator.style.display = 'none';
                    }
                    showPlayerInfoError(modal, '加载玩家数据失败，请确保桌面客户端正在运行');
                }
            });
    }
    
    makeRequest();
}

function renderPlayerInfo(modal, playerInfo, inventory, thralls) {
    const content = modal.querySelector('#playerInfoContent');
    
    if (!playerInfo) {
        content.innerHTML = `
            <div style="color: #b0bec5; text-align: center; padding: 40px;">
                <div style="font-size: 48px; margin-bottom: 15px;">❌</div>
                <div>未找到玩家信息</div>
            </div>
        `;
        return;
    }
    
    const stats = playerInfo.stats || {};
    
    let html = `
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
            <div>
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #00bcd4; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                    <div style="color: #00bcd4; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #00bcd4; padding-bottom: 5px;">
                        📋 基本信息
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;">
                        <div><span style="color: #b0bec5;">玩家ID:</span> <span style="color: #fff;">${playerInfo.player_id || '未知'}</span></div>
                        <div><span style="color: #b0bec5;">角色名:</span> <span style="color: #fff;">${playerInfo.char_name || '未知'}</span></div>
                        <div><span style="color: #b0bec5;">等级:</span> <span style="color: #4caf50;">${playerInfo.level || 1}</span></div>
                        <div><span style="color: #b0bec5;">生命值:</span> <span style="color: #f44336;">${playerInfo.health || 0}</span></div>
                        <div><span style="color: #b0bec5;">饱食度:</span> <span style="color: #ff9800;">${playerInfo.food || 0}%</span></div>
                        <div><span style="color: #b0bec5;">上一次存在坐标:</span> <span style="color: #2196f3;">${playerInfo.position || '未知'}</span></div>
                        <div style="margin-top: 5px;">
                            <button id="teleportPlayerBtn" class="btn" style="background: linear-gradient(45deg, #9c27b0, #ba68c8); color: #fff; padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">
                                📍 传送指定玩家到该处
                            </button>
                        </div>
                        <div id="teleportSelectContainer" style="display: none; margin-top: 5px;">
                            <select id="teleportPlayerSelect" class="form-control" style="background: rgba(0,0,0,0.3); border: 1px solid #00bcd4; color: #fff; font-size: 12px; padding: 5px; border-radius: 4px; width: 100%;">
                                <option value="">-- 选择在线玩家 --</option>
                            </select>
                        </div>
                        <div id="teleportResult" style="display: none; margin-top: 5px; padding: 8px; border-radius: 4px; font-size: 12px;"></div>
                        <div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid rgba(0, 188, 212, 0.3);"><span style="color: #b0bec5;">复活点坐标:</span> <span style="color: #e91e63;">${playerInfo.spawn_point || '未设置'}</span></div>
                        <div style="margin-top: 5px;">
                            <button id="teleportToSpawnBtn" class="btn" style="background: linear-gradient(45deg, #e91e63, #f06292); color: #fff; padding: 6px 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 12px;">
                                📍 传送指定玩家到复活点
                            </button>
                        </div>
                        <div id="teleportToSpawnSelectContainer" style="display: none; margin-top: 5px;">
                            <select id="teleportToSpawnPlayerSelect" class="form-control" style="background: rgba(0,0,0,0.3); border: 1px solid #e91e63; color: #fff; font-size: 12px; padding: 5px; border-radius: 4px; width: 100%;">
                                <option value="">-- 选择在线玩家 --</option>
                            </select>
                        </div>
                        <div id="teleportToSpawnResult" style="display: none; margin-top: 5px; padding: 8px; border-radius: 4px; font-size: 12px;"></div>
                    </div>
                </div>
                
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #00bcd4; border-radius: 8px; padding: 15px;">
                    <div style="color: #00bcd4; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #00bcd4; padding-bottom: 5px;">
                        👥 部落信息
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr; gap: 8px; font-size: 13px;">
                        <div><span style="color: #b0bec5;">部落ID:</span> <span style="color: #fff;">${playerInfo.guild_id || '无'}</span></div>
                        <div><span style="color: #b0bec5;">部落名:</span> <span style="color: #fff;">${playerInfo.guild_name || '无部落'}</span></div>
                    </div>
                </div>
            </div>
            
            <div>
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #00bcd4; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                    <div style="color: #00bcd4; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #00bcd4; padding-bottom: 5px;">
                        🎒 背包概览
                    </div>
                    ${renderInventoryOverview(inventory)}
                </div>
                
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #00bcd4; border-radius: 8px; padding: 15px; margin-bottom: 15px;">
                    <div style="color: #00bcd4; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #00bcd4; padding-bottom: 5px;">
                        👤 奴隶列表 (${thralls.length})
                    </div>
                    ${renderThrallsOverview(thralls)}
                </div>
                
                <div style="background: rgba(0,0,0,0.3); border: 1px solid #00bcd4; border-radius: 8px; padding: 15px;">
                    <div style="color: #00bcd4; font-weight: bold; font-size: 14px; margin-bottom: 10px; border-bottom: 1px solid #00bcd4; padding-bottom: 5px;">
                        📊 属性信息
                    </div>
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 13px;">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><span style="color: #b0bec5;">力量:</span> <span style="color: #f44336;">${stats.strength || 0}</span></span>
                            <button class="btn-edit-stat" data-stat="AttributeMight" data-name="力量" data-value="${stats.strength || 0}" style="background: rgba(244, 67, 54, 0.3); border: 1px solid #f44336; color: #f44336; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 11px;">修改</button>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><span style="color: #b0bec5;">灵活:</span> <span style="color: #4caf50;">${stats.agility || 0}</span></span>
                            <button class="btn-edit-stat" data-stat="AttributeAthleticism" data-name="灵活" data-value="${stats.agility || 0}" style="background: rgba(76, 175, 80, 0.3); border: 1px solid #4caf50; color: #4caf50; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 11px;">修改</button>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><span style="color: #b0bec5;">活力:</span> <span style="color: #2196f3;">${stats.vitality || 0}</span></span>
                            <button class="btn-edit-stat" data-stat="AttributeHealth" data-name="活力" data-value="${stats.vitality || 0}" style="background: rgba(33, 150, 243, 0.3); border: 1px solid #2196f3; color: #2196f3; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 11px;">修改</button>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><span style="color: #b0bec5;">毅力:</span> <span style="color: #9c27b0;">${stats.grit || 0}</span></span>
                            <button class="btn-edit-stat" data-stat="Attributestamina" data-name="毅力" data-value="${stats.grit || 0}" style="background: rgba(156, 39, 176, 0.3); border: 1px solid #9c27b0; color: #9c27b0; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 11px;">修改</button>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><span style="color: #b0bec5;">权威:</span> <span style="color: #ff9800;">${stats.authority || 0}</span></span>
                            <button class="btn-edit-stat" data-stat="AttributeLeadership" data-name="权威" data-value="${stats.authority || 0}" style="background: rgba(255, 152, 0, 0.3); border: 1px solid #ff9800; color: #ff9800; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 11px;">修改</button>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span><span style="color: #b0bec5;">专长:</span> <span style="color: #00bcd4;">${stats.expertise || 0}</span></span>
                            <button class="btn-edit-stat" data-stat="AttributeEncumbrance" data-name="专长" data-value="${stats.expertise || 0}" style="background: rgba(0, 188, 212, 0.3); border: 1px solid #00bcd4; color: #00bcd4; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 11px;">修改</button>
                        </div>
                        <div style="grid-column: 1 / -1; border-top: 1px solid rgba(0, 188, 212, 0.3); padding-top: 8px; margin-top: 5px; display: flex; justify-content: space-between; align-items: center;">
                            <span><span style="color: #ffeb3b;">未分配属性点: </span><span style="color: #ffeb3b; font-weight: bold; font-size: 16px;">${playerInfo.attribute_points || stats.attribute_points || 0}</span></span>
                            <button id="addAttributePointsBtn" style="background: rgba(255, 235, 59, 0.3); border: 1px solid #ffeb3b; color: #ffeb3b; padding: 2px 8px; border-radius: 3px; cursor: pointer; font-size: 11px;">增加</button>
                        </div>
                        <div style="grid-column: 1 / -1; font-size: 11px; color: #ff9800; background: rgba(255, 152, 0, 0.1); padding: 6px 8px; border-radius: 4px; margin-top: 5px;">
                            ⚠️ 提示：添加未分配属性点后页面可能无法及时刷新出来，实际上游戏内已经秒到账
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    content.innerHTML = html;
    
    modal.playerName = playerInfo.char_name;
    
    const editButtons = content.querySelectorAll('.btn-edit-stat');
    editButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const statCode = btn.dataset.stat;
            const statName = btn.dataset.name;
            const currentValue = btn.dataset.value;
            openEditStatModal(modal, modal.playerName, statCode, statName, currentValue);
        });
    });
    
    const addAttributePointsBtn = content.querySelector('#addAttributePointsBtn');
    if (addAttributePointsBtn) {
        addAttributePointsBtn.addEventListener('click', () => {
            const currentPoints = playerInfo.attribute_points || stats.attribute_points || 0;
            openAddAttributePointsModal(modal, modal.playerName, currentPoints);
        });
    }
    
    const teleportBtn = content.querySelector('#teleportPlayerBtn');
    const teleportSelectContainer = content.querySelector('#teleportSelectContainer');
    const teleportSelect = content.querySelector('#teleportPlayerSelect');
    const teleportResult = content.querySelector('#teleportResult');
    
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
            
            const position = playerInfo.position;
            if (!position) {
                showTeleportResult(teleportResult, false, '玩家坐标未知');
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
    
    const teleportToSpawnBtn = content.querySelector('#teleportToSpawnBtn');
    const teleportToSpawnSelectContainer = content.querySelector('#teleportToSpawnSelectContainer');
    const teleportToSpawnSelect = content.querySelector('#teleportToSpawnPlayerSelect');
    const teleportToSpawnResult = content.querySelector('#teleportToSpawnResult');
    
    if (teleportToSpawnBtn) {
        teleportToSpawnBtn.addEventListener('click', () => {
            if (!playerInfo.spawn_point) {
                showTeleportResult(teleportToSpawnResult, false, '该玩家未设置复活点');
                teleportToSpawnResult.style.display = 'block';
                return;
            }
            
            teleportToSpawnSelectContainer.style.display = 'block';
            teleportToSpawnBtn.disabled = true;
            teleportToSpawnBtn.textContent = '⏳ 获取在线玩家...';
            
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
                    teleportToSpawnSelect.innerHTML = '<option value="">-- 选择在线玩家 --</option>';
                    onlinePlayers.forEach(player => {
                        const option = document.createElement('option');
                        option.value = player.idx;
                        option.textContent = player.char_name;
                        option.dataset.idx = player.idx;
                        teleportToSpawnSelect.appendChild(option);
                    });
                    teleportToSpawnBtn.textContent = '📍 传送指定玩家到复活点';
                    teleportToSpawnBtn.disabled = false;
                } else {
                    teleportToSpawnBtn.textContent = '❌ 获取失败';
                    teleportToSpawnBtn.disabled = false;
                }
            })
            .catch(() => {
                teleportToSpawnBtn.textContent = '❌ 获取失败';
                teleportToSpawnBtn.disabled = false;
            });
        });
    }
    
    if (teleportToSpawnSelect) {
        teleportToSpawnSelect.addEventListener('change', (e) => {
            const selectedIdx = e.target.value;
            if (!selectedIdx) return;
            
            const spawnPoint = playerInfo.spawn_point;
            if (!spawnPoint) {
                showTeleportResult(teleportToSpawnResult, false, '复活点坐标未知');
                return;
            }
            
            const coords = spawnPoint.split(' ');
            if (coords.length < 3) {
                showTeleportResult(teleportToSpawnResult, false, '坐标格式错误');
                return;
            }
            
            const x = parseFloat(coords[0]);
            const y = parseFloat(coords[1]);
            const z = parseFloat(coords[2]);
            
            const teleportCmd = `con ${selectedIdx} TeleportPlayer ${x} ${y} ${z}`;
            
            teleportToSpawnResult.style.display = 'block';
            teleportToSpawnResult.style.background = 'rgba(233, 30, 99, 0.2)';
            teleportToSpawnResult.style.color = '#e91e63';
            teleportToSpawnResult.innerHTML = `⏳ 正在传送玩家...`;
            
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
                    showTeleportResult(teleportToSpawnResult, true, `传送成功！玩家已传送到复活点 (${x.toFixed(0)}, ${y.toFixed(0)}, ${z.toFixed(0)})`);
                } else {
                    showTeleportResult(teleportToSpawnResult, false, `传送失败: ${data.message || '未知错误'}`);
                }
            })
            .catch(() => {
                showTeleportResult(teleportToSpawnResult, false, '传送失败: 网络错误');
            });
        });
    }
}

function renderInventoryOverview(inventory) {
    let backpack = inventory.backpack || [];
    let equipment = inventory.equipment || [];
    let quickbar = inventory.quickbar || [];
    
    if (backpack && typeof backpack === 'object' && !Array.isArray(backpack)) {
        backpack = Object.values(backpack);
    }
    if (equipment && typeof equipment === 'object' && !Array.isArray(equipment)) {
        equipment = Object.values(equipment);
    }
    if (quickbar && typeof quickbar === 'object' && !Array.isArray(quickbar)) {
        quickbar = Object.values(quickbar);
    }
    
    const backpackCount = backpack.filter(i => i && i.template_id > 0).length;
    const equipmentCount = Math.max(0, equipment.filter(i => i && i.template_id > 0).length - 2);
    const quickbarCount = quickbar.filter(i => i && i.template_id > 0).length;
    
    return `
        <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; font-size: 12px;">
            <div style="text-align: center; padding: 10px; background: rgba(15, 52, 96, 0.3); border-radius: 4px;">
                <div style="color: #b0bec5;">背包</div>
                <div style="color: #fff; font-size: 18px; font-weight: bold;">${backpackCount}</div>
            </div>
            <div style="text-align: center; padding: 10px; background: rgba(15, 52, 96, 0.3); border-radius: 4px;">
                <div style="color: #b0bec5;">装备</div>
                <div style="color: #fff; font-size: 18px; font-weight: bold;">${equipmentCount}</div>
            </div>
            <div style="text-align: center; padding: 10px; background: rgba(15, 52, 96, 0.3); border-radius: 4px;">
                <div style="color: #b0bec5;">快捷栏</div>
                <div style="color: #fff; font-size: 18px; font-weight: bold;">${quickbarCount}</div>
            </div>
        </div>
    `;
}

function renderThrallsOverview(thralls) {
    if (!thralls || thralls.length === 0) {
        return `
            <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
                暂无奴隶
            </div>
        `;
    }
    
    let html = '<div style="display: grid; grid-template-columns: 1fr; gap: 8px; max-height: 200px; overflow-y: auto;">';
    
    thralls.forEach(thrall => {
        const thrallName = thrall.thrall_name || thrall.thrall_type || '未知奴隶';
        const level = thrall.level || 1;
        const health = thrall.health || 0;
        
        html += `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px; background: rgba(15, 52, 96, 0.3); border-radius: 4px; font-size: 12px;">
                <div style="color: #e91e63;">👤 ${thrallName}</div>
                <div style="color: #b0bec5;">
                    Lv.${level} | ❤️${health}
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    return html;
}

function showPlayerInfoError(modal, errorMessage) {
    const content = modal.querySelector('#playerInfoContent');
    content.innerHTML = `
        <div style="color: #ef5350; text-align: center; padding: 40px;">
            <div style="font-size: 48px; margin-bottom: 15px;">❌</div>
            <div style="font-size: 14px;">${errorMessage}</div>
        </div>
    `;
}

function openEditStatModal(parentModal, playerName, statCode, statName, currentValue) {
    const modalHtml = `
        <div class="modal" style="display: flex; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); z-index: 10000; justify-content: center; align-items: center;">
            <div class="modal-content" style="max-width: 400px; width: 90%; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border: 2px solid #00bcd4; border-radius: 8px;">
                <div class="modal-header" style="background: linear-gradient(90deg, #00bcd4 0%, #26c6da 100%); padding: 15px; border-radius: 6px 6px 0 0;">
                    <h3 style="color: #fff; margin: 0; font-size: 16px;">✏️ 修改${statName}属性</h3>
                </div>
                <div class="modal-body" style="padding: 20px;">
                    <div style="margin-bottom: 15px;">
                        <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">玩家: <span style="color: #fff;">${playerName}</span></div>
                        <div style="color: #b0bec5; font-size: 12px; margin-bottom: 10px;">当前值: <span style="color: #00bcd4;">${currentValue}</span></div>
                    </div>
                    <div class="form-group" style="margin-bottom: 15px;">
                        <label style="color: #b0bec5; font-size: 12px; display: block; margin-bottom: 5px;">新数值 (整数):</label>
                        <input type="number" id="newStatValue" class="form-control" value="${currentValue}" min="0" step="1" style="width: 100%; background: rgba(0,0,0,0.3); border: 1px solid #00bcd4; color: #fff; padding: 8px; border-radius: 4px;">
                    </div>
                    <div id="editStatResult" style="display: none; margin-bottom: 15px; padding: 10px; border-radius: 4px; font-size: 12px;"></div>
                    <div style="display: flex; gap: 10px; justify-content: flex-end;">
                        <button id="cancelEditStatBtn" class="btn" style="background: rgba(158, 158, 158, 0.3); border: 1px solid #9e9e9e; color: #9e9e9e; padding: 8px 16px; border-radius: 4px; cursor: pointer;">取消</button>
                        <button id="confirmEditStatBtn" class="btn" style="background: linear-gradient(45deg, #00bcd4, #4dd0e1); color: #fff; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer;">确定修改</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = modalHtml;
    const modal = tempDiv.firstElementChild;
    document.body.appendChild(modal);
    
    const closeBtn = modal.querySelector('#cancelEditStatBtn');
    const confirmBtn = modal.querySelector('#confirmEditStatBtn');
    const resultDiv = modal.querySelector('#editStatResult');
    const valueInput = modal.querySelector('#newStatValue');
    
    closeBtn.addEventListener('click', () => modal.remove());
    
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.remove();
    });
    
    confirmBtn.addEventListener('click', () => {
        const newValue = parseInt(valueInput.value);
        if (isNaN(newValue) || newValue < 0) {
            showEditStatResult(resultDiv, false, '请输入有效的正整数');
            return;
        }
        
        confirmBtn.disabled = true;
        confirmBtn.textContent = '处理中...';
        
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
            if (!data.success || !data.response) {
                showEditStatResult(resultDiv, false, '获取在线玩家列表失败');
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定修改';
                return;
            }
            
            const players = parseListPlayersForEdit(data.response);
            const player = players.find(p => p.char_name === playerName);
            
            if (!player) {
                showEditStatResult(resultDiv, false, `玩家 ${playerName} 不在线或未找到`);
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定修改';
                return;
            }
            
            const command = `con ${player.idx} setstat ${statCode} ${newValue}`;
            
            fetch(apiUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command: command })
            })
            .then(response => response.json())
            .then(result => {
                if (result.success) {
                    showEditStatResult(resultDiv, true, `成功！${statName}已修改为 ${newValue}`);
                    setTimeout(() => {
                        modal.remove();
                        if (parentModal) {
                            const refreshBtn = parentModal.querySelector('#refreshPlayerInfoBtn');
                            if (refreshBtn) {
                                refreshBtn.click();
                            }
                        }
                    }, 5000);
                } else {
                    showEditStatResult(resultDiv, false, `修改失败: ${result.message || '未知错误'}`);
                }
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定修改';
            })
            .catch(() => {
                showEditStatResult(resultDiv, false, '执行命令失败: 网络错误');
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定修改';
            });
        })
        .catch(() => {
            showEditStatResult(resultDiv, false, '获取玩家列表失败: 网络错误');
            confirmBtn.disabled = false;
            confirmBtn.textContent = '确定修改';
        });
    });
}

function parseListPlayersForEdit(response) {
    const onlinePlayers = [];
    if (!response) return onlinePlayers;
    
    const lines = response.split('\n');
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line || line.startsWith('Idx') || line.startsWith('---') || line.startsWith('There are') || line.startsWith('No players') || line.startsWith('Total')) continue;
        
        const parts = line.split('|').map(p => p.trim());
        if (parts.length >= 3) {
            onlinePlayers.push({
                idx: parts[0] || '',
                char_name: parts[1] || ''
            });
        }
    }
    return onlinePlayers;
}

function showEditStatResult(element, success, message) {
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

function openAddAttributePointsModal(parentModal, playerName, currentPoints) {
    const modalHtml = `
        <div class="modal" style="display: flex; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.7); z-index: 10000; justify-content: center; align-items: center;">
            <div class="modal-content" style="max-width: 400px; width: 90%; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border: 2px solid #ffeb3b; border-radius: 8px;">
                <div class="modal-header" style="background: linear-gradient(90deg, #ffeb3b 0%, #ffc107 100%); padding: 15px; border-radius: 6px 6px 0 0;">
                    <h3 style="color: #1a1a2e; margin: 0; font-size: 16px;">✨ 增加未分配属性点</h3>
                </div>
                <div class="modal-body" style="padding: 20px;">
                    <div style="margin-bottom: 15px;">
                        <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">玩家: <span style="color: #fff;">${playerName}</span></div>
                        <div style="color: #b0bec5; font-size: 12px; margin-bottom: 10px;">当前未分配属性点: <span style="color: #ffeb3b;">${currentPoints}</span></div>
                    </div>
                    <div class="form-group" style="margin-bottom: 15px;">
                        <label style="color: #b0bec5; font-size: 12px; display: block; margin-bottom: 5px;">增加数量 (整数):</label>
                        <input type="number" id="addPointsValue" class="form-control" value="1" min="1" step="1" style="width: 100%; background: rgba(0,0,0,0.3); border: 1px solid #ffeb3b; color: #fff; padding: 8px; border-radius: 4px;">
                    </div>
                    <div id="addPointsResult" style="display: none; margin-bottom: 15px; padding: 10px; border-radius: 4px; font-size: 12px;"></div>
                    <div style="display: flex; gap: 10px; justify-content: flex-end;">
                        <button id="cancelAddPointsBtn" class="btn" style="background: rgba(158, 158, 158, 0.3); border: 1px solid #9e9e9e; color: #9e9e9e; padding: 8px 16px; border-radius: 4px; cursor: pointer;">取消</button>
                        <button id="confirmAddPointsBtn" class="btn" style="background: linear-gradient(45deg, #ffeb3b, #ffc107); color: #1a1a2e; padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; font-weight: bold;">确定增加</button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = modalHtml;
    const modal = tempDiv.firstElementChild;
    document.body.appendChild(modal);
    
    const closeBtn = modal.querySelector('#cancelAddPointsBtn');
    const confirmBtn = modal.querySelector('#confirmAddPointsBtn');
    const resultDiv = modal.querySelector('#addPointsResult');
    const valueInput = modal.querySelector('#addPointsValue');
    
    closeBtn.addEventListener('click', () => modal.remove());
    
    modal.addEventListener('click', (e) => {
        if (e.target === modal) modal.remove();
    });
    
    confirmBtn.addEventListener('click', () => {
        const addValue = parseInt(valueInput.value);
        if (isNaN(addValue) || addValue <= 0) {
            showEditStatResult(resultDiv, false, '请输入有效的正整数');
            return;
        }
        
        confirmBtn.disabled = true;
        confirmBtn.textContent = '处理中...';
        
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
            if (!data.success || !data.response) {
                showEditStatResult(resultDiv, false, '获取在线玩家列表失败');
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定增加';
                return;
            }
            
            const players = parseListPlayersForEdit(data.response);
            const player = players.find(p => p.char_name === playerName);
            
            if (!player) {
                showEditStatResult(resultDiv, false, `玩家 ${playerName} 不在线或未找到`);
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定增加';
                return;
            }
            
            const command = `con ${player.idx} AddUndistributedAttributePoints ${addValue}`;
            
            fetch(apiUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command: command })
            })
            .then(response => response.json())
            .then(result => {
                if (result.success) {
                    showEditStatResult(resultDiv, true, `成功！已增加 ${addValue} 点未分配属性点`);
                    setTimeout(() => {
                        modal.remove();
                        if (parentModal) {
                            const refreshBtn = parentModal.querySelector('#refreshPlayerInfoBtn');
                            if (refreshBtn) {
                                refreshBtn.click();
                            }
                        }
                    }, 5000);
                } else {
                    showEditStatResult(resultDiv, false, `增加失败: ${result.message || '未知错误'}`);
                }
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定增加';
            })
            .catch(() => {
                showEditStatResult(resultDiv, false, '执行命令失败: 网络错误');
                confirmBtn.disabled = false;
                confirmBtn.textContent = '确定增加';
            });
        })
        .catch(() => {
            showEditStatResult(resultDiv, false, '获取玩家列表失败: 网络错误');
            confirmBtn.disabled = false;
            confirmBtn.textContent = '确定增加';
        });
    });
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
            onlinePlayers.push({
                idx: parts[0] || '',
                char_name: parts[1] || ''
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

window.openPlayerInfoModal = openPlayerInfoModal;
