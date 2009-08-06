package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import javax.swing.table.TableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.platform.api.engine.IAcceptsRuntimeInputs;
import org.pentaho.platform.api.engine.IActionSequenceResource;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IStreamingPojo;
import org.pentaho.platform.api.repository.IContentRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.jfreereport.helper.PentahoTableModel;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.rtf.RTFTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelTableModule;
import org.pentaho.reporting.engine.classic.core.modules.parser.base.ReportGenerator;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.engine.classic.core.util.beans.ConverterRegistry;
import org.pentaho.reporting.engine.classic.core.util.beans.ValueConverter;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.output.CSVOutput;
import org.pentaho.reporting.platform.plugin.output.HTMLOutput;
import org.pentaho.reporting.platform.plugin.output.PDFOutput;
import org.pentaho.reporting.platform.plugin.output.PageableHTMLOutput;
import org.pentaho.reporting.platform.plugin.output.RTFOutput;
import org.pentaho.reporting.platform.plugin.output.XLSOutput;
import org.xml.sax.InputSource;

public class SimpleReportingComponent implements IStreamingPojo, IAcceptsRuntimeInputs
{

  /**
   * The logging for logging messages from this component
   */
  private static final Log log = LogFactory.getLog(SimpleReportingComponent.class);

  public static final String OUTPUT_TYPE = "output-type";
  public static final String MIME_TYPE_HTML = "text/html";
  public static final String MIME_TYPE_PDF = "application/pdf";
  public static final String MIME_TYPE_XLS = "application/vnd.ms-excel";
  public static final String MIME_TYPE_RTF = "application/rtf";
  public static final String MIME_TYPE_CSV = "text/csv";

  public static final String XLS_WORKBOOK_PARAM = "workbook";

  public static final String REPORTLOAD_RESURL = "res-url";
  public static final String REPORT_DEFINITION_INPUT = "report-definition";
  public static final String USE_CONTENT_REPOSITORY = "useContentRepository";
  public static final String REPORTHTML_CONTENTHANDLER_PATTERN = "content-handler-pattern"; //$NON-NLS-1$
  public static final String REPORTGENERATE_YIELDRATE = "yield-rate"; //$NON-NLS-1$
  public static final String ACCEPTED_PAGE = "accepted-page";
  public static final String PAGINATE_OUTPUT = "paginate";

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
  private MasterReport report;
  private Map<String, Object> inputs;
  private OutputStream outputStream;
  private InputStream reportDefinitionInputStream;
  private Boolean useContentRepository = Boolean.FALSE;
  private IActionSequenceResource reportDefinition;
  private String reportDefinitionPath;
  private IPentahoSession session;
  private boolean paginateOutput = false;
  private int acceptedPage = -1;
  private int pageCount = -1;

  /*
   * Default constructor
   */
  public SimpleReportingComponent()
  {
  }

  // ----------------------------------------------------------------------------
  // BEGIN BEAN METHODS
  // ----------------------------------------------------------------------------

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
  public void setOutputType(String outputType)
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
  public void setReportDefinition(IActionSequenceResource reportDefinition)
  {
    this.reportDefinition = reportDefinition;
  }

  /**
   * This method will be called if an input is called reportDefinitionInputStream, or any variant of that with dashes report-definition-inputstream for example.
   * The primary purpose of this method is to facilitate unit testing.
   *
   * @param reportDefinitionInputStream any kind of InputStream which contains a valid report-definition
   */
  public void setReportDefinitionInputStream(InputStream reportDefinitionInputStream)
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
   * @param reportDefinitionPath
   */
  public void setReportDefinitionPath(String reportDefinitionPath)
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
  public void setPaginateOutput(boolean paginateOutput)
  {
    this.paginateOutput = paginateOutput;
  }

  public int getAcceptedPage()
  {
    return acceptedPage;
  }

  public void setAcceptedPage(int acceptedPage)
  {
    this.acceptedPage = acceptedPage;
  }

  /**
   * This method sets the IPentahoSession to use in order to access the pentaho platform file repository and content repository.
   *
   * @param session a valid pentaho session
   */
  public void setSession(IPentahoSession session)
  {
    this.session = session;
  }

  /**
   * This method returns the output-type for the streaming output, it is the same as what is returned by getOutputType() for consistency.
   *
   * @return the mime-type for the streaming output
   */
  public String getMimeType()
  {
    return outputType;
  }

  /**
   * This method sets the OutputStream to write streaming content on.
   *
   * @param outputStream an OutputStream to write to
   */
  public void setOutputStream(OutputStream outputStream)
  {
    this.outputStream = outputStream;
  }

