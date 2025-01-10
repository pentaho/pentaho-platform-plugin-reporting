/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.graphics.PageDrawable;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.graphics.PrintReportProcessor;
import org.pentaho.reporting.libraries.base.util.PngEncoder;
import org.pentaho.reporting.libraries.repository.ContentIOException;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.io.OutputStream;

public class PNGOutput implements ReportOutputHandler {
  private transient PrintReportProcessor proc;

  public PNGOutput() {
  }

  public Object getReportLock() {
    return this;
  }

  public boolean supportsPagination() {
    return false;
  }

  public void close() {
    if ( proc != null ) {
      proc.close();
    }
  }

  public int paginate( MasterReport report, int yieldRate ) throws ReportProcessingException, IOException,
    ContentIOException {
    return 0;
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
    OutputUtils.enforceQueryLimit( report );
    //TODO listener for async mode
    if ( proc == null ) {
      proc = create( report, yieldRate );
    }

    if ( proc.isPaginated() == false ) {
      proc.paginate();
    }
    final int pageCount = proc.getNumberOfPages();

    if ( pageCount <= acceptedPage ) {
      return -1;
    }

    final BufferedImage image = createImage( proc.getPageFormat( acceptedPage ) );

    final Rectangle rect = new Rectangle( 0, 0, image.getWidth(), image.getHeight() );
    // prepare the image by filling it ...
    final Graphics2D g2 = image.createGraphics();
    g2.setPaint( Color.white );
    g2.fill( rect );


    final PageDrawable pageDrawable = proc.getPageDrawable( acceptedPage );
    pageDrawable.draw( g2, rect );
    g2.dispose();

    // convert to PNG ...
    final PngEncoder encoder = new PngEncoder( image, true, 0, 9 );
    final byte[] data = encoder.pngEncode();

    outputStream.write( data );
    outputStream.flush();
    outputStream.close();
    return 0;
  }

  private PrintReportProcessor create( final MasterReport report, final int yieldRate )
    throws ReportProcessingException {
    final PrintReportProcessor proc = new PrintReportProcessor( report );
    if ( yieldRate > 0 ) {
      proc.addReportProgressListener( getYieldListener( yieldRate ) );
    }
    return proc;
  }

  /**
   * Create the empty image for the given page size.
   *
   * @return the generated image.
   */
  private static BufferedImage createImage( final PageFormat pf ) {
    // in this simple case we know, that all pages have the same size..
    final double width = pf.getWidth();
    final double height = pf.getHeight();
    // write the report to the temp file
    return new BufferedImage( (int) width, (int) height, BufferedImage.TYPE_BYTE_INDEXED );
  }
}
