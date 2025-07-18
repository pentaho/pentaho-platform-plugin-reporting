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
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.locale.IPentahoLocale;
import org.pentaho.platform.api.repository2.unified.IRepositoryFileData;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileAce;
import org.pentaho.platform.api.repository2.unified.RepositoryFileAcl;
import org.pentaho.platform.api.repository2.unified.RepositoryFilePermission;
import org.pentaho.platform.api.repository2.unified.RepositoryFileTree;
import org.pentaho.platform.api.repository2.unified.RepositoryRequest;
import org.pentaho.platform.api.repository2.unified.VersionSummary;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.test.platform.engine.core.SimpleObjectFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyFloat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests ExecuteContentHandler.createReportContent(...) katamari code.
 * <p>
 * Created by dima.prokopenko@gmail.com on 4/26/2016.
 */
public class ExecuteReportContentHandlerTest {

  private static final String sessionId = "sessionId_09";
  private static final String sessionName = "junitName_90";
  private static final String fileId = "junit F id";
  private static final String path = "some/path.prpt";
  private static final String instanceId = UUID.randomUUID().toString();

  private ReportContentGenerator contentGenerator = mock( ReportContentGenerator.class );
  private IPentahoSession session = mock( IPentahoSession.class );
  private OutputStream outputStream = mock( OutputStream.class );
  private SimpleReportingComponent reportComponent = mock( SimpleReportingComponent.class );
  private AuditWrapper audit = mock( AuditWrapper.class );

  private HttpServletResponse response = mock( HttpServletResponse.class );
  private IParameterProvider prameterProvider = mock( IParameterProvider.class );
  private Map<String, IParameterProvider> providersMap = new HashMap<>();

  private static SimpleObjectFactory factory = new SimpleObjectFactory();

  private static RepositoryFile file = mock( RepositoryFile.class );
  private static String fileName = "/someFile.prpt";

  private MasterReport report = mock( MasterReport.class );

  private Map<String, Object> inputs = new HashMap<>();

  @BeforeClass
  public static void beforeClass() {
    // define mock repository which will return mocked files
    factory.defineObject( IUnifiedRepository.class.getSimpleName(), MockUnifiedRepository.class.getName() );
    PentahoSystem.registerPrimaryObjectFactory( factory );

    when( file.getName() ).thenReturn( fileName );
  }

  @Before
  public void before() throws IOException, ResourceException {
    when( session.getId() ).thenReturn( sessionId );
    when( session.getName() ).thenReturn( sessionName );
    when( contentGenerator.getUserSession() ).thenReturn( session );

    // set test staging mode:
    inputs.put( "report-staging-mode", StagingMode.THRU );

    when( contentGenerator.createInputs() ).thenReturn( inputs );

    // set report mime type
    when( reportComponent.getMimeType() ).thenReturn( "text/html" );

    providersMap.put( "path", prameterProvider );
    when( prameterProvider.getParameter( eq( "httpresponse" ) ) ).thenReturn( response );
    when( contentGenerator.getParameterProviders() ).thenReturn( providersMap );
    when( contentGenerator.getInstanceId() ).thenReturn( instanceId );
    when( contentGenerator.getObjectName() ).thenReturn( ReportContentGenerator.class.getName() );
  }

