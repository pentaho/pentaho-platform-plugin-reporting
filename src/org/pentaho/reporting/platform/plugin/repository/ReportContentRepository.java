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
public class ReportContentRepository implements Repository
{
  private DefaultMimeRegistry mimeRegistry;
  private ReportContentLocation root;

  public ReportContentRepository(final RepositoryFile outputFolder)
  {
    this.root = new ReportContentLocation(outputFolder, this);
    this.mimeRegistry = new DefaultMimeRegistry();
  }

  public ContentLocation getRoot() throws ContentIOException
  {
    return root;
  }

  public MimeRegistry getMimeRegistry()
  {
    return mimeRegistry;
  }
}
