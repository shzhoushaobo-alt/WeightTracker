const CACHE_NAME = "weight-tracker-pwa-v2";
const ASSETS = [
  "./",
  "./index.html",
  "./styles.css",
  "./app.js",
  "./manifest.webmanifest",
  "./icons/icon-192.svg",
  "./icons/icon-512.svg",
  "https://cdn.jsdelivr.net/npm/chart.js@4.4.3/dist/chart.umd.min.js",
  "https://cdn.jsdelivr.net/npm/xlsx@0.18.5/dist/xlsx.full.min.js"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
      )
    ).then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  const url = new URL(event.request.url);
  const sameOrigin = url.origin === self.location.origin;
  const isCoreAsset =
    sameOrigin &&
    (url.pathname.endsWith("/index.html") ||
      url.pathname.endsWith("/styles.css") ||
      url.pathname.endsWith("/app.js"));

  event.respondWith(
    (async () => {
      if (isCoreAsset) {
        // Network-first for fast style/code updates during development.
        try {
          const fresh = await fetch(event.request);
          if (fresh && fresh.status === 200) {
            const copy = fresh.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
          }
          return fresh;
        } catch {
          return (await caches.match(event.request)) || (await caches.match("./index.html"));
        }
      }

      const cached = await caches.match(event.request);
      if (cached) return cached;
      try {
        const res = await fetch(event.request);
        if (res && res.status === 200) {
          const copy = res.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, copy));
        }
        return res;
      } catch {
        return caches.match("./index.html");
      }
    })()
  );
});
