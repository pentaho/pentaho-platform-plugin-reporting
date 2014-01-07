package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.pentaho.platform.engine.core.system.PentahoRequestContextHolder;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.PlainTextPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.xml.XmlPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.rtf.RTFTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xml.XmlTableModule;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.platform.plugin.SimpleReportingAction;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.messages.Messages;

public class DefaultReportOutputHandlerFactory implements ReportOutputHandlerFactory
{
  private boolean htmlStreamVisible;
  private boolean htmlPageVisible;
  private boolean xlsVisible;
  private boolean xlsxVisible;
  private boolean csvVisible;
  private boolean rtfVisible;
  private boolean pdfVisible;
  private boolean textVisible;
  private boolean mailVisible;
  private boolean xmlTableVisible;
  private boolean xmlPageVisible;
  private boolean pngVisible;

  private boolean htmlStreamAvailable;
  private boolean htmlPageAvailable;
  private boolean xlsAvailable;
  private boolean xlsxAvailable;
  private boolean csvAvailable;
  private boolean rtfAvailable;
  private boolean pdfAvailable;
  private boolean textAvailable;
  private boolean mailAvailable;
  private boolean xmlTableAvailable;
  private boolean xmlPageAvailable;
  private boolean pngAvailable;

  public DefaultReportOutputHandlerFactory()
  {
  }

  public boolean isHtmlStreamVisible()
  {
    return htmlStreamVisible;
  }

  public void setHtmlStreamVisible(final boolean htmlStreamVisible)
  {
    this.htmlStreamVisible = htmlStreamVisible;
  }

  public boolean isHtmlPageVisible()
  {
    return htmlPageVisible;
  }

  public void setHtmlPageVisible(final boolean htmlPageVisible)
  {
    this.htmlPageVisible = htmlPageVisible;
  }

  public boolean isXlsVisible()
  {
    return xlsVisible;
  }

  public void setXlsVisible(final boolean xlsVisible)
  {
    this.xlsVisible = xlsVisible;
  }

  public boolean isXlsxVisible()
  {
    return xlsxVisible;
  }

  public void setXlsxVisible(final boolean xlsxVisible)
  {
    this.xlsxVisible = xlsxVisible;
  }

  public boolean isCsvVisible()
  {
    return csvVisible;
  }

  public void setCsvVisible(final boolean csvVisible)
  {
    this.csvVisible = csvVisible;
  }

  public boolean isRtfVisible()
  {
    return rtfVisible;
  }

  public void setRtfVisible(final boolean rtfVisible)
  {
    this.rtfVisible = rtfVisible;
  }

  public boolean isPdfVisible()
  {
    return pdfVisible;
  }

  public void setPdfVisible(final boolean pdfVisible)
  {
    this.pdfVisible = pdfVisible;
  }

  public boolean isTextVisible()
  {
    return textVisible;
  }

  public void setTextVisible(final boolean textVisible)
  {
    this.textVisible = textVisible;
  }

  public boolean isMailVisible()
  {
    return mailVisible;
  }

  public void setMailVisible(final boolean mailVisible)
  {
    this.mailVisible = mailVisible;
  }

  public boolean isXmlTableVisible()
  {
    return xmlTableVisible;
  }

  public void setXmlTableVisible(final boolean xmlTableVisible)
  {
    this.xmlTableVisible = xmlTableVisible;
  }

  public boolean isXmlPageVisible()
  {
    return xmlPageVisible;
  }

  public void setXmlPageVisible(final boolean xmlPageVisible)
  {
    this.xmlPageVisible = xmlPageVisible;
  }

  public boolean isPngVisible()
  {
    return pngVisible;
  }

  public void setPngVisible(final boolean pngVisible)
  {
    this.pngVisible = pngVisible;
  }

  public boolean isHtmlStreamAvailable()
  {
    return htmlStreamAvailable;
  }

  public void setHtmlStreamAvailable(final boolean htmlStreamAvailable)
  {
    this.htmlStreamAvailable = htmlStreamAvailable;
  }

  public boolean isHtmlPageAvailable()
  {
    return htmlPageAvailable;
  }

  public void setHtmlPageAvailable(final boolean htmlPageAvailable)
  {
    this.htmlPageAvailable = htmlPageAvailable;
  }

  public boolean isXlsAvailable()
  {
    return xlsAvailable;
  }

  public void setXlsAvailable(final boolean xlsAvailable)
  {
    this.xlsAvailable = xlsAvailable;
  }

  public boolean isXlsxAvailable()
  {
    return xlsxAvailable;
  }

  public void setXlsxAvailable(final boolean xlsxAvailable)
  {
    this.xlsxAvailable = xlsxAvailable;
  }

  public boolean isCsvAvailable()
  {
    return csvAvailable;
  }

  public void setCsvAvailable(final boolean csvAvailable)
  {
    this.csvAvailable = csvAvailable;
  }

  public boolean isRtfAvailable()
  {
    return rtfAvailable;
  }

  public void setRtfAvailable(final boolean rtfAvailable)
  {
    this.rtfAvailable = rtfAvailable;
  }

