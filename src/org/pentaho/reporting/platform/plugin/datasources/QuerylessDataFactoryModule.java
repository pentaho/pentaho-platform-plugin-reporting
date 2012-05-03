package org.pentaho.reporting.platform.plugin.datasources;

import org.pentaho.reporting.engine.classic.core.metadata.ElementMetaDataParser;
import org.pentaho.reporting.libraries.base.boot.AbstractModule;
import org.pentaho.reporting.libraries.base.boot.ModuleInitializeException;
import org.pentaho.reporting.libraries.base.boot.SubSystem;

public class QuerylessDataFactoryModule extends AbstractModule {

  public QuerylessDataFactoryModule() throws ModuleInitializeException {
    loadModuleInfo();
  }

  /**
   * Initializes the module. Use this method to perform all initial setup operations.
   * This method is called only once in a modules lifetime. If the initializing cannot
   * be completed, throw a ModuleInitializeException to indicate the error,. The module
   * will not be available to the system.
   *
   * @param subSystem the subSystem.
   * @throws ModuleInitializeException if an error occurred while initializing the module.
   */
  public void initialize(final SubSystem subSystem) throws ModuleInitializeException {
    ElementMetaDataParser
        .initializeOptionalDataFactoryMetaData("org/pentaho/reporting/platform/plugin/datasources/meta-datafactory.xml"); //$NON-NLS-1$  
  }
}
