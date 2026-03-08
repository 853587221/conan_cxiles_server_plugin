const items = [];
let allItems = [];
let currentCategory = 'all';
let shopUsername = '';
let isShopOwner = false;
let adminCategories = [];
let isEditingCategory = false;
let editingCategoryId = null;

async function initShop() {
    const pathParts = window.location.pathname.split('/');
    if (pathParts.length >= 3 && pathParts[1] === 'shop') {
        shopUsername = pathParts[2];
    }

    if (!shopUsername) {
        document.getElementById('itemsGrid').innerHTML = '<div class="error-message">无法获取用户信息，请重新登录</div>';
        return;
    }

    try {
        await checkAdminPermission();
        await Promise.all([
            loadCategories(),
            loadProducts()
        ]);

        document.getElementById('searchInput').addEventListener('input', handleSearch);
    } catch (error) {
        console.error('初始化商城失败:', error);
        document.getElementById('itemsGrid').innerHTML = '<div class="error-message">加载商品失败，请刷新页面重试</div>';
    }
}

async function checkAdminPermission() {
    try {
        const response = await fetch('/api/verify-session', { credentials: 'same-origin' });
        
        if (response.status === 401) {
            isShopOwner = false;
            document.getElementById('adminBtn').style.display = 'none';
            return;
        }
        
        const data = await response.json();

        if (data.success && data.username === shopUsername) {
            isShopOwner = true;
            document.getElementById('adminBtn').style.display = 'block';
        } else {
            isShopOwner = false;
            document.getElementById('adminBtn').style.display = 'none';
        }
    } catch (error) {
        console.error('检查管理员权限失败:', error);
        isShopOwner = false;
    }
}

async function loadCategories() {
    try {
        const response = await fetch(`/api/shop/categories?username=${encodeURIComponent(shopUsername)}`);
        const data = await response.json();

        if (data.success && data.categories) {
            const categoryList = document.getElementById('categoryList');

            let html = '';
            data.categories.forEach((cat, index) => {
                const isActive = (index === 0) ? 'active' : '';
                const displayKey = cat.key === 'all' ? 'all' : cat.key;
                const count = cat.product_count || 0;
                html += `<li class="category-item ${isActive}" data-category="${displayKey}">`;
                html += `<span>${cat.icon || '📁'} ${cat.name}</span>`;
                html += `<span class="count">${count}</span>`;
                html += `</li>`;
            });
            categoryList.innerHTML = html;

            document.querySelectorAll('.category-item').forEach(item => {
                item.addEventListener('click', () => {
                    document.querySelectorAll('.category-item').forEach(i => i.classList.remove('active'));
                    item.classList.add('active');
                    currentCategory = item.dataset.category;
                    loadProducts(currentCategory);
                });
            });

            if (data.categories.length > 0) {
                currentCategory = data.categories[0].key === 'all' ? 'all' : data.categories[0].key;
            }
        }
    } catch (error) {
        console.error('加载分类失败:', error);
    }
}

async function loadProducts(category = 'all') {
    try {
        const url = `/api/shop/products?username=${encodeURIComponent(shopUsername)}`;

        const response = await fetch(url);
        const data = await response.json();

        if (data.success && data.products) {
            allItems.length = 0;
            data.products.forEach(p => {
                allItems.push({
                    id: p.id,
                    name: p.name,
                    icon: p.image ? '' : '📦',
                    image: p.image,
                    desc: p.description,
                    category: p.category_key,
                    price: p.price
                });
            });

            items.length = 0;
            if (category === 'all') {
                items.push(...allItems);
            } else {
                items.push(...allItems.filter(p => p.category === category));
            }

            updateCategoryCounts();
            renderItems();
        }
    } catch (error) {
        console.error('加载商品失败:', error);
    }
}

function updateCategoryCounts() {
    document.querySelectorAll('.category-item').forEach(item => {
        const catKey = item.dataset.category;
        if (catKey === 'all') {
            item.querySelector('.count').textContent = allItems.length;
        } else {
            const count = allItems.filter(p => p.category === catKey).length;
            item.querySelector('.count').textContent = count;
        }
    });
}

