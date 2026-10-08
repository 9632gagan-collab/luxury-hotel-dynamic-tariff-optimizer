/**
 * Royal Indian Stays - Planned List Interactivity (Project P68)
 * Handles client-side toggling, login redirection for unauthenticated visitors,
 * visual state synchronization across hotel cards and details pages, and luxury toast feedback.
 */
(function() {
    'use strict';

    function getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        if (meta) return meta.getAttribute('content');
        const input = document.querySelector('input[name="_csrf"]');
        if (input) return input.value;
        return '';
    }

    function showPlannedToast(message) {
        let toast = document.getElementById('globalPlannedToast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'globalPlannedToast';
            toast.className = 'planned-toast-notification';
            toast.innerHTML = '<i class="fas fa-bookmark" style="color: var(--gold-accent);"></i><span id="globalPlannedToastText"></span>';
            document.body.appendChild(toast);
        }
        const textSpan = document.getElementById('globalPlannedToastText');
        if (textSpan) textSpan.textContent = message;

        toast.style.display = 'inline-flex';
        toast.style.opacity = '1';

        clearTimeout(window.__plannedToastTimeout);
        window.__plannedToastTimeout = setTimeout(function() {
            toast.style.transition = 'opacity 0.4s ease, transform 0.4s ease';
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(8px)';
            setTimeout(function() {
                toast.style.display = 'none';
                toast.style.transform = 'translateY(0)';
            }, 400);
        }, 2800);
    }

    window.togglePlannedHotel = async function(btn, event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }

        const hotelId = btn.getAttribute('data-hotel-id');
        if (!hotelId) return;

        // Current page return destination
        const currentPath = window.location.pathname + window.location.search;

        try {
            const csrfToken = getCsrfToken();
            const formData = new URLSearchParams();
            formData.append('hotelId', hotelId);

            const response = await fetch('/guest/planned-list/toggle', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: formData.toString()
            });

            // If unauthorized or redirected to login, redirect visitor to login with proper returnUrl
            if (response.status === 401 || response.status === 403 || response.redirected) {
                const targetAddUrl = '/guest/planned-list/add?hotelId=' + encodeURIComponent(hotelId) + '&returnUrl=' + encodeURIComponent(currentPath);
                window.location.href = '/login?role=GUEST&redirect=' + encodeURIComponent(targetAddUrl);
                return;
            }

            if (response.ok) {
                const data = await response.json();
                if (data.redirectUrl) {
                    window.location.href = data.redirectUrl;
                    return;
                }

                const isPlanned = data.planned === true;
                const message = data.message || (isPlanned ? 'Added to your Planned List' : 'Removed from your Planned List');

                // Synchronize all instances of this hotel's planning button across the page
                document.querySelectorAll('[data-hotel-id="' + hotelId + '"]').forEach(function(el) {
                    const icon = el.querySelector('i');
                    if (isPlanned) {
                        el.classList.add('is-planned');
                        el.setAttribute('title', 'In your Planned List (Click to remove)');
                        el.setAttribute('aria-label', 'Remove from Planned List');
                        if (icon) {
                            icon.className = 'fas fa-bookmark';
                        }
                    } else {
                        el.classList.remove('is-planned');
                        el.setAttribute('title', 'Add to Planned List');
                        el.setAttribute('aria-label', 'Add to Planned List');
                        if (icon) {
                            icon.className = 'far fa-bookmark';
                        }
                    }

                    // Special handling for hotel details page label
                    const labelSpan = el.querySelector('#textPlannedDetails');
                    if (labelSpan) {
                        labelSpan.textContent = isPlanned ? 'Added to Planned List' : 'Add to Planned List';
                    }
                });

                showPlannedToast(message);
            } else {
                // If endpoint cannot process, fallback to traditional navigation
                const fallbackUrl = '/login?role=GUEST&redirect=' + encodeURIComponent('/guest/planned-list/add?hotelId=' + hotelId + '&returnUrl=' + encodeURIComponent(currentPath));
                window.location.href = fallbackUrl;
            }
        } catch (err) {
            console.error('Planned List toggle error:', err);
            // Fallback redirect to login
            const fallbackUrl = '/login?role=GUEST&redirect=' + encodeURIComponent('/guest/planned-list/add?hotelId=' + hotelId + '&returnUrl=' + encodeURIComponent(currentPath));
            window.location.href = fallbackUrl;
        }
    };

    window.togglePlannedHotelDetails = function(btn, event) {
        window.togglePlannedHotel(btn, event);
    };

})();
