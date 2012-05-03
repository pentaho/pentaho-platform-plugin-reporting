package org.pentaho.reporting.platform.plugin.repository;

import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.NameGenerator;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.03.2010
 * Time: 16:33:37
 *
 * @author Thomas Morgner.
 */
public interface PentahoNameGenerator extends NameGenerator
{
  public void initialize(ContentLocation contentLocation, final boolean safeToDelete);
}
