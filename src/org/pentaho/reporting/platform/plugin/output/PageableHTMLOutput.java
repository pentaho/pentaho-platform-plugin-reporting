package org.pentaho.reporting.platform.plugin.output;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.DisplayAllFlowSelector;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.SinglePageFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.PageableHtmlOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.file.FileRepository;
import org.pentaho.reporting.libraries.repository.stream.StreamRepository;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.PentahoURLRewriter;

public class PageableHTMLOutput implements ReportOutputHandler
{
  private String contentHandlerPattern;
  private ProxyOutputStream proxyOutputStream;
  private PageableReportProcessor proc;
  private AllItemsHtmlPrinter printer;

  public PageableHTMLOutput(final String contentHandlerPattern)
  {
    this.contentHandlerPattern = contentHandlerPattern;
  }

  public Object getReportLock()
  {
    return this;
  }

  public String getContentHandlerPattern()
  {
    return contentHandlerPattern;
  }

  public ProxyOutputStream getProxyOutputStream()
  {
    return proxyOutputStream;
  }

  public void setProxyOutputStream(final ProxyOutputStream proxyOutputStream)
  {
    this.proxyOutputStream = proxyOutputStream;
  }

  public HtmlPrinter getPrinter()
  {
    return printer;
  }

  protected PageableReportProcessor createReportProcessor(final MasterReport report, final int yieldRate)
      throws ReportProcessingException
  {

    proxyOutputStream = new ProxyOutputStream();

    printer = new AllItemsHtmlPrinter(report.getResourceManager());
    printer.setUrlRewriter(new PentahoURLRewriter(contentHandlerPattern, false));

    final PageableHtmlOutputProcessor outputProcessor = new PageableHtmlOutputProcessor(report.getConfiguration());
    outputProcessor.setPrinter(printer);
    proc = new PageableReportProcessor(report, outputProcessor);

    if (yieldRate > 0)
    {
      proc.addReportProgressListener(new YieldReportListener(yieldRate));
    }

    return proc;
  }

  protected void reinitOutputTarget() throws ReportProcessingException, ContentIOException
  {
    final IApplicationContext ctx = PentahoSystem.getApplicationContext();


    final ContentLocation dataLocation;
    final PentahoNameGenerator dataNameGenerator;
    if (ctx != null)
    {
      File dataDirectory = new File(ctx.getFileOutputPath("system/tmp/"));//$NON-NLS-1$
      if (dataDirectory.exists() && (dataDirectory.isDirectory() == false))
      {
        dataDirectory = dataDirectory.getParentFile();
        if (dataDirectory.isDirectory() == false)
        {
          throw new ReportProcessingException("Dead " + dataDirectory.getPath()); //$NON-NLS-1$
        }
      }
      else if (dataDirectory.exists() == false)
      {
        dataDirectory.mkdirs();
      }

      final FileRepository dataRepository = new FileRepository(dataDirectory);
      dataLocation = dataRepository.getRoot();
      dataNameGenerator = PentahoSystem.get(PentahoNameGenerator.class);
      if (dataNameGenerator == null)
      {
        throw new IllegalStateException
            (Messages.getInstance().getString("ReportPlugin.errorNameGeneratorMissingConfiguration"));
      }
      dataNameGenerator.initialize(dataLocation, true);
    }
    else
    {
      dataLocation = null;
      dataNameGenerator = null;
    }

    final StreamRepository targetRepository = new StreamRepository(null, proxyOutputStream, "report"); //$NON-NLS-1$
    final ContentLocation targetRoot = targetRepository.getRoot();

    printer.setContentWriter(targetRoot, new DefaultNameGenerator(targetRoot, "index", "html"));//$NON-NLS-1$//$NON-NLS-2$
    printer.setDataWriter(dataLocation, dataNameGenerator);
  }

  public int generate(final MasterReport report,
                          final int acceptedPage,
                          final OutputStream outputStream,
                          final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException
  {
    if (proc == null)
    {
      proc = createReportProcessor(report, yieldRate);
    }
    final PageableHtmlOutputProcessor outputProcessor = (PageableHtmlOutputProcessor) proc.getOutputProcessor();
    if (acceptedPage >= 0)
    {
      outputProcessor.setFlowSelector(new SinglePageFlowSelector(acceptedPage));
    }
    else
    {
      outputProcessor.setFlowSelector(new DisplayAllFlowSelector());
    }
    proxyOutputStream.setParent(outputStream);
    reinitOutputTarget();
    try
    {
      proc.processReport();
      return proc.getLogicalPageCount();
    }
    finally
    {
      outputStream.flush();
      printer.setContentWriter(null, null);
      printer.setDataWriter(null, null);
    }
  }

  public void close()
  {
    if (proc != null)
    {
      proc.close();
      proxyOutputStream = null;
    }

  }
}
