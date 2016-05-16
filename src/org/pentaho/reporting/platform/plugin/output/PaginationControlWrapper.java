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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.reporting.platform.plugin.cache.IReportContent;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PaginationControlWrapper {

  private PaginationControlWrapper() {
  }

  private static Log logger = LogFactory.getLog( PaginationControlWrapper.class );
  private static final String TEMPLATE_PATH = "system/reporting/reportviewer/paginationTemplate.html";
  private static final String STAGING_PATH = "system/tmp/";
  private static final Pattern CSS = Pattern.compile( "(.*link.*\\/pentaho\\/getImage\\?image=)(.*)(\".*)" );


  private static final StringBuilder builder = new StringBuilder();

  private static String pagebleHtml;


  public static void write( final OutputStream stream, final IReportContent content ) throws IOException {

    if ( StringUtil.isEmpty( pagebleHtml ) ) {
      pagebleHtml = getSolutionDirFileContent( TEMPLATE_PATH );
    }

    //get rid of old data
    builder.setLength( 0 );

    final String pages = getPageArray( content );

    final StrSubstitutor substitutor = new StrSubstitutor( Collections.singletonMap( "pages", pages ) );
    final String filledTemplate = substitutor.replace( pagebleHtml );

    stream.write( filledTemplate.getBytes() );
    stream.flush();

  }

  private static String getPageArray( final IReportContent content ) throws IOException {
    builder.append( "var pages = [ " );
    int index = 0;
    byte[] page = content.getPageData( index );
    while ( page != null ) {
      String pageContent = new String( page, "UTF-8" );
      try {
        pageContent = embedCss( pageContent );
      } catch ( final IOException e ) {
        //Can't embed, let's not fail and at least make it usable inside the platform
        logger.error( "Can't embed styles and images into scheduled HTML file: ", e );
      }
      builder.append( '\'' ).append( Base64.encodeBase64String( pageContent.getBytes() ) ).append( '\'' )
        .append( ", \n" );
      index++;
      page = content.getPageData( index );
    }
    //don't need comma in the end
    builder.setLength( builder.length() - 3 );
    builder.append( " ];\n" );
    return builder.toString();
  }

  private static String embedCss( String pageContent ) throws IOException {
    Matcher cssLinkMatcher = CSS.matcher( pageContent );
    while ( cssLinkMatcher.find() && cssLinkMatcher.groupCount() > 1 ) {
      final String cssContent = getSolutionDirFileContent( STAGING_PATH + cssLinkMatcher.group( 2 ) );
      pageContent = cssLinkMatcher.replaceFirst( "<style>\n" + cssContent + "\n</style>" );
      cssLinkMatcher = CSS.matcher( pageContent );
    }
    return pageContent;
  }


  private static String getSolutionDirFileContent( final String path ) throws IOException {
    final IApplicationContext context = PentahoSystem.getApplicationContext();
    final String templateFile =
      context == null ? null : context.getSolutionPath( path );
    if ( !StringUtil.isEmpty( templateFile ) ) {
      try ( FileInputStream fis = new FileInputStream( templateFile ) ) {
        return IOUtils.toString( fis );
      }
    } else {
      throw new FileNotFoundException( "Can't find file in solution directory: " + TEMPLATE_PATH );
    }
  }
}
