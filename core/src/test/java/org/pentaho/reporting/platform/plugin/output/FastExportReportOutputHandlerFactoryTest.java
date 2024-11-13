/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.output;

import junit.framework.TestCase;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;

import static org.mockito.Mockito.*;

public class FastExportReportOutputHandlerFactoryTest extends TestCase {
  public static final String FLAG = "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent";
  FastExportReportOutputHandlerFactory handlerFactory;

  protected void setUp() {
    handlerFactory = new FastExportReportOutputHandlerFactory();
  }

  public void testCreateCsvOutput() throws Exception {
    assertTrue( handlerFactory.createCsvOutput() instanceof FastCSVOutput );
    handlerFactory.setCsvAvailable( false );
    assertNull( handlerFactory.createCsvOutput() );
  }

  public void testCreateHtmlStreamOutput() throws Exception {
    final FastExportReportOutputHandlerFactory fact = spy( new FastExportReportOutputHandlerFactory() );

    final ReportOutputHandlerSelector selector = mock( ReportOutputHandlerSelector.class );
    fact.setHtmlStreamAvailable( false );
    assertNull( fact.createHtmlStreamOutput( selector ) );

    fact.setHtmlStreamAvailable( true );
    doReturn( true ).when( selector ).isUseJcrOutput();
    doReturn( "" ).when( selector ).getJcrOutputPath();
    doReturn( "" ).when( fact ).computeContentHandlerPattern( selector );
    assertTrue( fact.createHtmlStreamOutput( selector ) instanceof FastStreamJcrHtmlOutput );

    doReturn( false ).when( selector ).isUseJcrOutput();
    assertTrue( fact.createHtmlStreamOutput( selector ) instanceof FastStreamHtmlOutput );
  }

  public void testCreateXlsOutput() throws Exception {
    final ReportOutputHandlerSelector selector = mock( ReportOutputHandlerSelector.class );
    assertTrue( handlerFactory.createXlsOutput( selector ) instanceof FastXLSOutput );

    handlerFactory.setXlsxAvailable( false );
    assertNull( handlerFactory.createXlsOutput( selector ) );
  }

  public void testCreateXlsxOutput() throws Exception {
    final ReportOutputHandlerSelector selector = mock( ReportOutputHandlerSelector.class );
    assertTrue( handlerFactory.createXlsxOutput( selector ) instanceof FastXLSXOutput );

    handlerFactory.setXlsxAvailable( false );
    assertNull( handlerFactory.createXlsOutput( selector ) );
  }

  public void testGetIsCachePageableHtmlContentEnabled() throws Exception {
    final MasterReport report = mock( MasterReport.class );
    final ModifiableConfiguration config = ClassicEngineBoot.getInstance().getEditableConfig();

    config.setConfigProperty( FLAG, "true" );
    assertTrue( handlerFactory.isCachePageableHtmlContentEnabled( report ) );

    config.setConfigProperty( FLAG, "false" );
    assertFalse( handlerFactory.isCachePageableHtmlContentEnabled( report ) );

    config.setConfigProperty( FLAG, "" );
    assertFalse( handlerFactory.isCachePageableHtmlContentEnabled( report ) );

    doReturn( true ).when( report ).getAttribute( AttributeNames.Pentaho.NAMESPACE,
      AttributeNames.Pentaho.DYNAMIC_REPORT_CACHE );
    assertTrue( handlerFactory.isCachePageableHtmlContentEnabled( report ) );

    doReturn( false ).when( report ).getAttribute( AttributeNames.Pentaho.NAMESPACE,
      AttributeNames.Pentaho.DYNAMIC_REPORT_CACHE );
    assertFalse( handlerFactory.isCachePageableHtmlContentEnabled( report ) );

    config.setConfigProperty( FLAG, "true" );
    assertFalse( handlerFactory.isCachePageableHtmlContentEnabled( report ) );
  }
}
