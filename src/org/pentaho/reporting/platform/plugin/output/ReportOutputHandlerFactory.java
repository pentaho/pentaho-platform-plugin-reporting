package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface ReportOutputHandlerFactory
{
  Set<Map.Entry<String, String>> getSupportedOutputTypes();
  ReportOutputHandler createOutputHandlerForOutputType( final ReportOutputHandlerSelector selector) throws IOException;
  String getMimeType (final ReportOutputHandlerSelector selector);
}
