function openAutoExecuteModal() {
    const template = document.getElementById('autoExecuteModalTemplate');
    const modalClone = template.content.cloneNode(true);
    const modal = modalClone.querySelector('.modal');
    
    document.body.appendChild(modal);
    
    modal.addedConditions = [];
    modal.addedSecondaryConditions = [];
    
    loadCommandsAndCategories(modal);
    
    loadSavedRules(modal);
    
    const primaryConditionBtns = modal.querySelectorAll('.condition-btn:not(.secondary-btn)');
    primaryConditionBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const conditionType = this.getAttribute('data-condition');
            
            const existingCondition = modal.addedConditions.find(c => c.type === conditionType);
            if (existingCondition) {
                alert('该条件已经添加，请先删除已添加的条件');
                return;
            }
            
            openConditionSettingModal(modal, conditionType, 'primary');
        });
    });
    
    const secondaryConditionBtns = modal.querySelectorAll('.condition-btn.secondary-btn');
    secondaryConditionBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const conditionType = this.getAttribute('data-condition');
            
            if (modal.addedConditions.length === 0) {
                alert('该触发条件必须先有其它触发条件作为前置才能使用，请先添加其它条件。');
                return;
            }
            
            openConditionSettingModal(modal, conditionType, 'secondary');
        });
    });
    
    const executeTypeSelect = modal.querySelector('#executeType');
    executeTypeSelect.addEventListener('change', function() {
        updateExecuteSettings(modal);
    });
    
    const saveBtn = modal.querySelector('#saveAutoExecuteBtn');
    saveBtn.onclick = function() {
        saveAutoExecuteRule(modal);
    };
    
    const ruleSearchInput = modal.querySelector('#ruleSearch');
    if (ruleSearchInput) {
        ruleSearchInput.addEventListener('input', function() {
            loadSavedRules(modal);
        });
    }
    
    const insertNotificationVariableBtn = modal.querySelector('#insertNotificationVariableBtn');
    const notificationVariableList = modal.querySelector('#notificationVariableList');
    if (insertNotificationVariableBtn) {
        insertNotificationVariableBtn.addEventListener('click', function() {
            if (notificationVariableList.style.display === 'none') {
                notificationVariableList.style.display = 'block';
            } else {
                notificationVariableList.style.display = 'none';
            }
        });
        
        const notificationVariableBtns = modal.querySelectorAll('.notification-variable-btn');
        notificationVariableBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                const variable = this.getAttribute('data-variable');
                const notificationMessageInput = modal.querySelector('#notificationMessage');
                const cursorPosition = notificationMessageInput.selectionStart;
                const currentValue = notificationMessageInput.value;
                const newValue = currentValue.slice(0, cursorPosition) + variable + currentValue.slice(cursorPosition);
                notificationMessageInput.value = newValue;
                notificationMessageInput.focus();
                notificationMessageInput.setSelectionRange(cursorPosition + variable.length, cursorPosition + variable.length);
                notificationVariableList.style.display = 'none';
            });
        });
    }
    
    const insertFailNotificationVariableBtn = modal.querySelector('#insertFailNotificationVariableBtn');
    const failNotificationVariableList = modal.querySelector('#failNotificationVariableList');
    if (insertFailNotificationVariableBtn) {
        insertFailNotificationVariableBtn.addEventListener('click', function() {
            if (failNotificationVariableList.style.display === 'none') {
                failNotificationVariableList.style.display = 'block';
            } else {
                failNotificationVariableList.style.display = 'none';
            }
        });
        
        const failNotificationVariableBtns = modal.querySelectorAll('.fail-notification-variable-btn');
        failNotificationVariableBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                const variable = this.getAttribute('data-variable');
                const failNotificationMessageInput = modal.querySelector('#failNotificationMessage');
                const cursorPosition = failNotificationMessageInput.selectionStart;
                const currentValue = failNotificationMessageInput.value;
                const newValue = currentValue.slice(0, cursorPosition) + variable + currentValue.slice(cursorPosition);
                failNotificationMessageInput.value = newValue;
                failNotificationMessageInput.focus();
                failNotificationMessageInput.setSelectionRange(cursorPosition + variable.length, cursorPosition + variable.length);
                failNotificationVariableList.style.display = 'none';
            });
        });
    }
    
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

