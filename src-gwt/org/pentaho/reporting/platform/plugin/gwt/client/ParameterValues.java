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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.http.client.URL;

public class ParameterValues {
  private HashMap<String, ArrayList<String>> backend;
  private boolean trace;

  public ParameterValues() {
    backend = new HashMap<String, ArrayList<String>>();
  }

  public void setSelectedValue( final String parameter, final String value ) {
    setSelectedValues( parameter, new String[] { value } );
  }

  public void setSelectedValues( final String parameter, final String[] values ) {
    if ( values.length == 0 ) {
      backend.remove( parameter );
      return;
    }

    ArrayList<String> strings = backend.get( parameter );
    if ( strings == null ) {
      strings = new ArrayList<String>();
      backend.put( parameter, strings );
    }
    strings.clear();
    strings.addAll( Arrays.asList( values ) );
  }

  public void addSelectedValue( final String parameter, final String value ) {
    ArrayList<String> strings = backend.get( parameter );
    if ( strings == null ) {
      strings = new ArrayList<String>();
      backend.put( parameter, strings );
    }
    strings.add( value );
  }

  public void removeSelectedValue( final String parameter, final String value ) {
    final ArrayList<String> strings = backend.get( parameter );
    if ( strings == null ) {
      return;
    }
    strings.remove( value );
    if ( strings.isEmpty() ) {
      backend.remove( parameter );
    }
  }

  public String[] getParameterNames() {
    return backend.keySet().toArray( new String[backend.size()] );
  }

  public String[] getParameterValues( final String name ) {
    final ArrayList<String> strings = backend.get( name );
    if ( strings == null ) {
      return null;
    }
    return strings.toArray( new String[strings.size()] );
  }

  public boolean containsParameter( final String name ) {
    return backend.containsKey( name );
  }

  public String toURL() {
    final StringBuffer b = new StringBuffer();
    for ( final Map.Entry<String, ArrayList<String>> entry : backend.entrySet() ) {
      final String key = URL.encodeComponent( entry.getKey() );
      final ArrayList<String> list = entry.getValue();
      for ( final String value : list ) {
        if ( value == null ) {
          continue;
        }

        if ( b.length() > 0 ) {
          b.append( "&" );
        }
        b.append( key );
        b.append( '=' );
        b.append( URL.encodeComponent( value ) );
      }
    }
    return b.toString();
  }
}
