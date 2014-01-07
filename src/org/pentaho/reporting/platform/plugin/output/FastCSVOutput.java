package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.wizard.WizardProcessorUtil;
import org.pentaho.reporting.engine.classic.wizard.model.DetailFieldDefinition;
import org.pentaho.reporting.engine.classic.wizard.model.WizardSpecification;
import org.pentaho.reporting.libraries.repository.ContentIOException;

public class FastCSVOutput implements ReportOutputHandler
{
  public int generate(final MasterReport report,
                      final int acceptedPage,
                      final OutputStream outputStream,
                      final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {
    // todo generate
    WizardSpecification wizardSpecification =
        WizardProcessorUtil.loadWizardSpecification(report, report.getResourceManager());
    DetailFieldDefinition[] detailFields = wizardSpecification.getDetailFieldDefinitions();

    return 0;
  }

  public int paginate(final MasterReport report,
                      final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {
    return 0;
  }

  public void close()
  {

  }

  public boolean supportsPagination()
  {
    return false;
  }

  public Object getReportLock()
  {
    return this;
  }
}
