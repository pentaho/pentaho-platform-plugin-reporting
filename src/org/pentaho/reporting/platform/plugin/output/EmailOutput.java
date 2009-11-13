package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.StreamHtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.URLRewriter;
import org.pentaho.reporting.engine.classic.extensions.modules.mailer.MailURLRewriter;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.NameGenerator;
import org.pentaho.reporting.libraries.repository.email.EmailRepository;
import org.pentaho.reporting.platform.plugin.repository.PentahoURLRewriter;

public class EmailOutput
{
  private EmailOutput()
  {
  }

  public static boolean generate(final MasterReport report,
                                 final OutputStream outputStream,
                                 final String contentHandlerPattern,
                                 final int yieldRate)
      throws ReportProcessingException, IOException, ContentIOException, MessagingException
  {
    final IApplicationContext ctx = PentahoSystem.getApplicationContext();
    if (ctx == null)
    {
      return false;
    }

    final Properties props = new Properties();
    final Session session = Session.getInstance(props);
    final EmailRepository dataRepository = new EmailRepository(session);

    final URLRewriter rewriter = new MailURLRewriter();
    final ContentLocation dataLocation = dataRepository.getRoot();
    final NameGenerator dataNameGenerator = new DefaultNameGenerator(dataLocation);

    final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor(report.getConfiguration());
    final HtmlPrinter printer = new AllItemsHtmlPrinter(report.getResourceManager());
    printer.setContentWriter(dataLocation, new DefaultNameGenerator(dataLocation, "index", "html"));//$NON-NLS-1$//$NON-NLS-2$
    printer.setDataWriter(dataLocation, dataNameGenerator);
    printer.setUrlRewriter(rewriter);
    outputProcessor.setPrinter(printer);

    final StreamReportProcessor sp = new StreamReportProcessor(report, outputProcessor);
    if (yieldRate > 0)
    {
      sp.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    sp.processReport();
    dataRepository.writeEmail(outputStream);
    sp.close();

    outputStream.flush();
    outputStream.close();
    return true;
  }

}
