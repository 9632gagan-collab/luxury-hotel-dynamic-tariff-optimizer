/**
 * Royal Indian Stays - Home Page Scroll-Driven Cinematic Background Animation
 * 
 * Features:
 * - Direct scroll-driven cinematic progression through 300 authentic luxury hotel frames.
 * - Persistent session-wide image cache ensuring instant re-entry without redundant network requests.
 * - Comprehensive lifecycle management (init / destroy) for clean client-side SPA navigation.
 * - Inertial smoothing (lerp) for silky luxury motion without jitter or sudden jumps.
 * - Sub-frame dual-buffer cross-fading for seamless transitions between adjacent frames.
 * - Mathematical object-fit: cover rendering preserving 16:9 aspect ratio without distortion.
 * - Multi-tiered preloader (instant frame 1, priority head frames, keyframes, idle fill-in).
 * - Nearest-frame fallback ensuring zero flickering or blank frames during rapid scrolling.
 * - Retina/HiDPI display support with devicePixelRatio capping for 60fps performance.
 */
(function () {
    'use strict';

    const TOTAL_FRAMES = 300;
    const FRAME_PATH_PREFIX = '/images/background-frames/ezgif-frame-';
    const FRAME_EXTENSION = '.jpg';
    const LERP_FACTOR = 0.085; // Damping factor for slow, controlled luxury motion

    // Session-wide frame cache: persists across client-side page switches in memory
    window.__homeBgCache = window.__homeBgCache || {
        images: new Array(TOTAL_FRAMES),
        loaded: new Array(TOTAL_FRAMES).fill(false),
        loadedCount: 0,
        loadingIndices: new Set(),
        pendingIndices: new Set(),
        pendingQueue: []
    };
    const cache = window.__homeBgCache;
    if (!cache.pendingIndices) {
        cache.pendingIndices = new Set();
    }

    // Instance lifecycle references
    let canvas = null;
    let container = null;
    let ctx = null;

    let targetFrame = 0;
    let currentFrame = 0;
    let isAnimating = false;
    let animFrameId = null;

    let lastRenderedFloor = -1;
    let lastRenderedCeil = -1;
    let lastRenderedFraction = -1;

    let boundScrollHandler = null;
    let boundResizeHandler = null;
    let scrollObserver = null;
    let scrollDirListener = null;
    let luxuryTimeouts = [];

    let isQueuePaused = false;
    let progressiveStarted = false;

    // Helper: format frame index to 3-digit string (0 -> "001", 299 -> "300")
    function getFrameUrl(index) {
        const frameNum = String(index + 1).padStart(3, '0');
        return FRAME_PATH_PREFIX + frameNum + FRAME_EXTENSION;
    }

    function loadImage(index, priority = false) {
        if (index < 0 || index >= TOTAL_FRAMES) return;
        if (cache.images[index] || cache.loaded[index] || cache.loadingIndices.has(index)) {
            return;
        }

        if (!priority && cache.pendingIndices.has(index)) {
            return;
        }

        const img = new Image();
        img.decoding = 'async';

        img.onload = function () {
            cache.images[index] = img;
            cache.loaded[index] = true;
            cache.loadingIndices.delete(index);
            cache.pendingIndices.delete(index);
            cache.loadedCount++;

            if (container && (index === 0 || cache.loadedCount === 1)) {
                container.classList.add('loaded');
                render();
            } else if (Math.abs(currentFrame - index) < 2) {
                render();
            }

            // Once frame 0 is rendered, start progressive loading of remaining frames gently
            if (index === 0 && !progressiveStarted) {
                progressiveStarted = true;
                startProgressiveLoading();
            }

            processNextInQueue();
        };

        img.onerror = function () {
            cache.loadingIndices.delete(index);
            cache.pendingIndices.delete(index);
            processNextInQueue();
        };

        if (priority) {
            cache.loadingIndices.add(index);
            img.src = getFrameUrl(index);
        } else {
            cache.pendingIndices.add(index);
            cache.pendingQueue.push({ index: index, img: img });
            processNextInQueue();
        }
    }

    // Keep concurrency capped at 2 to preserve browser HTTP connection slots for navigation & APIs
    const MAX_CONCURRENT_LOADS = 2;
    function processNextInQueue() {
        if (isQueuePaused || !isHomePage()) {
            return;
        }
        while (cache.loadingIndices.size < MAX_CONCURRENT_LOADS && cache.pendingQueue.length > 0) {
            cache.pendingQueue.sort((a, b) => Math.abs(a.index - targetFrame) - Math.abs(b.index - targetFrame));
            const item = cache.pendingQueue.shift();
            if (item) {
                cache.pendingIndices.delete(item.index);
                if (!cache.loaded[item.index] && !cache.loadingIndices.has(item.index)) {
                    cache.loadingIndices.add(item.index);
                    item.img.src = getFrameUrl(item.index);
                }
            }
        }
    }

    // Cooperative idle background loader: loads frames progressively without blocking UI or navigation
    function startProgressiveLoading() {
        if (isQueuePaused || !isHomePage()) return;

        // 1. Immediate head cluster: frames 1 to 5
        for (let i = 1; i <= 5 && i < TOTAL_FRAMES; i++) {
            if (!cache.loaded[i] && !cache.images[i] && !cache.pendingIndices.has(i)) {
                loadImage(i, false);
            }
        }

        // 2. Cooperative scheduling for keyframes (every 10th frame: 10, 20, 30... 290)
        const scheduleKeyframes = function () {
            if (isQueuePaused || !isHomePage()) return;
            for (let i = 10; i < TOTAL_FRAMES; i += 10) {
                if (!cache.loaded[i] && !cache.images[i] && !cache.pendingIndices.has(i)) {
                    loadImage(i, false);
                }
            }
        };

        // 3. Idle scheduling for remaining intermediate frames
        const scheduleRemaining = function () {
            if (isQueuePaused || !isHomePage()) return;
            for (let i = 6; i < TOTAL_FRAMES; i++) {
                if (i % 10 !== 0 && !cache.loaded[i] && !cache.images[i] && !cache.pendingIndices.has(i)) {
                    loadImage(i, false);
                }
            }
        };

        if ('requestIdleCallback' in window) {
            window.requestIdleCallback(function () {
                scheduleKeyframes();
                window.requestIdleCallback(scheduleRemaining, { timeout: 3000 });
            }, { timeout: 1500 });
        } else {
            setTimeout(scheduleKeyframes, 200);
            setTimeout(scheduleRemaining, 800);
        }
    }

    // Find the closest loaded frame to prevent flickering
    function getLoadedOrNearest(index) {
        if (cache.loaded[index] && cache.images[index]) {
            return cache.images[index];
        }

        for (let offset = 1; offset < TOTAL_FRAMES; offset++) {
            const prev = index - offset;
            if (prev >= 0 && cache.loaded[prev] && cache.images[prev]) {
                return cache.images[prev];
            }
            const next = index + offset;
            if (next < TOTAL_FRAMES && cache.loaded[next] && cache.images[next]) {
                return cache.images[next];
            }
        }
        return null;
    }

    // Mathematical cover-style image drawing preserving original 16:9 aspect ratio
    function drawCoverImage(img, alpha) {
        if (!img || !img.complete || img.naturalWidth === 0 || !canvas || !ctx) return;

        const cw = canvas.width;
        const ch = canvas.height;
        const iw = img.naturalWidth || 1920;
        const ih = img.naturalHeight || 1080;

        const canvasRatio = cw / ch;
        const imgRatio = iw / ih;

        let sw, sh, sx, sy;

        if (canvasRatio > imgRatio) {
            sw = iw;
            sh = iw / canvasRatio;
            sx = 0;
            sy = (ih - sh) / 2;
        } else {
            sh = ih;
            sw = ih * canvasRatio;
            sx = (iw - sw) / 2;
            sy = 0;
        }

        ctx.save();
        ctx.globalAlpha = alpha;
        ctx.drawImage(img, sx, sy, sw, sh, 0, 0, cw, ch);
        ctx.restore();
    }

    // Render current frame with dual-frame alpha blending
    function render() {
        if (!canvas || !ctx) return;

        const floorIdx = Math.floor(currentFrame);
        const ceilIdx = Math.min(floorIdx + 1, TOTAL_FRAMES - 1);
        const fraction = currentFrame - floorIdx;

        if (floorIdx === lastRenderedFloor &&
            ceilIdx === lastRenderedCeil &&
            Math.abs(fraction - lastRenderedFraction) < 0.005) {
            return;
        }

        const imgA = getLoadedOrNearest(floorIdx);
        const imgB = (floorIdx !== ceilIdx) ? getLoadedOrNearest(ceilIdx) : null;

        if (!imgA && !imgB) return;

        ctx.clearRect(0, 0, canvas.width, canvas.height);

        if (imgA) {
            drawCoverImage(imgA, 1.0);
        }

        if (imgB && imgB !== imgA && fraction > 0.01) {
            drawCoverImage(imgB, fraction);
        }

        lastRenderedFloor = floorIdx;
        lastRenderedCeil = ceilIdx;
        lastRenderedFraction = fraction;
    }

    // Main animation loop
    function animate() {
        const diff = targetFrame - currentFrame;

        if (Math.abs(diff) > 0.001) {
            currentFrame += diff * LERP_FACTOR;
            render();
            animFrameId = requestAnimationFrame(animate);
        } else {
            currentFrame = targetFrame;
            render();
            isAnimating = false;
            animFrameId = null;
        }
    }

    function triggerAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            animFrameId = requestAnimationFrame(animate);
        }
    }

    // Scroll progress handler
    function updateScrollProgress() {
        const scrollY = window.pageYOffset || document.documentElement.scrollTop || 0;
        const maxScroll = Math.max(document.documentElement.scrollHeight - window.innerHeight, 1);
        const progress = Math.min(Math.max(scrollY / maxScroll, 0), 1);

        targetFrame = progress * (TOTAL_FRAMES - 1);
        triggerAnimation();

        preloadProximityFrames(Math.round(targetFrame));
    }

    function preloadProximityFrames(centerIdx) {
        if (isQueuePaused || !isHomePage()) return;
        const range = 6;
        for (let i = Math.max(0, centerIdx - range); i <= Math.min(TOTAL_FRAMES - 1, centerIdx + range); i++) {
            if (!cache.loaded[i] && !cache.images[i] && !cache.loadingIndices.has(i)) {
                if (cache.loadingIndices.size < MAX_CONCURRENT_LOADS) {
                    loadImage(i, true);
                } else if (!cache.pendingIndices.has(i)) {
                    loadImage(i, false);
                }
            }
        }
    }

    // Canvas resize handler
    function handleResize() {
        if (!canvas) return;

        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        const w = window.innerWidth;
        const h = window.innerHeight;

        const targetW = Math.round(w * dpr);
        const targetH = Math.round(h * dpr);

        if (canvas.width !== targetW || canvas.height !== targetH) {
            canvas.width = targetW;
            canvas.height = targetH;
            lastRenderedFloor = -1;
            lastRenderedCeil = -1;
            lastRenderedFraction = -1;
            render();
        }
    }

    /* =========================================================================
       Luxury Initial-Load & Bidirectional Scroll-Reveal Orchestrator
       ========================================================================= */
    function initLuxuryMotionOrchestrator() {
        document.documentElement.classList.add('js-ready');

        // Clear any previous scheduled reveal timeouts
        luxuryTimeouts.forEach(t => clearTimeout(t));
        luxuryTimeouts = [];

        const initialScrollY = window.pageYOffset || document.documentElement.scrollTop || 0;

        // Container loaded state
        luxuryTimeouts.push(setTimeout(function () {
            if (container && !container.classList.contains('loaded')) {
                container.classList.add('loaded');
            }
        }, 80));

        // Staggered reveal for hero elements
        const heroSection = document.querySelector('.luxury-load-hero');
        const heroBadge = document.querySelector('.hero-badge-item');
        const heroLogo = document.querySelector('.hero-logo-item');
        const heroTitle = document.querySelector('.hero-title-item');
        const heroSubtitle = document.querySelector('.hero-subtitle-item');
        const heroActions = document.querySelector('.hero-actions-item');
        const spotlightBanner = document.querySelector('.karnataka-spotlight');

        // Clean previous states to allow re-trigger on re-entry
        [heroSection, heroBadge, heroLogo, heroTitle, heroSubtitle, heroActions, spotlightBanner].forEach(function (el) {
            if (el) {
                el.classList.remove('luxury-revealed');
                void el.offsetWidth; // Force reflow
            }
        });

        luxuryTimeouts.push(setTimeout(function () {
            if (heroSection) heroSection.classList.add('luxury-revealed');
            if (heroBadge) heroBadge.classList.add('luxury-revealed');
            if (heroLogo) heroLogo.classList.add('luxury-revealed');
            if (heroTitle) heroTitle.classList.add('luxury-revealed');
        }, 150));

        luxuryTimeouts.push(setTimeout(function () {
            if (heroSubtitle) heroSubtitle.classList.add('luxury-revealed');
            if (heroActions) heroActions.classList.add('luxury-revealed');
        }, 320));

        luxuryTimeouts.push(setTimeout(function () {
            [heroSection, heroBadge, heroLogo, heroTitle, heroSubtitle, heroActions].forEach(function(el) {
                if (el && !el.classList.contains('luxury-revealed')) {
                    el.classList.add('luxury-revealed');
                }
            });
        }, 380));

        luxuryTimeouts.push(setTimeout(function () {
            if (spotlightBanner) {
                spotlightBanner.classList.add('luxury-revealed');
                spotlightBanner.classList.add('luxury-in-view');
            }
            const initialCards = document.querySelectorAll('.destinations-grid .destination-card');
            initialCards.forEach(function (card, idx) {
                const rect = card.getBoundingClientRect();
                if (rect.top < window.innerHeight * 0.95) {
                    luxuryTimeouts.push(setTimeout(function () {
                        card.classList.add('luxury-in-view');
                    }, idx * 70));
                }
            });
        }, 500));

        luxuryTimeouts.push(setTimeout(function () {
            document.body.classList.add('luxury-settled');
            if (heroSection) heroSection.classList.add('luxury-revealed');
            if (heroActions) heroActions.classList.add('luxury-revealed');
            if (spotlightBanner) spotlightBanner.classList.add('luxury-in-view');
        }, 1200));

        // Bidirectional Scroll Reveal System
        const scrollElements = document.querySelectorAll('.luxury-scroll-reveal');
        let lastScrollY = initialScrollY;
        let isScrollingUp = false;

        if (scrollDirListener) {
            window.removeEventListener('scroll', scrollDirListener);
        }
        scrollDirListener = function () {
            const currentY = window.pageYOffset || document.documentElement.scrollTop || 0;
            isScrollingUp = currentY < lastScrollY - 2;
            lastScrollY = currentY;
        };
        window.addEventListener('scroll', scrollDirListener, { passive: true });

        if (scrollObserver) {
            scrollObserver.disconnect();
            scrollObserver = null;
        }

        if ('IntersectionObserver' in window) {
            scrollObserver = new IntersectionObserver(function (entries) {
                entries.forEach(function (entry) {
                    const el = entry.target;
                    const rect = entry.boundingClientRect;

                    if (entry.isIntersecting) {
                        el.classList.add('luxury-in-view');
                    } else {
                        if (isScrollingUp && rect.top > window.innerHeight * 0.9) {
                            el.classList.remove('luxury-in-view');
                        }
                    }
                });
            }, {
                root: null,
                rootMargin: '0px 0px -40px 0px',
                threshold: [0, 0.15]
            });

            scrollElements.forEach(function (el) {
                const rect = el.getBoundingClientRect();
                if (rect.top < window.innerHeight * 0.85) {
                    el.classList.add('luxury-in-view');
                }
                scrollObserver.observe(el);
            });
        } else {
            scrollElements.forEach(function (el) {
                el.classList.add('luxury-in-view');
            });
        }
    }

    // Proper cleanup of active background animation instance when leaving Home
    function destroy() {
        isQueuePaused = true;

        if (animFrameId) {
            cancelAnimationFrame(animFrameId);
            animFrameId = null;
        }
        isAnimating = false;

        if (boundResizeHandler) {
            window.removeEventListener('resize', boundResizeHandler);
            boundResizeHandler = null;
        }
        if (boundScrollHandler) {
            window.removeEventListener('scroll', boundScrollHandler);
            boundScrollHandler = null;
        }
        if (scrollDirListener) {
            window.removeEventListener('scroll', scrollDirListener);
            scrollDirListener = null;
        }
        if (scrollObserver) {
            scrollObserver.disconnect();
            scrollObserver = null;
        }

        luxuryTimeouts.forEach(t => clearTimeout(t));
        luxuryTimeouts = [];

        const layer = document.getElementById('homeBgAnimLayer');
        if (layer) {
            layer.style.display = 'none';
        }
    }

    // Proper initialization & re-initialization when entering Home
    function init() {
        // 1. Clean up any previous running instance to avoid duplicate listeners or loops
        destroy();

        // 2. Ensure container and canvas exist in the DOM
        container = document.getElementById('homeBgAnimLayer');
        canvas = document.getElementById('homeBgCanvas');

        if (!container) {
            container = document.createElement('div');
            container.id = 'homeBgAnimLayer';
            container.className = 'home-bg-anim-layer';
            container.setAttribute('aria-hidden', 'true');

            canvas = document.createElement('canvas');
            canvas.id = 'homeBgCanvas';

            const scrim = document.createElement('div');
            scrim.className = 'home-bg-scrim';

            container.appendChild(canvas);
            container.appendChild(scrim);

            const header = document.querySelector('header');
            if (header && header.parentNode) {
                header.parentNode.insertBefore(container, header);
            } else {
                document.body.insertBefore(container, document.body.firstChild);
            }
        } else if (!canvas) {
            canvas = document.createElement('canvas');
            canvas.id = 'homeBgCanvas';
            container.insertBefore(canvas, container.firstChild);
        }

        container.style.display = '';

        ctx = canvas.getContext('2d', { alpha: true });
        if (!ctx) return;

        // Reset animation state cleanly for Home entry
        targetFrame = 0;
        currentFrame = 0;
        isAnimating = false;
        lastRenderedFloor = -1;
        lastRenderedCeil = -1;
        lastRenderedFraction = -1;

        // Ensure canvas is properly sized
        handleResize();

        // Attach listeners
        boundResizeHandler = handleResize;
        boundScrollHandler = updateScrollProgress;
        window.addEventListener('resize', boundResizeHandler, { passive: true });
        window.addEventListener('scroll', boundScrollHandler, { passive: true });

        // Ensure queue is unpaused on Home entry
        isQueuePaused = false;

        // Show Frame 1 immediately without delay
        if (cache.loaded[0] && cache.images[0]) {
            container.classList.add('loaded');
            render();
            if (!progressiveStarted) {
                progressiveStarted = true;
                startProgressiveLoading();
            } else {
                processNextInQueue();
            }
        } else {
            // Load Frame 0 FIRST and ALONE so it gets 100% network priority
            loadImage(0, true);
        }

        // Synchronize scroll progress on Home entry
        updateScrollProgress();

        // Initialize luxury motion reveal sequence
        initLuxuryMotionOrchestrator();
    }

    // Expose lifecycle hooks immediately on window
    window.initHomeBackgroundAnimation = init;
    window.destroyHomeBackgroundAnimation = destroy;

    function isHomePage() {
        var p = window.location.pathname;
        return (p === '/' || p.endsWith('/index.html')) && document.body.classList.contains('home-page');
    }

    // Auto-run if opened directly on Home
    if (isHomePage()) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', init);
        } else {
            init();
        }
    }
})();
