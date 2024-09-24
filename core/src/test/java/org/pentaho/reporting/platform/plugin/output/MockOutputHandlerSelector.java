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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

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
