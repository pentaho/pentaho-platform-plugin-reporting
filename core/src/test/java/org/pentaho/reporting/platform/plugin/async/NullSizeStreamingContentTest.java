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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NullSizeStreamingContentTest {
  @Test
  public void getStream() throws Exception {
    assertTrue( new AbstractAsyncReportExecution.NullSizeStreamingContent().getStream() instanceof NullInputStream );
  }

  @Test
  public void getContentSize() throws Exception {
    assertEquals( 0L, new AbstractAsyncReportExecution.NullSizeStreamingContent().getContentSize() );
  }

  @Test
  public void clean() throws Exception {
    assertTrue( new AbstractAsyncReportExecution.NullSizeStreamingContent().cleanContent() );
  }

}
