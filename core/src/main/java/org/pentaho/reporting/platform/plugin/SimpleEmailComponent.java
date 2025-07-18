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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import jakarta.activation.DataHandler;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Node;
import org.pentaho.platform.api.engine.IAcceptsRuntimeInputs;
import org.pentaho.platform.api.repository.IContentItem;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.reporting.platform.plugin.messages.Messages;

/**
 * Creation-Date: 27.09.2009, 00:30:00
 * 
 * @author Pedro Alves - WebDetails
 */
public class SimpleEmailComponent implements IAcceptsRuntimeInputs {

  /**
   * The logging for logging messages from this component
   */
  private static final Log log = LogFactory.getLog( SimpleEmailComponent.class );
  private static final String MAILER = "smtpsend"; //$NON-NLS-1$
  private Map<String, Object> inputs;
  private String outputType;

  // Inputs
  public static final String INPUT_TO = "to";
  public static final String INPUT_FROM = "from";
  public static final String INPUT_CC = "cc";
  public static final String INPUT_BCC = "bcc";
  public static final String INPUT_SUBJECT = "subject";
  public static final String INPUT_MESSAGEPLAIN = "message-plain";
  public static final String INPUT_MESSAGEHTML = "message-html";
  public static final String INPUT_MIMEMESSAGE = "mime-message";

  // beans
  private String to;
  private String from;
  private String cc;
  private String bcc;
  private String subject;
  private Object messagePlain;
  private Object messageHtml;
  private IContentItem mimeMessage;
  private String attachmentName;
  private IContentItem attachmentContent;
  private String attachmentName2;
  private IContentItem attachmentContent2;
  private String attachmentName3;

  /*
   * Default constructor
   */
  public SimpleEmailComponent() {
  }

  // ----------------------------------------------------------------------------
  // BEGIN BEAN METHODS
  // ----------------------------------------------------------------------------
  public String getBcc() {
    return bcc;
  }

  public void setBcc( final String bcc ) {
    this.bcc = bcc;
  }

  public String getCc() {
    return cc;
  }

