package com.github.ckaag.asset.list.notifications.portlet.notification;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;
import com.github.ckaag.asset.list.notifications.portlet.service.PortletLinkService;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.mail.kernel.model.MailMessage;
import com.liferay.mail.kernel.service.MailService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.UserNotificationDeliveryConstants;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserNotificationEventLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.mail.internet.InternetAddress;
import javax.portlet.PortletPreferences;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import static com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys.CLASS_NAME_PARAM;
import static com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys.CLASS_PK_PARAM;

@Component(service = NotificationService.class, immediate = true)
public class NotificationServiceImpl implements NotificationService {

    private static final String ASSET_MODIFIED_DATE = "ASSET_MODIFIED_DATE";
    private static final String ASSET_DESCRIPTION = "ASSET_DESCRIPTION";
    private static final String ASSET_TITLE = "ASSET_TITLE";
    private static final String ASSET_CREATE_DATE = "ASSET_CREATE_DATE";
    private static final String ASSET_PUBLISH_DATE = "ASSET_PUBLISH_DATE";
    private static final String ASSET_SUMMARY = "ASSET_SUMMARY";
    private static final String ASSET_USERNAME = "ASSET_USERNAME";
    private static final String ASSET_VIEW_URL = "ASSET_VIEW_URL";
    private static final String LIST_ITEM_PREFIX = "FOR_EACH_ASSET_START";
    private static final String LIST_ITEM_POSTFIX = "FOR_EACH_ASSET_END";
    public static final String ASSET_COUNT = "ASSET_COUNT";
    public static final String USER_FULL_NAME = "USER_FULL_NAME";
    public static final String USER_LAST_NAME = "USER_LAST_NAME";
    public static final String ASSET_LIST_TITLE = "ASSET_LIST_TITLE";
    public static final String LAST_MODIFIED_DATE = "LAST_MODIFIED_DATE";
    public static final String USER_FIRST_NAME = "USER_FIRST_NAME";

    public static final List<String> AVAILABLE_PLACEHOLDERS = Arrays.asList(ASSET_COUNT, USER_FIRST_NAME, USER_FULL_NAME, USER_LAST_NAME, ASSET_LIST_TITLE, LAST_MODIFIED_DATE, LIST_ITEM_PREFIX, LIST_ITEM_POSTFIX, ASSET_MODIFIED_DATE, ASSET_DESCRIPTION, ASSET_TITLE, ASSET_CREATE_DATE, ASSET_PUBLISH_DATE, ASSET_SUMMARY, ASSET_USERNAME, ASSET_VIEW_URL);


