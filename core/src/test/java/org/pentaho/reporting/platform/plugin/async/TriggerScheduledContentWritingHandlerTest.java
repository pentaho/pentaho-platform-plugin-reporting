/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.async;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

public class TriggerScheduledContentWritingHandlerTest {

  private PentahoAsyncExecutor<IAsyncReportState> pentahoAsyncExecutor;
  private IPentahoSession session;
  private static final IUnifiedRepository repository = mock( IUnifiedRepository.class );
  private ISecurityHelper iSecurityHelper;


  @BeforeClass
  public static void setUpClass() throws Exception {
    PentahoSystem.registerObject( repository, IUnifiedRepository.class );
  }

  @AfterClass
  public static void restore() {
    PentahoSystem.shutdown();
  }

  @Before
  public void setUp() throws Exception {
    session = new StandaloneSession( "test" );
    PentahoSessionHolder.setSession( session );
    pentahoAsyncExecutor = new PentahoAsyncExecutor<IAsyncReportState>( 1 );
    PentahoSystem.registerObject( repository, IUnifiedRepository.class );
    iSecurityHelper = mock( ISecurityHelper.class );
    SecurityHelper.setMockInstance( iSecurityHelper );
    reset( repository );
  }

  @Test
  public void notifyListeners() throws Exception {

    final CountDownLatch countDownLatch = new CountDownLatch( 1 );

    try {

      final UUID uuid = pentahoAsyncExecutor
        .addTask( new PentahoAsyncReportExecution( "junit-path", mock( SimpleReportingComponent.class ),
          mock( AsyncJobFileStagingHandler.class ), session, "junit", AuditWrapper.NULL ) {

          @Override public IFixedSizeStreamingContent call() throws Exception {
            countDownLatch.await();
            return null;
          }
        }, session );

      final PentahoAsyncExecutor<IAsyncReportState>.TriggerScheduledContentWritingHandler
        handler =
        pentahoAsyncExecutor.new TriggerScheduledContentWritingHandler( "test", "test",
          mock( IAsyncReportExecution.class ), new PentahoAsyncExecutor.CompositeKey( session, uuid ) );


      pentahoAsyncExecutor.updateSchedulingLocation( uuid, session, "test", "test" );

      handler.notifyListeners( "test" );

      verify( repository, times( 2 ) ).getFileById( "test" );

    } finally {
      countDownLatch.countDown();
    }
  }

  @Test
  public void onSuccess() throws Exception {
    final PentahoAsyncExecutor<IAsyncReportState>.TriggerScheduledContentWritingHandler
      handler1 =
      pentahoAsyncExecutor.new TriggerScheduledContentWritingHandler( null, "test",
        mock( IAsyncReportExecution.class ), mock( PentahoAsyncExecutor.CompositeKey.class ) );
    final PentahoAsyncExecutor<IAsyncReportState>.TriggerScheduledContentWritingHandler
      handler2 =
      pentahoAsyncExecutor.new TriggerScheduledContentWritingHandler( "", "test",
        mock( IAsyncReportExecution.class ), mock( PentahoAsyncExecutor.CompositeKey.class ) );
    final PentahoAsyncExecutor<IAsyncReportState>.TriggerScheduledContentWritingHandler
      handler3 =
      pentahoAsyncExecutor.new TriggerScheduledContentWritingHandler( "test", "test",
        mock( IAsyncReportExecution.class ), mock( PentahoAsyncExecutor.CompositeKey.class ) );
    final IFixedSizeStreamingContent iFixedSizeStreamingContent = mock( IFixedSizeStreamingContent.class );
    handler1.onSuccess( iFixedSizeStreamingContent );
    verify( iSecurityHelper, never() ).runAsUser( any(), any() );
    handler2.onSuccess( iFixedSizeStreamingContent );
    verify( iSecurityHelper, never() ).runAsUser( any(), any() );
    handler3.onSuccess( iFixedSizeStreamingContent );
    verify( iSecurityHelper, times( 1 ) ).runAsUser( any(), any() );
  }


  @Test
  public void onSuccessError() throws Exception {
    final ISecurityHelper instanceValue = mock( ISecurityHelper.class );
    doThrow( new Exception(  ) ).when( instanceValue ).runAsUser( any(), any() );
    SecurityHelper.setMockInstance( instanceValue );
    final PentahoAsyncExecutor<IAsyncReportState>.TriggerScheduledContentWritingHandler
      handler3 =
      pentahoAsyncExecutor.new TriggerScheduledContentWritingHandler( "test", "test",
        mock( IAsyncReportExecution.class ), mock( PentahoAsyncExecutor.CompositeKey.class ) );
    final IFixedSizeStreamingContent iFixedSizeStreamingContent = mock( IFixedSizeStreamingContent.class );
    handler3.onSuccess( iFixedSizeStreamingContent );
    verify( iSecurityHelper, times( 0 ) ).runAsUser( any(), any() );
  }

}
