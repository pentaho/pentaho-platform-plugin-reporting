package org.pentaho.reporting.platform.plugin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginProvider;
import org.pentaho.platform.api.engine.IServiceManager;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.api.engine.IPentahoDefinableObjectFactory.Scope;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.services.solution.SolutionEngine;
import org.pentaho.platform.plugin.services.pluginmgr.SystemPathXmlPluginProvider;
import org.pentaho.platform.plugin.services.pluginmgr.servicemgr.DefaultServiceManager;
import org.pentaho.platform.repository.solution.filebased.FileBasedSolutionRepository;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.TempDirectoryNameGenerator;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import junit.framework.TestCase;

/**
 * Unit tests for the ReportingComponent.
 * 
 * @author Michael D'Amour
 */
public class ReportingComponentTest extends TestCase {
  
  private MicroPlatform microPlatform;
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    microPlatform = new MicroPlatform("tests/integration-tests/resource/");
    microPlatform.define(ISolutionEngine.class, SolutionEngine.class);
    microPlatform.define(ISolutionRepository.class, FileBasedSolutionRepository.class);
    microPlatform.define(IPluginProvider.class, SystemPathXmlPluginProvider.class);
    microPlatform.define(IServiceManager.class, DefaultServiceManager.class, Scope.GLOBAL);
    microPlatform.define(PentahoNameGenerator.class, TempDirectoryNameGenerator.class, Scope.GLOBAL);
    IPentahoSession session = new StandaloneSession("test user");
    PentahoSessionHolder.setSession(session);
    microPlatform.start();
  }

  @Override
  protected void tearDown() throws Exception {
    microPlatform.stop();
  }


  public void testReportDefinitionAsInput() throws Exception
  {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream("tests/integration-tests/resource/solution/test/reporting/report.prpt"); //$NON-NLS-1$
    Map<String,Object> inputs = new HashMap<String, Object>();
    inputs.put(SimpleReportingComponent.REPORT_DEFINITION_INPUT, reportDefinition);
    rc.setInputs(inputs);
    rc.setOutputType("application/pdf"); //$NON-NLS-1$

    FileOutputStream outputStream = new FileOutputStream("/tmp/" + System.currentTimeMillis() + ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream(outputStream);

    // validate the component
    assertTrue(rc.validate());
    // execute the component
    assertTrue(rc.execute());
  }
  
  
  public void testPDF() throws Exception
  {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream("tests/integration-tests/resource/solution/test/reporting/report.prpt"); //$NON-NLS-1$
    rc.setReportDefinitionInputStream(reportDefinition);
    rc.setOutputType("application/pdf"); //$NON-NLS-1$

    FileOutputStream outputStream = new FileOutputStream("/tmp/" + System.currentTimeMillis() + ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream(outputStream);

    // validate the component
    assertTrue(rc.validate());
    // execute the component
    assertTrue(rc.execute());
  }

  public void testHTML() throws Exception
  {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream("tests/integration-tests/resource/solution/test/reporting/report.prpt"); //$NON-NLS-1$
    rc.setReportDefinitionInputStream(reportDefinition);
    rc.setOutputType("text/html"); //$NON-NLS-1$

    FileOutputStream outputStream = new FileOutputStream("/tmp/" + System.currentTimeMillis() + ".html"); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream(outputStream);

    // validate the component
    assertTrue(rc.validate());
    // execute the component
    assertTrue(rc.execute());
  }

}
