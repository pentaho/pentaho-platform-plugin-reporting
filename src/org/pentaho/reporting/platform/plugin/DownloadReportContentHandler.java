package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.OutputStream;
import javax.servlet.http.HttpServletResponse;

import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 15:57:44
 *
 * @author Thomas Morgner.
 */
public class DownloadReportContentHandler
{
  private IPentahoSession userSession;
  private IParameterProvider pathProvider;

  public DownloadReportContentHandler(final IPentahoSession userSession,
                                      final IParameterProvider pathProvider)
  {
    if (userSession == null)
    {
      throw new NullPointerException();
    }
    if (pathProvider == null)
    {
      throw new NullPointerException();
    }
    this.userSession = userSession;
    this.pathProvider = pathProvider;
  }

  public void createDownloadContent(final OutputStream outputStream,
                                     final String reportDefinitionPath) throws IOException
  {
    final ISolutionRepository repository = PentahoSystem.get(ISolutionRepository.class, userSession);
    final ISolutionFile file = repository.getSolutionFile(reportDefinitionPath, ISolutionRepository.ACTION_CREATE);
    final HttpServletResponse response = (HttpServletResponse) pathProvider.getParameter("httpresponse"); //$NON-NLS-1$ //$NON-NLS-2$

    // if the user has PERM_CREATE, we'll allow them to pull it for now, this is as relaxed
    // as I am comfortable with but I can imagine a PERM_READ or PERM_EXECUTE being used
    // in the future
    if (file.isDirectory() == false && file.isRoot() == false && repository.hasAccess(file, ISolutionRepository.ACTION_CREATE)
        || repository.hasAccess(file, ISolutionRepository.ACTION_UPDATE)) {
      final byte[] data = file.getData();
      if (data == null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } else {
        response.setHeader("Content-Disposition", "attach; filename=\"" + file.getFileName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        response.setHeader("Content-Description", file.getFileName()); //$NON-NLS-1$
        response.setDateHeader("Last-Modified", file.getLastModified()); //$NON-NLS-1$
        response.setContentLength(data.length); //$NON-NLS-1$
        response.setHeader("Cache-Control", "private, max-age=0, must-revalidate");
        outputStream.write(data);
      }
    } else {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
  }

}
