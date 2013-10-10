/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.connection;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.*;
import java.util.Map.Entry;

import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginManager;

import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.util.TypedTableModel;
import org.pentaho.reporting.engine.classic.extensions.datasources.cda.CdaQueryBackend;
import org.pentaho.reporting.engine.classic.extensions.datasources.cda.CdaResponseParser;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Class that implements CDA to be used LOCAL inside pentaho platform
 * 
 * @author dduque
 */

public class CdaPluginLocalQueryBackend extends CdaQueryBackend {
  public CdaPluginLocalQueryBackend() {
  }

  public TypedTableModel fetchData( final DataRow dataRow, final String method,
                                    final Map<String, String> extraParameter ) throws ReportDataFactoryException {
    try {
      final Map<String, Object> parameters = new HashMap<String, Object>();

      final Set<Entry<String, String>> paramterSet = extraParameter.entrySet();
      for ( final Entry<String, String> entry : paramterSet ) {
        parameters.put( entry.getKey(), entry.getValue() );
      }

      parameters.put( "outputType", "xml" );
      parameters.put( "solution", encodeParameter( getSolution() ) );
      parameters.put( "path", encodeParameter( getPath() ) );
      parameters.put( "file", encodeParameter( getFile() ) );

      final String responseBody = callPlugin( "cda", method, new SimpleParameterProvider( parameters ) );

      // convert String into InputStream
      final InputStream responseBodyIs = new ByteArrayInputStream( responseBody.getBytes( "UTF-8" ) );

      return CdaResponseParser.performParse( responseBodyIs );
    } catch ( UnsupportedEncodingException use ) {
      throw new ReportDataFactoryException( "Failed to encode parameter", use );
    } catch ( Exception e ) {
      throw new ReportDataFactoryException( "Failed to send request", e );
    }
  }

  private static String callPlugin( final String pluginName, final String method, final IParameterProvider params )
    throws ReportDataFactoryException {

    final IPentahoSession userSession = PentahoSessionHolder.getSession();
    final IPluginManager pluginManager = PentahoSystem.get( IPluginManager.class, userSession );

    try {


      Object cdaBean = pluginManager.getBean("cda.api");
      Class cdaBeanClass = cdaBean.getClass();

      Class[] paramTypes;
      Object[] paramValues;
      Method m;
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      if ("listParameters".equals(method)) {
        paramTypes = new Class[]{String.class,String.class,String.class,String.class,String.class,
                HttpServletResponse.class, HttpServletRequest.class};
        m =  cdaBeanClass.getMethod("listParameters", paramTypes);
        paramValues = new Object[7];
        paramValues[0] = URLDecoder.decode(params.getStringParameter("path", null), "UTF-8");
        paramValues[1] = params.getStringParameter("solution", "");
        paramValues[2] = params.getStringParameter("file", "");
        paramValues[3] = params.getStringParameter("outputType", "json");
        paramValues[4] = params.getStringParameter("dataAccessId", "<blank>");
        paramValues[5] = getResponse(outputStream);
        paramValues[6] = getRequest();
      } else {
        paramTypes = new Class[]{String.class, String.class, int.class,
                String.class, Boolean.class, Boolean.class, int.class, int.class, Boolean.class,
                List.class, HttpServletResponse.class, HttpServletRequest.class};
        m = cdaBeanClass.getMethod("doQueryPost", paramTypes);

        //Get parameters
        paramValues = new Object[12];
        paramValues[0] = URLDecoder.decode(params.getStringParameter("path", null), "UTF-8");
        paramValues[1] = params.getStringParameter("outputType", "json");
        paramValues[2] = (int) params.getLongParameter("outputIndexId", 1);
        paramValues[3] = params.getStringParameter("dataAccessId", "1");
        paramValues[4] = Boolean.parseBoolean(params.getStringParameter("bypassCache", "false"));
        paramValues[5] = Boolean.parseBoolean(params.getStringParameter("paginateQuery", "false"));
        paramValues[6] = (int) params.getLongParameter("pageSize", 0);
        paramValues[7] = (int) params.getLongParameter("pageStart", 0);
        paramValues[8] = Boolean.parseBoolean(params.getStringParameter("wrapItUp", "false"));

        String[] sortFields = params.getStringArrayParameter("sortBy", new String[0]);
        List<String> sortList = new ArrayList<String>(sortFields.length);
        for (String sortField : sortFields) {
          sortList.add(sortField);
        }
        paramValues[9] = sortList;
        paramValues[10] = getResponse(outputStream);
        paramValues[11] = getRequest();
      }

      m.invoke(cdaBean, paramValues);
      return outputStream.toString();
    } catch ( Exception e ) {
      throw new ReportDataFactoryException( "Failed to acquire " + pluginName + " plugin: ", e );
    }
  }


