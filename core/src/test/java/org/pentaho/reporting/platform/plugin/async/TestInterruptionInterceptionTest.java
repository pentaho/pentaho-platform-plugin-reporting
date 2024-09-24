/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

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
