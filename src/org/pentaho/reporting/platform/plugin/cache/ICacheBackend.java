/*
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
 *  Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved. *
 */
package org.pentaho.reporting.platform.plugin.cache;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Simple interface for cache backend
 */
public interface ICacheBackend {

  /**
   * Persist object
   *
   * @param key   path
   * @param value object
   * @return if operation succeed
   */
  boolean write( List<String> key, Serializable value );

  /**
   * Retrive object from storage
   *
   * @param key path
   * @return object
   */
  Serializable read( List<String> key );

  /**
   * Remove object from storage
   *
   * @param key path
   * @return if operation succeed
   */
  boolean purge( List<String> key );

  /**
   * Provides all keys that are children for key
   *
   * @return set of keys
   */
  Set<String> listKeys( List<String> key );

}
