function showItemContextMenu(x, y, item, positionId, invType, playerName) {
    const itemName = getItemName(item.template_id) || '未知物品';
    
    const itemConfig = itemConfigData[item.template_id];
    const itemCategory = itemConfig ? itemConfig.GUICategory : null;
    
    let menuItemsHtml = '';
    
    if (itemCategory === 'Weapon' || itemCategory === 'Tools') {
        menuItemsHtml += `
            <div class="context-menu-item" data-action="lightDamage" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; border-bottom: 1px solid #333; transition: all 0.2s;">
                修改轻击伤害
            </div>
            <div class="context-menu-item" data-action="heavyDamage" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; border-bottom: 1px solid #333; transition: all 0.2s;">
                修改重击伤害
            </div>
            <div class="context-menu-item" data-action="armorPen" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; border-bottom: 1px solid #333; transition: all 0.2s;">
                修改护甲穿透
            </div>
            <div class="context-menu-item" data-action="concussive" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; border-bottom: 1px solid #333; transition: all 0.2s;">
                修改眩晕
            </div>
        `;
    }
    
    if (itemCategory === 'Armor') {
        menuItemsHtml += `
            <div class="context-menu-item" data-action="armourValue" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; border-bottom: 1px solid #333; transition: all 0.2s;">
                修改护甲值
            </div>
        `;
    }
    
    if (itemCategory === 'Weapon' || itemCategory === 'Armor' || itemCategory === 'Tools') {
        menuItemsHtml += `
            <div class="context-menu-item" data-action="maxDurability" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; transition: all 0.2s;">
                修改最大耐久
            </div>
        `;
    }
    
    menuItemsHtml += `
        <div class="context-menu-item" data-action="maxStackSize" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; border-bottom: 1px solid #333; transition: all 0.2s;">
            允许堆叠数量
        </div>
    `;
    
    menuItemsHtml += `
        <div class="context-menu-item" data-action="perishRate" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; transition: all 0.2s;">
            修改腐朽
        </div>
    `;
    
    if (item.template_id !== 0) {
        menuItemsHtml += `
            <div class="context-menu-item" data-action="spawnItem" style="padding: 10px 15px; cursor: pointer; color: #b0bec5; font-size: 13px; transition: all 0.2s;">
                生成该物品
            </div>
        `;
    }
    
    if (!menuItemsHtml) {
        menuItemsHtml = '<div style="padding: 10px 15px; color: #b0bec5; font-size: 13px;">该物品类型不支持修改</div>';
    }
    
    const existingMenu = document.querySelector('.item-context-menu');
    if (existingMenu) {
        existingMenu.remove();
    }
    
    const menu = document.createElement('div');
    menu.className = 'item-context-menu';
    menu.style.cssText = `
        position: fixed;
        left: ${x}px;
        top: ${y}px;
        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
        border: 2px solid #0f3460;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
        z-index: 10000;
        min-width: 180px;
        max-height: 80vh;
        overflow-y: auto;
        overflow-x: hidden;
    `;
    
    menu.innerHTML = `
        <div style="padding: 10px 15px; background: rgba(233, 69, 96, 0.2); border-bottom: 1px solid #0f3460; color: #fff; font-size: 12px; font-weight: bold;">
            ${itemName}
        </div>
        ${menuItemsHtml}
    `;
    
    document.body.appendChild(menu);
    
    const menuRect = menu.getBoundingClientRect();
    const viewportHeight = window.innerHeight;
    if (menuRect.bottom > viewportHeight) {
        const newTop = Math.max(10, viewportHeight - menuRect.height - 10);
        menu.style.top = `${newTop}px`;
    }
    
    const viewportWidth = window.innerWidth;
    if (menuRect.right > viewportWidth) {
        const newLeft = Math.max(10, viewportWidth - menuRect.width - 10);
        menu.style.left = `${newLeft}px`;
    }
    
    const menuItems = menu.querySelectorAll('.context-menu-item');
    menuItems.forEach(menuItem => {
        menuItem.addEventListener('mouseenter', function() {
            this.style.background = 'rgba(233, 69, 96, 0.3)';
            this.style.color = '#fff';
        });
        menuItem.addEventListener('mouseleave', function() {
            this.style.background = 'transparent';
            this.style.color = '#b0bec5';
        });
    });
    
    menuItems.forEach(menuItem => {
        menuItem.addEventListener('click', function() {
            const action = this.getAttribute('data-action');
            menu.remove();
            openDamageSettingModal(item, positionId, invType, playerName, action);
        });
    });
    
    const closeMenuHandler = (e) => {
        if (!menu.contains(e.target)) {
            menu.remove();
            document.removeEventListener('click', closeMenuHandler);
        }
    };
    setTimeout(() => {
        document.addEventListener('click', closeMenuHandler);
    }, 0);
}