  /**
   * should audit success when report validated AND executed.
   *
   * @throws Exception
   */
  @Test
  public void testSuccessExecutionAudit() throws Exception {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    when( reportComponent.validate() ).thenReturn( true );
    when( reportComponent.execute() ).thenReturn( true );

    handler.createReportContent( outputStream, fileId, path, true, reportComponent, audit );

    // execution attempt registered
    verify( audit, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( ReportContentGenerator.class.getName() ),
      eq( handler.getClass().getName() ),
      eq( MessageTypes.INSTANCE_START ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( contentGenerator )
    );

    verify( audit, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( ReportContentGenerator.class.getName() ),
      eq( handler.getClass().getName() ),
      eq( MessageTypes.INSTANCE_END ),
      eq( instanceId ),
      eq( "" ),
      anyFloat(),
      eq( contentGenerator )
    );
  }

  /**
   * validation failed and execution even not started.
   *
   * @throws Exception
   */
  @Test
  public void testFailedValidationAudit() throws Exception {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    when( reportComponent.validate() ).thenReturn( false );

    when( reportComponent.execute() )
      .thenThrow( new Exception( "execution should not be called if valiedation failed" ) );

    handler.createReportContent( outputStream, fileId, path, true, reportComponent, audit );

    verify( audit, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( ReportContentGenerator.class.getName() ),
      eq( handler.getClass().getName() ),
      eq( MessageTypes.INSTANCE_START ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( contentGenerator )
    );

    verify( audit, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( ReportContentGenerator.class.getName() ),
      eq( handler.getClass().getName() ),
      eq( MessageTypes.FAILED ),
      eq( instanceId ),
      eq( "" ),
      anyFloat(),
      eq( contentGenerator )
    );
  }

  /**
   * validation passed but execution failed.
   *
   * @throws Exception
   */
  @Test
  public void testFailedExecutionAudit() throws Exception {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    when( reportComponent.validate() ).thenReturn( true );

    when( reportComponent.execute() ).thenReturn( false );

    handler.createReportContent( outputStream, fileId, path, true, reportComponent, audit );

    verify( audit, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( ReportContentGenerator.class.getName() ),
      eq( handler.getClass().getName() ),
      eq( MessageTypes.INSTANCE_START ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( contentGenerator )
    );

    verify( audit, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( ReportContentGenerator.class.getName() ),
      eq( handler.getClass().getName() ),
      eq( MessageTypes.FAILED ),
      eq( instanceId ),
      eq( "" ),
      anyFloat(),
      eq( contentGenerator )
    );
  }


  /**
   * Tests basic actions for report component when create normal report.
   */
  @Test
  public void testCreateReportContentReportComponent1() throws Exception {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    // validated and executed.
    when( reportComponent.validate() ).thenReturn( true );
    when( reportComponent.execute() ).thenReturn( true );

    // simulate pagable html.
    when( reportComponent.getComputedOutputTarget() ).thenReturn( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE );

    inputs.remove( ParameterXmlContentHandler.SYS_PARAM_SESSION_ID );

    boolean force = false;

    handler.createReportContent( outputStream, fileId, path, force, reportComponent, audit );

    assertNotNull( "always have sys param session Id set.",
      inputs.get( ParameterXmlContentHandler.SYS_PARAM_SESSION_ID ) );

    // report component initialized:
    verify( reportComponent, times( 1 ) ).setReportFileId( eq( fileId ) );
    verify( reportComponent, times( 1 ) ).setPaginateOutput( eq( true ) );
    verify( reportComponent, times( 1 ) ).setForceDefaultOutputTarget( force );

    // do override this value only for prpti reports, not for prpt.
    verify( reportComponent, times( 0 ) ).setForceUnlockPreferredOutput( anyBoolean() );
    verify( reportComponent, times( 1 ) ).setInputs( eq( inputs ) );

    // finally we obtain MasterReport from report component
    verify( reportComponent, times( 1 ) ).getReport();

    // we do set output stream to report component - for THRU it is wrapped servlet output stream
    verify( reportComponent, times( 1 ) ).setOutputStream( any( OutputStream.class ) );

    verify( reportComponent, times( 1 ) ).getMimeType();
  }

  /**
   * Test response headers set for successfull execution
   */
  @Test
  public void setSuccessHeadersTest() throws Exception {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    // validated and executed.
    when( reportComponent.validate() ).thenReturn( true );
    when( reportComponent.execute() ).thenReturn( true );

    handler.createReportContent( outputStream, fileId, path, true, reportComponent, audit );

    // since this is successfull report execution check for response headers set:
    verify( response, times( 1 ) )
      .setHeader( eq( "Content-Disposition" ), eq( "inline; filename*=UTF-8''%3AsomeFile.html" ) );
    verify( response, times( 1 ) ).setHeader( eq( "Content-Description" ), eq( fileName ) );
    verify( response, times( 1 ) ).setHeader( eq( "Cache-Control" ), eq( "private, max-age=0, must-revalidate" ) );
    verify( response, times( 1 ) ).setContentLength( anyInt() );

    // we don't flush or close servlet output stream manually:
    verify( outputStream, times( 0 ) ).flush();
    verify( outputStream, times( 0 ) ).close();

  }

  @Test
  public void setFailedHeadersTest() throws Exception {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    // validated and executed.
    when( reportComponent.validate() ).thenReturn( true );
    when( reportComponent.execute() ).thenReturn( false );

    handler.createReportContent( outputStream, fileId, path, true, reportComponent, audit );

    // headers set in any case:
    verify( response, times( 1 ) )
      .setHeader( eq( "Content-Disposition" ), eq( "inline; filename*=UTF-8''%3AsomeFile.html" ) );
    verify( response, times( 1 ) ).setHeader( eq( "Content-Description" ), eq( fileName ) );
    verify( response, times( 1 ) ).setHeader( eq( "Cache-Control" ), eq( "private, max-age=0, must-revalidate" ) );

    // sepcific for failed attempt:
    verify( response, times( 1 ) ).setStatus( eq( HttpServletResponse.SC_INTERNAL_SERVER_ERROR ) );

    // this is famous 'Sorry, we really did try'
    verify( outputStream, times( 1 ) )
      .write( Messages.getInstance().getString( "ReportPlugin.ReportValidationFailed" ).getBytes() );

    // oh we don't close output stream, but do a flush:
    verify( outputStream, times( 1 ) ).flush();
    verify( outputStream, times( 0 ) ).close();
  }

  /**
   * Check staging mode applied when passed as parameter
   */
  @Test
  public void testGetStagingModeFromInputs() {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    // we have set default value to THRU
    StagingMode mode = (StagingMode) inputs.get( "report-staging-mode" );
    assertEquals( StagingMode.THRU, mode );

    mode = handler.getStagingMode( inputs, report );
    assertEquals( StagingMode.THRU, mode );

    inputs.put( "report-staging-mode", StagingMode.MEMORY );

    mode = handler.getStagingMode( inputs, report );
    assertEquals( StagingMode.MEMORY, mode );
  }

  /**
   * Staging mode set as attribute for MasterReport
   */
  @Test
  public void testGetStagingHandlerFromReport() {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    // no mode from inputs
    inputs.remove( "report-staging-mode" );
    // staging mode set as a field for report:
    when( report.getAttribute(
      eq( AttributeNames.Pentaho.NAMESPACE ),
      eq( AttributeNames.Pentaho.STAGING_MODE ) )
    ).thenReturn( StagingMode.TMPFILE );

    StagingMode mode = handler.getStagingMode( inputs, report );

    assertEquals( StagingMode.TMPFILE, mode );
  }

  /**
   * Staging mode default if no request, no report definition, neither PentahoSystem.globalProperties does not contains
   * such values.
   */
  @Test
  public void testGetStagingModeDefault() {
    final ExecuteReportContentHandler handler = new ExecuteReportContentHandler( contentGenerator );

    // no mode from inputs, no from report (since we don't mock it directly)
    inputs.remove( "report-staging-mode" );

    StagingMode mode = handler.getStagingMode( inputs, report );

    assertEquals( StagingMode.THRU, mode );
  }

  @AfterClass
  public static void afterClass() {
    PentahoSystem.shutdown();
  }

  /**
   * A huge class mock to be used for pentaho factory. :(
   */
  public static class MockUnifiedRepository implements IUnifiedRepository {

    @Override public RepositoryFile getFile( String path ) {
      return null;
    }

    @Override public RepositoryFileTree getTree( String path, int depth, String filter, boolean showHidden ) {
      return null;
    }

    @Override public RepositoryFileTree getTree( RepositoryRequest repositoryRequest ) {
      return null;
    }

    @Override public RepositoryFile getFileAtVersion( Serializable fileId, Serializable versionId ) {
      return null;
    }

    @Override public RepositoryFile getFileById( Serializable fileId ) {
      return file;
    }

    @Override public RepositoryFile getFile( String path, boolean loadLocaleMaps ) {
      return null;
    }

    @Override public RepositoryFile getFileById( Serializable fileId, boolean loadLocaleMaps ) {
      return null;
    }

    @Override public RepositoryFile getFile( String path, IPentahoLocale locale ) {
      return null;
    }

    @Override public RepositoryFile getFileById( Serializable fileId, IPentahoLocale locale ) {
      return null;
    }

    @Override public RepositoryFile getFile( String path, boolean loadLocaleMaps, IPentahoLocale locale ) {
      return null;
    }

    @Override public RepositoryFile getFileById( Serializable fileId, boolean loadLocaleMaps, IPentahoLocale locale ) {
      return null;
    }

    @Override public <T extends IRepositoryFileData> T getDataForRead( Serializable fileId, Class<T> dataClass ) {
      return null;
    }

    @Override
    public <T extends IRepositoryFileData> T getDataAtVersionForRead( Serializable fileId, Serializable versionId,
                                                                      Class<T> dataClass ) {
      return null;
    }

    @Override public <T extends IRepositoryFileData> T getDataForExecute( Serializable fileId, Class<T> dataClass ) {
      return null;
    }

    @Override
    public <T extends IRepositoryFileData> T getDataAtVersionForExecute( Serializable fileId, Serializable versionId,
                                                                         Class<T> dataClass ) {
      return null;
    }

    @Override public <T extends IRepositoryFileData> List<T> getDataForReadInBatch( List<RepositoryFile> files,
                                                                                    Class<T> dataClass ) {
      return null;
    }

    @Override public <T extends IRepositoryFileData> List<T> getDataForExecuteInBatch( List<RepositoryFile> files,
                                                                                       Class<T> dataClass ) {
      return null;
    }

    @Override
    public RepositoryFile createFile( Serializable parentFolderId, RepositoryFile file, IRepositoryFileData data,
                                      String versionMessage ) {
      return null;
    }

    @Override
    public RepositoryFile createFile( Serializable parentFolderId, RepositoryFile file, IRepositoryFileData data,
                                      RepositoryFileAcl acl, String versionMessage ) {
      return null;
    }

    @Override
    public RepositoryFile createFolder( Serializable parFolderId, RepositoryFile file, String versionMessage ) {
      return null;
    }

    @Override
    public RepositoryFile createFolder( Serializable parentFolderId, RepositoryFile file, RepositoryFileAcl acl,
                                        String versionMessage ) {
      return null;
    }

    @Override public RepositoryFile updateFolder( RepositoryFile folder, String versionMessage ) {
      return null;
    }

    @Override public List<RepositoryFile> getChildren( Serializable folderId ) {
      return null;
    }

    @Override public List<RepositoryFile> getChildren( Serializable folderId, String filter ) {
      return null;
    }

    @Override public List<RepositoryFile> getChildren( Serializable folderId, String filter, Boolean showHiddenFiles ) {
      return null;
    }

    @Override public List<RepositoryFile> getChildren( RepositoryRequest repositoryRequest ) {
      return null;
    }

    @Override public RepositoryFile updateFile( RepositoryFile file, IRepositoryFileData data, String versionMessage ) {
      return null;
    }

    @Override public void deleteFile( Serializable fileId, boolean permanent, String versionMessage ) {

    }

    @Override public void deleteFile( Serializable fileId, String versionMessage ) {

    }

    @Override public void moveFile( Serializable fileId, String destAbsPath, String versionMessage ) {

    }

    @Override public void copyFile( Serializable fileId, String destAbsPath, String versionMessage ) {

    }

    @Override public void undeleteFile( Serializable fileId, String versionMessage ) {

    }

    @Override public List<RepositoryFile> getDeletedFiles( String origParentFolderPath ) {
      return null;
    }

    @Override public List<RepositoryFile> getDeletedFiles( String origParentFolderPath, String filter ) {
      return null;
    }

    @Override public List<RepositoryFile> getDeletedFiles() {
      return null;
    }

    @Override public boolean canUnlockFile( Serializable fileId ) {
      return false;
    }

    @Override public void lockFile( Serializable fileId, String message ) {

    }

    @Override public void unlockFile( Serializable fileId ) {

    }

    @Override public RepositoryFileAcl getAcl( Serializable fileId ) {
      return null;
    }

    @Override public RepositoryFileAcl updateAcl( RepositoryFileAcl acl ) {
      return null;
    }

    @Override public boolean hasAccess( String path, EnumSet<RepositoryFilePermission> permissions ) {
      return false;
    }

    @Override public List<RepositoryFileAce> getEffectiveAces( Serializable fileId ) {
      return null;
    }

    @Override public List<RepositoryFileAce> getEffectiveAces( Serializable fileId, boolean forceEntriesInheriting ) {
      return null;
    }

    @Override public VersionSummary getVersionSummary( Serializable fileId, Serializable versionId ) {
      return null;
    }

    @Override public List<VersionSummary> getVersionSummaryInBatch( List<RepositoryFile> files ) {
      return null;
    }

    @Override public List<VersionSummary> getVersionSummaries( Serializable fileId ) {
      return null;
    }

    @Override public void deleteFileAtVersion( Serializable fileId, Serializable versionId ) {

    }

    @Override public void restoreFileAtVersion( Serializable fileId, Serializable versionId, String versionMessage ) {

    }

    @Override public List<RepositoryFile> getReferrers( Serializable fileId ) {
      return null;
    }

    @Override public void setFileMetadata( Serializable fileId, Map<String, Serializable> metadataMap ) {

    }

    @Override public Map<String, Serializable> getFileMetadata( Serializable fileId ) {
      return null;
    }

    @Override public List<Character> getReservedChars() {
      return null;
    }

    @Override public List<Locale> getAvailableLocalesForFileById( Serializable fileId ) {
      return null;
    }

    @Override public List<Locale> getAvailableLocalesForFileByPath( String relPath ) {
      return null;
    }

    @Override public List<Locale> getAvailableLocalesForFile( RepositoryFile repositoryFile ) {
      return null;
    }

    @Override public Properties getLocalePropertiesForFileById( Serializable fileId, String locale ) {
      return null;
    }

    @Override public Properties getLocalePropertiesForFileByPath( String relPath, String locale ) {
      return null;
    }

    @Override public Properties getLocalePropertiesForFile( RepositoryFile repositoryFile, String locale ) {
      return null;
    }

    @Override public void setLocalePropertiesForFileById( Serializable fileId, String locale, Properties properties ) {

    }

    @Override public void setLocalePropertiesForFileByPath( String relPath, String locale, Properties properties ) {

    }

    @Override public void setLocalePropertiesForFile( RepositoryFile repoFile, String locale, Properties properties ) {

    }

    @Override public void deleteLocalePropertiesForFile( RepositoryFile repositoryFile, String locale ) {

    }
  }
}
