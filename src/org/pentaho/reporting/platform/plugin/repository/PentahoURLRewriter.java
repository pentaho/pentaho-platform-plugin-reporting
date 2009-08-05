package org.pentaho.reporting.platform.plugin.repository;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.pentaho.reporting.engine.classic.core.modules.output.table.html.URLRewriteException;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.URLRewriter;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;

/**
 * Creation-Date: 05.07.2007, 19:16:13
 * 
 * @author Thomas Morgner
 */
public class PentahoURLRewriter implements URLRewriter {
  private String pattern;
  private boolean useContentIdAsName;

  public PentahoURLRewriter(final String pattern, boolean useContentIdAsName)
  {
    this.pattern = pattern;
    this.useContentIdAsName = useContentIdAsName;
  }

  public String rewrite(final ContentEntity contentEntry, final ContentEntity dataEntity) throws URLRewriteException
  {
    try
    {
      final ArrayList<String> entityNames = new ArrayList<String>();
      entityNames.add(useContentIdAsName ? dataEntity.getContentId().toString() : dataEntity.getName());

      ContentLocation location = dataEntity.getParent();
      while (location != null)
      {
        entityNames.add(location.getName());
        location = location.getParent();
      }

      final ArrayList<String> contentNames = new ArrayList<String>();
      location = dataEntity.getRepository().getRoot();

      while (location != null)
      {
        contentNames.add(location.getName());
        location = location.getParent();
      }

      // now remove all path elements that are equal ..
      while ((contentNames.isEmpty() == false) && (entityNames.isEmpty() == false))
      {
        final String lastEntity = (String) entityNames.get(entityNames.size() - 1);
        final String lastContent = (String) contentNames.get(contentNames.size() - 1);
        if (lastContent.equals(lastEntity) == false)
        {
          break;
        }
        entityNames.remove(entityNames.size() - 1);
        contentNames.remove(contentNames.size() - 1);
      }

      final StringBuffer b = new StringBuffer();
      for (int i = entityNames.size() - 1; i >= 0; i--)
      {
        final String name = (String) entityNames.get(i);
        b.append(name);
        if (i != 0)
        {
          b.append("/");//$NON-NLS-1$
        }
      }

      if (pattern == null)
      {
        return b.toString();
      }

      String returnValue = MessageFormat.format(pattern, new Object[] { b.toString() });
      return returnValue;
    }
    catch (ContentIOException cioe)
    {
      throw new URLRewriteException();
    }

  }
}
