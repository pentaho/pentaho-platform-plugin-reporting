/*
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
 * Copyright (c) 2005-2011 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.pentaho.platform.api.engine.IContentGenerator;
import org.pentaho.platform.api.engine.IOutputHandler;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.engine.core.output.SimpleOutputHandler;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.util.TypedTableModel;
import org.pentaho.reporting.engine.classic.extensions.datasources.cda.CdaQueryBackend;
import org.pentaho.reporting.engine.classic.extensions.datasources.cda.CdaResponseParser;

/**
 * Class that implements CDA to be used LOCAL inside pentaho platform
 *
 * @author dduque
 */

public class CdaPluginLocalQueryBackend extends CdaQueryBackend
{
  public CdaPluginLocalQueryBackend()
  {
  }

  public TypedTableModel fetchData(final DataRow dataRow, final String method,
                                   final Map<String, String> extraParameter)
      throws ReportDataFactoryException
  {
    {
      try
      {
        final Map<String, Object> parameters = new HashMap<String, Object>();

        final Set<Entry<String, String>> paramterSet = extraParameter.entrySet();
        for (final Entry<String, String> entry : paramterSet)
        {
          parameters.put(entry.getKey(), entry.getValue());
        }

        parameters.put("outputType", "xml");
        parameters.put("solution", encodeParameter(getSolution()));
        parameters.put("path", encodeParameter(getPath()));
        parameters.put("file", encodeParameter(getFile()));

        final String responseBody = callPlugin("cda", method, new SimpleParameterProvider(parameters));


        // convert String into InputStream
        final InputStream responseBodyIs = new ByteArrayInputStream(responseBody.getBytes("UTF-8"));

        return CdaResponseParser.performParse(responseBodyIs);
      }
      catch (UnsupportedEncodingException use)
      {
        throw new ReportDataFactoryException("Failed to encode parameter", use);
      }
      catch (Exception e)
      {
        throw new ReportDataFactoryException("Failed to send request", e);
      }
    }
  }

  private static String callPlugin(final String pluginName,
                                  final String method,
                                  final IParameterProvider params) throws ReportDataFactoryException
  {

    final IPentahoSession userSession = PentahoSessionHolder.getSession();
    final IPluginManager pluginManager = PentahoSystem.get(IPluginManager.class, userSession);
    final IContentGenerator contentGenerator;
    try
    {
      contentGenerator = pluginManager.getContentGeneratorForType(pluginName, userSession);

      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      final Map<String, Object> pathMap = new HashMap<String, Object>();
      pathMap.put("path", "/" + method);
      final IParameterProvider pathParams = new SimpleParameterProvider(pathMap);
      final Map<String, IParameterProvider> paramProvider = new HashMap<String, IParameterProvider>();
      paramProvider.put(IParameterProvider.SCOPE_REQUEST, params);
      paramProvider.put("path", pathParams);


      final IOutputHandler outputHandler = new SimpleOutputHandler(outputStream, false);

      contentGenerator.setSession(userSession);
      contentGenerator.setOutputHandler(outputHandler);
      contentGenerator.setParameterProviders(paramProvider);
      contentGenerator.createContent();
      return outputStream.toString();
    }
    catch (Exception e)
    {
      throw new ReportDataFactoryException
          ("Failed to acquire " + pluginName + " plugin: ", e);
    }
  }


}

