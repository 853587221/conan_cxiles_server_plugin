let currentAdmin = null;

document.addEventListener('DOMContentLoaded', function() {
    initEventListeners();
    checkLoginStatus();
});

function initEventListeners() {
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('logout-btn').addEventListener('click', handleLogout);
    
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', handleTabChange);
    });
    
    document.getElementById('add-user-btn').addEventListener('click', () => openUserModal());
    document.getElementById('add-invite-code-btn').addEventListener('click', () => openInviteCodeModal());
    document.getElementById('generate-invite-code-btn').addEventListener('click', generateRandomInviteCode);
    
    document.getElementById('user-form').addEventListener('submit', handleUserSubmit);
    document.getElementById('invite-code-form').addEventListener('submit', handleInviteCodeSubmit);
    
    document.querySelectorAll('.close-btn, .close-modal-btn').forEach(btn => {
        btn.addEventListener('click', closeAllModals);
    });
    
    window.addEventListener('click', (e) => {
        if (e.target.classList.contains('modal')) {
            closeAllModals();
        }
    });
}

function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    fetch(`/api/login?username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentAdmin = data.username;
                localStorage.setItem('adminUsername', data.username);
                document.getElementById('admin-username').textContent = `管理员: ${data.username}`;
                document.getElementById('login-page').classList.add('hidden');
                document.getElementById('admin-page').classList.remove('hidden');
                loadUsers();
                loadInviteCodes();
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            showMessage('登录失败，请重试', 'error');
        });
}

function handleLogout() {
    fetch('/api/logout', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            currentAdmin = null;
            localStorage.removeItem('adminUsername');
            document.getElementById('login-page').classList.remove('hidden');
            document.getElementById('admin-page').classList.add('hidden');
            document.getElementById('username').value = '';
            document.getElementById('password').value = '';
        }
    })
    .catch(error => {
        console.error('退出登录失败:', error);
        currentAdmin = null;
        localStorage.removeItem('adminUsername');
        document.getElementById('login-page').classList.remove('hidden');
        document.getElementById('admin-page').classList.add('hidden');
        document.getElementById('username').value = '';
        document.getElementById('password').value = '';
    });
}

function handleTabChange(e) {
    const tab = e.target.dataset.tab;
    
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
    
    e.target.classList.add('active');
    document.getElementById(`${tab}-tab`).classList.add('active');
    
    if (tab === 'users') {
        loadUsers();
    } else if (tab === 'invite-codes') {
        loadInviteCodes();
    }
}

function loadUsers() {
    fetch('/api/users')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                renderUsersTable(data.users);
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            showMessage('加载用户列表失败', 'error');
        });
}

function renderUsersTable(users) {
    const tbody = document.getElementById('users-table-body');
    tbody.innerHTML = '';
    
    if (users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" class="empty-table">暂无用户</td></tr>';
        return;
    }
    
    users.forEach(user => {
        const tr = document.createElement('tr');
        
        let statusBadge = '<span class="badge badge-success">正常</span>';
        if (user.locked_until) {
            statusBadge = '<span class="badge badge-danger">已锁定</span>';
        } else if (user.valid_until && new Date(user.valid_until) < new Date()) {
            statusBadge = '<span class="badge badge-danger">已过期</span>';
        } else if (user.login_fail_count > 0) {
            statusBadge = `<span class="badge badge-warning">失败${user.login_fail_count}次</span>`;
        }
        
        let validUntilDisplay = user.valid_until ? user.valid_until : '无限期';
        
        let actionButtons = `
            <button class="btn btn-primary" onclick="editUser('${user.username}')">编辑</button>
            <button class="btn btn-danger" onclick="deleteUser('${user.username}')">删除</button>
        `;
        
        if (user.locked_until) {
            actionButtons = `
                <button class="btn btn-success" onclick="unlockUser('${user.username}')">解锁</button>
                ${actionButtons}
            `;
        }
        
        tr.innerHTML = `
            <td>${user.username}</td>
            <td>${user.rcon_ip}</td>
            <td>${user.rcon_port}</td>
            <td><span class="badge ${user.is_admin ? 'badge-success' : 'badge-info'}">${user.is_admin ? '是' : '否'}</span></td>
            <td>${user.invite_code || '-'}</td>
            <td>${user.register_time}</td>
            <td>${user.login_time || '-'}</td>
            <td>${validUntilDisplay}</td>
            <td>${statusBadge}</td>
            <td class="actions">${actionButtons}</td>
        `;
        tbody.appendChild(tr);
    });
}

function openUserModal() {
    document.getElementById('user-modal-title').textContent = '添加用户';
    document.getElementById('user-form').reset();
    document.getElementById('user-form').dataset.mode = 'add';
    document.getElementById('user-modal').classList.add('show');
}

function editUser(username) {
    console.log('开始编辑用户:', username);
    fetch(`/api/users/${encodeURIComponent(username)}`)
        .then(response => response.json())
        .then(data => {
            console.log('从API获取的用户数据:', data);
            if (data.success) {
                const user = data.user;
                console.log('用户对象:', user);
                console.log('用户 valid_until 值:', user.valid_until);
                document.getElementById('user-modal-title').textContent = '编辑用户';
                document.getElementById('user-form').dataset.mode = 'edit';
                document.getElementById('user-form').dataset.username = username;
                
                document.getElementById('user-username').value = user.username;
                document.getElementById('user-password').value = '';
                document.getElementById('user-rcon-ip').value = user.rcon_ip;
                document.getElementById('user-rcon-password').value = user.rcon_password;
                document.getElementById('user-rcon-port').value = user.rcon_port;
                document.getElementById('user-invite-code').value = user.invite_code || '';
                document.getElementById('user-is-admin').checked = user.is_admin;
                
                let validUntilValue = '';
                console.log('处理前的 validUntilValue:', validUntilValue);
                if (user.valid_until) {
                    validUntilValue = user.valid_until.replace(' ', 'T').substring(0, 16);
                    console.log('执行了replace和substring操作，结果:', validUntilValue);
                }
                document.getElementById('user-valid-until').value = validUntilValue;
                console.log('设置到input后的值:', document.getElementById('user-valid-until').value);
                
                console.log('加载用户有效期:', user.valid_until, '转换为:', validUntilValue);
                
                document.getElementById('user-modal').classList.add('show');
                
                setTimeout(() => {
                    console.log('模态框显示后的input值:', document.getElementById('user-valid-until').value);
                }, 100);
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            console.error('获取用户信息失败:', error);
            showMessage('获取用户信息失败', 'error');
        });
}

function handleUserSubmit(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());
    data.is_admin = document.getElementById('user-is-admin').checked;
    
    let validUntil = document.getElementById('user-valid-until').value;
    console.log('原始有效期值:', validUntil);
    
    if (validUntil) {
        validUntil = validUntil.replace('T', ' ') + ':00';
        data.valid_until = validUntil;
        console.log('转换后的有效期:', data.valid_until);
    } else {
        data.valid_until = null;
        console.log('有效期为空，设置为null');
    }
    
    console.log('提交的数据:', data);
    
    const mode = e.target.dataset.mode;
    const username = e.target.dataset.username;
    
    if (mode === 'edit') {
        data.original_username = username;
        
        if (!data.password) {
            delete data.password;
        }
        
        console.log('准备发送更新请求:', data);
        
        fetch('/api/users/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(response => {
            console.log('收到响应:', response.status);
            return response.json();
        })
        .then(result => {
            console.log('更新结果:', result);
            if (result.success) {
                showMessage(result.message, 'success');
                closeAllModals();
                loadUsers();
            } else {
                showMessage(result.message, 'error');
            }
        })
        .catch(error => {
            console.error('更新请求失败:', error);
            showMessage('操作失败，请重试', 'error');
        });
    } else {
        if (!data.password) {
            showMessage('密码不能为空', 'error');
            return;
        }
        
        fetch('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
        .then(response => response.json())
        .then(result => {
            if (result.success) {
                showMessage(result.message, 'success');
                closeAllModals();
                loadUsers();
            } else {
                showMessage(result.message, 'error');
            }
        })
        .catch(error => {
            showMessage('操作失败，请重试', 'error');
        });
    }
}

function toggleAdmin(username, is_admin) {
    fetch('/api/users/set-admin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, is_admin })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showMessage(result.message, 'success');
            loadUsers();
        } else {
            showMessage(result.message, 'error');
        }
    })
    .catch(error => {
        showMessage('操作失败，请重试', 'error');
    });
}

function deleteUser(username) {
    if (!confirm(`确定要删除用户 "${username}" 吗？`)) {
        return;
    }
    
    fetch('/api/users/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showMessage(result.message, 'success');
            loadUsers();
        } else {
            showMessage(result.message, 'error');
        }
    })
    .catch(error => {
        showMessage('操作失败，请重试', 'error');
    });
}

function unlockUser(username) {
    if (!confirm(`确定要解锁用户 "${username}" 吗？`)) {
        return;
    }
    
    fetch('/api/users/unlock', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showMessage(result.message, 'success');
            loadUsers();
        } else {
            showMessage(result.message, 'error');
        }
    })
    .catch(error => {
        showMessage('操作失败，请重试', 'error');
    });
}

function loadInviteCodes() {
    fetch('/api/invite-codes')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                renderInviteCodesTable(data.invite_codes);
            } else {
                showMessage(data.message, 'error');
            }
        })
        .catch(error => {
            showMessage('加载邀请码列表失败', 'error');
        });
}

function renderInviteCodesTable(codes) {
    const tbody = document.getElementById('invite-codes-table-body');
    tbody.innerHTML = '';
    
    if (codes.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="empty-table">暂无邀请码</td></tr>';
        return;
    }
    
    codes.forEach(code => {
        const tr = document.createElement('tr');
        const isExpired = code.expire_time && new Date(code.expire_time) < new Date();
        const isUsedOut = code.used_count >= code.max_uses;
        
        let statusBadge = '';
        if (!code.is_active) {
            statusBadge = '<span class="badge badge-danger">已停用</span>';
        } else if (isExpired) {
            statusBadge = '<span class="badge badge-warning">已过期</span>';
        } else if (isUsedOut) {
            statusBadge = '<span class="badge badge-warning">已用完</span>';
        } else {
            statusBadge = '<span class="badge badge-success">可用</span>';
        }
        
        tr.innerHTML = `
            <td>${code.code}</td>
            <td>${code.creator_id || '-'}</td>
            <td>${code.max_uses}</td>
            <td>${code.used_count}</td>
            <td>${statusBadge}</td>
            <td>${code.create_time}</td>
            <td>${code.expire_time || '永久'}</td>
            <td class="actions">
                <button class="btn ${code.is_active ? 'btn-warning' : 'btn-success'}" onclick="toggleInviteCode('${code.code}', ${!code.is_active})">
                    ${code.is_active ? '停用' : '激活'}
                </button>
                <button class="btn btn-danger" onclick="deleteInviteCode('${code.code}')">删除</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function openInviteCodeModal() {
    document.getElementById('invite-code-modal-title').textContent = '添加邀请码';
    document.getElementById('invite-code-form').reset();
    document.getElementById('invite-code-modal').classList.add('show');
}

function generateRandomInviteCode() {
    const length = 8;
    const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let code = '';
    for (let i = 0; i < length; i++) {
        code += characters.charAt(Math.floor(Math.random() * characters.length));
    }
    
    document.getElementById('invite-code-code').value = code;
    document.getElementById('invite-code-modal').classList.add('show');
}

function handleInviteCodeSubmit(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());
    data.max_uses = parseInt(data.max_uses);
    if (data.expire_days) {
        data.expire_days = parseInt(data.expire_days);
    }
    
    fetch('/api/invite-codes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showMessage(result.message, 'success');
            closeAllModals();
            loadInviteCodes();
        } else {
            showMessage(result.message, 'error');
        }
    })
    .catch(error => {
        showMessage('操作失败，请重试', 'error');
    });
}