function openDamageSettingModal(item, positionId, invType, playerName, damageType) {
    const itemName = getItemName(item.template_id) || '未知物品';
    const isSpawnItem = damageType === 'spawnItem';
    const isPerishRate = damageType === 'perishRate';
    
    if (isSpawnItem) {
        openSpawnItemModal(item, playerName);
        return;
    }
    
    if (isPerishRate) {
        openPerishRateModal(item, positionId, invType, playerName);
        return;
    }
    
    const isArmorPen = damageType === 'armorPen';
    const isMaxDurability = damageType === 'maxDurability';
    const isArmourValue = damageType === 'armourValue';
    const isConcussive = damageType === 'concussive';
    const isMaxStackSize = damageType === 'maxStackSize';
    const settingTypeName = isArmourValue ? '护甲值' : (isMaxDurability ? '最大耐久' : (isArmorPen ? '护甲穿透' : (isConcussive ? '眩晕' : (isMaxStackSize ? '允许堆叠数量' : (damageType === 'lightDamage' ? '轻击伤害' : '重击伤害')))));
    const damageStatId = damageType === 'lightDamage' ? 6 : 7;
    
    const existingModal = document.querySelector('.damage-setting-modal');
    if (existingModal) {
        existingModal.remove();
    }
    
    const modal = document.createElement('div');
    modal.className = 'modal damage-setting-modal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
    `;
    
    const maxValue = isArmorPen ? 100 : '';
    const inputPlaceholder = isArmorPen ? '0-100' : '0';
    const valueLabel = isArmourValue ? '护甲数值' : (isMaxDurability ? '耐久数值' : (isArmorPen ? '护甲穿透百分比' : (isMaxStackSize ? '最大允许堆叠数量' : (isConcussive ? '眩晕数值' : '伤害数值'))));
    
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 400px; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border: 2px solid #0f3460; border-radius: 8px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);">
            <div class="modal-header" style="background: linear-gradient(90deg, #e94560 0%, #0f3460 100%); border-bottom: 2px solid #e94560; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center;">
                <h3 style="color: #fff; margin: 0; font-size: 16px;">设置${settingTypeName}</h3>
                <button class="modal-close" style="color: #fff; font-size: 24px; background: none; border: none; cursor: pointer;">&times;</button>
            </div>
            <div class="modal-body" style="padding: 20px;">
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">物品名称</div>
                    <div style="color: #e94560; font-size: 14px; font-weight: bold;">${itemName}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">玩家名称</div>
                    <div style="color: #fff; font-size: 14px;">${playerName}</div>
                </div>
                <div style="margin-bottom: 20px;">
                    <label for="damageValue" style="color: #b0bec5; font-size: 12px; display: block; margin-bottom: 5px;">${valueLabel}</label>
                    <input type="number" id="damageValue" class="form-control" value="0" min="0" max="${maxValue}" placeholder="${inputPlaceholder}" style="background: rgba(255, 255, 255, 0.1); border: 1px solid #0f3460; color: #fff; padding: 10px; border-radius: 4px; width: 100%; box-sizing: border-box;">
                    ${isArmorPen ? '<div style="color: #b0bec5; font-size: 11px; margin-top: 5px;">范围: 0-100 (百分比)</div>' : ''}
                </div>
                <div style="display: flex; gap: 10px;">
                    <button id="saveDamageBtn" class="btn" style="flex: 1; background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff; padding: 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">保存</button>
                    <button id="cancelDamageBtn" class="btn" style="flex: 1; background: linear-gradient(45deg, #ef5350, #f44336); color: #fff; padding: 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">取消</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', () => modal.remove());
    
    const cancelBtn = modal.querySelector('#cancelDamageBtn');
    cancelBtn.addEventListener('click', () => modal.remove());
    
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
    
    const saveBtn = modal.querySelector('#saveDamageBtn');
    saveBtn.addEventListener('click', () => {
        const damageValue = modal.querySelector('#damageValue').value;
        if (damageValue === '') {
            alert('请输入数值');
            return;
        }
        
        const numValue = parseFloat(damageValue);
        if (isArmorPen) {
            if (numValue < 0 || numValue > 100) {
                alert('护甲穿透范围应为 0-100');
                return;
            }
        } else {
            if (numValue < 0) {
                alert('数值不能为负数');
                return;
            }
        }
        
        saveBtn.disabled = true;
        saveBtn.textContent = '保存中...';
        
        saveDamageSetting(playerName, positionId, damageStatId, numValue, invType, isArmorPen, isMaxDurability, isArmourValue, isConcussive, isMaxStackSize, modal, saveBtn);
    });
}

function showLoadingModal() {
    const existingModal = document.querySelector('.loading-modal');
    if (existingModal) {
        existingModal.remove();
    }
    
    const modal = document.createElement('div');
    modal.className = 'modal loading-modal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
    `;
    
    modal.innerHTML = `
        <div style="text-align: center; color: #fff;">
            <div class="spinner" style="
                width: 50px;
                height: 50px;
                border: 4px solid rgba(255, 255, 255, 0.3);
                border-top: 4px solid #e94560;
                border-radius: 50%;
                animation: spin 1s linear infinite;
                margin: 0 auto 15px;
            "></div>
            <div style="font-size: 16px;">正在修改...</div>
        </div>
        <style>
            @keyframes spin {
                0% { transform: rotate(0deg); }
                100% { transform: rotate(360deg); }
            }
        </style>
    `;
    
    document.body.appendChild(modal);
    return modal;
}