async function handleSearch(e) {
    const keyword = e.target.value.trim();

    if (!keyword) {
        loadProducts('all');
        return;
    }

    try {
        const response = await fetch(`/api/shop/search?username=${encodeURIComponent(shopUsername)}&keyword=${encodeURIComponent(keyword)}`);
        const data = await response.json();

        if (data.success && data.products) {
            items.length = 0;
            data.products.forEach(p => {
                items.push({
                    id: p.id,
                    name: p.name,
                    icon: p.icon,
                    desc: p.description,
                    category: p.category_key,
                    price: p.price
                });
            });

            document.querySelectorAll('.category-item').forEach(i => i.classList.remove('active'));
            renderItems();
        }
    } catch (error) {
        console.error('搜索商品失败:', error);
    }
}

function renderItems(category = null) {
    const grid = document.getElementById('itemsGrid');
    const filterCategory = category || currentCategory;

    const filteredItems = filterCategory === 'all'
        ? items
        : items.filter(item => item.category === filterCategory);

    if (filteredItems.length === 0) {
        grid.innerHTML = '<div class="empty-message">该分类暂无商品</div>';
        return;
    }

    grid.innerHTML = filteredItems.map(item => `
        <div class="item-card" data-id="${item.id}">
            ${item.image ? `<img data-src="/api/shop/image/${shopUsername}/${item.image}?size=200x200" data-full="/api/shop/image/${shopUsername}/${item.image}" alt="${item.name}" class="item-image lazy-load">` : `<span class="item-icon">${item.icon || '📦'}</span>`}
            <div class="item-name">${item.name}</div>
            <div class="item-desc">${item.desc}</div>
            <div class="item-price">
                <span>🪙</span>
                <span>${item.price.toLocaleString()}</span>
            </div>
        </div>
    `).join('');

    initLazyLoad();

    grid.querySelectorAll('.item-card').forEach(card => {
        card.addEventListener('click', function() {
            const itemId = parseInt(this.dataset.id);
            openProductDetailModal(itemId);
        });
        card.style.cursor = 'pointer';
    });

    grid.querySelectorAll('.item-image').forEach(img => {
        img.addEventListener('click', function(e) {
            e.stopPropagation();
            const fullImageSrc = this.dataset.full;
            if (fullImageSrc) {
                openImagePreview(fullImageSrc);
            }
        });
        img.style.cursor = 'pointer';
    });
}

async function loadAdminCategories() {
    try {
        const response = await fetch(`/api/shop/categories?username=${encodeURIComponent(shopUsername)}`);
        const data = await response.json();

        if (data.success && data.categories) {
            adminCategories = data.categories.filter(c => c.key !== 'all');
            renderCategoryAdminList();
            updateProductCategorySelect();
            setupCategoryAdminEvents();
        }
    } catch (error) {
        console.error('加载管理分类失败:', error);
    }
}

function renderCategoryAdminList() {
    const list = document.getElementById('categoryAdminList');

    if (adminCategories.length === 0) {
        list.innerHTML = '<div class="admin-empty">暂无分类，请添加</div>';
        return;
    }

    list.innerHTML = adminCategories.map(cat => `
        <div class="admin-item" data-id="${cat.id}">
            <div class="admin-item-info">
                <span class="admin-item-icon">${cat.icon || '📁'}</span>
                <span class="admin-item-name">${cat.name}</span>
            </div>
            <div class="admin-item-actions">
                <button class="admin-edit-btn" data-id="${cat.id}" data-key="${cat.key}" data-name="${cat.name}" data-icon="${cat.icon || ''}" data-sort="${cat.sort_order}">编辑</button>
                <button class="admin-delete-btn" data-id="${cat.id}">删除</button>
            </div>
        </div>
    `).join('');
}

function updateProductCategorySelect() {
    const select = document.getElementById('productCategory');
    select.innerHTML = '<option value="">选择分类</option>' +
        adminCategories.map(cat => `<option value="${cat.key}">${cat.icon || '📁'} ${cat.name}</option>`).join('');
}