  public boolean isPdfAvailable()
  {
    return pdfAvailable;
  }

  public void setPdfAvailable(final boolean pdfAvailable)
  {
    this.pdfAvailable = pdfAvailable;
  }

  public boolean isTextAvailable()
  {
    return textAvailable;
  }

  public void setTextAvailable(final boolean textAvailable)
  {
    this.textAvailable = textAvailable;
  }

  public boolean isMailAvailable()
  {
    return mailAvailable;
  }

  public void setMailAvailable(final boolean mailAvailable)
  {
    this.mailAvailable = mailAvailable;
  }

  public boolean isXmlTableAvailable()
  {
    return xmlTableAvailable;
  }

  public void setXmlTableAvailable(final boolean xmlTableAvailable)
  {
    this.xmlTableAvailable = xmlTableAvailable;
  }

  public boolean isXmlPageAvailable()
  {
    return xmlPageAvailable;
  }

  public void setXmlPageAvailable(final boolean xmlPageAvailable)
  {
    this.xmlPageAvailable = xmlPageAvailable;
  }

  public boolean isPngAvailable()
  {
    return pngAvailable;
  }

  public void setPngAvailable(final boolean pngAvailable)
  {
    this.pngAvailable = pngAvailable;
  }

  public String getMimeType(final ReportOutputHandlerSelector selector)
  {
    String outputTarget = selector.getOutputType();
    if (isHtmlPageAvailable() && HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_HTML;
    }
    if (isHtmlPageAvailable() && HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_HTML;
    }
    if (isXlsAvailable() && ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_XLS;
    }
    if (isXlsxAvailable() && ExcelTableModule.XLSX_FLOW_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_XLSX;
    }
    if (isCsvAvailable() && CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_CSV;
    }
    if (isRtfAvailable() && RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_RTF;
    }
    if (isPdfAvailable() && PdfPageableModule.PDF_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_PDF;
    }
    if (isTextAvailable() && PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_TXT;
    }
    if (isMailAvailable() && SimpleReportingComponent.MIME_TYPE_EMAIL.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_EMAIL;
    }
    if (isXmlTableAvailable() && XmlTableModule.TABLE_XML_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_XML;
    }
    if (isXmlPageAvailable() && XmlPageableModule.PAGEABLE_XML_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_XML;
    }
    if (isPngAvailable() && SimpleReportingAction.PNG_EXPORT_TYPE.equals(outputTarget))
    {
      return SimpleReportingComponent.MIME_TYPE_PNG;
    }
    return SimpleReportingAction.MIME_GENERIC_FALLBACK;
  }

  public Set<Map.Entry<String, String>> getSupportedOutputTypes()
  {
    Messages m = Messages.getInstance();
    LinkedHashMap<String, String> outputTypes = new LinkedHashMap<>();
    if (isHtmlPageVisible() && isHtmlPageAvailable())
    {
      outputTypes.put(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE, m.getString("ReportPlugin.outputHTMLPaginated"));
    }
    if (isHtmlStreamVisible() && isHtmlStreamAvailable())
    {
      outputTypes.put(HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE, m.getString("ReportPlugin.outputHTMLStream"));
    }
    if (isPdfVisible() && isPdfAvailable())
    {
      outputTypes.put(PdfPageableModule.PDF_EXPORT_TYPE, m.getString("ReportPlugin.outputPDF"));
    }
    if (isXlsVisible() && isXlsAvailable())
    {
      outputTypes.put(ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE, m.getString("ReportPlugin.outputXLS"));
    }
    if (isXlsxVisible() && isXlsxAvailable())
    {
      outputTypes.put(ExcelTableModule.XLSX_FLOW_EXPORT_TYPE, m.getString("ReportPlugin.outputXLSX"));
    }
    if (isCsvVisible() && isCsvAvailable())
    {
      outputTypes.put(CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE, m.getString("ReportPlugin.outputCSV"));
    }
    if (isRtfVisible() && isRtfAvailable())
    {
      outputTypes.put(RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE, m.getString("ReportPlugin.outputRTF"));
    }
    if (isTextVisible() && isTextAvailable())
    {
      outputTypes.put(PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE, m.getString("ReportPlugin.outputTXT"));
    }
    //noinspection unchecked
    return Collections.unmodifiableSet(outputTypes.entrySet());

  }

  public ReportOutputHandler createOutputHandlerForOutputType(final ReportOutputHandlerSelector selector) throws IOException
  {
    switch (selector.getOutputType())
    {
      case HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE:
        return createHtmlPageOutput(selector);
      case HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE:
        return createHtmlStreamOutput(selector);
      case SimpleReportingAction.PNG_EXPORT_TYPE:
        return createPngOutput();
      case XmlPageableModule.PAGEABLE_XML_EXPORT_TYPE:
        return createXmlPageableOutput();
      case XmlTableModule.TABLE_XML_EXPORT_TYPE:
        return createXmlTableOutput();
      case PdfPageableModule.PDF_EXPORT_TYPE:
        return createPdfOutput();
      case ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE:
        return createXlsOutput(selector);
      case ExcelTableModule.XLSX_FLOW_EXPORT_TYPE:
        return createXlsxOutput(selector);
      case CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE:
        return createCsvOutput();
      case RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE:
        return createRtfOutput();
      case SimpleReportingAction.MIME_TYPE_EMAIL:
        return createMailOutput();
      case PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE:
        return createTextOutput();
      default:
        return null;
    }
  }

