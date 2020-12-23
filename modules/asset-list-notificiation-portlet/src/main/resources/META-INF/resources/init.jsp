<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %><%@
taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %><%@
taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %><%@
taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>



<%@ page import="com.github.ckaag.asset.list.notifications.portlet.portlet.AssetListNotificationConfiguration" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>


<liferay-theme:defineObjects />

<portlet:defineObjects />


<%
	AssetListNotificationConfiguration myConfiguration = (AssetListNotificationConfiguration) renderRequest.getAttribute(AssetListNotificationConfiguration.class.getName());
%>
<%
	String selectedAssetList = StringPool.BLANK;
	String localizedBodyTemplate = StringPool.BLANK;
	String localizedSubjectTemplate = StringPool.BLANK;
	String emailFromName = StringPool.BLANK;
	String emailFromAddress = StringPool.BLANK;
	String receiverSelectMode = StringPool.BLANK;
	String userSpecificMode = StringPool.BLANK;
	String minutesBetweenMails = StringPool.BLANK;
%>
<%
	if (Validator.isNotNull(myConfiguration)) {
		selectedAssetList = portletPreferences.getValue("selectedAssetList", myConfiguration.selectedAssetList());
		localizedBodyTemplate = portletPreferences.getValue("localizedBodyTemplate", myConfiguration.localizedBodyTemplate());
		localizedSubjectTemplate = portletPreferences.getValue("localizedSubjectTemplate", myConfiguration.localizedSubjectTemplate());
		emailFromName = portletPreferences.getValue("emailFromName", myConfiguration.emailFromName());
		emailFromAddress = portletPreferences.getValue("emailFromAddress", myConfiguration.emailFromAddress());
		receiverSelectMode = portletPreferences.getValue("receiverSelectMode", myConfiguration.receiverSelectMode());
		userSpecificMode = portletPreferences.getValue("userSpecificMode", myConfiguration.userSpecificMode());
		minutesBetweenMails = portletPreferences.getValue("minutesBetweenMails", myConfiguration.minutesBetweenMails());
	}
%>
