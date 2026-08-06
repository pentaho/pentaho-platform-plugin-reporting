/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 *******************************************************************************/

package org.pentaho.reporting.platform.plugin;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelTableModule;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith( Parameterized.class )
public class LegacyLockedExcelOutputTest {

  @Parameterized.Parameters
  public static Collection<Object[]> legacyExcelOutputTypes() {
    return Arrays.asList( new Object[][] {
      { ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE, ExcelTableModule.XLSX_FLOW_EXPORT_TYPE },
      { ExcelTableModule.EXCEL_PAGE_EXPORT_TYPE, ExcelTableModule.XLSX_PAGE_EXPORT_TYPE },
      { ExcelTableModule.EXCEL_STREAM_EXPORT_TYPE, ExcelTableModule.XLSX_STREAM_EXPORT_TYPE }
    } );
  }

  private final String legacyOutputType;
  private final String xlsxOutputType;

  public LegacyLockedExcelOutputTest( final String legacyOutputType, final String xlsxOutputType ) {
    this.legacyOutputType = legacyOutputType;
    this.xlsxOutputType = xlsxOutputType;
  }

  @Test
  public void normalizesLockedLegacyExcelOutput() {
    final MasterReport report = createReport( true, legacyOutputType );

    SimpleReportingComponent.normalizeLegacyExcelOutput( report );

    assertEquals( xlsxOutputType, getPreferredOutputType( report ) );
  }

  @Test
  public void normalizesUnlockedLegacyExcelOutput() {
    final MasterReport report = createReport( false, legacyOutputType );

    SimpleReportingComponent.normalizeLegacyExcelOutput( report );

    assertEquals( xlsxOutputType, getPreferredOutputType( report ) );
  }

  @Test
  public void doesNotNormalizeOtherLockedOutput() {
    final MasterReport report = createReport( true, ExcelTableModule.XLSX_PAGE_EXPORT_TYPE );

    SimpleReportingComponent.normalizeLegacyExcelOutput( report );

    assertEquals( ExcelTableModule.XLSX_PAGE_EXPORT_TYPE, getPreferredOutputType( report ) );
  }

  private MasterReport createReport( final boolean locked, final String preferredOutputType ) {
    final MasterReport report = new MasterReport();
    report.setAttribute( AttributeNames.Core.NAMESPACE, AttributeNames.Core.LOCK_PREFERRED_OUTPUT_TYPE, locked );
    report.setAttribute( AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE, preferredOutputType );
    return report;
  }

  private Object getPreferredOutputType( final MasterReport report ) {
    return report.getAttribute( AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE );
  }
}
