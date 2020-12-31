package com.github.ckaag.asset.list.notifications.portlet.service;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;
import com.github.ckaag.asset.list.notifications.portlet.notification.NotificationService;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.list.asset.entry.provider.AssetListAssetEntryProvider;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.asset.list.service.AssetListEntryLocalServiceUtil;
import com.liferay.expando.kernel.exception.NoSuchTableException;
import com.liferay.expando.kernel.model.ExpandoColumnConstants;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.model.ExpandoValue;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoTableLocalServiceUtil;
import com.liferay.expando.kernel.service.ExpandoValueLocalService;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.ClassName;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.service.PortletPreferencesLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.segments.provider.SegmentsEntryProviderRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.PortletPreferences;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component(immediate = true, service = ListNotificationSender.class)
public class ListNotificationSenderImpl implements ListNotificationSender {

    private static final String LAST_MODIFIED_NOTIFICATION_EXPANDO = "LAST_MODIFIED_NOTIFICATION_EXPANDO";
    private static final String NOTIFICATION_OPTED_OUT_EXPANDO = "NOTIFICATION_OPTED_OUT_EXPANDO";
    private static final String NOTIFICATION_OPTED_IN_EXPANDO = "NOTIFICATION_OPTED_IN_EXPANDO";
    public static final String CUSTOM_FIELDS_DEFAULT_TABLE_NAME = "CUSTOM_FIELDS";

    @Activate
    public void activate() {
        makeSureCustomFieldsExist();
    }

    private void makeSureCustomFieldsExist() {
        for (Company company : CompanyLocalServiceUtil.getCompanies()) {
            long companyId = company.getCompanyId();
            try {
                ClassName className = ClassNameLocalServiceUtil.getClassName(AssetListEntry.class.getName());
                ExpandoTable expandoTable;
                try {
                    expandoTable = ExpandoTableLocalServiceUtil.getDefaultTable(companyId, className.getClassNameId());
                } catch (NoSuchTableException e) {
                    expandoTable = ExpandoTableLocalServiceUtil.addDefaultTable(companyId, className.getClassNameId());
                }
                createCustomFieldIfNotExists(expandoTable.getTableId(), LAST_MODIFIED_NOTIFICATION_EXPANDO, ExpandoColumnConstants.STRING);
                createCustomFieldIfNotExists(expandoTable.getTableId(), NOTIFICATION_OPTED_OUT_EXPANDO, ExpandoColumnConstants.LONG_ARRAY);
                createCustomFieldIfNotExists(expandoTable.getTableId(), NOTIFICATION_OPTED_IN_EXPANDO, ExpandoColumnConstants.LONG_ARRAY);
            } catch (Exception e) {
                LogFactoryUtil.getLog(this.getClass()).error("Something went wrong during creation of custom fields", e);
            }
        }
    }

    private void createCustomFieldIfNotExists(long tableId, String expandoName, int typeOfField) throws PortalException {
        boolean exists;
        try {
            exists = null != ExpandoColumnLocalServiceUtil.getColumn(tableId, expandoName);
        } catch (Exception e) {
            exists = false;
        }
        if (!exists) {
            ExpandoColumnLocalServiceUtil.addColumn(tableId, expandoName, typeOfField);
        }
    }

    @Override
    public void optOutOfNotification(AssetListEntry list, long userId) throws PortalException {
        Optional<Boolean> beforeState = this.getOptedInStatus(list, userId);
        if (beforeState.isPresent() && beforeState.get()) {
            removeFromOptList(NOTIFICATION_OPTED_IN_EXPANDO, list, userId);
        }
        if (beforeState.isEmpty() || beforeState.get()) {
            addToOptList(NOTIFICATION_OPTED_OUT_EXPANDO, list, userId);
        }
    }

    @Override
    public void optIntoNotification(AssetListEntry list, long userId) throws PortalException {
        Optional<Boolean> beforeState = this.getOptedInStatus(list, userId);
        if (beforeState.isPresent() && !beforeState.get()) {
            removeFromOptList(NOTIFICATION_OPTED_OUT_EXPANDO, list, userId);
        }
        if (beforeState.isEmpty() || !beforeState.get()) {
            addToOptList(NOTIFICATION_OPTED_IN_EXPANDO, list, userId);
        }
    }

