package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IAcceptsRuntimeInputs;
import org.pentaho.platform.api.engine.IActionSequenceResource;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IStreamingPojo;
import org.pentaho.platform.engine.core.system.PentahoRequestContextHolder;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.metadata.ReportProcessTaskRegistry;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.PlainTextPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.xml.XmlPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.rtf.RTFTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xml.XmlTableModule;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationMessage;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.extensions.modules.java14print.Java14PrintUtil;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.base.util.CSVQuoter;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.xmlns.common.ParserUtil;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.reporting.platform.plugin.output.CSVOutput;
import org.pentaho.reporting.platform.plugin.output.EmailOutput;
import org.pentaho.reporting.platform.plugin.output.PDFOutput;
import org.pentaho.reporting.platform.plugin.output.PNGOutput;
import org.pentaho.reporting.platform.plugin.output.PageableContentRepoHtmlOutput;
import org.pentaho.reporting.platform.plugin.output.PageableHTMLOutput;
import org.pentaho.reporting.platform.plugin.output.PlainTextOutput;
import org.pentaho.reporting.platform.plugin.output.RTFOutput;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;
import org.pentaho.reporting.platform.plugin.output.StreamContentRepoHtmlOutput;
import org.pentaho.reporting.platform.plugin.output.StreamHtmlOutput;
import org.pentaho.reporting.platform.plugin.output.XLSOutput;
import org.pentaho.reporting.platform.plugin.output.XLSXOutput;
import org.pentaho.reporting.platform.plugin.output.XmlPageableOutput;
import org.pentaho.reporting.platform.plugin.output.XmlTableOutput;

public class SimpleReportingComponent implements IStreamingPojo, IAcceptsRuntimeInputs
{

  /**
   * The logging for logging messages from this component
   */
  private static final Log log = LogFactory.getLog(SimpleReportingComponent.class);

  public static final String OUTPUT_TARGET = "output-target"; //$NON-NLS-1$

  public static final String OUTPUT_TYPE = "output-type"; //$NON-NLS-1$
  public static final String MIME_TYPE_HTML = "text/html"; //$NON-NLS-1$
  public static final String MIME_TYPE_EMAIL = "mime-message/text/html"; //$NON-NLS-1$
  public static final String MIME_TYPE_PDF = "application/pdf"; //$NON-NLS-1$
  public static final String MIME_TYPE_XLS = "application/vnd.ms-excel"; //$NON-NLS-1$
  public static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; //$NON-NLS-1$
  public static final String MIME_TYPE_RTF = "application/rtf"; //$NON-NLS-1$
  public static final String MIME_TYPE_CSV = "text/csv"; //$NON-NLS-1$
  public static final String MIME_TYPE_TXT = "text/plain"; //$NON-NLS-1$
  public static final String MIME_TYPE_XML = "application/xml"; //$NON-NLS-1$
  public static final String MIME_TYPE_PNG = "image/png"; //$NON-NLS-1$

  public static final String XLS_WORKBOOK_PARAM = "workbook"; //$NON-NLS-1$

  public static final String REPORTLOAD_RESURL = "res-url"; //$NON-NLS-1$
  public static final String REPORT_DEFINITION_INPUT = "report-definition"; //$NON-NLS-1$
  public static final String USE_CONTENT_REPOSITORY = "useContentRepository"; //$NON-NLS-1$
  public static final String REPORTHTML_CONTENTHANDLER_PATTERN = "content-handler-pattern"; //$NON-NLS-1$
  public static final String REPORTGENERATE_YIELDRATE = "yield-rate"; //$NON-NLS-1$
  public static final String ACCEPTED_PAGE = "accepted-page"; //$NON-NLS-1$
  public static final String PAGINATE_OUTPUT = "paginate"; //$NON-NLS-1$
  public static final String PRINT = "print"; //$NON-NLS-1$
  public static final String PRINTER_NAME = "printer-name"; //$NON-NLS-1$
  public static final String DASHBOARD_MODE = "dashboard-mode"; //$NON-NLS-1$
  private static final String MIME_GENERIC_FALLBACK = "application/octet-stream"; //$NON-NLS-1$
  public static final String PNG_EXPORT_TYPE = "pageable/X-AWT-Graphics;image-type=png";
  private static final String CONTENT_LINKING_PARAMETER = "::cl";
  private static final String CONTENT_LINKING_WIDGET_ID = "::cl_id";