  protected ReportOutputHandler createTextOutput()
  {
    if (isTextAvailable() == false)
    {
      return null;
    }
    return new PlainTextOutput();
  }

  protected ReportOutputHandler createMailOutput()
  {
    if (isMailAvailable() == false)
    {
      return null;
    }
    return new EmailOutput();
  }

  protected ReportOutputHandler createRtfOutput()
  {
    if (isRtfAvailable() == false)
    {
      return null;
    }
    return new RTFOutput();
  }

  protected ReportOutputHandler createCsvOutput()
  {
    if (isCsvAvailable() == false)
    {
      return null;
    }
    return new CSVOutput();
  }

  protected ReportOutputHandler createXlsxOutput(final ReportOutputHandlerSelector selector) throws IOException
  {
    if (isXlsxAvailable() == false)
    {
      return null;
    }
    XLSXOutput xlsxOutput = new XLSXOutput();
    xlsxOutput.setTemplateDataFromStream
        (selector.getInput(SimpleReportingAction.XLS_WORKBOOK_PARAM, null, InputStream.class));
    return xlsxOutput;
  }

  protected ReportOutputHandler createXlsOutput(final ReportOutputHandlerSelector selector) throws IOException
  {
    if (isXlsAvailable() == false)
    {
      return null;
    }
    XLSOutput xlsOutput = new XLSOutput();
    xlsOutput.setTemplateDataFromStream
        (selector.getInput(SimpleReportingAction.XLS_WORKBOOK_PARAM, null, InputStream.class));
    return xlsOutput;
  }

  protected ReportOutputHandler createPdfOutput()
  {
    if (isPdfAvailable() == false)
    {
      return null;
    }
    return new PDFOutput();
  }

  protected ReportOutputHandler createXmlTableOutput()
  {
    if (isXmlTableAvailable() == false)
    {
      return null;
    }
    return new XmlTableOutput();
  }

  protected ReportOutputHandler createXmlPageableOutput()
  {
    if (isXmlPageAvailable() == false)
    {
      return null;
    }
    return new XmlPageableOutput();
  }

  protected ReportOutputHandler createPngOutput()
  {
    if (isPngAvailable() == false)
    {
      return null;
    }
    return new PNGOutput();
  }

  protected ReportOutputHandler createHtmlStreamOutput(final ReportOutputHandlerSelector selector)
  {
    if (isHtmlStreamAvailable() == false)
    {
      return null;
    }
    if (selector.isUseJcrOutput())
    {
      // use the content repository
      final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
      String contentHandlerPattern = PentahoRequestContextHolder.getRequestContext().getContextPath();
      contentHandlerPattern +=
          selector.getInput(SimpleReportingAction.REPORTHTML_CONTENTHANDLER_PATTERN,
              globalConfig.getConfigProperty("org.pentaho.web.JcrContentHandler"), String.class); //$NON-NLS-1$
      StreamJcrHtmlOutput streamJcrHtmlOutput = new StreamJcrHtmlOutput();
      streamJcrHtmlOutput.setContentHandlerPattern(contentHandlerPattern);
      streamJcrHtmlOutput.setJcrOutputPath(selector.getJcrOutputPath());
      return streamJcrHtmlOutput;
    }
    else
    {
      final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
      String contentHandlerPattern = PentahoRequestContextHolder.getRequestContext().getContextPath();
      contentHandlerPattern +=
          selector.getInput(SimpleReportingAction.REPORTHTML_CONTENTHANDLER_PATTERN,
              globalConfig.getConfigProperty("org.pentaho.web.ContentHandler"), String.class); //$NON-NLS-1$
      // don't use the content repository
      StreamHtmlOutput streamHtmlOutput = new StreamHtmlOutput();
      streamHtmlOutput.setContentHandlerPattern(contentHandlerPattern);
      return streamHtmlOutput;
    }
  }

  protected ReportOutputHandler createHtmlPageOutput(final ReportOutputHandlerSelector selector)
  {
    if (isHtmlPageAvailable() == false)
    {
      return null;
    }
    // use the content repository
    final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
    String contentHandlerPattern = PentahoRequestContextHolder.getRequestContext().getContextPath();
    contentHandlerPattern +=
        selector.getInput(SimpleReportingAction.REPORTHTML_CONTENTHANDLER_PATTERN,
            globalConfig.getConfigProperty("org.pentaho.web.ContentHandler"), String.class); //$NON-NLS-1$

    PageableHTMLOutput pageableHTMLOutput = new PageableHTMLOutput();
    pageableHTMLOutput.setContentHandlerPattern(contentHandlerPattern);
    return pageableHTMLOutput;
  }
}
