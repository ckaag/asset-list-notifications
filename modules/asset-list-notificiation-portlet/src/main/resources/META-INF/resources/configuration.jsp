<%@ page import="com.liferay.portal.kernel.util.Constants" %>

<%@ include file="/init.jsp" %>

<liferay-portlet:actionURL
	portletConfiguration="<%= true %>"
	var="configurationActionURL"
/>

<liferay-portlet:renderURL
	portletConfiguration="<%= true %>"
	var="configurationRenderURL"
/>

<div class="container">

<aui:form action="<%= configurationActionURL %>" method="post" name="fm">
	<aui:input
		name="<%= Constants.CMD %>"
		type="hidden"
		value="<%= Constants.UPDATE %>"
	/>

	<aui:input
		name="redirect"
		type="hidden"
		value="<%= configurationRenderURL %>"
	/>

	<aui:fieldset>
		<aui:select
			label="selected-asset-list-field-label"
			name="selectedAssetList"
			value="<%= selectedAssetList %>"
		>
            <c:forEach var="entry" items="${availableAssetListEntries}">
                <aui:option value="${entry.getPrimaryKey()}">${entry.getTitle()}</aui:option>
            </c:forEach>
		</aui:select>
	</aui:fieldset>


    <aui:select label="receiver-select-mode-label" name="receiverSelectMode" value="<%= receiverSelectMode %>">
        <aui:option value="optin"><liferay-ui:message key="assetlistnotification.opt-in-mode"/></aui:option>
        <aui:option value="optout"><liferay-ui:message key="assetlistnotification.opt-out-mode"/></aui:option>
        <aui:option value="forced"><liferay-ui:message key="assetlistnotification.this-is-not-a-choice"/></aui:option>
    </aui:select>
    <aui:select label="user-specific-mode-label" name="userSpecificMode" value="<%= userSpecificMode %>">
        <aui:option value="true"><liferay-ui:message key="assetlistnotification.user-specific-mode"/></aui:option>
        <%-- <aui:option value="false"><liferay-ui:message key="assetlistnotification.everybody-gets-same"/></aui:option> --%>
    </aui:select>


    <aui:input name="minutesBetweenMails" value="<%= minutesBetweenMails %>" type="number" min="1" max="43200" />

    <aui:input name="emailFromAddress" value="<%= emailFromAddress %>" type="email" />
    <aui:input name="emailFromName" value="<%= emailFromName %>" type="text" />

    <label class="control-label" > Mail subject template </label>
    <liferay-ui:input-localized name="localizedSubjectTemplate" xml="<%= localizedSubjectTemplate %>" />

    <label class="control-label" > Mail body template </label>
    <liferay-ui:input-localized name="localizedBodyTemplate" xml="<%= localizedBodyTemplate %>" type="editor" />



    <liferay-ui:message key="assetlistnotification.replacement-description"/>

    <ul>
    <c:forEach var="entry" items="${placeholders}">
        <li>${entry}</li>
    </c:forEach>
    </ul>


	<aui:button-row>
		<aui:button type="submit"></aui:button>
	</aui:button-row>
</aui:form>

</div>
