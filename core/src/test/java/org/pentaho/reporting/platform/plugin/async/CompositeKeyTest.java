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

import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.StandaloneSession;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompositeKeyTest {
  @Test
  public void isSameSession() throws Exception {

    final IPentahoSession session1 = new StandaloneSession( "one" );
    final IPentahoSession session2 = new StandaloneSession( "two" );

    final PentahoAsyncExecutor.CompositeKey key1 = new PentahoAsyncExecutor.CompositeKey( session1, UUID.randomUUID() );

    assertTrue( key1.isSameSession( session1.getId() ) );
    assertFalse( key1.isSameSession( session2.getId() ) );
  }


  @Test
  public void testEquals() throws Exception {

    final IPentahoSession session1 = new StandaloneSession( "one" );
    final IPentahoSession session2 = new StandaloneSession( "two" );

    final PentahoAsyncExecutor.CompositeKey key1 = new PentahoAsyncExecutor.CompositeKey( session1, UUID.randomUUID() );
    final PentahoAsyncExecutor.CompositeKey key2 = new PentahoAsyncExecutor.CompositeKey( session2, UUID.randomUUID() );
    assertTrue( key1.equals( key1 ) );
    assertFalse( key1.equals( key2 ) );
    assertFalse( key1.equals( null ) );
    assertFalse( key1.equals( UUID.randomUUID() ) );


  }

}
