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
    htmlStreamVisible = true;
    htmlPageVisible = true;
    xlsVisible = true;
    xlsxVisible = true;
    csvVisible = true;
    rtfVisible = true;
    pdfVisible = true;
    textVisible = true;
    mailVisible = true;
    xmlTableVisible = true;
    xmlPageVisible = true;
    pngVisible = true;

    htmlStreamAvailable = true;
    htmlPageAvailable = true;
    xlsAvailable = true;
    xlsxAvailable = true;
    csvAvailable = true;
    rtfAvailable = true;
    pdfAvailable = true;
    textAvailable = true;
    mailAvailable = true;
    xmlTableAvailable = true;
    xmlPageAvailable = true;
    pngAvailable = true;
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
    LinkedHashMap<String, String> outputTypes = new LinkedHashMap<String, String>();
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
    String t = selector.getOutputType();
    if (HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(t))
    {
      return createHtmlPageOutput(selector);
    }
    if (HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(t))
    {
      return createHtmlStreamOutput(selector);
    }
    if (SimpleReportingAction.PNG_EXPORT_TYPE.equals(t))
    {
      return createPngOutput();
    }
    if (XmlPageableModule.PAGEABLE_XML_EXPORT_TYPE.equals(t))
    {
      return createXmlPageableOutput();
    }
    if (XmlTableModule.TABLE_XML_EXPORT_TYPE.equals(t))
    {
      return createXmlTableOutput();
    }
    if (PdfPageableModule.PDF_EXPORT_TYPE.equals(t))
    {
      return createPdfOutput();
    }
    if (ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE.equals(t))
    {
      return createXlsOutput(selector);
    }
    if (ExcelTableModule.XLSX_FLOW_EXPORT_TYPE.equals(t))
    {
      return createXlsxOutput(selector);
    }
    if (CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE.equals(t))
    {
      return createCsvOutput();
    }
    if (RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE.equals(t))
    {
      return createRtfOutput();
    }
    if (SimpleReportingAction.MIME_TYPE_EMAIL.equals(t))
    {
      return createMailOutput();
    }
    if (PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE.equals(t))
    {
      return createTextOutput();
    }
    else
    {
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
      String contentHandlerPattern = computeContentHandlerPattern(selector);
      StreamJcrHtmlOutput streamJcrHtmlOutput = new StreamJcrHtmlOutput();
      streamJcrHtmlOutput.setContentHandlerPattern(contentHandlerPattern);
      streamJcrHtmlOutput.setJcrOutputPath(selector.getJcrOutputPath());
      return streamJcrHtmlOutput;
    }
    else
    {
      String contentHandlerPattern = computeContentHandlerPattern(selector);
      // don't use the content repository
      StreamHtmlOutput streamHtmlOutput = new StreamHtmlOutput();
      streamHtmlOutput.setContentHandlerPattern(contentHandlerPattern);
      return streamHtmlOutput;
    }
  }

  protected String computeContentHandlerPattern(final ReportOutputHandlerSelector selector)
  {
    String configKey;
    if (selector.isUseJcrOutput())
    {
      configKey = "org.pentaho.web.JcrContentHandler";
    }
    else
    {
      configKey = "org.pentaho.web.ContentHandler";
    }
    final Configuration globalConfig = ClassicEngineBoot.getInstance().getGlobalConfig();
    String contentHandlerPattern = PentahoRequestContextHolder.getRequestContext().getContextPath();
    contentHandlerPattern +=
        selector.getInput(SimpleReportingAction.REPORTHTML_CONTENTHANDLER_PATTERN,
            globalConfig.getConfigProperty(configKey), String.class); //$NON-NLS-1$
    return contentHandlerPattern;
  }

  protected ReportOutputHandler createHtmlPageOutput(final ReportOutputHandlerSelector selector)
  {
    if (isHtmlPageAvailable() == false)
    {
      return null;
    }
    // use the content repository
    String contentHandlerPattern = computeContentHandlerPattern(selector);

    PageableHTMLOutput pageableHTMLOutput = new PageableHTMLOutput();
    pageableHTMLOutput.setContentHandlerPattern(contentHandlerPattern);
    return pageableHTMLOutput;
  }
}
