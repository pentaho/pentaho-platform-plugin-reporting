/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportState;
import org.pentaho.reporting.engine.classic.core.event.async.ReportListenerThreadHolder;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportExecution;
import org.pentaho.reporting.platform.plugin.async.IJobIdGenerator;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.SimpleObjectFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test basic functionality and audit usage.
 * <p>
 * Created by dima.prokopenko@gmail.com on 4/21/2016.
 */
public class BackgroundJobContentGeneratorTest {

  public static final String instanceId = "instanceId";
  public static final String sessionId = "sessionId";
  public static final String sessionName = "junitName";
  public static final String path = "junitPath";
  public static final String fieldId = "junitFieldId";

  public static final UUID uuid = UUID.randomUUID();

  IApplicationContext appContext = mock( IApplicationContext.class );
  SimpleObjectFactory factory = new SimpleObjectFactory();
  SimpleReportingComponent component = mock( SimpleReportingComponent.class );

  IPentahoSession session = mock( IPentahoSession.class );
  IParameterProvider paramProvider = mock( IParameterProvider.class );
  HttpServletResponse httpResponse = mock( HttpServletResponse.class );
  HttpServletRequest httpRequest = mock( HttpServletRequest.class );

  Map<String, IParameterProvider> parameterProviders;
  private IJobIdGenerator jobIdGenerator;

  @Before
  public void before() throws ObjectFactoryException {
    Path customStaging =
      Paths.get( System.getProperty( "java.io.tmpdir" ), BackgroundJobContentGeneratorTest.class.getSimpleName() );

    when( appContext.getSolutionPath( anyString() ) ).thenReturn( customStaging.toString() );

    //register mock executor to not execute anything
    factory.defineObject( PentahoAsyncExecutor.BEAN_NAME, MockPentahoAsyncExecutor.class.getName() );

    PentahoSystem.setApplicationContext( appContext );
    PentahoSystem.registerPrimaryObjectFactory( factory );

    jobIdGenerator = mock( IJobIdGenerator.class );
    when( jobIdGenerator.acquire( any(), any() ) ).thenReturn( true );
    PentahoSystem.registerObject( jobIdGenerator, IJobIdGenerator.class );

    when( session.getId() ).thenReturn( sessionId );
    when( session.getName() ).thenReturn( sessionName );

    parameterProviders = new HashMap<>();
    parameterProviders.put( "path", paramProvider );
    parameterProviders.put( IParameterProvider.SCOPE_REQUEST, paramProvider );
    when( paramProvider.getParameter( "httpresponse" ) ).thenReturn( httpResponse );
    when( paramProvider.getParameter( "httprequest" ) ).thenReturn( httpRequest );
    when( paramProvider.getStringParameter( anyString(), anyString() ) )
      .thenAnswer( ( i ) -> {
        if ( !"reservedId".equals( i.getArguments()[ 0 ] ) ) {
          return i.getArguments()[ 1 ];
        } else {
          return uuid.toString();
        }
      } );
    when( paramProvider.getParameterNames() ).thenReturn( Collections.emptyIterator() );
    when( httpRequest.getContextPath() ).thenReturn( "/custompath" );

    PentahoSessionHolder.setSession( session );
  }

