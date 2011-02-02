package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.repository.ContentIOException;

/**
 * Todo: Document me!
 * <p/>
 * Date: 02.02.11
 * Time: 17:06
 *
 * @author Thomas Morgner.
 */
public interface ReportOutputHandler
{
  public int paginate(final MasterReport report,
                             final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException;

  public boolean generate(final MasterReport report,
                                 final int acceptedPage,
                                 final OutputStream outputStream,
                                 final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException;

  public void close();
}
