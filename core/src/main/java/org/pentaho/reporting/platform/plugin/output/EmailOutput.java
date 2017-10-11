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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.StreamHtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.extensions.modules.mailer.MailURLRewriter;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.email.EmailRepository;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import javax.mail.MessagingException;
import javax.mail.Session;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class EmailOutput implements ReportOutputHandler {

  private static final String ROTATED_TEXT_AS_IMAGES =
    "org.pentaho.reporting.engine.classic.core.modules.output.table.html.RotatedTextAsImages";

  public EmailOutput() {
  }

  public Object getReportLock() {
    return this;
  }

  public int paginate( final MasterReport report, final int yieldRate ) throws ReportProcessingException, IOException,
    ContentIOException {
    return 0;
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
    final IApplicationContext ctx = PentahoSystem.getApplicationContext();
    if ( ctx == null ) {
      return -1;
    }


    try {

      final Configuration configuration = report.getConfiguration();

      if ( configuration instanceof ModifiableConfiguration ) {
        final ModifiableConfiguration modifiableConfiguration = (ModifiableConfiguration) configuration;
        modifiableConfiguration.setConfigProperty( HtmlTableModule.INLINE_STYLE, "true" );
        modifiableConfiguration.setConfigProperty( HtmlTableModule.EXTERNALIZE_STYLE, "false" );
        modifiableConfiguration.setConfigProperty( ROTATED_TEXT_AS_IMAGES, "true" );
      }

      final Properties props = new Properties();
      final Session session = Session.getInstance( props );
      final EmailRepository dataRepository = new EmailRepository( session );
      final ContentLocation dataLocation = dataRepository.getRoot();

      final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor( configuration );
      final HtmlPrinter printer = new AllItemsHtmlPrinter( report.getResourceManager() );
      printer.setContentWriter( dataLocation,
        new DefaultNameGenerator( dataLocation, "index", "html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
      printer.setDataWriter( dataLocation, new DefaultNameGenerator( dataLocation ) );
      printer.setUrlRewriter( new MailURLRewriter() );
      outputProcessor.setPrinter( printer );

      final StreamReportProcessor sp = new StreamReportProcessor( report, outputProcessor );
      if ( yieldRate > 0 ) {
        sp.addReportProgressListener( getYieldListener( yieldRate ) );
      }

      final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();

      doProcess( listener, sp );
      dataRepository.writeEmail( outputStream );
      outputStream.flush();
      return 0;
    } catch ( final MessagingException e ) {
      throw new ReportProcessingException( "Error", e );
    }

  }

  public boolean supportsPagination() {
    return false;
  }

  public void close() {

  }
}
