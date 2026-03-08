function openServerTimeConditionModal(parentModal, level = 'primary') {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth();
    const currentDay = now.getDate();
    
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 750px;">
            <div class="modal-header">
                <h3>📅 时间条件</h3>
                <button class="modal-close">&times;</button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label>时间类型：</label>
                    <select id="timeType" class="form-control">
                        <option value="date_range">指定日期时间</option>
                        <option value="weekday">每周固定星期</option>
                    </select>
                </div>
                
                <div id="dateRangeSection">
                    <div style="display: flex; gap: 20px;">
                        <div style="flex: 1;">
                            <div class="form-group">
                                <label>开始时间：</label>
                                <div class="datetime-picker" style="background: #1a1a1a; border: 1px solid #333; border-radius: 8px; padding: 15px;">
                                    <div class="date-selector" style="margin-bottom: 10px;">
                                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                                            <button type="button" class="btn-nav-month" data-target="start" data-direction="prev" style="background: #333; border: none; color: #fff; padding: 5px 10px; border-radius: 4px; cursor: pointer;">◀</button>
                                            <span id="startMonthYear" style="color: #fff; font-weight: bold;"></span>
                                            <button type="button" class="btn-nav-month" data-target="start" data-direction="next" style="background: #333; border: none; color: #fff; padding: 5px 10px; border-radius: 4px; cursor: pointer;">▶</button>
                                        </div>
                                        <div id="startCalendar" class="calendar-grid" style="display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; text-align: center;"></div>
                                    </div>
                                    <div style="display: flex; gap: 10px; align-items: center;">
                                        <label style="color: #b0bec5; font-size: 13px;">时间：</label>
                                        <select id="startHour" class="form-control" style="width: 70px;">
                                            ${Array.from({length: 24}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 0 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                                        </select>
                                        <span style="color: #b0bec5;">:</span>
                                        <select id="startMinute" class="form-control" style="width: 70px;">
                                            ${Array.from({length: 60}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 0 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                                        </select>
                                    </div>
                                    <input type="hidden" id="startDate">
                                </div>
                            </div>
                        </div>
                        <div style="flex: 1;">
                            <div class="form-group">
                                <label>结束时间：</label>
                                <div class="datetime-picker" style="background: #1a1a1a; border: 1px solid #333; border-radius: 8px; padding: 15px;">
                                    <div class="date-selector" style="margin-bottom: 10px;">
                                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                                            <button type="button" class="btn-nav-month" data-target="end" data-direction="prev" style="background: #333; border: none; color: #fff; padding: 5px 10px; border-radius: 4px; cursor: pointer;">◀</button>
                                            <span id="endMonthYear" style="color: #fff; font-weight: bold;"></span>
                                            <button type="button" class="btn-nav-month" data-target="end" data-direction="next" style="background: #333; border: none; color: #fff; padding: 5px 10px; border-radius: 4px; cursor: pointer;">▶</button>
                                        </div>
                                        <div id="endCalendar" class="calendar-grid" style="display: grid; grid-template-columns: repeat(7, 1fr); gap: 2px; text-align: center;"></div>
                                    </div>
                                    <div style="display: flex; gap: 10px; align-items: center;">
                                        <label style="color: #b0bec5; font-size: 13px;">时间：</label>
                                        <select id="endHour" class="form-control" style="width: 70px;">
                                            ${Array.from({length: 24}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 23 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                                        </select>
                                        <span style="color: #b0bec5;">:</span>
                                        <select id="endMinute" class="form-control" style="width: 70px;">
                                            ${Array.from({length: 60}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 59 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                                        </select>
                                    </div>
                                    <input type="hidden" id="endDate">
                                </div>
                            </div>
                        </div>
                    </div>
                    <div style="color: #b0bec5; font-size: 12px; margin-top: 10px; padding: 10px; background: rgba(255,255,255,0.05); border-radius: 4px;">
                        💡 提示：点击日历中的日期进行选择，<span style="color: #4caf50;">绿色</span>表示已选择的日期。结束时间必须大于开始时间，且不能选择已过去的日期。
                    </div>
                </div>
                
                <div id="weekdaySection" style="display: none;">
                    <div class="form-group">
                        <label>选择星期：</label>
                        <div style="display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px;">
                            <label class="checkbox-label weekday-label" data-day="1" style="background: #1a1a1a; padding: 12px 20px; border-radius: 8px; border: 2px solid #333; cursor: pointer; transition: all 0.2s;">
                                <input type="checkbox" class="weekday-checkbox" value="1" style="display: none;"> <span>周一</span>
                            </label>
                            <label class="checkbox-label weekday-label" data-day="2" style="background: #1a1a1a; padding: 12px 20px; border-radius: 8px; border: 2px solid #333; cursor: pointer; transition: all 0.2s;">
                                <input type="checkbox" class="weekday-checkbox" value="2" style="display: none;"> <span>周二</span>
                            </label>
                            <label class="checkbox-label weekday-label" data-day="3" style="background: #1a1a1a; padding: 12px 20px; border-radius: 8px; border: 2px solid #333; cursor: pointer; transition: all 0.2s;">
                                <input type="checkbox" class="weekday-checkbox" value="3" style="display: none;"> <span>周三</span>
                            </label>
                            <label class="checkbox-label weekday-label" data-day="4" style="background: #1a1a1a; padding: 12px 20px; border-radius: 8px; border: 2px solid #333; cursor: pointer; transition: all 0.2s;">
                                <input type="checkbox" class="weekday-checkbox" value="4" style="display: none;"> <span>周四</span>
                            </label>
                            <label class="checkbox-label weekday-label" data-day="5" style="background: #1a1a1a; padding: 12px 20px; border-radius: 8px; border: 2px solid #333; cursor: pointer; transition: all 0.2s;">
                                <input type="checkbox" class="weekday-checkbox" value="5" style="display: none;"> <span>周五</span>
                            </label>
                            <label class="checkbox-label weekday-label" data-day="6" style="background: #1a1a1a; padding: 12px 20px; border-radius: 8px; border: 2px solid #333; cursor: pointer; transition: all 0.2s;">
                                <input type="checkbox" class="weekday-checkbox" value="6" style="display: none;"> <span>周六</span>
                            </label>
                            <label class="checkbox-label weekday-label" data-day="0" style="background: #1a1a1a; padding: 12px 20px; border-radius: 8px; border: 2px solid #333; cursor: pointer; transition: all 0.2s;">
                                <input type="checkbox" class="weekday-checkbox" value="0" style="display: none;"> <span>周日</span>
                            </label>
                        </div>
                    </div>
                    <div class="form-group">
                        <label>时间段：</label>
                        <div style="display: flex; gap: 10px; align-items: center; margin-top: 10px;">
                            <select id="weekdayStartHour" class="form-control" style="width: 70px;">
                                ${Array.from({length: 24}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 0 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                            </select>
                            <span style="color: #b0bec5;">:</span>
                            <select id="weekdayStartMinute" class="form-control" style="width: 70px;">
                                ${Array.from({length: 60}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 0 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                            </select>
                            <span style="color: #b0bec5; margin: 0 10px;">至</span>
                            <select id="weekdayEndHour" class="form-control" style="width: 70px;">
                                ${Array.from({length: 24}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 23 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                            </select>
                            <span style="color: #b0bec5;">:</span>
                            <select id="weekdayEndMinute" class="form-control" style="width: 70px;">
                                ${Array.from({length: 60}, (_, i) => `<option value="${String(i).padStart(2, '0')}" ${i === 59 ? 'selected' : ''}>${String(i).padStart(2, '0')}</option>`).join('')}
                            </select>
                        </div>
                        <div style="color: #b0bec5; font-size: 12px; margin-top: 5px;">
                            💡 提示：设置每天的时间段，比如只在晚上18:00到22:00触发。默认全天有效。
                        </div>
                    </div>
                </div>
                
                <div class="form-group" style="margin-top: 20px;">
                    <button id="addServerTimeConditionBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #4caf50, #66bb6a); color: #e8f5e8;">
                        ✓ 添加条件
                    </button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    const timeTypeSelect = modal.querySelector('#timeType');
    const dateRangeSection = modal.querySelector('#dateRangeSection');
    const weekdaySection = modal.querySelector('#weekdaySection');
    
    let startCalendarDate = new Date(currentYear, currentMonth, 1);
    let endCalendarDate = new Date(currentYear, currentMonth, 1);
    let selectedStartDate = null;
    let selectedEndDate = null;
    
    function renderCalendar(target, calendarDate, selectedDate) {
        const calendarId = target + 'Calendar';
        const monthYearId = target + 'MonthYear';
        const calendar = modal.querySelector('#' + calendarId);
        const monthYearSpan = modal.querySelector('#' + monthYearId);
        
        const year = calendarDate.getFullYear();
        const month = calendarDate.getMonth();
        
        const monthNames = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'];
        monthYearSpan.textContent = `${year}年 ${monthNames[month]}`;
        
        const dayNames = ['日', '一', '二', '三', '四', '五', '六'];
        let html = '';
        dayNames.forEach(day => {
            html += `<div style="color: #b0bec5; font-size: 12px; padding: 5px;">${day}</div>`;
        });
        
        const firstDay = new Date(year, month, 1).getDay();
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        for (let i = 0; i < firstDay; i++) {
            html += `<div style="padding: 5px;"></div>`;
        }
        
        for (let day = 1; day <= daysInMonth; day++) {
            const dateObj = new Date(year, month, day);
            const isPast = dateObj < today;
            const isSelected = selectedDate && 
                selectedDate.getFullYear() === year && 
                selectedDate.getMonth() === month && 
                selectedDate.getDate() === day;
            
            let style = 'padding: 8px 5px; cursor: pointer; border-radius: 4px; font-size: 13px; transition: all 0.2s;';
            
            if (isSelected) {
                style += 'background: #4caf50; color: #fff; font-weight: bold;';
            } else if (isPast) {
                style += 'color: #555; cursor: not-allowed;';
            } else {
                style += 'color: #fff;';
            }
            
            html += `<div class="calendar-day" data-target="${target}" data-year="${year}" data-month="${month}" data-day="${day}" style="${style}">${day}</div>`;
        }
        
        calendar.innerHTML = html;
        
        calendar.querySelectorAll('.calendar-day').forEach(dayEl => {
            dayEl.addEventListener('click', function() {
                const dYear = parseInt(this.dataset.year);
                const dMonth = parseInt(this.dataset.month);
                const dDay = parseInt(this.dataset.day);
                const dateObj = new Date(dYear, dMonth, dDay);
                const today = new Date();
                today.setHours(0, 0, 0, 0);
                
                if (dateObj < today) {
                    return;
                }
                
                if (target === 'start') {
                    selectedStartDate = dateObj;
                    modal.querySelector('#startDate').value = `${dYear}-${String(dMonth + 1).padStart(2, '0')}-${String(dDay).padStart(2, '0')}`;
                    renderCalendar('start', startCalendarDate, selectedStartDate);
                } else {
                    selectedEndDate = dateObj;
                    modal.querySelector('#endDate').value = `${dYear}-${String(dMonth + 1).padStart(2, '0')}-${String(dDay).padStart(2, '0')}`;
                    renderCalendar('end', endCalendarDate, selectedEndDate);
                }
            });
        });
    }
    
    modal.querySelectorAll('.btn-nav-month').forEach(btn => {
        btn.addEventListener('click', function() {
            const target = this.dataset.target;
            const direction = this.dataset.direction;
            
            if (target === 'start') {
                if (direction === 'prev') {
                    startCalendarDate.setMonth(startCalendarDate.getMonth() - 1);
                } else {
                    startCalendarDate.setMonth(startCalendarDate.getMonth() + 1);
                }
                renderCalendar('start', startCalendarDate, selectedStartDate);
            } else {
                if (direction === 'prev') {
                    endCalendarDate.setMonth(endCalendarDate.getMonth() - 1);
                } else {
                    endCalendarDate.setMonth(endCalendarDate.getMonth() + 1);
                }
                renderCalendar('end', endCalendarDate, selectedEndDate);
            }
        });
    });
    
    renderCalendar('start', startCalendarDate, null);
    renderCalendar('end', endCalendarDate, null);
    
    modal.querySelectorAll('.weekday-label').forEach(label => {
        label.addEventListener('click', function() {
            const checkbox = this.querySelector('.weekday-checkbox');
            setTimeout(() => {
                if (checkbox.checked) {
                    this.style.borderColor = '#4caf50';
                    this.style.background = 'rgba(76, 175, 80, 0.2)';
                } else {
                    this.style.borderColor = '#333';
                    this.style.background = '#1a1a1a';
                }
            }, 0);
        });
    });
    
    timeTypeSelect.addEventListener('change', function() {
        if (this.value === 'date_range') {
            dateRangeSection.style.display = 'block';
            weekdaySection.style.display = 'none';
        } else {
            dateRangeSection.style.display = 'none';
            weekdaySection.style.display = 'block';
        }
    });
    
    const addBtn = modal.querySelector('#addServerTimeConditionBtn');
    addBtn.addEventListener('click', function() {
        const timeType = timeTypeSelect.value;
        
        if (timeType === 'date_range') {
            if (!selectedStartDate || !selectedEndDate) {
                alert('请选择开始日期和结束日期');
                return;
            }
            
            const startHour = modal.querySelector('#startHour').value;
            const startMinute = modal.querySelector('#startMinute').value;
            const endHour = modal.querySelector('#endHour').value;
            const endMinute = modal.querySelector('#endMinute').value;
            
            const start = new Date(selectedStartDate.getFullYear(), selectedStartDate.getMonth(), selectedStartDate.getDate(), parseInt(startHour), parseInt(startMinute));
            const end = new Date(selectedEndDate.getFullYear(), selectedEndDate.getMonth(), selectedEndDate.getDate(), parseInt(endHour), parseInt(endMinute));
            const now = new Date();
            
            if (end <= start) {
                alert('结束时间必须大于开始时间');
                return;
            }
            
            if (end <= now) {
                alert('结束时间不能是已经过去的时间');
                return;
            }
            
            const formatDateTime = (dt) => {
                return `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')} ${String(dt.getHours()).padStart(2, '0')}:${String(dt.getMinutes()).padStart(2, '0')}`;
            };
            
            const conditionData = {
                type: 'server_time',
                operator: 'date_range',
                value: `${formatDateTime(start)}|${formatDateTime(end)}`
            };
            
            if (level === 'primary') {
                parentModal.addedConditions.push(conditionData);
            } else {
                parentModal.addedSecondaryConditions.push(conditionData);
            }
            updateAddedConditionsList(parentModal);
        } else {
            const selectedDays = [];
            modal.querySelectorAll('.weekday-checkbox:checked').forEach(cb => {
                selectedDays.push(cb.value);
            });
            
            if (selectedDays.length === 0) {
                alert('请至少选择一个星期');
                return;
            }
            
            const startHour = modal.querySelector('#weekdayStartHour').value;
            const startMinute = modal.querySelector('#weekdayStartMinute').value;
            const endHour = modal.querySelector('#weekdayEndHour').value;
            const endMinute = modal.querySelector('#weekdayEndMinute').value;
            
            const conditionData = {
                type: 'server_time',
                operator: 'weekday',
                value: `${selectedDays.join(',')}|${startHour}:${startMinute}|${endHour}:${endMinute}`
            };
            
            if (level === 'primary') {
                parentModal.addedConditions.push(conditionData);
            } else {
                parentModal.addedSecondaryConditions.push(conditionData);
            }
            updateAddedConditionsList(parentModal);
        }
        
        modal.remove();
    });
    
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', function() {
        modal.remove();
    });
    
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            modal.remove();
        }
    });
}

function openConditionSettingModal(parentModal, conditionType, level = 'primary') {
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
    
    const conditionName = conditionNames[conditionType];
    
    if (conditionType === 'new_player') {
        const conditionData = {
            type: conditionType,
            operator: 'eq',
            value: 'yes'
        };
        
        if (level === 'primary') {
            parentModal.addedConditions.push(conditionData);
            updateAddedConditionsList(parentModal);
        } else {
            parentModal.addedSecondaryConditions.push(conditionData);
            updateAddedConditionsList(parentModal);
        }
        return;
    }
    
    if (conditionType === 'vip') {
        const modal = document.createElement('div');
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 500px;">
                <div class="modal-header">
                    <h3>👑 设置会员条件</h3>
                    <button class="modal-close">&times;</button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label>会员状态：</label>
                        <select id="vipStatus" class="form-control">
                            <option value="yes">是会员</option>
                            <option value="no">不是会员</option>
                        </select>
                    </div>
                    <div style="color: #b0bec5; font-size: 12px; margin-bottom: 15px; padding: 10px; background: rgba(255,255,255,0.05); border-radius: 4px;">
                        💡 提示：判断玩家是否为会员。会员需要在玩家管理中设置会员有效期。
                    </div>
                    <div class="form-group" style="margin-top: 20px;">
                        <button id="addVipConditionBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #4caf50, #66bb6a); color: #e8f5e8;">
                            ✓ 添加条件
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        const closeBtn = modal.querySelector('.modal-close');
        closeBtn.addEventListener('click', function() {
            modal.remove();
        });
        
        const addBtn = modal.querySelector('#addVipConditionBtn');
        addBtn.addEventListener('click', function() {
            const vipStatus = modal.querySelector('#vipStatus').value;
            
            const conditionData = {
                type: 'vip',
                operator: 'eq',
                value: vipStatus
            };
            
            if (level === 'primary') {
                parentModal.addedConditions.push(conditionData);
            } else {
                parentModal.addedSecondaryConditions.push(conditionData);
            }
            updateAddedConditionsList(parentModal);
            modal.remove();
        });
        
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                modal.remove();
            }
        });
        
        return;
    }
    
    if (conditionType === 'server_time') {
        openServerTimeConditionModal(parentModal, level);
        return;
    }
    
    let operatorOptions = '';
    if (conditionType === 'keyword') {
        operatorOptions = `
            <option value="eq">等于</option>
            <option value="startsWith">开头等于</option>
            <option value="contains">包含</option>
            <option value="notContains">不包含</option>
            <option value="endsWith">结尾等于</option>
        `;
    } else if (conditionType === 'playtime') {
        operatorOptions = `
            <option value="interval">每隔</option>
            <option value="eq">等于</option>
            <option value="gt">大于</option>
            <option value="lt">小于</option>
            <option value="gte">等于或者大于</option>
            <option value="lte">等于或者小于</option>
        `;
    } else if (conditionType === 'item') {
        operatorOptions = `
            <option value="eq">等于</option>
            <option value="gt">大于</option>
            <option value="lt">小于</option>
            <option value="gte">大于等于</option>
            <option value="lte">小于等于</option>
        `;
    } else {
        operatorOptions = `
            <option value="gte">等于或者大于</option>
            <option value="eq">等于</option>
            <option value="gt">大于</option>
            <option value="lt">小于</option>
            <option value="lte">等于或者小于</option>
        `;
    }
    
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 600px;">
            <div class="modal-header">
                <h3>⚙️ 设置${conditionName}条件</h3>
                <button class="modal-close">&times;</button>
            </div>
            <div class="modal-body">
                ${conditionType === 'item' ? `
                    <div class="form-group">
                        <label>物品ID：</label>
                        <input type="text" 
                               id="itemTemplateId" 
                               class="form-control" 
                               placeholder="输入物品ID（在游戏中登录管理员权限，鼠标触摸物品图标可以查看物品ID。）">
                    </div>
                    <div class="form-group">
                        <div style="display: flex; gap: 10px;">
                            <div style="flex: 1;">
                                <label>比较条件：</label>
                                <select id="conditionOperator" class="form-control">
                                    <option value="gte">大于等于</option>
                                    <option value="eq">等于</option>
                                    <option value="gt">大于</option>
                                    <option value="lt">小于</option>
                                    <option value="lte">小于等于</option>
                                </select>
                            </div>
                            <div style="flex: 1;">
                                <label>数量：</label>
                                <input type="number" 
                                       id="itemQuantity" 
                                       class="form-control" 
                                       placeholder="数量"
                                       min="1"
                                       value="1">
                            </div>
                        </div>
                    </div>
                    <div class="form-group">
                        <button id="addItemBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #2196f3, #64b5f6); width: 100%;">
                            + 添加物品
                        </button>
                    </div>
                    <div class="form-group">
                        <label>已添加的物品：</label>
                        <div id="addedItemsList" style="border: 1px solid #333; border-radius: 4px; padding: 10px; min-height: 100px; max-height: 300px; overflow-y: auto;"></div>
                    </div>
                ` : `
                    <div class="form-group">
                        <label>比较条件：</label>
                        <select id="conditionOperator" class="form-control">
                            ${operatorOptions}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>${conditionName}值${conditionType === 'playtime' ? '（分钟）' : ''}：</label>
                        <input type="${conditionType === 'keyword' || conditionType === 'item' ? 'text' : 'number'}" 
                               id="conditionValue" 
                               class="form-control" 
                               placeholder="输入${conditionName}值${conditionType === 'playtime' ? '（分钟）' : ''}">
                    </div>
                `}
                <div class="form-group" style="margin-top: 20px;">
                    <button id="addConditionBtn" class="btn btn-primary" style="background: linear-gradient(45deg, #4caf50, #66bb6a); color: #e8f5e8;">
                        ✓ 添加条件
                    </button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    let addedItems = [];
    
    if (conditionType === 'item') {
        const existingConditions = level === 'primary' ? parentModal.addedConditions : parentModal.addedSecondaryConditions;
        const existingItemCondition = existingConditions.find(c => c.type === 'item');
        
        if (existingItemCondition) {
            const operatorSelect = modal.querySelector('#conditionOperator');
            if (operatorSelect) {
                operatorSelect.value = existingItemCondition.operator;
            }
            
            const items = existingItemCondition.value.split(';');
            items.forEach(itemStr => {
                itemStr = itemStr.trim();
                if (!itemStr) return;
                
                const parts = itemStr.split(':');
                if (parts.length === 2) {
                    addedItems.push({
                        templateId: parts[0],
                        quantity: parseInt(parts[1])
                    });
                }
            });
        }
    }
    
    function updateAddedItemsList() {
        const listContainer = modal.querySelector('#addedItemsList');
        
        if (addedItems.length === 0) {
            listContainer.innerHTML = '<div style="color: #b0bec5; text-align: center; padding: 20px;">暂无添加的物品</div>';
            return;
        }
        
        const operator = modal.querySelector('#conditionOperator').value;
        const operatorNames = {
            eq: '等于',
            gt: '大于',
            lt: '小于',
            gte: '大于等于',
            lte: '小于等于'
        };
        const operatorText = operatorNames[operator] || operator;
        
        let html = '';
        addedItems.forEach((item, index) => {
            html += `
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #333;">
                    <span>
                        <strong>物品ID:</strong> ${item.templateId} | <strong>数量:</strong> ${item.quantity} | <strong>条件:</strong> ${operatorText}
                    </span>
                    <button type="button" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" data-index="${index}">
                        删除
                    </button>
                </div>
            `;
        });
        
        listContainer.innerHTML = html;
        
        const deleteBtns = listContainer.querySelectorAll('.btn-danger');
        deleteBtns.forEach(btn => {
            btn.addEventListener('click', function() {
                const index = parseInt(this.getAttribute('data-index'));
                addedItems.splice(index, 1);
                updateAddedItemsList();
            });
        });
    }
    
    if (conditionType === 'item') {
        const operatorSelect = modal.querySelector('#conditionOperator');
        operatorSelect.addEventListener('change', function() {
            updateAddedItemsList();
        });
        
        const addItemBtn = modal.querySelector('#addItemBtn');
        addItemBtn.addEventListener('click', function() {
            const templateId = modal.querySelector('#itemTemplateId').value.trim();
            if (!templateId) {
                alert('请输入物品ID');
                return;
            }
            
            const quantity = modal.querySelector('#itemQuantity').value;
            if (!quantity || parseInt(quantity) < 1) {
                alert('请输入有效的物品数量（至少为1）');
                return;
            }
            
            addedItems.push({
                templateId: templateId,
                quantity: parseInt(quantity)
            });
            
            modal.querySelector('#itemTemplateId').value = '';
            modal.querySelector('#itemQuantity').value = '1';
            
            updateAddedItemsList();
        });
        
        updateAddedItemsList();
    }
    
    const addBtn = modal.querySelector('#addConditionBtn');
    addBtn.addEventListener('click', function() {
        const operator = modal.querySelector('#conditionOperator').value;
        
        if (conditionType === 'item') {
            if (addedItems.length === 0) {
                alert('请至少添加一个物品');
                return;
            }
            
            const itemsValue = addedItems.map(item => `${item.templateId}:${item.quantity}`).join(';');
            
            const existingConditions = level === 'primary' ? parentModal.addedConditions : parentModal.addedSecondaryConditions;
            const existingItemConditionIndex = existingConditions.findIndex(c => c.type === 'item');
            
            const conditionData = {
                type: conditionType,
                operator: operator,
                value: itemsValue
            };
            
            if (existingItemConditionIndex !== -1) {
                existingConditions[existingItemConditionIndex] = conditionData;
            } else {
                existingConditions.push(conditionData);
            }
            
            updateAddedConditionsList(parentModal);
        } else {
            const value = modal.querySelector('#conditionValue').value;
            
            if (!value) {
                alert(`请输入${conditionName}值`);
                return;
            }
            
            const conditionData = {
                type: conditionType,
                operator: operator,
                value: conditionType === 'keyword' || conditionType === 'item' ? value : parseFloat(value)
            };
            
            if (level === 'primary') {
                parentModal.addedConditions.push(conditionData);
                updateAddedConditionsList(parentModal);
            } else {
                parentModal.addedSecondaryConditions.push(conditionData);
                updateAddedSecondaryConditionsList(parentModal);
            }
        }
        
        modal.remove();
    });
    
    const closeBtn = modal.querySelector('.modal-close');
    closeBtn.addEventListener('click', function() {
        modal.remove();
    });
    
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            modal.remove();
        }
    });
}

function updateAddedConditionsList(modal) {
    const listContainer = modal.querySelector('#addedConditionsList');
    
    if (modal.addedConditions.length === 0 && modal.addedSecondaryConditions.length === 0) {
        listContainer.innerHTML = '';
        return;
    }
    
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
    
    let html = '<div style="border: 1px solid #6a1b9a; border-radius: 8px; padding: 15px; background: linear-gradient(145deg, #1a1a1a, #101010);">';
    html += '<h5 style="margin: 0 0 10px 0; color: #9c27b0;">已添加的条件：</h5>';
    
    modal.addedConditions.forEach((condition, index) => {
        let conditionText = '';
        if (condition.type === 'new_player') {
            conditionText = `<strong>${conditionNames[condition.type]}</strong>`;
        } else if (condition.type === 'vip') {
            const vipText = condition.value === 'yes' ? '是会员' : '不是会员';
            conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${vipText}`;
        } else if (condition.type === 'server_time') {
            const operator = condition.operator;
            const value = condition.value;
            if (operator === 'weekday') {
                const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
                const parts = value.split('|');
                const selectedDays = parts[0].split(',').map(d => weekdayNames[parseInt(d)]).join('、');
                conditionText = `<strong>${conditionNames[condition.type]}</strong>: 每周的 ${selectedDays}`;
                if (parts.length > 1) {
                    conditionText += ` (${parts[1]})`;
                }
            } else {
                conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${value}`;
            }
        } else if (condition.type === 'item') {
            const items = condition.value.split(';');
            const itemTexts = items.map(item => {
                const parts = item.split(':');
                if (parts.length === 2) {
                    const templateId = parts[0];
                    const quantity = parts[1];
                    return `物品ID ${templateId} 的数量 ${quantity}`;
                }
                return item;
            });
            
            if (items.length === 1) {
                conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${operatorNames[condition.operator]} ${itemTexts[0]}`;
            } else {
                conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${operatorNames[condition.operator]}<br>`;
                conditionText += `<div style="margin-left: 10px;">`;
                itemTexts.forEach(text => {
                    conditionText += `• ${text}<br>`;
                });
                conditionText += `</div>`;
            }
        } else {
            conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${operatorNames[condition.operator]} ${condition.value}`;
        }
        
        html += `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #333;">
                <span>
                    ${conditionText}
                </span>
                <button type="button" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" data-index="${index}" data-level="primary">
                    删除
                </button>
            </div>
        `;
    });
    
    if (modal.addedSecondaryConditions.length > 0) {
        html += '<div style="margin-top: 15px;">';
        html += '<h6 style="margin: 0 0 8px 0; color: #b0bec5; font-size: 14px;">二级条件：</h6>';
        
        modal.addedSecondaryConditions.forEach((condition, index) => {
            let conditionText = '';
            if (condition.type === 'vip') {
                const vipText = condition.value === 'yes' ? '是会员' : '不是会员';
                conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${vipText}`;
            } else if (condition.type === 'item') {
                const items = condition.value.split(';');
                const itemTexts = items.map(item => {
                    const parts = item.split(':');
                    if (parts.length === 2) {
                        const templateId = parts[0];
                        const quantity = parts[1];
                        return `物品ID ${templateId} 的数量 ${quantity}`;
                    }
                    return item;
                });
                
                if (items.length === 1) {
                    conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${operatorNames[condition.operator]} ${itemTexts[0]}`;
                } else {
                    conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${operatorNames[condition.operator]}<br>`;
                    conditionText += `<div style="margin-left: 10px;">`;
                    itemTexts.forEach(text => {
                        conditionText += `• ${text}<br>`;
                    });
                    conditionText += `</div>`;
                }
            } else {
                conditionText = `<strong>${conditionNames[condition.type]}</strong>: ${operatorNames[condition.operator]} ${condition.value}`;
            }
            
            html += `
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #333;">
                    <span>
                        ${conditionText}
                    </span>
                    <button type="button" class="btn btn-danger" style="padding: 4px 8px; font-size: 12px;" data-index="${index}" data-level="secondary">
                        删除
                    </button>
                </div>
            `;
        });
        
        html += '</div>';
    }
    
    html += '</div>';
    listContainer.innerHTML = html;
    
    const deleteBtns = listContainer.querySelectorAll('.btn-danger');
    deleteBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const index = parseInt(this.getAttribute('data-index'));
            const level = this.getAttribute('data-level');
            if (level === 'primary') {
                modal.addedConditions.splice(index, 1);
                updateAddedConditionsList(modal);
            } else {
                modal.addedSecondaryConditions.splice(index, 1);
                updateAddedConditionsList(modal);
            }
        });
    });
}

function saveAutoExecuteRule(modal) {
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
    
    const ruleData = {
        rule_name: ruleName,
        conditions: selectedConditions,
        secondary_conditions: modal.addedSecondaryConditions.length > 0 ? modal.addedSecondaryConditions : null,
        execute_type: executeType,
        execute_data: executeData,
        after_execute: Object.keys(afterExecute).length > 0 ? afterExecute : null,
        enabled: true
    };
    
    fetch('/api/auto-trigger-rules/create', {
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
            modal.addedConditions = [];
            modal.addedSecondaryConditions = [];
            updateAddedConditionsList(modal);
        } else {
            alert('保存失败: ' + result.message);
        }
    })
    .catch(error => {
        console.error('保存规则失败:', error);
        alert('保存失败，请检查网络连接');
    });
}

function updateAddedSecondaryConditionsList(modal) {
    updateAddedConditionsList(modal);
}
