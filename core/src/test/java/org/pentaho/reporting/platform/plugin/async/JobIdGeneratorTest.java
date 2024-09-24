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

import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class JobIdGeneratorTest {

  @Test
  public void onLogout() throws Exception {
    final JobIdGenerator jobIdGenerator = new JobIdGenerator();
    final IPentahoSession mock = mock( IPentahoSession.class );
    final UUID generateId = jobIdGenerator.generateId( mock );
    assertNotNull( generateId );
    jobIdGenerator.onLogout( mock );
    assertFalse( jobIdGenerator.acquire( mock, generateId ) );
  }


  @Test
  public void onLogoutNullSession() throws Exception {
    final JobIdGenerator jobIdGenerator = new JobIdGenerator();
    final IPentahoSession mock = mock( IPentahoSession.class );
    final UUID generateId = jobIdGenerator.generateId( mock );
    assertNotNull( generateId );
    jobIdGenerator.onLogout( null );
    assertTrue( jobIdGenerator.acquire( mock, generateId ) );
  }

  @Test
  public void generateId() throws Exception {
    final JobIdGenerator jobIdGenerator = new JobIdGenerator();
    final IPentahoSession mock = mock( IPentahoSession.class );
    final UUID generateId = jobIdGenerator.generateId( mock );
    assertNotNull( generateId );
  }

  @Test( expected = ArgumentNullException.class )
  public void generateIdNullSession() throws Exception {
    final JobIdGenerator jobIdGenerator = new JobIdGenerator();
    jobIdGenerator.generateId( null );
  }

  @Test
  public void acquire() throws Exception {
    final JobIdGenerator jobIdGenerator = new JobIdGenerator();
    final IPentahoSession mock = mock( IPentahoSession.class );
    final UUID generateId = jobIdGenerator.generateId( mock );
    assertNotNull( generateId );
    assertFalse( jobIdGenerator.acquire( mock( IPentahoSession.class ), generateId ) );
    assertFalse( jobIdGenerator.acquire( mock, UUID.randomUUID() ) );
    assertTrue( jobIdGenerator.acquire( mock, generateId ) );
    assertFalse( jobIdGenerator.acquire( mock, generateId ) );
  }


  @Test( expected = ArgumentNullException.class )
  public void acquireNullSession() throws Exception {
    final JobIdGenerator jobIdGenerator = new JobIdGenerator();
    jobIdGenerator.acquire( null, UUID.randomUUID() );
  }

  @Test
  public void acquireNullId() throws Exception {
    final JobIdGenerator jobIdGenerator = new JobIdGenerator();
    assertFalse( jobIdGenerator.acquire( mock( IPentahoSession.class ), null ) );
  }

}
