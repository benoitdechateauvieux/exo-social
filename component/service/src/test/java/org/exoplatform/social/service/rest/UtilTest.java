/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.service.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.service.test.AbstractServiceTest;


/**
 * Unit Test for {@link Util}.
 *
 * @author <a href="http://hoatle.net">hoatle (hoatlevan at gmail dot com)</a>
 * @since Jun 16, 2011
 */
public class UtilTest extends AbstractServiceTest {

  /**
   * Tests {@link Util#getMediaType(String, String[])}.
   */
  public void testGetMediaType() {
    // unsupported media type
    try {
      Util.getMediaType("xml", new String[] {"json"});
      fail("Expecting WebApplicationException: 415 status");
    } catch (WebApplicationException wae) {
      assertEquals(415, wae.getResponse().getStatus());
    }

    MediaType jsonMediaType = Util.getMediaType("json", new String[]{"json"});
    assertEquals(MediaType.APPLICATION_JSON_TYPE, jsonMediaType);


    MediaType xmlMediaType = Util.getMediaType("xml", new String[]{"json", "xml", "atom", "rss"});
    assertEquals(MediaType.APPLICATION_XML_TYPE, xmlMediaType);

    try {
      Util.getMediaType("rss", new String[]{"json", "xml", "rss"});
      fail("Expecting WebApplicationException: 406 status");
    } catch (WebApplicationException wae) {
      assertEquals(406, wae.getResponse().getStatus());
    }
  }

  /**
   * Tests {@link org.exoplatform.social.service.rest.Util#getIdentityManager()}
   */
  public void testGetIdentityManager() {
    IdentityManager identityManager = Util.getIdentityManager();
    assertNotNull("identityManager must not be null", identityManager);
  }

  /**
   * Tests {@link Util#getIdentityManager(String)}.
   */
  public void testGetIdentityManagerByPortalContainerName() {
    IdentityManager identityManager = Util.getIdentityManager("portal");
    assertNotNull("identityManager must not be null", identityManager);
  }

  /**
   * Tests {@link Util#getSpaceService()}.
   */
  public void testGetSpaceService() {
    SpaceService spaceService = Util.getSpaceService();
    assertNotNull("spaceService must not be null", spaceService);
  }

  /**
   * Tests {@link Util#getSpaceService(String)}.
   */
  public void testGetSpaceServiceByPortalContainerName() {
    SpaceService spaceService = Util.getSpaceService("portal");
    assertNotNull("spaceService must not be null", spaceService);
  }

  /**
   * Tests {@link Util#getActivityManager()}.
   */
  public void testGetActivityManager() {
    ActivityManager activityManager = Util.getActivityManager();
    assertNotNull("activityManager must not be null", activityManager);
  }

  /**
   * Tests {@link Util#getActivityManager(String)}.
   */
  public void testGetActivityManagerByPortalContainerName() {
    ActivityManager activityManager = Util.getActivityManager("portal");
    assertNotNull("activityManager must not be null", activityManager);
  }

  /**
   * Tests {@link Util#getRelationshipManager()}.
   */
  public void testGetRelationshipManager() {
    RelationshipManager relationshipManager = Util.getRelationshipManager();
    assertNotNull("relationshipManager must not be null", relationshipManager);
  }

  /**
   * Tests {@link Util#getRelationshipManager(String)}.
   */
  public void testGetRelationshipManagerByPortalContainerName() {
    RelationshipManager relationshipManager = Util.getRelationshipManager("portal");
    assertNotNull("relationshipManager must not be null", relationshipManager);
  }

  /**
   * Tests {@link Util#getPortalContainerByName(String)}.
   */
  public void testGetPortalContainerByName() {
    PortalContainer portalContainer1 = Util.getPortalContainerByName("wrong");
    assertNull("portalContainer1 must be null", portalContainer1);
    PortalContainer portalContainer2 = Util.getPortalContainerByName("portal");
    assertNotNull("portalContainer2 must not be null", portalContainer2);
  }


  /**
   * Tests {@link Util#convertTimestampToTimeString(long)}.
   */
  public void testConvertTimestampToTimeString() {
    long timestamp = 1308643759381L;
    //With GTM +07:00: Tue Jun 21 15:09:19 +0700 2011
    if ("GTM +07:00".equals(TimeZone.getDefault().getID())) {
      String expected = "Tue Jun 21 15:09:19 +0700 2011";
      assertEquals(expected, Util.convertTimestampToTimeString(timestamp));
    }
  }