  /**
   * Static initializer block to guarantee that the ReportingComponent will be in a state where the reporting engine will be booted. We have a system listener
   * which will boot the reporting engine as well, but we do not want to solely rely on users having this setup correctly. The errors you receive if the engine
   * is not booted are not very helpful, especially to outsiders, so we are trying to provide multiple paths to success. Enjoy.
   */
  static
  {
    final ReportingSystemStartupListener startup = new ReportingSystemStartupListener();
    startup.startup(null);
  }

  /**
   * The output-type for the generated report, such as PDF, XLS, CSV, HTML, etc This must be the mime-type!
   */
  private String outputType;
  private String outputTarget;
  private String defaultOutputTarget;
  private MasterReport report;
  private Map<String, Object> inputs;
  private OutputStream outputStream;
  private InputStream reportDefinitionInputStream;
  private Boolean useContentRepository;
  private IActionSequenceResource reportDefinition;
  private String reportDefinitionPath;
  private boolean paginateOutput;
  private int acceptedPage;
  private int pageCount;
  private boolean dashboardMode;
  /*
   * These fields are for enabling printing
   */
  private boolean print = false;
  private String printer;

  /*
   * Default constructor
   */

  public SimpleReportingComponent()
  {
    this.inputs = Collections.emptyMap();
    acceptedPage = -1;
    pageCount = -1;
    useContentRepository = Boolean.FALSE;
    defaultOutputTarget = HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE;
  }

  // ----------------------------------------------------------------------------
  // BEGIN BEAN METHODS
  // ----------------------------------------------------------------------------


  public String getDefaultOutputTarget()
  {
    return defaultOutputTarget;
  }

  public void setDefaultOutputTarget(final String defaultOutputTarget)
  {
    if (defaultOutputTarget == null)
    {
      throw new NullPointerException();
    }
    this.defaultOutputTarget = defaultOutputTarget;
  }

  public String getOutputTarget()
  {
    return outputTarget;
  }

  public void setOutputTarget(final String outputTarget)
  {
    this.outputTarget = outputTarget;
  }

  /**
   * Sets the mime-type for determining which report output type to generate. This should be a mime-type for consistency with streaming output mime-types.
   *
   * @param outputType the desired output type (mime-type) for the report engine to generate
   */
  public void setOutputType(final String outputType)
  {
    this.outputType = outputType;
  }

  /**
   * Gets the output type, this should be a mime-type for consistency with streaming output mime-types.
   *
   * @return the current output type for the report
   */
  public String getOutputType()
  {
    return outputType;
  }

  /**
   * This method returns the resource for the report-definition, if available.
   *
   * @return the report-definition resource
   */
  public IActionSequenceResource getReportDefinition()
  {
    return reportDefinition;
  }

  /**
   * Sets the report-definition if it is provided to us by way of an action-sequence resource. The name must be reportDefinition or report-definition.
   *
   * @param reportDefinition a report-definition as seen (wrapped) by an action-sequence
   */
  public void setReportDefinition(final IActionSequenceResource reportDefinition)
  {
    this.reportDefinition = reportDefinition;
  }

  /**
   * This method will be called if an input is called reportDefinitionInputStream, or any variant of that with dashes report-definition-inputstream for example.
   * The primary purpose of this method is to facilitate unit testing.
   *
   * @param reportDefinitionInputStream any kind of InputStream which contains a valid report-definition
   */
  public void setReportDefinitionInputStream(final InputStream reportDefinitionInputStream)
  {
    this.reportDefinitionInputStream = reportDefinitionInputStream;
  }

  /**
   * Returns the path to the report definition (for platform use this is a path in the solution repository)
   *
   * @return reportdefinitionPath
   */
  public String getReportDefinitionPath()
  {
    return reportDefinitionPath;
  }

  /**
   * Sets the path to the report definition (platform path)
   *
   * @param reportDefinitionPath the path to the report definition.
   */
  public void setReportDefinitionPath(final String reportDefinitionPath)
  {
    this.reportDefinitionPath = reportDefinitionPath;
  }

  /**
   * Returns true if the report engine will be asked to use a paginated (HTML) output processor
   *
   * @return paginated
   */
  public boolean isPaginateOutput()
  {
    return paginateOutput;
  }

  /**
   * Set the paging mode used by the reporting engine. This will also be set if an input
   *
   * @param paginateOutput page mode
   */
  public void setPaginateOutput(final boolean paginateOutput)
  {
    this.paginateOutput = paginateOutput;
  }

  public int getAcceptedPage()
  {
    return acceptedPage;
  }

  public void setAcceptedPage(final int acceptedPage)
  {
    this.acceptedPage = acceptedPage;
  }

