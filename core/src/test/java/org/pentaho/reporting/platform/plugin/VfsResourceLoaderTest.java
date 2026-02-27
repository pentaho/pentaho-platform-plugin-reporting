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

import junit.framework.TestCase;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.libraries.resourceloader.FactoryParameterKey;
import org.pentaho.reporting.libraries.resourceloader.ParameterKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceData;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceKeyCreationException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VfsResourceLoaderTest extends TestCase {
  private VfsResourceLoader vfsResourceLoader;

  @Before
    @Override
  public void setUp() {
    vfsResourceLoader = new VfsResourceLoader();
  }

  @Test
    @SuppressWarnings( "unchecked" )
  public void testConstructor() {
    VfsResourceLoader loader = new VfsResourceLoader();
    assertNotNull( loader );
  }

  @Test
  public void testSetResourceManager() {
    ResourceManager manager = new ResourceManager();
    vfsResourceLoader.setResourceManager( manager );
    assertEquals( manager, vfsResourceLoader.getManager() );
  }

  @Test
  public void testGetManager() {
    assertNull( vfsResourceLoader.getManager() );
    ResourceManager manager = new ResourceManager();
    vfsResourceLoader.setResourceManager( manager );
    assertNotNull( vfsResourceLoader.getManager() );
    assertEquals( manager, vfsResourceLoader.getManager() );
  }

  @Test
  public void testGetSchema() {
    assertEquals( "pvfs", vfsResourceLoader.getSchema() );
    assertEquals( VfsResourceLoader.VFS_SCHEMA_NAME, vfsResourceLoader.getSchema() );
  }

  @Test
  public void testLoad() throws Exception {
    ResourceKey key = new ResourceKey( "pvfs", "path/to/resource.txt", null );
    ResourceData result = vfsResourceLoader.load( key );
    assertNotNull( result );
    assertEquals( key, result.getKey() );
    assertTrue( result instanceof VfsResourceData );
  }

  @Test
  public void testLoadWithFactoryParameters() throws Exception {
      @SuppressWarnings( "unchecked" )
    Map params = new HashMap<>();
    params.put( "param1", "value1" );
    ResourceKey key = new ResourceKey( "pvfs", "path/to/resource.txt", params );
    ResourceData result = vfsResourceLoader.load( key );
    assertNotNull( result );
    assertEquals( key, result.getKey() );
  }

  @Test
  public void testLoadUsesFileObjectFromFactoryParametersForInputStream() throws Exception {
    FileObject fileObject = mock( FileObject.class );
    FileName fileName = mock( FileName.class );
    FileContent fileContent = mock( FileContent.class );

    byte[] expected = "stream-content-from-fileObject".getBytes( StandardCharsets.UTF_8 );
    InputStream expectedStream = new ByteArrayInputStream( expected );

    when( fileObject.getName() ).thenReturn( fileName );
    when( fileName.getPath() ).thenReturn( "tmp/report.prpt" );
    when( fileObject.exists() ).thenReturn( true );
    when( fileObject.isFile() ).thenReturn( true );
    when( fileObject.isReadable() ).thenReturn( true );
    when( fileObject.getContent() ).thenReturn( fileContent );
    when( fileContent.getInputStream() ).thenReturn( expectedStream );

    Map<ParameterKey, Object> helperObjects = new HashMap<>();
    helperObjects.put( new FactoryParameterKey( "fileObject" ), fileObject );

    ResourceKey key = vfsResourceLoader.createKey(
      VfsResourceLoader.VFS_SCHEMA_NAME + VfsResourceLoader.SCHEMA_SEPARATOR + fileObject.getName().getPath(),
      helperObjects );

    ResourceData resourceData = vfsResourceLoader.load( key );
    assertTrue( resourceData instanceof VfsResourceData );

    byte[] actual = ( (VfsResourceData) resourceData ).getResourceAsStream( new ResourceManager() ).readAllBytes();
    assertEquals( new String( expected, StandardCharsets.UTF_8 ), new String( actual, StandardCharsets.UTF_8 ) );
  }

  @Test
  public void testIsSupportedKey() {
    ResourceKey supportedKey = new ResourceKey( "pvfs", "path/to/resource.txt", null );
    assertTrue( vfsResourceLoader.isSupportedKey( supportedKey ) );

    ResourceKey unsupportedKey = new ResourceKey( "http", "path/to/resource.txt", null );
    assertFalse( vfsResourceLoader.isSupportedKey( unsupportedKey ) );
    
    ResourceKey anotherUnsupportedKey = new ResourceKey( "", "path/to/resource.txt", null );
    assertFalse( vfsResourceLoader.isSupportedKey( anotherUnsupportedKey ) );
  }

  @Test
  public void testCreateKeyWithValidString() throws Exception {
    String value = "pvfs://path/to/resource.txt";
    ResourceKey key = vfsResourceLoader.createKey( value, null );
    assertNotNull( key );
    assertEquals( "pvfs", key.getSchema() );
    assertEquals( "path/to/resource.txt", key.getIdentifier() );
  }

  @Test
  public void testCreateKeyWithFactoryKeys() throws Exception {
    @SuppressWarnings( "unchecked" )
    String value = "pvfs://path/to/resource.txt";
    Map factoryKeys = new HashMap<>();
    factoryKeys.put( "key1", "value1" );
    factoryKeys.put( "key2", "value2" );
    
    ResourceKey key = vfsResourceLoader.createKey( value, factoryKeys );
    assertNotNull( key );
    assertEquals( "pvfs", key.getSchema() );
    assertEquals( "path/to/resource.txt", key.getIdentifier() );
    assertEquals( factoryKeys, key.getFactoryParameters() );
  }

  @Test
  public void testCreateKeyWithInvalidString() throws Exception {
    String value = "http://path/to/resource.txt";
    ResourceKey key = vfsResourceLoader.createKey( value, null );
    assertNull( key );
  }

  @Test
  public void testCreateKeyWithNonStringValue() throws Exception {
    Integer value = 123;
    ResourceKey key = vfsResourceLoader.createKey( value, null );
    assertNull( key );
  }

  @Test
  public void testCreateKeyWithEmptyString() throws Exception {
    String value = "";
    ResourceKey key = vfsResourceLoader.createKey( value, null );
    assertNull( key );
  }

  @Test
  public void testDeriveKeyWithRelativePath() throws Exception {
    ResourceKey parent = new ResourceKey( "pvfs", "path/to/parent.txt", null );
    String relativePath = "child.txt";
    
    ResourceKey derivedKey = vfsResourceLoader.deriveKey( parent, relativePath, null );
    assertNotNull( derivedKey );
    assertEquals( "pvfs", derivedKey.getSchema() );
    assertEquals( "path/to/child.txt", derivedKey.getIdentifier() );
  }

  @Test
  public void testDeriveKeyWithRelativePathAndWindowsSeparator() throws Exception {
    ResourceKey parent = new ResourceKey( "pvfs", "path\\to\\parent.txt", null );
    String relativePath = "child.txt";
    
    ResourceKey derivedKey = vfsResourceLoader.deriveKey( parent, relativePath, null );
    assertNotNull( derivedKey );
    assertEquals( "pvfs", derivedKey.getSchema() );
    assertEquals( "path\\to\\child.txt", derivedKey.getIdentifier() );
  }

  @Test
  public void testDeriveKeyWithAbsolutePath() throws Exception {
    ResourceKey parent = new ResourceKey( "pvfs", "path/to/parent.txt", null );
    String absolutePath = "pvfs://another/path/resource.txt";
    
    ResourceKey derivedKey = vfsResourceLoader.deriveKey( parent, absolutePath, null );
    assertNotNull( derivedKey );
    assertEquals( "pvfs", derivedKey.getSchema() );
    assertEquals( "another/path/resource.txt", derivedKey.getIdentifier() );
  }

  @Test
  public void testDeriveKeyWithFactoryParameters() throws Exception {
      @SuppressWarnings( "unchecked" )
    Map parentParams = new HashMap<>();
    parentParams.put( "parentKey", "parentValue" );
    ResourceKey parent = new ResourceKey( "pvfs", "path/to/parent.txt", parentParams );
    
    Map derivedParams = new HashMap<>();
    derivedParams.put( "derivedKey", "derivedValue" );
    
    String relativePath = "child.txt";
    ResourceKey derivedKey = vfsResourceLoader.deriveKey( parent, relativePath, derivedParams );
    
    assertNotNull( derivedKey );
    assertEquals( "pvfs", derivedKey.getSchema() );
    assertEquals( "path/to/child.txt", derivedKey.getIdentifier() );
    
    Map<?, ?> factoryParams = derivedKey.getFactoryParameters();
    assertTrue( factoryParams.containsKey( "parentKey" ) );
    assertTrue( factoryParams.containsKey( "derivedKey" ) );
    assertEquals( "parentValue", factoryParams.get( "parentKey" ) );
    assertEquals( "derivedValue", factoryParams.get( "derivedKey" ) );
  }

  @Test
  public void testDeriveKeyPreservesParentParameters() throws Exception {
    Map parentParams = new HashMap<>();
    parentParams.put( "key1", "value1" );
    ResourceKey parent = new ResourceKey( "pvfs", "path/to/parent.txt", parentParams );
    
    String relativePath = "child.txt";
    ResourceKey derivedKey = vfsResourceLoader.deriveKey( parent, relativePath, null );
    
    assertEquals( "value1", derivedKey.getFactoryParameters().get( "key1" ) );
  }

  @Test
  public void testDeserialize() {
    ResourceKey bundleKey = new ResourceKey( "pvfs", "bundle", null );
    String stringKey = "pvfs://path/to/resource.txt";
    
    try {
      vfsResourceLoader.deserialize( bundleKey, stringKey );
      fail( "Expected ResourceKeyCreationException" );
    } catch ( ResourceKeyCreationException ex ) {
      // Expected exception
      assertTrue( true );
    }
  }

  @Test
  public void testSerialize() {
    ResourceKey bundleKey = new ResourceKey( "pvfs", "bundle", null );
    ResourceKey key = new ResourceKey( "pvfs", "path/to/resource.txt", null );
    
    try {
      vfsResourceLoader.serialize( bundleKey, key );
      fail( "Expected ResourceKeyCreationException" );
    } catch ( Exception ex ) {
      assertTrue( ex instanceof ResourceKeyCreationException );
    }
  }

  @Test
  public void testToURL() {
    ResourceKey key = new ResourceKey( "pvfs", "path/to/resource.txt", null );
    assertNull( vfsResourceLoader.toURL( key ) );
  }

  @Test
  public void testIsSupportedDeserializer() {
    assertFalse( vfsResourceLoader.isSupportedDeserializer( "pvfs://path/to/resource.txt" ) );
    assertFalse( vfsResourceLoader.isSupportedDeserializer( "" ) );
    assertFalse( vfsResourceLoader.isSupportedDeserializer( "http://example.com" ) );
  }

  @Test
  public void testSchemaConstants() {
    assertEquals( "pvfs", VfsResourceLoader.VFS_SCHEMA_NAME );
    assertEquals( "://", VfsResourceLoader.SCHEMA_SEPARATOR );
    assertEquals( "/", VfsResourceLoader.PATH_SEPARATOR );
    assertEquals( "\\", VfsResourceLoader.WIN_PATH_SEPARATOR );
  }

  @Test
  public void testDeriveKeyWithMixedSeparators() throws Exception {
    // Test with Unix-style path in parent
    ResourceKey parentUnix = new ResourceKey( "pvfs", "path/to/parent.txt", null );
    String relativePath = "subfolder/child.txt";
    ResourceKey derivedKey = vfsResourceLoader.deriveKey( parentUnix, relativePath, null );
    assertEquals( "path/to/subfolder/child.txt", derivedKey.getIdentifier() );
    
    // Test with Windows-style path in parent
    ResourceKey parentWindows = new ResourceKey( "pvfs", "path\\to\\parent.txt", null );
    ResourceKey derivedKeyWindows = vfsResourceLoader.deriveKey( parentWindows, relativePath, null );
    assertEquals( "path\\to\\subfolder/child.txt", derivedKeyWindows.getIdentifier() );
  }

  @Test
  public void testDeriveKeyWithRootPath() throws Exception {
    ResourceKey parent = new ResourceKey( "pvfs", "/parent.txt", null );
    String relativePath = "child.txt";
    
    ResourceKey derivedKey = vfsResourceLoader.deriveKey( parent, relativePath, null );
    assertEquals( "/child.txt", derivedKey.getIdentifier() );
  }
}
