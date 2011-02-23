package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.*;

import org.pentaho.platform.api.engine.*;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.core.output.SimpleOutputHandler;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.platform.engine.services.solution.SolutionUrlContentGenerator;
import org.pentaho.platform.plugin.outputs.FileOutputHandler;
import org.pentaho.platform.plugin.services.pluginmgr.DefaultPluginManager;
import org.pentaho.platform.plugin.services.pluginmgr.PluginResourceLoader;
import org.pentaho.platform.repository2.unified.fs.FileSystemBackedUnifiedRepository;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.*;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by IntelliJ IDEA.
 * User: rmansoor
 * Date: 2/22/11
 * Time: 3:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportViewDelegateContentGeneratorTest extends TestCase {

  private static MicroPlatform microPlatform = new MicroPlatform();
  private static IUnifiedRepository repo;

  public ReportViewDelegateContentGeneratorTest() throws Exception {
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


    ReportViewerDelegateContentGenerator cg = new ReportViewerDelegateContentGenerator();

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    SimpleParameterProvider pathParams = new SimpleParameterProvider();
    Map<String,IParameterProvider> parameterProviders = new HashMap<String,IParameterProvider>();
    parameterProviders.put( "path" , pathParams ); //$NON-NLS-1$
    pathParams.setParameter( "path" , "solution/test/reporting/report.prpt");

    IOutputHandler handler = new SimpleOutputHandler(out, false);
    cg.setOutputHandler(handler);
    cg.setParameterProviders(parameterProviders);
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
