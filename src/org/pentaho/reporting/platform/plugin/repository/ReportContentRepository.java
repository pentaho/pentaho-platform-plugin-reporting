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

package org.pentaho.reporting.platform.plugin.repository;

import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultMimeRegistry;
import org.pentaho.reporting.libraries.repository.MimeRegistry;
import org.pentaho.reporting.libraries.repository.Repository;

/**
 * Creation-Date: 05.07.2007, 14:43:40
 * 
 * @author Thomas Morgner
 */
public class ReportContentRepository implements Repository {
  private DefaultMimeRegistry mimeRegistry;
  private ReportContentLocation root;

  public ReportContentRepository( final RepositoryFile outputFolder ) {
    this.root = new ReportContentLocation( outputFolder, this );
    this.mimeRegistry = new DefaultMimeRegistry();
  }

  public ContentLocation getRoot() throws ContentIOException {
    return root;
  }

  public MimeRegistry getMimeRegistry() {
    return mimeRegistry;
  }
}
