package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.messages.Messages;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 16:02:06
 *
 * @author Thomas Morgner.
 */
public class ExecuteReportContentHandler
{
  private static final Log logger = LogFactory.getLog(ExecuteReportContentHandler.class);
  private IPentahoSession userSession;
  private ReportContentGenerator contentGenerator;
  private static final String FORCED_BUFFERED_WRITING = "org.pentaho.reporting.engine.classic.core.modules.output.table.html.ForceBufferedWriting";

  public ExecuteReportContentHandler(final ReportContentGenerator contentGenerator)
  {
    this.contentGenerator = contentGenerator;
    this.userSession = contentGenerator.getUserSession();
  }

  public void createReportContent(final OutputStream outputStream, final String reportDefinitionPath) throws Exception
  {
    final long start = System.currentTimeMillis();
    final Map<String, Object> inputs = contentGenerator.createInputs();
    AuditHelper.audit(userSession.getId(), userSession.getName(), reportDefinitionPath,
        contentGenerator.getObjectName(), getClass().getName(), MessageTypes.INSTANCE_START,
        contentGenerator.getInstanceId(), "", 0, contentGenerator); //$NON-NLS-1$

    String result = MessageTypes.INSTANCE_END;
    StagingHandler reportStagingHandler = null;
    try
    {
      // produce rendered report
      final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
      reportComponent.setSession(userSession);
      reportComponent.setReportDefinitionPath(reportDefinitionPath);
      reportComponent.setPaginateOutput(true);
      reportComponent.setDefaultOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
      reportComponent.setInputs(inputs);

      StagingMode stagingMode = null;
      final Object o = inputs.get("report-staging-mode");
      if (o != null)
      {
        try
        {
          stagingMode = StagingMode.valueOf(String.valueOf(o));
        }
        catch (IllegalArgumentException ie)
        {
          logger.trace("Staging mode was specified but invalid");
        }
      }
      final MasterReport report = reportComponent.getReport();
      if (stagingMode == null)
      {
        stagingMode = (StagingMode) report.getAttribute
            (AttributeNames.Pentaho.NAMESPACE, AttributeNames.Pentaho.STAGING_MODE);
      }
      reportStagingHandler = new StagingHandler(outputStream, stagingMode, this.userSession);

      if (reportStagingHandler.isFullyBuffered())
      {
        // it is safe to disable the buffered writing for the report now that we have a
        // extra buffering in place. 
        report.getReportConfiguration().setConfigProperty(FORCED_BUFFERED_WRITING, "false");
      }

      reportComponent.setOutputStream(reportStagingHandler.getStagingOutputStream());

      // the requested mime type can be null, in that case the report-component will resolve the desired
      // type from the output-target.
      // Hoever, the report-component will inspect the inputs independently from the mimetype here.

      final ISolutionRepository repository = PentahoSystem.get(ISolutionRepository.class, userSession);
      final ISolutionFile file = repository.getSolutionFile(reportDefinitionPath, ISolutionRepository.ACTION_EXECUTE);

      // add all inputs (request parameters) to report component
      final String mimeType = reportComponent.getMimeType();

      // If we haven't set an accepted page, -1 will be the default, which will give us a report
      // with no pages. This default is used so that when we do our parameter interaction with the
      // engine we can spend as little time as possible rendering unused pages, making it no pages.
      // We are going to intentionally reset the accepted page to the first page, 0, at this point,
      // if the accepted page is -1.
      final String outputTarget = reportComponent.getComputedOutputTarget();
      if (HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(outputTarget) && reportComponent.getAcceptedPage() < 0)
      {
        reportComponent.setAcceptedPage(0);
      }

      if (logger.isDebugEnabled())
      {
        logger.debug(Messages.getInstance().getString("ReportPlugin.logStartGenerateContent", mimeType,//$NON-NLS-1$
            outputTarget, String.valueOf(reportComponent.getAcceptedPage())));
      }

      HttpServletResponse response = null;
      boolean streamToBrowser = false;
      final IParameterProvider pathProviders = contentGenerator.getParameterProviders().get("path");
      if (pathProviders != null)
      {
        final Object httpResponse = pathProviders.getParameter("httpresponse");
        if (httpResponse instanceof HttpServletResponse)
        { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          response = (HttpServletResponse) httpResponse; //$NON-NLS-1$ //$NON-NLS-2$
          if (reportStagingHandler.getStagingMode() == StagingMode.THRU)
          {
            // Direct back - check output stream...
            final OutputStream respOutputStream = response.getOutputStream();
            if (respOutputStream == outputStream)
            {
              //
              // Massive assumption here -
              //  Assume the container returns the same object on successive calls to response.getOutputStream()
              streamToBrowser = true;
            }
          }
        }
      }

      final String extension = MimeHelper.getExtension(mimeType);
      String filename = file.getFileName();
      if (filename.lastIndexOf(".") != -1)
      { //$NON-NLS-1$
        filename = filename.substring(0, filename.lastIndexOf(".")); //$NON-NLS-1$
      }

      final boolean validates = reportComponent.validate();
      if (!validates)
      {
        sendErrorResponse(response, outputStream, reportStagingHandler);
      }
      else
      {
        if (streamToBrowser)
        {
          // Send headers before we begin execution
          response.setHeader("Content-Disposition", "inline; filename=\"" + filename + extension + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          response.setHeader("Content-Description", file.getFileName()); //$NON-NLS-1$
          response.setHeader("Cache-Control", "private, max-age=0, must-revalidate");
        }
        if (reportComponent.execute())
        {
          if (response != null)
          {
            if (reportStagingHandler.canSendHeaders())
            {
              response.setHeader("Content-Disposition", "inline; filename=\"" + filename + extension + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
              response.setHeader("Content-Description", file.getFileName()); //$NON-NLS-1$
              response.setHeader("Cache-Control", "private, max-age=0, must-revalidate");
              response.setHeader("Content-Size", String.valueOf(reportStagingHandler.getWrittenByteCount()));
            }
          }
          if (logger.isDebugEnabled())
          {
            logger.debug(Messages.getInstance().getString("ReportPlugin.logEndGenerateContent", String.valueOf(reportStagingHandler.getWrittenByteCount())));//$NON-NLS-1$
          }
          reportStagingHandler.complete(); // will copy bytes to final destination...

        }
        else
        { // failed execution
          sendErrorResponse(response, outputStream, reportStagingHandler);
        }
      }
    }
    catch (Exception ex)
    {
      result = MessageTypes.INSTANCE_FAILED;
      throw ex;
    }
    finally
    {
      if (reportStagingHandler != null)
      {
        reportStagingHandler.close();
      }
      final long end = System.currentTimeMillis();
      AuditHelper.audit(userSession.getId(), userSession.getName(), reportDefinitionPath,
          contentGenerator.getObjectName(), getClass().getName(), result, contentGenerator.getInstanceId(),
          "", ((float) (end - start) / 1000), contentGenerator); //$NON-NLS-1$
    }
  }

  private void sendErrorResponse(final HttpServletResponse response,
                                 final OutputStream outputStream,
                                 final StagingHandler reportStagingHandler)
      throws IOException
  {
    if (response != null)
    {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    if (logger.isDebugEnabled())
    {
      logger.debug(Messages.getInstance().getString("ReportPlugin.logErrorGenerateContent"));//$NON-NLS-1$
    }
    if (reportStagingHandler.canSendHeaders())
    {
      //
      // Can send headers is another way to check whether the real destination has been
      // pre-polluted with data.
      //
      outputStream.write(Messages.getInstance().getString("ReportPlugin.ReportValidationFailed").getBytes()); //$NON-NLS-1$
      outputStream.flush();
    }
  }

}
