package org.pentaho.reporting.platform.plugin.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pentaho.platform.api.repository.IContentItem;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.LibRepositoryBoot;
import org.pentaho.reporting.libraries.repository.Repository;

/**
 * Creation-Date: 05.07.2007, 14:54:08
 *
 * @author Thomas Morgner
 */
public class ReportContentItem implements ContentItem
{
  private IContentItem backend;

  private ReportContentLocation parent;

  public ReportContentItem(final IContentItem backend, final ReportContentLocation parent)
  {
    if (parent == null)
    {
      throw new NullPointerException();
    }
    if (backend == null)
    {
      throw new NullPointerException();
    }
    this.backend = backend;
    this.parent = parent;
  }

  public String getMimeType() throws ContentIOException
  {
    return backend.getMimeType();
  }

  public OutputStream getOutputStream() throws ContentIOException, IOException
  {
    return backend.getOutputStream(parent.getActionName());
  }

  public InputStream getInputStream() throws ContentIOException, IOException
  {
    return backend.getInputStream();
  }

  public boolean isReadable()
  {
    return false;
  }

  public boolean isWriteable()
  {
    return true;
  }

  public String getName()
  {
    return backend.getName();
  }

  public Object getContentId()
  {
    return backend.getId();
  }

  public Object getAttribute(final String domain, final String key)
  {
    if (LibRepositoryBoot.REPOSITORY_DOMAIN.equals(domain))
    {
      if (LibRepositoryBoot.SIZE_ATTRIBUTE.equals(key))
      {
        return new Long(backend.getFileSize());
      }
      else if (LibRepositoryBoot.VERSION_ATTRIBUTE.equals(key))
      {
        return backend.getFileDateTime();
      }
    }
    return null;
  }

  public boolean setAttribute(final String domain, final String key, final Object object)
  {
    return false;
  }

  public ContentLocation getParent()
  {
    return parent;
  }

  public Repository getRepository()
  {
    return parent.getRepository();
  }

  public boolean delete()
  {
    backend.removeVersion(backend.getId());
    return true;
  }
}
