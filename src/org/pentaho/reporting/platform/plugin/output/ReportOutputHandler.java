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

package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.repository.ContentIOException;

public interface ReportOutputHandler {
  /**
   * Returns the number of pages in the report.
   * 
   * @param report
   *          the report to handle.
   * @param yieldRate
   *          the yield rate.
   * @return the number of pages generated. This is ignored if {@link #supportsPagination()} returns false.
   * @throws ReportProcessingException
   *           if the report processing fails.
   * @throws IOException
   *           if there is an IO error.
   * @throws ContentIOException
   *           if there is an IO error.
   */
  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
      final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException;

  public int paginate( final MasterReport report, final int yieldRate ) throws ReportProcessingException, IOException,
    ContentIOException;

  public void close();

  /**
   * Does the output target support pagination?
   */
  public boolean supportsPagination();

  public Object getReportLock();
}