  public void setUseContentRepository(Boolean useContentRepository)
  {
    this.useContentRepository = useContentRepository;
  }

  /**
   * This method sets the map of *all* the inputs which are available to this component. This allows us to use action-sequence inputs as parameters for our
   * reports.
   *
   * @param inputs a Map containing inputs
   */
  public void setInputs(Map<String, Object> inputs)
  {
    this.inputs = inputs;
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
      paginateOutput = "true".equalsIgnoreCase("" + inputs.get(PAGINATE_OUTPUT));
      if (paginateOutput && inputs.containsKey(ACCEPTED_PAGE))
      {
        acceptedPage = Integer.parseInt("" + inputs.get(ACCEPTED_PAGE));
      }
    }
  }

  // ----------------------------------------------------------------------------
  // END BEAN METHODS
  // ----------------------------------------------------------------------------

  protected Object getInput(final String key, final Object defaultValue)
  {
    if (inputs != null)
    {
      Object input = inputs.get(key);
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
        ReportGenerator generator = ReportGenerator.createInstance();
        InputSource repDefInputSource = new InputSource(reportDefinitionInputStream);
        report = generator.parseReport(repDefInputSource, getDefinedResourceURL(null));
      }
      else if (reportDefinition != null)
      {
        // load the report definition as an action-sequence resource
        report = ReportCreator.createReport(reportDefinition.getAddress(), session);
      }
      else
      {
        report = ReportCreator.createReport(reportDefinitionPath, session);
      }
    }
    return report;
  }

  private String computeEffectiveOutputTarget(final MasterReport report)
  {

    if (outputTarget != null)
    {
      return outputTarget;
    }

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

    // if nothing is specified explicity, we may as well ask the report what it prefers..
    final Object preferredOutputType = report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE);
    if (preferredOutputType != null)
    {
      return String.valueOf(preferredOutputType);
    }
    // default to HTML stream ..
    return HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE;
  }

  /**
   * Apply inputs (if any) to corresponding report parameters, care is taken when checking parameter types to perform any necessary casting and conversion.
   *
   * @param report  a MasterReport object to apply parameters to
   * @param context a ParameterContext for which the parameters will be under
   */
  public void applyInputsToReportParameters(final MasterReport report, final ParameterContext context)
  {
    // apply inputs to report
    if (inputs != null)
    {
      ParameterDefinitionEntry[] params = report.getParameterDefinition().getParameterDefinitions();
      for (ParameterDefinitionEntry param : params)
      {
        String paramName = param.getName();
        Object value = inputs.get(paramName);
        Object defaultValue = param.getDefaultValue(context);
        if (value == null && defaultValue != null)
        {
          value = defaultValue;
        }
        if (value != null)
        {
          addParameter(report, param, paramName, value);
        }
      }
    }
  }

  private Object convert(final Class targetType, final Object rawValue) throws NumberFormatException
  {
    if (targetType == null)
    {
      throw new NullPointerException();
    }

    if (rawValue == null)
    {
      return null;
    }
    if (targetType.isInstance(rawValue))
    {
      return rawValue;
    }

    if (targetType.isAssignableFrom(TableModel.class) &&
        IPentahoResultSet.class.isAssignableFrom(rawValue.getClass()))
    {
      // wrap IPentahoResultSet to simulate TableModel
      return new PentahoTableModel((IPentahoResultSet) rawValue);
    }

    final String valueAsString = String.valueOf(rawValue);
    if (StringUtils.isEmpty(valueAsString))
    {
      return null;
    }

   if (targetType.equals(Date.class))
    {
      return new Date(new Long(valueAsString));
    }
    else
    {
      final ValueConverter valueConverter = ConverterRegistry.getInstance().getValueConverter(targetType);
      if (valueConverter != null)
      {
        try
        {
          return valueConverter.toPropertyValue(valueAsString);
        }
        catch (BeanException e)
        {
          throw new RuntimeException("Unable to convert parameter from String to real value.");
        }
      }
    }
    return rawValue;
  }

  private void addParameter(MasterReport report, ParameterDefinitionEntry param, String key, Object value)
  {
    if (value.getClass().isArray())
    {
      final Class componentType;
      if (param.getValueType().isArray())
      {
        componentType = param.getValueType().getComponentType();
      }
      else
      {
        componentType = param.getValueType();
      }

      final int length = Array.getLength(value);
      Object array = Array.newInstance(componentType, length);
      for (int i = 0; i < length; i++)
      {
        Array.set(array, i, convert(componentType, Array.get(value, i)));
      }
      report.getParameterValues().put(key, array);
    }
    else if (isAllowMultiSelect(param))
    {
      // if the parameter allows multi selections, wrap this single input in an array
      // and re-call addParameter with it
      Object[] array = new Object[1];
      array[0] = value;
      addParameter(report, param, key, array);
    }
    else
    {
      report.getParameterValues().put(key, convert(param.getValueType(), value));
    }
  }

  private boolean isAllowMultiSelect(ParameterDefinitionEntry parameter)
  {
    if (parameter instanceof ListParameter)
    {
      return ((ListParameter) parameter).isAllowMultiSelection();
    }
    return false;
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
    if (getInput(REPORTGENERATE_YIELDRATE, null) != null)
    {
      final Object inputValue = inputs.get(REPORTGENERATE_YIELDRATE);
      if (inputValue instanceof Number)
      {
        Number n = (Number) inputValue;
        if (n.intValue() < 1)
        {
          return 0;
        }
        return n.intValue();
      }
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
    if (StringUtils.isEmpty(outputType))
    {
      log.error("[input] The output type does not exist.");
      return false;
    }
    if (reportDefinition == null && reportDefinitionInputStream == null && reportDefinitionPath == null)
    {
      log.error("[input] A report-definition was not provided.");
      return false;
    }
    if (paginateOutput && acceptedPage < 0)
    {
      log.warn("[input] With pageable output, accepted-page should be provided and >= 0 (using default).");
    }
    if (reportDefinition != null && reportDefinitionPath != null && session == null)
    {
      log.error("[session] A valid session must be provided if the report-definition is given as a resource or by path.");
      return false;
    }
    if (outputStream == null)
    {
      log.error("[output] A valid OutputStream was not provided.");
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
    MasterReport report = getReport();

    try
    {
      ParameterContext parameterContext = new DefaultParameterContext(report);
      // open parameter context
      parameterContext.open();
      applyInputsToReportParameters(report, parameterContext);
      parameterContext.close();

      final String outputType = computeEffectiveOutputTarget(report);
      if (HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(outputType))
      {
        String contentHandlerPattern = (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN, ClassicEngineBoot.getInstance().getGlobalConfig()
            .getConfigProperty("org.pentaho.web.ContentHandler"));
        if (useContentRepository)
        {
          // use the content repository
          contentHandlerPattern = (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN, ClassicEngineBoot.getInstance().getGlobalConfig().getConfigProperty(
              "org.pentaho.web.resource.ContentHandler"));
          IContentRepository contentRepository = PentahoSystem.get(IContentRepository.class, session);
          pageCount = PageableHTMLOutput.generate(session, report, acceptedPage, outputStream, contentRepository, contentHandlerPattern, getYieldRate());
          return true;
        }
        else
        {
          // don't use the content repository
          pageCount = PageableHTMLOutput.generate(report, acceptedPage, outputStream, contentHandlerPattern, getYieldRate());
          return true;
        }
      }
      if (HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(outputType))
      {
        String contentHandlerPattern = (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN, ClassicEngineBoot.getInstance().getGlobalConfig()
            .getConfigProperty("org.pentaho.web.ContentHandler"));
        if (useContentRepository)
        {
          // use the content repository
          contentHandlerPattern = (String) getInput(REPORTHTML_CONTENTHANDLER_PATTERN, ClassicEngineBoot.getInstance().getGlobalConfig().getConfigProperty(
              "org.pentaho.web.resource.ContentHandler"));
          IContentRepository contentRepository = PentahoSystem.get(IContentRepository.class, session);
          return HTMLOutput.generate(session, report, outputStream, contentRepository, contentHandlerPattern, getYieldRate());
        }
        else
        {
          // don't use the content repository
          return HTMLOutput.generate(report, outputStream, contentHandlerPattern, getYieldRate());
        }
      }
      else if (PdfPageableModule.PDF_EXPORT_TYPE.equals(outputType))
      {
        return PDFOutput.generate(report, outputStream, getYieldRate());
      }
      else if (ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE.equals(outputType))
      {
        final InputStream templateInputStream = (InputStream) getInput(XLS_WORKBOOK_PARAM, null);
        return XLSOutput.generate(report, outputStream, templateInputStream, getYieldRate());
      }
      else if (CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE.equals(outputType))
      {
        return CSVOutput.generate(report, outputStream, getYieldRate());
      }
      else if (RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE.equals(outputType))
      {
        return RTFOutput.generate(report, outputStream, getYieldRate());
      }
    }
    catch (Throwable t)
    {
      log.error("[execute] Component execution failed.", t);
    }
    // lets not pretend we were successfull, if the export type was not a valid one.
    return false;
  }

}
