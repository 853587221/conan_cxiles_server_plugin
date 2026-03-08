const CACHE_NAME = 'rcon-icons-v2';
const ICONS_CACHE = 'rcon-icons-data-v2';

self.addEventListener('install', (event) => {
    console.log('[Service Worker] 安装中...');
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            console.log('[Service Worker] 缓存已创建');
            return cache.addAll([]);
        })
    );
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    console.log('[Service Worker] 激活中...');
    event.waitUntil(
        caches.keys().then((cacheNames) => {
            return Promise.all(
                cacheNames.map((cacheName) => {
                    if (cacheName !== CACHE_NAME && cacheName !== ICONS_CACHE) {
                        console.log('[Service Worker] 删除旧缓存:', cacheName);
                        return caches.delete(cacheName);
                    }
                })
            );
        })
    );
    self.clients.claim();
});

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    // 不缓存 000000.json 配置文件，确保每次都获取最新版本
    if (url.pathname === '/Icons_PNG/000000.json') {
        event.respondWith(
            fetch(event.request).then((networkResponse) => {
                return networkResponse;
            }).catch(() => {
                return new Response('配置加载失败', { status: 404 });
            })
        );
        return;
    }

    if (url.pathname.startsWith('/Icons_PNG/')) {
        event.respondWith(
            caches.open(ICONS_CACHE).then((cache) => {
                return cache.match(event.request).then((cachedResponse) => {
                    if (cachedResponse) {
                        console.log('[Service Worker] 从缓存加载:', url.pathname);
                        return cachedResponse;
                    }

                    console.log('[Service Worker] 从网络加载并缓存:', url.pathname);
                    return fetch(event.request).then((networkResponse) => {
                        if (!networkResponse || networkResponse.status !== 200 || networkResponse.type !== 'basic') {
                            return networkResponse;
                        }

                        const responseToCache = networkResponse.clone();
                        cache.put(event.request, responseToCache);
                        return networkResponse;
                    }).catch(() => {
                        return new Response('图标加载失败', { status: 404 });
                    });
                });
            })
        );
    }
});

self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'CLEAR_ICON_CACHE') {
        caches.delete(ICONS_CACHE).then(() => {
            console.log('[Service Worker] 图标缓存已清除');
            event.ports[0].postMessage({ success: true });
        });
    }

    if (event.data && event.data.type === 'GET_CACHE_SIZE') {
        caches.open(ICONS_CACHE).then((cache) => {
            return cache.keys().then((keys) => {
                event.ports[0].postMessage({ count: keys.length });
            });
        });
    }
});
