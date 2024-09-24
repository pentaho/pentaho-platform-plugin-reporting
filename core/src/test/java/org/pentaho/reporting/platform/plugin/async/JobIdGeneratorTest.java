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
