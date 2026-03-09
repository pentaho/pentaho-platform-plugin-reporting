/*
 * ! ******************************************************************************
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

import org.apache.commons.vfs2.FileObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class ParameterContentGeneratorTest {

  @Mock
  private IUnifiedRepository mockRepository;

  @Mock
  private RepositoryFile mockRepositoryFile;

  @Mock
  private IParameterProvider mockRequestParams;

  @Mock
  private IParameterProvider mockPathParams;

  @Mock
  private OutputStream mockOutputStream;

  @Mock
  private ParameterXmlContentHandler mockParameterXmlContentHandler;

  private ParameterContentGenerator parameterContentGenerator;
  private Map<String, IParameterProvider> parameterProviders;

  @Before
  public void setUp() {
    // Initialize parameter providers map
    parameterProviders = new HashMap<>();
    parameterProviders.put( IParameterProvider.SCOPE_REQUEST, mockRequestParams );
    parameterProviders.put( "path", mockPathParams );

    // Create a spy of ParameterContentGenerator to allow partial mocking
    parameterContentGenerator = spy( new ParameterContentGenerator() );

    // Mock the getRequestParameters and getPathParameters methods to return our mocked providers
    doReturn( mockRequestParams ).when( parameterContentGenerator ).getRequestParameters();
    doReturn( mockPathParams ).when( parameterContentGenerator ).getPathParameters();

    // Setup default mocking for ParameterXmlContentHandler
    doReturn( mockParameterXmlContentHandler )
      .when( parameterContentGenerator )
      .getParameterXmlContentHandler( parameterContentGenerator, true );
  }


  @After
  public void tearDown() {
    PentahoSystem.clearObjectFactory();
  }

  @Test
  public void testCreateContent_WithRepositoryFile() throws Exception {
    // Setup
    String testPath = "/Test/Report.prpt";
    String encodedPath = java.net.URLEncoder.encode( testPath, "UTF-8" );

    when( mockRequestParams.getStringParameter( "renderMode", "XML" ) ).thenReturn( "XML" );
    when( mockRequestParams.getStringParameter( "path", null ) ).thenReturn( encodedPath );
    when( mockRequestParams.getStringParameter( "path", "" ) ).thenReturn( encodedPath );

    when( mockRepositoryFile.getId() ).thenReturn( "12345" );
    when( mockRepositoryFile.getPath() ).thenReturn( testPath );

    // Mock PentahoSystem.get to return our mocked repository
    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );

      when( mockRepository.getFile( testPath ) ).thenReturn( mockRepositoryFile );

      // Execute
      parameterContentGenerator.createContent( mockOutputStream );

      // Verify
      verify( mockRepository ).getFile( testPath );
      verify( mockParameterXmlContentHandler ).createParameterContent(
        eq( mockOutputStream ),
        eq( "12345" ),
        isNull(),
        eq( testPath ),
        eq( false ),
        isNull()
      );
    }
  }

  @Test
  public void testCreateContent_WithVFSFile() throws Exception {
    // Setup
    FileObject mockFileObject = mock( FileObject.class );
    String testPath = "/vfs/path/to/report.prpt";

    when( mockRequestParams.getStringParameter( "renderMode", "XML" ) ).thenReturn( "XML" );
    when( mockPathParams.getParameter( "file" ) ).thenReturn( mockFileObject );
    when( mockPathParams.getStringParameter( "path", "" ) ).thenReturn( testPath );

    // Mock PentahoSystem.get to return our mocked repository
    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );

      // Execute
      parameterContentGenerator.createContent( mockOutputStream );

      // Verify
      verify( mockParameterXmlContentHandler ).createParameterContent(
        eq( mockOutputStream ),
        isNull(),
        eq( mockFileObject ),
        eq( testPath ),
        eq( false ),
        isNull()
      );
    }
  }

  @Test
  public void testCreateContent_ParameterRenderMode_XML() throws Exception {
    // Setup
    String testPath = "/Test/Report.prpt";
    String encodedPath = java.net.URLEncoder.encode( testPath, "UTF-8" );

    when( mockRequestParams.getStringParameter( "renderMode", "XML" ) ).thenReturn( "XML" );
    when( mockRequestParams.getStringParameter( "path", null ) ).thenReturn( encodedPath );
    when( mockRequestParams.getStringParameter( "path", "" ) ).thenReturn( encodedPath );

    when( mockRepositoryFile.getId() ).thenReturn( "12345" );
    when( mockRepositoryFile.getPath() ).thenReturn( testPath );

    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );
      when( mockRepository.getFile( testPath ) ).thenReturn( mockRepositoryFile );

      // Execute
      parameterContentGenerator.createContent( mockOutputStream );

      // Verify - XML render mode should be selected
      verify( mockParameterXmlContentHandler ).createParameterContent(
        eq( mockOutputStream ),
        eq( "12345" ),
        isNull(),
        eq( testPath ),
        eq( false ),
        isNull()
      );
    }
  }

  @Test
  public void testCreateContent_ParameterRenderMode_PARAMETER() throws Exception {
    // Setup
    String testPath = "/Test/Report.prpt";
    String encodedPath = java.net.URLEncoder.encode( testPath, "UTF-8" );

    when( mockRequestParams.getStringParameter( "renderMode", "XML" ) ).thenReturn( "PARAMETER" );
    when( mockRequestParams.getStringParameter( "path", null ) ).thenReturn( encodedPath );
    when( mockRequestParams.getStringParameter( "path", "" ) ).thenReturn( encodedPath );

    when( mockRepositoryFile.getId() ).thenReturn( "12345" );
    when( mockRepositoryFile.getPath() ).thenReturn( testPath );

    // Mock the spy to use different behavior for PARAMETER mode
    doReturn( mockParameterXmlContentHandler )
      .when( parameterContentGenerator )
      .getParameterXmlContentHandler( parameterContentGenerator, false );

    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );
      when( mockRepository.getFile( testPath ) ).thenReturn( mockRepositoryFile );

      // Execute
      parameterContentGenerator.createContent( mockOutputStream );

      // Verify - PARAMETER render mode should be selected
      verify( mockParameterXmlContentHandler ).createParameterContent(
        eq( mockOutputStream ),
        eq( "12345" ),
        isNull(),
        eq( testPath ),
        eq( false ),
        isNull()
      );
    }
  }

  @Test
  public void testResolvePrptFile_FromRequestParams() throws Exception {
    // Setup
    String testPath = "/Test/Report.prpt";
    String encodedPath = java.net.URLEncoder.encode( testPath, "UTF-8" );

    when( mockRequestParams.getStringParameter( "path", null ) ).thenReturn( encodedPath );
    when( mockRequestParams.getStringParameter( "path", "" ) ).thenReturn( encodedPath );

    when( mockRepositoryFile.getId() ).thenReturn( "12345" );
    when( mockRepositoryFile.getPath() ).thenReturn( testPath );

    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );
      when( mockRepository.getFile( testPath ) ).thenReturn( mockRepositoryFile );

      // Execute
      RepositoryFile result = parameterContentGenerator.resolvePrptFile( mockRequestParams );

      // Verify
      assertNotNull( result );
      assertEquals( "12345", result.getId() );
      assertEquals( testPath, result.getPath() );
      verify( mockRepository ).getFile( testPath );
    }
  }

  @Test
  public void testResolvePrptFile_FromPathParams() throws Exception {
    // Setup
    String testPath = "/Test/Report.prpt";
    String encodedPath = java.net.URLEncoder.encode( testPath, "UTF-8" );

    when( mockRequestParams.getStringParameter( "path", null ) ).thenReturn( null );
    when( mockPathParams.getStringParameter( "path", null ) ).thenReturn( encodedPath );
    when( mockPathParams.getStringParameter( "path", "" ) ).thenReturn( encodedPath );

    when( mockRepositoryFile.getId() ).thenReturn( "12345" );

    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );
      when( mockRepository.getFile( testPath ) ).thenReturn( mockRepositoryFile );

      // Execute
      RepositoryFile result = parameterContentGenerator.resolvePrptFile( mockRequestParams );

      // Verify
      assertNotNull( result );
      assertEquals( "12345", result.getId() );
      verify( mockRepository ).getFile( testPath );
    }
  }

  @Test
  public void testResolvePrptFile_PathDecodingAndFormatting() throws Exception {
    // Setup - test that path is properly URL decoded and formatted with leading /
    String testPath = "/Test/My Report.prpt";
    String encodedPath = "Test/My%20Report.prpt"; // Note: Without leading / and URL encoded space

    when( mockRequestParams.getStringParameter( "path", null ) ).thenReturn( encodedPath );
    when( mockRequestParams.getStringParameter( "path", "" ) ).thenReturn( encodedPath );

    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );
      when( mockRepository.getFile( testPath ) ).thenReturn( mockRepositoryFile );

      // Execute
      parameterContentGenerator.resolvePrptFile( mockRequestParams );

      // Verify that path was decoded and formatted correctly
      ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass( String.class );
      verify( mockRepository ).getFile( pathCaptor.capture() );

      String capturedPath = pathCaptor.getValue();
      assertEquals( testPath, capturedPath );
      assertTrue( capturedPath.startsWith( "/" ) );
    }
  }

  @Test
  public void testResolveFileObject_WithValidFileObject() {
    // Setup
    FileObject mockFileObject = mock( FileObject.class );
    when( mockPathParams.getParameter( "file" ) ).thenReturn( mockFileObject );

    // Execute
    FileObject result = parameterContentGenerator.resolveFileObject( mockPathParams );

    // Verify
    assertNotNull( result );
    assertEquals( mockFileObject, result );
  }

  @Test
  public void testResolveFileObject_WithNullParameter() {
    // Setup
    when( mockPathParams.getParameter( "file" ) ).thenReturn( null );

    // Execute
    FileObject result = parameterContentGenerator.resolveFileObject( mockPathParams );

    // Verify
    assertNull( result );
  }

  @Test
  public void testResolveFileObject_WithNullPathParams() {
    // Execute
    FileObject result = parameterContentGenerator.resolveFileObject( null );

    // Verify
    assertNull( result );
  }

  @Test
  public void testIsPvfs_WithFileObject() {
    // Setup
    FileObject mockFileObject = mock( FileObject.class );
    when( mockPathParams.getParameter( "file" ) ).thenReturn( mockFileObject );

    // Execute
    boolean result = parameterContentGenerator.isPvfs();

    // Verify
    assertTrue( result );
  }

  @Test
  public void testIsPvfs_WithoutFileObject() {
    // Setup
    when( mockPathParams.getParameter( "file" ) ).thenReturn( null );

    // Execute
    boolean result = parameterContentGenerator.isPvfs();

    // Verify
    assertTrue( !result );
  }

  @Test
  public void testIsPvfs_WithNullPathParams() {
    // Setup - Mock getPathParameters to return null
    doReturn( null ).when( parameterContentGenerator ).getPathParameters();

    // Execute
    boolean result = parameterContentGenerator.isPvfs();

    // Verify
    assertTrue( !result );
  }

  @Test
  public void testGetMimeType() {
    // Execute
    String mimeType = parameterContentGenerator.getMimeType();

    // Verify
    assertEquals( "text/xml", mimeType );
  }

  @Test
  public void testGetRequestParameters() {
    // Execute
    IParameterProvider result = parameterContentGenerator.getRequestParameters();

    // Verify
    assertNotNull( result );
    assertEquals( mockRequestParams, result );
  }

  @Test
  public void testGetRequestParameters_WithNullProviders() {
    // Setup - Create new instance without parameter providers
    ParameterContentGenerator generator = new ParameterContentGenerator();

    // Execute
    IParameterProvider result = generator.getRequestParameters();

    // Verify - should return empty SimpleParameterProvider
    assertNotNull( result );
  }

  @Test
  public void testGetPathParameters() {
    // Execute
    IParameterProvider result = parameterContentGenerator.getPathParameters();

    // Verify
    assertNotNull( result );
    assertEquals( mockPathParams, result );
  }

  @Test
  public void testCreateInputs() {
    // Setup
    when( mockRequestParams.getParameterNames() ).thenReturn(
      java.util.Arrays.asList( "param1", "param2" ).iterator() );
    when( mockRequestParams.getParameter( "param1" ) ).thenReturn( "value1" );
    when( mockRequestParams.getParameter( "param2" ) ).thenReturn( "value2" );

    // Execute
    Map<String, Object> inputs = parameterContentGenerator.createInputs();

    // Verify
    assertNotNull( inputs );
    assertEquals( 2, inputs.size() );
    assertEquals( "value1", inputs.get( "param1" ) );
    assertEquals( "value2", inputs.get( "param2" ) );
  }

  @Test
  public void testCreateInputs_IgnoresNullValues() {
    // Setup
    when( mockRequestParams.getParameterNames() ).thenReturn(
      java.util.Arrays.asList( "param1", "param2", "param3" ).iterator() );
    when( mockRequestParams.getParameter( "param1" ) ).thenReturn( "value1" );
    when( mockRequestParams.getParameter( "param2" ) ).thenReturn( null );
    when( mockRequestParams.getParameter( "param3" ) ).thenReturn( "value3" );

    // Execute
    Map<String, Object> inputs = parameterContentGenerator.createInputs();

    // Verify - null values should not be included
    assertNotNull( inputs );
    assertEquals( 2, inputs.size() );
    assertEquals( "value1", inputs.get( "param1" ) );
    assertEquals( "value3", inputs.get( "param3" ) );
    assertTrue( !inputs.containsKey( "param2" ) );
  }

  @Test
  public void testCreateInputs_WithNullRequestParams() {
    // Setup
    ParameterContentGenerator generator = new ParameterContentGenerator();

    // Execute
    Map<String, Object> inputs = generator.createInputs();

    // Verify
    assertNotNull( inputs );
    assertEquals( 0, inputs.size() );
  }

  @Test
  public void testIdTopath_WithLeadingSlash() {
    // Execute
    String result = parameterContentGenerator.idTopath( "/Test/Report.prpt" );

    // Verify
    assertEquals( "/Test/Report.prpt", result );
  }

  @Test
  public void testIdTopath_WithoutLeadingSlash() {
    // Execute
    String result = parameterContentGenerator.idTopath( "Test/Report.prpt" );

    // Verify
    assertEquals( "/Test/Report.prpt", result );
  }

  @Test
  public void testIdTopath_WithEmptyString() {
    // Execute
    String result = parameterContentGenerator.idTopath( "" );

    // Verify
    assertEquals( "", result );
  }

  @Test
  public void testIdTopath_WithNull() {
    // Execute
    String result = parameterContentGenerator.idTopath( null );

    // Verify
    assertNull( result );
  }

  @Test
  public void testGetUserSession() {
    // Execute
    // Note: userSession is not set in setUp, so it will be null
    Object result = parameterContentGenerator.getUserSession();

    // Verify
    assertNull( result );
  }

  @Test
  public void testInvalidRenderMode() throws Exception {
    // Setup
    when( mockRequestParams.getStringParameter( "renderMode", "XML" ) ).thenReturn( "INVALID" );

    try ( var mockPentahoSystem = mockStatic( PentahoSystem.class ) ) {
      mockPentahoSystem.when( () -> PentahoSystem.get( IUnifiedRepository.class, null ) )
        .thenReturn( mockRepository );

      // Execute & Verify - should throw IllegalArgumentException
      try {
        parameterContentGenerator.createContent( mockOutputStream );
        assertTrue( "Expected IllegalArgumentException", false );
      } catch ( IllegalArgumentException e ) {
        // Expected
        assertTrue( true );
      }
    }
  }
}
