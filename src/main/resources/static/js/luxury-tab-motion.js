/**
 * Royal Indian Stays - Global Luxury Content Pop-Up & Re-Entry Motion System
 * 
 * Requirements Fulfilled:
 * 1. REAL GLOBAL ENTER/RE-ENTER ANIMATION SYSTEM:
 *    - Triggers every time the user opens new content (tab change, option click, route navigation, scroll).
 * 2. Visual Kinematics:
 *    - Start: opacity 0, transform: translateY(25px) scale(0.95)
 *    - End: opacity 1, transform: translateY(0) scale(1)
 *    - Duration: 550ms - 700ms (620ms) with smooth luxury ease-out cubic-bezier(0.16, 1, 0.3, 1)
 * 3. Actual Content Animates:
 *    - Headings, descriptions, buttons, cards, images, info blocks, tables cascade with subtle stagger.
 * 4. Staggered Content Reveal:
 *    - 80ms - 90ms between sequential cards/options.
 * 5. Re-entry on Scroll-Up:
 *    - Elements entering viewport replay smoothly when scrolling down and up.
 * 6. SPA Seamless Navigation:
 *    - Navigating between existing tabs/pages swaps content and restarts animations on every navigation event.
 */
(function() {
    'use strict';

    var isNavigating = false;
    var scrollObserver = null;
    var activeAnimationTimeouts = [];

    // Filter elements to avoid double-animating parent and child
    function filterTopLevelElements(elements) {
        var set = new Set(elements);
        return Array.from(elements).filter(function(el) {
            var parent = el.parentElement;
            while (parent && parent !== document.body) {
                if (set.has(parent)) {
                    return false;
                }
                parent = parent.parentElement;
            }
            return true;
        });
    }

    // Clear any pending timeouts from previous animation runs
    function clearTimeouts() {
        activeAnimationTimeouts.forEach(function(t) {
            clearTimeout(t);
        });
        activeAnimationTimeouts = [];
    }

    // Trigger the Luxury Pop-Up Reveal Keyframe on an element
    function animateElement(el, delayMs) {
        // Reset classes
        el.classList.remove('luxury-popup-hidden', 'luxury-popup-settled', 'luxury-popup-animating', 'luxury-popup-item', 'is-revealed');
        el.style.animationDelay = (delayMs || 0) + 'ms';

        // Force browser layout flush so keyframe 0% (opacity 0, translateY(25px) scale(0.95)) is guaranteed to be applied
        void el.offsetWidth;

        el.classList.add('luxury-popup-animating');
        // Also support backward-compatible classes
        el.classList.add('luxury-popup-item');
        el.classList.add('is-revealed');

        // Once keyframe finishes, transition cleanly to settled state so hover effects work without lock
        var handleAnimationEnd = function() {
            el.removeEventListener('animationend', handleAnimationEnd);
            el.classList.remove('luxury-popup-animating');
            el.classList.add('luxury-popup-settled');
            el.style.animationDelay = '';
        };

        el.addEventListener('animationend', handleAnimationEnd, { once: true });

        // Fallback timer in case animationend was suppressed (e.g., hidden tab or edge cases)
        var timer = setTimeout(function() {
            handleAnimationEnd();
        }, (delayMs || 0) + 700);
        activeAnimationTimeouts.push(timer);
    }

    // Hide an element that has scrolled out of view or is waiting below viewport
    function hideElement(el) {
        el.classList.remove('luxury-popup-animating', 'luxury-popup-settled', 'luxury-popup-item', 'is-revealed');
        el.classList.add('luxury-popup-hidden');
        el.style.animationDelay = '';
    }

    // Scroll Observer for scroll-down reveal and scroll-up / re-entry
    function initScrollObserver() {
        if (scrollObserver) {
            scrollObserver.disconnect();
        }

        if (!('IntersectionObserver' in window)) return;

        scrollObserver = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                var el = entry.target;
                if (entry.isIntersecting) {
                    // Element entered viewport (scrolling down or scrolling up): replay pop-up reveal!
                    if (!el.classList.contains('luxury-popup-settled') && !el.classList.contains('luxury-popup-animating')) {
                        animateElement(el, 30);
                    }
                } else {
                    // Element left viewport: check if it scrolled past threshold
                    var rect = entry.boundingClientRect;
                    var vh = window.innerHeight || document.documentElement.clientHeight;
                    if (rect.bottom < -20 || rect.top > vh + 20) {
                        // Reset to hidden so when user scrolls back, it replays seamlessly
                        hideElement(el);
                    }
                }
            });
        }, {
            root: null,
            rootMargin: '-10px 0px -10px 0px',
            threshold: 0.05
        });
    }

    // Content Selector for all existing pages, tabs, dashboards, and views
    var CONTENT_SELECTOR = [
        '.page-header > div',
        '.page-header .page-actions',
        '.sub-navbar',
        '.search-hero-bar',
        '#categoryBarContainer',
        '.guest-nav-grid > .guest-nav-card',
        '.quick-nav-grid > .quick-nav-card',
        '.quick-nav-grid form .quick-nav-card',
        '.staff-metric-grid > .staff-metric-card',
        '.admin-options-grid > .card',
        '.stats-grid > .stat-card',
        '.hotels-full-grid > .hotel-card',
        '.planned-grid > .planned-card',
        '.hotel-hero-card',
        '.info-card',
        '.destination-card',
        '.experience-card',
        '.karnataka-spotlight',
        '.section-intro-card',
        '.section-header-glass',
        '.map-svg-container',
        '.state-detail-panel',
        '.booking-card',
        '.notification-card',
        '.msg-card-row',
        '.empty-planned-state',
        '.profile-card',
        '.card',
        '.table-card',
        '.auth-card',
        '.hero-section'
    ].join(', ');

    // Run the content entrance animation sequence on the currently mounted page
    function runContentEntranceAnimation() {
        var mainWrapper = document.querySelector('.main-wrapper');
        if (!mainWrapper) return;

        clearTimeouts();
        initScrollObserver();

        // Reset main wrapper state
        mainWrapper.classList.remove('page-transitioning-out');

        var allTargets = mainWrapper.querySelectorAll(CONTENT_SELECTOR);
        var filteredTargets = filterTopLevelElements(allTargets);

        var viewportHeight = window.innerHeight || document.documentElement.clientHeight;
        var visibleCount = 0;

        filteredTargets.forEach(function(el) {
            var rect = el.getBoundingClientRect();
            // If element is currently visible in viewport:
            if (rect.top < viewportHeight * 0.96 && rect.bottom > 0) {
                var delay = Math.min(visibleCount * 85, 950); // 85ms stagger between sequential elements
                visibleCount++;
                animateElement(el, delay);
            } else {
                // If element is below viewport: mark as hidden and observe for scroll entrance / re-entry
                hideElement(el);
                if (scrollObserver) {
                    scrollObserver.observe(el);
                }
            }
        });
    }

    // Execute scripts contained in newly mounted HTML
    function executeScripts(container) {
        var scripts = container.querySelectorAll('script');
        scripts.forEach(function(oldScript) {
            var newScript = document.createElement('script');
            Array.from(oldScript.attributes).forEach(function(attr) {
                newScript.setAttribute(attr.name, attr.value);
            });
            newScript.textContent = oldScript.textContent;
            oldScript.parentNode.replaceChild(newScript, oldScript);
        });
    }

    // Seamless client-side navigation between tabs/pages
    function navigateTo(url, pushState) {
        if (pushState === undefined) pushState = true;
        if (isNavigating) return;
        isNavigating = true;

        // Immediately reset scroll position to 0 so the sticky header NEVER inherits
        // scroll offset from Home hero or jumps/slides from center
        window.scrollTo(0, 0);
        document.documentElement.scrollTop = 0;
        document.body.scrollTop = 0;

        // Cancel logo animation and background animation callbacks immediately so navigation is never blocked
        if (typeof window.__stopRoyalLogoAnimation === 'function') {
            window.__stopRoyalLogoAnimation();
        }
        if (typeof window.destroyHomeBackgroundAnimation === 'function') {
            window.destroyHomeBackgroundAnimation();
        }

        var mainWrapper = document.querySelector('.main-wrapper');
        if (mainWrapper) {
            mainWrapper.classList.add('page-transitioning-out');
        }

        fetch(url, {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(function(res) {
            if (!res.ok) {
                window.location.href = url;
                return;
            }
            var contentType = res.headers.get('content-type');
            if (contentType && !contentType.includes('text/html')) {
                window.location.href = url;
                return;
            }
            // If redirected by server, update final URL
            if (res.url && res.url !== url) {
                url = res.url;
            }
            return res.text();
        })
        .then(function(htmlText) {
            if (!htmlText) return;

            var parser = new DOMParser();
            var doc = parser.parseFromString(htmlText, 'text/html');

            var newMain = doc.querySelector('.main-wrapper');
            if (!newMain) {
                window.location.href = url;
                return;
            }

            // Allow 140ms for smooth outbound fade
            setTimeout(function() {
                // Ensure scroll is at top before DOM updates
                window.scrollTo(0, 0);
                document.documentElement.scrollTop = 0;
                document.body.scrollTop = 0;

                // Update document title
                if (doc.title) {
                    document.title = doc.title;
                }

                // Update active navigation state in navbar
                var newNavLinks = doc.querySelector('.nav-links');
                var curNavLinks = document.querySelector('.nav-links');
                if (newNavLinks && curNavLinks) {
                    curNavLinks.innerHTML = newNavLinks.innerHTML;
                }

                var newPortal = doc.querySelector('.portal-switch');
                var curPortal = document.querySelector('.portal-switch');
                if (newPortal && curPortal) {
                    curPortal.innerHTML = newPortal.innerHTML;
                }

                // Update body class
                document.body.className = doc.body.className;

                // Determine whether the incoming page is Home strictly
                var isEnteringHome = doc.body.classList.contains('home-page') ||
                                     doc.getElementById('homeBgAnimLayer') !== null ||
                                     (function() {
                                         try {
                                             var parsed = new URL(url, window.location.origin);
                                             return parsed.pathname === '/' || parsed.pathname.endsWith('/index.html');
                                         } catch(e) { return url === '/' || url === '/index.html'; }
                                     })();

                var incomingBgLayer = doc.getElementById('homeBgAnimLayer');
                var currentBgLayer = document.getElementById('homeBgAnimLayer');

                // Remove any stale non-home background container if present
                var staleBg = document.getElementById('globalNonHomeBg');
                if (staleBg && staleBg.parentNode) {
                    staleBg.parentNode.removeChild(staleBg);
                }

                if (isEnteringHome) {
                    document.body.classList.add('home-page');
                    // Reset any inline background so Home relies solely on its cinematic layer
                    document.body.style.backgroundImage = '';
                    document.body.style.backgroundSize = '';
                    document.body.style.backgroundPosition = '';
                    document.body.style.backgroundRepeat = '';
                    document.body.style.backgroundAttachment = '';

                    // Ensure background animation layer is in DOM
                    if (!currentBgLayer && incomingBgLayer) {
                        var header = document.querySelector('header');
                        if (header && header.parentNode) {
                            header.parentNode.insertBefore(incomingBgLayer, header);
                        } else {
                            document.body.insertBefore(incomingBgLayer, document.body.firstChild);
                        }
                    } else if (currentBgLayer) {
                        currentBgLayer.style.display = '';
                    }

                    // Reset scroll to top so Home animation starts correctly from frame 001
                    window.scrollTo(0, 0);

                    // Properly initialize/reinitialize Home background animation
                    if (typeof window.initHomeBackgroundAnimation === 'function') {
                        window.initHomeBackgroundAnimation();
                    } else {
                        var scriptEl = document.createElement('script');
                        scriptEl.src = '/js/home-background-animation.js';
                        scriptEl.onload = function() {
                            if (typeof window.initHomeBackgroundAnimation === 'function') {
                                window.initHomeBackgroundAnimation();
                            }
                        };
                        document.head.appendChild(scriptEl);
                    }
                } else {
                    // Non-Home Page: RESTORE NORMAL ORIGINAL BACKGROUND (No image, clean cream background)
                    document.body.classList.remove('home-page');
                    document.body.style.backgroundImage = '';
                    document.body.style.backgroundSize = '';
                    document.body.style.backgroundPosition = '';
                    document.body.style.backgroundRepeat = '';
                    document.body.style.backgroundAttachment = '';

                    if (currentBgLayer) {
                        currentBgLayer.style.display = 'none';
                    }
                    if (typeof window.destroyHomeBackgroundAnimation === 'function') {
                        window.destroyHomeBackgroundAnimation();
                    }
                }

                // Swap main content & copy styling to avoid layout shifting (e.g. login/register flex centering)
                if (mainWrapper) {
                    mainWrapper.innerHTML = newMain.innerHTML;
                    mainWrapper.className = newMain.className;
                    if (newMain.hasAttribute('style')) {
                        mainWrapper.setAttribute('style', newMain.getAttribute('style'));
                    } else {
                        mainWrapper.removeAttribute('style');
                    }
                }

                // Update browser history
                if (pushState) {
                    window.history.pushState({ url: url }, doc.title, url);
                }

                // Ensure scroll position is cleanly at top
                window.scrollTo(0, 0);
                document.documentElement.scrollTop = 0;
                document.body.scrollTop = 0;

                // Execute any page-specific inline scripts
                if (mainWrapper) {
                    executeScripts(mainWrapper);
                }

                // Re-bind dynamic option listeners
                initDynamicOptionAnimations();

                // Re-initialize logo animation for any newly mounted logo canvases
                if (typeof window.__initRoyalLogoAnimation === 'function') {
                    window.__initRoyalLogoAnimation();
                }

                // Trigger the Luxury Content Entrance Animation on every navigation!
                runContentEntranceAnimation();

                isNavigating = false;
            }, 140);
        })
        .catch(function(err) {
            console.warn('Navigation fallback:', err);
            window.location.href = url;
            isNavigating = false;
        });
    }

    // Dynamic Option Click Handler (Interactive Map Drawer, District Pills)
    function initDynamicOptionAnimations() {
        var drawer = document.getElementById('stateDetailDrawer');
        var drawerPills = document.getElementById('stateDrawerPills');
        if (drawerPills) {
            var mutationObserver = new MutationObserver(function() {
                var pills = drawerPills.querySelectorAll('.district-btn');
                pills.forEach(function(pill, idx) {
                    animateElement(pill, idx * 50);
                });
                if (drawer) {
                    drawer.classList.remove('luxury-dynamic-popup');
                    void drawer.offsetWidth;
                    drawer.classList.add('luxury-dynamic-popup');
                }
            });
            mutationObserver.observe(drawerPills, { childList: true });
        }
    }

    // Intercept clicks on links, tabs, and option cards for seamless luxury navigation
    document.addEventListener('click', function(e) {
        // 1. Direct link click
        var link = e.target.closest('a');
        if (link) {
            var href = link.getAttribute('href');
            if (!href) return;

            // Skip in-page anchors, javascript, external protocols, downloads, target="_blank", and modifier clicks
            if (href.startsWith('#') || href.startsWith('javascript:') || href.startsWith('mailto:') || href.startsWith('tel:')) {
                return;
            }
            if (link.getAttribute('target') === '_blank' || link.hasAttribute('download')) {
                return;
            }
            if (e.ctrlKey || e.metaKey || e.shiftKey || e.altKey || e.button !== 0) {
                return;
            }

            // Verify same-origin
            try {
                var targetUrl = new URL(link.href, window.location.origin);
                if (targetUrl.origin !== window.location.origin) {
                    return;
                }

                // If same path, search, and hash, allow normal browser anchor behavior
                if (targetUrl.pathname === window.location.pathname &&
                    targetUrl.search === window.location.search &&
                    targetUrl.hash) {
                    return;
                }

                // Intercept and navigate seamlessly
                e.preventDefault();
                navigateTo(link.href, true);
                return;
            } catch (err) {
                // Allow default navigation on URL parse errors
                return;
            }
        }

        // 2. Card click (when clicking on a guest-nav-card or hotel-card outside explicit buttons)
        var card = e.target.closest('.guest-nav-card, .quick-nav-card, .hotel-card');
        if (card) {
            // Ignore if clicked on an action button, form, input, or bookmark toggle
            if (e.target.closest('button, input, select, textarea, form, .btn-planned-toggle, .btn-planned-floating, .btn-google-maps')) {
                return;
            }
            var targetLink = card.querySelector('a.card-action-btn, a.btn-primary, a[href*="/hotels/"], a[href]');
            if (targetLink && targetLink.href) {
                try {
                    var cardUrl = new URL(targetLink.href, window.location.origin);
                    if (cardUrl.origin === window.location.origin) {
                        e.preventDefault();
                        navigateTo(cardUrl.href, true);
                    }
                } catch (err) {}
            }
        }
    });

    // Intercept GET forms (e.g. Search and Filters on Hotels page)
    document.addEventListener('submit', function(e) {
        var form = e.target;
        if (!form || (form.method && form.method.toLowerCase() !== 'get')) {
            return;
        }

        var action = form.getAttribute('action') || window.location.pathname;
        try {
            var actionUrl = new URL(action, window.location.origin);
            if (actionUrl.origin !== window.location.origin) return;

            var formData = new FormData(form);
            var searchParams = new URLSearchParams();
            formData.forEach(function(val, key) {
                if (val !== '') {
                    searchParams.append(key, val);
                }
            });
            var finalUrl = actionUrl.pathname + (searchParams.toString() ? '?' + searchParams.toString() : '');
            e.preventDefault();
            navigateTo(finalUrl, true);
        } catch (err) {}
    });

    // Handle Browser Back & Forward buttons
    window.addEventListener('popstate', function() {
        window.scrollTo(0, 0);
        document.documentElement.scrollTop = 0;
        document.body.scrollTop = 0;
        navigateTo(window.location.href, false);
    });

    // Handle BFCache page restoration
    window.addEventListener('pageshow', function(e) {
        if (e.persisted) {
            window.scrollTo(0, 0);
            document.documentElement.scrollTop = 0;
            document.body.scrollTop = 0;
            runContentEntranceAnimation();
        }
    });

    // Clean initialization on DOM ready
    function initApp() {
        if ('scrollRestoration' in history) {
            history.scrollRestoration = 'manual';
        }
        var isHome = document.body.classList.contains('home-page') ||
                     (function() {
                         var p = window.location.pathname || '';
                         return p === '/' || p.endsWith('/index.html');
                     })();
        if (!isHome) {
            document.body.classList.remove('home-page');
            document.body.style.backgroundImage = '';
            var staleBg = document.getElementById('globalNonHomeBg');
            if (staleBg && staleBg.parentNode) {
                staleBg.parentNode.removeChild(staleBg);
            }
        }
        initDynamicOptionAnimations();
        runContentEntranceAnimation();
    }

    // Start on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initApp);
    } else {
        initApp();
    }
})();
