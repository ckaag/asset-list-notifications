package com.github.ckaag.asset.list.notifications.portlet.portlet;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;
import com.github.ckaag.asset.list.notifications.portlet.service.ListNotificationSender;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.asset.list.service.AssetListEntryLocalService;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.*;

import javax.portlet.*;
import java.io.IOException;
import java.util.*;

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
    public static final String ASSET_REDIRECT_URL = "assetRedirectUrl";
    private static final Log log = LogFactoryUtil.getLog(AssetListNotificationPortlet.class);
    // to dynamically use assetrenderers to get view urls for each asset if we want
    private final Map<String, List<AssetRendererFactory<?>>> assetRendererFactoryHashMap = new HashMap<>();
    @Reference
    private ListNotificationSender listNotificationSender;
    @Reference
    private AssetListEntryLocalService assetListEntryLocalService;
    private volatile AssetListNotificationConfiguration assetListNotificationConfiguration;

    @Override
    public void doView(
            RenderRequest renderRequest, RenderResponse renderResponse)
            throws IOException, PortletException {

        checkForAssetRedirect(renderRequest, renderResponse);


        renderRequest.setAttribute(AssetListNotificationConfiguration.class.getName(), assetListNotificationConfiguration);

        renderRequest.setAttribute(AssetListNotificationPortlet.class.getName(), assetListNotificationConfiguration);

        renderRequest.setAttribute("valueChangeButton", getTextForChangeButton(renderRequest));

        super.doView(renderRequest, renderResponse);
    }

    private String getTextForChangeButton(RenderRequest renderRequest) throws PortletException {
        try {
            String receiverSelectMode = renderRequest.getPreferences().getValue("receiverSelectMode", "");
            if ("forced".equals(receiverSelectMode)) {
                return LanguageUtil.get(renderRequest.getLocale(), "assetlistnotification.disabled");
            }
            return LanguageUtil.get(renderRequest.getLocale(), isSubscribed(renderRequest) ? "assetlistnotification.unsubscribe" : "assetlistnotification.subscribe");
        } catch (Exception e) {
            throw new PortletException(e);
        }
    }

    private boolean isSubscribed(RenderRequest renderRequest) throws PortalException {
        ThemeDisplay td = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
        ListNotificationSender service = this.listNotificationSender;
        AssetListEntry entity = getAssetEntryList(renderRequest);
        long userId = td.getUserId();
        String receiverSelectMode = renderRequest.getPreferences().getValue("receiverSelectMode", "");
        boolean isOptIn = "optin".equals(receiverSelectMode);
        boolean isOptOut = "optout".equals(receiverSelectMode);
        Optional<Boolean> prev = service.getOptedInStatus(entity, userId);
        return (!isOptIn && !isOptOut) || (isOptIn && prev.orElse(false)) || (isOptOut && !(prev.isPresent() && !prev.get()));
    }

    private AssetListEntry getAssetEntryList(PortletRequest portletRequest) throws PortalException {
        String idTxt = portletRequest.getPreferences().getValue("selectedAssetList", "0");
        long id = Long.parseLong(idTxt);
        return assetListEntryLocalService.getAssetListEntry(id);
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

    @SuppressWarnings("unused")
    public void toggleSubscriptionForMeManually(ActionRequest actionRequest, ActionResponse actionResponse) throws PortalException {
        ThemeDisplay td = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);
        ListNotificationSender service = this.listNotificationSender;
        AssetListEntry entity = getAssetEntryList(actionRequest);
        long userId = td.getUserId();
        String receiverSelectMode = actionRequest.getPreferences().getValue("receiverSelectMode", "");
        boolean isOptIn = "optin".equals(receiverSelectMode);
        boolean isOptOut = "optout".equals(receiverSelectMode);
        if (!isOptIn && !isOptOut) {
            throw new IllegalArgumentException("cannot opt in or out according to portlet settings");
        }
        Optional<Boolean> prev = service.getOptedInStatus(entity, userId);
        if (prev.isEmpty()) {
            if (isOptIn) {
                service.optIntoNotification(entity, userId);
            } else {
                service.optOutOfNotification(entity, userId);
            }
        } else if (prev.get()) {
            service.optOutOfNotification(entity, userId);
        } else {
            service.optIntoNotification(entity, userId);
        }
    }

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