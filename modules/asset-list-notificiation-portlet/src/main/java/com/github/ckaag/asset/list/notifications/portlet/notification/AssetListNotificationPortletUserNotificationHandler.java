package com.github.ckaag.asset.list.notifications.portlet.notification;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.UserNotificationEvent;
import com.liferay.portal.kernel.notifications.BaseUserNotificationHandler;
import com.liferay.portal.kernel.notifications.UserNotificationHandler;
import com.liferay.portal.kernel.service.ServiceContext;
import org.osgi.service.component.annotations.Component;

@Component(
        immediate = true,
        property = {"javax.portlet.name=" + AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION},
        service = UserNotificationHandler.class
)
public class AssetListNotificationPortletUserNotificationHandler extends BaseUserNotificationHandler {
    public AssetListNotificationPortletUserNotificationHandler() {
        setPortletId(AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION);
    }

    @Override
    protected String getBody(UserNotificationEvent userNotificationEvent, ServiceContext serviceContext) throws Exception {
        JSONObject jsonObject = JSONFactoryUtil.createJSONObject(userNotificationEvent.getPayload());
        try {
            return jsonObject.getString("body");
        } catch (Exception e) {
            _log.debug("Problem with parsing Notification json: " + userNotificationEvent.getPayload(), e);
            return userNotificationEvent.getPayload();
        }
    }

    private static final Log _log = LogFactoryUtil.getLog(AssetListNotificationPortletUserNotificationHandler.class);

}
