package org.pentaho.reporting.platform.plugin.output.util;

public class ReportUtilsFactory {
  private static final ReportUtils reportUtils = new ReportUtils();

  public static ReportUtils getReportUtils() {
    return reportUtils;
  }
}
