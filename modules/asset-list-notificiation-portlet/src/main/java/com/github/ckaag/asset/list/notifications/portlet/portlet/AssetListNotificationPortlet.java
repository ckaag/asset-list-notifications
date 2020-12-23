package com.github.ckaag.asset.list.notifications.portlet.portlet;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;

import javax.portlet.*;

import com.github.ckaag.asset.list.notifications.portlet.service.ListNotificationSender;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author chris
 */
@Component(
        immediate = true,
        property = {
                "com.liferay.portlet.display-category=category.sample",
                "com.liferay.portlet.header-portlet-css=/css/main.css",
                "com.liferay.portlet.instanceable=true",
                "javax.portlet.display-name=AssetListNotification",
                "javax.portlet.init-param.config-template=/configuration.jsp",
                "javax.portlet.init-param.template-path=/",
                "javax.portlet.init-param.view-template=/view.jsp",
                "javax.portlet.name=" + AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION,
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.security-role-ref=power-user,user"
        },
        service = Portlet.class
)
public class AssetListNotificationPortlet extends MVCPortlet {
    private static final Log log = LogFactoryUtil.getLog(AssetListNotificationPortlet.class);

    @Reference
    private ListNotificationSender listNotificationSender;

    @Override
    public void doView(
            RenderRequest renderRequest, RenderResponse renderResponse)
            throws IOException, PortletException {

        renderRequest.setAttribute(AssetListNotificationPortlet.class.getName(), assetListNotificationConfiguration);

        for (Map.Entry<AssetListEntry, PortletPreferences> entry : listNotificationSender.getNotificationPortlets().entrySet()) {
            LocalDateTime lastModifiedDate = listNotificationSender.getLastModifiedDate(entry.getKey(), entry.getValue());
            log.info("last modified: " + lastModifiedDate.format(DateTimeFormatter.ISO_DATE_TIME));
            List<AssetEntry> elements = listNotificationSender.getEntriesSinceLastModified(entry.getKey(), entry.getValue(), lastModifiedDate);
            if (!elements.isEmpty()) {
                //listNotificationSender.updateLastDates(entry.getKey(), entry.getValue(), true, true);
            }
            //log.info("entry " + entry.getKey().getTitle() + "has entries: " + elements.size());
            for (AssetEntry element : elements) {
                //log.info("element: " + element.getTitle());
            }
        }

        super.doView(renderRequest, renderResponse);
    }

    @Activate
    @Modified
    protected void activate(Map<Object, Object> properties) {
        assetListNotificationConfiguration = ConfigurableUtil.createConfigurable(AssetListNotificationConfiguration.class, properties);
    }

    private volatile AssetListNotificationConfiguration assetListNotificationConfiguration;


    public void updateSubscriptionForMeManually() {
        //get portlet pref, extract asset list entry, and write to list
        throw new UnsupportedOperationException("not yet implemented");
    }
}