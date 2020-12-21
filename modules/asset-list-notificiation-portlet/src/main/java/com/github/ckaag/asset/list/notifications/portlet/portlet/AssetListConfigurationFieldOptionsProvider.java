package com.github.ckaag.asset.list.notifications.portlet.portlet;

import com.liferay.asset.list.service.AssetListEntryLocalService;
import com.liferay.configuration.admin.definition.ConfigurationFieldOptionsProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.github.ckaag.asset.list.notifications.portlet.constants.AssetListNotificationPortletKeys.NOTIFICATION_CONFIG_PID;

@Component(
        property = {
                "configuration.field.name=selectedAssetList",
                "configuration.pid=" + NOTIFICATION_CONFIG_PID
        },
        service = ConfigurationFieldOptionsProvider.class
)
public class AssetListConfigurationFieldOptionsProvider implements ConfigurationFieldOptionsProvider {

    @Reference
    private AssetListEntryLocalService assetListEntryLocalService;

    @Override
    public List<Option> getOptions() {
        return assetListEntryLocalService.getAssetListEntries(0, 1_000_000).stream().map(ale -> new Option() {
            @Override
            public String getLabel(Locale locale) {
                return ale.getTitle();
            }

            @Override
            public String getValue() {
                return String.valueOf(ale.getPrimaryKey());
            }
        }).collect(Collectors.toList());
    }
}
