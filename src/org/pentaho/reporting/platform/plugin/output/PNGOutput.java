package org.pentaho.reporting.platform.plugin.output;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.graphics.PageDrawable;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.graphics.PrintReportProcessor;
import org.pentaho.reporting.libraries.base.util.PngEncoder;
import org.pentaho.reporting.libraries.repository.ContentIOException;

public class PNGOutput
{
  public static int paginate(final MasterReport report,
                             final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException
  {
    PrintReportProcessor proc = null;
    try
    {
      proc = new PrintReportProcessor(report);
      if (yieldRate > 0)
      {
        proc.addReportProgressListener(new YieldReportListener(yieldRate));
      }
      proc.paginate();
      return proc.getNumberOfPages();
    }
    finally
    {
      if (proc != null)
      {
        proc.close();
      }
    }
  }

  public static boolean generate(final MasterReport report,
                             final int acceptedPage,
                             final OutputStream outputStream,
                             final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException
  {
    PrintReportProcessor proc = null;
    try
    {
      proc = new PrintReportProcessor(report);
      if (yieldRate > 0)
      {
        proc.addReportProgressListener(new YieldReportListener(yieldRate));
      }
      proc.paginate();
      final int pageCount = proc.getNumberOfPages();

      if (pageCount <= acceptedPage)
      {
        return false;
      }

      final BufferedImage image = createImage(proc.getPageFormat(acceptedPage));

      final Rectangle rect = new Rectangle(0, 0, image.getWidth(), image.getHeight());
      // prepare the image by filling it ...
      final Graphics2D g2 = image.createGraphics();
      g2.setPaint(Color.white);
      g2.fill(rect);

      final PageDrawable pageDrawable = proc.getPageDrawable(acceptedPage);
      pageDrawable.draw(g2, rect);
      g2.dispose();

      // convert to PNG ...
      final PngEncoder encoder = new PngEncoder(image, true, 0, 9);
      final byte[] data = encoder.pngEncode();

      outputStream.write(data);
      outputStream.flush();
      outputStream.close();
      return true;
    }
    finally
    {
      if (proc != null)
      {
        proc.close();
      }
    }
  }

  /**
   * Create the empty image for the given page size.
   *
   * @return the generated image.
   */
  private static BufferedImage createImage(final PageFormat pf)
  {
    // in this simple case we know, that all pages have the same size..
    final double width = pf.getWidth();
    final double height = pf.getHeight();
    //write the report to the temp file
    return new BufferedImage
        ((int) width, (int) height, BufferedImage.TYPE_BYTE_INDEXED);
  }
}