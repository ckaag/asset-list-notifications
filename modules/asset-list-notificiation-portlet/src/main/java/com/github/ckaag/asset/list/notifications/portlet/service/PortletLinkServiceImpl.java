package com.github.ckaag.asset.list.notifications.portlet.service;

import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.util.Portal;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component(immediate = true, service = PortletLinkService.class)
public class PortletLinkServiceImpl implements PortletLinkService {
    public static final String UNDERSCORE = "_";
    public static final String GUEST = "/guest";
    public static final String WEB = "/web";
    public static final String P_P_ID = "p_p_id";

    //e.g. /web/guest/gallery?p_p_id=com_gitlab_oetti_portlet_event_participation_by_mail_PortletEventParticipationByMailPortlet_INSTANCE_yGoPtBp1Rc12&_com_gitlab_oetti_portlet_event_participation_by_mail_PortletEventParticipationByMailPortlet_INSTANCE_yGoPtBp1Rc12_jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJldmVudElkIjotMSwiZXhwIjoxNjAyMjY0MjIzLCJpYXQiOjE2MDE2NTk0MjMsImVtYWlsIjoidGVzdEBsaWZlcmF5LmNvbSJ9.6W8QmFobaDvi0zRccAUAD9up_5yANJLdnPeQ2FEUbhs
    ///web/guest/gallery?p_p_id=com_gitlab_oetti_portlet_event_participation_by_mail_PortletEventParticipationByMailPortlet_INSTANCE_yGoPtBp1Rc12&_com_gitlab_oetti_portlet_event_participation_by_mail_PortletEventParticipationByMailPortlet_INSTANCE_yGoPtBp1Rc12_jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJldmVudElkIjotMSwiZXhwIjoxNjAyMjY1MTY1LCJpYXQiOjE2MDE2NjAzNjUsImVtYWlsIjoidGVzdEBsaWZlcmF5LmNvbSJ9.NJK7RF5f8B3ASEdin62V_nANEr4LFn5Mk5GvKE1l3SM&_com_gitlab_oetti_portlet_event_participation_by_mail_PortletEventParticipationByMailPortlet_INSTANCE_yGoPtBp1Rc12_jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJldmVudElkIjotMSwiZXhwIjoxNjAyMjY1MTY1LCJpYXQiOjE2MDE2NjAzNjUsImVtYWlsIjoidGVzdEBsaWZlcmF5LmNvbSJ9.NJK7RF5f8B3ASEdin62V_nANEr4LFn5Mk5GvKE1l3SM&_com_gitlab_oetti_portlet_event_participation_by_mail_PortletEventParticipationByMailPortlet_INSTANCE_yGoPtBp1Rc12_jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJldmVudElkIjotMSwiZXhwIjoxNjAyMjY1MTY1LCJpYXQiOjE2MDE2NjAzNjUsImVtYWlsIjoidGVzdEBsaWZlcmF5LmNvbSJ9.NJK7RF5f8B3ASEdin62V_nANEr4LFn5Mk5GvKE1l3SM

    @Reference
    private Portal portal;

    @Reference
    private LayoutLocalService layoutLocalService;

    @Override
    public String buildRelativePortletUrl(long plid, String portletId, Map<String, List<String>> portletSpecificParameters, Map<String, List<String>> generalParameters) {
        final Layout layout = layoutLocalService.fetchLayout(plid);
        if (layout == null) {
            return null;
        }
        final String path = (layout.isPrivateLayout() ? GUEST : WEB) + layout.getGroup().getFriendlyURL() + layout.getFriendlyURL();
        final String query = buildQuery(portletId, Objects.requireNonNullElse(portletSpecificParameters, Collections.emptyMap()), Objects.requireNonNullElseGet(generalParameters, () -> Map.of(P_P_ID, List.of(portletId))));
        return path + (query != null && !query.isBlank() ? "?" + query : "");
    }

    private String buildQuery(String portletId, Map<String, List<String>> portletSpecificParameters, Map<String, List<String>> generalParameters) {
        if (portletId == null) {
            return null;
        }
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : portletSpecificParameters.entrySet()) {
            for (String value : entry.getValue()) {
                entries.add(UNDERSCORE + portletId + UNDERSCORE + entry.getKey() + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        for (Map.Entry<String, List<String>> entry : generalParameters.entrySet()) {
            for (String value : entry.getValue()) {
                entries.add(entry.getKey() + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return String.join("&", entries);
    }

}
