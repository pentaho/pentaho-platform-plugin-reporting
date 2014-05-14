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

import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.html.FastHtmlContentItems;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.StreamHtmlOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.stream.StreamRepository;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.PentahoURLRewriter;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;

import java.io.IOException;
import java.io.OutputStream;

public class StreamJcrHtmlOutput extends AbstractHtmlOutput
{
  private String jcrOutputPath;

  public StreamJcrHtmlOutput()
  {
  }

  public StreamJcrHtmlOutput(final String contentHandlerPattern, final String jcrOutputPath)
  {
    super(contentHandlerPattern);
    this.jcrOutputPath = jcrOutputPath;
  }

  public String getJcrOutputPath()
  {
    return jcrOutputPath;
  }

  public void setJcrOutputPath(String jcrOutputPath)
  {
    this.jcrOutputPath = jcrOutputPath;
  }

  protected FastHtmlContentItems computeContentItems(final OutputStream outputStream) throws ReportProcessingException, ContentIOException
  {
    IUnifiedRepository repo = PentahoSystem.get(IUnifiedRepository.class);
    final RepositoryFile outputFolder = repo.getFile(jcrOutputPath);

    final ReportContentRepository repository = new ReportContentRepository(outputFolder);
    final ContentLocation dataLocation = repository.getRoot();

    final PentahoNameGenerator dataNameGenerator = createPentahoNameGenerator();
    dataNameGenerator.initialize(dataLocation, isSafeToDelete());

    final StreamRepository targetRepository = new StreamRepository(null, outputStream, "report"); //$NON-NLS-1$
    final ContentLocation targetRoot = targetRepository.getRoot();

    FastHtmlContentItems contentItems = new FastHtmlContentItems();
    contentItems.setContentWriter(targetRoot, new DefaultNameGenerator(targetRoot, "index", "html"));
    contentItems.setDataWriter(dataLocation, dataNameGenerator);
    contentItems.setUrlRewriter(new PentahoURLRewriter(getContentHandlerPattern(), true));
    return contentItems;
  }

  public int generate(final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                      final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {

    FastHtmlContentItems contentItems = computeContentItems(outputStream);
    final HtmlPrinter printer = new AllItemsHtmlPrinter(report.getResourceManager());
    printer.setContentWriter(contentItems.getContentLocation(), contentItems.getContentNameGenerator());
    printer.setDataWriter(contentItems.getDataLocation(), contentItems.getDataNameGenerator());
    printer.setUrlRewriter(contentItems.getUrlRewriter());

    final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor(report.getConfiguration());
    outputProcessor.setPrinter(printer);

    final StreamReportProcessor sp = new StreamReportProcessor(report, outputProcessor);
    try
    {
      sp.processReport();
    }
    finally
    {
      sp.close();
    }

    outputStream.flush();
    return 0;
  }
}