function setupCategoryAdminEvents() {
    document.querySelectorAll('.admin-edit-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.dataset.id);
            const key = btn.dataset.key;
            const name = btn.dataset.name;
            const icon = btn.dataset.icon;
            const sortOrder = parseInt(btn.dataset.sort);
            editCategory(id, key, name, icon, sortOrder);
        });
    });

    document.querySelectorAll('.admin-delete-btn[data-id]').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.dataset.id);
            deleteCategory(id);
        });
    });
}

async function handleCategorySubmit() {
    if (isEditingCategory && editingCategoryId) {
        await updateCategory();
    } else {
        await addCategory();
    }
}

async function addCategory() {
    const name = document.getElementById('newCategoryName').value.trim();
    const icon = document.getElementById('newCategoryIcon').value.trim();
    const sortOrder = parseInt(document.getElementById('newCategorySort').value) || 0;

    if (!name) {
        alert('请输入分类名称');
        return;
    }

    let key = name.toLowerCase().replace(/\s+/g, '_').replace(/[^\u4e00-\u9fa5a-z0-9_]/g, '');
    if (!key) {
        key = 'category_' + Date.now();
    }
    
    console.log('[DEBUG] addCategory - name:', name, 'key:', key, 'icon:', icon, 'sort_order:', sortOrder);

    try {
        const response = await fetch('/api/shop/admin/category/add', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ key, name, icon, sort_order: sortOrder })
        });
        const data = await response.json();

        if (data.success) {
            resetCategoryForm();
            await loadAdminCategories();
            await loadCategories();
            await loadProducts();
            alert('分类添加成功');
        } else {
            alert(data.message || '添加失败');
        }
    } catch (error) {
        console.error('添加分类失败:', error);
        alert('添加失败，请重试');
    }
}

async function updateCategory() {
    const id = editingCategoryId;
    const name = document.getElementById('newCategoryName').value.trim();
    const icon = document.getElementById('newCategoryIcon').value.trim();
    const sortOrder = parseInt(document.getElementById('newCategorySort').value) || 0;
    let key = name.toLowerCase().replace(/\s+/g, '_').replace(/[^\u4e00-\u9fa5a-z0-9_]/g, '');
    if (!key) {
        key = 'category_' + Date.now();
    }

    if (!id || !name) {
        alert('请完善分类信息');
        return;
    }
    
    console.log('[DEBUG] updateCategory - id:', id, 'name:', name, 'key:', key, 'icon:', icon, 'sort_order:', sortOrder);

    try {
        const response = await fetch('/api/shop/admin/category/update', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id, key, name, icon, sort_order: sortOrder })
        });
        const data = await response.json();

        if (data.success) {
            resetCategoryForm();
            await loadAdminCategories();
            await loadCategories();
            await loadProducts();
            alert('分类更新成功');
        } else {
            alert(data.message || '更新失败');
        }
    } catch (error) {
        console.error('更新分类失败:', error);
        alert('更新失败，请重试');
    }
}

function resetCategoryForm() {
    document.getElementById('newCategoryName').value = '';
    document.getElementById('newCategoryIcon').value = '';
    document.getElementById('newCategorySort').value = '0';
    document.getElementById('addCategoryBtn').textContent = '添加分类';
    document.getElementById('cancelCategoryEditBtn').style.display = 'none';
    isEditingCategory = false;
    editingCategoryId = null;
}

function editCategory(id, key, name, icon, sortOrder) {
    document.getElementById('newCategoryName').value = name;
    document.getElementById('newCategoryIcon').value = icon;
    document.getElementById('newCategorySort').value = sortOrder;
    document.getElementById('addCategoryBtn').textContent = '更新分类';
    document.getElementById('cancelCategoryEditBtn').style.display = 'inline-block';
    isEditingCategory = true;
    editingCategoryId = id;
}

async function deleteCategory(id) {
    if (!confirm('确定要删除此分类吗？分类下的所有商品也会被删除。')) {
        return;
    }

    try {
        const response = await fetch(`/api/shop/admin/category/delete/${id}`, {
            method: 'POST',
            credentials: 'same-origin'
        });
        const data = await response.json();

        if (data.success) {
            await loadAdminCategories();
            await loadCategories();
            await loadProducts();
            alert('分类已删除');
        } else {
            alert(data.message || '删除失败');
        }
    } catch (error) {
        console.error('删除分类失败:', error);
        alert('删除失败，请重试');
    }
}

