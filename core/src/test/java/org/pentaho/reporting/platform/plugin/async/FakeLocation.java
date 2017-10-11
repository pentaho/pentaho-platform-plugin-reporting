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

import org.eclipse.jetty.util.ConcurrentHashSet;
import org.pentaho.reporting.libraries.repository.ContentCreationException;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.Repository;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FakeLocation implements ContentLocation {

  private CountDownLatch latch;


  public FakeLocation() {
  }

  public FakeLocation( final CountDownLatch firstLatch ) {
    this.latch = firstLatch;
  }

  private Set<String> files = new ConcurrentHashSet<>();

  @Override public ContentEntity[] listContents() throws ContentIOException {
    throw new UnsupportedOperationException();
  }

  @Override public ContentEntity getEntry( final String s ) throws ContentIOException {
    throw new UnsupportedOperationException();
  }

  @Override public ContentItem createItem( final String s ) throws ContentCreationException {
    try {
      if ( latch != null ) {
        latch.countDown();
        latch.await();
      }
      Thread.sleep( 100 );
    } catch ( final InterruptedException e ) {
      e.printStackTrace();
    }
    if ( exists( s ) ) {
      throw new ContentCreationException();
    } else {
      files.add( s );
      final ContentItem mock = mock( ContentItem.class );
      try {
        when( mock.getOutputStream() ).thenReturn( new org.apache.commons.io.output.NullOutputStream() );
      } catch ( ContentIOException | IOException e ) {
        e.printStackTrace();
      }
      return mock;
    }

  }

  @Override public ContentLocation createLocation( final String s ) throws ContentCreationException {
    throw new UnsupportedOperationException();
  }

  @Override public boolean exists( final String s ) {
    return files.contains( s );
  }

  @Override public String getName() {
    throw new UnsupportedOperationException();
  }

  @Override public Object getContentId() {
    throw new UnsupportedOperationException();
  }

  @Override public Object getAttribute( final String s, final String s1 ) {
    return null;
  }

  @Override public boolean setAttribute( final String s, final String s1, final Object o ) {
    throw new UnsupportedOperationException();
  }

  @Override public ContentLocation getParent() {
    throw new UnsupportedOperationException();
  }

  @Override public Repository getRepository() {
    throw new UnsupportedOperationException();
  }

  @Override public boolean delete() {
    throw new UnsupportedOperationException();
  }
}