function hideLoadingModal() {
    const modal = document.querySelector('.loading-modal');
    if (modal) {
        modal.remove();
    }
}

function openSpawnItemModal(item, playerName) {
    const itemName = getItemName(item.template_id) || '未知物品';
    const templateId = item.template_id;
    
    const existingModal = document.querySelector('.spawn-item-modal');
    if (existingModal) {
        existingModal.remove();
    }
    
    const modal = document.createElement('div');
    modal.className = 'modal spawn-item-modal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
    `;
    
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 400px; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border: 2px solid #0f3460; border-radius: 8px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);">
            <div class="modal-header" style="background: linear-gradient(90deg, #e94560 0%, #0f3460 100%); border-bottom: 2px solid #e94560; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center;">
                <h3 style="color: #fff; margin: 0; font-size: 16px;">生成物品</h3>
                <button class="modal-close" style="color: #fff; font-size: 24px; background: none; border: none; cursor: pointer;">&times;</button>
            </div>
            <div class="modal-body" style="padding: 20px;">
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">物品名称</div>
                    <div style="color: #e94560; font-size: 14px; font-weight: bold;">${itemName}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">模板ID</div>
                    <div style="color: #fff; font-size: 14px;">${templateId}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">玩家名称</div>
                    <div style="color: #fff; font-size: 14px;">${playerName}</div>
                </div>
                <div style="margin-bottom: 20px;">
                    <label for="spawnAmount" style="color: #b0bec5; font-size: 12px; display: block; margin-bottom: 5px;">生成数量</label>
                    <input type="number" id="spawnAmount" class="form-control" value="1" min="1" placeholder="1" style="background: rgba(255, 255, 255, 0.1); border: 1px solid #0f3460; color: #fff; padding: 10px; border-radius: 4px; width: 100%; box-sizing: border-box;">
                </div>
                <div style="display: flex; gap: 10px;">
                    <button id="spawnItemBtn" class="btn" style="flex: 1; background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff; padding: 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">生成</button>
                    <button id="cancelSpawnBtn" class="btn" style="flex: 1; background: linear-gradient(45deg, #ef5350, #f44336); color: #fff; padding: 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">取消</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', () => modal.remove());
    
    const cancelBtn = modal.querySelector('#cancelSpawnBtn');
    cancelBtn.addEventListener('click', () => modal.remove());
    
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
    
    const spawnBtn = modal.querySelector('#spawnItemBtn');
    spawnBtn.addEventListener('click', () => {
        const amountValue = modal.querySelector('#spawnAmount').value;
        if (amountValue === '' || parseInt(amountValue) < 1) {
            alert('请输入有效的数量（至少为1）');
            return;
        }
        
        const amount = parseInt(amountValue);
        spawnBtn.disabled = true;
        spawnBtn.textContent = '生成中...';
        
        spawnItemToPlayer(playerName, templateId, amount, modal, spawnBtn);
    });
}

function spawnItemToPlayer(playerName, templateId, amount, modal, spawnBtn) {
    const loadingModal = showLoadingModal();
    
    getPlayerIndexByName(playerName).then(playerIndex => {
        if (playerIndex === null) {
            hideLoadingModal();
            if (modal) modal.remove();
            alert('该玩家不在线，无法生成物品');
            return;
        }
        
        const command = `con ${playerIndex} spawnitem ${templateId} ${amount}`;
        
        const rconModeToggle = document.getElementById('rconModeToggle');
        const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
        const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
        
        fetch(apiUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command })
        })
        .then(response => response.json())
        .then(data => {
            hideLoadingModal();
            if (modal) modal.remove();
            if (data.success) {
                alert('物品生成成功！');
            } else {
                alert('物品生成失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            hideLoadingModal();
            if (modal) modal.remove();
            console.error('发送命令请求失败:', error);
            alert('发送命令请求失败: ' + error.message);
        });
    }).catch(error => {
        hideLoadingModal();
        if (modal) modal.remove();
        console.error('获取玩家索引失败:', error);
        alert('获取玩家索引失败: ' + error.message);
    });
}

function saveDamageSetting(playerName, positionId, damageStatId, damageValue, invType, isArmorPen, isMaxDurability, isArmourValue, isConcussive = false, isMaxStackSize = false, damageModal = null, saveBtn = null) {
    const loadingModal = showLoadingModal();
    
    getPlayerIndexByName(playerName).then(playerIndex => {
        if (playerIndex === null) {
            hideLoadingModal();
            if (damageModal) damageModal.remove();
            alert('该玩家不在线，无法修改物品属性');
            return;
        }
        
        if (isConcussive) {
            const command1 = `con ${playerIndex} SetInventoryItemStat ${positionId} DamageConcussiveLightOnHit ${damageValue} ${invType}`;
            const command2 = `con ${playerIndex} SetInventoryItemStat ${positionId} DamageConcussiveHeavyOnHit ${damageValue} ${invType}`;
            
            const rconModeToggle = document.getElementById('rconModeToggle');
            const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
            const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
            
            Promise.all([
                fetch(apiUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: command1 })
                }),
                fetch(apiUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: command2 })
                })
            ])
            .then(responses => Promise.all(responses.map(r => r.json())))
            .then(dataArray => {
                hideLoadingModal();
                if (damageModal) damageModal.remove();
                const allSuccess = dataArray.every(data => data.success);
                if (allSuccess) {
                    alert('修改成功！');
                } else {
                    const errorMsg = dataArray.find(data => !data.success)?.message || '未知错误';
                    alert('修改失败: ' + errorMsg);
                }
            })
            .catch(error => {
                hideLoadingModal();
                if (damageModal) damageModal.remove();
                console.error('发送命令请求失败:', error);
                alert('发送命令请求失败: ' + error.message);
            });
        } else {
            let command;
            if (isArmourValue) {
                command = `con ${playerIndex} SetInventoryItemStat ${positionId} ArmourValue ${damageValue} ${invType}`;
            } else if (isMaxDurability) {
                command = `con ${playerIndex} SetInventoryItemStat ${positionId} MaxDurability ${damageValue} ${invType}`;
            } else if (isMaxStackSize) {
                command = `con ${playerIndex} SetInventoryItemStat ${positionId} MaxStackSize ${damageValue} ${invType}`;
            } else if (isArmorPen) {
                const armorPenValue = damageValue / 100;
                command = `con ${playerIndex} SetInventoryItemStat ${positionId} ArmorPen ${armorPenValue} ${invType}`;
            } else {
                command = `con ${playerIndex} SetInventoryItemIntStat ${positionId} ${damageStatId} ${damageValue} ${invType}`;
            }
            sendRconCommand(command, damageModal);
        }
    }).catch(error => {
        hideLoadingModal();
        if (damageModal) damageModal.remove();
        console.error('获取玩家索引失败:', error);
        alert('获取玩家索引失败: ' + error.message);
    });
}

function getPlayerIndexByName(playerName) {
    const rconModeToggle = document.getElementById('rconModeToggle');
    const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
    const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
    
    return fetch(apiUrl, {
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
        if (!data.success) {
            throw new Error(data.message || '获取玩家列表失败');
        }
        
        const responseText = data.response;
        
        const lines = responseText.split('\n');
        
        for (const line of lines) {
            if (line.includes(playerName)) {
                const match = line.match(/^\s*(\d+)\s*\|/);
                if (match) {
                    return parseInt(match[1]);
                }
            }
        }
        
        return null;
    });
}

function sendRconCommand(command, damageModal = null) {
    const rconModeToggle = document.getElementById('rconModeToggle');
    const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
    const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
    
    return fetch(apiUrl, {
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
        hideLoadingModal();
        if (damageModal) damageModal.remove();
        if (data.success) {
            alert('修改成功！');
        } else {
            alert('修改失败: ' + data.message);
        }
    })
    .catch(error => {
        hideLoadingModal();
        if (damageModal) damageModal.remove();
        console.error('发送命令请求失败:', error);
        alert('发送命令请求失败: ' + error.message);
    });
}

function openPerishRateModal(item, positionId, invType, playerName) {
    const itemName = getItemName(item.template_id) || '未知物品';
    const itemConfig = itemConfigData[item.template_id];
    const itemCategory = itemConfig ? itemConfig.GUICategory : null;
    const supportedCategories = ['Weapon', 'Armor', 'Tools', 'Consumable'];
    
    if (!supportedCategories.includes(itemCategory)) {
        if (confirm('该物品不支持使用自定义腐朽时间，仅支持0秒。\n\n支持的物品类型：武器、护甲、工具、消耗品\n\n确定要修改为0秒吗？')) {
            savePerishRateDirect(playerName, positionId, invType, 9999);
        }
        return;
    }
    
    const maxDurability = itemConfig ? (itemConfig.MaxDurability || 100) : 100;
    
    const existingModal = document.querySelector('.perish-rate-modal');
    if (existingModal) {
        existingModal.remove();
    }
    
    const modal = document.createElement('div');
    modal.className = 'modal perish-rate-modal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
    `;
    
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 450px; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border: 2px solid #0f3460; border-radius: 8px; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);">
            <div class="modal-header" style="background: linear-gradient(90deg, #e94560 0%, #0f3460 100%); border-bottom: 2px solid #e94560; padding: 15px 20px; display: flex; justify-content: space-between; align-items: center;">
                <h3 style="color: #fff; margin: 0; font-size: 16px;">设置腐朽时间</h3>
                <button class="modal-close" style="color: #fff; font-size: 24px; background: none; border: none; cursor: pointer;">&times;</button>
            </div>
            <div class="modal-body" style="padding: 20px;">
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">物品名称</div>
                    <div style="color: #e94560; font-size: 14px; font-weight: bold;">${itemName}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">玩家名称</div>
                    <div style="color: #fff; font-size: 14px;">${playerName}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 5px;">物品最大耐久</div>
                    <div style="color: #fff; font-size: 14px;">${maxDurability}</div>
                </div>
                <div style="margin-bottom: 20px;">
                    <label style="color: #b0bec5; font-size: 12px; display: block; margin-bottom: 10px;">腐朽时间</label>
                    <div style="display: flex; gap: 10px; align-items: center;">
                        <div style="flex: 1;">
                            <input type="number" id="perishHours" class="form-control" value="0" min="0" placeholder="0" style="background: rgba(255, 255, 255, 0.1); border: 1px solid #0f3460; color: #fff; padding: 10px; border-radius: 4px; width: 100%; box-sizing: border-box; text-align: center;">
                            <div style="color: #b0bec5; font-size: 11px; margin-top: 5px; text-align: center;">小时</div>
                        </div>
                        <div style="flex: 1;">
                            <input type="number" id="perishMinutes" class="form-control" value="0" min="0" max="59" placeholder="0" style="background: rgba(255, 255, 255, 0.1); border: 1px solid #0f3460; color: #fff; padding: 10px; border-radius: 4px; width: 100%; box-sizing: border-box; text-align: center;">
                            <div style="color: #b0bec5; font-size: 11px; margin-top: 5px; text-align: center;">分钟</div>
                        </div>
                        <div style="flex: 1;">
                            <input type="number" id="perishSeconds" class="form-control" value="0" min="0" max="59" placeholder="0" style="background: rgba(255, 255, 255, 0.1); border: 1px solid #0f3460; color: #fff; padding: 10px; border-radius: 4px; width: 100%; box-sizing: border-box; text-align: center;">
                            <div style="color: #b0bec5; font-size: 11px; margin-top: 5px; text-align: center;">秒</div>
                        </div>
                    </div>
                    <div style="color: #b0bec5; font-size: 11px; margin-top: 10px;">
                        计算公式: PerishRate = 25 × ${maxDurability} ÷ 总秒数
                    </div>
                </div>
                <div style="display: flex; gap: 10px;">
                    <button id="savePerishRateBtn" class="btn" style="flex: 1; background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff; padding: 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">保存</button>
                    <button id="cancelPerishRateBtn" class="btn" style="flex: 1; background: linear-gradient(45deg, #ef5350, #f44336); color: #fff; padding: 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 14px;">取消</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', () => modal.remove());
    
    const cancelBtn = modal.querySelector('#cancelPerishRateBtn');
    cancelBtn.addEventListener('click', () => modal.remove());
    
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
    
    const saveBtn = modal.querySelector('#savePerishRateBtn');
    saveBtn.addEventListener('click', () => {
        const hours = parseInt(modal.querySelector('#perishHours').value) || 0;
        const minutes = parseInt(modal.querySelector('#perishMinutes').value) || 0;
        const seconds = parseInt(modal.querySelector('#perishSeconds').value) || 0;
        
        if (hours < 0 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
            alert('请输入有效的时间值（分钟和秒数为0-59）');
            return;
        }
        
        const totalSeconds = hours * 3600 + minutes * 60 + seconds;
        if (totalSeconds <= 0) {
            alert('腐朽时间必须大于0秒');
            return;
        }
        
        saveBtn.disabled = true;
        saveBtn.textContent = '保存中...';
        
        savePerishRateSetting(playerName, positionId, invType, maxDurability, totalSeconds, modal, saveBtn);
    });
}

