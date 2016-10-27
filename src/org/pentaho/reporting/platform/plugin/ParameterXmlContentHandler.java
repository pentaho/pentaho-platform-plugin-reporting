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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.ReportElement;
import org.pentaho.reporting.engine.classic.core.Section;
import org.pentaho.reporting.engine.classic.core.designtime.datafactory.DesignTimeDataFactoryContext;
import org.pentaho.reporting.engine.classic.core.function.Expression;
import org.pentaho.reporting.engine.classic.core.function.FormulaExpression;
import org.pentaho.reporting.engine.classic.core.metadata.ExpressionMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ExpressionPropertyMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ExpressionRegistry;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.parameters.AbstractParameter;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContextWrapper;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterValues;
import org.pentaho.reporting.engine.classic.core.parameters.PlainParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.parameters.StaticListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationMessage;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;
import org.pentaho.reporting.engine.classic.core.style.ElementStyleKeys;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.engine.classic.core.util.beans.ConverterRegistry;
import org.pentaho.reporting.engine.classic.core.util.beans.ValueConverter;
import org.pentaho.reporting.engine.classic.extensions.drilldown.DrillDownProfile;
import org.pentaho.reporting.engine.classic.extensions.drilldown.DrillDownProfileMetaData;
import org.pentaho.reporting.libraries.base.util.HashNMap;
import org.pentaho.reporting.libraries.base.util.NullOutputStream;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.libraries.formula.DefaultFormulaContext;
import org.pentaho.reporting.libraries.formula.lvalues.DataTable;
import org.pentaho.reporting.libraries.formula.lvalues.FormulaFunction;
import org.pentaho.reporting.libraries.formula.lvalues.LValue;
import org.pentaho.reporting.libraries.formula.lvalues.StaticValue;
import org.pentaho.reporting.libraries.formula.parser.FormulaParser;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.reporting.platform.plugin.output.FastExportReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandlerFactory;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class ParameterXmlContentHandler {
  public static final String SYS_PARAM_ACCEPTED_PAGE = "accepted-page";
  public static final String SYS_PARAM_RENDER_MODE = "renderMode";
  public static final String SYS_PARAM_CONTENT_LINK = "::cl";
  public static final String SYS_PARAM_SESSION_ID = "::session";
  public static final String SYS_SERVER_NAMESPACE =
    "http://reporting.pentaho.org/namespaces/engine/parameter-attributes/server";
  private static final Log logger = LogFactory.getLog( ParameterXmlContentHandler.class );
  private static final String SYS_PARAM_OUTPUT_TARGET = SimpleReportingComponent.OUTPUT_TARGET;
  private static final String SYS_PARAM_DESTINATION = "destination";
  private static final String GROUP_SYSTEM = "system";
  private static final String GROUP_PARAMETERS = "parameters";
  private static final String SYS_PARAM_TAB_NAME = "::TabName";
  private static final String SYS_PARAM_TAB_ACTIVE = "::TabActive";
  private static final String SYS_IGNORE_PARAM = "::org.pentaho.reporting";
  private static final String SYS_PARAM_HTML_PROPORTIONAL_WIDTH = "htmlProportionalWidth";
  private static final String CONFIG_PARAM_HTML_PROPORTIONAL_WIDTH =
    "org.pentaho.reporting.engine.classic.core.modules.output.table.html.ProportionalColumnWidths";
  private static final String SYS_PARAM_IS_QUERY_CONTROL_ENABLED = "query-limit-ui-enabled";
  private static final String SYS_PARAM_REPORT_QUERY_LIMIT = "report-query-limit";
  static final String SYS_PARAM_QUERY_LIMIT = "query-limit";
  private static final String SYS_PARAM_MAX_QUERY_LIMIT = "maximum-query-limit";
  // default visibility for testing purposes
  Document document;
  private Map<String, ParameterDefinitionEntry> systemParameter;
  private boolean paginate;
  private IParameterProvider requestParameters;
  private Map<String, Object> inputs;

  public ParameterXmlContentHandler( final ParameterContentGenerator contentGenerator, final boolean paginate ) {
    this.paginate = paginate;
    this.inputs = contentGenerator.createInputs();
    this.requestParameters = contentGenerator.getRequestParameters();
  }

  public static String convertParameterValueToString( final ParameterDefinitionEntry parameter,
                                                      final ParameterContext context, final Object value,
                                                      final Class<?> type ) throws BeanException {
    if ( value == null ) {
      return null;
    }

    // PIR-652
    if ( value instanceof Object[] ) {
      Object[] o = (Object[]) value;
      if ( o.length == 1 ) {
        return String.valueOf( o[ 0 ] );
      }
    }

    // PIR-724 - Convert String arrays to a single string with values separated by '|'
    if ( value instanceof String[] ) {
      String[] o = (String[]) value;
      return org.apache.commons.lang.StringUtils.join( o, '|' );
    }

    final ValueConverter valueConverter = ConverterRegistry.getInstance().getValueConverter( type );
    if ( valueConverter == null ) {
      return String.valueOf( value );
    }
    if ( Date.class.isAssignableFrom( type ) ) {
      if ( value instanceof Date == false ) {
        throw new BeanException( Messages.getInstance().getString( "ReportPlugin.errorNonDateParameterValue" ) );
      }

      final String timezone =
        parameter.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
          ParameterAttributeNames.Core.TIMEZONE, context );
      final DateFormat dateFormat;
      if ( timezone == null || "server".equals( timezone ) || //$NON-NLS-1$
        "client".equals( timezone ) ) { //$NON-NLS-1$
        // nothing needed ..
        // for server: Just print it as it is, including the server timezone.
        dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ); //$NON-NLS-1$
      } else {
        // for convinience for the clients we send the date in the correct timezone.
        final TimeZone timeZoneObject;
        if ( "utc".equals( timezone ) ) { //$NON-NLS-1$
          timeZoneObject = TimeZone.getTimeZone( "UTC" ); //$NON-NLS-1$
        } else {
          timeZoneObject = TimeZone.getTimeZone( timezone );
        }
        dateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ); //$NON-NLS-1$
        dateFormat.setTimeZone( timeZoneObject );
      }
      final Date d = (Date) value;
      return dateFormat.format( d );
    }
    if ( Number.class.isAssignableFrom( type ) ) {
      final ValueConverter numConverter = ConverterRegistry.getInstance().getValueConverter( BigDecimal.class );
      return numConverter.toAttributeValue( new BigDecimal( String.valueOf( value ) ) );
    }
    return valueConverter.toAttributeValue( value );
  }

  private static void appendPageCount( final SimpleReportingComponent reportComponent, final Element parameters )
    throws Exception {
    reportComponent.setOutputStream( new NullOutputStream() );

    // so that we don't actually produce anything, we'll accept no pages in this mode
    final int acceptedPage = reportComponent.getAcceptedPage();
    reportComponent.setAcceptedPage( -1 );

    // we can ONLY get the # of pages by asking the report to run
    if ( reportComponent.validate() ) {
      if ( !reportComponent.outputSupportsPagination() ) {
        return;
      }
      final int totalPageCount = reportComponent.paginate();
      parameters.setAttribute( SimpleReportingComponent.PAGINATE_OUTPUT, "true" ); //$NON-NLS-1$
      parameters.setAttribute( "page-count", String.valueOf( totalPageCount ) ); //$NON-NLS-1$ //$NON-NLS-2$
      // use the saved value (we changed it to -1 for performance)
      parameters.setAttribute( SimpleReportingComponent.ACCEPTED_PAGE, String.valueOf( acceptedPage ) ); //$NON-NLS-1$
    }
  }

  private IParameterProvider getRequestParameters() {
    return requestParameters;
  }

  private Map<String, ParameterDefinitionEntry> getSystemParameter() {
    if ( systemParameter == null ) {
      final Map<String, ParameterDefinitionEntry> parameter = new LinkedHashMap<String, ParameterDefinitionEntry>();
      parameter.put( SYS_PARAM_OUTPUT_TARGET, createOutputParameter() );
      parameter.put( SYS_PARAM_CONTENT_LINK, createContentLinkingParameter() ); // NON-NLS
      parameter.put( SYS_PARAM_TAB_NAME, createGenericSystemParameter( SYS_PARAM_TAB_NAME, false, true ) ); // NON-NLS
      parameter.put( SYS_PARAM_TAB_ACTIVE,
        createGenericBooleanSystemParameter( SYS_PARAM_TAB_ACTIVE, false, true ) ); // NON-NLS
      // parameter.put("solution", createGenericSystemParameter("solution", false, false)); // NON-NLS
      parameter.put( "yield-rate", createGenericIntSystemParameter( "yield-rate", false, false ) ); // NON-NLS
      parameter.put( SYS_PARAM_ACCEPTED_PAGE,
        createGenericIntSystemParameter( SYS_PARAM_ACCEPTED_PAGE, false, false ) ); // NON-NLS
      parameter.put( SYS_PARAM_SESSION_ID,
        createGenericSystemParameter( SYS_PARAM_SESSION_ID, false, false ) ); // NON-NLS
      parameter.put( "path", createGenericSystemParameter( "path", false, false ) ); // NON-NLS
      // parameter.put("name", createGenericSystemParameter("name", false, false)); // NON-NLS
      // parameter.put("action", createGenericSystemParameter("action", true, false)); // NON-NLS
      parameter.put( "id", createGenericSystemParameter( "id", false, false ) ); // NON-NLS
      parameter.put( "output-type", createGenericSystemParameter( "output-type", true, false ) ); // NON-NLS
      parameter.put( "layout", createGenericSystemParameter( "layout", true, false ) ); // NON-NLS
      parameter.put( "content-handler-pattern",
        createGenericSystemParameter( "content-handler-pattern", true, false ) ); // NON-NLS
      parameter.put( "autoSubmit", createGenericBooleanSystemParameter( "autoSubmit", true, true ) ); // NON-NLS
      parameter.put( "autoSubmitUI", createGenericBooleanSystemParameter( "autoSubmitUI", true, true ) ); // NON-NLS
      parameter.put( "dashboard-mode",
        createGenericBooleanSystemParameter( "dashboard-mode", false, true ) ); // NON-NLS
      parameter.put( "showParameters", createGenericBooleanSystemParameter( "showParameters", true, true ) ); // NON-NLS
      parameter.put( "paginate", createGenericBooleanSystemParameter( "paginate", true, false ) ); // NON-NLS
      parameter.put( "ignoreDefaultDates",
        createGenericBooleanSystemParameter( "ignoreDefaultDates", true, false ) ); // NON-NLS
      parameter.put( "print", createGenericBooleanSystemParameter( "print", false, false ) ); // NON-NLS
      parameter.put( "printer-name", createGenericSystemParameter( "printer-name", false, false ) ); // NON-NLS
      parameter.put( SYS_PARAM_RENDER_MODE, createRenderModeSystemParameter() ); // NON-NLS
      parameter.put( SYS_PARAM_HTML_PROPORTIONAL_WIDTH, createGenericBooleanSystemParameter(
        SYS_PARAM_HTML_PROPORTIONAL_WIDTH, false, true ) );
      parameter.put( SYS_PARAM_IS_QUERY_CONTROL_ENABLED, createGenericBooleanSystemParameter( SYS_PARAM_IS_QUERY_CONTROL_ENABLED, false, false ) );
      parameter.put( SYS_PARAM_QUERY_LIMIT, createGenericIntSystemParameter( SYS_PARAM_QUERY_LIMIT, false, false ) );
      parameter.put( SYS_PARAM_MAX_QUERY_LIMIT, createGenericIntSystemParameter( SYS_PARAM_MAX_QUERY_LIMIT, false, false ) );

      systemParameter = Collections.unmodifiableMap( parameter );
    }

    return systemParameter;
  }

  /**
   * Defines whether parameter with display-type "datepicker" that have no default value set shall default to "today".
   * This setting generates a default value for the parameter UI, but has no effect otherwise. It is flawed from the
   * very beginning and should not be used.
   *
   * @return whether we generate default dates.
   */
  private boolean isGenerateDefaultDates() {
    final Object value = inputs.get( "ignoreDefaultDates" ); // NON-NLS
    if ( value == null ) {
      // we do not generate default dates until it is explicitly requested.
      // if the users want default values for parameters then let them define those in the parameter
      return false;
    }

    return "true".equals( value );
  }

  public void createParameterContent( final OutputStream outputStream, final Serializable fileId, final String path,
                                      boolean overrideOutputType, MasterReport report ) throws Exception {
    final Object rawSessionId = inputs.get( ParameterXmlContentHandler.SYS_PARAM_SESSION_ID );
    if ( ( rawSessionId instanceof String ) == false || "".equals( rawSessionId ) ) {
      inputs.put( ParameterXmlContentHandler.SYS_PARAM_SESSION_ID, UUIDUtil.getUUIDAsString() );
    }

    this.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final IParameterProvider requestParams = getRequestParameters();

    final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    reportComponent.setReportFileId( fileId );
    if ( report != null ) {
      reportComponent.setReport( report );
    }
    reportComponent.setPaginateOutput( true );

    final boolean isMobile =
      "true".equals( requestParams.getStringParameter( "mobile", "false" ) ); //$NON-NLS-1$ //$NON-NLS-2$

    if ( isMobile ) {
      overrideOutputType = true;
      reportComponent.setForceDefaultOutputTarget( true );
    } else {
      reportComponent.setForceDefaultOutputTarget( overrideOutputType );
    }
    reportComponent.setDefaultOutputTarget( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE );

    if ( path.endsWith( ".prpti" ) && requestParams.getParameter( "json" ) == null ) {
      reportComponent.setForceUnlockPreferredOutput( true );
    }

    reportComponent.setInputs( inputs );

    report = reportComponent.getReport();

    final DefaultParameterContext parameterContext = new DefaultParameterContext( report );
    final ValidationResult vr;
    final Element parameters;
    try {
      // apply inputs to parameters
      final ValidationResult validationResult =
        ReportContentUtil.applyInputsToReportParameters( report, parameterContext, inputs, new ValidationResult() );

      final ReportParameterDefinition reportParameterDefinition = report.getParameterDefinition();
      vr =
        reportParameterDefinition.getValidator().validate( validationResult, reportParameterDefinition,
          parameterContext );

      // determine dependent parameters
      HashNMap<String, String> dependencies = null;
      try {
        dependencies = getDependentParameters( vr.getParameterValues(), parameterContext, report );
      } catch ( ReportDataFactoryException e ) {
        logger.debug( "unable to determine dependent parameters", e );
      }

      parameters = document.createElement( GROUP_PARAMETERS ); //$NON-NLS-1$
      parameters
        .setAttribute( "is-prompt-needed", String.valueOf( vr.isEmpty() == false ) ); //$NON-NLS-1$ //$NON-NLS-2$
      parameters.setAttribute( "ignore-biserver-5538", "true" );

      // check if pagination is allowed and turned on

      final Boolean autoSubmitFlag =
        requestFlag( "autoSubmit", report, AttributeNames.Core.NAMESPACE, AttributeNames.Core.AUTO_SUBMIT_PARAMETER,
          "org.pentaho.reporting.engine.classic.core.ParameterAutoSubmit" );
      if ( Boolean.TRUE.equals( autoSubmitFlag ) ) {
        parameters.setAttribute( "autoSubmit", "true" );
      } else if ( Boolean.FALSE.equals( autoSubmitFlag ) ) {
        parameters.setAttribute( "autoSubmit", "false" );
      }

      final Boolean autoSubmitUiFlag =
        requestFlag( "autoSubmitUI",
          report, // NON-NLS
          AttributeNames.Core.NAMESPACE, AttributeNames.Core.AUTO_SUBMIT_DEFAULT,
          "org.pentaho.reporting.engine.classic.core.ParameterAutoSubmitUI" );
      if ( Boolean.FALSE.equals( autoSubmitUiFlag ) ) {
        parameters.setAttribute( "autoSubmitUI", "false" ); // NON-NLS
      } else {
        parameters.setAttribute( "autoSubmitUI", "true" ); // NON-NLS
      }

      parameters.setAttribute( "layout", requestConfiguration( "layout",
        report, // NON-NLS
        AttributeNames.Core.NAMESPACE, AttributeNames.Core.PARAMETER_UI_LAYOUT,
        "org.pentaho.reporting.engine.classic.core.ParameterUiLayout" ) );

      final ParameterDefinitionEntry[] parameterDefinitions = reportParameterDefinition.getParameterDefinitions();
      // Collect all parameter, but allow user-parameter to override system parameter.
      // It is the user's problem if the types do not match and weird errors occur, but
      // there are sensible usecases where this should be allowed.
      // System parameter must come last in the list, as this is how it was done in the original
      // version and this is how people expect it to be now.
      final LinkedHashMap<String, ParameterDefinitionEntry> reportParameters =
        new LinkedHashMap<String, ParameterDefinitionEntry>();
      for ( final ParameterDefinitionEntry parameter : parameterDefinitions ) {
        reportParameters.put( parameter.getName(), parameter );
      }
      for ( final Map.Entry<String, ParameterDefinitionEntry> entry : getSystemParameter().entrySet() ) {
        if ( reportParameters.containsKey( entry.getKey() ) == false ) {
          reportParameters.put( entry.getKey(), entry.getValue() );
        }
      }

      if ( overrideOutputType ) {
        final ParameterDefinitionEntry definitionEntry = reportParameters.get( SimpleReportingComponent.OUTPUT_TARGET );
        if ( definitionEntry instanceof AbstractParameter ) {
          final AbstractParameter parameter = (AbstractParameter) definitionEntry;
          parameter.setHidden( true );
          parameter.setMandatory( false );
        }
      } else {
        hideOutputParameterIfLocked( report, reportParameters );
      }

      final Map<String, Object> inputs =
        computeRealInput( parameterContext, reportParameters, reportComponent.getComputedOutputTarget(), vr );

      final Boolean showParameterUI = requestFlag( "showParameters", report, // NON-NLS
        AttributeNames.Core.NAMESPACE, AttributeNames.Core.SHOW_PARAMETER_UI, null );
      if ( Boolean.FALSE.equals( showParameterUI ) ) {
        inputs.put( "showParameters", Boolean.FALSE ); // NON-NLS
      } else {
        inputs.put( "showParameters", Boolean.TRUE ); // NON-NLS
      }

      // Adding proportional width config parameter
      String proportionalWidth =
        report.getReportConfiguration().getConfigProperty( CONFIG_PARAM_HTML_PROPORTIONAL_WIDTH );
      inputs.put( SYS_PARAM_HTML_PROPORTIONAL_WIDTH, Boolean.valueOf( proportionalWidth ) );

      // Adding row limit config parameters
      Boolean isQueryLimitControlEnabled = false;
      Integer maxQueryLimit = null;
      final IPluginManager pm = PentahoSystem.get( IPluginManager.class );
      if ( pm != null ) {
        isQueryLimitControlEnabled = Boolean.parseBoolean(
          (String) pm.getPluginSetting( "reporting", "settings/query-limit-ui-enabled", "false" ) );
        maxQueryLimit = isQueryLimitControlEnabled ? NumberUtils.toInt( (String) pm.getPluginSetting( "reporting", "settings/query-limit", "0" ), 0 ) : 0;
      }
      inputs.put( SYS_PARAM_IS_QUERY_CONTROL_ENABLED, isQueryLimitControlEnabled );
      inputs.put( SYS_PARAM_REPORT_QUERY_LIMIT, report.getQueryLimit() );
      inputs.put( SYS_PARAM_QUERY_LIMIT, report.getQueryLimit() );
      inputs.put( SYS_PARAM_MAX_QUERY_LIMIT, maxQueryLimit );


      //get changed parameters from request
      final String[] changedParamsQuery = requestParams.getStringArrayParameter( "changedParameters", null );

      appendParametersList( parameterContext, vr, parameters, dependencies, reportParameters, inputs,
        changedParamsQuery );

      if ( vr.isEmpty() == false ) {
        parameters.appendChild( createErrorElements( vr ) );
      }

      final String[] outputParameter = new OutputParameterCollector().collectParameter( report );
      for ( int i = 0; i < outputParameter.length; i++ ) {
        final String outputParameterName = outputParameter[ i ];
        // <output-parameter displayName="Territory" id="[Markets].[Territory]"/>
        final Element element = document.createElement( "output-parameter" ); // NON-NLS
        element.setAttribute( "displayName", outputParameterName ); // NON-NLS
        element.setAttribute( "id", outputParameterName ); // NON-NLS
        parameters.appendChild( element );
      }

      if ( vr.isEmpty() && paginate
        && reportComponent.getComputedOutputTarget()
        .equals( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE ) ) { //$NON-NLS-1$ //$NON-NLS-2$

        appendPageCount( reportComponent, parameters );
      }
      document.appendChild( parameters );

      final DOMSource source = new DOMSource( document );
      final StreamResult result = new StreamResult( outputStream );
      final Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.transform( source, result );
      // close parameter context
    } finally {
      parameterContext.close();
    }
  }

  protected void appendParametersList( final DefaultParameterContext parameterContext,
                                    final ValidationResult vr,
                                    final Element parameters,
                                    final HashNMap<String, String> dependencies,
                                    final LinkedHashMap<String, ParameterDefinitionEntry> reportParameters,
                                    final Map<String, Object> inputs,
                                    final String[] changedParamsQuery )
    throws CloneNotSupportedException, BeanException, ReportDataFactoryException {
    if ( changedParamsQuery != null && changedParamsQuery.length > 0 ) {
      final Set<Object> changedParameters = new HashSet<>( Arrays.asList( changedParamsQuery ) );

      //Changed parameters and their dependencies
      final Set<Object> changedWithDependencies = addAllDependencies( dependencies, changedParameters );

      //Filter definitions
      final List<ParameterDefinitionEntry> changedParamDefinitions =
        reportParameters.values().stream().filter( p -> changedWithDependencies.contains( p.getName() ) ).collect( Collectors

          .toList() );

      //Filtered list without attributes
      renderParameters( parameterContext, vr, parameters, new HashNMap<>(), changedParamDefinitions, inputs, Boolean.TRUE );
      parameters.setAttribute( "minimized", "true" );
    } else {
      //Initial parameter call or not async mode
      renderParameters( parameterContext, vr, parameters, dependencies, reportParameters.values(), inputs, Boolean.FALSE );
    }
  }

  private void renderParameters( final DefaultParameterContext parameterContext,
                                 final ValidationResult vr,
                                 final Element parameters,
                                 final HashNMap<String, String> dependencies,
                                 final Collection<ParameterDefinitionEntry> reportParameters,
                                 final Map<String, Object> inputs,
                                 final boolean  ignoreAttributes ) throws BeanException, ReportDataFactoryException {
    for ( final ParameterDefinitionEntry parameter : reportParameters ) {
      final Object selections = inputs.get( parameter.getName() );
      final ParameterContextWrapper wrapper =
        new ParameterContextWrapper( parameterContext, vr.getParameterValues() );
      final Element el = createParameterElement( parameter, wrapper, selections, dependencies, ignoreAttributes  );
      createParameterDependencies( el, parameter, dependencies );
      parameters.appendChild( el );
    }
  }

  HashNMap<String, String> getDependentParameters( ReportParameterValues computedParameterValues,
                                                   ParameterContext parameterContext, MasterReport report )
    throws ReportDataFactoryException {
    HashNMap<String, String> downstreamParams = new HashNMap<>();

    final ReportParameterDefinition reportParameterDefinition = report.getParameterDefinition();

    DesignTimeDataFactoryContext factoryContext = new DesignTimeDataFactoryContext( report );

    for ( ParameterDefinitionEntry entry : reportParameterDefinition.getParameterDefinitions() ) {
      // default list parameter is only dynamic values provider now.
      if ( entry instanceof DefaultListParameter ) {

        DefaultListParameter listParameter = (DefaultListParameter) entry;

        // inspect for dependent fields
        final String queryName = listParameter.getQueryName();
        if ( queryName != null ) {
          CompoundDataFactory cdf = CompoundDataFactory.normalize( report.getDataFactory() );
          final DataFactory dataFactoryForQuery = cdf.getDataFactoryForQuery( queryName );

          if ( dataFactoryForQuery != null ) {
            dataFactoryForQuery.initialize( factoryContext );

            String[] fields = dataFactoryForQuery.getMetaData()
              .getReferencedFields( dataFactoryForQuery, queryName, computedParameterValues );

            if ( fields != null ) {
              for ( String field : fields ) {
                if ( !field.startsWith( SYS_IGNORE_PARAM ) ) {
                  downstreamParams.add( field, entry.getName() );
                }
              }
            }
          }
        }

      }
      // inspect post proc formulas, def values, etc.
      computeNormalLineage( parameterContext, entry, downstreamParams );
    }

    return downstreamParams;
  }

  private Set<Object> addAllDependencies( final HashNMap<String, String> dependencies, final Set<Object> changedNames )
    throws CloneNotSupportedException {

    if ( dependencies != null && !dependencies.isEmpty() ) {
      final Set<Object> destination = new HashSet<>( changedNames );
      final Iterator<Object> changedIterator = changedNames.iterator();
      while ( changedIterator.hasNext() ) {
        final String nextName = String.valueOf( changedIterator.next() );
        fillParams( destination, dependencies, nextName );
      }
      return destination;
    }

    return changedNames;
  }

  private void fillParams( final Set<Object> destination, final HashNMap<String, String> dependencies,
                           final String paramName ) {
    final Iterator<String> all = dependencies.getAll( paramName );
    if ( all != null && all.hasNext() ) {
      while ( all.hasNext() ) {
        final String nextName = all.next();
        destination.add( nextName );
        fillParams( destination, dependencies, nextName );
      }
    }
  }

  void computeNormalLineage( ParameterContext pc, ParameterDefinitionEntry pe, HashNMap<String, String> m ) {
    String name = pe.getName();
    String defValue =
      pe.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
        ParameterAttributeNames.Core.DEFAULT_VALUE_FORMULA, pc );
    String postProc =
      pe.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
        ParameterAttributeNames.Core.POST_PROCESSOR_FORMULA, pc );
    final String formula =
      pe.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
        ParameterAttributeNames.Core.DISPLAY_VALUE_FORMULA, pc );

    for ( String field : analyzeFormula( defValue ) ) {
      m.add( field, name );
    }
    for ( String field : analyzeFormula( formula ) ) {
      m.add( field, name );
    }
    for ( String field : analyzeFormula( postProc ) ) {
      m.add( field, name );
    }
  }

  private String[] analyzeFormula( String formula ) {
    if ( formula == null ) {
      return new String[ 0 ];
    }
    FormulaExpression fe = new FormulaExpression();
    fe.setFormula( formula );

    final ExpressionMetaData md =
      ExpressionRegistry.getInstance().getExpressionMetaData( fe.getClass().getName() );

    final ExpressionPropertyMetaData pd = md.getPropertyDescription( "formula" );

    return pd.getReferencedFields( fe, fe.getFormula() );
  }

  private Map<String, Object> computeRealInput( final ParameterContext parameterContext,
                                                final LinkedHashMap<String, ParameterDefinitionEntry> reportParameters,
                                                final String computedOutputTarget,
                                                final ValidationResult result ) {
    final Map<String, Object> realInputs = new HashMap<String, Object>();
    realInputs.put( SYS_PARAM_DESTINATION, lookupDestination() );

    final ReportParameterValues parameterValues = result.getParameterValues();

    for ( final ParameterDefinitionEntry parameter : reportParameters.values() ) {
      final String parameterName = parameter.getName();
      final Object parameterFromReport = parameterValues.get( parameterName );
      if ( parameterFromReport != null ) {
        // always prefer the report parameter. The user's input has been filtered already and values
        // may have been replaced by a post-processing formula.
        //
        realInputs.put( parameterName, parameterFromReport );
        continue;
      }

      // the parameter values collection only contains declared parameter. So everything else will
      // be handled now. This is also the time to handle rejected parameter. For these parameter,
      // the calculated value for the report is <null>.
      final Object value = inputs.get( parameterName );
      if ( value == null ) {
        // have no value, so we use the default value ..
        realInputs.put( parameterName, null );
        continue;
      }

      try {
        final Object translatedValue = ReportContentUtil.computeParameterValue( parameterContext, parameter, value );
        if ( translatedValue != null ) {
          realInputs.put( parameterName, translatedValue );
        } else {
          realInputs.put( parameterName, null );
        }
      } catch ( Exception be ) {
        if ( logger.isDebugEnabled() ) {
          logger.debug( Messages.getInstance().getString( "ReportPlugin.debugParameterCannotBeConverted",
            parameter.getName(), String.valueOf( value ) ), be );
        }
      }
    }

    // thou cannot override the output target with invalid values ..
    realInputs.put( SYS_PARAM_OUTPUT_TARGET, computedOutputTarget );
    return realInputs;
  }

  private void hideOutputParameterIfLocked( final MasterReport report,
                                            final Map<String, ParameterDefinitionEntry> reportParameters ) {
    final boolean lockOutputType =
      Boolean.TRUE.equals( report.getAttribute( AttributeNames.Core.NAMESPACE,
        AttributeNames.Core.LOCK_PREFERRED_OUTPUT_TYPE ) );
    final ParameterDefinitionEntry definitionEntry = reportParameters.get( SimpleReportingComponent.OUTPUT_TARGET );
    if ( definitionEntry instanceof AbstractParameter ) {
      final AbstractParameter parameter = (AbstractParameter) definitionEntry;
      parameter.setHidden( lockOutputType );
      parameter.setMandatory( !lockOutputType );
    }
  }

  // default visibility for testing purposes
  void createParameterDependencies( final Element element, final ParameterDefinitionEntry parameter,
                                            final HashNMap dependencies ) {
    if ( element == null || parameter == null ) {
      throw new NullPointerException();
    }
    if ( dependencies == null || dependencies.isEmpty() || !dependencies.containsKey( parameter.getName() ) ) {
      return;
    }
    final Element valuesElement = document.createElement( "dependencies" );
    Iterator it = dependencies.getAll( parameter.getName() );
    while ( it.hasNext() ) {
      Object rName = it.next();
      String dependency = String.valueOf( rName );
      Element name = document.createElement( "name" );
      name.setTextContent( dependency );
      valuesElement.appendChild( name );
    }

    element.appendChild( valuesElement );
  }

  Element createParameterElement( final ParameterDefinitionEntry parameter,
                                  final ParameterContext parameterContext, final Object selections,
                                  final HashNMap<String, String> dependencies,
                                  final boolean ignoreAttributes )
    throws BeanException,
    ReportDataFactoryException {
    try {
      final Element parameterElement = document.createElement( "parameter" ); //$NON-NLS-1$
      parameterElement.setAttribute( "name", parameter.getName() ); //$NON-NLS-1$
      final Class<?> valueType = parameter.getValueType();
      parameterElement.setAttribute( "type", valueType.getName() ); //$NON-NLS-1$
      parameterElement
        .setAttribute( "is-mandatory", String.valueOf( parameter.isMandatory() ) ); //$NON-NLS-1$ //$NON-NLS-2$

      final String[] namespaces = parameter.getParameterAttributeNamespaces();
      boolean isMustValidateOnServerSet = false;
      if ( !ignoreAttributes ) {
        for ( int i = 0; i < namespaces.length; i++ ) {
          final String namespace = namespaces[ i ];
          final String[] attributeNames = parameter.getParameterAttributeNames( namespace );
          for ( final String attributeName : attributeNames ) {
            final String attributeValue = parameter.getTranslatedParameterAttribute( namespace, attributeName, parameterContext );
            // expecting: label, parameter-render-type, parameter-layout
            // but others possible as well, so we set them all
            final Element attributeElement = document.createElement( "attribute" ); // NON-NLS
            attributeElement.setAttribute( "namespace", namespace ); // NON-NLS
            attributeElement.setAttribute( "name", attributeName ); // NON-NLS
            attributeElement.setAttribute( "value", attributeValue ); // NON-NLS

            parameterElement.appendChild( attributeElement );

            if ( isParameterServerValidated( attributeName, attributeValue, valueType ) ) {
              // must validate on server
              final Element attrElement = document.createElement( "attribute" );
              attrElement.setAttribute( "namespace", SYS_SERVER_NAMESPACE );
              attrElement.setAttribute( "name", "must-validate-on-server" );
              attrElement.setAttribute( "value", Boolean.TRUE.toString() );
              parameterElement.appendChild( attrElement );
              isMustValidateOnServerSet = true;
            }
          }
        }
      }

      //parameter dependencies: see backlog-7980
      if ( dependencies != null && !dependencies.isEmpty() && dependencies.getAll( parameter.getName() ).hasNext() && !ignoreAttributes ) {
        List<String> deps = Lists.newArrayList( dependencies.getAll( parameter.getName() ) );

        if ( !deps.isEmpty() ) {
          if ( !isMustValidateOnServerSet ) {
            // must validate on server
            final Element attributeElement = document.createElement( "attribute" );
            attributeElement.setAttribute( "namespace", SYS_SERVER_NAMESPACE );
            attributeElement.setAttribute( "name", "must-validate-on-server" );
            attributeElement.setAttribute( "value", Boolean.TRUE.toString() );
            parameterElement.appendChild( attributeElement );
          }

          // and it is also has a dependencies
          final Element oneMoreAttribute = document.createElement( "attribute" );
          oneMoreAttribute.setAttribute( "namespace", SYS_SERVER_NAMESPACE );
          oneMoreAttribute.setAttribute( "name", "has-downstream-dependent-parameter" );
          oneMoreAttribute.setAttribute( "value", Boolean.TRUE.toString() );
          parameterElement.appendChild( oneMoreAttribute );
        }
      }

      final Class<?> elementValueType;
      if ( valueType.isArray() ) {
        elementValueType = valueType.getComponentType();
      } else {
        elementValueType = valueType;
      }

      final LinkedHashSet<Object> selectionSet = new LinkedHashSet<Object>();
      if ( selections != null ) {
        if ( selections.getClass().isArray() ) {
          final int length = Array.getLength( selections );
          for ( int i = 0; i < length; i++ ) {
            final Object value = Array.get( selections, i );
            selectionSet.add( resolveSelectionValue( value ) );
          }
        } else {
          selectionSet.add( resolveSelectionValue( selections ) );
        }
      } else {
        final String type =
          parameter.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE,
            parameterContext );
        if ( ParameterAttributeNames.Core.TYPE_DATEPICKER.equals( type ) && Date.class.isAssignableFrom( valueType ) ) {
          if ( isGenerateDefaultDates() ) {
            selectionSet.add( new Date() );
          }
        }
      }
      if ( Date.class.isAssignableFrom( elementValueType ) ) {
        parameterElement
          .setAttribute( "timezone-hint",
            computeTimeZoneHint( parameter, parameterContext, selectionSet ) ); //$NON-NLS-1$
      }

      @SuppressWarnings( "rawtypes" )
      final LinkedHashSet handledValues = (LinkedHashSet) selectionSet.clone();

      if ( parameter instanceof ListParameter ) {
        final ListParameter asListParam = (ListParameter) parameter;
        parameterElement.setAttribute( "is-multi-select",
          String.valueOf( asListParam.isAllowMultiSelection() ) ); //$NON-NLS-1$ //$NON-NLS-2$
        parameterElement
          .setAttribute( "is-strict", String.valueOf( asListParam.isStrictValueCheck() ) ); //$NON-NLS-1$ //$NON-NLS-2$
        parameterElement.setAttribute( "is-list", "true" ); //$NON-NLS-1$ //$NON-NLS-2$

        final Element valuesElement = document.createElement( "values" ); //$NON-NLS-1$
        parameterElement.appendChild( valuesElement );

        final ParameterValues possibleValues = asListParam.getValues( parameterContext );
        for ( int i = 0; i < possibleValues.getRowCount(); i++ ) {
          Object key = possibleValues.getKeyValue( i );
          Object value = possibleValues.getTextValue( i );

          final Element valueElement = document.createElement( "value" ); //$NON-NLS-1$
          valuesElement.appendChild( valueElement );

          if ( hasISOControlChars( key, elementValueType ) || hasISOControlChars( value, elementValueType ) ) {
            // if either key or value have illegal chars, base64 encode them
            // and set the encoded="true" flag.
            key = Base64.encodeBase64String( key.toString().getBytes() );
            value = Base64.encodeBase64String( value.toString().getBytes() );
            valueElement.setAttribute( "encoded", "true" );
          }
          valueElement
            .setAttribute( "label", HtmlUtils.htmlEscape( String.valueOf( value ) ) ); //$NON-NLS-1$ //$NON-NLS-2$
          valueElement.setAttribute( "type", elementValueType.getName() ); //$NON-NLS-1$

          if ( key instanceof Number ) {
            final BigDecimal bd = new BigDecimal( String.valueOf( key ) );
            valueElement.setAttribute( "selected", String.valueOf( selectionSet.contains( bd ) ) ); //$NON-NLS-1$
            handledValues.remove( bd );
          } else if ( key instanceof Timestamp ) {
            Timestamp origKey = (Timestamp) possibleValues.getKeyValue( i );
            valueElement.setAttribute( "selected",
              String.valueOf( selectionSet.contains( new Date( origKey.getTime() ) ) ) ); //$NON-NLS-1$
            handledValues.remove( key );
          } else if ( key == null ) {
            if ( selections == null || selectionSet.contains( null ) ) {
              valueElement.setAttribute( "selected", "true" ); //$NON-NLS-1$
              handledValues.remove( null );
            }
          } else {
            // key may have been encoded, we want the original raw value.
            Object origKey = possibleValues.getKeyValue( i );
            valueElement.setAttribute( "selected", String.valueOf( selectionSet.contains( origKey ) ) ); //$NON-NLS-1$
            handledValues.remove( key );
          }
          if ( key == null ) {
            valueElement.setAttribute( "null", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
          } else {
            valueElement.setAttribute( "null", "false" ); //$NON-NLS-1$ //$NON-NLS-2$
            valueElement.setAttribute( "value", convertParameterValueToString( parameter, parameterContext, key,
              elementValueType ) ); //$NON-NLS-1$ //$NON-NLS-2$
          }

        }

        // Only add invalid values to the selection list for non-strict parameters
        if ( !asListParam.isStrictValueCheck() ) {
          for ( final Object key : handledValues ) {
            final Element valueElement = document.createElement( "value" ); //$NON-NLS-1$
            valuesElement.appendChild( valueElement );

            valueElement.setAttribute(
              "label", Messages.getInstance()
                .getString( "ReportPlugin.autoParameter", String.valueOf( key ) ) ); //$NON-NLS-1$ //$NON-NLS-2$
            valueElement.setAttribute( "type", elementValueType.getName() ); //$NON-NLS-1$

            if ( key instanceof Number ) {
              BigDecimal bd = new BigDecimal( String.valueOf( key ) );
              valueElement.setAttribute( "selected", String.valueOf( selectionSet.contains( bd ) ) ); //$NON-NLS-1$
            } else {
              valueElement.setAttribute( "selected", String.valueOf( selectionSet.contains( key ) ) ); //$NON-NLS-1$
            }

            if ( key == null ) {
              valueElement.setAttribute( "null", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
              valueElement.setAttribute( "null", "false" ); //$NON-NLS-1$ //$NON-NLS-2$
              valueElement.setAttribute( "value", convertParameterValueToString( parameter, parameterContext, key,
                elementValueType ) ); //$NON-NLS-1$ //$NON-NLS-2$
            }

          }
        }
      } else if ( parameter instanceof PlainParameter ) {
        // apply defaults, this is the easy case
        parameterElement.setAttribute( "is-multi-select", "false" ); //$NON-NLS-1$ //$NON-NLS-2$
        parameterElement.setAttribute( "is-strict", "false" ); //$NON-NLS-1$ //$NON-NLS-2$
        parameterElement.setAttribute( "is-list", "false" ); //$NON-NLS-1$ //$NON-NLS-2$

        if ( selections != null ) {
          final Element valuesElement = document.createElement( "values" ); //$NON-NLS-1$
          parameterElement.appendChild( valuesElement );

          final Element valueElement = document.createElement( "value" ); //$NON-NLS-1$
          valuesElement.appendChild( valueElement );
          valueElement.setAttribute( "type", valueType.getName() ); //$NON-NLS-1$
          valueElement.setAttribute( "selected", "true" ); //$NON-NLS-1$
          valueElement.setAttribute( "null", "false" ); //$NON-NLS-1$ //$NON-NLS-2$
          final String value = convertParameterValueToString( parameter, parameterContext, selections, valueType );
          valueElement.setAttribute( "value", value ); //$NON-NLS-1$ //$NON-NLS-2$
          valueElement.setAttribute( "label", HtmlUtils.htmlEscape( value ) ); //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
      return parameterElement;
    } catch ( BeanException be ) {
      logger.error( Messages.getInstance().getString( "ReportPlugin.errorFailedToGenerateParameter",
        parameter.getName(), String.valueOf( selections ) ), be );
      throw be;
    }
  }

  protected boolean isParameterServerValidated( String attributeName, String attributeValue, Class<?> valueType ) {
    // Parameters with parameter-render-type textbox or textarea and types(number, date or string) can be validated on the client.
    if ( attributeValue.equals( ParameterAttributeNames.Core.TYPE_TEXTBOX ) || attributeValue.equals( ParameterAttributeNames.Core.TYPE_MULTILINE ) ) {
      if ( !Number.class.isAssignableFrom( valueType ) && valueType != String.class && !Date.class.isAssignableFrom( valueType ) ) {
        return true;
      }
    }

    if ( ParameterAttributeNames.Core.POST_PROCESSOR_FORMULA.equals( attributeName ) ) {
      return true;
    }

    if ( ParameterAttributeNames.Core.RE_EVALUATE_ON_FAILED_VALUES.equals( attributeName ) || ParameterAttributeNames.Core.AUTOFILL_SELECTION.equals( attributeName ) ) {
      return "true".equals( attributeValue );
    }

    return false;
  }

  /**
   * Determine whether value contains any ISO control characters, which are not allowed in XML.
   * http://www.w3.org/TR/2006/REC-xml11-20060816/#charsets
   */
  private boolean hasISOControlChars( Object value, Class<?> type ) {
    if ( value == null ) {
      return false;
    }

    if ( type == String.class ) {
      String string = value.toString();
      for ( int i = 0; i < string.length(); i++ ) {
        if ( Character.isISOControl( string.charAt( i ) ) ) {
          return true;
        }
      }
      return false;
    } else if ( type == Character.class ) {
      return Character.isISOControl( ( (Character) value ) );
    }
    return false;
  }

  private Object resolveSelectionValue( Object value ) {
    // convert all numerics to BigDecimals for cross-numeric-class matching
    if ( value instanceof Number ) {
      return new BigDecimal( String.valueOf( value.toString() ) );
    } else {
      return value;
    }

  }

  private String computeTimeZoneHint( final ParameterDefinitionEntry parameter,
                                      final ParameterContext parameterContext,
                                      final LinkedHashSet<Object> selectionSet ) {
    // add a timezone hint ..
    final String timezoneSpec =
      parameter.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TIMEZONE,
        parameterContext );
    if ( "client".equals( timezoneSpec ) ) { //$NON-NLS-1$
      return ( "" );
    } else {
      final TimeZone timeZone;
      final StringBuffer value = new StringBuffer();
      if ( timezoneSpec == null || "server".equals( timezoneSpec ) ) { //$NON-NLS-1$
        timeZone = TimeZone.getDefault();
      } else if ( "utc".equals( timezoneSpec ) ) { //$NON-NLS-1$
        timeZone = TimeZone.getTimeZone( "UTC" ); //$NON-NLS-1$
      } else {
        timeZone = TimeZone.getTimeZone( timezoneSpec );
      }

      final int offset;
      if ( selectionSet != null && selectionSet.size() > 0 ) {
        Date date = (Date) selectionSet.iterator().next();
        offset = timeZone.getOffset( date.getTime() );
      } else {
        offset = timeZone.getRawOffset();
      }

      if ( offset < 0 ) {
        value.append( "-" );
      } else {
        value.append( "\\+" );
      }

      final int seconds = Math.abs( offset / 1000 );
      final int minutesRaw = seconds / 60;
      final int hours = minutesRaw / 60;
      final int minutes = minutesRaw % 60;
      if ( hours < 10 ) {
        value.append( "0" );
      }
      value.append( hours );
      if ( minutes < 10 ) {
        value.append( "0" );
      }
      value.append( minutes );
      return value.toString();
    }
  }

  // default visibility for testing purposes
  Element createErrorElements( final ValidationResult vr ) {
    final Element errors = document.createElement( "errors" ); //$NON-NLS-1$
    for ( final String property : vr.getProperties() ) {
      for ( final ValidationMessage message : vr.getErrors( property ) ) {
        final Element error = document.createElement( "error" ); //$NON-NLS-1$
        error.setAttribute( "parameter", property ); //$NON-NLS-1$
        error.setAttribute( "message", message.getMessage() ); //$NON-NLS-1$
        errors.appendChild( error );
      }
    }
    final ValidationMessage[] globalMessages = vr.getErrors();
    for ( int i = 0; i < globalMessages.length; i++ ) {
      final ValidationMessage globalMessage = globalMessages[ i ];
      final Element error = document.createElement( "global-error" ); //$NON-NLS-1$
      error.setAttribute( "message", globalMessage.getMessage() ); //$NON-NLS-1$
      errors.appendChild( error );
    }
    return errors;
  }

  private PlainParameter createGenericSystemParameter( final String parameterName, final boolean deprecated,
                                                       final boolean preferredParameter ) {
    return createGenericSystemParameter( parameterName, deprecated, preferredParameter, String.class );
  }

  private PlainParameter createGenericSystemParameter( final String parameterName, final boolean deprecated,
                                                       final boolean preferredParameter, final Class<?> type ) {
    final PlainParameter destinationParameter = new PlainParameter( parameterName, type );
    destinationParameter.setMandatory( false );
    destinationParameter.setHidden( true );
    destinationParameter.setRole( ParameterAttributeNames.Core.ROLE_SYSTEM_PARAMETER );
    destinationParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PREFERRED, String.valueOf( preferredParameter ) );
    destinationParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP, GROUP_SYSTEM );
    destinationParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP_LABEL, Messages.getInstance().getString(
        "ReportPlugin.SystemParameters" ) );
    destinationParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.LABEL, parameterName );
    destinationParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.TYPE, ParameterAttributeNames.Core.TYPE_TEXTBOX );
    destinationParameter.setDeprecated( deprecated );
    return destinationParameter;
  }

  private PlainParameter createGenericBooleanSystemParameter( final String parameterName, final boolean deprecated,
                                                              final boolean preferredParameter ) {
    return createGenericSystemParameter( parameterName, deprecated, preferredParameter, Boolean.class );
  }

  private PlainParameter createGenericIntSystemParameter( final String parameterName, final boolean deprecated,
                                                          final boolean preferredParameter ) {
    return createGenericSystemParameter( parameterName, deprecated, preferredParameter, Integer.class );
  }

  private StaticListParameter createContentLinkingParameter() {

    final StaticListParameter parameter =
      new StaticListParameter( SYS_PARAM_CONTENT_LINK, true, false, String[].class );
    parameter.setMandatory( false );
    parameter.setHidden( true );
    parameter.setRole( ParameterAttributeNames.Core.ROLE_SYSTEM_PARAMETER );
    parameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.PREFERRED,
      "false" );
    parameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP, GROUP_SYSTEM );
    parameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP_LABEL, Messages.getInstance().getString(
        "ReportPlugin.SystemParameters" ) );
    parameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.LABEL,
      Messages.getInstance().getString( "ReportPlugin.ContentLinking" ) );
    parameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE,
      ParameterAttributeNames.Core.TYPE_LIST );
    return parameter;
  }

  private Object lookupDestination() {
    return inputs.get( SYS_PARAM_DESTINATION );
  }

  private StaticListParameter createOutputParameter() {

    final StaticListParameter listParameter =
      new StaticListParameter( SYS_PARAM_OUTPUT_TARGET, false, true, String.class );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PREFERRED, String.valueOf( true ) );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP, GROUP_PARAMETERS );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP_LABEL, Messages.getInstance().getString(
        "ReportPlugin.ReportParameters" ) );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.LABEL,
      Messages.getInstance().getString( "ReportPlugin.OutputType" ) );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE,
      ParameterAttributeNames.Core.TYPE_DROPDOWN );
    listParameter.setRole( ParameterAttributeNames.Core.ROLE_SYSTEM_PARAMETER );

    ReportOutputHandlerFactory handlerFactory = PentahoSystem.get( ReportOutputHandlerFactory.class );
    if ( handlerFactory == null ) {
      handlerFactory = new FastExportReportOutputHandlerFactory();
    }

    for ( Map.Entry<String, String> entry : handlerFactory.getSupportedOutputTypes() ) {
      listParameter.addValues( entry.getKey(), entry.getValue() );
    }

    return listParameter;
  }

  private ParameterDefinitionEntry createRenderModeSystemParameter() {
    final StaticListParameter listParameter =
      new StaticListParameter( SYS_PARAM_RENDER_MODE, false, true, String.class );
    listParameter.setHidden( true );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PREFERRED, String.valueOf( false ) );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP, GROUP_SYSTEM );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.PARAMETER_GROUP_LABEL, Messages.getInstance().getString(
        "ReportPlugin.SystemParameters" ) );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.LABEL,
      SYS_PARAM_RENDER_MODE );
    listParameter.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE,
      ParameterAttributeNames.Core.TYPE_DROPDOWN );
    listParameter.setRole( ParameterAttributeNames.Core.ROLE_SYSTEM_PARAMETER );
    listParameter.addValues( "XML", "XML" ); // NON-NLS
    listParameter.addValues( "REPORT", "REPORT" ); // NON-NLS
    listParameter.addValues( "DOWNLOAD", "DOWNLOAD" ); // NON-NLS
    listParameter.addValues( "PARAMETER", "PARAMETER" ); // NON-NLS
    return listParameter;
  }

  private Boolean requestFlag( final String parameter, final MasterReport report, final String attributeNamespace,
                               final String attributeName, final String configurationKey ) {
    if ( parameter != null ) {
      final IParameterProvider parameters = getRequestParameters();
      final String parameterValue = parameters.getStringParameter( parameter, "" );
      if ( "true".equals( parameterValue ) ) {
        return Boolean.TRUE;
      }
      if ( "false".equals( parameterValue ) ) {
        return Boolean.FALSE;
      }
    }

    if ( attributeNamespace != null && attributeName != null ) {
      final Object attr = report.getAttribute( attributeNamespace, attributeName );
      if ( Boolean.TRUE.equals( attr ) || "true".equals( attr ) ) {
        return Boolean.TRUE;
      }
      if ( Boolean.FALSE.equals( attr ) || "false".equals( attr ) ) {
        return Boolean.FALSE;
      }
    }

    if ( configurationKey != null ) {
      final String configProperty = report.getConfiguration().getConfigProperty( configurationKey );
      if ( "true".equals( configProperty ) ) {
        return Boolean.TRUE;
      }
      if ( "false".equals( configProperty ) ) {
        return Boolean.FALSE;
      }
    }
    return null;
  }

  private String requestConfiguration( final String parameter, final MasterReport report,
                                       final String attributeNamespace, final String attributeName,
                                       final String configurationKey ) {
    if ( parameter != null ) {
      final IParameterProvider parameters = getRequestParameters();
      final String parameterValue = parameters.getStringParameter( parameter, "" );
      if ( StringUtils.isEmpty( parameterValue ) == false ) {
        return parameterValue;
      }
    }

    if ( attributeNamespace != null && attributeName != null ) {
      final Object attr = report.getAttribute( attributeNamespace, attributeName );
      if ( attr != null && StringUtils.isEmpty( String.valueOf( attr ) ) == false ) {
        return String.valueOf( attr );
      }
    }

    if ( configurationKey != null ) {
      final String configProperty = report.getConfiguration().getConfigProperty( configurationKey );
      if ( StringUtils.isEmpty( configProperty ) == false ) {
        return configProperty;
      }
    }
    return null;
  }

  private static class OutputParameterCollector {
    private OutputParameterCollector() {
    }

    public String[] collectParameter( final MasterReport reportDefinition ) {
      final LinkedHashSet<String> parameter = new LinkedHashSet<String>();

      inspectElement( reportDefinition, parameter );
      traverseSection( reportDefinition, parameter );

      return parameter.toArray( new String[ parameter.size() ] );
    }

    private void traverseSection( final Section section, final LinkedHashSet<String> parameter ) {
      final int count = section.getElementCount();
      for ( int i = 0; i < count; i++ ) {
        final ReportElement element = section.getElement( i );
        inspectElement( element, parameter );
        if ( element instanceof Section ) {
          traverseSection( (Section) element, parameter );
        }
      }
    }

    private void inspectElement( final ReportElement element, final LinkedHashSet<String> parameter ) {
      try {
        final Expression expression = element.getStyleExpression( ElementStyleKeys.HREF_TARGET );
        if ( expression instanceof FormulaExpression == false ) {
          // DrillDown only works with the formula function of the same name
          return;
        }

        final FormulaExpression fe = (FormulaExpression) expression;
        final String formulaText = fe.getFormulaExpression();
        if ( StringUtils.isEmpty( formulaText ) ) {
          // DrillDown only works with the formula function of the same name
          return;
        }

        if ( formulaText.startsWith( "DRILLDOWN" ) == false ) { // NON-NLS
          // DrillDown only works if the function is the only element. Everything else is beyond our control.
          return;
        }
        final FormulaParser formulaParser = new FormulaParser();
        final LValue value = formulaParser.parse( formulaText );
        if ( value instanceof FormulaFunction == false ) {
          // Not a valid formula or a complex term - we do not handle that
          return;
        }
        final DefaultFormulaContext context = new DefaultFormulaContext();
        value.initialize( context );

        final FormulaFunction fn = (FormulaFunction) value;
        final LValue[] params = fn.getChildValues();
        if ( params.length != 3 ) {
          // Malformed formula: Need 3 parameter
          return;
        }
        final String config = extractText( params[ 0 ] );
        if ( config == null ) {
          // Malformed formula: No statically defined config profile
          return;
        }

        final DrillDownProfile profile = DrillDownProfileMetaData.getInstance().getDrillDownProfile( config );
        if ( profile == null ) {
          // Malformed formula: Unknown drilldown profile
          return;
        }

        if ( StringUtils.isEmpty( profile.getAttribute( "group" ) ) || !profile.getAttribute( "group" )
          .startsWith( "pentaho" ) ) { // NON-NLS
          // Only 'pentaho' and 'pentaho-sugar' drill-down profiles can be used. Filters out all other third party
          // drilldowns
          return;
        }

        if ( params[ 2 ] instanceof DataTable == false ) {
          // Malformed formula: Not a parameter table
          return;
        }
        final DataTable dataTable = (DataTable) params[ 2 ];
        final int rowCount = dataTable.getRowCount();
        final int colCount = dataTable.getColumnCount();
        if ( colCount != 2 ) {
          // Malformed formula: Parameter table is invalid. Must be two cols, many rows ..
          return;
        }

        for ( int i = 0; i < rowCount; i++ ) {
          final LValue valueAt = dataTable.getValueAt( i, 0 );
          final String name = extractText( valueAt );
          if ( name == null ) {
            continue;
          }
          parameter.add( name );
        }
      } catch ( Exception e ) {
        // ignore ..
        CommonUtil.checkStyleIgnore();
      }
    }

    private String extractText( final LValue value ) {
      if ( value == null ) {
        return null;
      }
      if ( value.isConstant() ) {
        if ( value instanceof StaticValue ) {
          final StaticValue staticValue = (StaticValue) value;
          final Object o = staticValue.getValue();
          if ( o == null ) {
            return null; // NON-NLS
          }
          return String.valueOf( o );
        }
      }
      return null; // NON-NLS

    }

  }
}
