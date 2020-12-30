package com.github.ckaag.asset.list.notifications.portlet.service;

import com.liferay.portal.kernel.model.PortletPreferences;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.List;
import java.util.Map;

public interface PortletLinkService {

    String buildRelativePortletUrl(long plid, String portletId, Map<String, List<String>> portletSpecificParameters, Map<String, List<String>> generalParameters);

    default String buildRelativePortletUrl(long plid, String portletId, Map<String, List<String>> portletSpecificParameters) {
        return this.buildRelativePortletUrl(plid, portletId, portletSpecificParameters, null);
    }

    default String buildRelativePortletUrl(PortletPreferences portletPreferences, Map<String, List<String>> portletSpecificParameters) {
        if (portletPreferences == null) return null;
        return this.buildRelativePortletUrl(portletPreferences.getPlid(), portletPreferences.getPortletId(), portletSpecificParameters, null);
    }

}
