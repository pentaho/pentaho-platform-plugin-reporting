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

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;

public class DateTextFormat implements TextFormat {
  private DateTimeFormat dateFormat;

  public DateTextFormat( final String pattern ) {
    this.dateFormat = DateTimeFormat.getFormat( pattern );
  }

  public String format( final Object value ) throws IllegalArgumentException {
    if ( value == null ) {
      return null;
    }
    if ( value instanceof Date == false ) {
      throw new IllegalArgumentException();
    }

    return dateFormat.format( (Date) value );
  }

  public Object parse( final String value ) throws IllegalArgumentException {
    if ( value == null || value.length() == 0 ) {
      return null;
    }
    return dateFormat.parse( value );
  }
}