  /**
   * Tests {@link Util#getBaseUrl()}.
   */
  public void testGetBaseUrl() {
    String baseUrl1 = "http://localhost:8080";

    String urlRequest1 = baseUrl1 + "/social/rest/v1/identity/123456.json?fields=fullName,avatarUrl";
    setFakeCurrentEnvironmentContext(urlRequest1);
    String gotBaseUrl1 = Util.getBaseUrl();
    assertEquals("gotBaseUrl1 must be: " + baseUrl1, baseUrl1, gotBaseUrl1);

    String urlRequest2 = baseUrl1 + "/social/rest/v1/identity/123456.json#id?fields=fullName,avatarUrl&limit=20";
    setFakeCurrentEnvironmentContext(urlRequest2);
    String gotBaseUrl2 = Util.getBaseUrl();
    assertEquals("gotBaseUrl2 must be: " + baseUrl1, baseUrl1, gotBaseUrl2);

    String baseUrl2 = "http://www.social.demo.exoplatform.org";
    String urlRequest3 = baseUrl2 + "/social/rest/v1/identity/123456.json?fields=fullName,avatarUrl";
    setFakeCurrentEnvironmentContext(urlRequest3);
    String gotBaseUrl3 = Util.getBaseUrl();
    assertEquals("gotBaseUrl3 must be: " + baseUrl2, baseUrl2, gotBaseUrl3);

    String urlRequest4 = baseUrl2 + "/social/rest/v1/identity/123456.json#id?fields=fullName,avatarUrl&limit=20";
    setFakeCurrentEnvironmentContext(urlRequest4);
    String gotBaseUrl4 = Util.getBaseUrl();
    assertEquals("gotBaseUrl4 must be: " + baseUrl2, baseUrl2, gotBaseUrl4);

    String baseUrl3 = "http://social.demo.exoplatform.org:80";
    String urlRequest5 = baseUrl3 + "/social/rest/v1/identity/123456#id?fields=fullName,avatarUrl&limit=20";
    setFakeCurrentEnvironmentContext(urlRequest5);
    String gotBaseUrl5 = Util.getBaseUrl();
    assertEquals("gotBaseUrl5 must return: " + baseUrl3, baseUrl3, gotBaseUrl5);
  }

  private void setFakeCurrentEnvironmentContext(String urlRequest) {
    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new FakeHttpServletRequest(urlRequest);
    envctx.put(HttpServletRequest.class, httpRequest);
    EnvironmentContext.setCurrent(envctx);
  }


  /**
   * Fake HttpServletRequest
   */
  private class FakeHttpServletRequest implements HttpServletRequest {
    URI uriRequest;
    
    public FakeHttpServletRequest(String urlRequest) {
      try {
        uriRequest = new URI(urlRequest);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Failed to create FakeHttpServletRequest");
      }
    }
    
    public String getAuthType() {
      return null;  
    }

    public Cookie[] getCookies() {
      return new Cookie[0];  
    }

    public long getDateHeader(String name) {
      return 0;  
    }

    public String getHeader(String name) {
      return null;  
    }

    public Enumeration getHeaders(String name) {
      return null;  
    }

    public Enumeration getHeaderNames() {
      return null;  
    }

    public int getIntHeader(String name) {
      return 0;  
    }

    public String getMethod() {
      return null;  
    }

    public String getPathInfo() {
      return null;  
    }

    public String getPathTranslated() {
      return null;  
    }

    public String getContextPath() {
      return null;  
    }

    public String getQueryString() {
      return null;  
    }

    public String getRemoteUser() {
      return null;  
    }

    public boolean isUserInRole(String role) {
      return false;  
    }

    public Principal getUserPrincipal() {
      return null;  
    }

    public String getRequestedSessionId() {
      return null;  
    }

    public String getRequestURI() {
      return null;  
    }

    public StringBuffer getRequestURL() {
      return null;  
    }

    public String getServletPath() {
      return null;  
    }

    public HttpSession getSession(boolean create) {
      return null;  
    }

    public HttpSession getSession() {
      return null;  
    }

    public boolean isRequestedSessionIdValid() {
      return false;  
    }

    public boolean isRequestedSessionIdFromCookie() {
      return false;  
    }

    public boolean isRequestedSessionIdFromURL() {
      return false;  
    }

    public boolean isRequestedSessionIdFromUrl() {
      return false;  
    }

    public Object getAttribute(String name) {
      return null;  
    }

    public Enumeration getAttributeNames() {
      return null;  
    }

    public String getCharacterEncoding() {
      return null;  
    }

    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
      
    }

    public int getContentLength() {
      return 0;  
    }

    public String getContentType() {
      return null;  
    }

    public ServletInputStream getInputStream() throws IOException {
      return null;  
    }

    public String getParameter(String name) {
      return null;  
    }

    public Enumeration getParameterNames() {
      return null;  
    }

    public String[] getParameterValues(String name) {
      return new String[0];  
    }

    public Map getParameterMap() {
      return null;  
    }

    public String getProtocol() {
      return null;  
    }

    public String getScheme() {
      return uriRequest.getScheme();
    }

    public String getServerName() {
      return null;  
    }

    public int getServerPort() {
      return 0;  
    }

    public BufferedReader getReader() throws IOException {
      return null;  
    }

    public String getRemoteAddr() {
      return null;
    }

    public String getRemoteHost() {
      return uriRequest.getHost();
    }

    public void setAttribute(String name, Object o) {
      
    }

    public void removeAttribute(String name) {
      
    }

    public Locale getLocale() {
      return null;  
    }

    public Enumeration getLocales() {
      return null;  
    }

    public boolean isSecure() {
      return false;  
    }

    public RequestDispatcher getRequestDispatcher(String path) {
      return null;  
    }

    public String getRealPath(String path) {
      return null;  
    }

    public int getRemotePort() {
      return uriRequest.getPort();
    }

    public String getLocalName() {
      return null;  
    }

    public String getLocalAddr() {
      return null;  
    }

    public int getLocalPort() {
      return 0;  
    }
  }


}
