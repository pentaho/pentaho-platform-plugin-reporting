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


package org.pentaho.reporting.platform.plugin.repository;

import junit.framework.TestCase;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.reporting.libraries.repository.Repository;

import java.io.OutputStream;
import java.util.Date;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportContentItemTest extends TestCase {
  ReportContentItem reportContentItem;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;
  Repository repository;

  protected void setUp() {
    repositoryFile = mock( RepositoryFile.class );
    doReturn( "fileName" ).when( repositoryFile ).getName();

    reportContentLocation = mock( ReportContentLocation.class );
    repository = mock( Repository.class );
    doReturn( repository ).when( reportContentLocation ).getRepository();

    reportContentItem = new ReportContentItem( repositoryFile, reportContentLocation, "" );
  }

  public void testSetMimeType() throws Exception {
    assertEquals( "", reportContentItem.getMimeType() );
    String newMimeType = "new-mime-type"; //$NON-NLS-1$
    reportContentItem.setMimeType( newMimeType );
    assertEquals( newMimeType, reportContentItem.getMimeType() );
  }

  public void testGetContentId() throws Exception {
    doReturn( "/home/admin/namewithcolon:/picture118632338.png" ).when( repositoryFile ).getPath();
    String expectedResult = "%3Ahome%3Aadmin%3Anamewithcolon%09%3Apicture118632338.png";

    assertEquals( expectedResult, reportContentItem.getContentId() );
  }

  public void testIsReadable() throws Exception {
    assertFalse( reportContentItem.isReadable() );
  }

  public void testIsWritable() throws Exception {
    assertTrue( reportContentItem.isWriteable() );
  }

  public void testGetParent() throws Exception {
    assertEquals( reportContentLocation, reportContentItem.getParent() );
  }

  public void testSetAttribute() throws Exception {
    assertFalse( reportContentItem.setAttribute( "", "", null ) );
  }

  public void testGetName() throws Exception {
    assertEquals( "fileName", reportContentItem.getName() );
  }

  public void testGetRepository() throws Exception {
    assertEquals( repository, reportContentItem.getRepository() );
  }

  public void testGetAttribute() throws Exception {
    assertNull( reportContentItem.getAttribute( "", "" ) );

    Object result = reportContentItem.getAttribute( "org.jfree.repository", "size" );
    assertEquals( "0", result.toString() );

    Date date = new Date();
    doReturn( date ).when( repositoryFile ).getLastModifiedDate();

    result = reportContentItem.getAttribute( "org.jfree.repository", "version" );
    assertEquals( date, result );
  }
}
