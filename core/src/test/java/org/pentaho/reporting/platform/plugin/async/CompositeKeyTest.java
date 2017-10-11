/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

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
