package com.github.ckaag.asset.list.notifications.portlet.portlet;


import aQute.bnd.annotation.metatype.Meta;

import static com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys.NOTIFICATION_CONFIG_PID;


@Meta.OCD(
        id = NOTIFICATION_CONFIG_PID
)
public interface AssetListNotificationConfiguration {

    @Meta.AD(required = false)
    public String selectedAssetList();

    @Meta.AD(required = false)
    public String localizedBodyTemplate();

    @Meta.AD(required = false)
    public String localizedSubjectTemplate();

    @Meta.AD(required = false, deflt = "Anonymous")
    public String emailFromName();

    @Meta.AD(required = false, deflt = "notification@localhost.localdomain")
    public String emailFromAddress();

    @Meta.AD(required = false, deflt = "forced")
    public String receiverSelectMode();

    @Meta.AD(required = false)
    public String userSpecificMode();

    @Meta.AD(required = false)
    public String minutesBetweenMails();
}
