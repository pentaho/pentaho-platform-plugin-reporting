package org.pentaho.reporting.platform.plugin.repository;

import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.NameGenerator;

public interface PentahoNameGenerator extends NameGenerator
{
  public void initialize(ContentLocation contentLocation, final boolean safeToDelete);
}
