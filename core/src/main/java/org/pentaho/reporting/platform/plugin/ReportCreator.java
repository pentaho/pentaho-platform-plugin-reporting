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

package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;

import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.parser.base.ReportGenerator;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.xml.sax.InputSource;

public class ReportCreator {
  public static MasterReport createReport( final InputStream inputStream, final URL url ) throws IOException,
    ResourceException {
    final ReportGenerator generator = ReportGenerator.createInstance();
    final InputSource repDefInputSource = new InputSource( inputStream );
    return generator.parseReport( repDefInputSource, url );
  }

  public static MasterReport createReportByName( final String fullFilePathAndName ) throws ResourceException,
    IOException {
    IUnifiedRepository unifiedRepository =
        PentahoSystem.get( IUnifiedRepository.class, PentahoSessionHolder.getSession() );
    RepositoryFile repositoryFile = unifiedRepository.getFile( fullFilePathAndName );
    if ( repositoryFile == null ) {
      throw new IOException( "File " + fullFilePathAndName + " not found in repository" );
    } else {
      return createReport( repositoryFile.getId() );
    }
  }

  public static MasterReport createReport( final Serializable fileId ) throws ResourceException, IOException {
    final ResourceManager resourceManager = new ResourceManager();
    resourceManager.registerDefaults();
    final HashMap helperObjects = new HashMap();
    // add the runtime context so that PentahoResourceData class can get access
    // to the solution repo

    ResourceKey key = null;

    IUnifiedRepository unifiedRepository =
        PentahoSystem.get( IUnifiedRepository.class, PentahoSessionHolder.getSession() );
    RepositoryFile repositoryFile = unifiedRepository.getFileById( fileId );
    if ( repositoryFile != null ) {
      key =
          resourceManager.createKey( RepositoryResourceLoader.SOLUTION_SCHEMA_NAME
              + RepositoryResourceLoader.SCHEMA_SEPARATOR + repositoryFile.getPath(), helperObjects );
    } else {
      key =
          resourceManager.createKey( RepositoryResourceLoader.SOLUTION_SCHEMA_NAME
              + RepositoryResourceLoader.SCHEMA_SEPARATOR + fileId, helperObjects );
    }

    final Resource resource = resourceManager.create( key, null, MasterReport.class );
    return (MasterReport) resource.getResource();
  }

}
