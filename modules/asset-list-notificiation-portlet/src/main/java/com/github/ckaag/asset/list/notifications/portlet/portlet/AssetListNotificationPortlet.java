package com.github.ckaag.asset.list.notifications.portlet.portlet;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;

import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import java.io.IOException;
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


        @Override
        public void doView(
                RenderRequest renderRequest, RenderResponse renderResponse)
                throws IOException, PortletException {

                renderRequest.setAttribute(AssetListNotificationPortlet.class.getName(),assetListNotificationConfiguration);

                super.doView(renderRequest, renderResponse);
        }

        @Activate
        @Modified
        protected void activate(Map<Object, Object> properties) {
                assetListNotificationConfiguration = ConfigurableUtil.createConfigurable(AssetListNotificationConfiguration.class, properties);
        }

        private static final Log _log = LogFactoryUtil.getLog(AssetListNotificationPortlet.class);

        private volatile AssetListNotificationConfiguration assetListNotificationConfiguration;
}