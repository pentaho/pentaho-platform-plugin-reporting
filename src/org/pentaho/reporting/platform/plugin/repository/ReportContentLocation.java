package org.pentaho.reporting.platform.plugin.repository;

import java.util.ArrayList;
import java.util.Iterator;

import org.pentaho.platform.api.repository.IContentItem;
import org.pentaho.platform.api.repository.IContentLocation;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.engine.classic.core.util.IntegerCache;
import org.pentaho.reporting.libraries.repository.ContentCreationException;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.LibRepositoryBoot;
import org.pentaho.reporting.libraries.repository.Repository;
import org.pentaho.reporting.libraries.base.util.IOUtils;

/**
 * Creation-Date: 05.07.2007, 14:45:06
 *
 * @author Thomas Morgner
 */
public class ReportContentLocation implements ContentLocation {
  private IContentLocation location;

  private ReportContentRepository repository;

  private String actionName;

  public ReportContentLocation(final IContentLocation location,
                               final ReportContentRepository repository,
                               final String actionName)
  {
    if (location == null)
    {
      throw new NullPointerException("Content-Location cannot be null");
    }
    if (repository == null)
    {
      throw new NullPointerException();
    }
    this.location = location;
    this.repository = repository;
    this.actionName = actionName;
  }

  public ContentEntity[] listContents() throws ContentIOException
  {
    final ArrayList<ReportContentItem> itemCollection = new ArrayList<ReportContentItem>();
    final Iterator<IContentItem> iterator = this.location.getContentItemIterator();
    while (iterator.hasNext())
    {
      itemCollection.add(new ReportContentItem(iterator.next(), this));
    }
    return itemCollection.toArray(new ContentEntity[itemCollection.size()]);
  }

  public ContentEntity getEntry(final String string) throws ContentIOException
  {
    final IContentItem rawItem = this.location.getContentItemByName(string);
    if (rawItem == null)
    {
      throw new ContentIOException("Could not get ContentItem entry"); //$NON-NLS-1$
    }
    return new ReportContentItem(rawItem, this);
  }

  public ContentItem createItem(final String name) throws ContentCreationException
  {
    final String extension = IOUtils.getInstance().getFileExtension(name);
    final String mimeType = MimeHelper.getMimeTypeFromExtension(extension);
    final IContentItem iContentItem = this.location.newContentItem(name, "Generated Report Content", //$NON-NLS-1$
        extension, mimeType, null, IContentItem.WRITEMODE_OVERWRITE);
    return new ReportContentItem(iContentItem, this);
  }

  public ContentLocation createLocation(final String string) throws ContentCreationException
  {
    throw new ContentCreationException("Cannot create a content-location: " + string); //$NON-NLS-1$
  }

  public boolean exists(final String string)
  {
    return (this.location.getContentItemByName(string) != null);
  }

  public String getActionName()
  {
    return actionName;
  }

  public String getName()
  {
    return this.location.getName();
  }

  public Object getContentId()
  {
    return this.location.getId();
  }

  public Object getAttribute(final String domain, final String key)
  {
    if (LibRepositoryBoot.REPOSITORY_DOMAIN.equals(domain))
    {
      if (LibRepositoryBoot.VERSION_ATTRIBUTE.equals(key))
      {
        return IntegerCache.getInteger(location.getRevision());
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
    // We have no parent ...
    return null;
  }

  public Repository getRepository()
  {
    return repository;
  }

  public boolean delete()
  {
    // cannot be deleted ..
    return false;
  }
}
