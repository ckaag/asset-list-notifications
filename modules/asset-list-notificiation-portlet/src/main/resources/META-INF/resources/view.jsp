<%@ include file="/init.jsp" %>


<c:if test="${not empty assetRedirectUrl}">
    <script>
        window.location.href = '${assetRedirectUrl}';
    </script>
</c:if>


<portlet:actionURL name="toggleSubscriptionForMeManually" var="toggleActionUrl" />

<aui:form action="<%= toggleActionUrl %>" name="fm">
	<aui:button-row>
		<aui:button name="submitButton" type="submit" value="${valueChangeButton}" disabled="${disableChangeButton}" />
	</aui:button-row>
</aui:form>
