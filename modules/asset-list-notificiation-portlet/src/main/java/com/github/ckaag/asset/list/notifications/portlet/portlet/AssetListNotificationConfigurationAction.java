package com.github.ckaag.asset.list.notifications.portlet.portlet;

import com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys;
import com.liferay.asset.list.model.AssetListEntry;
import com.liferay.asset.list.service.AssetListEntryService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.ConfigurationAction;
import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.settings.LocalizedValuesMap;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import org.osgi.service.component.annotations.*;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component(
        configurationPid = AssetListNotificationPortletKeys.NOTIFICATION_CONFIG_PID,
        configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true,
        property = "javax.portlet.name=" + AssetListNotificationPortletKeys.ASSETLISTNOTIFICATION,
        service = ConfigurationAction.class
)
public class AssetListNotificationConfigurationAction extends DefaultConfigurationAction {
    @Override
    public void include(
            PortletConfig portletConfig, HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse)
            throws Exception {

        httpServletRequest.setAttribute(
                AssetListNotificationConfiguration.class.getName(),
                assetListNotificationConfiguration);

        ThemeDisplay td = (ThemeDisplay) httpServletRequest.getAttribute(WebKeys.THEME_DISPLAY);
        long groupId = td.getScopeGroupId();
        List<AssetListEntry> entries = assetListEntryService.getAssetListEntries(groupId, 0, 1_000_000, null);
        httpServletRequest.setAttribute("availableAssetListEntries", entries);

        super.include(portletConfig, httpServletRequest, httpServletResponse);
    }

    @Override
    public void processAction(
            PortletConfig portletConfig, ActionRequest actionRequest,
            ActionResponse actionResponse)
            throws Exception {
        ThemeDisplay td = (ThemeDisplay) actionRequest.getAttribute(WebKeys.THEME_DISPLAY);

        String selectedAssetList = ParamUtil.getString(actionRequest, "selectedAssetList");
        String emailFromName = ParamUtil.getString(actionRequest, "emailFromName");
        String emailFromAddress = ParamUtil.getString(actionRequest, "emailFromAddress");
        String receiverSelectMode = ParamUtil.getString(actionRequest, "receiverSelectMode");
        String userSpecificMode = ParamUtil.getString(actionRequest, "userSpecificMode");
        String minutesBetweenMails = ParamUtil.getString(actionRequest, "minutesBetweenMails");
        Map<Locale, String> localizedBodyTemplate = LocalizationUtil.getLocalizationMap(actionRequest, "localizedBodyTemplate");
        String localizedBodyTemplateXml = buildXmlFromMap(localizedBodyTemplate, "MailBody");
        Map<Locale, String> localizedSubjectTemplate = LocalizationUtil.getLocalizationMap(actionRequest, "localizedSubjectTemplate");
        String localizedSubjectTemplateXml = buildXmlFromMap(localizedSubjectTemplate, "MailSubject");

        _log.info("selectedAssetList = " + selectedAssetList);
        _log.info("localizedBodyTemplate = " + localizedBodyTemplateXml);

        setPreference(actionRequest, "selectedAssetList", selectedAssetList);
        setPreference(actionRequest, "localizedBodyTemplate", localizedBodyTemplateXml);
        setPreference(actionRequest, "localizedSubjectTemplate", localizedSubjectTemplateXml);
        setPreference(actionRequest, "emailFromName", emailFromName);
        setPreference(actionRequest, "emailFromAddress", emailFromAddress);
        setPreference(actionRequest, "receiverSelectMode", receiverSelectMode);
        setPreference(actionRequest, "userSpecificMode", userSpecificMode);
        setPreference(actionRequest, "minutesBetweenMails", minutesBetweenMails);

        super.processAction(portletConfig, actionRequest, actionResponse);
    }

    @Activate
    @Modified
    protected void activate(Map<Object, Object> properties) {
        assetListNotificationConfiguration = ConfigurableUtil.createConfigurable(
                AssetListNotificationConfiguration.class, properties);
    }

    private static final Log _log = LogFactoryUtil.getLog(
            AssetListNotificationConfigurationAction.class);

    private volatile AssetListNotificationConfiguration assetListNotificationConfiguration;

    @Reference
    private AssetListEntryService assetListEntryService;

    private String buildXmlFromMap(Map<Locale, String> translationMap, String key) {
        LocalizedValuesMap localizedValuesMap = new LocalizedValuesMap(StringPool.BLANK);
        for (Map.Entry<Locale, String> entry : translationMap.entrySet()) {
            localizedValuesMap.put(entry.getKey(), entry.getValue());
        }
        return LocalizationUtil.getXml(localizedValuesMap, key);
    }

}
