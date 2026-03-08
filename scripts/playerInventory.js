function openPlayerItemsModal(playerId, playerName) {
    const template = document.getElementById('playerItemsModalTemplate');
    const modal = template.content.cloneNode(true).querySelector('.modal');
    document.body.appendChild(modal);
    
    modal.querySelector('#playerItemName').textContent = playerName;
    
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
    
    loadPlayerInventory(modal, playerName);

    const itemSearchInput = modal.querySelector('#itemSearch');
    if (itemSearchInput) {
        itemSearchInput.addEventListener('input', (e) => {
            const searchTerm = e.target.value.toLowerCase().trim();
            filterBackpackItems(modal, searchTerm);
        });
    }

    const refreshBtn = modal.querySelector('#refreshInventoryBtn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            loadPlayerInventory(modal, playerName);
        });
    }

    const backpackGrid = modal.querySelector('#backpackGrid');
    if (backpackGrid) {
        backpackGrid.addEventListener('click', (e) => {
            const slot = e.target.closest('.backpack-slot');
            if (!slot) return;

            const backpackSlots = modal.querySelectorAll('.backpack-slot');
            const index = parseInt(slot.dataset.slot);

            backpackSlots.forEach(s => {
                s.style.borderColor = '#0f3460';
                s.style.background = 'rgba(15, 52, 96, 0.3)';
            });
            slot.style.borderColor = '#e94560';
            slot.style.background = 'rgba(233, 69, 96, 0.2)';

            const itemData = slot.dataset.item;
            if (itemData) {
                const item = JSON.parse(itemData);
                displayItemDetail(modal, item);
            } else {
                displayEmptySlotDetail(modal, '背包', index + 1);
            }
        });

        backpackGrid.addEventListener('contextmenu', (e) => {
            const slot = e.target.closest('.backpack-slot');
            if (!slot) return;
            e.preventDefault();

            const itemData = slot.dataset.item;
            if (!itemData) return;

            const item = JSON.parse(itemData);
            const index = parseInt(slot.dataset.slot);
            showItemContextMenu(e.clientX, e.clientY, item, index, 0, playerName);
        });
    }
    
    const equipSlots = modal.querySelectorAll('.equip-slot');
    equipSlots.forEach((slot) => {
        slot.addEventListener('click', () => {
            const slotType = slot.dataset.slot;
            const slotNames = {
                helmet: '头盔',
                chest: '胸甲',
                gloves: '手套',
                legs: '护腿',
                boots: '靴子'
            };
            
            const itemData = slot.dataset.item;
            if (itemData) {
                const item = JSON.parse(itemData);
                displayItemDetail(modal, item);
            } else {
                displayEmptySlotDetail(modal, slotNames[slotType], slotType);
            }
        });

        slot.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            const itemData = slot.dataset.item;
            if (!itemData) return;

            const item = JSON.parse(itemData);
            const slotType = slot.dataset.slot;
            const slotMapping = {
                'helmet': 3,
                'chest': 4,
                'gloves': 5,
                'legs': 6,
                'boots': 7
            };
            const positionId = slotMapping[slotType];
            showItemContextMenu(e.clientX, e.clientY, item, positionId, 1, playerName);
        });
    });
    
    const hotbarSlots = modal.querySelectorAll('.hotbar-slot');
    hotbarSlots.forEach((slot, index) => {
        slot.addEventListener('click', () => {
            const itemData = slot.dataset.item;
            if (itemData) {
                const item = JSON.parse(itemData);
                displayItemDetail(modal, item);
            } else {
                displayEmptySlotDetail(modal, '快捷栏', index + 1);
            }
        });

        slot.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            const itemData = slot.dataset.item;
            if (!itemData) return;

            const item = JSON.parse(itemData);
            const positionId = index;
            showItemContextMenu(e.clientX, e.clientY, item, positionId, 2, playerName);
        });
    });
}

