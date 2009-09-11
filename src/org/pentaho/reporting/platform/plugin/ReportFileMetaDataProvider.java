package org.pentaho.reporting.platform.plugin;

import java.io.InputStream;

import org.pentaho.platform.api.engine.IFileInfo;
import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.engine.SolutionFileMetaAdapter;
import org.pentaho.platform.engine.core.solution.FileInfo;

public class ReportFileMetaDataProvider extends SolutionFileMetaAdapter
{

  public ReportFileMetaDataProvider()
  {
  }

  public void setLogger(ILogger logger)
  {
  }

  public IFileInfo getFileInfo(ISolutionFile solutionFile, InputStream in)
  {
    IFileInfo fileInfo = null;
    try
    {
      fileInfo = new FileInfo();
      String title = solutionFile.getFileName();
      if (solutionFile.getFileName().endsWith(".prpt")) { //$NON-NLS-1$
        title = title.substring(0,title.indexOf(".prpt")); //$NON-NLS-1$
      }
      fileInfo.setTitle(title);
      fileInfo.setAuthor(""); //$NON-NLS-1$
      fileInfo.setDescription(solutionFile.getFullPath());
    } catch (Exception e)
    {
      fileInfo = null;
    }
    return fileInfo;
  }

}
