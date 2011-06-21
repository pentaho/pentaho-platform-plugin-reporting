package org.pentaho.reporting.platform.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IRuntimeContext;
import org.pentaho.platform.engine.core.output.SimpleOutputHandler;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.plugin.services.messages.Messages;
import org.pentaho.test.platform.engine.core.BaseTest;

import java.io.OutputStream;
import java.util.Map;

/**
 * Integration tests for the ReportingComponent.
 * 
 * @author David Kincade
 */
public class ReportingComponentIntegrationTest extends BaseTest {

  // Logger
  private static final Log log = LogFactory.getLog(ReportingComponentIntegrationTest.class);

  private static final String SOLUTION_PATH = "tests/integration-tests/resource/solution"; //$NON-NLS-1$

  public String getSolutionPath()
  {
    return SOLUTION_PATH;
  }

  public Map getRequiredListeners()
  {
    Map listeners = super.getRequiredListeners();
    listeners.put("jfree-report", "jfree-report"); //$NON-NLS-1$ //$NON-NLS-2$
    return listeners;
  }

  public void test1_pdf()
  {
    startTest();
    SimpleParameterProvider parameterProvider = new SimpleParameterProvider();
    parameterProvider.setParameter("outputType", "application/pdf"); //$NON-NLS-1$ //$NON-NLS-2$
    OutputStream outputStream = getOutputStream("ReportingTest.test1", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
    SimpleOutputHandler outputHandler = new SimpleOutputHandler(outputStream, true);
    StandaloneSession session = new StandaloneSession(Messages.getString("BaseTest.DEBUG_JUNIT_SESSION")); //$NON-NLS-1$
    IRuntimeContext context = run("test", "reporting", "test1.xaction", null, false, parameterProvider, outputHandler, session); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    assertEquals(Messages.getString("BaseTest.USER_RUNNING_ACTION_SEQUENCE"), IRuntimeContext.RUNTIME_STATUS_SUCCESS, context.getStatus()); //$NON-NLS-1$
    finishTest();
  }
  
  public void test1_html()
  {
    startTest();
    SimpleParameterProvider parameterProvider = new SimpleParameterProvider();
    parameterProvider.setParameter("outputType", "text/html"); //$NON-NLS-1$ //$NON-NLS-2$
    OutputStream outputStream = getOutputStream("ReportingTest.test1", ".html"); //$NON-NLS-1$ //$NON-NLS-2$
    SimpleOutputHandler outputHandler = new SimpleOutputHandler(outputStream, true);
    StandaloneSession session = new StandaloneSession(Messages.getString("BaseTest.DEBUG_JUNIT_SESSION")); //$NON-NLS-1$
    IRuntimeContext context = run("test", "reporting", "test1.xaction", null, false, parameterProvider, outputHandler, session); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    assertEquals(Messages.getString("BaseTest.USER_RUNNING_ACTION_SEQUENCE"), IRuntimeContext.RUNTIME_STATUS_SUCCESS, context.getStatus()); //$NON-NLS-1$
    finishTest();
  }

  public void testBiServer4250_pdf()
  {
    startTest();
    SimpleParameterProvider parameterProvider = new SimpleParameterProvider();
    parameterProvider.setParameter("outputType", "application/pdf"); //$NON-NLS-1$ //$NON-NLS-2$
    OutputStream outputStream = getOutputStream("ReportingTest.testbiserver4250", ".pdf"); //$NON-NLS-1$ //$NON-NLS-2$
    SimpleOutputHandler outputHandler = new SimpleOutputHandler(outputStream, true);
    StandaloneSession session = new StandaloneSession(Messages.getString("BaseTest.DEBUG_JUNIT_SESSION")); //$NON-NLS-1$
    IRuntimeContext context = run("test", "reporting", "biserver-4250.xaction", null, false, parameterProvider, outputHandler, session); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    assertEquals(Messages.getString("BaseTest.USER_RUNNING_ACTION_SEQUENCE"), IRuntimeContext.RUNTIME_STATUS_SUCCESS, context.getStatus()); //$NON-NLS-1$
    finishTest();
  }


}
