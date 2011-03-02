package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.Test;
import org.pentaho.platform.api.engine.*;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.core.output.SimpleOutputHandler;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.platform.plugin.services.pluginmgr.DefaultPluginManager;
import org.pentaho.platform.repository2.unified.fs.FileSystemBackedUnifiedRepository;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: rmansoor
 * Date: 2/24/11
 * Time: 9:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class ReportContentGeneratorTest extends TestCase {
  private static MicroPlatform microPlatform = new MicroPlatform();
  private static IUnifiedRepository repo;

  public ReportContentGeneratorTest() throws Exception {
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    microPlatform = new MicroPlatform("tests/integration-tests/resource/");
    microPlatform.define(IUnifiedRepository.class, FileSystemBackedUnifiedRepository.class, IPentahoDefinableObjectFactory.Scope.GLOBAL);
    microPlatform.define(IPluginResourceLoader.class, MockPluginResourceLoader.class, IPentahoDefinableObjectFactory.Scope.GLOBAL);
    microPlatform.define(IPluginManager.class, DefaultPluginManager.class, IPentahoDefinableObjectFactory.Scope.GLOBAL);
    FileSystemBackedUnifiedRepository unifiedRepository = (FileSystemBackedUnifiedRepository) PentahoSystem.get(IUnifiedRepository.class, null);
    unifiedRepository.setRootDir(new File("tests/integration-tests/resource/"));
  }


  @Test
  public void testRenderThroughContentGenerator() throws PlatformInitializationException {


    ReportContentGenerator cg = new ReportContentGenerator();

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    SimpleParameterProvider pathParams = new SimpleParameterProvider();
    Map<String,IParameterProvider> parameterProviders = new HashMap<String,IParameterProvider>();
    parameterProviders.put( "path" , pathParams ); //$NON-NLS-1$
    pathParams.setParameter( "path" , "solution/test/reporting/report.prpt");

    IOutputHandler handler = new SimpleOutputHandler(out, false);
    cg.setOutputHandler(handler);
    cg.setParameterProviders(parameterProviders);
    cg.setMessagesList(new ArrayList<String>());
    cg.setSession(PentahoSessionHolder.getSession());

    try {
      cg.createContent();
      assertTrue(true);
    } catch (Exception e) {
        assertFalse("Did no expect the exception",true);
    }

    String content = new String( out.toByteArray() );

    assertEquals( content.indexOf("reportviewer.nocache.js") >= 0, true);
  }
}
