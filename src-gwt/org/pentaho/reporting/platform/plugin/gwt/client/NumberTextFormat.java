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

import com.google.gwt.i18n.client.NumberFormat;

public class NumberTextFormat implements TextFormat {
  private NumberFormat formatter;

  public NumberTextFormat( final String pattern ) {
    formatter = NumberFormat.getFormat( pattern );
  }

  public String format( final Object value ) throws IllegalArgumentException {
    if ( value == null ) {
      return null;
    }
    if ( value instanceof Number == false ) {
      throw new IllegalArgumentException();
    }
    final Number n = (Number) value;
    return formatter.format( n.doubleValue() );
  }

  public Object parse( final String value ) throws NumberFormatException {
    if ( value == null || value.length() == 0 ) {
      return null;
    }
    return formatter.parse( value );
  }
}
