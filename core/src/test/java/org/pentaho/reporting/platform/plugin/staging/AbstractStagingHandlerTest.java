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


package org.pentaho.reporting.platform.plugin.staging;

import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class AbstractStagingHandlerTest {
  @Test
  public void getStagingHandlerImpl() throws Exception {
    File tmp = new File( "target/test/resource/solution/system/tmp" );
    tmp.mkdirs();

    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    try {
      final StagingHandler handler1 =
        AbstractStagingHandler.getStagingHandlerImpl( null, null, StagingMode.THRU );
      assertTrue( handler1 instanceof ThruStagingHandler );
      final StagingHandler handler2 =
        AbstractStagingHandler.getStagingHandlerImpl( null, null, StagingMode.MEMORY );
      assertTrue( handler2 instanceof MemStagingHandler );

      final StagingHandler handler3 =
        AbstractStagingHandler.getStagingHandlerImpl( null, new StandaloneSession( "test" ), StagingMode.TMPFILE );
      assertTrue( handler3 instanceof TempFileStagingHandler );

    } finally {
      microPlatform.stop();
      microPlatform = null;
    }
  }
}
