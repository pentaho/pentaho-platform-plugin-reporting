package org.pentaho.reporting.platform.plugin;

import org.pentaho.platform.api.engine.IPluginLifecycleListener;
import org.pentaho.platform.api.engine.PluginLifecycleException;

public class LifecycleListener implements IPluginLifecycleListener
{

  public void init() throws PluginLifecycleException
  {
    // load reporting plugin
    ReportingSystemStartupListener startupListener = new ReportingSystemStartupListener();
    startupListener.startup(null);
  }

  public void loaded() throws PluginLifecycleException
  {
  }

  public void unLoaded() throws PluginLifecycleException
  {
  }

}