async function loadAdminProducts() {
    try {
        const response = await fetch(`/api/shop/products?username=${encodeURIComponent(shopUsername)}`);
        const data = await response.json();

        if (data.success && data.products) {
            renderProductAdminList(data.products);
        }
    } catch (error) {
        console.error('加载管理商品失败:', error);
    }
}

function renderProductAdminList(products) {
    const list = document.getElementById('productAdminList');

    if (products.length === 0) {
        list.innerHTML = '<div class="admin-empty">暂无商品，请添加</div>';
        return;
    }

    list.innerHTML = products.map(p => `
        <div class="admin-item" data-id="${p.id}">
            <div class="admin-item-info">
                ${p.image ? `<img data-src="/api/shop/image/${shopUsername}/${p.image}?size=60x60" alt="${p.name}" class="admin-item-image lazy-load">` : `<span class="admin-item-icon">${'📦'}</span>`}
                <span class="admin-item-name">${p.name} - 🪙${p.price}</span>
            </div>
            <div class="admin-item-actions">
                <button class="admin-edit-btn product-edit-btn" data-id="${p.id}" data-category="${p.category_key}" data-name="${p.name}" data-image="${(p.image || '').replace(/"/g, '&quot;')}" data-desc="${(p.description || '').replace(/"/g, '&quot;')}" data-price="${p.price}" data-sort="${p.sort_order}">编辑</button>
                <button class="admin-delete-btn product-delete-btn" data-id="${p.id}">删除</button>
            </div>
        </div>
    `).join('');
    
    initLazyLoad();
    setupProductAdminEvents();
}

function setupProductAdminEvents() {
    document.querySelectorAll('.product-edit-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.dataset.id);
            const categoryKey = btn.dataset.category;
            const name = btn.dataset.name;
            const image = btn.dataset.image;
            const description = btn.dataset.desc;
            const price = parseFloat(btn.dataset.price);
            const sortOrder = parseInt(btn.dataset.sort);
            editProduct(id, categoryKey, name, image, description, price, sortOrder);
        });
    });

    document.querySelectorAll('.product-delete-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = parseInt(btn.dataset.id);
            deleteProduct(id);
        });
    });
}