function updateExecuteSettings(modal) {
    const executeType = modal.querySelector('#executeType').value;
    const singleSetting = modal.querySelector('#singleCommandSetting');
    const categorySetting = modal.querySelector('#categoryCommandSetting');
    
    if (executeType === 'single') {
        singleSetting.style.display = 'block';
        categorySetting.style.display = 'none';
    } else if (executeType === 'category') {
        singleSetting.style.display = 'none';
        categorySetting.style.display = 'block';
    } else {
        singleSetting.style.display = 'none';
        categorySetting.style.display = 'none';
    }
}

function loadCommandsAndCategories(modal) {
    fetch('/api/categories')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const categorySelect = modal.querySelector('#categorySelect');
                categorySelect.innerHTML = '';
                
                data.categories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.name;
                    option.textContent = category.name;
                    categorySelect.appendChild(option);
                });
            }
        });
    
    fetch('/api/commands')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                modal.allCommands = data.commands;
                
                const optionsContainer = modal.querySelector('#singleCommandOptions');
                const searchInput = modal.querySelector('#singleCommandSearch');
                const hiddenSelect = modal.querySelector('#singleCommandSelect');
                
                renderCommandOptions(modal, data.commands);
                
                searchInput.addEventListener('input', function() {
                    filterCommands(modal, this.value);
                    optionsContainer.style.display = 'block';
                });
                
                searchInput.addEventListener('focus', function() {
                    optionsContainer.style.display = 'block';
                });
                
                document.addEventListener('click', function(e) {
                    if (!e.target.closest('.custom-select-wrapper')) {
                        optionsContainer.style.display = 'none';
                    }
                });
            }
        });
    
    loadSavedRules(modal);
}

function renderCommandOptions(modal, commands) {
    const optionsContainer = modal.querySelector('#singleCommandOptions');
    optionsContainer.innerHTML = '';
    
    if (commands.length === 0) {
        const noResult = document.createElement('div');
        noResult.style.padding = '10px';
        noResult.style.color = '#b0bec5';
        noResult.style.fontSize = '12px';
        noResult.textContent = '未找到匹配的命令';
        optionsContainer.appendChild(noResult);
        return;
    }
    
    commands.forEach(cmd => {
        const option = document.createElement('div');
        option.className = 'custom-select-option';
        option.style.padding = '10px 12px';
        option.style.cursor = 'pointer';
        option.style.borderBottom = '1px solid #333';
        option.style.fontSize = '13px';
        option.style.color = '#b0bec5';
        option.innerHTML = `<strong style="color: #9c27b0;">${cmd.name}</strong> <span style="color: #666;">(${cmd.category})</span>`;
        
        option.addEventListener('mouseenter', function() {
            this.style.background = '#2a2a2a';
        });
        
        option.addEventListener('mouseleave', function() {
            this.style.background = 'transparent';
        });
        
        option.addEventListener('click', function() {
            const searchInput = modal.querySelector('#singleCommandSearch');
            const hiddenSelect = modal.querySelector('#singleCommandSelect');
            
            searchInput.value = cmd.name;
            hiddenSelect.value = cmd.example;
            optionsContainer.style.display = 'none';
        });
        
        optionsContainer.appendChild(option);
    });
}

function filterCommands(modal, searchText) {
    const optionsContainer = modal.querySelector('#singleCommandOptions');
    const searchInput = modal.querySelector('#singleCommandSearch');
    
    if (!searchText) {
        renderCommandOptions(modal, modal.allCommands);
    } else {
        const filteredCommands = modal.allCommands.filter(cmd => {
            const searchLower = searchText.toLowerCase();
            return cmd.name.toLowerCase().includes(searchLower) || 
                   cmd.category.toLowerCase().includes(searchLower) ||
                   (cmd.description && cmd.description.toLowerCase().includes(searchLower));
        });
        
        renderCommandOptions(modal, filteredCommands);
    }
}