    private void addToOptList(String expandoName, AssetListEntry entity, long userId) throws PortalException {
        ExpandoValue ev = expandoValueLocalService.getValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, expandoName, entity.getAssetListEntryId());
        if (ev == null) {
            expandoValueLocalService.addValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, expandoName, entity.getAssetListEntryId(), new long[]{userId});
        } else {
            long[] prev = ev.getLongArray();
            long[] next = addUniqueToArray(prev, userId);
            ev.setLongArray(next);
            expandoValueLocalService.updateExpandoValue(ev);
        }
    }

    private void removeFromOptList(String expandoName, AssetListEntry entity, long userId) throws PortalException {
        ExpandoValue ev = expandoValueLocalService.getValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, expandoName, entity.getAssetListEntryId());
        if (ev == null) {
            expandoValueLocalService.addValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, expandoName, entity.getAssetListEntryId(), new long[]{});
        } else {
            long[] prev = ev.getLongArray();
            long[] next = removeUniqueFromArray(prev, userId);
            ev.setLongArray(next);
            expandoValueLocalService.updateExpandoValue(ev);
        }
    }

    private long[] removeUniqueFromArray(long[] prev, long userId) {
        if (prev == null || prev.length == 0) {
            return new long[0];
        }
        long[] out = new long[prev.length - 1];
        int outIdx = 0;
        for (long l : prev) {
            if (l != userId) {
                out[outIdx] = l;
                outIdx += 1;
            }
        }
        return out;
    }

    private long[] addUniqueToArray(long[] prev, long userId) {
        if (prev == null || prev.length == 0) {
            return new long[]{userId};
        }
        for (long f : prev) {
            if (f == userId) {
                return prev;
            }
        }
        long[] out = new long[prev.length + 1];
        System.arraycopy(prev, 0, out, 0, prev.length);
        out[out.length - 1] = userId;
        return out;
    }


    @Override
    public Optional<Boolean> getOptedInStatus(AssetListEntry entity, long userId) throws PortalException {
        ExpandoValue evIn = expandoValueLocalService.getValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, NOTIFICATION_OPTED_IN_EXPANDO, entity.getAssetListEntryId());
        if (evIn != null) {
            for (long uid : evIn.getLongArray()) {
                if (uid == userId) {
                    return Optional.of(true);
                }
            }
        }
        ExpandoValue evOut = expandoValueLocalService.getValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, NOTIFICATION_OPTED_OUT_EXPANDO, entity.getAssetListEntryId());
        if (evOut != null) {
            for (long uid : evOut.getLongArray()) {
                if (uid == userId) {
                    return Optional.of(false);
                }
            }
        }
        return Optional.empty();
    }

    private void setModifiedCustomField(AssetListEntry entity, String value) throws PortalException {
        ExpandoValue ev = expandoValueLocalService.getValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, LAST_MODIFIED_NOTIFICATION_EXPANDO, entity.getAssetListEntryId());
        ev.setString(value);
        expandoValueLocalService.updateExpandoValue(ev);
    }

    @Reference
    ExpandoValueLocalService expandoValueLocalService;

    private String getModifiedCustomField(AssetListEntry entity) throws PortalException {
        return expandoValueLocalService.getValue(entity.getCompanyId(), AssetListEntry.class.getName(), CUSTOM_FIELDS_DEFAULT_TABLE_NAME, LAST_MODIFIED_NOTIFICATION_EXPANDO, entity.getAssetListEntryId()).getString();
    }

    @Override
    public Map<AssetListEntry, AbstractMap.SimpleImmutableEntry<com.liferay.portal.kernel.model.PortletPreferences, PortletPreferences>> getNotificationPortlets() {
        DynamicQuery dq = PortletPreferencesLocalServiceUtil.dynamicQuery();
        dq.add(RestrictionsFactoryUtil.like("portletId", AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION + "%"));
        List<com.liferay.portal.kernel.model.PortletPreferences> list = PortletPreferencesLocalServiceUtil.dynamicQuery(dq);
        HashMap<AssetListEntry, AbstractMap.SimpleImmutableEntry<com.liferay.portal.kernel.model.PortletPreferences, PortletPreferences>> result = new HashMap<>();
        for (com.liferay.portal.kernel.model.PortletPreferences portletPreferences : list) {
            PortletPreferences portletPref = PortletPreferencesLocalServiceUtil.fetchPreferences(portletPreferences.getCompanyId(), portletPreferences.getOwnerId(), portletPreferences.getOwnerType(), portletPreferences.getPlid(), portletPreferences.getPortletId());
            String assetListEntryIdString = portletPref.getValue("selectedAssetList", "-1");
            try {
                long id = Long.parseLong(assetListEntryIdString);
                AssetListEntry ale = AssetListEntryLocalServiceUtil.fetchAssetListEntry(id);
                if (ale != null) {
                    result.put(ale, new AbstractMap.SimpleImmutableEntry<>(portletPreferences, portletPref));
                }
            } catch (Exception e) {
                LogFactoryUtil.getLog(this.getClass()).error("Something broke", e);
            }
        }
        return result;
    }

    @Override
    public LocalDateTime getLastModifiedDate(AssetListEntry list, PortletPreferences portletPreferences) {
        try {
            String value = this.getModifiedCustomField(list);
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now().minusMinutes(1);
        }
    }

    @Override
    public LocalDateTime getLastSentDate(AssetListEntry list, PortletPreferences portletPreferences) {
        return this.getLastModifiedDate(list, portletPreferences);
    }

    @Override
    public void updateLastDates(AssetListEntry list, PortletPreferences portletPreferences, boolean lastModified, boolean lastSent) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        if (lastModified || lastSent) {
            try {
                setModifiedCustomField(list, date);
            } catch (PortalException e) {
                LogFactoryUtil.getLog(this.getClass()).error("While trying to set custom field on classpk " + list.getAssetListEntryId(), e);
            }
        }
    }

    @Reference
    private AssetListAssetEntryProvider assetListAssetEntryProvider;

    @Reference
    private SegmentsEntryProviderRegistry segmentsEntryProviderRegistry;

    @Override
    public List<AssetEntry> getEntriesSinceLastModified(AssetListEntry list, PortletPreferences portletPreferences, LocalDateTime lastModifiedDate) {
        long[] segments = new long[0];
        try {
            segments = segmentsEntryProviderRegistry.getSegmentsEntryIds(list.getGroupId(), User.class.getName(), list.getUserId(), null);
        } catch (PortalException e) {
            LogFactoryUtil.getLog(this.getClass()).error(e);
        }
        initializePermissionChecker(list.getUserId());
        List<AssetEntry> raw = assetListAssetEntryProvider.getAssetEntries(list, segments, 0, 1_000);
        //noinspection UnnecessaryLocalVariable
        List<AssetEntry> filtered = raw.stream().filter(t -> !lastModifiedDate.isAfter(convertToLocalDateViaInstant(t.getModifiedDate()))).collect(Collectors.toList());
        return filtered;
    }

    private void initializePermissionChecker(long userId) {
        PermissionChecker permissionChecker;
        PrincipalThreadLocal.setName(userId);
        permissionChecker = PermissionCheckerFactoryUtil.create(userLocalService.fetchUser(userId));
        PermissionThreadLocal.setPermissionChecker(permissionChecker);
    }

    @Reference
    private UserLocalService userLocalService;

    @Override
    public List<User> getSubscribingUsers(AssetListEntry list, PortletPreferences portletPreferences) {
        return userLocalService.getGroupUsers(list.getGroupId());
    }

    private LocalDateTime convertToLocalDateViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    @Reference
    private NotificationService notificationService;

    @Override
    public void sendMailInternal(LocalDateTime lastModifiedDate, AssetListEntry list, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences portletPreferences, List<User> receivingUsers, List<AssetEntry> content) throws PortalException {
        String optMode = portletPreferences.getValue("receiverSelectMode", "");
        boolean isOptIn = "optin".equals(optMode);
        boolean isOptOut = "optout".equals(optMode);
        for (User user : receivingUsers) {
            boolean isListeningForMail = (!isOptIn && !isOptOut) || (isOptIn && this.getOptedInStatus(list, user.getUserId()).stream().allMatch(t -> t)) || (isOptOut && this.getOptedInStatus(list, user.getUserId()).orElse(true));
            //noinspection UnnecessaryLocalVariable
            boolean isListeningForNotification = isListeningForMail;
            notificationService.sendUserMailAndNotificationIfRequested(isListeningForMail, isListeningForNotification, user, list, portletPreferences.getValue("emailFromAddress", "notification@localhost.localdomain"), portletPreferences.getValue("emailFromName", "Anonymous"), lastModifiedDate, liferayPortletPreferences, portletPreferences, content);
        }
    }

}
