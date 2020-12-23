package com.github.ckaag.asset.list.notifications.portlet.notification;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;
import com.liferay.portal.kernel.model.UserNotificationDeliveryConstants;
import com.liferay.portal.kernel.notifications.UserNotificationDefinition;
import com.liferay.portal.kernel.notifications.UserNotificationDeliveryType;
import org.osgi.service.component.annotations.Component;

@Component(immediate = true, service = UserNotificationDefinition.class, property = {"javax.portlet.name=" + AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION})
public class AssetListNotificationUserNotificationDefinition extends UserNotificationDefinition {
    public AssetListNotificationUserNotificationDefinition() {
        super(AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION, 0,
                AssetListNotificationPortletKeys.NOTIFICATION_TYPE,
                "receive-a-notification-when-an-admin-logs-in");

        this.addUserNotificationDeliveryType(
                new UserNotificationDeliveryType("email", UserNotificationDeliveryConstants.TYPE_EMAIL, true, true));
        this.addUserNotificationDeliveryType(
                new UserNotificationDeliveryType("website", UserNotificationDeliveryConstants.TYPE_WEBSITE, true, true));
    }
}