function loadSavedRules(modal) {
    const savedRulesList = modal.querySelector('#savedRulesList');
    const searchInput = modal.querySelector('#ruleSearch');
    
    savedRulesList.innerHTML = `
        <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
            加载中...
        </div>
    `;
    
    Promise.all([
        fetch('/api/auto-trigger-rules').then(response => response.json()),
        fetch('/api/commands').then(response => response.json())
    ])
    .then(([rulesResult, commandsResult]) => {
        if (rulesResult.success && rulesResult.rules && rulesResult.rules.length > 0) {
            const conditionNames = {
                keyword: '关键词',
                amount: '金额',
                tag: '标签',
                level: '等级',
                playtime: '在线时间',
                new_player: '新玩家',
                server_time: '时间条件',
                item: '背包物品',
                vip: '会员'
            };
            
            const operatorNames = {
                eq: '等于',
                startsWith: '开头等于',
                contains: '包含',
                notContains: '不包含',
                endsWith: '结尾等于',
                gt: '大于',
                lt: '小于',
                gte: '等于或者大于',
                lte: '等于或者小于',
                interval: '每隔',
                date_range: '日期范围',
                weekday: '星期'
            };
            
            const commands = commandsResult.success ? commandsResult.commands : [];
            const commandMap = {};
            commands.forEach(cmd => {
                commandMap[cmd.example] = cmd.name;
            });
            
            const searchText = searchInput ? searchInput.value.toLowerCase() : '';
            
            let filteredRules = rulesResult.rules;
            
            if (searchText) {
                filteredRules = rulesResult.rules.filter(rule => {
                    const ruleName = (rule.rule_name || '').toLowerCase();
                    const statusText = rule.enabled ? '已启用' : '已禁用';
                    const executeTypeText = rule.execute_type === 'single' ? '单个命令' : rule.execute_type === 'category' ? '命令分类' : '不操作';
                    
                    let commandDisplay = '';
                    if (rule.execute_type === 'single') {
                        commandDisplay = commandMap[rule.execute_data.command] || rule.execute_data.command.replace(/^\//, '').split(' ')[0];
                    } else if (rule.execute_type === 'category') {
                        commandDisplay = rule.execute_data.category;
                    }
                    
                    let conditionsText = '';
                    if (rule.conditions && rule.conditions.length > 0) {
                        conditionsText = rule.conditions.map(cond => {
                            if (cond.type === 'new_player') {
                                return conditionNames[cond.type];
                            }
                            if (cond.type === 'server_time') {
                                return `${conditionNames[cond.type]} ${cond.value}`;
                            }
                            return `${conditionNames[cond.type]} ${operatorNames[cond.operator]} ${cond.value}`;
                        }).join(' ');
                    }
                    
                    let afterExecuteText = '';
                    if (rule.after_execute) {
                        if (rule.after_execute.amountOperation) {
                            const opText = rule.after_execute.amountOperation === 'add' ? '增加' : rule.after_execute.amountOperation === 'deduct' ? '扣除' : '强行指定';
                            afterExecuteText += `金额 ${opText} ${rule.after_execute.amountValue} `;
                        }
                        if (rule.after_execute.setTag) {
                            afterExecuteText += `设置标签 ${rule.after_execute.setTag}`;
                        }
                        if (rule.after_execute.notificationMessage) {
                            afterExecuteText += ` 发送通知 "${rule.after_execute.notificationMessage}"`;
                        }
                    }
                    
                    return ruleName.includes(searchText) ||
                           statusText.includes(searchText) ||
                           executeTypeText.includes(searchText) ||
                           commandDisplay.toLowerCase().includes(searchText) ||
                           conditionsText.toLowerCase().includes(searchText) ||
                           afterExecuteText.toLowerCase().includes(searchText);
                });
            }
            
            let html = '<div style="border: 1px solid #6a1b9a; border-radius: 8px; padding: 15px; background: linear-gradient(145deg, #1a1a1a, #101010);">';
            
            if (filteredRules.length === 0) {
                html += `
                    <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
                        ${searchText ? '未找到匹配的规则' : '暂无保存的自动触发规则'}
                    </div>
                `;
            } else {
                filteredRules.forEach((rule, index) => {
                    const statusColor = rule.enabled ? '#4caf50' : '#ef5350';
                    const statusText = rule.enabled ? '已启用' : '已禁用';
                    
                    let commandDisplay = '';
                    if (rule.execute_type === 'single') {
                        const commandName = commandMap[rule.execute_data.command] || rule.execute_data.command.replace(/^\//, '').split(' ')[0];
                        commandDisplay = `<div style="margin-left: 10px;">${commandName}</div>`;
                    } else if (rule.execute_type === 'category') {
                        commandDisplay = `<div style="margin-left: 10px;">${rule.execute_data.category}</div>`;
                    }
                    
                    html += `
                        <div style="margin-bottom: 15px; padding: 15px; border: 1px solid #333; border-radius: 5px; background: #0a0a0a;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                                <div>
                                    <strong style="color: #9c27b0; font-size: 16px;">${rule.rule_name || '未命名规则'}</strong>
                                    <span style="background: ${statusColor}; color: #fff; padding: 2px 8px; border-radius: 3px; font-size: 11px; margin-left: 8px;">${statusText}</span>
                                </div>
                                <div style="text-align: right;">
                                    <div style="color: #b0bec5; font-size: 11px;">ID: ${rule.id}</div>
                                </div>
                            </div>
                            <div style="color: #b0bec5; font-size: 13px; margin-bottom: 10px;">
                                <div style="margin-bottom: 5px;"><strong style="color: #e94560;">触发条件：</strong></div>
                                ${rule.conditions && rule.conditions.length > 0 ? rule.conditions.map(cond => {
                                    if (cond.type === 'new_player') {
                                        return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}</div>`;
                                    }
                                    if (cond.type === 'vip') {
                                        const vipText = cond.value === 'yes' ? '是会员' : '不是会员';
                                        return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: ${vipText}</div>`;
                                    }
                                    if (cond.type === 'server_time') {
                                        const operator = cond.operator;
                                        const value = cond.value;
                                        if (operator === 'weekday') {
                                            const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
                                            const selectedDays = value.split(',').map(d => weekdayNames[parseInt(d)]).join('、');
                                            return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: 每周的 ${selectedDays}</div>`;
                                        } else {
                                            return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: ${value}</div>`;
                                        }
                                    }
                                    if (cond.type === 'item') {
                                        const items = cond.value.split(';');
                                        const itemTexts = items.map(item => {
                                            const parts = item.split(':');
                                            if (parts.length === 2) {
                                                return `物品ID ${parts[0]} 的数量 ${parts[1]}`;
                                            }
                                            return item;
                                        });
                                        return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: ${operatorNames[cond.operator]} ${itemTexts.join(', ')}</div>`;
                                    }
                                    return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: ${operatorNames[cond.operator]} ${cond.value}</div>`;
                                }).join('') : '<div style="margin-left: 10px;">无</div>'}
                            </div>
                            ${rule.secondary_conditions && rule.secondary_conditions.length > 0 ? `
                            <div style="color: #b0bec5; font-size: 13px; margin-bottom: 10px;">
                                <div style="margin-bottom: 5px;"><strong style="color: #ff9800;">二级条件：</strong></div>
                                ${rule.secondary_conditions.map(cond => {
                                    if (cond.type === 'vip') {
                                        const vipText = cond.value === 'yes' ? '是会员' : '不是会员';
                                        return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: ${vipText}</div>`;
                                    }
                                    if (cond.type === 'item') {
                                        const items = cond.value.split(';');
                                        const itemTexts = items.map(item => {
                                            const parts = item.split(':');
                                            if (parts.length === 2) {
                                                return `物品ID ${parts[0]} 的数量 ${parts[1]}`;
                                            }
                                            return item;
                                        });
                                        return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: ${operatorNames[cond.operator]} ${itemTexts.join(', ')}</div>`;
                                    }
                                    return `<div style="margin-left: 10px;">• ${conditionNames[cond.type]}: ${operatorNames[cond.operator]} ${cond.value}</div>`;
                                }).join('')}
                            </div>
                            ` : ''}
                            <div style="color: #b0bec5; font-size: 13px; margin-bottom: 10px;">
                                <div style="margin-bottom: 5px;"><strong style="color: #2196f3;">执行操作：</strong></div>
                                ${rule.execute_type === 'single' ? `<div style="margin-left: 10px;">• 单个命令: ${commandMap[rule.execute_data.command] || rule.execute_data.command}</div>` : 
                                  rule.execute_type === 'category' ? `<div style="margin-left: 10px;">• 命令分类: ${rule.execute_data.category}</div>` : 
                                  '<div style="margin-left: 10px;">• 不执行操作</div>'}
                            </div>
                            ${rule.after_execute ? `
                            <div style="color: #b0bec5; font-size: 13px; margin-bottom: 10px;">
                                <div style="margin-bottom: 5px;"><strong style="color: #4caf50;">执行后操作：</strong></div>
                                ${rule.after_execute.amountOperation ? `<div style="margin-left: 10px;">• 金额: ${rule.after_execute.amountOperation === 'add' ? '增加' : rule.after_execute.amountOperation === 'deduct' ? '扣除' : '强行指定'} ${rule.after_execute.amountValue}</div>` : ''}
                                ${rule.after_execute.setTag ? `<div style="margin-left: 10px;">• 设置标签: ${rule.after_execute.setTag}</div>` : ''}
                                ${rule.after_execute.notificationMessage ? `<div style="margin-left: 10px;">• 条件通过通知: "${rule.after_execute.notificationMessage}"</div>` : ''}
                                ${rule.after_execute.failNotificationMessage ? `<div style="margin-left: 10px;">• 条件未通过通知: "${rule.after_execute.failNotificationMessage}"</div>` : ''}
                            </div>
                            ` : ''}
                            <div style="margin-top: 10px; text-align: right; display: flex; gap: 10px; justify-content: flex-end;">
                                <button type="button" class="btn btn-info" style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #2196f3, #42a5f5);" onclick="toggleRule(${rule.id})">
                                    ${rule.enabled ? '🔴 禁用' : '🟢 启用'}
                                </button>
                                <button type="button" class="btn btn-primary" style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #ff9800, #ffb74d);" onclick="editRule(${rule.id})">
                                    ✏️ 编辑
                                </button>
                                <button type="button" class="btn btn-danger" style="padding: 6px 12px; font-size: 12px; background: linear-gradient(45deg, #f44336, #d32f2f);" onclick="deleteRule(${rule.id}, this)">
                                    🗑️ 删除
                                </button>
                            </div>
                        </div>
                    `;
                });
            }
            
            html += '</div>';
            savedRulesList.innerHTML = html;
        } else {
            savedRulesList.innerHTML = `
                <div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">
                    暂无保存的自动触发规则
                </div>
            `;
        }
    })
    .catch(error => {
        console.error('加载规则列表失败:', error);
        savedRulesList.innerHTML = `
            <div style="color: #ef5350; font-style: italic; text-align: center; padding: 20px;">
                加载失败，请刷新页面重试
            </div>
        `;
    });
}

function toggleRule(ruleId) {
    const modal = document.querySelector('.modal');
    if (!modal) return;

    const savedRulesList = modal.querySelector('#savedRulesList');
    if (!savedRulesList) return;

    const scrollTop = savedRulesList.scrollTop;

    fetch(`/api/auto-trigger-rules/${ruleId}/toggle`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            loadSavedRules(modal);
            setTimeout(() => {
                const newSavedRulesList = modal.querySelector('#savedRulesList');
                if (newSavedRulesList) {
                    newSavedRulesList.scrollTop = scrollTop;
                }
            }, 50);
        } else {
            alert('操作失败: ' + result.message);
        }
    })
    .catch(error => {
        console.error('切换规则状态失败:', error);
        alert('操作失败，请检查网络连接');
    });
}

