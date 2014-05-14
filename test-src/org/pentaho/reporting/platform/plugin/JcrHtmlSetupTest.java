package org.pentaho.reporting.platform.plugin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.html.FastHtmlContentItems;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.stream.StreamContentLocation;
import org.pentaho.reporting.platform.plugin.output.StreamJcrHtmlOutput;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.PentahoURLRewriter;
import org.pentaho.reporting.platform.plugin.repository.ReportContentLocation;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;

import static org.junit.Assert.assertTrue;

public class JcrHtmlSetupTest {
  private MicroPlatform microPlatform;

  @Before
  public void setUp() throws Exception {
    new File("./resource/solution/system/tmp").mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession(session);
  }

  @After
  public void tearDown() throws Exception {
    microPlatform.stop();
  }

  @Test
  public void testCorrectSetup() throws ReportProcessingException, ContentIOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    TestStreamJcrHtmlOutput test = new TestStreamJcrHtmlOutput();
    test.setContentHandlerPattern("something");
    test.setJcrOutputPath("/");

    FastHtmlContentItems result = test.computeContentItems(bout);
    assertTrue(result.getContentLocation() instanceof StreamContentLocation);
    assertTrue(result.getContentNameGenerator() instanceof DefaultNameGenerator);
    assertTrue(result.getDataLocation() instanceof ReportContentLocation);
    assertTrue(result.getDataLocation().getRepository() instanceof ReportContentRepository);
    assertTrue(result.getDataNameGenerator() instanceof PentahoNameGenerator);
    assertTrue(result.getUrlRewriter() instanceof PentahoURLRewriter);
  }

  private static class TestStreamJcrHtmlOutput extends StreamJcrHtmlOutput {
    @Override
    protected FastHtmlContentItems computeContentItems(OutputStream outputStream)
            throws ReportProcessingException, ContentIOException {
      return super.computeContentItems(outputStream);
    }
  }
}
