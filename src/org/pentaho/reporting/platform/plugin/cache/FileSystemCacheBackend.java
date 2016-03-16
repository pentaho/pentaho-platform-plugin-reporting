/* !
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this
 *  program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  or from the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 *
 */
package org.pentaho.reporting.platform.plugin.cache;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.libraries.base.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Default interface for cache backend
 */
public class FileSystemCacheBackend implements ICacheBackend {

  private static final Log logger = LogFactory.getLog( FileSystemCacheBackend.class );
  public static final String REPLACEMENT = "_";
  public static final String SLASHES = "[/\\\\]+";
  public static final String EXT = "\\.metadata|\\.data";
  public static final String DATA = ".data";
  public static final String METADATA = ".metadata";
  private final Map<List<String>, ReentrantReadWriteLock> syncMap;

  private String cachePath;

  public FileSystemCacheBackend() {
    syncMap = new HashMap<>();
  }

  public void setCachePath( final String cachePath ) {
    this.cachePath = getSystemTmp() + cachePath;
  }

  @Override
  public boolean write( final List<String> key, final Serializable value,
                        final Map<String, Serializable> metaData ) {
    final List<String> cleanKey = sanitizeKeySegments( key );
    final List<Lock> locks = lockForWrite( cleanKey );
    try {
      final String filePath = cachePath + StringUtils.join( cleanKey, File.separator );
      if ( writeFile( value, filePath + DATA ) ) {
        return false;
      }

      final HashMap<String, Serializable> writeableMetaData = new HashMap<>();
      if ( metaData != null ) {
        writeableMetaData.putAll( metaData );
      }
      if ( writeFile( writeableMetaData, filePath + METADATA ) ) {
        return false;
      }
      return true;
    } finally {
      unlock( locks );
    }
  }

  private boolean writeFile( Serializable value, String filePath ) {
    final File file = new File( filePath );
    try {
      //create file structure
      file.getParentFile().mkdirs();
      if ( !file.exists() ) {
        file.createNewFile();
      }
      //closable resources
      try ( final FileOutputStream fout = new FileOutputStream( file );
            final ObjectOutputStream oos = new ObjectOutputStream( fout ) ) {
        oos.writeObject( value );
      }
    } catch ( final IOException e ) {
      logger.error( "Can't write cache: ", e );
      return true;
    }
    return false;
  }


  @Override
  public Serializable read( final List<String> key ) {
    Object result = null;
    final List<String> cleanKey = sanitizeKeySegments( key );
    final List<Lock> locks = lockForRead( cleanKey );
    try {
      final String filePath = cachePath + StringUtils.join( cleanKey, File.separator ) + DATA;
      final File f = new File( filePath );
      if ( !f.exists() ) {
        return null;
      }

      try ( final FileInputStream fis = new FileInputStream( f );
            final ObjectInputStream ois = new ObjectInputStream( fis ) ) {
        result = ois.readObject();
      } catch ( final Exception e ) {
        logger.debug( "Can't read cache: ", e );
      }
      return (Serializable) result;
    } finally {
      unlock( locks );
    }
  }

  private Map<String, Serializable> readMetaData( final List<String> key ) {
    Object result = null;
    final List<String> cleanKey = sanitizeKeySegments( key );
    final List<String> noExtCleanKey = new ArrayList<>( cleanKey.size() );

    for ( final String cleanSegment : cleanKey ) {
      noExtCleanKey.add( cleanSegment.replaceAll( EXT, "" ) );
    }

    final List<Lock> locks = lockForRead( noExtCleanKey );
    try {
      final String filePath = cachePath + StringUtils.join( noExtCleanKey, File.separator ) + METADATA;
      final File f = new File( filePath );
      if ( !f.exists() ) {
        return null;
      }

      try ( final FileInputStream fis = new FileInputStream( f );
            final ObjectInputStream ois = new ObjectInputStream( fis ) ) {
        result = ois.readObject();
      } catch ( final Exception e ) {
        logger.debug( "Can't read cache: ", e );
      }
      if ( result instanceof Map ) {
        return (Map<String, Serializable>) result;
      } else {
        return null;
      }
    } finally {
      unlock( locks );
    }
  }

  /**
   * Locks are released in reverse order. First we release the more specialized locks and traverse upwards towards the
   * root directory.
   *
   * @param locks
   */
  private void unlock( final List<Lock> locks ) {
    for ( int i = locks.size() - 1; i >= 0; i-- ) {
      final Lock lock = locks.get( i );
      lock.unlock();
    }
  }

  private List<Lock> lockForRead( List<String> key ) {
    List<Lock> retval;
    if ( !key.isEmpty() ) {
      final List<String> parent = key.subList( 0, key.size() - 1 );
      retval = lockForRead( parent );
    } else {
      retval = new ArrayList<>();
    }

    final Lock lock = getLock( key ).readLock();
    lock.lock();
    retval.add( lock );
    return retval;
  }

