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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.repository.IContentItem;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class SimpleEmailComponentTest extends TestCase {
  SimpleEmailComponent sec;

  @Before
  public void setUp() throws Exception {
    sec = new SimpleEmailComponent();
  }

  @Test
  public void testSetBcc() throws Exception {
    sec.setBcc( "bcc" );
    assertEquals( "bcc", sec.getBcc() );
  }

  @Test
  public void testSetCc() throws Exception {
    sec.setCc( "cc" );
    assertEquals( "cc", sec.getCc() );
  }

  @Test
  public void testSetFrom() throws Exception {
    sec.setFrom( "from" );
    assertEquals( "from", sec.getFrom() );
  }

  @Test
  public void testSetSubject() throws Exception {
    sec.setSubject( "subject" );
    assertEquals( "subject", sec.getSubject() );
  }

  @Test
  public void testSetTo() throws Exception {
    sec.setTo( "to" );
    assertEquals( "to", sec.getTo() );
  }

  @Test
  public void testSetMessageHtml() throws Exception {
    sec.setMessageHtml( "messageHtml" );
    assertEquals( "messageHtml", sec.getMessageHtml() );
  }

  @Test
  public void testSetMessagePlain() throws Exception {
    sec.setMessagePlain( "messagePlain" );
    assertEquals( "messagePlain", sec.getMessagePlain() );
  }

  @Test
  public void testSetMimeMessage() throws Exception {
    IContentItem mimeMessage = mock( IContentItem.class );
    sec.setMimeMessage( mimeMessage );
    assertEquals( mimeMessage, sec.getMimeMessage() );
  }

  @Test
  public void testSetAttachmentContent() throws Exception {
    IContentItem attachmentContent = mock( IContentItem.class );
    sec.setAttachmentContent( attachmentContent );
    assertEquals( attachmentContent, sec.getAttachmentContent() );
  }

  @Test
  public void testSetAttachmentContent2() throws Exception {
    IContentItem attachmentContent = mock( IContentItem.class );
    sec.setAttachmentContent2( attachmentContent );
    assertEquals( attachmentContent, sec.getAttachmentContent2() );
  }

  @Test
  public void testSetAttachmentContent3() throws Exception {
    IContentItem attachmentContent = mock( IContentItem.class );
    sec.setAttachmentContent3( attachmentContent );
    assertEquals( attachmentContent, sec.getAttachmentContent3() );
  }

  @Test
  public void testSetAttachmentName() throws Exception {
    sec.setAttachmentName( "attachmentName" );
    assertEquals( "attachmentName", sec.getAttachmentName() );
  }

  @Test
  public void testSetAttachmentName2() throws Exception {
    sec.setAttachmentName2( "attachmentName" );
    assertEquals( "attachmentName", sec.getAttachmentName2() );
  }

  @Test
  public void testSetAttachmentName3() throws Exception {
    sec.setAttachmentName3( "attachmentName" );
    assertEquals( "attachmentName", sec.getAttachmentName3() );
  }

  @Test
  public void testSetOutputYpe() throws Exception {
    sec.setOutputType( "outputType" );
    assertEquals( "outputType", sec.getOutputType() );
  }

  @Test
  public void testSetInputs() throws Exception {
    final IContentItem contentItem = mock( IContentItem.class );
    Map<String, Object> inputs = new HashMap<String, Object>() {{
        put( "from", "fromValue" );
        put( "to", "toValue" );
        put( "cc", "ccValue" );
        put( "bcc", "bccValue" );
        put( "subject", "subjectValue" );
        put( "message-plain", "message-plainValue" );
        put( "message-html", "message-htmlValue" );
        put( "mime-message", contentItem );
    }};
    sec.setInputs( inputs );

    for ( int i = 0; i < inputs.size(); i++ ) {
      String key = inputs.keySet().toArray()[ i ].toString();
      assertEquals( inputs.get( key ), sec.getInput( key, "" ) );
    }
  }

  @Test
  public void testValidate() throws Exception {
    assertFalse( sec.validate() );
    sec.setTo( "to" );
    assertFalse( sec.validate() );
    sec.setFrom( "from" );
    assertFalse( sec.validate() );
    sec.setMessagePlain( "messagePlain" );
    sec.setMessageHtml( "messageHtml" );
    sec.setMimeMessage( mock( IContentItem.class ) );
    assertTrue( sec.validate() );
  }
}
