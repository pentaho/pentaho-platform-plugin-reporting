/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/

package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface ReportOutputHandlerFactory {
  Set<Map.Entry<String, String>> getSupportedOutputTypes();

  ReportOutputHandler createOutputHandlerForOutputType( final ReportOutputHandlerSelector selector ) throws IOException;

  String getMimeType( final ReportOutputHandlerSelector selector );
}