function deleteRule(ruleId, buttonElement) {
    if (!confirm('确定要删除这个规则吗？')) {
        return;
    }

    const modal = document.querySelector('.modal');
    if (!modal) return;

    const savedRulesList = modal.querySelector('#savedRulesList');
    if (!savedRulesList) return;

    const scrollTop = savedRulesList.scrollTop;

    fetch('/api/auto-trigger-rules/delete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ id: ruleId })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            loadSavedRules(modal);
            setTimeout(() => {
                const newSavedRulesList = modal.querySelector('#savedRulesList');
                if (newSavedRulesList) {
                    newSavedRulesList.scrollTop = scrollTop;
                }
            }, 50);
        } else {
            alert('删除失败: ' + result.message);
        }
    })
    .catch(error => {
        console.error('删除规则失败:', error);
        alert('删除失败，请检查网络连接');
    });
}

function editRule(ruleId) {
    fetch('/api/auto-trigger-rules')
        .then(response => response.json())
        .then(result => {
            if (result.success && result.rules) {
                const rule = result.rules.find(r => r.id === ruleId);
                if (rule) {
                    const modal = document.querySelector('.modal');
                    if (modal) {
                        modal.querySelector('#ruleNameInput').value = rule.rule_name || '';
                        
                        modal.addedConditions = [];
                        if (rule.conditions && rule.conditions.length > 0) {
                            rule.conditions.forEach(condition => {
                                modal.addedConditions.push(condition);
                            });
                        }
                        updateAddedConditionsList(modal);
                        
                        modal.addedSecondaryConditions = [];
                        if (rule.secondary_conditions && rule.secondary_conditions.length > 0) {
                            rule.secondary_conditions.forEach(condition => {
                                modal.addedSecondaryConditions.push(condition);
                            });
                        }
                        updateAddedConditionsList(modal);
                        
                        modal.querySelector('#executeType').value = rule.execute_type;
                        updateExecuteSettings(modal);
                        
                        if (rule.execute_type === 'single' && rule.execute_data && rule.execute_data.command) {
                            const singleCommandSelect = modal.querySelector('#singleCommandSelect');
                            const searchInput = modal.querySelector('#singleCommandSearch');
                            
                            singleCommandSelect.value = rule.execute_data.command;
                            
                            const command = modal.allCommands.find(cmd => cmd.example === rule.execute_data.command);
                            if (command) {
                                searchInput.value = command.name;
                            }
                        } else if (rule.execute_type === 'category' && rule.execute_data && rule.execute_data.category) {
                            const categorySelect = modal.querySelector('#categorySelect');
                            categorySelect.value = rule.execute_data.category;
                        }
                        
                        if (rule.after_execute) {
                            modal.querySelector('#amountOperation').value = rule.after_execute.amountOperation || '';
                            modal.querySelector('#amountValue').value = rule.after_execute.amountValue || '';
                            modal.querySelector('#setTag').value = rule.after_execute.setTag || '';
                            modal.querySelector('#notificationMessage').value = rule.after_execute.notificationMessage || '';
                            modal.querySelector('#failNotificationMessage').value = rule.after_execute.failNotificationMessage || '';
                        } else {
                            modal.querySelector('#amountOperation').value = '';
                            modal.querySelector('#amountValue').value = '';
                            modal.querySelector('#setTag').value = '';
                            modal.querySelector('#notificationMessage').value = '';
                            modal.querySelector('#failNotificationMessage').value = '';
                        }
                        
                        modal.originalEnabled = rule.enabled;
                        
                        const saveBtn = modal.querySelector('#saveAutoExecuteBtn');
                        saveBtn.textContent = '💾 更新自动触发规则';
                        saveBtn.onclick = function() {
                            updateAutoExecuteRule(modal, ruleId);
                        };
                        
                        modal.querySelector('.modal-section').scrollIntoView({ behavior: 'smooth' });
                    }
                } else {
                    alert('未找到该规则');
                }
            } else {
                alert('加载规则失败');
            }
        })
        .catch(error => {
            console.error('加载规则失败:', error);
            alert('加载规则失败，请检查网络连接');
        });
}