function savePerishRateSetting(playerName, positionId, invType, maxDurability, totalSeconds, modal, saveBtn) {
    const loadingModal = showLoadingModal();
    
    const perishRate = (25 * maxDurability) / totalSeconds;
    
    getPlayerIndexByName(playerName).then(playerIndex => {
        if (playerIndex === null) {
            hideLoadingModal();
            if (modal) modal.remove();
            alert('该玩家不在线，无法修改物品属性');
            return;
        }
        
        const command = `con ${playerIndex} SetInventoryItemStat ${positionId} PerishRate ${perishRate} ${invType}`;
        
        const rconModeToggle = document.getElementById('rconModeToggle');
        const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
        const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
        
        fetch(apiUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command })
        })
        .then(response => response.json())
        .then(data => {
            hideLoadingModal();
            if (modal) modal.remove();
            if (data.success) {
                alert('腐朽时间设置成功！');
            } else {
                alert('腐朽时间设置失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            hideLoadingModal();
            if (modal) modal.remove();
            console.error('发送命令请求失败:', error);
            alert('发送命令请求失败: ' + error.message);
        });
    }).catch(error => {
        hideLoadingModal();
        if (modal) modal.remove();
        console.error('获取玩家索引失败:', error);
        alert('获取玩家索引失败: ' + error.message);
    });
}

function savePerishRateDirect(playerName, positionId, invType, perishRate) {
    const loadingModal = showLoadingModal();
    
    getPlayerIndexByName(playerName).then(playerIndex => {
        if (playerIndex === null) {
            hideLoadingModal();
            alert('该玩家不在线，无法修改物品属性');
            return;
        }
        
        const command = `con ${playerIndex} SetInventoryItemStat ${positionId} PerishRate ${perishRate} ${invType}`;
        
        const rconModeToggle = document.getElementById('rconModeToggle');
        const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
        const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
        
        fetch(apiUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command })
        })
        .then(response => response.json())
        .then(data => {
            hideLoadingModal();
            if (data.success) {
                alert('腐朽时间设置成功！');
            } else {
                alert('腐朽时间设置失败: ' + (data.message || '未知错误'));
            }
        })
        .catch(error => {
            hideLoadingModal();
            console.error('发送命令请求失败:', error);
            alert('发送命令请求失败: ' + error.message);
        });
    }).catch(error => {
        hideLoadingModal();
        console.error('获取玩家索引失败:', error);
        alert('获取玩家索引失败: ' + error.message);
    });
}

window.showItemContextMenu = showItemContextMenu;
