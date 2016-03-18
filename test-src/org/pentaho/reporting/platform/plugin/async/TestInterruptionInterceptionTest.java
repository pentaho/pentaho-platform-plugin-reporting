/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfReportUtil;

/**
 * See BACKLOG-7081 for additional details.
 *
 * Created by dima.prokopenko@gmail.com on 3/15/2016.
 */
public class TestInterruptionInterceptionTest {

  @Test public void testInterruptFlagProblem() throws Exception {
    PentahoAsyncExecutionInterruptTest t1 = new PentahoAsyncExecutionInterruptTest();
    t1.before();
    t1.testInterrupt();

    PentahoAsyncReportExecutorTest t2 = new PentahoAsyncReportExecutorTest();
    t2.before();
    t2.testCanCompleteTask();
  }

  @Test public void testInterruptFlagProblem2() throws Exception {
    PentahoAsyncExecutionInterruptTest t1 = new PentahoAsyncExecutionInterruptTest();
    t1.before();
    t1.testInterrupt();
    PdfReportUtil.createPDF( new MasterReport(), new NullOutputStream() );
  }

}
