<%@page import="com.dotmarketing.filters.CMSFilter"%>
<%@ page import="java.util.*" %>
<%@ page import="java.net.URL" %>
<%@ page import="java.lang.Exception" %>
<%@ page import="com.dotmarketing.util.UtilMethods" %>
<%@ page import="com.dotmarketing.beans.Host" %>
<%@ page import="com.dotmarketing.business.web.WebAPILocator"%>
<%@ page import="com.dotmarketing.business.CacheLocator"%>
<%@ page import="com.dotmarketing.util.Logger"%>
<%@ page import="com.dotmarketing.db.DbConnectionFactory"%>
<%@ page import="com.liferay.portal.util.ImageKey" %>
<%@ page import="com.liferay.portal.util.WebKeys" %>
<%@page import="com.liferay.portal.language.LanguageUtil"%>
<%@page import="com.liferay.portal.model.Company"%>
<%@page import="com.dotmarketing.util.CompanyUtils"%> 
<%try{		
	Host host = WebAPILocator.getHostWebAPI().getCurrentHost(request);
	Host defaultHost = WebAPILocator.getHostWebAPI().findDefaultHost(WebAPILocator.getUserWebAPI().getSystemUser(),false);
	Company company = CompanyUtils.getDefaultCompany();
	String portalUrl =  company.getPortalURL();
	String IMAGE_PATH = (String) application.getAttribute(WebKeys.IMAGE_PATH);
	String defaultImage =  IMAGE_PATH+"/company_logo?img_id="+company.getCompanyId()+"&key="+ImageKey.get(company.getCompanyId());
		
	String ep_originatingHost = host.getHostname();
	String ep_errorCode = "503";
    String ep_error_uri = (String)request.getAttribute("javax.servlet.forward.request_uri");
	
	// Get 503 from virtual link
	String pointer = (String) com.dotmarketing.cache.VirtualLinksCache.getPathFromCache(host.getHostname() + ":/cms503Page");
	if (!UtilMethods.isSet(pointer)) {
		pointer = (String) com.dotmarketing.cache.VirtualLinksCache.getPathFromCache("/cms503Page");
	}
	
	Logger.debug(this, "cms503Page path is: " + pointer);
	
	// if we have a virtual link, see if the page exists.  pointer will be set to null if not
	if (UtilMethods.isSet(pointer)) {
		if (pointer.startsWith("/")) {
		// if the virtual link is a relative path, the path is validated within the current host
			pointer = com.dotmarketing.cache.LiveCache.getPathFromCache(pointer, host);	
			Logger.debug(this, "cms503Page relative path is: " + pointer + " - host: " + host.getHostname() + " and pointer: " + pointer);
		} else {
		// if virtual link points to a host or alias in dotCMS server, the path needs to be validated.
		// Otherwise, the original external pointer is kept for the redirect

			try {
				URL errorPageUrl = new URL(pointer);
				String errorPageHost = errorPageUrl.getHost();
				String errorPagePath = errorPageUrl.getPath();
				
				Logger.debug(this, "cms503Page - errorPageHost: " + errorPageHost + " and errorPagePath: " + errorPagePath);
				
				Host internalHost = WebAPILocator.getHostWebAPI().findByName(errorPageHost, WebAPILocator.getUserWebAPI().getAnonymousUser(), true);
				Host internalAlias = WebAPILocator.getHostWebAPI().findByAlias(errorPageHost, WebAPILocator.getUserWebAPI().getAnonymousUser(), true);
				
				// 503 Virtual Link is pointing to a host in dotCMS
				if ( internalHost != null) {				
					String absPointer = com.dotmarketing.cache.LiveCache.getPathFromCache(errorPagePath, internalHost);
					if (absPointer == null) {
						pointer = null;
					}
					Logger.debug(this, "cms503age absolute internal path is: " + pointer + " - internalHost: " + internalHost.getHostname() + " and errorPagePath: " + errorPagePath);
				
				// 503 Virtual Link is poiting to an alias in dotCMS
				} else if ( internalAlias != null) {
					String absPointer = com.dotmarketing.cache.LiveCache.getPathFromCache(errorPagePath, internalAlias);
					if (absPointer == null) {
						pointer = null;
					}
					Logger.debug(this, "cms503Page absolute internal path is: " + pointer + " - internalAlias: " + internalAlias.getHostname() + " and errorPagePath: " + errorPagePath);
				
				// 503 Virtual Link is pointing to an external page
				} else {
					Logger.debug(this, "cms503Page absolute external path is: " + pointer);
				}
					
			} catch (Exception e){
				Logger.error(this, "cms503Page path is incorrect: " + pointer + e.getMessage(), e);
				pointer = null;
			}
		}
	}
	
	// if we have virtual link and page exists, redirect or forward
    if(UtilMethods.isSet(pointer) ){
        if (pointer.startsWith("/")) {
            Logger.debug(this, "cms503Page forwarding to relative path: " + pointer);
            request.setAttribute(CMSFilter.CMS_FILTER_URI_OVERRIDE, pointer);
            // Serving a page through the velocity servlet
            request.getRequestDispatcher("/servlets/VelocityServlet").forward(request, response);
        } else {
            pointer = pointer + "?ep_originatingHost="+ep_originatingHost+"&ep_errorCode="+ep_errorCode+"&ep_error_uri="+ep_error_uri;
            Logger.debug(this, "cms503Page redirecting to absolute path: " + pointer);
            response.sendRedirect(pointer);
        }
        return;
    }
	
	
	%>




<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>
<head>
	<link rel="shortcut icon" href="http://<%=defaultHost.getHostname()%>/home/favicon.ico"" type="image/x-icon">

	<title><%= LanguageUtil.get(pageContext,"503-page-title") %></title>

	<style type="text/css">
		body{
			font-family: helvetica, san-serif;
			padding:20px;
			margin-top:0px;
		}
		#main {
			width: 400px;
		}
		#footer {
			text-align:center;
		}
		h1 {
			font-size: 20px;

		}
		#logo{
			float: left;
		}
		#text{
			float: left;
		}
	</style>

</head>
<body>
<div id="main">

	<div id="text">	
		<h1><%= LanguageUtil.get(pageContext,"503-title") %></h1>

		<p><%= LanguageUtil.get(pageContext,"503-body2") %></p>
	</div>

<br clear="all"/>&nbsp;<br clear="all"/>

</body>
</html>
<%} catch( Exception e){
	Logger.error(this, "cms503Page cant display " + e.getMessage(), e);	
%>
503
	
	
	
<% }finally{
	DbConnectionFactory.closeConnection();
}%>

