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

public class MockOutputHandlerSelector implements ReportOutputHandlerSelector {
  private String outputType;

  public MockOutputHandlerSelector() {
  }

  public void setOutputType( final String outputType ) {
    this.outputType = outputType;
  }

  public String getOutputType() {
    return outputType;
  }

  public MasterReport getReport() {
    return null;
  }

  public boolean isUseJcrOutput() {
    return false;
  }

  public String getJcrOutputPath() {
    return null;
  }

  public <T> T getInput( String parameterName, T defaultValue, Class<T> idx ) {
    return null;
  }
}
