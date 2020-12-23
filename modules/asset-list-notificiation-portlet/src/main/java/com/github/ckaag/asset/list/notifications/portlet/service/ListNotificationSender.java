package com.github.ckaag.asset.list.notifications.portlet.service;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;

import javax.portlet.PortletPreferences;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ListNotificationSender {
    Map<AssetListEntry, PortletPreferences> getNotificationPortlets();

    LocalDateTime getLastModifiedDate(AssetListEntry list, PortletPreferences portletPreferences);

    LocalDateTime getLastSentDate(AssetListEntry list, PortletPreferences portletPreferences);

    void updateLastDates(AssetListEntry list, PortletPreferences portletPreferences, boolean lastModified, boolean lastSent);

    List<AssetEntry> getEntriesSinceLastModified(AssetListEntry list, PortletPreferences portletPreferences, LocalDateTime lastModifiedDate);

    List<User> getSubscribingUsers(AssetListEntry list, PortletPreferences portletPreferences);

    void sendMailInternal(LocalDateTime lastModifiedDate, AssetListEntry list, PortletPreferences portletPreferences, List<User> receivingUserIds, List<AssetEntry> content);

    default void sendMailUpdatesTo(LocalDateTime lastModifiedDate, AssetListEntry list, PortletPreferences portletPreferences, List<User> receivingUserIds, List<AssetEntry> content, boolean updateLastModifiedField) {
        sendMailInternal(lastModifiedDate, list, portletPreferences, receivingUserIds, content);
        if (updateLastModifiedField) {
            this.updateLastDates(list, portletPreferences, true, true);
        }
    }

    default LocalDateTime getNextScheduledUpdateInstant(PortletPreferences portletPreferences, LocalDateTime lastSentDate) {
        if (lastSentDate == null) {
            return LocalDateTime.now().minusMinutes(1);
        } else {
            int minutes;
            try {
                minutes = Integer.parseInt(portletPreferences.getValue("minutesBetweenMails", "1"));
            } catch (Exception e) {
                minutes = 1;
            }
            return lastSentDate.plusMinutes(minutes);
        }
    }

    default void tickScheduler() {
        for (Map.Entry<AssetListEntry, PortletPreferences> pair : getNotificationPortlets().entrySet()) {
            PortletPreferences portletPreferences = pair.getValue();
            LocalDateTime lastSentDate = getLastSentDate(pair.getKey(), portletPreferences);
            if (LocalDateTime.now().isAfter(getNextScheduledUpdateInstant(portletPreferences, lastSentDate))) {
                LocalDateTime lastModifiedDate = getLastModifiedDate(pair.getKey(), portletPreferences);
                List<AssetEntry> entries = getEntriesSinceLastModified(pair.getKey(), portletPreferences, lastModifiedDate);
                if (!entries.isEmpty()) {
                    LogFactoryUtil.getLog(this.getClass()).info("Sending out subscription mails for Content Set" + pair.getKey().getTitle() + " on site " + pair.getKey().getGroupId());
                    List<User> receivers = getSubscribingUsers(pair.getKey(), portletPreferences);
                    sendMailUpdatesTo(lastModifiedDate, pair.getKey(), portletPreferences, receivers, entries, true);
                }
            }
        }
    }
}
