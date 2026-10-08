/**
 * Royal Indian Stays - Lightweight High-Performance Logo Animation Engine
 * 
 * Architecture & Per-Page Lifecycle:
 * 1. Home Page: Plays once on initial entrance, locked timing/appearance.
 *    Returning to Home keeps the final frame and does NOT replay.
 * 2. Non-Home Pages (Registration, Login, Guest, Staff, Admin, etc.):
 *    When a non-Home page is opened:
 *    - First frame appears immediately (zero blank container).
 *    - Frame-by-frame animation plays once (Frame 0 -> Frame 119).
 *    - Reaches final frame and STOPS permanently.
 *    - Never loops, never replays on hover/mouse/scroll/re-render/in-page tabs.
 * 3. Shared Cache: All pages share the single frame cache (no re-downloading).
 * 4. Immediate Navigation: Halts current playback immediately on navigation.
 *    Never blocks or delays navigation.
 */
(function () {
    'use strict';

    var TOTAL_FRAMES = 120;
    var FRAME_PATH_PREFIX = '/images/logo-frames/ezgif-frame-';
    var FRAME_EXTENSION = '.jpg';
    var TARGET_FPS = 32; // Smooth luxury cadence (~3.7s total)
    var FRAME_INTERVAL = 1000 / TARGET_FPS;

    // Singleton global cache for frame assets shared across all pages
    if (!window.__royalLogoCache) {
        window.__royalLogoCache = {
            images: new Array(TOTAL_FRAMES),
            loaded: new Array(TOTAL_FRAMES).fill(false),
            loadedCount: 0,
            activeCanvases: new Set(),
            activeDownloads: 0,
            maxConcurrent: 2
        };
    }
    var cache = window.__royalLogoCache;

    // Page-level animation state controller
    var pageAnimationId = null;
    var isPageAnimationRunning = false;
    var pageAnimationStartTime = null;
    var lastAnimatedPath = null;
    var pageAnimationDoneForPath = {};

    function getFrameUrl(index) {
        var frameNum = String(index + 1).padStart(3, '0');
        return FRAME_PATH_PREFIX + frameNum + FRAME_EXTENSION;
    }

    function isHomePage() {
        var p = window.location.pathname || '';
        return p === '/' || p === '' || p.endsWith('/index') || p.endsWith('/index.html');
    }

    // Stream single frame safely
    function loadSingleFrame(index, callback) {
        if (cache.images[index] || cache.loaded[index]) {
            if (callback) callback();
            return;
        }

        cache.activeDownloads++;
        var img = new Image();
        img.decoding = 'async';

        img.onload = function () {
            cache.images[index] = img;
            cache.loaded[index] = true;
            cache.loadedCount++;
            cache.activeDownloads--;

            if (callback) callback();
        };

        img.onerror = function () {
            cache.activeDownloads--;
            if (callback) callback();
        };

        img.src = getFrameUrl(index);
    }

    // Gentle progressive lookahead: preload ~4-6 frames ahead of active playback
    function streamNextFrames(currentPlayingIndex) {
        if (!isPageAnimationRunning) return;

        var lookaheadMax = Math.min(currentPlayingIndex + 6, TOTAL_FRAMES - 1);
        for (var i = currentPlayingIndex; i <= lookaheadMax; i++) {
            if (!cache.loaded[i] && !cache.images[i] && cache.activeDownloads < cache.maxConcurrent) {
                loadSingleFrame(i);
            }
        }
    }

    // Find nearest loaded frame for zero flicker
    function getLoadedOrNearest(index) {
        if (cache.loaded[index] && cache.images[index]) {
            return cache.images[index];
        }

        // Look backward
        for (var b = index - 1; b >= 0; b--) {
            if (cache.loaded[b] && cache.images[b]) {
                return cache.images[b];
            }
        }

        // Look forward
        for (var f = index + 1; f < TOTAL_FRAMES; f++) {
            if (cache.loaded[f] && cache.images[f]) {
                return cache.images[f];
            }
        }

        return null;
    }

    // Fast GPU blit to canvas (Zero layout changes, fixed 512x512 buffer)
    function drawFrame(canvas, img) {
        if (!canvas || !img || !img.complete) return;
        var ctx = canvas.getContext('2d', { alpha: false });
        if (!ctx) return;

        ctx.drawImage(img, 0, 0, 512, 512);
    }

    // Render specified frame to all active canvas elements on the currently visible page
    function renderFrameToAll(frameIndex) {
        var img = getLoadedOrNearest(frameIndex);
        if (!img) return;

        cache.activeCanvases.forEach(function (canvas) {
            if (!document.body.contains(canvas)) {
                cache.activeCanvases.delete(canvas);
                return;
            }
            drawFrame(canvas, img);
        });
    }

    // Stop current animation playback immediately
    function stopCurrentAnimation() {
        isPageAnimationRunning = false;
        if (pageAnimationId) {
            cancelAnimationFrame(pageAnimationId);
            pageAnimationId = null;
        }
        pageAnimationStartTime = null;
    }

    // Run frame-by-frame animation once for the active page
    function runAnimationForCurrentPage(isHome) {
        stopCurrentAnimation();
        isPageAnimationRunning = true;
        pageAnimationStartTime = null;

        // Display the first frame instantly
        renderFrameToAll(0);

        function step(timestamp) {
            if (!isPageAnimationRunning) return;

            if (pageAnimationStartTime === null) {
                pageAnimationStartTime = timestamp;
            }

            var elapsed = timestamp - pageAnimationStartTime;
            var frameIdx = Math.floor(elapsed / FRAME_INTERVAL);

            if (frameIdx >= TOTAL_FRAMES - 1) {
                // Final frame reached: render final frame 120 and STOP permanently
                frameIdx = TOTAL_FRAMES - 1;
                renderFrameToAll(frameIdx);
                isPageAnimationRunning = false;
                pageAnimationId = null;

                if (isHome) {
                    window.__homeLogoAnimationDone = true;
                } else {
                    pageAnimationDoneForPath[window.location.pathname || '/'] = true;
                }
                return;
            }

            renderFrameToAll(frameIdx);
            streamNextFrames(frameIdx);

            pageAnimationId = requestAnimationFrame(step);
        }

        pageAnimationId = requestAnimationFrame(step);
    }

    // Register a canvas element
    function registerLogoCanvas(canvas) {
        if (!canvas) return;

        // Ensure canvas has fixed native resolution buffer once (no layout thrashing)
        if (canvas.width !== 512 || canvas.height !== 512) {
            canvas.width = 512;
            canvas.height = 512;
        }

        cache.activeCanvases.add(canvas);

        var isHome = isHomePage();
        var currentPath = window.location.pathname || '/';

        // Check if animation for this page has already finished
        if ((isHome && window.__homeLogoAnimationDone) || (!isHome && pageAnimationDoneForPath[currentPath])) {
            // Already finished: render final frame statically
            var finalImg = getLoadedOrNearest(TOTAL_FRAMES - 1);
            if (finalImg) {
                drawFrame(canvas, finalImg);
            } else {
                loadSingleFrame(TOTAL_FRAMES - 1, function () {
                    drawFrame(canvas, cache.images[TOTAL_FRAMES - 1]);
                });
            }
        } else {
            // Render first frame immediately so logo is never blank
            var firstImg = getLoadedOrNearest(0);
            if (firstImg) {
                drawFrame(canvas, firstImg);
            } else {
                loadSingleFrame(0, function () {
                    drawFrame(canvas, cache.images[0]);
                });
            }
        }
    }

    // Find and register all logo canvases on the page
    function discoverLogos() {
        // Clean out detached canvases
        cache.activeCanvases.forEach(function (canvas) {
            if (!document.body.contains(canvas)) {
                cache.activeCanvases.delete(canvas);
            }
        });

        var canvases = document.querySelectorAll('canvas.royal-animated-logo, canvas.brand-logo-img, canvas.hero-logo-img, canvas.auth-logo-img, canvas.footer-logo-img');
        canvases.forEach(function (c) {
            registerLogoCanvas(c);
        });

        // Upgrade any remaining static img elements seamlessly
        var imgs = document.querySelectorAll('img.brand-logo-img, img.hero-logo-img');
        imgs.forEach(function (img) {
            var c = document.createElement('canvas');
            c.width = 512;
            c.height = 512;
            c.className = img.className + ' royal-animated-logo';
            var s = img.getAttribute('style');
            if (s) c.setAttribute('style', s);
            c.setAttribute('role', 'img');
            c.setAttribute('aria-label', img.alt || 'Royal Indian Stays Logo');
            if (img.parentNode) {
                img.parentNode.replaceChild(c, img);
                registerLogoCanvas(c);
            }
        });
    }

    // Public API for page entrance & re-entry
    window.__initRoyalLogoAnimation = function () {
        discoverLogos();

        var currentPath = window.location.pathname || '/';
        var isHome = isHomePage();

        if (isHome) {
            // Home Page behavior (LOCKED)
            if (window.__homeLogoAnimationDone) {
                // Home logo animation already played: do NOT make it run again
                renderFrameToAll(TOTAL_FRAMES - 1);
            } else if (!isPageAnimationRunning) {
                // First entrance on Home: play once
                runAnimationForCurrentPage(true);
            }
            lastAnimatedPath = currentPath;
            return;
        }

        // NON-HOME PAGES (Registration, Login, Guest, Staff, Admin, etc.)
        // When opening an existing page that contains the logo:
        // FIRST FRAME APPEARS -> ANIMATION PLAYS ONCE -> FINAL FRAME -> STOP
        if (currentPath !== lastAnimatedPath) {
            // Different page opened: play its animation once!
            lastAnimatedPath = currentPath;
            pageAnimationDoneForPath[currentPath] = false;
            runAnimationForCurrentPage(false);
        } else if (pageAnimationDoneForPath[currentPath]) {
            // Same page (e.g. in-page role tabs, re-renders, hovers): keep final frame
            renderFrameToAll(TOTAL_FRAMES - 1);
        } else if (!isPageAnimationRunning) {
            runAnimationForCurrentPage(false);
        }
    };

    // Public API for page leave / navigation start
    window.__stopRoyalLogoAnimation = function () {
        stopCurrentAnimation();
        // Render final frame on leaving canvases so there are no partial frames left
        renderFrameToAll(TOTAL_FRAMES - 1);
    };

    // Initialize on DOM ready
    function init() {
        discoverLogos();

        // Load initial frame 0 and final frame 119
        loadSingleFrame(0, function () {
            renderFrameToAll(0);
            window.__initRoyalLogoAnimation();
        });
        loadSingleFrame(TOTAL_FRAMES - 1);

        // Fallback timer: ensure animation starts within 150ms even if network delayed
        setTimeout(function () {
            if (!isPageAnimationRunning) {
                window.__initRoyalLogoAnimation();
            }
        }, 150);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
