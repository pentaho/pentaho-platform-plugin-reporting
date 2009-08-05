package org.pentaho.reporting.platform.plugin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * Unit tests for the ReportingComponent.
 * 
 * @author Michael D'Amour
 */
public class ReportingComponentTest extends TestCase {
  
  public void testReportDefinitionAsInput() throws Exception
  {
    // create an instance of the component
    SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    FileInputStream reportDefinition = new FileInputStream("tests/integration-tests/resource/solution/test/reporting/report.prpt");
    Map<String,Object> inputs = new HashMap<String, Object>();
    inputs.put(SimpleReportingComponent.REPORT_DEFINITION_INPUT, reportDefinition);
    rc.setInputs(inputs);
    rc.setOutputType("application/pdf");

    FileOutputStream outputStream = new FileOutputStream("/tmp/" + System.currentTimeMillis() + ".pdf");
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
    FileInputStream reportDefinition = new FileInputStream("tests/integration-tests/resource/solution/test/reporting/report.prpt");
    rc.setReportDefinitionInputStream(reportDefinition);
    rc.setOutputType("application/pdf");

    FileOutputStream outputStream = new FileOutputStream("/tmp/" + System.currentTimeMillis() + ".pdf");
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
    FileInputStream reportDefinition = new FileInputStream("tests/integration-tests/resource/solution/test/reporting/report.prpt");
    rc.setReportDefinitionInputStream(reportDefinition);
    rc.setOutputType("text/html");

    FileOutputStream outputStream = new FileOutputStream("/tmp/" + System.currentTimeMillis() + ".html");
    rc.setOutputStream(outputStream);

    // validate the component
    assertTrue(rc.validate());
    // execute the component
    assertTrue(rc.execute());
  }

}