    private String replaceBasicElements(Locale locale, String template, User receiver, AssetListEntry sourcedList, @SuppressWarnings("unused") PortletPreferences preferences, LocalDateTime lastModified, List<AssetEntry> newAssets) {
        DateTimeFormatter sdf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM).withLocale(locale);
        return template.replace(ASSET_COUNT, String.valueOf(newAssets.size())).replace(LAST_MODIFIED_DATE, sdf.format(lastModified)).replace(ASSET_LIST_TITLE, sourcedList.getTitle()).replace(USER_FULL_NAME, receiver.getFullName()).replace(USER_FIRST_NAME, receiver.getFirstName()).replace(USER_LAST_NAME, receiver.getLastName());
    }

    @Override
    public String buildHtmlBodyMail(User receiver, AssetListEntry sourcedList, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences preferences, LocalDateTime lastModified, List<AssetEntry> newAssets) {
        String templates = preferences.getValue("localizedBodyTemplate", null);
        if (templates == null) {
            return null;
        }
        Locale locale = receiver.getLocale();
        String template = LocalizationUtil.getLocalizationMap(templates).get(locale);
        template = replaceBasicElements(locale, template, receiver, sourcedList, preferences, lastModified, newAssets);
        StringBuilder sb = new StringBuilder();
        int startIndex;
        int endIndex = 0;
        while (true) {
            //find next match or up to end of string
            startIndex = template.indexOf(LIST_ITEM_PREFIX, endIndex);
            if (startIndex < 0) {
                //early return
                sb.append(template.substring(endIndex));
                break;
            } else {
                sb.append(template.substring(endIndex, startIndex));
                endIndex = template.indexOf(LIST_ITEM_POSTFIX, startIndex + LIST_ITEM_PREFIX.length());
                if (endIndex < 0) {
                    LogFactoryUtil.getLog(this.getClass()).error("missing " + LIST_ITEM_POSTFIX + " in template");
                    break;
                } else {
                    for (AssetEntry asset : newAssets) {
                        sb.append(replaceListItemTemplate(locale, template.substring(startIndex + LIST_ITEM_PREFIX.length(), endIndex), asset, liferayPortletPreferences));
                    }
                    endIndex = endIndex + LIST_ITEM_POSTFIX.length();
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String buildSubjectMail(User receiver, AssetListEntry sourcedList, com.liferay.portal.kernel.model.PortletPreferences liferayPortletPreferences, PortletPreferences preferences, LocalDateTime lastModified, List<AssetEntry> newAssets) {
        String templates = preferences.getValue("localizedSubjectTemplate", null);
        if (templates == null) {
            return null;
        }
        Locale locale = receiver.getLocale();
        String template = LocalizationUtil.getLocalizationMap(templates).get(locale);
        return replaceBasicElements(locale, template, receiver, sourcedList, preferences, lastModified, newAssets);
    }


    private String replaceListItemTemplate(Locale locale, String subtemplate, AssetEntry asset, com.liferay.portal.kernel.model.PortletPreferences portletPreferences) {
        DateFormat sdf = SimpleDateFormat.getDateTimeInstance(2, 2, locale);
        return replaceLink(subtemplate.replace(ASSET_MODIFIED_DATE, sdf.format(asset.getModifiedDate())).replace(ASSET_TITLE, asset.getTitle()).replace(ASSET_DESCRIPTION, asset.getDescription()).replace(ASSET_CREATE_DATE, sdf.format(asset.getCreateDate())).replace(ASSET_PUBLISH_DATE, sdf.format(asset.getPublishDate())).replace(ASSET_SUMMARY, asset.getSummary()).replace(ASSET_USERNAME, asset.getUserName()), portletPreferences, asset);
    }

    private String replaceLink(String template, com.liferay.portal.kernel.model.PortletPreferences portletPreferences, AssetEntry asset) {
        if (template.contains(ASSET_VIEW_URL)) {
            return template.replace(ASSET_VIEW_URL, portletLinkService.buildRelativePortletUrl(portletPreferences, Map.of(CLASS_NAME_PARAM, Collections.singletonList(asset.getClassName()), CLASS_PK_PARAM, Collections.singletonList(String.valueOf(asset.getClassPK())))));
        } else {
            return template;
        }
    }

    @Reference
    private MailService mailService;

    @Override
    public void sendUserMail(String emailFromAddress, String emailFromName, User receiver, String subject, String body) {
        MailMessage mail = new MailMessage();
        try {
            String receiverEmail = receiver.getEmailAddress();
            mail.setBody(body);
            mail.setSubject(subject);
            mail.setTo(new InternetAddress(receiverEmail));
            mail.setFrom(new InternetAddress(emailFromAddress, emailFromName));
            mail.setHTMLFormat(true);
            log.info(String.format("Sending mail with subject '%s' to recipient '%s'", mail.getSubject(), receiverEmail));
            mailService.sendEmail(mail);
        } catch (Exception e) {
            log.error(e);
        }
    }

    private static final Log log = LogFactoryUtil.getLog(NotificationServiceImpl.class);

    @Reference
    private UserNotificationEventLocalService userNotificationEventLocalService;

    @Override
    public void sendUserNotification(User receiver, String body) {
        long timestamp = new Date().getTime();
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setUuid(UUID.randomUUID().toString()); // required by underlying service method
        String payload = buildPayload(body);
        try {
            userNotificationEventLocalService.addUserNotificationEvent(receiver.getUserId(), AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION, timestamp, DELIVERY_TYPE_WEBSITE, (LocalDateTime.now().plusYears(1).toInstant(ZoneOffset.UTC).toEpochMilli()), payload, false, false, serviceContext);
        } catch (PortalException e) {
            log.error(e);
        }
    }

    private static final int DELIVERY_TYPE_WEBSITE = UserNotificationDeliveryConstants.TYPE_WEBSITE;

    private static String buildPayload(String body) {
        return JSONFactoryUtil.looseSerializeDeep(getBasicRequiredJsonFields("no-header-key-defined-yet", body));
    }

    @SuppressWarnings("SameParameterValue")
    private static Map<String, Object> getBasicRequiredJsonFields(String headerKey, String body) {
        Map<Locale, String> localizedMap = new HashMap<>();
        localizedMap.put(LocaleUtil.getDefault(), headerKey);
        Map<String, Object> out = new HashMap<>();
        out.put(
                "entryURL", "http://localhost:8080/web/guest/manage" //unknown if needed by liferay
        );
        out.put(
                "localizedBodyMap", localizedMap //needed by us in Handler code; unknown if needed by liferay itself
        );
        out.put(
                "localizedContext", new HashMap<>() //unknown if needed by liferay
        );
        out.put(
                "localizedSubjectMap", localizedMap //needed by us in Handler code; unknown if needed by liferay itself
        );
        out.put(
                "context", new HashMap<>() //unknown if needed by liferay
        );
        out.put(
                "entryTitle", headerKey //unknown if needed by liferay
        );
        out.put("body", body); //our html body
        return out;
    }


    @Override
    public boolean isListeningForMail(User receiver) {
        //TODO: implement
        return true;
    }

    @Override
    public boolean isListeningForNotification(User receiver) {
        //TODO: implement
        return true;
    }

    @Reference
    private PortletLinkService portletLinkService;
}
