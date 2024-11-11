/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/
package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.MasterReport;

public interface ReportOutputHandlerSelector {
  public String getOutputType();

  public MasterReport getReport();

  public boolean isUseJcrOutput();

  public String getJcrOutputPath();

  public <T> T getInput( String parameterName, T defaultValue, Class<T> idx );
}
