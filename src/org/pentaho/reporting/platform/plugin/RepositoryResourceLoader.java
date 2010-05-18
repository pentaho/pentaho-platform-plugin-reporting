/*
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
 * Copyright 2008 Pentaho Corporation.  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.pentaho.reporting.libraries.base.util.IOUtils;
import org.pentaho.reporting.libraries.resourceloader.ResourceData;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceKeyCreationException;
import org.pentaho.reporting.libraries.resourceloader.ResourceLoader;
import org.pentaho.reporting.libraries.resourceloader.ResourceLoadingException;
import org.pentaho.reporting.platform.plugin.messages.Messages;

/**
 * This class is implemented to support loading solution files from the pentaho repository into pentaho-reporting
 *
 * @author Will Gorman/Michael D'Amour
 */
public class RepositoryResourceLoader implements ResourceLoader
{

  public static final String SOLUTION_SCHEMA_NAME = "pentaho2"; //$NON-NLS-1$

  public static final String SCHEMA_SEPARATOR = "://"; //$NON-NLS-1$

  private static final String PREFIX = SOLUTION_SCHEMA_NAME + SCHEMA_SEPARATOR;

  /**
   * default constructor
   */
  public RepositoryResourceLoader()
  {
  }

  /**
   * get the schema name, in this case it's always "solution"
   *
   * @return the schema name
   */
  public String getSchema()
  {
    return SOLUTION_SCHEMA_NAME;
  }

  /**
   * create a resource data object
   *
   * @param key resource key
   * @return resource data
   * @throws ResourceLoadingException
   */
  public ResourceData load(final ResourceKey key) throws ResourceLoadingException
  {
    if (isSupportedKey(key) == false)
    {
      throw new ResourceLoadingException("Key format is not recognized.");
    }
    return new RepositoryResourceData(key);
  }

  /**
   * Checks, whether this resource loader implementation was responsible for
   * creating this key.
   *
   * @param key the key that should be tested.
   * @return true, if the key is supported.
   */
  public boolean isSupportedKey(final ResourceKey key)
  {
    if (key.getSchema().equals(getSchema()))
    {
      return true;
    }
    return false;
  }

  /**
   * Creates a new resource key from the given object and the factory keys.
   *
   * @param value       the key value.
   * @param factoryKeys optional parameter map (can be null).
   * @return the created key or null, if the format was not recognized.
   * @throws ResourceKeyCreationException if creating the key failed.
   */
  public ResourceKey createKey(final Object value, final Map factoryKeys)
      throws ResourceKeyCreationException
  {
    if (value instanceof String == false)
    {
      return null;
    }

    final String valueString = (String) value;
    if (valueString.startsWith(PREFIX))
    {
      final String path = valueString.substring(PREFIX.length());
      return new ResourceKey(getSchema(), path, factoryKeys);
    }
    return null;
  }

  /**
   * derive a key from an existing key, used when a relative path is given.
   *
   * @param parent the parent key
   * @param data   the new data to be keyed
   * @return derived key
   * @throws ResourceKeyCreationException
   */
  public ResourceKey deriveKey(final ResourceKey parent,
                               String path,
                               final Map data) throws ResourceKeyCreationException
  {

    // update url to absolute path if currently a relative path
    if (!path.startsWith(PREFIX))
    {
      // we are looking for the current directory specified by the parent. currently
      // the pentaho system uses the native File.separator, so we need to support it
      // we're simply looking for the last "/" or "\" in the parent's url.
      final String originalPath = (String) parent.getIdentifier();
      final String normalizedPath = originalPath.replace('\\', '/');
      final String computedPath = IOUtils.getInstance().createRelativePath(path, normalizedPath);
      path = computedPath.replace('/', File.separatorChar);
    }

    final Map derivedValues = new HashMap(parent.getFactoryParameters());
    if (data != null)
    {
      derivedValues.putAll(data);
    }
    return new ResourceKey(getSchema(), path, derivedValues);
  }

  public ResourceKey deserialize(final ResourceKey bundleKey, final String stringKey) throws ResourceKeyCreationException
  {
    // For now, we are just going to have to pass on this one
    throw new ResourceKeyCreationException(Messages.getInstance().getString("ReportPlugin.cannotDeserializeZipResourceKey")); //$NON-NLS-1$
  }

  public String serialize(final ResourceKey bundleKey, final ResourceKey key) throws ResourceException
  {
    // For now, we are just going to have to pass on this one
    throw new ResourceKeyCreationException(Messages.getInstance().getString("ReportPlugin.cannotSerializeZipResourceKey")); //$NON-NLS-1$
  }

  public URL toURL(final ResourceKey key)
  {
    // not supported ..
    return null;
  }

  public boolean isSupportedDeserializer(final String data)
  {
    // For now, we are just going to have to pass on this one
    return false;
  }

}