function loadPlayerInventory(modal, playerName) {
    const loadingIndicator = modal.querySelector('#loadingIndicator');
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
                renderInventoryData(modal, data.inventory);
            } else {
                console.error('获取背包数据失败:', data.message);
                showInventoryError(modal, data.message || '获取背包数据失败');
            }
        })
        .catch(error => {
            console.error('加载背包数据出错:', error);
            showInventoryError(modal, '加载背包数据失败，请确保桌面客户端正在运行');
        })
        .finally(() => {
            if (loadingIndicator) {
                loadingIndicator.style.display = 'none';
            }
        });
}

function generateBackpackSlots(modal, itemCount) {
    const backpackGrid = modal.querySelector('#backpackGrid');
    if (!backpackGrid) return;

    backpackGrid.innerHTML = '';

    const maxSlots = 500;
    const slotsToGenerate = Math.max(50, itemCount);

    for (let i = 0; i < slotsToGenerate && i < maxSlots; i++) {
        const slot = document.createElement('div');
        slot.className = 'backpack-slot';
        slot.dataset.slot = i;
        slot.style.cssText = 'aspect-ratio: 1; background: rgba(15, 52, 96, 0.3); border: 2px solid #0f3460; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; cursor: pointer; transition: all 0.3s; position: relative;';
        slot.onmouseout = function() {
            this.style.borderColor = '#0f3460';
            this.style.background = 'rgba(15, 52, 96, 0.3)';
        };

        backpackGrid.appendChild(slot);
    }
}

function filterBackpackItems(modal, searchTerm) {
    const backpackGrid = modal.querySelector('#backpackGrid');
    if (!backpackGrid || !modal.inventory) return;

    const slots = backpackGrid.querySelectorAll('.backpack-slot');
    
    slots.forEach((slot, index) => {
        const itemData = slot.dataset.item;
        if (!itemData) {
            slot.style.display = 'flex';
            return;
        }

        const item = JSON.parse(itemData);
        const itemName = getItemName(item.template_id) || '';
        const templateId = String(item.template_id);
        
        const matchesSearch = !searchTerm || 
            itemName.toLowerCase().includes(searchTerm) ||
            templateId.includes(searchTerm);
        
        slot.style.display = matchesSearch ? 'flex' : 'none';
    });
}

