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
  /**
   * Returns the number of pages in the report. If the output target does not support pagination,
   * return zero.
   *
   * @param report the report to handle.
   * @param yieldRate the yield rate.
   * @return the number of pages generated or zero for a non-paginatable report.
   * @throws ReportProcessingException if the report processing fails.
   * @throws IOException if there is an IO error.
   * @throws ContentIOException if there is an IO error.
   */
  public int paginate(final MasterReport report,
                             final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException;

  public boolean generate(final MasterReport report,
                                 final int acceptedPage,
                                 final OutputStream outputStream,
                                 final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException;

  public void close();

  public Object getReportLock();
}