async function saveProduct() {
    const editId = document.getElementById('editProductId').value;
    const categoryKey = document.getElementById('productCategory').value;
    const name = document.getElementById('productName').value.trim();
    const image = document.getElementById('productImage').value.trim();
    const description = document.getElementById('productDesc').value.trim();
    const price = parseFloat(document.getElementById('productPrice').value) || 0;
    const sortOrder = parseInt(document.getElementById('productSort').value) || 0;

    console.log('[DEBUG] saveProduct - categoryKey:', categoryKey, 'name:', name, 'price:', price);
    console.log('[DEBUG] categoryKey type:', typeof categoryKey, 'name type:', typeof name, 'price type:', typeof price);

    if (!categoryKey || !name || price <= 0) {
        alert('请完善商品信息（分类、名称为必填，价格必须大于0）');
        return;
    }

    const url = editId ? '/api/shop/admin/product/update' : '/api/shop/admin/product/add';
    const body = editId
        ? { id: parseInt(editId), name, image, description, category: categoryKey, price, sort_order: sortOrder }
        : { name, image, description, category: categoryKey, price, sort_order: sortOrder };

    try {
        const response = await fetch(url, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await response.json();

        if (data.success) {
            clearProductForm();
            await loadAdminProducts();
            await loadProducts();
            alert(editId ? '商品更新成功' : '商品添加成功');
        } else {
            alert(data.message || '保存失败');
        }
    } catch (error) {
        console.error('保存商品失败:', error);
        alert('保存失败，请重试');
    }
}

function editProduct(id, categoryKey, name, image, description, price, sortOrder) {
    document.getElementById('editProductId').value = id;
    document.getElementById('productCategory').value = categoryKey;
    document.getElementById('productName').value = name;
    document.getElementById('productImage').value = image;
    document.getElementById('productDesc').value = description;
    document.getElementById('productPrice').value = price;
    document.getElementById('productSort').value = sortOrder;
    document.getElementById('saveProductBtn').textContent = '更新商品';
    document.getElementById('cancelEditBtn').style.display = 'inline-block';
    
    if (image) {
        const preview = document.getElementById('productImagePreview');
        preview.innerHTML = `<img src="/api/shop/image/${shopUsername}/${image}?size=100x100" alt="商品图片" style="max-width: 100px; max-height: 100px;">`;
    }
}

function clearProductForm() {
    document.getElementById('editProductId').value = '';
    document.getElementById('productCategory').value = '';
    document.getElementById('productName').value = '';
    document.getElementById('productImage').value = '';
    document.getElementById('productDesc').value = '';
    document.getElementById('productPrice').value = '';
    document.getElementById('productSort').value = '0';
    document.getElementById('saveProductBtn').textContent = '保存商品';
    document.getElementById('cancelEditBtn').style.display = 'none';
    document.getElementById('productImagePreview').innerHTML = '';
}

async function deleteProduct(id) {
    if (!confirm('确定要删除此商品吗？')) {
        return;
    }

    try {
        const response = await fetch(`/api/shop/admin/product/delete/${id}`, {
            method: 'POST',
            credentials: 'same-origin'
        });
        const data = await response.json();

        if (data.success) {
            await loadAdminProducts();
            await loadProducts();
            alert('商品已删除');
        } else {
            alert(data.message || '删除失败');
        }
    } catch (error) {
        console.error('删除商品失败:', error);
        alert('删除失败，请重试');
    }
}

function openAdminModal() {
    document.getElementById('adminModal').style.display = 'flex';
    loadAdminCategories();
    loadAdminProducts();
    initIconPickers();
}

function closeAdminModal() {
    document.getElementById('adminModal').style.display = 'none';
    clearProductForm();
    resetCategoryForm();
}

let queryGoldPlayers = [];

function openQueryGoldModal() {
    document.getElementById('queryGoldUsername').value = shopUsername;
    document.getElementById('queryGoldSearch').value = '';
    document.getElementById('queryGoldResult').style.display = 'none';
    document.getElementById('queryGoldModal').style.display = 'flex';
    queryGold();
}

function closeQueryGoldModal() {
    document.getElementById('queryGoldModal').style.display = 'none';
    queryGoldPlayers = [];
}

function openImagePreview(imageSrc) {
    document.getElementById('imagePreviewImg').src = imageSrc;
    document.getElementById('imagePreviewModal').style.display = 'flex';
}

function closeImagePreviewModal() {
    document.getElementById('imagePreviewModal').style.display = 'none';
    document.getElementById('imagePreviewImg').src = '';
}

async function queryGold() {
    const username = document.getElementById('queryGoldUsername').value;
    if (!username) {
        alert('请输入用户名');
        return;
    }

    try {
        const response = await fetch(`/shop/query?username=${encodeURIComponent(username)}`);
        const data = await response.json();

        if (data.success) {
            queryGoldPlayers = data.players || [];
            renderQueryGoldPlayers(queryGoldPlayers);
        } else {
            alert(data.message || '查询失败');
        }
    } catch (error) {
        console.error('查询金币失败:', error);
        alert('查询失败，请重试');
    }
}

function renderQueryGoldPlayers(players) {
    const playerList = document.getElementById('queryGoldPlayerList');
    
    if (players && players.length > 0) {
        let html = '<div style="margin-bottom: 10px; color: #6495ed;">共找到 ' + players.length + ' 个角色</div>';
        html += '<div style="display: grid; gap: 10px; max-height: 400px; overflow-y: auto; padding-right: 5px;">';
        
        players.forEach(player => {
            html += `
                <div style="display: flex; justify-content: space-between; align-items: center; padding: 12px; background: rgba(0,0,0,0.3); border-radius: 8px;">
                    <span style="font-weight: bold;">${player.char_name}</span>
                    <span style="color: #ffd700; font-weight: bold;">🪙 ${player.gold.toLocaleString()}</span>
                </div>
            `;
        });
        
        html += '</div>';
        playerList.innerHTML = html;
    } else {
        playerList.innerHTML = '<div style="text-align: center; color: #888;">暂无角色数据</div>';
    }
    
    document.getElementById('queryGoldResult').style.display = 'block';
}

function switchAdminTab(tabName) {
    document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.admin-tab-content').forEach(c => c.style.display = 'none');
    document.querySelector(`.admin-tab[data-tab="${tabName}"]`).classList.add('active');
    document.getElementById(`${tabName}Tab`).style.display = 'block';
}

function initIconPickers() {
    document.querySelectorAll('.icon-picker-container').forEach(container => {
        const input = container.querySelector('input');
        const picker = container.querySelector('.icon-picker');
        if (!input || !picker) return;
        
        input.onclick = (e) => {
            e.stopPropagation();
            e.preventDefault();
            const isShow = picker.classList.contains('show');
            document.querySelectorAll('.icon-picker').forEach(p => p.classList.remove('show'));
            if (!isShow) {
                picker.classList.add('show');
            }
        };
    });
}

function handleIconClick(e) {
    if (!e.target.classList.contains('icon-option')) return;
    e.stopPropagation();
    e.preventDefault();
    
    const picker = e.target.closest('.icon-picker');
    const container = picker?.closest('.icon-picker-container');
    const input = container?.querySelector('input');
    if (!picker || !input) return;
    
    const icon = e.target.dataset.icon;
    input.value = icon;
    picker.querySelectorAll('.icon-option').forEach(o => o.classList.remove('selected'));
    e.target.classList.add('selected');
    picker.classList.remove('show');
}

function closeAllPickers(e) {
    if (e && e.target.closest('.icon-picker-container')) return;
    document.querySelectorAll('.icon-picker').forEach(picker => {
        picker.classList.remove('show');
    });
}

document.addEventListener('DOMContentLoaded', function() {
    initShop();
    
    document.addEventListener('click', closeAllPickers, true);
    document.addEventListener('click', handleIconClick, true);

    document.getElementById('queryGoldBtn').addEventListener('click', openQueryGoldModal);
    document.getElementById('closeQueryGoldModal').addEventListener('click', closeQueryGoldModal);
    document.getElementById('closeImagePreviewModal').addEventListener('click', closeImagePreviewModal);
    
    document.getElementById('imagePreviewModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeImagePreviewModal();
        }
    });
    
    document.getElementById('queryGoldSearch').addEventListener('input', (e) => {
        const searchTerm = e.target.value.toLowerCase().trim();
        if (!searchTerm) {
            renderQueryGoldPlayers(queryGoldPlayers);
        } else {
            const filtered = queryGoldPlayers.filter(player => 
                player.char_name.toLowerCase().includes(searchTerm)
            );
            renderQueryGoldPlayers(filtered);
        }
    });
    document.getElementById('adminBtn').addEventListener('click', openAdminModal);
    document.getElementById('closeAdminModal').addEventListener('click', closeAdminModal);
    document.getElementById('addCategoryBtn').addEventListener('click', handleCategorySubmit);
    document.getElementById('saveProductBtn').addEventListener('click', saveProduct);
    document.getElementById('cancelEditBtn').addEventListener('click', () => {
        clearProductForm();
    });
    
    const cancelCategoryEditBtn = document.getElementById('cancelCategoryEditBtn');
    if (cancelCategoryEditBtn) {
        cancelCategoryEditBtn.addEventListener('click', () => {
            resetCategoryForm();
        });
    }

    document.getElementById('uploadProductImageBtn').addEventListener('click', () => {
        document.getElementById('productImageUpload').click();
    });

    document.getElementById('productImageUpload').addEventListener('change', async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg', 'image/gif', 'image/webp', 'image/bmp'];
        if (!allowedTypes.includes(file.type)) {
            alert('只支持 PNG、JPEG、GIF、WebP、BMP 格式的图片');
            return;
        }

        const formData = new FormData();
        formData.append('image', file);

        try {
            const response = await fetch('/api/shop/admin/product/upload', {
                method: 'POST',
                credentials: 'same-origin',
                body: formData
            });
            const data = await response.json();

            if (data.success) {
                document.getElementById('productImage').value = data.filename;
                const preview = document.getElementById('productImagePreview');
                preview.innerHTML = `<img src="/api/shop/image/${shopUsername}/${data.filename}?size=100x100" alt="商品图片" style="max-width: 100px; max-height: 100px;">`;
            } else {
                alert(data.message || '上传失败');
            }
        } catch (error) {
            console.error('上传图片失败:', error);
            alert('上传失败，请重试');
        }
    });

    document.querySelectorAll('.admin-tab').forEach(tab => {
        tab.addEventListener('click', () => switchAdminTab(tab.dataset.tab));
    });

    document.getElementById('queryGoldModal').addEventListener('click', (e) => {
        if (e.target === document.getElementById('queryGoldModal')) {
            closeQueryGoldModal();
        }
    });
});

