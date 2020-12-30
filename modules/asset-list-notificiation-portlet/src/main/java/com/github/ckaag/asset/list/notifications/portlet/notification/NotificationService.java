package com.github.ckaag.asset.list.notifications.portlet.notification;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.portal.kernel.model.User;

import javax.portlet.PortletPreferences;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {
    String buildHtmlBodyMail(User receiver, AssetListEntry sourcedList, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences preferences, LocalDateTime lastModified, List<AssetEntry> newAssets);

    String buildSubjectMail(User receiver, AssetListEntry sourcedList, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences preferences, LocalDateTime lastModified, List<AssetEntry> newAssets);

    void sendUserMail(String emailFromAddress, String emailFromName, User receiver, String subject, String body);

    void sendUserNotification(User receiver, String body);

    boolean isListeningForMail(User receiver);

    boolean isListeningForNotification(User receiver);

    default void sendUserMailAndNotificationIfRequested(User receiver, AssetListEntry sourcedList, String emailFromAddress, String emailFromName, LocalDateTime lastModified, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences preferences, List<AssetEntry> newAssets) {
        final boolean isMail = isListeningForMail(receiver);
        final boolean isNotification = isListeningForNotification(receiver);
        if (!isMail && !isNotification) return;
        final String body = buildHtmlBodyMail(receiver, sourcedList, liferayPortletPreferences, preferences, lastModified, newAssets);
        if (isNotification) {
            sendUserNotification(receiver, body);
        }
        if (isMail) {
            final String subject = buildSubjectMail(receiver, sourcedList, liferayPortletPreferences, preferences, lastModified, newAssets);
            sendUserMail(emailFromAddress, emailFromName, receiver, subject, body);
        }
    }
}