function toggleInviteCode(code, is_active) {
    const endpoint = is_active ? '/api/invite-codes/activate' : '/api/invite-codes/deactivate';
    
    fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showMessage(result.message, 'success');
            loadInviteCodes();
        } else {
            showMessage(result.message, 'error');
        }
    })
    .catch(error => {
        showMessage('操作失败，请重试', 'error');
    });
}

function deleteInviteCode(code) {
    if (!confirm(`确定要删除邀请码 "${code}" 吗？`)) {
        return;
    }
    
    fetch('/api/invite-codes/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            showMessage(result.message, 'success');
            loadInviteCodes();
        } else {
            showMessage(result.message, 'error');
        }
    })
    .catch(error => {
        showMessage('操作失败，请重试', 'error');
    });
}

function closeAllModals() {
    document.querySelectorAll('.modal').forEach(modal => {
        modal.classList.remove('show');
    });
}

function showMessage(message, type = 'info') {
    const messageModal = document.getElementById('message-modal');
    const messageContent = document.getElementById('message-content');
    
    messageContent.textContent = message;
    messageContent.className = 'message-content';
    
    if (type === 'success') {
        messageContent.classList.add('success');
    } else if (type === 'error') {
        messageContent.classList.add('error');
    }
    
    messageModal.classList.add('show');
}

function checkLoginStatus() {
    const savedUsername = localStorage.getItem('adminUsername');
    if (savedUsername) {
        fetch(`/api/verify-session?username=${encodeURIComponent(savedUsername)}`)
            .then(response => response.json())
            .then(data => {
                if (data.success && data.is_admin) {
                    currentAdmin = savedUsername;
                    document.getElementById('admin-username').textContent = `管理员: ${savedUsername}`;
                    document.getElementById('login-page').classList.add('hidden');
                    document.getElementById('admin-page').classList.remove('hidden');
                    loadUsers();
                    loadInviteCodes();
                } else {
                    localStorage.removeItem('adminUsername');
                }
            })
            .catch(error => {
                localStorage.removeItem('adminUsername');
            });
    }
}