function updateAutoExecuteRule(modal, ruleId) {
    if (modal.addedConditions.length === 0) {
        alert('请至少添加一个触发条件');
        return;
    }
    
    const selectedConditions = modal.addedConditions;
    
    const executeType = modal.querySelector('#executeType').value;
    const executeData = {
        type: executeType
    };
    
    if (executeType === 'single') {
        executeData.command = modal.querySelector('#singleCommandSelect').value;
    } else if (executeType === 'category') {
        executeData.category = modal.querySelector('#categorySelect').value;
    }
    
    const amountOperation = modal.querySelector('#amountOperation').value;
    const amountValue = modal.querySelector('#amountValue').value;
    const setTag = modal.querySelector('#setTag').value;
    const notificationMessage = modal.querySelector('#notificationMessage').value.trim();
    const failNotificationMessage = modal.querySelector('#failNotificationMessage').value.trim();
    
    const afterExecute = {};
    if (amountOperation && amountValue) {
        afterExecute.amountOperation = amountOperation;
        afterExecute.amountValue = parseFloat(amountValue);
    }
    if (setTag) {
        afterExecute.setTag = setTag;
    }
    if (notificationMessage) {
        afterExecute.notificationMessage = notificationMessage;
    }
    if (failNotificationMessage) {
        afterExecute.failNotificationMessage = failNotificationMessage;
    }
    
    const ruleNameInput = modal.querySelector('#ruleNameInput').value.trim();
    const ruleName = ruleNameInput || `自动触发规则 ${new Date().toLocaleString()}`;
    
    const enabled = modal.originalEnabled !== undefined ? modal.originalEnabled : true;
    
    const ruleData = {
        id: ruleId,
        rule_name: ruleName,
        conditions: selectedConditions,
        secondary_conditions: modal.addedSecondaryConditions.length > 0 ? modal.addedSecondaryConditions : null,
        execute_type: executeType,
        execute_data: executeData,
        after_execute: Object.keys(afterExecute).length > 0 ? afterExecute : null,
        enabled: enabled
    };
    
    fetch('/api/auto-trigger-rules/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(ruleData)
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            loadSavedRules(modal);
            const saveBtn = modal.querySelector('#saveAutoExecuteBtn');
            saveBtn.textContent = '💾 保存自动触发规则';
            saveBtn.onclick = function() {
                saveAutoExecuteRule(modal);
            };
            modal.addedConditions = [];
            updateAddedConditionsList(modal);
            modal.addedSecondaryConditions = [];
            updateAddedConditionsList(modal);
            modal.querySelector('#ruleNameInput').value = '';
            modal.querySelector('#executeType').value = 'single';
            updateExecuteSettings(modal);
            modal.querySelector('#amountOperation').value = '';
            modal.querySelector('#amountValue').value = '';
            modal.querySelector('#setTag').value = '';
            delete modal.originalEnabled;
        } else {
            alert('更新失败: ' + result.message);
        }
    })
    .catch(error => {
        console.error('更新规则失败:', error);
        alert('更新失败，请检查网络连接');
    });
}

window.openAutoExecuteModal = openAutoExecuteModal;
window.toggleRule = toggleRule;
window.deleteRule = deleteRule;
window.editRule = editRule;