function initLazyLoad() {
    const lazyImages = document.querySelectorAll('.lazy-load');
    
    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                const src = img.dataset.src;
                
                if (src) {
                    img.src = src;
                    img.classList.remove('lazy-load');
                    observer.unobserve(img);
                }
            }
        });
    }, {
        rootMargin: '50px 0px',
        threshold: 0.1
    });
    
    lazyImages.forEach(img => {
        imageObserver.observe(img);
    });
}

function shareShop() {
    const shareUrl = window.location.href;
    
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(shareUrl).then(() => {
            showShareToast();
        }).catch(err => {
            console.error('复制失败:', err);
            fallbackCopy(shareUrl);
        });
    } else {
        fallbackCopy(shareUrl);
    }
}

function fallbackCopy(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
        document.execCommand('copy');
        showShareToast();
    } catch (err) {
        console.error('复制失败:', err);
        alert('复制失败，请手动复制链接');
    }
    
    document.body.removeChild(textArea);
}

function showShareToast() {
    const toast = document.getElementById('shareToast');
    toast.classList.add('show');
    
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('shareBtn').addEventListener('click', shareShop);
});

function initLazyLoad() {
    const lazyImages = document.querySelectorAll('.lazy-load');
    
    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                const src = img.dataset.src;
                
                if (src) {
                    img.src = src;
                    img.classList.remove('lazy-load');
                    observer.unobserve(img);
                }
            }
        });
    }, {
        rootMargin: '50px 0px',
        threshold: 0.1
    });
    
    lazyImages.forEach(img => {
        imageObserver.observe(img);
    });
}