  private static HttpServletRequest getRequest() {
    return new HttpServletRequest() {
      @Override
      public String getAuthType() {
        return null;
      }

      @Override
      public Cookie[] getCookies() {
        return new Cookie[0];
      }

      @Override
      public long getDateHeader(String s) {
        return 0;
      }

      @Override
      public String getHeader(String s) {
        return null;
      }

      @Override
      public Enumeration getHeaders(String s) {
        return null;
      }

      @Override
      public Enumeration getHeaderNames() {
        return null;
      }

      @Override
      public int getIntHeader(String s) {
        return 0;
      }

      @Override
      public String getMethod() {
        return null;
      }

      @Override
      public String getPathInfo() {
        return null;
      }

      @Override
      public String getPathTranslated() {
        return null;
      }

      @Override
      public String getContextPath() {
        return null;
      }

      @Override
      public String getQueryString() {
        return null;
      }

      @Override
      public String getRemoteUser() {
        return null;
      }

      @Override
      public boolean isUserInRole(String s) {
        return false;
      }

      @Override
      public Principal getUserPrincipal() {
        return null;
      }

      @Override
      public String getRequestedSessionId() {
        return null;
      }

      @Override
      public String getRequestURI() {
        return null;
      }

      @Override
      public StringBuffer getRequestURL() {
        return null;
      }

      @Override
      public String getServletPath() {
        return null;
      }

      @Override
      public HttpSession getSession(boolean b) {
        return null;
      }

      @Override
      public HttpSession getSession() {
        return null;
      }

      @Override
      public boolean isRequestedSessionIdValid() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdFromCookie() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdFromURL() {
        return false;
      }

      @Override
      public boolean isRequestedSessionIdFromUrl() {
        return false;
      }

      @Override
      public Object getAttribute(String s) {
        return null;
      }

      @Override
      public Enumeration getAttributeNames() {
        return null;
      }

      @Override
      public String getCharacterEncoding() {
        return null;
      }

      @Override
      public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
      }

      @Override
      public int getContentLength() {
        return 0;
      }

      @Override
      public String getContentType() {
        return null;
      }

      @Override
      public ServletInputStream getInputStream() throws IOException {
        return null;
      }

      @Override
      public String getParameter(String s) {
        return null;
      }

      @Override
      public Enumeration getParameterNames() {
        return new Hashtable(0).elements();
      }

      @Override
      public String[] getParameterValues(String s) {
        return new String[0];
      }

      @Override
      public Map getParameterMap() {
        return null;
      }

      @Override
      public String getProtocol() {
        return null;
      }

      @Override
      public String getScheme() {
        return null;
      }

      @Override
      public String getServerName() {
        return null;
      }

      @Override
      public int getServerPort() {
        return 0;
      }

      @Override
      public BufferedReader getReader() throws IOException {
        return null;
      }

      @Override
      public String getRemoteAddr() {
        return null;
      }

      @Override
      public String getRemoteHost() {
        return null;
      }

      @Override
      public void setAttribute(String s, Object o) {
      }

      @Override
      public void removeAttribute(String s) {
      }

      @Override
      public Locale getLocale() {
        return null;
      }

      @Override
      public Enumeration getLocales() {
        return null;
      }

      @Override
      public boolean isSecure() {
        return false;
      }

      @Override
      public RequestDispatcher getRequestDispatcher(String s) {
        return null;
      }

      @Override
      public String getRealPath(String s) {
        return null;
      }

      @Override
      public int getRemotePort() {
        return 0;
      }

      @Override
      public String getLocalName() {
        return null;
      }

      @Override
      public String getLocalAddr() {
        return null;
      }

      @Override
      public int getLocalPort() {
        return 0;
      }
    }        ;
  }


  private static HttpServletResponse getResponse (final OutputStream stream) {
    return new HttpServletResponse() {
      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        return new DelegatingServletOutputStream(stream);
      }

      //Needed to override but no implementation provided

      @Override
      public void addCookie(Cookie cookie) {
      }

      @Override
      public boolean containsHeader(String s) {
        return false;
      }

      @Override
      public String encodeURL(String s) {
        return null;
      }

      @Override
      public String encodeRedirectURL(String s) {
        return null;
      }

      @Override
      public String encodeUrl(String s) {
        return null;
      }

      @Override
      public String encodeRedirectUrl(String s) {
        return null;
      }

      @Override
      public void sendError(int i, String s) throws IOException {
      }

      @Override
      public void sendError(int i) throws IOException {
      }

      @Override
      public void sendRedirect(String s) throws IOException {
      }

      @Override
      public void setDateHeader(String s, long l) {
      }

      @Override
      public void addDateHeader(String s, long l) {
      }

      @Override
      public void setHeader(String s, String s2) {
      }

      @Override
      public void addHeader(String s, String s2) {
      }

      @Override
      public void setIntHeader(String s, int i) {
      }

      @Override
      public void addIntHeader(String s, int i) {
      }

      @Override
      public void setStatus(int i) {
      }

      @Override
      public void setStatus(int i, String s) {

      }

      @Override
      public String getCharacterEncoding() {
        return null;
      }

      @Override
      public String getContentType() {
        return null;
      }

      @Override
      public PrintWriter getWriter() throws IOException {
        return null;
      }

      @Override
      public void setCharacterEncoding(String s) {
      }

      @Override
      public void setContentLength(int i) {
      }

      @Override
      public void setContentType(String s) {
      }

      @Override
      public void setBufferSize(int i) {
      }

      @Override
      public int getBufferSize() {
        return 0;
      }

      @Override
      public void flushBuffer() throws IOException {
      }

      @Override
      public void resetBuffer() {
      }

      @Override
      public boolean isCommitted() {
        return false;
      }

      @Override
      public void reset() {
      }

      @Override
      public void setLocale(Locale locale) {
      }

      @Override
      public Locale getLocale() {
        return null;
      }
    } ;
  }



  private static class DelegatingServletOutputStream extends ServletOutputStream {
    private final OutputStream targetStream;
    /**
     * Create a new DelegatingServletOutputStream.
     * @param targetStream the target OutputStream
     */

    public DelegatingServletOutputStream(OutputStream targetStream) {
      this.targetStream = targetStream;
    }


    public void write(int b) throws IOException {
      this.targetStream.write(b);
    }

    public void flush() throws IOException {
      super.flush();
      this.targetStream.flush();
    }

    public void close() throws IOException {
      super.close();
      this.targetStream.close();
    }
  }


}
