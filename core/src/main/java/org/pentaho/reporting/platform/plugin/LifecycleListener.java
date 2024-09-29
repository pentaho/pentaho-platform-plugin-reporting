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


package org.pentaho.reporting.platform.plugin;

import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;

public class LifecycleListener implements IPluginLifecycleListener {

  public void init() throws PluginLifecycleException {
    // load reporting plugin
    final ReportingSystemStartupListener startupListener = new ReportingSystemStartupListener();
    startupListener.startup( null );
  }

  public void loaded() throws PluginLifecycleException {
  }

  public void unLoaded() throws PluginLifecycleException {
  }

}