function openProductDetailModal(itemId) {
    const item = items.find(i => i.id === itemId);
    if (!item) return;

    document.getElementById('detailProductName').textContent = item.name;

    const imageContainer = document.getElementById('detailProductImage');
    if (item.image) {
        const fullImageUrl = `/api/shop/image/${shopUsername}/${item.image}`;
        imageContainer.innerHTML = `<img src="${fullImageUrl}" alt="${item.name}" id="detailMainImage">`;
        setTimeout(() => {
            const detailImg = document.getElementById('detailMainImage');
            if (detailImg) {
                detailImg.addEventListener('click', () => {
                    openImagePreview(fullImageUrl);
                });
                detailImg.style.cursor = 'pointer';
            }
        }, 0);
    } else {
        imageContainer.innerHTML = `<span class="item-icon">${item.icon || '📦'}</span>`;
    }

    document.getElementById('detailProductPrice').innerHTML = `<span>🪙</span><span>${item.price.toLocaleString()}</span>`;
    document.getElementById('detailProductDesc').textContent = item.desc || '暂无描述';

    document.getElementById('productDetailModal').style.display = 'flex';
}

function closeProductDetailModal() {
    document.getElementById('productDetailModal').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('shareBtn').addEventListener('click', shareShop);
    document.getElementById('closeProductDetailModal').addEventListener('click', closeProductDetailModal);
    
    document.getElementById('productDetailModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeProductDetailModal();
        }
    });
});
