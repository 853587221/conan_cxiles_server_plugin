let itemConfigData = {};
let itemConfigLoaded = false;

function loadItemConfig() {
    const cacheBuster = '?v=' + Date.now();
    fetch('/Icons_PNG/000000.json' + cacheBuster)
        .then(response => {
            if (!response.ok) {
                throw new Error('HTTP error! status: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (!Array.isArray(data)) {
                throw new Error('Invalid data format');
            }
            itemConfigData = {};
            data.forEach(item => {
                if (item && item.RowName) {
                    itemConfigData[item.RowName] = item;
                }
            });
            itemConfigLoaded = true;
            console.log('物品配置加载成功，共 ' + Object.keys(itemConfigData).length + ' 个物品');
            checkIconCacheStatus();
        })
        .catch(error => {
            console.error('加载物品配置失败:', error);
            itemConfigLoaded = false;
            if (!cacheBuster.includes('retry')) {
                console.log('尝试重新加载物品配置...');
                fetch('/Icons_PNG/000000.json?retry=1')
                    .then(response => response.json())
                    .then(data => {
                        if (Array.isArray(data)) {
                            itemConfigData = {};
                            data.forEach(item => {
                                if (item && item.RowName) {
                                    itemConfigData[item.RowName] = item;
                                }
                            });
                            itemConfigLoaded = true;
                            console.log('物品配置重新加载成功');
                        }
                    })
                    .catch(retryError => {
                        console.error('重新加载物品配置失败:', retryError);
                    });
            }
        });
}

function getItemIconPath(templateId) {
    const item = itemConfigData[templateId];
    if (!item) return null;

    if (item.Icon && item.Icon !== '' && item.Icon !== 'None') {
        return '/Icons_PNG/' + item.Icon + '.png';
    }

    if (item.IconLayers && item.IconLayers !== '' && item.IconLayers !== 'None') {
        return '/Icons_PNG/' + item.IconLayers + '.png';
    }

    return null;
}

function getItemName(templateId) {
    const item = itemConfigData[templateId];
    if (!item) return null;
    return item.Name;
}

function checkIconCacheStatus() {
    if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
        const channel = new MessageChannel();
        channel.port1.onmessage = (event) => {
        };
        navigator.serviceWorker.controller.postMessage({ type: 'GET_CACHE_SIZE' }, [channel.port2]);
    }
}

function clearIconCache() {
    if ('serviceWorker' in navigator && navigator.serviceWorker.controller) {
        const channel = new MessageChannel();
        channel.port1.onmessage = (event) => {
            if (event.data.success) {
                alert('图标缓存已清除，下次加载时会重新从服务器获取');
            }
        };
        navigator.serviceWorker.controller.postMessage({ type: 'CLEAR_ICON_CACHE' }, [channel.port2]);
    } else {
        alert('Service Worker未运行，无法清除缓存');
    }
}

function preloadIcons() {
    if (!itemConfigData || Object.keys(itemConfigData).length === 0) {
        return;
    }

    const uniqueIconPaths = new Set();

    Object.keys(itemConfigData).forEach(templateId => {
        const iconPath = getItemIconPath(templateId);
        if (iconPath) {
            uniqueIconPaths.add(iconPath);
        }
    });

    let loadedCount = 0;
    let failedCount = 0;

    uniqueIconPaths.forEach(iconPath => {
        const img = new Image();
        img.onload = () => {
            loadedCount++;
        };
        img.onerror = () => {
            failedCount++;
        };
        img.src = iconPath;
    });
}

window.preloadIcons = preloadIcons;
window.clearIconCache = clearIconCache;
window.checkIconCacheStatus = checkIconCacheStatus;
