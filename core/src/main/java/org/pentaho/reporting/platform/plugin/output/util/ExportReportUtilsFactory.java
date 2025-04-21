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

package org.pentaho.reporting.platform.plugin.output.util;

public class ExportReportUtilsFactory {
  private static final ThreadLocal<ExportReportUtils> threadLocalInstance = ThreadLocal.withInitial( ExportReportUtils::new );

  private ExportReportUtilsFactory() {
    // Prevent instantiation
  }

  public static ExportReportUtils getUtil() {
    return threadLocalInstance.get();
  }

  public static void clear() {
    threadLocalInstance.remove();
  }
}