function renderInventoryData(modal, inventory) {
    modal.inventory = inventory;
    
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

    const maxItemId = backpack.length > 0 ? Math.max(...backpack.map(item => item.item_id || 0)) : 0;
    generateBackpackSlots(modal, Math.max(50, maxItemId + 1));

    const backpackGrid = modal.querySelector('#backpackGrid');
    const backpackSlots = backpackGrid.querySelectorAll('.backpack-slot');
    
    backpack.forEach((item) => {
        const slotIndex = item.item_id;
        if (slotIndex >= 0 && slotIndex < backpackSlots.length) {
            const slot = backpackSlots[slotIndex];
            slot.dataset.item = JSON.stringify(item);
            
            const iconPath = getItemIconPath(item.template_id);
            const itemName = getItemName(item.template_id);
            
            if (iconPath) {
                slot.innerHTML = `
                    <img src="${iconPath}" alt="${itemName || '物品'}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                    <div style="display: none; width: 100%; height: 100%; align-items: center; justify-content: center; font-size: 24px;">📦</div>
                    <span class="item-count" style="position: absolute; bottom: 2px; right: 2px; background: rgba(0,0,0,0.8); color: #fff; padding: 1px 4px; border-radius: 3px; font-size: 10px; font-weight: bold;">${item.quantity || 1}</span>
                `;
            } else {
                slot.innerHTML = `
                    <span style="font-size: 24px;">📦</span>
                    <span class="item-count" style="position: absolute; bottom: 2px; right: 2px; background: rgba(0,0,0,0.8); color: #fff; padding: 1px 4px; border-radius: 3px; font-size: 10px; font-weight: bold;">${item.quantity || 1}</span>
                `;
            }
            slot.style.borderColor = '#0f3460';
            slot.style.background = 'rgba(15, 52, 96, 0.3)';
        }
    });

    const equipSlots = modal.querySelectorAll('.equip-slot');
    const slotMapping = {
        'helmet': 3,
        'chest': 4,
        'gloves': 5,
        'legs': 6,
        'boots': 7
    };
    
    equipSlots.forEach(slot => {
        const slotType = slot.dataset.slot;
        const targetItemId = slotMapping[slotType];
        const item = equipment.find(i => i && i.item_id === targetItemId);
        if (item) {
            slot.dataset.item = JSON.stringify(item);
            const iconPath = getItemIconPath(item.template_id);
            const itemName = getItemName(item.template_id);
            
            if (iconPath) {
                slot.innerHTML = `
                    <img src="${iconPath}" alt="${itemName || '装备'}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                    <div style="display: none; width: 100%; height: 100%; align-items: center; justify-content: center; font-size: 24px;">🛡️</div>
                `;
            } else {
                slot.innerHTML = `
                    <span style="font-size: 24px;">🛡️</span>
                `;
            }
            slot.style.borderColor = '#0f3460';
            slot.style.background = 'rgba(15, 52, 96, 0.3)';
        } else {
            delete slot.dataset.item;
            slot.innerHTML = '';
            slot.style.borderColor = '#0f3460';
            slot.style.background = 'rgba(15, 52, 96, 0.3)';
        }
    });
    
    const hotbarSlots = modal.querySelectorAll('.hotbar-slot');
    hotbarSlots.forEach((slot, index) => {
        const item = quickbar.find(i => i && i.item_id === index);
        if (item) {
            slot.dataset.item = JSON.stringify(item);
            const iconPath = getItemIconPath(item.template_id);
            const itemName = getItemName(item.template_id);
            
            if (iconPath) {
                slot.innerHTML = `
                    <img src="${iconPath}" alt="${itemName || '物品'}" style="width: 100%; height: 100%; object-fit: contain;" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
                    <div style="display: none; width: 100%; height: 100%; align-items: center; justify-content: center; font-size: 24px;">⚡</div>
                    <span class="item-count" style="position: absolute; bottom: 2px; right: 2px; background: rgba(0,0,0,0.8); color: #fff; padding: 1px 4px; border-radius: 3px; font-size: 10px; font-weight: bold;">${item.quantity || 1}</span>
                `;
            } else {
                slot.innerHTML = `
                    <span style="font-size: 24px;">⚡</span>
                    <span class="item-count" style="position: absolute; bottom: 2px; right: 2px; background: rgba(0,0,0,0.8); color: #fff; padding: 1px 4px; border-radius: 3px; font-size: 10px; font-weight: bold;">${item.quantity || 1}</span>
                `;
            }
            slot.style.borderColor = '#0f3460';
            slot.style.background = 'rgba(15, 52, 96, 0.3)';
        } else {
            delete slot.dataset.item;
            slot.innerHTML = '';
            slot.style.borderColor = '#0f3460';
            slot.style.background = 'rgba(15, 52, 96, 0.3)';
        }
    });
}