  @Test
  public void testSuccessExecutionAudit() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );

    when( component.validate() ).thenReturn( true );

    AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );

    // execution attempt registered
    verify( wrapper, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( generator.getClass().getName() ),
      eq( generator.getClass().getName() ),
      eq( MessageTypes.EXECUTION ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( generator )
    );

    verify( generator, times( 0 ) ).sendErrorResponse();
    verify( generator, times( 1 ) ).sendSuccessRedirect( eq( uuid ) );

    final String contextUrl = httpRequest.getContextPath();
    verify( httpResponse ).sendRedirect( eq( contextUrl + BackgroundJobReportContentGenerator.REDIRECT_PREFIX + uuid.toString()
      + BackgroundJobReportContentGenerator.REDIRECT_POSTFIX ) );

    assertEquals( "BACKLOG-6745", instanceId, ReportListenerThreadHolder.getRequestId() );

  }

  @Test
  public void testFailedExecutionAudit() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );

    // validation will not pass
    when( component.validate() ).thenReturn( false );

    AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );

    // execution attempt registered
    verify( wrapper, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( generator.getClass().getName() ),
      eq( generator.getClass().getName() ),
      eq( MessageTypes.EXECUTION ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( generator )
    );

    // then fail registered also.
    verify( wrapper, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( generator.getClass().getName() ),
      eq( generator.getClass().getName() ),
      eq( MessageTypes.FAILED ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( generator )
    );

    verify( generator, times( 0 ) ).sendSuccessRedirect( any( UUID.class ) );
    verify( generator, times( 1 ) ).sendErrorResponse();
  }

  @Test
  public void testSimpleReportingComponentInitialized() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );
    when( component.validate() ).thenReturn( true );
    AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );

    verify( component, times( 1 ) ).setReportFileId( eq( fieldId ) );
    verify( component, times( 0 ) ).setReportDefinitionPath( anyString() );
    verify( component, times( 0 ) ).setReportDefinitionInputStream( any( InputStream.class ) );

    verify( component, times( 1 ) ).setPaginateOutput( eq( true ) );
    verify( component, times( 1 ) ).setForceDefaultOutputTarget( eq( false ) );
    verify( component, times( 1 ) ).setDefaultOutputTarget( eq( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE ) );

    // this is not a prpti report
    verify( component, times( 0 ) ).setForceUnlockPreferredOutput( anyBoolean() );
  }

  @Test
  public void testSimpleReportingComponentInitializedPrpti() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );
    when( component.validate() ).thenReturn( true );
    AuditWrapper wrapper = mock( AuditWrapper.class );

    // we determine prpti report by the enc of the string
    generator.createReportContent( fieldId, path + ".prpti", component, wrapper );

    verify( component, times( 1 ) ).setReportFileId( eq( fieldId ) );
    verify( component, times( 0 ) ).setReportDefinitionPath( anyString() );
    verify( component, times( 0 ) ).setReportDefinitionInputStream( any( InputStream.class ) );

    verify( component, times( 1 ) ).setPaginateOutput( eq( true ) );
    verify( component, times( 1 ) ).setForceDefaultOutputTarget( eq( false ) );
    verify( component, times( 1 ) ).setDefaultOutputTarget( eq( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE ) );

    // this is prpti report
    verify( component, times( 1 ) ).setForceUnlockPreferredOutput( eq( true ) );
  }


  @Test
  public void testProvidedUuid() throws Exception {
    final BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );
    when( component.validate() ).thenReturn( true );
    final AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );

    verify( generator ).sendSuccessRedirect( uuid );
  }


  @Test
  public void testWrongProvidedUuid() throws Exception {
    final Map<String, IParameterProvider> customParameterProviders = new HashMap<>();
    final IParameterProvider customParamProvider = mock( IParameterProvider.class );
    customParameterProviders.put( "path", customParamProvider );
    customParameterProviders.put( IParameterProvider.SCOPE_REQUEST, customParamProvider );
    when( customParamProvider.getParameter( "httpresponse" ) ).thenReturn( httpResponse );
    when( customParamProvider.getParameter( "httprequest" ) ).thenReturn( httpRequest );
    when( customParamProvider.getStringParameter( anyString(), anyString() ) )
      .thenAnswer( ( i ) -> {
        if ( !"reservedId".equals( i.getArguments()[ 0 ] ) ) {
          return i.getArguments()[ 1 ];
        } else {
          return "not a uuid";
        }
      } );
    when( customParamProvider.getParameterNames() ).thenReturn( Collections.emptyIterator() );
    when( jobIdGenerator.acquire( any(), eq( null ) ) ).thenReturn( false );
    final BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( customParameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );
    when( component.validate() ).thenReturn( true );
    final AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );
    verify( generator, times( 1 ) ).sendSuccessRedirect( any() );
    verify( generator, never() ).sendSuccessRedirect( uuid );
  }


  @Test
  public void testNullGenerator() throws Exception {
    PentahoSystem.shutdown();
    PentahoSystem.setApplicationContext( appContext );
    PentahoSystem.registerPrimaryObjectFactory( factory );

    final BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );
    when( component.validate() ).thenReturn( true );
    final AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );
    verify( generator, times( 1 ) ).sendSuccessRedirect( any() );
    verify( generator, never() ).sendSuccessRedirect( uuid );
  }

  @AfterClass
  public static void afterClass() {
    PentahoSessionHolder.removeSession();
    PentahoSystem.shutdown();
  }

  /**
   * We can't use mock for this class. Since we obtain it through pentaho factory - it should be public available class,
   * not a particular mocked object.
   */
  public static class MockPentahoAsyncExecutor implements IPentahoAsyncExecutor {

    @Override public Future<IFixedSizeStreamingContent> getFuture( UUID id, IPentahoSession session ) {
      return null;
    }

    @Override public void cleanFuture( UUID id, IPentahoSession session ) {

    }

    @Override public UUID addTask( IAsyncReportExecution task, IPentahoSession session ) {
      return uuid;
    }

    @Override public UUID addTask( IAsyncReportExecution task, IPentahoSession session, UUID uuid1 ) {
      return uuid1;
    }

    @Override public IAsyncReportState getReportState(UUID id, IPentahoSession session ) {
      return null;
    }

    @Override public void requestPage( UUID id, IPentahoSession session, int page ) {

    }

    @Override public boolean schedule( UUID uuid, IPentahoSession session ) {
      return true;
    }

    @Override public boolean preSchedule( UUID uuid, IPentahoSession session ) {
      return false;
    }

    @Override public UUID recalculate( UUID uuid, IPentahoSession session ) {
      return null;
    }

    @Override public void updateSchedulingLocation( UUID uuid, IPentahoSession session, Serializable location,
                                                    String newName ) {

    }

    @Override
    public void shutdown() {

    }
  }
}
