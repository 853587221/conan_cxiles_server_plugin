document.addEventListener('DOMContentLoaded', function() {
    loadItemConfig();
    
    const loginForm = document.getElementById('loginForm');
    const loginMessage = document.getElementById('loginMessage');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    
    const hostInput = document.getElementById('host');
    const rconPasswordInput = document.getElementById('password');
    const portInput = document.getElementById('port');
    const saveConnectionCheckbox = document.getElementById('saveConnection');
    const statusDot = document.getElementById('statusDot');
    const statusText = document.getElementById('statusText');
    const commandInput = document.getElementById('commandInput');
    const sendBtn = document.getElementById('sendBtn');
    const consoleOutput = document.getElementById('consoleOutput');
    
    function appendConsoleMessage(message, type = 'info') {
        if (!consoleOutput) return;
        
        const messageDiv = document.createElement('div');
        let color = '#b0bec5';
        
        switch(type) {
            case 'success':
                color = '#4caf50';
                break;
            case 'error':
                color = '#ef5350';
                break;
            case 'warning':
                color = '#ff9800';
                break;
            case 'chat':
                color = '#00bcd4';
                break;
            case 'gold':
                color = '#ffd700';
                break;
            case 'tag':
                color = '#9c27b0';
                break;
            case 'info':
            default:
                color = '#b0bec5';
        }
        
        messageDiv.style.color = color;
        messageDiv.textContent = message;
        consoleOutput.appendChild(messageDiv);
        
        consoleOutput.scrollTop = consoleOutput.scrollHeight;
    }
    
    function updateServerStats(stats) {
        const serverStatsElement = document.getElementById('serverStats');
        
        if (!serverStatsElement || !stats) return;
        
        let statusMessage = '';
        
        if (stats.uptime_formatted) {
            const uptime = stats.uptime_formatted;
            const formattedUptime = uptime
                .replace(/(\d+)h/, '$1小时')
                .replace(/(\d+)m/, '$1分钟')
                .replace(/(\d+)s/, '$1秒');
            statusMessage += `运行时间: ${formattedUptime} | `;
        }
        
        if (stats.players !== undefined) {
            statusMessage += `在线玩家: ${stats.players} | `;
        }
        
        if (stats.cpu && stats.cpu.current !== undefined) {
            statusMessage += `CPU: ${stats.cpu.current.toFixed(2)}% | `;
        }
        
        if (stats.memory && stats.memory.used !== undefined) {
            const used = stats.memory.used.toFixed(2);
            const available = stats.memory.total.toFixed(2);
            statusMessage += `内存: ${used}GB/${available}GB | `;
        }
        
        if (stats.fps && stats.fps.average !== undefined) {
            statusMessage += `FPS: ${stats.fps.average.toFixed(1)}`;
        }
        
        if (statusMessage) {
            serverStatsElement.style.display = 'inline';
            serverStatsElement.textContent = statusMessage;
        }
    }
    
    let eventSource = null;
    
    function startPlayerJoinMonitoring() {
        if (eventSource) {
            eventSource.close();
        }
        
        eventSource = new EventSource('/api/events');
        
        eventSource.onmessage = function(event) {
            try {
                const data = JSON.parse(event.data);
                
                if (data.type === 'player_join' && data.data && data.data.log_in) {
                    const playerName = data.data.log_in;
                    appendConsoleMessage(playerName + ' 加入游戏', 'success');
                }
                
                if (data.type === 'player_leave' && data.data && data.data.log_out) {
                    const playerName = data.data.log_out;
                    appendConsoleMessage(playerName + ' 退出游戏', 'warning');
                }
                
                if (data.type === 'chat_message' && data.data && data.data.player_info) {
                    const charName = data.data.player_info.Char_name;
                    const message = data.data.said;
                    appendConsoleMessage(charName + '：' + message, 'chat');
                }
                
                if (data.type === 'server_stats' && data.data) {
                    updateServerStats(data.data);
                }
                
                if (data.type === 'player_respawn' && data.data) {
                    const charName = data.data.respawn || data.data.char_name;
                    if (charName) {
                        appendConsoleMessage(charName + ' 已经重生', 'success');
                    }
                }
                
                if (data.type === 'new_player' && data.data) {
                    const charName = data.data.log_in || data.data.player_info?.Char_name;
                    if (charName) {
                        appendConsoleMessage('发现新玩家 ' + charName, 'success');
                    }
                }
                
                if (data.type === 'command_executed' && data.data) {
                    const playerName = data.data.player_name;
                    const commandName = data.data.command_name;
                    const ruleName = data.data.rule_name;
                    const success = data.data.success;
                    const statusText = success ? '✅' : '❌';
                    appendConsoleMessage(statusText + ' ' + ruleName + ': 对 ' + playerName + ' 执行命令 ' + commandName, success ? 'info' : 'error');
                }
                
                if (data.type === 'gold_changed' && data.data) {
                    const playerName = data.data.player_name;
                    const operationText = data.data.operation_text;
                    const amount = data.data.amount;
                    const oldGold = data.data.old_gold;
                    const newGold = data.data.new_gold;
                    const ruleName = data.data.rule_name;
                    appendConsoleMessage('💰 ' + ruleName + ': ' + playerName + ' 金额' + operationText + ' ' + amount + ' (' + oldGold + ' → ' + newGold + ')', 'gold');
                }
                
                if (data.type === 'tag_changed' && data.data) {
                    const playerName = data.data.player_name;
                    const oldTag = data.data.old_tag;
                    const newTag = data.data.new_tag;
                    const ruleName = data.data.rule_name;
                    appendConsoleMessage('🏷️ ' + ruleName + ': ' + playerName + ' 标签变更 (' + oldTag + ' → ' + newTag + ')', 'tag');
                }
            } catch (error) {
                console.error('解析SSE消息失败:', error);
            }
        };
        
        eventSource.onerror = function(error) {
            console.error('SSE连接错误:', error);
            eventSource.close();
            
            fetch('/api/events', { method: 'GET' })
                .then(response => {
                    if (response.status === 401) {
                        window.location.href = 'login.html';
                    } else {
                        setTimeout(() => {
                            startPlayerJoinMonitoring();
                        }, 5000);
                    }
                })
                .catch(() => {
                    setTimeout(() => {
                        startPlayerJoinMonitoring();
                    }, 5000);
                });
        };
    }
    
    if (!loginForm && consoleOutput) {
        const savedUsername = localStorage.getItem('rconUsername');
        if (!savedUsername) {
            window.location.href = 'login.html';
            return;
        }
        
        fetch('/api/verify-session')
            .then(response => response.json())
            .then(data => {
                if (!data.success || !data.username) {
                    localStorage.removeItem('rconUsername');
                    window.location.href = 'login.html';
                }
            })
            .catch(error => {
                console.error('验证登录状态失败:', error);
                localStorage.removeItem('rconUsername');
                window.location.href = 'login.html';
            });
        
        startPlayerJoinMonitoring();
        
        const autoExecuteBtn = document.getElementById('autoExecuteBtn');
        if (autoExecuteBtn) {
            autoExecuteBtn.addEventListener('click', function() {
                openAutoExecuteModal();
            });
        }
        
        const playerManageBtn = document.getElementById('playerManageBtn');
        if (playerManageBtn) {
            playerManageBtn.addEventListener('click', function() {
                openPlayerManageModal();
            });
        }
        
        const moreBtn = document.getElementById('moreBtn');
        if (moreBtn) {
            moreBtn.addEventListener('click', function() {
                const template = document.getElementById('moreModalTemplate');
                const modalClone = template.content.cloneNode(true);
                const modal = modalClone.querySelector('.modal');
                document.body.appendChild(modal);
                
                const closeBtn = modal.querySelector('.modal-close');
                if (closeBtn) {
                    closeBtn.addEventListener('click', function() {
                        modal.remove();
                    });
                }
                
                modal.addEventListener('click', function(e) {
                    if (e.target === modal) {
                        modal.remove();
                    }
                });
                
                const shopBtn = modal.querySelector('#shopBtn');
                if (shopBtn) {
                    shopBtn.addEventListener('click', function() {
                        window.open('/shop', '_blank');
                    });
                }
                
                const chatHistoryBtn = modal.querySelector('#chatHistoryBtn');
                if (chatHistoryBtn) {
                    chatHistoryBtn.addEventListener('click', function() {
                        modal.remove();
                        openChatHistoryModal();
                    });
                }
                
                const downloadPluginBtn = modal.querySelector('#downloadPluginBtn');
                if (downloadPluginBtn) {
                    downloadPluginBtn.addEventListener('click', function() {
                        const link = document.createElement('a');
                        link.href = '/plug-in/RCONDesktopClient.exe';
                        link.download = 'RCONDesktopClient.exe';
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                    });
                }
                
                const downloadAndroidAppBtn = modal.querySelector('#downloadAndroidAppBtn');
                if (downloadAndroidAppBtn) {
                    downloadAndroidAppBtn.addEventListener('click', function() {
                        const link = document.createElement('a');
                        link.href = '/plug-in/LFZKNGLQ.apk';
                        link.download = 'LFZKNGLQ.apk';
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                    });
                }
                
                const aiSettingsBtn = modal.querySelector('#aiSettingsBtn');
                if (aiSettingsBtn) {
                    aiSettingsBtn.addEventListener('click', function() {
                        modal.remove();
                        openAiSettingsModal();
                    });
                }
                
                const qqBotSettingsBtn = modal.querySelector('#qqBotSettingsBtn');
                if (qqBotSettingsBtn) {
                    qqBotSettingsBtn.addEventListener('click', function() {
                        modal.remove();
                        openQqBotSettingsModal();
                    });
                }
                
                const dataMigrationBtn = modal.querySelector('#dataMigrationBtn');
                if (dataMigrationBtn) {
                    dataMigrationBtn.addEventListener('click', function() {
                        modal.remove();
                        openDataMigrationModal();
                    });
                }
                
                const dataCleanupBtn = modal.querySelector('#dataCleanupBtn');
                if (dataCleanupBtn) {
                    dataCleanupBtn.addEventListener('click', function() {
                        modal.remove();
                        openDataCleanupModal();
                    });
                }
            });
        }
        
        function openDataCleanupModal() {
            const template = document.getElementById('dataCleanupModalTemplate');
            const modalClone = template.content.cloneNode(true);
            const modal = modalClone.querySelector('.modal');
            document.body.appendChild(modal);
            
            const closeBtn = modal.querySelector('.modal-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', function() {
                    modal.remove();
                });
            }
            
            modal.addEventListener('click', function(e) {
                if (e.target === modal) {
                    modal.remove();
                }
            });
            
            const executeBtn = modal.querySelector('#executeCleanupBtn');
            if (executeBtn) {
                executeBtn.addEventListener('click', function() {
                    const cleanupItems = [];
                    if (modal.querySelector('#cleanupAllPlayers').checked) {
                        cleanupItems.push('all_players');
                    }
                    if (modal.querySelector('#cleanupGold').checked) {
                        cleanupItems.push('gold');
                    }
                    if (modal.querySelector('#cleanupPermission').checked) {
                        cleanupItems.push('permission_level');
                    }
                    if (modal.querySelector('#cleanupSpawnPoint').checked) {
                        cleanupItems.push('spawn_point');
                    }
                    if (modal.querySelector('#cleanupMonthCard').checked) {
                        cleanupItems.push('month_card');
                    }
                    
                    if (cleanupItems.length === 0) {
                        alert('请至少选择一项要清理的数据');
                        return;
                    }
                    
                    let confirmMsg = '确定要清理以下数据吗？此操作不可恢复！\n\n';
                    if (cleanupItems.includes('all_players')) {
                        confirmMsg += '• 所有玩家数据\n';
                    }
                    if (cleanupItems.includes('gold')) {
                        confirmMsg += '• 所有玩家金额\n';
                    }
                    if (cleanupItems.includes('permission_level')) {
                        confirmMsg += '• 所有玩家权限标签\n';
                    }
                    if (cleanupItems.includes('spawn_point')) {
                        confirmMsg += '• 所有玩家复活点\n';
                    }
                    if (cleanupItems.includes('month_card')) {
                        confirmMsg += '• 所有玩家会员\n';
                    }
                    confirmMsg += '\n确定执行清理吗？';
                    
                    const confirmed = confirm(confirmMsg);
                    
                    if (!confirmed) {
                        return;
                    }
                    
                    executeBtn.disabled = true;
                    executeBtn.textContent = '清理中...';
                    
                    fetch('/api/cleanup/execute', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            items: cleanupItems
                        })
                    })
                    .then(response => response.json())
                    .then(result => {
                        executeBtn.disabled = false;
                        executeBtn.textContent = '🗑️ 执行清理';
                        
                        if (result.success) {
                            alert('清理成功！\n' + result.message);
                            modal.remove();
                        } else {
                            alert(result.message || '清理失败');
                        }
                    })
                    .catch(error => {
                        executeBtn.disabled = false;
                        executeBtn.textContent = '🗑️ 执行清理';
                        console.error('数据清理失败:', error);
                        alert('清理失败: ' + error.message);
                    });
                });
            }
        }
        
        function openDataMigrationModal() {
            const template = document.getElementById('dataMigrationModalTemplate');
            const modalClone = template.content.cloneNode(true);
            const modal = modalClone.querySelector('.modal');
            document.body.appendChild(modal);
            
            const closeBtn = modal.querySelector('.modal-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', function() {
                    modal.remove();
                });
            }
            
            modal.addEventListener('click', function(e) {
                if (e.target === modal) {
                    modal.remove();
                }
            });
            
            const verifyBtn = modal.querySelector('#verifyMigrationBtn');
            if (verifyBtn) {
                verifyBtn.addEventListener('click', function() {
                    const targetUsername = modal.querySelector('#migrationTargetUsername').value.trim();
                    const targetPassword = modal.querySelector('#migrationTargetPassword').value.trim();
                    
                    if (!targetUsername || !targetPassword) {
                        alert('请输入目标账号和密码');
                        return;
                    }
                    
                    verifyBtn.disabled = true;
                    verifyBtn.textContent = '验证中...';
                    
                    fetch('/api/migration/verify', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            target_username: targetUsername,
                            target_password: targetPassword
                        })
                    })
                    .then(response => response.json())
                    .then(result => {
                        verifyBtn.disabled = false;
                        verifyBtn.textContent = '🔐 验证账号';
                        
                        if (result.success) {
                            modal.querySelector('#currentUsername').textContent = result.current_username;
                            modal.querySelector('#verifiedTargetUsername').textContent = targetUsername;
                            modal.querySelector('#migrationStatus').style.display = 'block';
                            modal.dataset.targetUsername = targetUsername;
                        } else {
                            alert(result.message || '验证失败');
                        }
                    })
                    .catch(error => {
                        verifyBtn.disabled = false;
                        verifyBtn.textContent = '🔐 验证账号';
                        console.error('验证账号失败:', error);
                        alert('验证失败: ' + error.message);
                    });
                });
            }
            
            const executeBtn = modal.querySelector('#executeMigrationBtn');
            if (executeBtn) {
                executeBtn.addEventListener('click', function() {
                    const targetUsername = modal.dataset.targetUsername;
                    
                    if (!targetUsername) {
                        alert('请先验证目标账号');
                        return;
                    }
                    
                    const selectedTables = [];
                    if (modal.querySelector('#migrateAiService').checked) {
                        selectedTables.push('ai_service');
                    }
                    if (modal.querySelector('#migrateAutoTriggerRules').checked) {
                        selectedTables.push('auto_trigger_rules');
                    }
                    if (modal.querySelector('#migrateCommandsCategories').checked) {
                        selectedTables.push('commands');
                        selectedTables.push('categories');
                    }
                    if (modal.querySelector('#migratePlayers').checked) {
                        selectedTables.push('players');
                    }
                    if (modal.querySelector('#migrateProducts').checked) {
                        selectedTables.push('product_categories');
                        selectedTables.push('products');
                    }
                    if (modal.querySelector('#migrateChatMessages').checked) {
                        selectedTables.push('chat_messages');
                    }
                    
                    if (selectedTables.length === 0) {
                        alert('请至少选择一项要迁移的数据');
                        return;
                    }
                    
                    const confirmed = confirm('将会把该用户所选的数据替换到目标账号用户上，被替换的用户数据将被抹除替换前的所选数据！确定替换吗？');
                    
                    if (!confirmed) {
                        return;
                    }
                    
                    executeBtn.disabled = true;
                    executeBtn.textContent = '迁移中...';
                    
                    fetch('/api/migration/execute', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            target_username: targetUsername,
                            tables: selectedTables
                        })
                    })
                    .then(response => response.json())
                    .then(result => {
                        executeBtn.disabled = false;
                        executeBtn.textContent = '🔄 开始迁移';
                        
                        if (result.success) {
                            alert('数据迁移成功！\n' + result.message);
                            modal.remove();
                        } else {
                            alert(result.message || '迁移失败');
                        }
                    })
                    .catch(error => {
                        executeBtn.disabled = false;
                        executeBtn.textContent = '🔄 开始迁移';
                        console.error('数据迁移失败:', error);
                        alert('迁移失败: ' + error.message);
                    });
                });
            }
        }
        
        function openChatHistoryModal() {
            const template = document.getElementById('chatHistoryModalTemplate');
            const modalClone = template.content.cloneNode(true);
            const modal = modalClone.querySelector('.modal');
            document.body.appendChild(modal);
            
            const closeBtn = modal.querySelector('.modal-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', function() {
                    modal.remove();
                });
            }
            
            modal.addEventListener('click', function(e) {
                if (e.target === modal) {
                    modal.remove();
                }
            });
            
            modal.currentOffset = 0;
            modal.isLoading = false;
            modal.hasMore = true;
            
            loadChatMessages(modal);
            
            const modalBody = modal.querySelector('.modal-body');
            modalBody.addEventListener('scroll', function() {
                if (modalBody.scrollTop === 0 && modal.hasMore && !modal.isLoading) {
                    loadMoreChatMessages(modal);
                }
            });
        }
        
        function openQqBotSettingsModal() {
            const template = document.getElementById('qqBotSettingsModalTemplate');
            const modalClone = template.content.cloneNode(true);
            const modal = modalClone.querySelector('.modal');
            document.body.appendChild(modal);
            
            const closeBtn = modal.querySelector('.modal-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', function() {
                    modal.remove();
                });
            }
            
            modal.addEventListener('click', function(e) {
                if (e.target === modal) {
                    modal.remove();
                }
            });
            
            const tabBtns = modal.querySelectorAll('.qq-bot-tab-btn');
            tabBtns.forEach(btn => {
                btn.addEventListener('click', function() {
                    tabBtns.forEach(b => {
                        b.classList.remove('active');
                        b.style.background = 'rgba(255,255,255,0.1)';
                        b.style.color = '#b0bec5';
                        b.style.border = '1px solid #333';
                    });
                    this.classList.add('active');
                    this.style.background = 'linear-gradient(45deg, #12b7f5, #4fc3f7)';
                    this.style.color = '#fff';
                    this.style.border = 'none';
                    
                    const tabName = this.dataset.tab;
                    modal.querySelectorAll('.qq-bot-panel').forEach(panel => {
                        panel.style.display = 'none';
                    });
                    
                    const targetPanel = modal.querySelector('#qqBot' + tabName.charAt(0).toUpperCase() + tabName.slice(1) + 'Panel');
                    if (targetPanel) {
                        targetPanel.style.display = 'block';
                    }
                });
            });
            
            fetch('/api/qq-bot/settings')
                .then(response => response.json())
                .then(result => {
                    if (result.success && result.settings) {
                        const settings = result.settings;
                        modal.querySelector('#qqBotBindSuccessMsg').value = settings.Binding_message_1 || '';
                        modal.querySelector('#qqBotBindFailMsg').value = settings.Binding_message_2 || '';
                        modal.querySelector('#qqBotBindAlreadyMsg').value = settings.Binding_message_3 || '';
                        modal.querySelector('#qqBotCheckinSuccessMsg').value = settings.sign_message_1 || '';
                        modal.querySelector('#qqBotCheckinAlreadyMsg').value = settings.sign_message_2 || '';
                        modal.querySelector('#qqBotCheckinNotBoundMsg').value = settings.sign_message_3 || '';
                        modal.querySelector('#qqBotBalanceSuccessMsg').value = settings.balance_message_1 || '';
                        modal.querySelector('#qqBotBalanceNotBoundMsg').value = settings.balance_message_2 || '';
                        modal.querySelector('#qqBotOnlineHeaderMsg').value = settings.online_players_message || '';
                        
                        const resetType = settings.sign_reset_type || 'daily';
                        const resetHour = settings.sign_reset_hour || 0;
                        const intervalHours = settings.sign_interval_hours || 24;
                        
                        if (resetType === 'daily') {
                            modal.querySelector('#signResetTypeDaily').checked = true;
                            modal.querySelector('#dailyResetSettings').style.display = 'block';
                            modal.querySelector('#intervalResetSettings').style.display = 'none';
                        } else {
                            modal.querySelector('#signResetTypeInterval').checked = true;
                            modal.querySelector('#dailyResetSettings').style.display = 'none';
                            modal.querySelector('#intervalResetSettings').style.display = 'block';
                        }
                        modal.querySelector('#signResetHour').value = resetHour;
                        modal.querySelector('#signIntervalHours').value = intervalHours;
                        
                        const goldType = settings.sign_gold_type || 'fixed';
                        const goldFixed = settings.sign_gold_fixed || 100;
                        const goldMin = settings.sign_gold_min || 1;
                        const goldMax = settings.sign_gold_max || 50;
                        
                        if (goldType === 'fixed') {
                            modal.querySelector('#signGoldTypeFixed').checked = true;
                            modal.querySelector('#fixedGoldSettings').style.display = 'block';
                            modal.querySelector('#randomGoldSettings').style.display = 'none';
                        } else {
                            modal.querySelector('#signGoldTypeRandom').checked = true;
                            modal.querySelector('#fixedGoldSettings').style.display = 'none';
                            modal.querySelector('#randomGoldSettings').style.display = 'block';
                        }
                        modal.querySelector('#signGoldFixed').value = goldFixed;
                        modal.querySelector('#signGoldMin').value = goldMin;
                        modal.querySelector('#signGoldMax').value = goldMax;
                        modal.querySelector('#signGoldWeight').value = settings.sign_gold_weight || 1.0;
                    }
                })
                .catch(error => {
                    console.error('加载QQ机器人设置失败:', error);
                });
            
            const signResetTypeRadios = modal.querySelectorAll('input[name="signResetType"]');
            signResetTypeRadios.forEach(radio => {
                radio.addEventListener('change', function() {
                    if (this.value === 'daily') {
                        modal.querySelector('#dailyResetSettings').style.display = 'block';
                        modal.querySelector('#intervalResetSettings').style.display = 'none';
                    } else {
                        modal.querySelector('#dailyResetSettings').style.display = 'none';
                        modal.querySelector('#intervalResetSettings').style.display = 'block';
                    }
                });
            });
            
            const signGoldTypeRadios = modal.querySelectorAll('input[name="signGoldType"]');
            signGoldTypeRadios.forEach(radio => {
                radio.addEventListener('change', function() {
                    if (this.value === 'fixed') {
                        modal.querySelector('#fixedGoldSettings').style.display = 'block';
                        modal.querySelector('#randomGoldSettings').style.display = 'none';
                    } else {
                        modal.querySelector('#fixedGoldSettings').style.display = 'none';
                        modal.querySelector('#randomGoldSettings').style.display = 'block';
                    }
                });
            });
            
            const testRandomGoldBtn = modal.querySelector('#testRandomGoldBtn');
            if (testRandomGoldBtn) {
                testRandomGoldBtn.addEventListener('click', function() {
                    const minVal = parseInt(modal.querySelector('#signGoldMin').value) || 1;
                    const maxVal = parseInt(modal.querySelector('#signGoldMax').value) || 50;
                    const weight = parseFloat(modal.querySelector('#signGoldWeight').value) || 1.0;
                    
                    const result = weightedRandom(minVal, maxVal, weight);
                    const resultSpan = modal.querySelector('#testRandomGoldResult');
                    if (resultSpan) {
                        resultSpan.textContent = `获得 ${result} 金币`;
                        resultSpan.style.color = result > (minVal + maxVal) / 2 ? '#4caf50' : '#ffc107';
                    }
                });
            }
            
            function weightedRandom(minVal, maxVal, weight) {
                if (minVal >= maxVal) return minVal;
                
                const values = [];
                const weights = [];
                
                for (let val = minVal; val <= maxVal; val++) {
                    values.push(val);
                    const distance = val - minVal;
                    const w = 1.0 / (1.0 + distance * weight);
                    weights.push(w);
                }
                
                const totalWeight = weights.reduce((a, b) => a + b, 0);
                const randVal = Math.random() * totalWeight;
                
                let cumulative = 0;
                for (let i = 0; i < weights.length; i++) {
                    cumulative += weights[i];
                    if (randVal <= cumulative) {
                        return values[i];
                    }
                }
                
                return values[values.length - 1];
            }
            
            function saveQqBotSettings(settings) {
                fetch('/api/qq-bot/settings', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(settings)
                })
                .then(response => response.json())
                .then(result => {
                    if (result.success) {
                        alert('保存成功！');
                    } else {
                        alert(result.message || '保存失败');
                    }
                })
                .catch(error => {
                    console.error('保存QQ机器人设置失败:', error);
                    alert('保存失败: ' + error.message);
                });
            }
            
            const saveQqBotBindBtn = modal.querySelector('#saveQqBotBindBtn');
            if (saveQqBotBindBtn) {
                saveQqBotBindBtn.addEventListener('click', function() {
                    const settings = {
                        Binding_message_1: modal.querySelector('#qqBotBindSuccessMsg').value,
                        Binding_message_2: modal.querySelector('#qqBotBindFailMsg').value,
                        Binding_message_3: modal.querySelector('#qqBotBindAlreadyMsg').value
                    };
                    saveQqBotSettings(settings);
                });
            }
            
            const saveQqBotCheckinBtn = modal.querySelector('#saveQqBotCheckinBtn');
            if (saveQqBotCheckinBtn) {
                saveQqBotCheckinBtn.addEventListener('click', function() {
                    const resetType = modal.querySelector('input[name="signResetType"]:checked').value;
                    const goldType = modal.querySelector('input[name="signGoldType"]:checked').value;
                    const settings = {
                        sign_message_1: modal.querySelector('#qqBotCheckinSuccessMsg').value,
                        sign_message_2: modal.querySelector('#qqBotCheckinAlreadyMsg').value,
                        sign_message_3: modal.querySelector('#qqBotCheckinNotBoundMsg').value,
                        sign_reset_type: resetType,
                        sign_reset_hour: parseInt(modal.querySelector('#signResetHour').value) || 0,
                        sign_interval_hours: parseInt(modal.querySelector('#signIntervalHours').value) || 24,
                        sign_gold_type: goldType,
                        sign_gold_fixed: parseInt(modal.querySelector('#signGoldFixed').value) || 100,
                        sign_gold_min: parseInt(modal.querySelector('#signGoldMin').value) || 1,
                        sign_gold_max: parseInt(modal.querySelector('#signGoldMax').value) || 50,
                        sign_gold_weight: parseFloat(modal.querySelector('#signGoldWeight').value) || 1.0
                    };
                    saveQqBotSettings(settings);
                });
            }
            
            const saveQqBotBalanceBtn = modal.querySelector('#saveQqBotBalanceBtn');
            if (saveQqBotBalanceBtn) {
                saveQqBotBalanceBtn.addEventListener('click', function() {
                    const settings = {
                        balance_message_1: modal.querySelector('#qqBotBalanceSuccessMsg').value,
                        balance_message_2: modal.querySelector('#qqBotBalanceNotBoundMsg').value
                    };
                    saveQqBotSettings(settings);
                });
            }
            
            const saveQqBotOnlineBtn = modal.querySelector('#saveQqBotOnlineBtn');
            if (saveQqBotOnlineBtn) {
                saveQqBotOnlineBtn.addEventListener('click', function() {
                    const settings = {
                        online_players_message: modal.querySelector('#qqBotOnlineHeaderMsg').value
                    };
                    saveQqBotSettings(settings);
                });
            }
            
            const varToggleButtons = [
                { btn: 'insertBindSuccessVarBtn', list: 'bindSuccessVarList' },
                { btn: 'insertBindFailVarBtn', list: 'bindFailVarList' },
                { btn: 'insertBindAlreadyVarBtn', list: 'bindAlreadyVarList' },
                { btn: 'insertCheckinSuccessVarBtn', list: 'checkinSuccessVarList' },
                { btn: 'insertCheckinAlreadyVarBtn', list: 'checkinAlreadyVarList' },
                { btn: 'insertCheckinNotBoundVarBtn', list: 'checkinNotBoundVarList' },
                { btn: 'insertBalanceSuccessVarBtn', list: 'balanceSuccessVarList' },
                { btn: 'insertBalanceNotBoundVarBtn', list: 'balanceNotBoundVarList' },
                { btn: 'insertOnlineVarBtn', list: 'onlineVarList' }
            ];
            
            varToggleButtons.forEach(item => {
                const btn = modal.querySelector('#' + item.btn);
                const list = modal.querySelector('#' + item.list);
                if (btn && list) {
                    btn.addEventListener('click', function() {
                        list.style.display = list.style.display === 'none' ? 'block' : 'none';
                    });
                }
            });
            
            const variableBtns = modal.querySelectorAll('.qq-bot-variable-btn');
            variableBtns.forEach(btn => {
                btn.addEventListener('click', function() {
                    const targetId = this.dataset.target;
                    const variable = this.dataset.variable;
                    const textarea = modal.querySelector('#' + targetId);
                    if (textarea) {
                        const start = textarea.selectionStart;
                        const end = textarea.selectionEnd;
                        const text = textarea.value;
                        textarea.value = text.substring(0, start) + variable + text.substring(end);
                        textarea.selectionStart = textarea.selectionEnd = start + variable.length;
                        textarea.focus();
                    }
                });
            });
        }
        
        function openAiSettingsModal() {
            const template = document.getElementById('aiSettingsModalTemplate');
            const modalClone = template.content.cloneNode(true);
            const modal = modalClone.querySelector('.modal');
            document.body.appendChild(modal);
            
            const closeBtn = modal.querySelector('.modal-close');
            if (closeBtn) {
                closeBtn.addEventListener('click', function() {
                    modal.remove();
                });
            }
            
            let savedModelName = '';
            
            fetch('/api/ai-service')
                .then(response => response.json())
                .then(result => {
                    if (result.success && result.service) {
                        const service = result.service;
                        modal.querySelector('#aiModelUrl').value = service.url || '';
                        modal.querySelector('#aiApiKey').value = service.key || '';
                        savedModelName = service.name || '';
                        modal.querySelector('#aiSystemPrompt').value = service.Prompter || '';
                        modal.querySelector('#aiKeyword').value = service.keyword || '';
                        modal.querySelector('#aiEnabled').checked = service.enabled || false;
                        
                        const statusText = modal.querySelector('#aiStatusText');
                        if (statusText) {
                            if (service.enabled) {
                                statusText.textContent = '(已开启)';
                                statusText.style.color = '#4caf50';
                            } else {
                                statusText.textContent = '(未开启)';
                                statusText.style.color = '#f44336';
                            }
                        }
                        
                        if (savedModelName) {
                            const modelSelect = modal.querySelector('#aiModelId');
                            const option = document.createElement('option');
                            option.value = savedModelName;
                            option.textContent = savedModelName;
                            option.selected = true;
                            modelSelect.insertBefore(option, modelSelect.firstChild);
                        }
                    }
                })
                .catch(error => {
                    console.error('加载AI服务配置失败:', error);
                });
            
            const aiEnabledToggle = modal.querySelector('#aiEnabled');
            if (aiEnabledToggle) {
                aiEnabledToggle.addEventListener('change', function() {
                    const statusText = modal.querySelector('#aiStatusText');
                    if (statusText) {
                        if (this.checked) {
                            statusText.textContent = '(已开启)';
                            statusText.style.color = '#4caf50';
                        } else {
                            statusText.textContent = '(未开启)';
                            statusText.style.color = '#f44336';
                        }
                    }
                });
            }
            
            const fetchModelsBtn = modal.querySelector('#fetchModelsBtn');
            if (fetchModelsBtn) {
                fetchModelsBtn.addEventListener('click', function() {
                    const url = modal.querySelector('#aiModelUrl').value.trim();
                    const key = modal.querySelector('#aiApiKey').value.trim();
                    
                    if (!url || !key) {
                        alert('请先填写模型地址和密钥');
                        return;
                    }
                    
                    fetchModelsBtn.disabled = true;
                    fetchModelsBtn.textContent = '获取中...';
                    
                    fetch('/api/ai-service/models', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            url: url,
                            key: key
                        })
                    })
                    .then(response => response.json())
                    .then(result => {
                        fetchModelsBtn.disabled = false;
                        fetchModelsBtn.textContent = '🔄 获取模型';
                        
                        const manualInputDiv = modal.querySelector('#manualModelInput');
                        
                        if (result.success && result.models && result.models.length > 0) {
                            const modelSelect = modal.querySelector('#aiModelId');
                            modelSelect.innerHTML = '';
                            
                            result.models.forEach(model => {
                                const option = document.createElement('option');
                                option.value = model.id;
                                option.textContent = model.name || model.id;
                                if (model.id === savedModelName) {
                                    option.selected = true;
                                }
                                modelSelect.appendChild(option);
                            });
                            
                            manualInputDiv.style.display = 'none';
                            alert('成功获取 ' + result.models.length + ' 个模型');
                        } else {
                            const modelSelect = modal.querySelector('#aiModelId');
                            modelSelect.innerHTML = '<option value="">获取失败，请手动输入</option>';
                            manualInputDiv.style.display = 'block';
                            
                            if (savedModelName) {
                                modal.querySelector('#aiModelIdManual').value = savedModelName;
                            }
                            
                            alert('获取模型列表失败: ' + (result.message || '未知错误') + '\n已为您显示手动输入框，请输入模型ID');
                        }
                    })
                    .catch(error => {
                        fetchModelsBtn.disabled = false;
                        fetchModelsBtn.textContent = '🔄 获取模型';
                        console.error('获取模型列表失败:', error);
                        
                        const modelSelect = modal.querySelector('#aiModelId');
                        modelSelect.innerHTML = '<option value="">获取失败，请手动输入</option>';
                        
                        const manualInputDiv = modal.querySelector('#manualModelInput');
                        manualInputDiv.style.display = 'block';
                        
                        if (savedModelName) {
                            modal.querySelector('#aiModelIdManual').value = savedModelName;
                        }
                        
                        alert('获取模型列表失败: ' + error.message + '\n已为您显示手动输入框，请输入模型ID');
                    });
                });
            }
            
            const saveBtn = modal.querySelector('#saveAiSettingsBtn');
            if (saveBtn) {
                saveBtn.addEventListener('click', function() {
                    const url = modal.querySelector('#aiModelUrl').value.trim();
                    const key = modal.querySelector('#aiApiKey').value.trim();
                    
                    const manualInputDiv = modal.querySelector('#manualModelInput');
                    let name;
                    if (manualInputDiv.style.display === 'block') {
                        name = modal.querySelector('#aiModelIdManual').value.trim();
                    } else {
                        name = modal.querySelector('#aiModelId').value.trim();
                    }
                    
                    const Prompter = modal.querySelector('#aiSystemPrompt').value.trim();
                    const keyword = modal.querySelector('#aiKeyword').value.trim();
                    const enabled = modal.querySelector('#aiEnabled').checked;
                    
                    fetch('/api/ai-service/save', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            url: url,
                            key: key,
                            name: name,
                            Prompter: Prompter,
                            keyword: keyword,
                            enabled: enabled
                        })
                    })
                    .then(response => response.json())
                    .then(result => {
                        if (result.success) {
                            alert('AI服务配置保存成功');
                            modal.remove();
                        } else {
                            alert('保存失败: ' + (result.message || '未知错误'));
                        }
                    })
                    .catch(error => {
                        console.error('保存AI服务配置失败:', error);
                        alert('保存失败: ' + error.message);
                    });
                });
            }
        }

        
        function loadChatMessages(modal) {
            const container = modal.querySelector('#chatMessagesContainer');
            
            fetch('/api/chat-messages')
                .then(response => response.json())
                .then(result => {
                    if (result.success && result.messages) {
                        modal.total = result.total;
                        modal.hasMore = result.has_more;
                        modal.currentOffset = result.offset + result.limit;
                        
                        if (result.messages.length === 0) {
                            container.innerHTML = '<div style="color: #b0bec5; font-style: italic; text-align: center; padding: 20px;">暂无聊天记录</div>';
                            return;
                        }
                        
                        let html = '';
                        result.messages.slice().reverse().forEach(msg => {
                            const date = new Date(msg.timestamp * 1000);
                            const dateStr = date.toLocaleString('zh-CN', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                second: '2-digit'
                            });
                            
                            html += `
                                <div style="background: rgba(15, 52, 96, 0.3); border: 1px solid #0f3460; border-radius: 8px; padding: 12px; margin-bottom: 10px;">
                                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                                        <span style="color: #e94560; font-weight: bold;">${msg.char_name || '未知玩家'}</span>
                                        <span style="color: #b0bec5; font-size: 12px;">${dateStr}</span>
                                    </div>
                                    <div style="color: #e0e0e0; word-wrap: break-word;">${msg.message}</div>
                                </div>
                            `;
                        });
                        
                        container.innerHTML = html;
                        
                        setTimeout(() => {
                            const modalBody = modal.querySelector('.modal-body');
                            modalBody.scrollTop = modalBody.scrollHeight;
                        }, 100);
                    } else {
                        container.innerHTML = '<div style="color: #ef5350; text-align: center; padding: 20px;">加载失败</div>';
                    }
                })
                .catch(error => {
                    console.error('加载聊天记录失败:', error);
                    container.innerHTML = '<div style="color: #ef5350; text-align: center; padding: 20px;">加载失败: ' + error.message + '</div>';
                });
        }
        
        function loadMoreChatMessages(modal) {
            modal.isLoading = true;
            const container = modal.querySelector('#chatMessagesContainer');
            
            const loadingDiv = document.createElement('div');
            loadingDiv.id = 'chatLoading';
            loadingDiv.style.cssText = 'text-align: center; padding: 20px; color: #b0bec5;';
            loadingDiv.textContent = '加载中...';
            container.insertBefore(loadingDiv, container.firstChild);
            
            fetch(`/api/chat-messages?offset=${modal.currentOffset}&limit=10`)
                .then(response => response.json())
                .then(result => {
                    if (result.success && result.messages) {
                        modal.hasMore = result.has_more;
                        modal.currentOffset = result.offset + result.limit;
                        
                        loadingDiv.remove();
                        
                        if (result.messages.length === 0) {
                            return;
                        }
                        
                        let html = '';
                        result.messages.slice().reverse().forEach(msg => {
                            const date = new Date(msg.timestamp * 1000);
                            const dateStr = date.toLocaleString('zh-CN', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                second: '2-digit'
                            });
                            
                            html += `
                                <div style="background: rgba(15, 52, 96, 0.3); border: 1px solid #0f3460; border-radius: 8px; padding: 12px; margin-bottom: 10px;">
                                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                                        <span style="color: #e94560; font-weight: bold;">${msg.char_name || '未知玩家'}</span>
                                        <span style="color: #b0bec5; font-size: 12px;">${dateStr}</span>
                                    </div>
                                    <div style="color: #e0e0e0; word-wrap: break-word;">${msg.message}</div>
                                </div>
                            `;
                        });
                        
                        const tempDiv = document.createElement('div');
                        tempDiv.innerHTML = html;
                        
                        while (tempDiv.firstChild) {
                            container.insertBefore(tempDiv.firstChild, container.firstChild);
                        }
                    } else {
                        loadingDiv.textContent = '加载失败';
                    }
                })
                .catch(error => {
                    console.error('加载更多聊天记录失败:', error);
                    loadingDiv.textContent = '加载失败: ' + error.message;
                })
                .finally(() => {
                    modal.isLoading = false;
                });
        }
        
        const shopBtn = document.getElementById('shopBtn');
        if (shopBtn) {
            shopBtn.addEventListener('click', function() {
                window.open('/shop', '_blank');
            });
        }
        
        fetch('/api/rcon/connection-info', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        })
        .then(response => {
            if (response.status === 401) {
                window.location.href = 'login.html';
                throw new Error('Unauthorized');
            }
            return response.json();
        })
        .then(data => {
            if (data.success && data.connection_info) {
                hostInput.value = data.connection_info.host;
                rconPasswordInput.value = data.connection_info.password;
                portInput.value = data.connection_info.port;
                
                const rconModeToggle = document.getElementById('rconModeToggle');
                if (rconModeToggle) {
                    rconModeToggle.checked = (data.connection_info.rcon_mode === 'sse');
                }
                
                if (data.connection_info.rcon_mode === 'sse') {
                    commandInput.disabled = false;
                    sendBtn.disabled = false;
                    appendConsoleMessage('无公网模式已启用，通过插件发送命令，适用于局域网，无需连接到rcon，拥有公网的用户为了稳定性请不要开启，确保已经在游戏服务端已打开登录插件且开启rcon服务。', 'warning');
                } else if (data.connection_info.host && data.connection_info.password) {
                    fetch('/api/rcon/connect', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            host: data.connection_info.host,
                            password: data.connection_info.password,
                            port: data.connection_info.port,
                            rcon_mode: data.connection_info.rcon_mode || 'direct',
                            saveConnection: true
                        })
                    })
                    .then(response => response.json())
                    .then(data => {
                        if (data.success) {
                            appendConsoleMessage(data.message, 'success');
                            statusDot.className = 'status-dot connected';
                            statusText.textContent = '已连接';
                            commandInput.disabled = false;
                            sendBtn.disabled = false;
                        } else {
                            appendConsoleMessage(data.message, 'error');
                        }
                    })
                    .catch(error => {
                        appendConsoleMessage('自动重连失败: ' + error.message, 'error');
                    });
                }
            }
        })
        .catch(error => {
            if (error.message !== 'Unauthorized') {
                console.error('Error getting connection info:', error);
            }
        });
    }
    
    const connectBtn = document.getElementById('connectBtn');
    if (connectBtn) {
        connectBtn.addEventListener('click', function() {
            const host = hostInput.value.trim();
            const password = rconPasswordInput.value.trim();
            const port = portInput.value.trim();
            const saveConnection = saveConnectionCheckbox ? saveConnectionCheckbox.checked : false;
            const rconModeToggle = document.getElementById('rconModeToggle');
            const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
            
            if (!host || !password || !port) {
                appendConsoleMessage('请填写所有连接信息', 'error');
                return;
            }
            
            appendConsoleMessage('正在连接...', 'info');
            connectBtn.disabled = true;
            
            fetch('/api/rcon/connect', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    host: host,
                    password: password,
                    port: port,
                    rcon_mode: rconMode,
                    saveConnection: saveConnection
                })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    appendConsoleMessage(data.message, 'success');
                    statusDot.className = 'status-dot connected';
                    statusText.textContent = '已连接';
                    commandInput.disabled = false;
                    sendBtn.disabled = false;
                } else {
                    appendConsoleMessage(data.message, 'error');
                }
            })
            .catch(error => {
                appendConsoleMessage('连接请求失败: ' + error.message, 'error');
            })
            .finally(() => {
                connectBtn.disabled = false;
            });
        });
    }
    
    const clearBtn = document.querySelector('.btn-clear');
    if (clearBtn) {
        clearBtn.addEventListener('click', function() {
            if (consoleOutput) {
                consoleOutput.innerHTML = '';
                appendConsoleMessage('控制台已清空', 'info');
            }
        });
    }
    
    const onlinePlayersBtn = document.querySelector('.btn-online-players');
    if (onlinePlayersBtn) {
        onlinePlayersBtn.addEventListener('click', function() {
            appendConsoleMessage('发送命令: listplayers', 'info');
            onlinePlayersBtn.disabled = true;
            
            const rconModeToggle = document.getElementById('rconModeToggle');
            const rconMode = rconModeToggle?.checked ? 'sse' : 'direct';
            
            const apiUrl = rconMode === 'sse' ? '/api/rcon/send-via-sse' : '/api/rcon/send';
            
            fetch(apiUrl, {
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
                if (data.success) {
                    const attempts = data.attempts || 1;
                    if (attempts > 1) {
                        appendConsoleMessage('命令响应: ' + data.response + ' (重试' + (attempts - 1) + '次后成功)', 'success');
                    } else {
                        appendConsoleMessage('命令响应: ' + data.response, 'success');
                    }
                } else {
                    const attempts = data.attempts || 1;
                    if (attempts > 1) {
                        appendConsoleMessage('命令执行失败（已重试' + (attempts - 1) + '次）: ' + data.message, 'error');
                    } else {
                        appendConsoleMessage('命令执行失败: ' + data.message, 'error');
                    }
                }
            })
            .catch(error => {
                appendConsoleMessage('发送命令请求失败: ' + error.message, 'error');
            })
            .finally(() => {
                onlinePlayersBtn.disabled = false;
            });
        });
    }
    
    if (sendBtn) {
        sendBtn.addEventListener('click', function() {
            const command = commandInput.value.trim();
            
            if (!command) {
                appendConsoleMessage('请输入命令', 'error');
                return;
            }
            
            appendConsoleMessage('发送命令: ' + command, 'info');
            sendBtn.disabled = true;
            
            commandInput.value = '';
            
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
                    const attempts = data.attempts || 1;
                    if (attempts > 1) {
                        appendConsoleMessage('命令响应: ' + data.response + ' (重试' + (attempts - 1) + '次后成功)', 'success');
                    } else {
                        appendConsoleMessage('命令响应: ' + data.response, 'success');
                    }
                } else {
                    const attempts = data.attempts || 1;
                    if (attempts > 1) {
                        appendConsoleMessage('命令执行失败（已重试' + (attempts - 1) + '次）: ' + data.message, 'error');
                    } else {
                        appendConsoleMessage('命令执行失败: ' + data.message, 'error');
                    }
                }
            })
            .catch(error => {
                appendConsoleMessage('发送命令请求失败: ' + error.message, 'error');
            })
            .finally(() => {
                sendBtn.disabled = false;
            });
        });
    }
    
    if (commandInput) {
        commandInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                sendBtn.click();
            }
        });
    }
    
    const rconModeToggle = document.getElementById('rconModeToggle');
    if (rconModeToggle) {
        rconModeToggle.addEventListener('change', function() {
            const rconMode = this.checked ? 'sse' : 'direct';
            
            if (rconMode === 'sse') {
                commandInput.disabled = false;
                sendBtn.disabled = false;
                appendConsoleMessage('无公网模式已启用，通过插件发送命令，适用于局域网，无需登录连接到rcon，拥有公网的用户为了稳定性请不要开启，确保已经在游戏服务端已打开登录插件且开启rcon服务。', 'warning');
            } else {
                if (statusText.textContent !== '已连接') {
                    commandInput.disabled = true;
                    sendBtn.disabled = true;
                    appendConsoleMessage('请先连接RCON服务器', 'warning');
                }
            }
            
            fetch('/api/rcon/save-mode', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    rcon_mode: rconMode
                })
            })
            .then(response => response.json())
            .then(data => {
            })
            .catch(error => {
                console.error('保存RCON模式请求失败:', error);
            });
        });
    }
    
    const quickCommands = document.querySelector('.quick-commands');
    const commandsList = document.getElementById('commandsList');
    const categoryFilter = document.getElementById('categoryFilter');
    const editCategoriesBtn = document.getElementById('editCategoriesBtn');
    const editCommandsBtn = document.getElementById('editCommandsBtn');
    
    if (quickCommands && commandsList) {
        loadCategories();
        loadCommands();
        
        if (categoryFilter) {
            categoryFilter.addEventListener('change', function() {
                loadCommands(this.value);
            });
        }
        
        if (editCategoriesBtn) {
            editCategoriesBtn.addEventListener('click', function() {
                openCategoriesModal();
            });
        }
        
        if (editCommandsBtn) {
            editCommandsBtn.addEventListener('click', function() {
                openCommandsModal();
            });
        }
    }
    
    function loadCategories() {
        fetch('/api/categories')
            .then(response => {
                if (response.status === 401) {
                    window.location.href = 'login.html';
                    throw new Error('Unauthorized');
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    updateCategoryFilter(data.categories);
                }
            })
            .catch(error => {
                console.error('Error loading categories:', error);
            });
    }
    
    function updateCategoryFilter(categories) {
        if (!categoryFilter) return;
        
        const currentValue = categoryFilter.value;
        
        categoryFilter.innerHTML = '<option value="all">全部命令</option>';
        
        categories.forEach(category => {
            const option = document.createElement('option');
            option.value = category.name;
            option.textContent = category.name;
            categoryFilter.appendChild(option);
        });
        
        categoryFilter.value = currentValue;
    }
    
    function loadCommands(category = 'all') {
        let url = '/api/commands';
        if (category !== 'all') {
            url = `/api/commands/category/${encodeURIComponent(category)}`;
        }
        
        fetch(url)
            .then(response => {
                if (response.status === 401) {
                    window.location.href = 'login.html';
                    throw new Error('Unauthorized');
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    renderCommands(data.commands);
                }
            })
            .catch(error => {
                console.error('Error loading commands:', error);
                renderError('加载命令失败，请刷新页面重试...');
            });
    }
    
    function renderError(message) {
        const errorHtml = `<p style="color: #ef5350; text-align: center; padding: 20px;">${message}</p>`;
        appendCommandsHtml(errorHtml);
    }
    
    function renderCommands(commands) {
        if (!commandsList) return;
        
        if (commands.length === 0) {
            const emptyHtml = '<p style="color: #b0bec5; text-align: center; padding: 20px;">暂无命令，请添加命令...</p>';
            appendCommandsHtml(emptyHtml);
            return;
        }
        
        let html = '';
        
        commands.forEach(cmd => {
            html += `<div class="command-item" data-example="${cmd.example}" title="${cmd.description}">
                        <div class="command-name">${cmd.name}</div>
                        <div class="command-desc">${cmd.description}</div>
                    </div>`;
        });
        
        appendCommandsHtml(html);
        
        commandsList.querySelectorAll('.command-item').forEach(item => {
            item.addEventListener('click', function() {
                const example = this.getAttribute('data-example');
                if (commandInput) {
                    commandInput.value = example;
                    commandInput.focus();
                }
            });
        });
    }
    
    function appendCommandsHtml(html) {
        if (!commandsList) return;
        
        commandsList.innerHTML = html;
    }
    
    function openCategoriesModal() {
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 700px; max-height: 90vh; display: flex; flex-direction: column;">
                <div class="modal-header">
                    <h3>📂 分类管理</h3>
                    <button class="modal-close">&times;</button>
                </div>
                <div class="modal-body" style="flex: 1; overflow-y: auto; padding: 20px;">
                    <div class="modal-section" style="margin-bottom: 20px;">
                        <h4>➕ 添加新分类</h4>
                        <div class="form-group">
                            <label for="newCategoryName">分类名称：</label>
                            <input type="text" id="newCategoryName" placeholder="输入分类名称" class="form-control">
                        </div>
                        <div class="form-group">
                            <label for="newCategoryDesc">分类描述：</label>
                            <textarea id="newCategoryDesc" placeholder="输入分类描述（可选）" class="form-control" rows="2"></textarea>
                        </div>
                        <div class="modal-actions" style="margin-top: 10px;">
                            <button id="addCategoryBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff;">💾 添加分类</button>
                        </div>
                    </div>
                    <div class="modal-section">
                        <h4>📋 现有分类</h4>
                        <div id="categoriesList"></div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        loadCategoriesList(modal);
        
        const addCategoryBtn = modal.querySelector('#addCategoryBtn');
        addCategoryBtn.addEventListener('click', function() {
            const name = modal.querySelector('#newCategoryName').value.trim();
            const description = modal.querySelector('#newCategoryDesc').value.trim();
            
            if (!name) {
                alert('请输入分类名称');
                return;
            }
            
            fetch('/api/categories/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name, description })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('分类添加成功');
                    modal.querySelector('#newCategoryName').value = '';
                    modal.querySelector('#newCategoryDesc').value = '';
                    loadCategoriesList(modal);
                    loadCategories();
                    loadCommands(categoryFilter.value);
                } else {
                    alert('添加失败：' + data.message);
                }
            })
            .catch(error => {
                console.error('Error adding category:', error);
                alert('添加失败，请稍后重试');
            });
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
    
    function loadCategoriesList(modal) {
        const categoriesList = modal.querySelector('#categoriesList');
        
        fetch('/api/categories')
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    let html = '';
                    data.categories.forEach(category => {
                        html += `
                            <div class="category-item" style="background: linear-gradient(145deg, #1a1a1a, #101010); border: 1px solid #6a1b9a; border-radius: 8px; padding: 15px; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; gap: 15px;">
                                <div class="category-info" style="flex: 1; min-width: 0;">
                                    <strong style="color: #ce93d8; display: block; margin-bottom: 4px;">${category.name}</strong>
                                    <p style="color: #b0bec5; margin: 0; font-size: 13px;">${category.description || '无描述'}</p>
                                </div>
                                <div class="category-actions" style="display: flex; gap: 8px; flex-shrink: 0;">
                                    <button class="btn btn-sm btn-edit" style="background: linear-gradient(45deg, #2196f3, #42a5f5); color: #fff; padding: 6px 12px; font-size: 12px; border: none; border-radius: 4px; cursor: pointer;" onclick="editCategory(${category.id}, '${category.name}', '${category.description || ''}')">✏️ 编辑</button>
                                    <button class="btn btn-sm btn-delete" style="background: linear-gradient(45deg, #f44336, #e57373); color: #fff; padding: 6px 12px; font-size: 12px; border: none; border-radius: 4px; cursor: pointer;" onclick="deleteCategory(${category.id})">🗑️ 删除</button>
                                </div>
                            </div>
                        `;
                    });
                    categoriesList.innerHTML = html || '<p style="color: #b0bec5; text-align: center; padding: 20px;">暂无分类</p>';
                }
            })
            .catch(error => {
                console.error('Error loading categories list:', error);
                categoriesList.innerHTML = '<p style="color: #ef5350;">加载分类失败</p>';
            });
    }
    
    window.editCategory = function(id, name, description) {
        const editModal = document.createElement('div');
        editModal.className = 'modal';
        editModal.innerHTML = `
            <div class="modal-content" style="max-width: 500px;">
                <div class="modal-header">
                    <h3>✏️ 编辑分类</h3>
                    <button class="modal-close">&times;</button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="editCategoryName">分类名称：</label>
                        <input type="text" id="editCategoryName" value="${name}" class="form-control">
                    </div>
                    <div class="form-group">
                        <label for="editCategoryDesc">分类描述：</label>
                        <textarea id="editCategoryDesc" class="form-control" rows="2">${description || ''}</textarea>
                    </div>
                    <div class="modal-actions" style="margin-top: 15px;">
                        <button id="saveCategoryBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff;">💾 保存修改</button>
                        <button id="cancelEditBtn" class="btn btn-secondary">取消</button>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(editModal);
        
        editModal.querySelector('#saveCategoryBtn').addEventListener('click', function() {
            const newName = editModal.querySelector('#editCategoryName').value.trim();
            const newDesc = editModal.querySelector('#editCategoryDesc').value.trim();
            
            if (!newName) {
                alert('分类名称不能为空');
                return;
            }
            
            fetch('/api/categories/update', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id, name: newName, description: newDesc })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('分类更新成功');
                    loadCategories();
                    loadCommands(categoryFilter.value);
                    document.querySelectorAll('.modal').forEach(modal => {
                        if (modal.querySelector('#categoriesList')) {
                            loadCategoriesList(modal);
                        }
                        if (modal.querySelector('#commandsList')) {
                            loadCommandsList(modal);
                        }
                    });
                    editModal.remove();
                } else {
                    alert('更新失败：' + data.message);
                }
            })
            .catch(error => {
                console.error('Error updating category:', error);
                alert('更新失败，请稍后重试');
            });
        });
        
        editModal.querySelector('#cancelEditBtn').addEventListener('click', function() {
            editModal.remove();
        });
        
        editModal.querySelector('.modal-close').addEventListener('click', function() {
            editModal.remove();
        });
    };
    
    window.deleteCategory = function(id) {
        if (!confirm('确定要删除这个分类吗？此操作会同时删除该分类下的所有命令！')) {
            return;
        }
        
        fetch('/api/categories/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ id })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('分类删除成功');
                loadCategories();
                loadCommands(categoryFilter.value);
                document.querySelectorAll('.modal').forEach(modal => {
                    if (modal.querySelector('#categoriesList')) {
                        loadCategoriesList(modal);
                    }
                });
            } else {
                alert('删除失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error deleting category:', error);
            alert('删除失败，请稍后重试');
        });
    };
    
    function openCommandsModal() {
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 900px; max-height: 90vh; display: flex; flex-direction: column;">
                <div class="modal-header">
                    <h3>📝 命令管理</h3>
                    <button class="modal-close">&times;</button>
                </div>
                <div class="modal-body" style="flex: 1; overflow-y: auto; padding: 20px;">
                    <div class="modal-section" style="margin-bottom: 20px;">
                        <h4>➕ 添加新命令</h4>
                        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                            <div class="form-group">
                                <label for="newCommandName">命令名称：</label>
                                <input type="text" id="newCommandName" placeholder="输入命令名称" class="form-control">
                            </div>
                            <div class="form-group">
                                <label for="newCommandCategory">所属分类：</label>
                                <select id="newCommandCategory" class="form-control">
                                    <option value="">选择分类</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-group">
                            <label for="newCommandDesc">命令描述：</label>
                            <textarea id="newCommandDesc" placeholder="输入命令描述（可选）" class="form-control" rows="2"></textarea>
                        </div>
                        <div class="form-group">
                            <label for="newCommandExample">命令示例：</label>
                            <div style="display: flex; gap: 10px; align-items: center;">
                                <input type="text" id="newCommandExample" placeholder="输入命令示例" class="form-control" style="flex: 1;">
                                <button id="insertVariableBtn" class="btn btn-secondary" title="插入变量" style="padding: 8px 12px; white-space: nowrap;">📌 变量</button>
                            </div>
                        </div>
                        <div class="form-group">
                            <div id="variableList" style="display: none; background: rgba(156, 39, 176, 0.1); border: 1px solid #6a1b9a; border-radius: 8px; padding: 15px; margin-top: 5px;">
                                <div style="margin-bottom: 10px; font-weight: bold; color: #ce93d8;">可用的变量，点击自动插入：</div>
                                <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px;">
                                    <button class="variable-btn" data-variable="@复活点" style="padding: 8px 14px; background: linear-gradient(45deg, #2196f3, #42a5f5); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">📍 复活点坐标</button>
                                    <button class="variable-btn" data-variable="@角色名" style="padding: 8px 14px; background: linear-gradient(45deg, #ff9800, #ffb74d); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">👤 角色名</button>
                                    <button class="variable-btn" data-variable="@同部落角色名" style="padding: 8px 14px; background: linear-gradient(45deg, #9c27b0, #ab47bc); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">👥 同部落角色名</button>
                                    <button class="variable-btn" data-variable="@玩家等级" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">📊 玩家等级</button>
                                    <button class="variable-btn" data-variable="@玩家力量" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">💪 玩家力量</button>
                                    <button class="variable-btn" data-variable="@玩家灵活" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">🏃 玩家灵活</button>
                                    <button class="variable-btn" data-variable="@玩家活力" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">❤️ 玩家活力</button>
                                    <button class="variable-btn" data-variable="@玩家毅力" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">⚡ 玩家毅力</button>
                                    <button class="variable-btn" data-variable="@玩家权威" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">👑 玩家权威</button>
                                    <button class="variable-btn" data-variable="@玩家专长" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">🎒 玩家专长</button>
                                    <button class="variable-btn" data-variable="@玩家当前坐标" style="padding: 8px 14px; background: linear-gradient(45deg, #00bcd4, #4dd0e1); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">📍 玩家当前坐标</button>
                                </div>
                                <div style="font-size: 12px; color: #b0bec5; line-height: 1.6;">
                                    <div style="margin-bottom: 8px; padding: 10px; background: rgba(33, 150, 243, 0.1); border-left: 3px solid #2196f3; border-radius: 4px;">
                                        <strong style="color: #64b5f6;">@复活点坐标</strong>：游戏中必须要有床，且首次放置床铺后需要自杀一次才能激活，构建传送到复活点命令示例："con 索引 TeleportPlayer @复活点坐标"
                                    </div>
                                    <div style="margin-bottom: 8px; padding: 10px; background: rgba(255, 152, 0, 0.1); border-left: 3px solid #ff9800; border-radius: 4px;">
                                        <strong style="color: #ffb74d;">@角色名</strong>：自动触发命令如果关键词设置开头为"cs",那么游戏中必须输入"cs@角色名"只有@存在才能让本系统获取角色名，你也可以设置关键词开头为@，这样游戏中直接输入"@角色名"就能直接触发命令了，构建传送到玩家身边的命令示例："con 索引 TeleportPlayer @角色名"，观看玩家命令示例："con 索引 ViewPlayer @角色名"
                                    </div>
                                    <div style="margin-bottom: 8px; padding: 10px; background: rgba(156, 39, 176, 0.1); border-left: 3px solid #9c27b0; border-radius: 4px;">
                                        <strong style="color: #ce93d8;">@同部落角色名</strong>：对于@角色名不同的是这个变量只对同部落的玩家起作用，适合PVP模式，但是注意的是两个人都必须要有床且同在一个部落，构建传送到同部落玩家身边的命令示例："con 索引 TeleportPlayer @同部落角色名"
                                    </div>
                                    <div style="margin-bottom: 8px; padding: 10px; background: rgba(76, 175, 80, 0.1); border-left: 3px solid #4caf50; border-radius: 4px;">
                                        <strong style="color: #81c784;">@玩家等级 / @玩家力量 / @玩家灵活 / @玩家活力 / @玩家毅力 / @玩家权威 / @玩家专长</strong>：获取玩家的属性数值，支持数学运算（+、-、*、/），例如："con 索引 setstat AttributeMight @玩家力量+1" 如果@玩家力量是20，+1则是21
                                    </div>
                                    <div style="margin-bottom: 8px; padding: 10px; background: rgba(0, 188, 212, 0.1); border-left: 3px solid #00bcd4; border-radius: 4px;">
                                        <strong style="color: #4dd0e1;">@玩家当前坐标</strong>：获取玩家的当前坐标（X Y Z格式），构建传送命令示例："con 索引 TeleportPlayer @玩家当前坐标"
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-actions" style="margin-top: 10px;">
                            <button id="addCommandBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff;">💾 添加命令</button>
                        </div>
                    </div>
                    <div class="modal-section">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                            <h4 style="margin: 0;">📋 现有命令</h4>
                            <div style="display: flex; align-items: center; gap: 10px;">
                                <label for="commandFilterCategory" style="margin: 0; color: #b0bec5;">筛选：</label>
                                <select id="commandFilterCategory" class="form-control" style="width: 150px;">
                                    <option value="all">全部分类</option>
                                </select>
                            </div>
                        </div>
                        <div id="commandsList"></div>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        fetch('/api/categories')
            .then(response => response.json())
            .then(data => {
                const newCommandCategory = modal.querySelector('#newCommandCategory');
                const commandFilterCategory = modal.querySelector('#commandFilterCategory');
                
                data.categories.forEach(cat => {
                    const option1 = document.createElement('option');
                    option1.value = cat.name;
                    option1.textContent = cat.name;
                    newCommandCategory.appendChild(option1);
                    
                    const option2 = document.createElement('option');
                    option2.value = cat.name;
                    option2.textContent = cat.name;
                    commandFilterCategory.appendChild(option2);
                });
            });
        
        loadCommandsList(modal);
        
        modal.querySelector('#commandFilterCategory').addEventListener('change', function() {
            loadCommandsList(modal, this.value);
        });
        
        modal.querySelector('#addCommandBtn').addEventListener('click', function() {
            const name = modal.querySelector('#newCommandName').value.trim();
            const category = modal.querySelector('#newCommandCategory').value;
            const description = modal.querySelector('#newCommandDesc').value.trim();
            const example = modal.querySelector('#newCommandExample').value.trim();
            
            if (!name || !category || !example) {
                alert('请填写命令名称、分类和示例');
                return;
            }
            
            fetch('/api/commands/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name, description, category, example })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('命令添加成功');
                    modal.querySelector('#newCommandName').value = '';
                    modal.querySelector('#newCommandDesc').value = '';
                    modal.querySelector('#newCommandExample').value = '';
                    loadCommandsList(modal, modal.querySelector('#commandFilterCategory').value);
                    loadCommands(categoryFilter.value);
                } else {
                    alert('添加失败：' + data.message);
                }
            })
            .catch(error => {
                console.error('Error adding command:', error);
                alert('添加失败，请稍后重试');
            });
        });
        
        const insertVariableBtn = modal.querySelector('#insertVariableBtn');
        const variableList = modal.querySelector('#variableList');
        
        if (insertVariableBtn && variableList) {
            insertVariableBtn.addEventListener('click', function() {
                variableList.style.display = variableList.style.display === 'none' ? 'block' : 'none';
            });
        }
        
        const variableBtns = modal.querySelectorAll('.variable-btn');
        variableBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                const variable = this.getAttribute('data-variable');
                const exampleInput = modal.querySelector('#newCommandExample');
                const cursorPosition = exampleInput.selectionStart;
                const currentValue = exampleInput.value;
                const newValue = currentValue.slice(0, cursorPosition) + variable + currentValue.slice(cursorPosition);
                exampleInput.value = newValue;
                exampleInput.focus();
                exampleInput.setSelectionRange(cursorPosition + variable.length, cursorPosition + variable.length);
                variableList.style.display = 'none';
            });
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
    
    function loadCommandsList(modal, category = 'all') {
        const commandsList = modal.querySelector('#commandsList');
        
        let url = '/api/commands';
        if (category !== 'all') {
            url = `/api/commands/category/${encodeURIComponent(category)}`;
        }
        
        fetch(url)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    let html = '';
                    data.commands.forEach(cmd => {
                        html += `
                            <div class="command-item-modal" style="background: linear-gradient(145deg, #1a1a1a, #101010); border: 1px solid #6a1b9a; border-radius: 8px; padding: 15px; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: flex-start; gap: 15px;">
                                <div class="command-info" style="flex: 1; min-width: 0;">
                                    <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px;">
                                        <strong style="color: #ce93d8;">${cmd.name}</strong>
                                        <span style="background: rgba(156, 39, 176, 0.3); color: #ce93d8; padding: 2px 8px; border-radius: 4px; font-size: 12px;">${cmd.category}</span>
                                    </div>
                                    <p style="color: #b0bec5; margin: 0 0 8px 0; font-size: 13px;">${cmd.description || '无描述'}</p>
                                    <code style="background: rgba(0,0,0,0.3); padding: 4px 8px; border-radius: 4px; font-size: 12px; color: #80cbc4; display: block; word-break: break-all;">${cmd.example}</code>
                                </div>
                                <div class="command-actions" style="display: flex; flex-direction: column; gap: 6px; flex-shrink: 0;">
                                    <button class="btn btn-sm btn-edit" style="background: linear-gradient(45deg, #2196f3, #42a5f5); color: #fff; padding: 6px 12px; font-size: 12px; border: none; border-radius: 4px; cursor: pointer;" onclick="editCommand(${cmd.id}, '${cmd.name}', '${cmd.category}', '${(cmd.description || '').replace(/'/g, "\\'")}', '${cmd.example.replace(/'/g, "\\'")}')">✏️ 编辑</button>
                                    <button class="btn btn-sm btn-delete" style="background: linear-gradient(45deg, #f44336, #e57373); color: #fff; padding: 6px 12px; font-size: 12px; border: none; border-radius: 4px; cursor: pointer;" onclick="deleteCommand(${cmd.id})">🗑️ 删除</button>
                                </div>
                            </div>
                        `;
                    });
                    commandsList.innerHTML = html || '<p style="color: #b0bec5; text-align: center; padding: 20px;">暂无命令</p>';
                }
            })
            .catch(error => {
                console.error('Error loading commands list:', error);
                commandsList.innerHTML = '<p style="color: #ef5350;">加载命令失败</p>';
            });
    }
    
    window.editCommand = function(id, name, category, description, example) {
        const editModal = document.createElement('div');
        editModal.className = 'modal';
        editModal.innerHTML = `
            <div class="modal-content" style="max-width: 600px;">
                <div class="modal-header">
                    <h3>✏️ 编辑命令</h3>
                    <button class="modal-close">&times;</button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label for="editCommandName">命令名称：</label>
                        <input type="text" id="editCommandName" value="${name}" class="form-control">
                    </div>
                    <div class="form-group">
                        <label for="editCommandCategory">所属分类：</label>
                        <select id="editCommandCategory" class="form-control">
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="editCommandDesc">命令描述：</label>
                        <textarea id="editCommandDesc" class="form-control" rows="2">${description || ''}</textarea>
                    </div>
                    <div class="form-group">
                        <label for="editCommandExample">命令示例：</label>
                        <div style="display: flex; gap: 10px; align-items: center;">
                            <input type="text" id="editCommandExample" value="${example}" class="form-control" style="flex: 1;">
                            <button id="editInsertVariableBtn" class="btn btn-secondary" title="插入变量" style="padding: 8px 12px; white-space: nowrap;">📌 变量</button>
                        </div>
                    </div>
                    <div class="form-group">
                        <div id="editVariableList" style="display: none; background: rgba(156, 39, 176, 0.1); border: 1px solid #6a1b9a; border-radius: 8px; padding: 15px; margin-top: 5px;">
                            <div style="margin-bottom: 10px; font-weight: bold; color: #ce93d8;">可用的变量，点击自动插入：</div>
                            <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px;">
                                <button class="edit-variable-btn" data-variable="@复活点" style="padding: 8px 14px; background: linear-gradient(45deg, #2196f3, #42a5f5); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">📍 复活点坐标</button>
                                <button class="edit-variable-btn" data-variable="@角色名" style="padding: 8px 14px; background: linear-gradient(45deg, #ff9800, #ffb74d); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">👤 角色名</button>
                                <button class="edit-variable-btn" data-variable="@同部落角色名" style="padding: 8px 14px; background: linear-gradient(45deg, #9c27b0, #ab47bc); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">👥 同部落角色名</button>
                                <button class="edit-variable-btn" data-variable="@玩家等级" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">📊 玩家等级</button>
                                <button class="edit-variable-btn" data-variable="@玩家力量" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">💪 玩家力量</button>
                                <button class="edit-variable-btn" data-variable="@玩家灵活" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">🏃 玩家灵活</button>
                                <button class="edit-variable-btn" data-variable="@玩家活力" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">❤️ 玩家活力</button>
                                <button class="edit-variable-btn" data-variable="@玩家毅力" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">⚡ 玩家毅力</button>
                                <button class="edit-variable-btn" data-variable="@玩家权威" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">👑 玩家权威</button>
                                <button class="edit-variable-btn" data-variable="@玩家专长" style="padding: 8px 14px; background: linear-gradient(45deg, #4caf50, #66bb6a); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">🎒 玩家专长</button>
                                <button class="edit-variable-btn" data-variable="@玩家当前坐标" style="padding: 8px 14px; background: linear-gradient(45deg, #00bcd4, #4dd0e1); border: none; border-radius: 6px; cursor: pointer; font-size: 13px; color: #fff;">📍 玩家当前坐标</button>
                            </div>
                            <div style="font-size: 12px; color: #b0bec5; line-height: 1.6;">
                                <div style="margin-bottom: 8px; padding: 10px; background: rgba(33, 150, 243, 0.1); border-left: 3px solid #2196f3; border-radius: 4px;">
                                    <strong style="color: #64b5f6;">@复活点坐标</strong>：游戏中必须要有床，且首次放置床铺后需要自杀一次才能激活，构建传送到复活点命令示例："con 索引 TeleportPlayer @复活点坐标"
                                </div>
                                <div style="margin-bottom: 8px; padding: 10px; background: rgba(255, 152, 0, 0.1); border-left: 3px solid #ff9800; border-radius: 4px;">
                                    <strong style="color: #ffb74d;">@角色名</strong>：自动触发命令如果关键词设置开头为"cs",那么游戏中必须输入"cs@角色名"只有@存在才能让本系统获取角色名，你也可以设置关键词开头为@，这样游戏中直接输入"@角色名"就能直接触发命令了，构建传送到玩家身边的命令示例："con 索引 TeleportPlayer @角色名"，观看玩家命令示例："con 索引 ViewPlayer @角色名"
                                </div>
                                <div style="margin-bottom: 8px; padding: 10px; background: rgba(156, 39, 176, 0.1); border-left: 3px solid #9c27b0; border-radius: 4px;">
                                    <strong style="color: #ce93d8;">@同部落角色名</strong>：对于@角色名不同的是这个变量只对同部落的玩家起作用，适合PVP模式，但是注意的是两个人都必须要有床且同在一个部落，构建传送到同部落玩家身边的命令示例："con 索引 TeleportPlayer @同部落角色名"
                                </div>
                                <div style="margin-bottom: 8px; padding: 10px; background: rgba(76, 175, 80, 0.1); border-left: 3px solid #4caf50; border-radius: 4px;">
                                    <strong style="color: #81c784;">@玩家等级 / @玩家力量 / @玩家灵活 / @玩家活力 / @玩家毅力 / @玩家权威 / @玩家专长</strong>：获取玩家的属性数值，支持数学运算（+、-、*、/），例如："con 索引 setstat AttributeMight @玩家力量+1" 如果@玩家力量是20，+1则是21
                                </div>
                                <div style="margin-bottom: 8px; padding: 10px; background: rgba(0, 188, 212, 0.1); border-left: 3px solid #00bcd4; border-radius: 4px;">
                                    <strong style="color: #4dd0e1;">@玩家当前坐标</strong>：获取玩家的当前坐标（X Y Z格式），构建传送命令示例："con 索引 TeleportPlayer @玩家当前坐标"
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-actions" style="margin-top: 15px;">
                        <button id="saveCommandBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #4caf50, #66bb6a); color: #fff;">💾 保存修改</button>
                        <button id="cancelEditBtn" class="btn btn-secondary">取消</button>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(editModal);
        
        fetch('/api/categories')
            .then(response => response.json())
            .then(data => {
                const categorySelect = editModal.querySelector('#editCommandCategory');
                data.categories.forEach(cat => {
                    const option = document.createElement('option');
                    option.value = cat.name;
                    option.textContent = cat.name;
                    if (cat.name === category) {
                        option.selected = true;
                    }
                    categorySelect.appendChild(option);
                });
            });
        
        editModal.querySelector('#saveCommandBtn').addEventListener('click', function() {
            const newName = editModal.querySelector('#editCommandName').value.trim();
            const newCategory = editModal.querySelector('#editCommandCategory').value;
            const newDesc = editModal.querySelector('#editCommandDesc').value.trim();
            const newExample = editModal.querySelector('#editCommandExample').value.trim();
            
            if (!newName || !newCategory || !newExample) {
                alert('请填写命令名称、分类和示例');
                return;
            }
            
            fetch('/api/commands/update', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ id, name: newName, description: newDesc, category: newCategory, example: newExample })
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    editModal.remove();
                    loadCommands(categoryFilter.value);
                    document.querySelectorAll('.modal').forEach(modal => {
                        if (modal.querySelector('#commandsList')) {
                            const modalCategoryFilter = modal.querySelector('#commandFilterCategory');
                            const currentCategory = modalCategoryFilter ? modalCategoryFilter.value : 'all';
                            loadCommandsList(modal, currentCategory);
                        }
                    });
                } else {
                    alert('更新失败：' + data.message);
                }
            })
            .catch(error => {
                console.error('Error updating command:', error);
                alert('更新失败，请稍后重试');
            });
        });
        
        editModal.querySelector('#cancelEditBtn').addEventListener('click', function() {
            editModal.remove();
        });
        
        const editInsertVariableBtn = editModal.querySelector('#editInsertVariableBtn');
        const editVariableList = editModal.querySelector('#editVariableList');
        
        if (editInsertVariableBtn && editVariableList) {
            editInsertVariableBtn.addEventListener('click', function() {
                editVariableList.style.display = editVariableList.style.display === 'none' ? 'block' : 'none';
            });
        }
        
        const editVariableBtns = editModal.querySelectorAll('.edit-variable-btn');
        editVariableBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                const variable = this.getAttribute('data-variable');
                const exampleInput = editModal.querySelector('#editCommandExample');
                const cursorPosition = exampleInput.selectionStart;
                const currentValue = exampleInput.value;
                const newValue = currentValue.slice(0, cursorPosition) + variable + currentValue.slice(cursorPosition);
                exampleInput.value = newValue;
                exampleInput.focus();
                exampleInput.setSelectionRange(cursorPosition + variable.length, cursorPosition + variable.length);
                editVariableList.style.display = 'none';
            });
        });
        
        const closeBtn = editModal.querySelector('.modal-close');
        closeBtn.addEventListener('click', function() {
            editModal.remove();
        });
    };
    
    window.deleteCommand = function(id) {
        if (!confirm('确定要删除这个命令吗？')) {
            return;
        }
        
        fetch('/api/commands/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ id })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('命令删除成功');
                loadCommands(categoryFilter.value);
                document.querySelectorAll('.modal').forEach(modal => {
                    if (modal.querySelector('#commandsList')) {
                        const modalCategoryFilter = modal.querySelector('#commandFilterCategory');
                        const currentCategory = modalCategoryFilter ? modalCategoryFilter.value : 'all';
                        loadCommandsList(modal, currentCategory);
                    }
                });
            } else {
                alert('删除失败：' + data.message);
            }
        })
        .catch(error => {
            console.error('Error deleting command:', error);
            alert('删除失败，请稍后重试');
        });
    };
});

window.preloadIcons = preloadIcons;
window.clearIconCache = clearIconCache;
window.checkIconCacheStatus = checkIconCacheStatus;
