package org.pentaho.reporting.platform.plugin.output.util;

public class ExportReportUtilsFactory {
  private static final ThreadLocal<ExportReportUtils> threadLocalInstance = ThreadLocal.withInitial( ExportReportUtils::new );

  private ExportReportUtilsFactory() {
    // Prevent instantiation
  }

  public static ExportReportUtils getUtil() {
    return threadLocalInstance.get();
  }

  public static void clear() {
    threadLocalInstance.remove();
  }
}