  /**
   * Acquires read locks for all sub-directories, and a final write lock for the current working directory. It acquires
   * the parent locks first, before trying to get more local locks.
   *
   * @param key
   * @return
   */
  private List<Lock> lockForWrite( List<String> key ) {
    List<Lock> retval;
    if ( !key.isEmpty() ) {
      final List<String> parent = key.subList( 0, key.size() - 1 );
      retval = lockForRead( parent );
    } else {
      retval = new ArrayList<>();
    }

    final Lock lock = getLock( key ).writeLock();
    lock.lock();
    retval.add( lock );
    return retval;
  }

  /**
   * Returns an object for read/write synchronization
   *
   * @param key compound key
   * @return lock object
   */
  private synchronized ReentrantReadWriteLock getLock( final List<String> key ) {
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock( true );
    if ( !syncMap.containsKey( key ) ) {
      syncMap.put( key, lock );
      return lock;
    }
    return syncMap.get( key );
  }

  public void purgeSegment( final List<String> key,
                            final BiPredicate<List<String>, Map<String, Serializable>> p ) {
    final List<String> cleanKey = sanitizeKeySegments( key );
    final List<Lock> locks = lockForWrite( cleanKey );
    try {
      for ( String name : listKeys( cleanKey ) ) {
        ArrayList<String> entryKey = new ArrayList<>( cleanKey );
        entryKey.add( name );
        final Map<String, Serializable> metaData = readMetaData( entryKey );
        if ( p.test( entryKey, metaData ) ) {
          purge( entryKey );
        }
      }
      for ( String name : listSegments( cleanKey ) ) {
        ArrayList<String> entryKey = new ArrayList<>( cleanKey );
        entryKey.add( name );
        purgeSegment( entryKey, p );
      }
    } finally {
      unlock( locks );
    }
  }

  @Override
  public boolean purge( final List<String> key ) {
    final List<String> cleanKey = sanitizeKeySegments( key );
    final List<Lock> locks = lockForWrite( cleanKey );
    try {
      final String fileName = cachePath + StringUtils.join( cleanKey, File.separator );

      if ( fileName.endsWith( DATA ) ) {
        final File data = new File( fileName );
        if ( !data.exists() ) {
          return true;
        }
        final File meta = new File( fileName.replace( DATA, METADATA ) );
        syncMap.remove( cleanKey );
        return data.delete() && meta.delete();
      }

      final File file = new File( fileName );

      if ( file.isDirectory() ) {
        final Set<String> subKeys = listKeys( cleanKey );
        for ( final String subKey : subKeys ) {
          final ArrayList<String> subEntry = new ArrayList<>( cleanKey );
          subEntry.add( subKey );
          purge( subEntry );
        }

        syncMap.remove( cleanKey );
        FileUtils.deleteDirectory( file );
        return !file.exists();
      }


      final File data = new File( fileName + DATA );

      if ( !file.exists() && !data.exists() ) {
        return true;
      }

      final File metadata = new File( fileName + METADATA );
      syncMap.remove( cleanKey );
      return data.delete() && metadata.delete();

    } catch ( final Exception e ) {
      logger.debug( "Can't delete cache: ", e );
      return false;
    } finally {
      unlock( locks );
    }
  }

  private Set<String> listKeys( final List<String> unsafeKey ) {
    final List<String> sanitized = sanitizeKeySegments( unsafeKey );
    final Set<String> resultSet = new HashSet<>();
    final File directory = new File( cachePath + StringUtils.join( sanitized, File.separator ) );
    final File[] fList = directory.listFiles();
    if ( fList != null ) {
      for ( final File file : fList ) {
        final String name = file.getName();
        if ( file.isFile() && name.endsWith( DATA ) ) {
          resultSet.add( IOUtils.getInstance().getFileName( name ) );
        }
      }
    }
    return resultSet;
  }

  private Set<String> listSegments( final List<String> unsafeKey ) {
    final List<String> sanitized = sanitizeKeySegments( unsafeKey );
    final Set<String> resultSet = new HashSet<>();
    final File directory = new File( cachePath + StringUtils.join( sanitized, File.separator ) );
    final File[] fList = directory.listFiles();
    if ( fList != null ) {
      for ( final File file : fList ) {
        if ( file.isDirectory() ) {
          resultSet.add( file.getName() );
        }
      }
    }
    return resultSet;
  }

  private String getSystemTmp() {
    final String s = System.getProperty( "java.io.tmpdir" ); //$NON-NLS-1$
    final char c = s.charAt( s.length() - 1 );
    if ( ( c != '/' ) && ( c != '\\' ) ) {
      System.setProperty( "java.io.tmpdir", s + "/" ); //$NON-NLS-1$//$NON-NLS-2$
    }
    return s;
  }

  private static List<String> sanitizeKeySegments( final List<String> key ) {
    final List<String> clean = new ArrayList<>( key.size() );
    for ( final String segment : key ) {
      clean.add( segment.replaceAll( SLASHES, REPLACEMENT ) );
    }
    return clean;
  }

}
