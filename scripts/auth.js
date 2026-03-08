document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');
    const loginMessage = document.getElementById('loginMessage');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    
    if (loginForm) {
        fetch('/api/verify-session')
            .then(response => response.json())
            .then(data => {
                if (data.success && data.username) {
                    localStorage.setItem('rconUsername', data.username);
                    window.location.href = 'index.html';
                } else {
                    localStorage.removeItem('rconUsername');
                }
            })
            .catch(error => {
                console.error('验证登录状态失败:', error);
                localStorage.removeItem('rconUsername');
            });
        
        let requireCaptcha = false;
        
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const username = usernameInput.value.trim();
            const password = passwordInput.value.trim();
            const captchaInput = document.getElementById('captcha');
            const captchaValue = captchaInput ? captchaInput.value.trim() : '';
            
            if (!username || !password) {
                showLoginMessage('请输入用户名和密码', 'error');
                return;
            }
            
            if (requireCaptcha && !captchaValue) {
                showLoginMessage('请输入验证码', 'error');
                return;
            }
            
            showLoginMessage('正在登录...', 'info');
            
            const formData = new URLSearchParams();
            formData.append('username', username);
            formData.append('password', password);
            if (requireCaptcha) {
                formData.append('captcha', captchaValue);
            }
            
            fetch('/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    localStorage.setItem('rconUsername', username);
                    showLoginMessage('登录成功！正在跳转...', 'success');
                    setTimeout(() => {
                        window.location.href = 'index.html';
                    }, 1000);
                } else {
                    showLoginMessage(data.message || '用户名或密码错误', 'error');
                    
                    if (data.require_captcha) {
                        requireCaptcha = true;
                        showCaptcha();
                        loadCaptcha(username);
                    }
                }
            })
            .catch(error => {
                showLoginMessage('登录请求失败，请稍后重试', 'error');
                console.error('Login error:', error);
            });
        });
        
        function showCaptcha() {
            const captchaGroup = document.getElementById('captchaGroup');
            if (captchaGroup) {
                captchaGroup.style.display = 'block';
            }
        }
        
        function loadCaptcha(username) {
            fetch(`/api/captcha?username=${encodeURIComponent(username)}`)
                .then(response => response.json())
                .then(data => {
                    if (data.success && data.captcha) {
                        const captchaDisplay = document.getElementById('captchaDisplay');
                        if (captchaDisplay) {
                            captchaDisplay.textContent = data.captcha;
                            captchaDisplay.style.display = 'block';
                        }
                    }
                })
                .catch(error => {
                    console.error('获取验证码失败:', error);
                });
        }
        
        const refreshCaptchaBtn = document.getElementById('refreshCaptchaBtn');
        if (refreshCaptchaBtn) {
            refreshCaptchaBtn.addEventListener('click', function() {
                const username = usernameInput.value.trim();
                if (username) {
                    loadCaptcha(username);
                }
            });
        }
    }
    
    function showLoginMessage(message, type) {
        if (loginMessage) {
            loginMessage.textContent = message;
            loginMessage.className = 'login-message ' + type;
        }
    }
    
    const changePasswordLink = document.getElementById('changePasswordLink');
    const changePasswordModal = document.getElementById('changePasswordModal');
    
    if (changePasswordLink && changePasswordModal) {
        changePasswordLink.addEventListener('click', function(e) {
            e.preventDefault();
            changePasswordModal.style.display = 'flex';
        });
        
        const closeBtn = changePasswordModal.querySelector('.modal-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', function() {
                changePasswordModal.style.display = 'none';
            });
        }
        
        changePasswordModal.addEventListener('click', function(e) {
            if (e.target === changePasswordModal) {
                changePasswordModal.style.display = 'none';
            }
        });
        
        const changePasswordBtn = document.getElementById('changePasswordBtn');
        if (changePasswordBtn) {
            changePasswordBtn.addEventListener('click', function() {
                const username = document.getElementById('cp-username').value.trim();
                const oldPassword = document.getElementById('cp-old-password').value.trim();
                const newPassword = document.getElementById('cp-new-password').value.trim();
                const confirmPassword = document.getElementById('cp-confirm-password').value.trim();
                
                if (!username || !oldPassword || !newPassword || !confirmPassword) {
                    alert('请填写所有字段');
                    return;
                }
                
                if (newPassword.length < 6) {
                    alert('新密码至少需要6个字符');
                    return;
                }
                
                if (newPassword !== confirmPassword) {
                    alert('两次输入的新密码不一致');
                    return;
                }
                
                if (oldPassword === newPassword) {
                    alert('新密码不能与旧密码相同');
                    return;
                }
                
                const formData = new URLSearchParams();
                formData.append('username', username);
                formData.append('old_password', oldPassword);
                formData.append('new_password', newPassword);
                
                fetch('/api/change-password', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: formData
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        alert('密码修改成功！请使用新密码登录');
                        changePasswordModal.style.display = 'none';
                        document.getElementById('cp-username').value = '';
                        document.getElementById('cp-old-password').value = '';
                        document.getElementById('cp-new-password').value = '';
                        document.getElementById('cp-confirm-password').value = '';
                    } else {
                        alert(data.message || '密码修改失败');
                    }
                })
                .catch(error => {
                    alert('密码修改失败，请稍后重试');
                    console.error('Change password error:', error);
                });
            });
        }
    }
    
    const registerForm = document.getElementById('registerForm');
    const registerMessage = document.getElementById('registerMessage');
    
    if (registerForm) {
        registerForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const username = document.getElementById('reg-username').value.trim();
            const password = document.getElementById('reg-password').value.trim();
            const confirmPassword = document.getElementById('reg-confirm-password').value.trim();
            const inviteCode = document.getElementById('reg-invite-code').value.trim();
            
            if (!username || !password || !confirmPassword) {
                showRegisterMessage('请填写所有必填字段', 'error');
                return;
            }
            
            if (username.length < 3) {
                showRegisterMessage('用户名至少需要3个字符', 'error');
                return;
            }
            
            if (password.length < 6) {
                showRegisterMessage('密码至少需要6个字符', 'error');
                return;
            }
            
            if (password !== confirmPassword) {
                showRegisterMessage('两次输入的密码不一致', 'error');
                return;
            }
            
            showRegisterMessage('正在注册...', 'info');
            
            const formData = new URLSearchParams();
            formData.append('username', username);
            formData.append('password', password);
            if (inviteCode) {
                formData.append('invite_code', inviteCode);
            }
            
            fetch('/api/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showRegisterMessage('注册成功！正在跳转...', 'success');
                    setTimeout(() => {
                        window.location.href = 'index.html';
                    }, 1500);
                } else {
                    showRegisterMessage(data.message || '注册失败', 'error');
                }
            })
            .catch(error => {
                showRegisterMessage('注册请求失败，请稍后重试', 'error');
                console.error('Registration error:', error);
            });
        });
    }
    
    function showRegisterMessage(message, type) {
        if (registerMessage) {
            registerMessage.textContent = message;
            registerMessage.className = 'register-message ' + type;
        }
    }
    
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            if (confirm('确定要退出登录吗？')) {
                localStorage.removeItem('rconUsername');
                fetch('/api/logout', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    }
                })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        window.location.href = 'login.html';
                    }
                })
                .catch(error => {
                    console.error('退出登录失败:', error);
                    window.location.href = 'login.html';
                });
            }
        });
    }
});