  public void setCc( final String cc ) {
    this.cc = cc;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom( final String from ) {
    this.from = from;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject( final String subject ) {
    this.subject = subject;
  }

  public String getTo() {
    return to;
  }

  public void setTo( final String to ) {
    this.to = to;
  }

  public Object getMessageHtml() {
    return messageHtml;
  }

  public void setMessageHtml( final Object messageHtml ) {
    this.messageHtml = messageHtml;
  }

  public Object getMessagePlain() {
    return messagePlain;
  }

  public void setMessagePlain( final Object messagePlain ) {
    this.messagePlain = messagePlain;
  }

  public IContentItem getMimeMessage() {
    return mimeMessage;
  }

  public void setMimeMessage( final IContentItem mimeMessage ) {
    this.mimeMessage = mimeMessage;
  }

  public IContentItem getAttachmentContent() {
    return attachmentContent;
  }

  public void setAttachmentContent( final IContentItem attachmentContent ) {
    this.attachmentContent = attachmentContent;
  }

  public String getAttachmentName() {
    return attachmentName;
  }

  public void setAttachmentName( final String attachmentName ) {
    this.attachmentName = attachmentName;
  }

  public IContentItem getAttachmentContent2() {
    return attachmentContent2;
  }

  public void setAttachmentContent2( final IContentItem attachmentContent2 ) {
    this.attachmentContent2 = attachmentContent2;
  }

  public IContentItem getAttachmentContent3() {
    return attachmentContent3;
  }

  public void setAttachmentContent3( final IContentItem attachmentContent3 ) {
    this.attachmentContent3 = attachmentContent3;
  }

  public String getAttachmentName2() {
    return attachmentName2;
  }

  public void setAttachmentName2( final String attachmentName2 ) {
    this.attachmentName2 = attachmentName2;
  }

  public String getAttachmentName3() {
    return attachmentName3;
  }

  public void setAttachmentName3( final String attachmentName3 ) {
    this.attachmentName3 = attachmentName3;
  }

  private IContentItem attachmentContent3;

  /**
   * Sets the mime-type for determining which report output type to generate. This should be a mime-type for consistency
   * with streaming output mime-types.
   * 
   * @param outputType
   *          the desired output type (mime-type) for the report engine to generate
   */
  public void setOutputType( final String outputType ) {
    this.outputType = outputType;
  }

  /**
   * Gets the output type, this should be a mime-type for consistency with streaming output mime-types.
   * 
   * @return the current output type for the report
   */
  public String getOutputType() {
    return outputType;
  }

  /**
   * This method sets the map of *all* the inputs which are available to this component. This allows us to use
   * action-sequence inputs as parameters for our reports.
   * 
   * @param inputs
   *          a Map containing inputs
   */
  public void setInputs( final Map<String, Object> inputs ) {
    this.inputs = inputs;

    if ( inputs.containsKey( INPUT_FROM ) ) {
      setFrom( (String) inputs.get( INPUT_FROM ) );
    }
    if ( inputs.containsKey( INPUT_TO ) ) {
      setTo( (String) inputs.get( INPUT_TO ) );
    }
    if ( inputs.containsKey( INPUT_CC ) ) {
      setCc( (String) inputs.get( INPUT_CC ) );
    }
    if ( inputs.containsKey( INPUT_BCC ) ) {
      setBcc( (String) inputs.get( INPUT_BCC ) );
    }
    if ( inputs.containsKey( INPUT_SUBJECT ) ) {
      setSubject( (String) inputs.get( INPUT_SUBJECT ) );
    }
    if ( inputs.containsKey( INPUT_MESSAGEPLAIN ) ) {
      setMessagePlain( (String) inputs.get( INPUT_MESSAGEPLAIN ) );
    }
    if ( inputs.containsKey( INPUT_MESSAGEHTML ) ) {
      setMessageHtml( (String) inputs.get( INPUT_MESSAGEHTML ) );
    }
    if ( inputs.containsKey( INPUT_MIMEMESSAGE ) ) {
      setMimeMessage( (IContentItem) inputs.get( INPUT_MIMEMESSAGE ) );
    }
  }

  // ----------------------------------------------------------------------------
  // END BEAN METHODS
  // ----------------------------------------------------------------------------
  protected Object getInput( final String key, final Object defaultValue ) {
    if ( inputs != null ) {
      final Object input = inputs.get( key );
      if ( input != null ) {
        return input;
      }
    }
    return defaultValue;
  }

  /**
   * This method will determine if the component instance 'is valid.' The validate() is called after all of the bean
   * 'setters' have been called, so we may validate on the actual values, not just the presence of inputs as we were
   * historically accustomed to.
   * <p/>
   * Since we should have a list of all action-sequence inputs, we can determine if we have sufficient inputs to meet
   * the parameter requirements This would include validation of values and ranges of values.
   * 
   * @return true if valid
   * @throws Exception
   */
  public boolean validate() throws Exception {

    boolean result = true;

    if ( getTo() == null ) {
      log.error( Messages.getInstance().getString( "ReportPlugin.emailToNotProvided" ) ); //$NON-NLS-1$
      return false;

    } else if ( getFrom() == null ) {
      log.error( Messages.getInstance().getString( "ReportPlugin.emailFromNotProvided" ) ); //$NON-NLS-1$
      return false;

    } else if ( ( getMessagePlain() == null ) && ( getMessageHtml() == null ) && ( getMimeMessage() == null ) ) {
      log.error( Messages.getInstance().getString( "ReportPlugin.emailContentNotProvided" ) ); //$NON-NLS-1$
      result = false;
    }

    return result;

  }

  /**
   * Perform the primary function of this component, this is, to execute. This method will be invoked immediately
   * following a successful validate().
   * <p/>
   * This method has 2 ways of working:
   * <p/>
   * 1. You supply a mimeMessage: That mimeMessage will be sent; Optionally, contents will be added as attachment and
   * the original mimeMessage will be encapsulated under a multipart/mixed
   * <p/>
   * <p/>
   * 2. You supply a messageHtml and/or a messageText. A new mimemessage will be built. If you supply both, a
   * multipart/alternative will be used. After that attachments will be included
   * 
   * @return true if successful execution
   * @throws Exception
   */
  public boolean execute() throws Exception {

    try {

      // Get the session object
      final Session session = buildSession();

      // Create the message
      final MimeMessage msg = new MimeMessage( session );

      // From, to, etc.
      applyMessageHeaders( msg );

      // Get main message multipart
      final Multipart multipartBody = getMultipartBody( session );

      // Process attachments
      final Multipart mainMultiPart = processAttachments( multipartBody );
      msg.setContent( mainMultiPart );

      // Send it

      msg.setHeader( "X-Mailer", MAILER ); //$NON-NLS-1$
      msg.setSentDate( new Date() );

      Transport.send( msg );

      return true;

    } catch ( SendFailedException e ) {
      log.error( Messages.getInstance().getString( "ReportPlugin.emailSendFailed" ) ); //$NON-NLS-1$
    } catch ( AuthenticationFailedException e ) {
      log.error( Messages.getInstance().getString( "ReportPlugin.emailAuthenticationFailed" ) ); //$NON-NLS-1$
    }
    return false;

  }

  private Multipart getMultipartBody( final Session session ) throws MessagingException, IOException {

    // if we have a mimeMessage, use it. Otherwise, build one with what we have
    // We can have both a messageHtml and messageText. Build according to it

    MimeMultipart parentMultipart = new MimeMultipart();
    MimeBodyPart htmlBodyPart = null, textBodyPart = null;

    if ( getMimeMessage() != null ) {

      // Rebuild a MimeMessage and use this one

      final MimeBodyPart original = new MimeBodyPart();
      final MimeMessage originalMimeMessage = new MimeMessage( session, getMimeMessage().getInputStream() );
      final MimeMultipart relatedMultipart = (MimeMultipart) originalMimeMessage.getContent();

      parentMultipart = relatedMultipart;

      htmlBodyPart = new MimeBodyPart();
      htmlBodyPart.setContent( relatedMultipart );

    }

    // The information we have in the mime-message overrides the getMessageHtml.
    if ( getMessageHtml() != null && htmlBodyPart != null ) {

      final String content = getInputString( getMessageHtml() );

      htmlBodyPart = new MimeBodyPart();
      htmlBodyPart.setContent( content, "text/html; charset=" + LocaleHelper.getSystemEncoding() );
      final MimeMultipart htmlMultipart = new MimeMultipart();
      htmlMultipart.addBodyPart( htmlBodyPart );

      parentMultipart = htmlMultipart;
    }

    if ( getMessagePlain() != null ) {

      final String content = getInputString( getMessagePlain() );

      textBodyPart = new MimeBodyPart();
      textBodyPart.setContent( content, "text/plain; charset=" + LocaleHelper.getSystemEncoding() );
      final MimeMultipart textMultipart = new MimeMultipart();
      textMultipart.addBodyPart( textBodyPart );

      parentMultipart = textMultipart;
    }

    // We have both text and html? Encapsulate it in a multipart/alternative

    if ( htmlBodyPart != null && textBodyPart != null ) {

      final MimeMultipart alternative = new MimeMultipart( "alternative" );
      alternative.addBodyPart( textBodyPart );
      alternative.addBodyPart( htmlBodyPart );

      parentMultipart = alternative;

    }

    return parentMultipart;

  }

  private String getInputString( final Object param ) throws IOException {

    if ( param instanceof String ) {
      return (String) param;
    } else if ( param instanceof IContentItem ) {

      final InputStream in = ( (IContentItem) param ).getInputStream();
      // Convert to String
      final ByteArrayOutputStream out = new ByteArrayOutputStream();

      int nextChar;
      while ( ( nextChar = in.read() ) != -1 ) {
        out.write( nextChar );
      }

      return new String( out.toString( LocaleHelper.getSystemEncoding() ) );

    }

    throw new IllegalStateException( "Input is not a String or ContentItem" );

  }

  private Multipart processAttachments( final Multipart multipartBody ) throws MessagingException, IOException {

    if ( getAttachmentContent() == null ) {

      // We don't have a first attachment, won't even search for the others.
      return multipartBody;

    }

    // We have attachments; Creating a multipart-mixed

    final MimeMultipart mixedMultipart = new MimeMultipart( "mixed" );

    // Add the first part
    final MimeBodyPart bodyPart = new MimeBodyPart();
    bodyPart.setContent( multipartBody );
    mixedMultipart.addBodyPart( bodyPart );

    // Process each of the attachments we have
    processSpecificAttachment( mixedMultipart, getAttachmentContent() );
    processSpecificAttachment( mixedMultipart, getAttachmentContent2() );
    processSpecificAttachment( mixedMultipart, getAttachmentContent3() );

    return mixedMultipart;

  }

  private void processSpecificAttachment( final MimeMultipart mixedMultipart, final IContentItem attachmentContent )
    throws IOException, MessagingException {

    // Add this attachment

    if ( attachmentContent != null ) {

      final ByteArrayDataSource dataSource =
          new ByteArrayDataSource( attachmentContent.getInputStream(), attachmentContent.getMimeType() );
      final MimeBodyPart attachmentBodyPart = new MimeBodyPart();
      attachmentBodyPart.setDataHandler( new DataHandler( dataSource ) );
      attachmentBodyPart.setFileName( getAttachmentName() );
      mixedMultipart.addBodyPart( attachmentBodyPart );

    }

  }

  private void applyMessageHeaders( final MimeMessage msg ) throws Exception {

    msg.setFrom( new InternetAddress( from ) );
    msg.setRecipients( Message.RecipientType.TO, InternetAddress.parse( to, false ) );

    if ( ( cc != null ) && ( cc.trim().length() > 0 ) ) {
      msg.setRecipients( Message.RecipientType.CC, InternetAddress.parse( cc, false ) );
    }
    if ( ( bcc != null ) && ( bcc.trim().length() > 0 ) ) {
      msg.setRecipients( Message.RecipientType.BCC, InternetAddress.parse( bcc, false ) );
    }

    if ( subject != null ) {
      msg.setSubject( subject, LocaleHelper.getSystemEncoding() );
    }

  }

  private Session buildSession() throws Exception {

    final Properties props = new Properties();

    try {
      final Document configDocument =
          PentahoSystem.getSystemSettings().getSystemSettingsDocument( "smtp-email/email_config.xml" ); //$NON-NLS-1$
      final List properties = configDocument.selectNodes( "/email-smtp/properties/*" ); //$NON-NLS-1$
      final Iterator propertyIterator = properties.iterator();
      while ( propertyIterator.hasNext() ) {
        final Node propertyNode = (Node) propertyIterator.next();
        final String propertyName = propertyNode.getName();
        final String propertyValue = propertyNode.getText();
        props.put( propertyName, propertyValue );
      }
    } catch ( Exception e ) {
      log.error( Messages.getInstance().getString( "ReportPlugin.emailConfigFileInvalid" ) ); //$NON-NLS-1$
      throw e;
    }

    final boolean authenticate = "true".equals( props.getProperty( "mail.smtp.auth" ) ); //$NON-NLS-1$//$NON-NLS-2$

    // Get a Session object

    final Session session;
    if ( authenticate ) {
      final Authenticator authenticator = new EmailAuthenticator();
      session = Session.getInstance( props, authenticator );
    } else {
      session = Session.getInstance( props );
    }

    // if debugging is not set in the email config file, match the
    // component debug setting
    if ( !props.containsKey( "mail.debug" ) ) { //$NON-NLS-1$
      session.setDebug( true );
    }

    return session;

  }

  /**
   * This method returns the output-type for the streaming output, it is the same as what is returned by getOutputType()
   * for consistency.
   * 
   * @return the mime-type for the streaming output
   */
  public String getMimeType() {
    return outputType;
  }

  private static class EmailAuthenticator extends Authenticator {

    private EmailAuthenticator() {
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      final String user = PentahoSystem.getSystemSetting( "smtp-email/email_config.xml", "mail.userid", null ); //$NON-NLS-1$ //$NON-NLS-2$
      final String password = PentahoSystem.getSystemSetting( "smtp-email/email_config.xml", "mail.password", null ); //$NON-NLS-1$ //$NON-NLS-2$
      return new PasswordAuthentication( user, password );
    }
  }
}
