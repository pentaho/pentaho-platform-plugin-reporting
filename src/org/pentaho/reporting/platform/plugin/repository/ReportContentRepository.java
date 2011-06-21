package org.pentaho.reporting.platform.plugin.repository;

import org.pentaho.platform.api.repository.IContentLocation;
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

  public ReportContentRepository(final IContentLocation root, final String actionName)
  {
    this.root = new ReportContentLocation(root, this, actionName);
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
