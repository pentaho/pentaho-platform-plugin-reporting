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
 * Copyright 2006 - 2018 Hitachi Vantara.  All rights reserved.
 */

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