  /**
   * This method sets the IPentahoSession to use in order to access the pentaho platform file repository and content repository.
   *
   * @param session a valid pentaho session
   * @deprecated No longer used.
   */
  public void setSession(final IPentahoSession session)
  {
  }

  public boolean isDashboardMode()
  {
    return dashboardMode;
  }

  public void setDashboardMode(final boolean dashboardMode)
  {
    this.dashboardMode = dashboardMode;
  }

  /**
   * This method returns the mime-type for the streaming output based on the effective output target.
   *
   * @return the mime-type for the streaming output
   * @see SimpleReportingComponent#computeEffectiveOutputTarget()
   */
  public String getMimeType()
  {
    try
    {
      final String outputTarget = computeEffectiveOutputTarget();
      if (log.isDebugEnabled())
      {
        log.debug(Messages.getInstance().getString("ReportPlugin.logComputedOutputTarget", outputTarget));
      }
      if (HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_HTML;
      }
      if (HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_HTML;
      }
      if (ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_XLS;
      }
      if (ExcelTableModule.XLSX_FLOW_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_XLSX;
      }
      if (CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_CSV;
      }
      if (RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_RTF;
      }
      if (PdfPageableModule.PDF_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_PDF;
      }
      if (PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_TXT;
      }
      if (SimpleReportingComponent.MIME_TYPE_EMAIL.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_EMAIL;
      }
      if (XmlTableModule.TABLE_XML_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_XML;
      }
      if (XmlPageableModule.PAGEABLE_XML_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_XML;
      }
      if (PNG_EXPORT_TYPE.equals(outputTarget))
      {
        return SimpleReportingComponent.MIME_TYPE_PNG;
      }
    }
    catch (IOException e)
    {
      if (log.isDebugEnabled())
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.logErrorMimeTypeFull"), e);
      }
      else if (log.isWarnEnabled())
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.logErrorMimeTypeShort", e.getMessage()));
      }
    }
    catch (ResourceException e)
    {
      if (log.isDebugEnabled())
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.logErrorMimeTypeFull"), e);
      }
      else if (log.isWarnEnabled())
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.logErrorMimeTypeShort", e.getMessage()));
      }
    }
    return MIME_GENERIC_FALLBACK;
  }

  /**
   * This method sets the OutputStream to write streaming content on.
   *
   * @param outputStream an OutputStream to write to
   */
  public void setOutputStream(final OutputStream outputStream)
  {
    this.outputStream = outputStream;
  }

  public void setUseContentRepository(final Boolean useContentRepository)
  {
    this.useContentRepository = useContentRepository;
  }

  /**
   * This method checks if the output is targeting a printer
   *
   * @return true if the output is supposed to go to a printer
   */
  public boolean isPrint()
  {
    return print;
  }

  /**
   * Set whether or not to send the report to a printer
   *
   * @param print a flag indicating whether the report should be printed.
   */
  public void setPrint(final boolean print)
  {
    this.print = print;
  }

  /**
   * This method gets the name of the printer the report will be sent to
   *
   * @return the name of the printer that the report will be sent to
   */
  public String getPrinter()
  {
    return printer;
  }

  /**
   * Set the name of the printer to send the report to
   *
   * @param printer the name of the printer that the report will be sent to, a null value will be interpreted as the default printer
   */
  public void setPrinter(final String printer)
  {
    this.printer = printer;
  }


  /**
   * This method sets the map of *all* the inputs which are available to this component. This allows us to use action-sequence inputs as parameters for our
   * reports.
   *
   * @param inputs a Map containing inputs
   */
  public void setInputs(final Map<String, Object> inputs)
  {
    if (inputs == null)
    {
      this.inputs = Collections.emptyMap();
      return;
    }

    this.inputs = inputs;
    if (inputs.containsKey(OUTPUT_TYPE))
    {
      setOutputType(String.valueOf(inputs.get(OUTPUT_TYPE)));
    }
    if (inputs.containsKey(OUTPUT_TARGET))
    {
      setOutputTarget(String.valueOf(inputs.get(OUTPUT_TARGET)));
    }
    if (inputs.containsKey(REPORT_DEFINITION_INPUT))
    {
      setReportDefinitionInputStream((InputStream) inputs.get(REPORT_DEFINITION_INPUT));
    }
    if (inputs.containsKey(USE_CONTENT_REPOSITORY))
    {
      setUseContentRepository((Boolean) inputs.get(USE_CONTENT_REPOSITORY));
    }
    if (inputs.containsKey(PAGINATE_OUTPUT))
    {
      paginateOutput = "true".equals(String.valueOf(inputs.get(PAGINATE_OUTPUT))); //$NON-NLS-1$
    }
    if (inputs.containsKey(ACCEPTED_PAGE))
    {
      acceptedPage = ParserUtil.parseInt(String.valueOf(inputs.get(ACCEPTED_PAGE)), -1); //$NON-NLS-1$
    }
    if (inputs.containsKey(PRINT))
    {
      print = "true".equals(String.valueOf(inputs.get(PRINT))); //$NON-NLS-1$
    }
    if (inputs.containsKey(PRINTER_NAME))
    {
      printer = String.valueOf(inputs.get(PRINTER_NAME));
    }
    if (inputs.containsKey(DASHBOARD_MODE))
    {
      dashboardMode = "true".equals(String.valueOf(inputs.get(DASHBOARD_MODE))); //$NON-NLS-1$
    }
  }

  // ----------------------------------------------------------------------------
  // END BEAN METHODS
  // ----------------------------------------------------------------------------

  protected Object getInput(final String key, final Object defaultValue)
  {
    if (inputs != null)
    {
      final Object input = inputs.get(key);
      if (input != null)
      {
        return input;
      }
    }
    return defaultValue;
  }

  /**
   * Get the MasterReport for the report-definition, the MasterReport object will be cached as needed, using the PentahoResourceLoader.
   *
   * @return a parsed MasterReport object
   * @throws ResourceException
   * @throws IOException
   */
  public MasterReport getReport() throws ResourceException, IOException
  {
    if (report == null)
    {
      if (reportDefinitionInputStream != null)
      {
        report = ReportCreator.createReport(reportDefinitionInputStream, getDefinedResourceURL(null));
      }
      else if (reportDefinition != null)
      {
        // load the report definition as an action-sequence resource
        report = ReportCreator.createReport(reportDefinition.getAddress());
      }
      else if (reportDefinitionPath != null)
      {
        report = ReportCreator.createReport(reportDefinitionPath);
      }
      else
      {
        throw new ResourceException();
      }

      final String clText = extractContentLinkSpec();
      report.setReportEnvironment(new PentahoReportEnvironment(report.getConfiguration(), clText));
    }

    return report;
  }

  private String extractContentLinkSpec()
  {
    if (inputs == null)
    {
      return null;
    }

    final Object clRaw = inputs.get(CONTENT_LINKING_PARAMETER);
    if (clRaw == null)
    {
      return null;
    }

    if (clRaw instanceof Collection)
    {
      final Collection c = (Collection) clRaw;
      final CSVQuoter quoter = new CSVQuoter(',', '"');
      final StringBuilder b = new StringBuilder();
      for (final Object o : c)
      {
        final String s = quoter.doQuoting(String.valueOf(o));
        if (b.length() > 0)
        {
          b.append(',');
        }
        b.append(s);
      }
      return b.toString();
    }

    if (clRaw.getClass().isArray())
    {
      final CSVQuoter quoter = new CSVQuoter(',', '"');
      final StringBuilder b = new StringBuilder();
      for (int i = 0, size = Array.getLength(clRaw); i < size; i++)
      {
        final Object o = Array.get(clRaw, i);
        final String s = quoter.doQuoting(String.valueOf(o));
        if (b.length() > 0)
        {
          b.append(',');
        }
        b.append(s);
      }
      return b.toString();
    }
    else
    {
      return String.valueOf(clRaw);
    }
  }

  private boolean isValidOutputType(final String outputType)
  {
    if (PNG_EXPORT_TYPE.equals(outputType))
    {
      return true;
    }
    return ReportProcessTaskRegistry.getInstance().isExportTypeRegistered(outputType);
  }

  /**
   * Computes the effective output target that will be used when running the report. This method does not
   * modify any of the properties of this class.
   * <p/>
   * The algorithm to determine the output target is as follows:
   * <ul>
   * <li>
   * If the report attribute "lock-preferred-output-type" is set, and the attribute preferred-output-type is set,
   * the report will always be exported to the specified output type.</li>
   * <li>If the component has the parameter "output-target" set, this output target will be used.</li>
   * <li>If the component has the parameter "output-type" set, the mime-type will be translated into a suitable
   * output target (depends on other parameters like paginate as well.</li>
   * <li>If neither output-target or output-type are specified, the report's preferred output type will be used.</li>
   * <li>If no preferred output type is set, we default to HTML export.</li>
   * </ul>
   * <p/>
   * If the output type given is invalid, the report will not be executed and calls to
   * <code>SimpleReportingComponent#getMimeType()</code> will yield the generic "application/octet-stream" response.
   *
   * @return
   * @throws IOException
   * @throws ResourceException
   */
  private String computeEffectiveOutputTarget() throws IOException, ResourceException
  {
    final MasterReport report = getReport();
    if (Boolean.TRUE.equals(report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.LOCK_PREFERRED_OUTPUT_TYPE)))
    {
      // preferred output type is one of the engine's output-target identifiers. It is not a mime-type string.
      // The engine supports multiple subformats per mime-type (example HTML: zipped/streaming/flow/pageable)
      // The mime-mapping would be inaccurate.
      final Object preferredOutputType = report.getAttribute
          (AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE);
      if (preferredOutputType != null)
      {
        final String preferredOutputTypeString = String.valueOf(preferredOutputType);
        if (isValidOutputType(preferredOutputTypeString))
        {
          // if it is a recognized process-type, then fine, return it.
          return preferredOutputTypeString;
        }

        final String mappedLegacyType = mapOutputTypeToOutputTarget(preferredOutputTypeString);
        if (mappedLegacyType != null)
        {
          log.warn(Messages.getInstance().getString("ReportPlugin.warnLegacyLockedOutput", preferredOutputTypeString));
          return mappedLegacyType;
        }

        log.warn(Messages.getInstance().getString("ReportPlugin.warnInvalidLockedOutput", preferredOutputTypeString));
      }
    }

    final String outputTarget = getOutputTarget();
    if (outputTarget != null)
    {
      if (isValidOutputType(outputTarget) == false)
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.warnInvalidOutputTarget", outputTarget));
      }
      // if a engine-level output target is given, use it as it is. We can assume that the user knows how to
      // map from that to a real mime-type.
      return outputTarget;
    }

    final String mappingFromParams = mapOutputTypeToOutputTarget(getOutputType());
    if (mappingFromParams != null)
    {
      return mappingFromParams;
    }

    // if nothing is specified explicity, we may as well ask the report what it prefers..
    final Object preferredOutputType = report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE);
    if (preferredOutputType != null)
    {
      final String preferredOutputTypeString = String.valueOf(preferredOutputType);
      if (isValidOutputType(preferredOutputTypeString))
      {
        return preferredOutputTypeString;
      }

      final String mappedLegacyType = mapOutputTypeToOutputTarget(preferredOutputTypeString);
      if (mappedLegacyType != null)
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.warnLegacyPreferredOutput", preferredOutputTypeString));
        return mappedLegacyType;
      }

      log.warn(Messages.getInstance().getString("ReportPlugin.warnInvalidPreferredOutput",
          preferredOutputTypeString, getDefaultOutputTarget()));
      return getDefaultOutputTarget();
    }

    if (StringUtils.isEmpty(getOutputTarget()) == false || StringUtils.isEmpty(getOutputType()) == false)
    {
      // if you have come that far, it means you really messed up. Sorry, this error is not a error caused
      // by our legacy code - it is more likely that you just entered values that are totally wrong.
      log.error(Messages.getInstance().getString("ReportPlugin.warnInvalidOutputType", getOutputType(),
          getDefaultOutputTarget()));
    }
    return getDefaultOutputTarget();
  }

  public String getComputedOutputTarget() throws IOException, ResourceException
  {
    return computeEffectiveOutputTarget();
  }

  private String mapOutputTypeToOutputTarget(final String outputType)
  {
    // if the user has given a mime-type instead of a output-target, lets map it to the "best" choice. If the
    // user wanted full control, he would have used the output-target property instead.
    if (MIME_TYPE_CSV.equals(outputType))
    {
      return CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE;
    }
    if (MIME_TYPE_HTML.equals(outputType))
    {
      if (isPaginateOutput())
      {
        return HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE;
      }
      return HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE;
    }
    if (MIME_TYPE_XML.equals(outputType))
    {
      if (isPaginateOutput())
      {
        return XmlTableModule.TABLE_XML_EXPORT_TYPE;
      }
      return XmlPageableModule.PAGEABLE_XML_EXPORT_TYPE;
    }
    if (MIME_TYPE_PDF.equals(outputType))
    {
      return PdfPageableModule.PDF_EXPORT_TYPE;
    }
    if (MIME_TYPE_RTF.equals(outputType))
    {
      return RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE;
    }
    if (MIME_TYPE_XLS.equals(outputType))
    {
      return ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE;
    }
    if (MIME_TYPE_XLSX.equals(outputType))
    {
      return ExcelTableModule.XLSX_FLOW_EXPORT_TYPE;
    }
    if (MIME_TYPE_EMAIL.equals(outputType))
    {
      return MIME_TYPE_EMAIL;
    }
    if (MIME_TYPE_TXT.equals(outputType))
    {
      return PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE;
    }

    if ("pdf".equalsIgnoreCase(outputType)) //$NON-NLS-1$
    {
      log.warn(Messages.getInstance().getString("ReportPlugin.warnDeprecatedPDF"));
      return PdfPageableModule.PDF_EXPORT_TYPE;
    }
    else if ("html".equalsIgnoreCase(outputType)) //$NON-NLS-1$
    {
      log.warn(Messages.getInstance().getString("ReportPlugin.warnDeprecatedHTML"));
      if (isPaginateOutput())
      {
        return HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE;
      }
      return HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE;
    }
    else if ("csv".equalsIgnoreCase(outputType)) //$NON-NLS-1$
    {
      log.warn(Messages.getInstance().getString("ReportPlugin.warnDeprecatedCSV"));
      return CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE;
    }
    else if ("rtf".equalsIgnoreCase(outputType)) //$NON-NLS-1$
    {
      log.warn(Messages.getInstance().getString("ReportPlugin.warnDeprecatedRTF"));
      return RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE;
    }
    else if ("xls".equalsIgnoreCase(outputType)) //$NON-NLS-1$
    {
      log.warn(Messages.getInstance().getString("ReportPlugin.warnDeprecatedXLS"));
      return ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE;
    }
    else if ("txt".equalsIgnoreCase(outputType)) //$NON-NLS-1$
    {
      log.warn(Messages.getInstance().getString("ReportPlugin.warnDeprecatedTXT"));
      return PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE;
    }
    return null;
  }

  /**
   * Apply inputs (if any) to corresponding report parameters, care is taken when checking parameter types to perform any necessary casting and conversion.
   *
   * @param report  a MasterReport object to apply parameters to
   * @param context a ParameterContext for which the parameters will be under
   * @deprecated use the single parameter version instead. This method will now fail with an error if the
   *             report passed in is not the same as the report this component has. This method will be removed in
   *             version 4.0.
   */
  public void applyInputsToReportParameters(final MasterReport report, final ParameterContext context)
  {
    try
    {
      if (getReport() != report)
      {
        throw new IllegalStateException(Messages.getInstance().getString("ReportPlugin.errorForeignReportInput"));
      }
      final ValidationResult validationResult = applyInputsToReportParameters(context, null);
      if (validationResult.isEmpty() == false)
      {
        throw new IllegalStateException(Messages.getInstance().getString("ReportPlugin.errorApplyInputsFailed"));
      }
    }
    catch (IOException e)
    {
      throw new IllegalStateException(Messages.getInstance().getString("ReportPlugin.errorApplyInputsFailed"), e);
    }
    catch (ResourceException e)
    {
      throw new IllegalStateException(Messages.getInstance().getString("ReportPlugin.errorApplyInputsFailed"), e);
    }
  }

  /**
   * Apply inputs (if any) to corresponding report parameters, care is taken when
   * checking parameter types to perform any necessary casting and conversion.
   *
   * @param context          a ParameterContext for which the parameters will be under
   * @param validationResult the validation result that will hold the warnings. If null, a new one will be created.
   * @return the validation result containing any parameter validation errors.
   * @throws java.io.IOException if the report of this component could not be parsed.
   * @throws ResourceException   if the report of this component could not be parsed.
   */
  public ValidationResult applyInputsToReportParameters(final ParameterContext context,
                                                        ValidationResult validationResult)
      throws IOException, ResourceException
  {
    if (validationResult == null)
    {
      validationResult = new ValidationResult();
    }
    // apply inputs to report
    if (inputs != null)
    {
      final MasterReport report = getReport();
      final ParameterDefinitionEntry[] params = report.getParameterDefinition().getParameterDefinitions();
      final ReportParameterValues parameterValues = report.getParameterValues();
      for (final ParameterDefinitionEntry param : params)
      {
        final String paramName = param.getName();
        try
        {
          final Object computedParameter = ReportContentUtil.computeParameterValue(context, param, inputs.get(paramName));
          parameterValues.put(param.getName(), computedParameter);
          if (log.isInfoEnabled())
          {
            log.info(Messages.getInstance().getString("ReportPlugin.infoParameterValues",
                paramName, inputs.get(paramName), computedParameter));
          }
        }
        catch (Exception e)
        {
          if (log.isWarnEnabled())
          {
            log.warn(Messages.getInstance().getString("ReportPlugin.logErrorParametrization"), e);
          }
          validationResult.addError(paramName, new ValidationMessage(e.getMessage()));
        }
      }
    }
    return validationResult;
  }

  private URL getDefinedResourceURL(final URL defaultValue)
  {
    if (inputs == null || inputs.containsKey(REPORTLOAD_RESURL) == false)
    {
      return defaultValue;
    }

    try
    {
      final String inputStringValue = (String) getInput(REPORTLOAD_RESURL, null);
      return new URL(inputStringValue);
    }
    catch (Exception e)
    {
      return defaultValue;
    }
  }

  protected int getYieldRate()
  {
    final Object yieldRate = getInput(REPORTGENERATE_YIELDRATE, null);
    if (yieldRate instanceof Number)
    {
      final Number n = (Number) yieldRate;
      if (n.intValue() < 1)
      {
        return 0;
      }
      return n.intValue();
    }
    return 0;
  }

  /**
   * This method returns the number of logical pages which make up the report. This results of this method are available only after validate/execute have been
   * successfully called. This field has no setter, as it should never be set by users.
   *
   * @return the number of logical pages in the report
   */
  public int getPageCount()
  {
    return pageCount;
  }

  /**
   * This method will determine if the component instance 'is valid.' The validate() is called after all of the bean 'setters' have been called, so we may
   * validate on the actual values, not just the presence of inputs as we were historically accustomed to.
   * <p/>
   * Since we should have a list of all action-sequence inputs, we can determine if we have sufficient inputs to meet the parameter requirements of the
   * report-definition. This would include validation of values and ranges of values.
   *
   * @return true if valid
   * @throws Exception
   */
  public boolean validate() throws Exception
  {
    if (reportDefinition == null && reportDefinitionInputStream == null && reportDefinitionPath == null)
    {
      log.error(Messages.getInstance().getString("ReportPlugin.reportDefinitionNotProvided")); //$NON-NLS-1$
      return false;
    }
    if (outputStream == null && print == false)
    {
      log.error(Messages.getInstance().getString("ReportPlugin.outputStreamRequired")); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  /**
   * Perform the primary function of this component, this is, to execute. This method will be invoked immediately following a successful validate().
   *
   * @return true if successful execution
   * @throws Exception
   */
  public boolean execute() throws Exception
  {
    final MasterReport report = getReport();

    try
    {
      final DefaultParameterContext parameterContext = new DefaultParameterContext(report);
      // open parameter context
      final ValidationResult vr = applyInputsToReportParameters(parameterContext, null);
      if (vr.isEmpty() == false)
      {
        return false;
      }
      parameterContext.close();

      if (isPrint())
      {
        // handle printing
        // basic logic here is: get the default printer, attempt to resolve the user specified printer, default back as needed
        PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
        if (StringUtils.isEmpty(getPrinter()) == false)
        {
          final PrintService[] services = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
          for (final PrintService service : services)
          {
            if (service.getName().equals(printer))
            {
              printService = service;
            }
          }
          if ((printer == null) && (services.length > 0))
          {
            printService = services[0];
          }
        }
        Java14PrintUtil.printDirectly(report, printService);
        return true;
      }

      final String outputType = computeEffectiveOutputTarget();
      final ReportOutputHandler reportOutputHandler = createOutputHandlerForOutputType(outputType);
      if (reportOutputHandler == null)
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.warnUnprocessableRequest", outputType));
        return false;
      }

      final boolean result = reportOutputHandler.generate(report, acceptedPage, outputStream, getYieldRate());
      reportOutputHandler.close();
      return result;
    }
    catch (Throwable t)
    {
      log.error(Messages.getInstance().getString("ReportPlugin.executionFailed"), t); //$NON-NLS-1$
    }
    // lets not pretend we were successfull, if the export type was not a valid one.
    return false;
  }

  protected ReportOutputHandler createOutputHandlerForOutputType(final String outputType) throws IOException
  {
    final ReportOutputHandler reportOutputHandler;
    if (HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(outputType))
    {
      if (dashboardMode)
      {
        report.getReportConfiguration().setConfigProperty(HtmlTableModule.BODY_FRAGMENT, "true");
      }
      if (useContentRepository)
      {
        // use the content repository
        final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
        final String contentHandlerPattern = (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN,
            globalConfig.getConfigProperty("org.pentaho.web.resource.ContentHandler")); //$NON-NLS-1$
        reportOutputHandler = new PageableContentRepoHtmlOutput(contentHandlerPattern);
      }
      else
      {
        // don't use the content repository
        final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
        String contentHandlerPattern = PentahoRequestContextHolder.getRequestContext().getContextPath();
        contentHandlerPattern += (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN,
            globalConfig.getConfigProperty("org.pentaho.web.ContentHandler")); //$NON-NLS-1$
        reportOutputHandler = new PageableHTMLOutput(contentHandlerPattern);
      }
    }
    else if (HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(outputType))
    {
      if (dashboardMode)
      {
        report.getReportConfiguration().setConfigProperty(HtmlTableModule.BODY_FRAGMENT, "true");
      }
      if (useContentRepository)
      {
        // use the content repository
        final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
        final String contentHandlerPattern = (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN,
            globalConfig.getConfigProperty("org.pentaho.web.resource.ContentHandler")); //$NON-NLS-1$
        reportOutputHandler = new StreamContentRepoHtmlOutput(contentHandlerPattern);
      }
      else
      {
        final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
        String contentHandlerPattern = PentahoRequestContextHolder.getRequestContext().getContextPath();
        contentHandlerPattern += (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN,
            globalConfig.getConfigProperty("org.pentaho.web.ContentHandler")); //$NON-NLS-1$
        // don't use the content repository
        reportOutputHandler = new StreamHtmlOutput(contentHandlerPattern);
      }
    }
    else if (PNG_EXPORT_TYPE.equals(outputType))
    {
      reportOutputHandler = new PNGOutput();
    }
    else if (XmlPageableModule.PAGEABLE_XML_EXPORT_TYPE.equals(outputType))
    {
      reportOutputHandler = new XmlPageableOutput();
    }
    else if (XmlTableModule.TABLE_XML_EXPORT_TYPE.equals(outputType))
    {
      reportOutputHandler = new XmlTableOutput();
    }
    else if (PdfPageableModule.PDF_EXPORT_TYPE.equals(outputType))
    {
      reportOutputHandler = new PDFOutput();
    }
    else if (ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE.equals(outputType))
    {
      final InputStream templateInputStream = (InputStream) getInput(XLS_WORKBOOK_PARAM, null);
      reportOutputHandler = new XLSOutput(templateInputStream);
    }
    else if (ExcelTableModule.XLSX_FLOW_EXPORT_TYPE.equals(outputType))
    {
      final InputStream templateInputStream = (InputStream) getInput(XLS_WORKBOOK_PARAM, null);
      reportOutputHandler = new XLSXOutput(templateInputStream);
    }
    else if (CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE.equals(outputType))
    {
      reportOutputHandler = new CSVOutput();
    }
    else if (RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE.equals(outputType))
    {
      reportOutputHandler = new RTFOutput();
    }
    else if (MIME_TYPE_EMAIL.equals(outputType))
    {
      reportOutputHandler = new EmailOutput();
    }
    else if (PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE.equals(outputType))
    {
      reportOutputHandler = new PlainTextOutput();
    }
    else
    {
      reportOutputHandler = null;
    }
    return reportOutputHandler;
  }

  /**
   * Perform a pagination run.
   *
   * @return the number of pages or streams generated.
   * @throws IOException if an IO error occurred while loading the report.
   * @throws ResourceException if a resource loading error occurred.
   */
  public int paginate() throws IOException, ResourceException
  {
    final MasterReport report = getReport();

    try
    {
      final ParameterContext parameterContext = new DefaultParameterContext(report);
      // open parameter context
      final ValidationResult vr = applyInputsToReportParameters(parameterContext, null);
      if (vr.isEmpty() == false)
      {
        return 0;
      }

      parameterContext.close();

      if (isPrint())
      {
        return 0;
      }

      final String outputType = computeEffectiveOutputTarget();
      final ReportOutputHandler reportOutputHandler = createOutputHandlerForOutputType(outputType);
      if (reportOutputHandler == null)
      {
        log.warn(Messages.getInstance().getString("ReportPlugin.warnUnprocessableRequest", outputType));
        return 0;
      }
      return reportOutputHandler.paginate(report, getYieldRate());

    }
    catch (Throwable t)
    {
      log.error(Messages.getInstance().getString("ReportPlugin.executionFailed"), t); //$NON-NLS-1$
    }
    // lets not pretend we were successfull, if the export type was not a valid one.
    return 0;
  }
}
