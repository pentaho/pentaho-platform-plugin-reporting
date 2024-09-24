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
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfReportUtil;

import static org.mockito.Mockito.mock;

/**
 * See BACKLOG-7081 for additional details.
 * <p>
 * Created by dima.prokopenko@gmail.com on 3/15/2016.
 */
public class TestInterruptionInterceptionTest {

  IPentahoSession userSession = mock( IPentahoSession.class );

  @Before
  public void before() {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();
    PentahoSessionHolder.setSession( userSession );
  }

  @After
  public void after() {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();
  }

  @Test public void testInterruptFlagProblem() throws Exception {
    PentahoAsyncExecutionInterruptTest t1 = new PentahoAsyncExecutionInterruptTest();
    t1.before();
    t1.testInterrupt();
    t1.after();


    PentahoAsyncReportExecutorTest t2 = new PentahoAsyncReportExecutorTest();
    t2.before();
    t2.testCanCompleteTask();
    t2.after();
  }

  @Test public void testInterruptFlagProblem2() throws Exception {
    PentahoAsyncExecutionInterruptTest t1 = new PentahoAsyncExecutionInterruptTest();

    t1.before();
    t1.testInterrupt();
    t1.after();

    PdfReportUtil.createPDF( new MasterReport(), new NullOutputStream() );
  }

}