function displayItemDetail(modal, item) {
    const detailContent = modal.querySelector('#selectedItemContent');
    if (!detailContent) return;
    
    if (!itemConfigLoaded) {
        detailContent.innerHTML = `
            <div style="color: #ff9800; text-align: center; padding: 20px;">
                <div style="font-size: 32px; margin-bottom: 10px;">⏳</div>
                <div style="font-size: 14px;">物品配置加载中，请稍候...</div>
            </div>
        `;
        const checkInterval = setInterval(() => {
            if (itemConfigLoaded) {
                clearInterval(checkInterval);
                displayItemDetail(modal, item);
            }
        }, 100);
        setTimeout(() => clearInterval(checkInterval), 5000);
        return;
    }
    
    const itemEmoji = item.inv_type === 1 ? '🛡️' : '📦';
    const iconPath = getItemIconPath(item.template_id);
    const itemName = getItemName(item.template_id);
    
    const itemConfig = itemConfigData[item.template_id];
    const categoryMap = {
        'Armor': '护甲',
        'Weapon': '武器',
        'Consumable': '消耗品',
        'Material': '材料',
        'Component': '组件',
        'Utility': '多功能',
        'BuildingItem': '建筑物品',
        'Decoration': '装饰',
        'Tools': '工具'
    };
    const itemCategory = itemConfig && itemConfig.GUICategory ? (categoryMap[itemConfig.GUICategory] || itemConfig.GUICategory) : '未知';
    
    let iconHtml = '';
    if (iconPath) {
        iconHtml = `
            <img src="${iconPath}" alt="${itemName || '物品'}" style="width: 64px; height: 64px; object-fit: contain;" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
            <div style="display: none; width: 64px; height: 64px; background: rgba(15, 52, 96, 0.3); border: 2px solid #0f3460; border-radius: 8px; align-items: center; justify-content: center; font-size: 32px;">${itemEmoji}</div>
        `;
    } else {
        iconHtml = `
            <div style="width: 64px; height: 64px; background: rgba(15, 52, 96, 0.3); border: 2px solid #0f3460; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 32px;">
                ${itemEmoji}
            </div>
        `;
    }
    
    detailContent.innerHTML = `
        <div style="display: flex; gap: 15px; align-items: flex-start;">
            ${iconHtml}
            <div style="flex: 1;">
                <div style="color: #e94560; font-weight: bold; font-size: 16px; margin-bottom: 5px;">${itemName || item.instance_name || item.item_path || '未知物品'}</div>
                <div style="color: #b0bec5; font-size: 13px; margin-bottom: 8px;">模板ID: ${item.template_id}</div>
                <div style="display: flex; gap: 15px; font-size: 12px;">
                    <span style="color: #4caf50;">数量: ${item.quantity}</span>
                    <span style="color: #2196f3;">位置ID: ${item.item_id}</span>
                    <span style="color: #ff9800;">背包类型: ${item.inv_type}</span>
                    <span style="color: #9c27b0;">物品类型: ${itemCategory}</span>
                </div>
            </div>
        </div>
    `;
}

function displayEmptySlotDetail(modal, slotName, slotId) {
    const detailContent = modal.querySelector('#selectedItemContent');
    if (!detailContent) return;
    
    const emoji = slotName === '快捷栏' ? '⚡' : (slotName === '头盔' || slotName === '胸甲' || slotName === '手套' || slotName === '护腿' || slotName === '靴子' ? '🛡️' : '📦');
    
    detailContent.innerHTML = `
        <div style="display: flex; gap: 15px; align-items: flex-start;">
            <div style="width: 64px; height: 64px; background: rgba(15, 52, 96, 0.3); border: 2px solid #0f3460; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 32px;">
                ${emoji}
            </div>
            <div style="flex: 1;">
                <div style="color: #e94560; font-weight: bold; font-size: 16px; margin-bottom: 5px;">${slotName} ${typeof slotId === 'number' ? slotId : ''}</div>
                <div style="color: #b0bec5; font-size: 13px; margin-bottom: 8px;">当前未装备任何物品</div>
            </div>
        </div>
    `;
}

function showInventoryError(modal, errorMessage) {
    const detailContent = modal.querySelector('#selectedItemContent');
    if (detailContent) {
        detailContent.innerHTML = `
            <div style="color: #ef5350; text-align: center; padding: 20px;">
                <div style="font-size: 32px; margin-bottom: 10px;">❌</div>
                <div style="font-size: 14px;">${errorMessage}</div>
            </div>
        `;
    }
}

window.openPlayerItemsModal = openPlayerItemsModal;
