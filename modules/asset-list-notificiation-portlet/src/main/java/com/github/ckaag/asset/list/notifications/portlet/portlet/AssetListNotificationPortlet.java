package com.github.ckaag.asset.list.notifications.portlet.portlet;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;
import com.github.ckaag.asset.list.notifications.portlet.service.ListNotificationSender;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.util.ParamUtil;
import org.osgi.service.component.annotations.*;

import javax.portlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys.CLASS_NAME_PARAM;
import static com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys.CLASS_PK_PARAM;

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
    public static final String ASSET_REDIRECT_URL = "assetRedirectUrl";

    @Reference
    private ListNotificationSender listNotificationSender;

    @Override
    public void doView(
            RenderRequest renderRequest, RenderResponse renderResponse)
            throws IOException, PortletException {

        checkForAssetRedirect(renderRequest, renderResponse);

        renderRequest.setAttribute(AssetListNotificationPortlet.class.getName(), assetListNotificationConfiguration);

        renderRequest.setAttribute("valueChangeButton", getTextForChangeButton(renderRequest));

        super.doView(renderRequest, renderResponse);
    }

    private String getTextForChangeButton(RenderRequest renderRequest) {
        return LanguageUtil.get(renderRequest.getLocale(), isSubscribed(renderRequest) ? "assetlistnotification.unsubscribe" : "assetlistnotification.subscribe");
    }

    private boolean isSubscribed(RenderRequest renderRequest) {
        //TODO: implement functionality based on stored values and configuration
        return false;
    }

    private void checkForAssetRedirect(RenderRequest renderRequest, RenderResponse renderResponse) {
        String className = ParamUtil.get(renderRequest, CLASS_NAME_PARAM, "");
        long classPK = ParamUtil.get(renderRequest, CLASS_PK_PARAM, 0L);
        if (!className.isEmpty()) {
            if (classPK > 0L) {
                String urlToAsset = getUrlToAsset(renderRequest, renderResponse, className, classPK);
                if (urlToAsset != null && !urlToAsset.trim().isEmpty()) {
                    renderRequest.setAttribute(ASSET_REDIRECT_URL, urlToAsset);
                }
            }
        }
    }

    private String getUrlToAsset(RenderRequest renderRequest, RenderResponse renderResponse, String className, long classPK) {
        if (assetRendererFactoryHashMap.containsKey(className)) {
            if (!assetRendererFactoryHashMap.get(className).isEmpty()) {
                try {
                    if (renderRequest instanceof LiferayPortletRequest) {
                        if (renderResponse instanceof LiferayPortletResponse) {
                            return assetRendererFactoryHashMap.get(className).get(0).getAssetRenderer(classPK).getURLViewInContext((LiferayPortletRequest) renderRequest, (LiferayPortletResponse) renderResponse, "#404-asset-link-not-found");
                        }
                    }
                } catch (Exception e) {
                    log.error(e);
                    return null;
                }
            }
        }
        return null;
    }

    @Activate
    @Modified
    protected void activate(Map<Object, Object> properties) {
        assetListNotificationConfiguration = ConfigurableUtil.createConfigurable(AssetListNotificationConfiguration.class, properties);
    }

    private volatile AssetListNotificationConfiguration assetListNotificationConfiguration;


    @SuppressWarnings("unused")
    public void toggleSubscriptionForMeManually(ActionRequest actionRequest, ActionResponse actionResponse) {
        //get portlet pref, extract asset list entry, and write to list
        throw new UnsupportedOperationException("not yet implemented");
    }


    // to dynamically use assetrenderers to get view urls for each asset if we want
    private final Map<String, List<AssetRendererFactory<?>>> assetRendererFactoryHashMap = new HashMap<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeAssetRendererFactory")
    protected void addAssetRendererFactory(AssetRendererFactory<?> factory) {
        assetRendererFactoryHashMap.computeIfAbsent(factory.getClassName(), k -> new ArrayList<>()).add(factory);
    }

    @SuppressWarnings("unused")
    protected void removeAssetRendererFactory(AssetRendererFactory<?> factory) {
        if (assetRendererFactoryHashMap.containsKey(factory.getClassName())) {
            List<AssetRendererFactory<?>> entry = assetRendererFactoryHashMap.get(factory.getClassName());
            if (entry.contains(factory)) {
                entry.remove(factory);
                if (entry.isEmpty()) {
                    assetRendererFactoryHashMap.remove(factory.getClassName());
                }
            }
        }
    }
}