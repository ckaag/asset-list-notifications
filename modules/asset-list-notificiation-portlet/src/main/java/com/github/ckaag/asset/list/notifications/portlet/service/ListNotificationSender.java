package com.github.ckaag.asset.list.notifications.portlet.service;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;

import javax.portlet.PortletPreferences;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ListNotificationSender {
    void optOutOfNotification(AssetListEntry list, long userId) throws PortalException;

    void optIntoNotification(AssetListEntry list, long userId) throws PortalException;

    Optional<Boolean> getOptedInStatus(AssetListEntry entity, long userId) throws PortalException;

    Map<AssetListEntry, AbstractMap.SimpleImmutableEntry<com.liferay.portal.kernel.model.PortletPreferences, PortletPreferences>> getNotificationPortlets();

    LocalDateTime getLastModifiedDate(AssetListEntry list, PortletPreferences portletPreferences);

    LocalDateTime getLastSentDate(AssetListEntry list, PortletPreferences portletPreferences);

    void updateLastDates(AssetListEntry list, PortletPreferences portletPreferences, boolean lastModified, boolean lastSent);

    List<AssetEntry> getEntriesSinceLastModified(AssetListEntry list, PortletPreferences portletPreferences, LocalDateTime lastModifiedDate);

    List<User> getSubscribingUsers(AssetListEntry list, PortletPreferences portletPreferences);

    void sendMailInternal(LocalDateTime lastModifiedDate, AssetListEntry list, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences portletPreferences, List<User> receivingUserIds, List<AssetEntry> content) throws PortalException;

    default void sendMailUpdatesTo(LocalDateTime lastModifiedDate, AssetListEntry list, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences portletPreferences, List<User> receivingUserIds, List<AssetEntry> content, boolean updateLastModifiedField) throws PortalException {
        sendMailInternal(lastModifiedDate, list, liferayPortletPreferences, portletPreferences, receivingUserIds, content);
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

    default void tickScheduler() throws PortalException {
        for (Map.Entry<AssetListEntry, AbstractMap.SimpleImmutableEntry<com.liferay.portal.kernel.model.PortletPreferences, PortletPreferences>> pair : getNotificationPortlets().entrySet()) {
            AbstractMap.SimpleImmutableEntry<com.liferay.portal.kernel.model.PortletPreferences, PortletPreferences> portletPreferencesPair = pair.getValue();
            LocalDateTime lastSentDate = getLastSentDate(pair.getKey(), portletPreferencesPair.getValue());
            if (LocalDateTime.now().isAfter(getNextScheduledUpdateInstant(portletPreferencesPair.getValue(), lastSentDate))) {
                LocalDateTime lastModifiedDate = getLastModifiedDate(pair.getKey(), portletPreferencesPair.getValue());
                List<AssetEntry> entries = getEntriesSinceLastModified(pair.getKey(), portletPreferencesPair.getValue(), lastModifiedDate);
                if (!entries.isEmpty()) {
                    LogFactoryUtil.getLog(this.getClass()).info("Sending out subscription mails for Content Set" + pair.getKey().getTitle() + " on site " + pair.getKey().getGroupId());
                    List<User> receivers = getSubscribingUsers(pair.getKey(), portletPreferencesPair.getValue());
                    sendMailUpdatesTo(lastModifiedDate, pair.getKey(), portletPreferencesPair.getKey(), portletPreferencesPair.getValue(), receivers, entries, true);
                }
            }
        }
    }
}
