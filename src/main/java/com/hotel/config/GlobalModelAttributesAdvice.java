package com.hotel.config;

import com.hotel.service.InternalCommunicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.util.Set;

/**
 * Controller Advice providing global model attributes across all view endpoints.
 * Ensures consistent availability of notification and messaging counts for the Admin navbar.
 */
@ControllerAdvice
public class GlobalModelAttributesAdvice {

    private final InternalCommunicationService internalCommunicationService;

    @Autowired
    public GlobalModelAttributesAdvice(InternalCommunicationService internalCommunicationService) {
        this.internalCommunicationService = internalCommunicationService;
    }

    @ModelAttribute
    public void populateGlobalAttributes(Model model, Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            try {
                long unreadNotifs = internalCommunicationService.getUnreadNotificationCount(username);
                model.addAttribute("unreadNotificationCount", unreadNotifs);

                Set<Long> unreadMsgIds = internalCommunicationService.getUnreadMessageIdsForUser(username);
                long unreadMsgs = unreadMsgIds != null ? unreadMsgIds.size() : 0;
                model.addAttribute("unreadMessageCount", unreadMsgs);
            } catch (Exception ignored) {
                // Safeguard against uninitialized tables during early bootstrapping
            }
        }
    }
}
