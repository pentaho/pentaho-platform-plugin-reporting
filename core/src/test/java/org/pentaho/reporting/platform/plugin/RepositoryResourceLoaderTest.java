package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.libraries.resourceloader.ResourceData;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceKeyCreationException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

import java.util.HashMap;
import java.util.Map;

public class RepositoryResourceLoaderTest extends TestCase {
  RepositoryResourceLoader rrl;

  @Before
  public void setUp() {
    rrl = new RepositoryResourceLoader();
  }

  @Test
  public void testSetResourceManager() throws Exception {
    ResourceManager manager = new ResourceManager();
    rrl.setResourceManager( manager );
    assertEquals( manager, rrl.getManager() );
  }

  @Test
  public void testGetSchema() throws Exception {
    assertEquals( rrl.SOLUTION_SCHEMA_NAME, rrl.getSchema() );
  }

  @Test
  public void testLoad() throws Exception {
    ResourceKey key = new ResourceKey( "", "", null );
    ResourceData result = rrl.load( key );
    assertEquals( key, result.getKey() );
  }

  @Test
  public void testIsSupportedKey() throws Exception {
    ResourceKey key = new ResourceKey( "", "", null );
    rrl.load( key );
    assertFalse( rrl.isSupportedKey( key ) );

    key = new ResourceKey( rrl.SOLUTION_SCHEMA_NAME, "", null );
    rrl.load( key );
    assertTrue( rrl.isSupportedKey( key ) );
  }

  @Test
  public void testCreateKey() throws Exception {
    assertNull( rrl.createKey( 0, null ) );
    Map<String, Object> keys = new HashMap<String, Object>() {{
        put( "key", "value" );
    }};
    ResourceKey key = rrl.createKey( rrl.SOLUTION_SCHEMA_NAME + rrl.SCHEMA_SEPARATOR, keys );
    assertNotNull( key );
    assertEquals( rrl.SOLUTION_SCHEMA_NAME, key.getSchema() );
  }

  @Test
  public void testDeriveKey() throws Exception {
    ResourceKey key = new ResourceKey( "", "", null );
    Map<String, Object> keys = new HashMap<String, Object>() {{
        put( "key", "value" );
    }};
    ResourceKey derivedKey = rrl.deriveKey( key, "", keys );
    assertEquals( rrl.SOLUTION_SCHEMA_NAME, derivedKey.getSchema() );
    assertEquals( "", derivedKey.getIdentifierAsString() );

    String path = rrl.SOLUTION_SCHEMA_NAME + rrl.SCHEMA_SEPARATOR;

    key = new ResourceKey( rrl.SOLUTION_SCHEMA_NAME + rrl.SCHEMA_SEPARATOR, "/", null );
    derivedKey = rrl.deriveKey( key, path, keys );
    assertEquals( rrl.SOLUTION_SCHEMA_NAME, derivedKey.getSchema() );
    assertEquals( rrl.SOLUTION_SCHEMA_NAME + rrl.SCHEMA_SEPARATOR, derivedKey.getIdentifierAsString() );
  }

  @Test
  public void testDefaults() throws Exception {
    ResourceKey key = new ResourceKey( "", "", null );
    assertNull( rrl.toURL( key ) );

    assertFalse( rrl.isSupportedDeserializer( "" ) );

    try {
      rrl.serialize( key, key );
    } catch ( ResourceKeyCreationException ex ) {
      assertTrue( true );
    }

    try {
      rrl.deserialize( key, "" );
    } catch ( ResourceKeyCreationException ex ) {
      assertTrue( true );
    }
  }
}
