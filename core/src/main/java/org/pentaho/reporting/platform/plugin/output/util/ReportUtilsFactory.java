package org.pentaho.reporting.platform.plugin.output.util;

public class ReportUtilsFactory {
  private static final ThreadLocal<ReportUtils> threadLocalInstance = ThreadLocal.withInitial( ReportUtils::new );

  private ReportUtilsFactory() {
    // Prevent instantiation
  }

  public static ReportUtils getReportUtils() {
    return threadLocalInstance.get();
  }

  public static void clear() {
    threadLocalInstance.remove();
  }
}

