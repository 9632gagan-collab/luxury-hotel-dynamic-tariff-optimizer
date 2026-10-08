/**
 * Royal Indian Stays - PWA Service Worker
 * Safe Caching Strategy:
 * - NEVER caches sensitive admin/staff/guest/booking/api routes.
 * - Stale-while-revalidate only for public static assets (CSS, JS, logo).
 * - Network-only for all dynamic and authenticated operations.
 */

const CACHE_NAME = 'royal-stays-v1';
const STATIC_ASSETS = [
  '/',
  '/css/style.css',
  '/js/logo-animation.js',
  '/js/luxury-tab-motion.js',
  '/images/logo.png',
  '/manifest.json'
];

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(STATIC_ASSETS).catch((err) => {
        console.warn('PWA: Static asset pre-cache skipped', err);
      });
    })
  );
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) => {
      return Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            return caches.delete(key);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);

  // Exclude non-GET requests, POST requests, and sensitive endpoints
  if (
    request.method !== 'GET' ||
    url.pathname.startsWith('/admin') ||
    url.pathname.startsWith('/staff') ||
    url.pathname.startsWith('/guest') ||
    url.pathname.startsWith('/bookings') ||
    url.pathname.startsWith('/login') ||
    url.pathname.startsWith('/logout') ||
    url.pathname.startsWith('/api') ||
    url.pathname.startsWith('/demo') ||
    url.pathname.startsWith('/health')
  ) {
    // Network only - Never store sensitive or dynamic session content in client cache
    return;
  }

  // Cache static assets (CSS, JS, images, fonts) with stale-while-revalidate
  if (
    url.pathname.startsWith('/css/') ||
    url.pathname.startsWith('/js/') ||
    url.pathname.startsWith('/images/') ||
    url.pathname === '/manifest.json'
  ) {
    event.respondWith(
      caches.match(request).then((cachedResponse) => {
        const fetchPromise = fetch(request).then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
          }
          return networkResponse;
        }).catch(() => cachedResponse);

        return cachedResponse || fetchPromise;
      })
    );
    return;
  }

  // Network first for HTML navigation
  event.respondWith(
    fetch(request).catch(() => caches.match(request))
  );
});
