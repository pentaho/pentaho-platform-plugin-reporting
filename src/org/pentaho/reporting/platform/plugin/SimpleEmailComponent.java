package org.pentaho.reporting.platform.plugin;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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
public class SimpleEmailComponent implements IAcceptsRuntimeInputs
{

  /**
   * The logging for logging messages from this component
   */
  private static final Log log = LogFactory.getLog(SimpleEmailComponent.class);
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
  private String messagePlain;
  private String messageHtml;
  private IContentItem mimeMessage;
  private String attachmentName;
  private IContentItem attachmentContent;
  private String attachmentName2;
  private IContentItem attachmentContent2;
  private String attachmentName3;

  /*
  * Default constructor
  */
  public SimpleEmailComponent()
  {
  }

  // ----------------------------------------------------------------------------
  // BEGIN BEAN METHODS
  // ----------------------------------------------------------------------------
  public String getBcc()
  {
    return bcc;
  }

  public void setBcc(final String bcc)
  {
    this.bcc = bcc;
  }

  public String getCc()
  {
    return cc;
  }

  public void setCc(final String cc)
  {
    this.cc = cc;
  }

  public String getFrom()
  {
    return from;
  }

  public void setFrom(final String from)
  {
    this.from = from;
  }

  public String getSubject()
  {
    return subject;
  }

  public void setSubject(final String subject)
  {
    this.subject = subject;
  }

  public String getTo()
  {
    return to;
  }

  public void setTo(final String to)
  {
    this.to = to;
  }

  public String getMessageHtml()
  {
    return messageHtml;
  }

  public void setMessageHtml(final String messageHtml)
  {
    this.messageHtml = messageHtml;
  }

  public String getMessagePlain()
  {
    return messagePlain;
  }

  public void setMessagePlain(final String messagePlain)
  {
    this.messagePlain = messagePlain;
  }

  public IContentItem getMimeMessage()
  {
    return mimeMessage;
  }

  public void setMimeMessage(final IContentItem mimeMessage)
  {
    this.mimeMessage = mimeMessage;
  }

  public IContentItem getAttachmentContent()
  {
    return attachmentContent;
  }

  public void setAttachmentContent(final IContentItem attachmentContent)
  {
    this.attachmentContent = attachmentContent;
  }

  public String getAttachmentName()
  {
    return attachmentName;
  }

  public void setAttachmentName(final String attachmentName)
  {
    this.attachmentName = attachmentName;
  }

  public IContentItem getAttachmentContent2()
  {
    return attachmentContent2;
  }

  public void setAttachmentContent2(final IContentItem attachmentContent2)
  {
    this.attachmentContent2 = attachmentContent2;
  }

  public IContentItem getAttachmentContent3()
  {
    return attachmentContent3;
  }

  public void setAttachmentContent3(final IContentItem attachmentContent3)
  {
    this.attachmentContent3 = attachmentContent3;
  }

  public String getAttachmentName2()
  {
    return attachmentName2;
  }

  public void setAttachmentName2(final String attachmentName2)
  {
    this.attachmentName2 = attachmentName2;
  }

  public String getAttachmentName3()
  {
    return attachmentName3;
  }

  public void setAttachmentName3(final String attachmentName3)
  {
    this.attachmentName3 = attachmentName3;
  }

  private IContentItem attachmentContent3;

  /**
   * Sets the mime-type for determining which report output type to generate. This should be a mime-type for consistency with streaming output mime-types.
   *
   * @param outputType the desired output type (mime-type) for the report engine to generate
   */
  public void setOutputType(final String outputType)
  {
    this.outputType = outputType;
  }

  /**
   * Gets the output type, this should be a mime-type for consistency with streaming output mime-types.
   *
   * @return the current output type for the report
   */
  public String getOutputType()
  {
    return outputType;
  }

  /**
   * This method sets the map of *all* the inputs which are available to this component. This allows us to use action-sequence inputs as parameters for our
   * reports.
   *
   * @param inputs a Map containing inputs
   */
  public void setInputs(final Map<String, Object> inputs)
  {
    this.inputs = inputs;


    if (inputs.containsKey(INPUT_FROM))
    {
      setFrom((String) inputs.get(INPUT_FROM));
    }
    if (inputs.containsKey(INPUT_TO))
    {
      setTo((String) inputs.get(INPUT_TO));
    }
    if (inputs.containsKey(INPUT_CC))
    {
      setCc((String) inputs.get(INPUT_CC));
    }
    if (inputs.containsKey(INPUT_BCC))
    {
      setBcc((String) inputs.get(INPUT_BCC));
    }
    if (inputs.containsKey(INPUT_SUBJECT))
    {
      setSubject((String) inputs.get(INPUT_SUBJECT));
    }
    if (inputs.containsKey(INPUT_MESSAGEPLAIN))
    {
      setMessagePlain((String) inputs.get(INPUT_MESSAGEPLAIN));
    }
    if (inputs.containsKey(INPUT_MESSAGEHTML))
    {
      setMessageHtml((String) inputs.get(INPUT_MESSAGEHTML));
    }
    if (inputs.containsKey(INPUT_MIMEMESSAGE))
    {
      setMimeMessage((IContentItem) inputs.get(INPUT_MIMEMESSAGE));
    }
  }

  // ----------------------------------------------------------------------------
  // END BEAN METHODS
  // ----------------------------------------------------------------------------
  protected Object getInput(final String key, final Object defaultValue)
  {
    if (inputs != null)
    {
      final Object input = inputs.get(key);
      if (input != null)
      {
        return input;
      }
    }
    return defaultValue;
  }

  /**
   * This method will determine if the component instance 'is valid.' The validate() is called after all of the bean 'setters' have been called, so we may
   * validate on the actual values, not just the presence of inputs as we were historically accustomed to.
   * <p/>
   * Since we should have a list of all action-sequence inputs, we can determine if we have sufficient inputs to meet the parameter requirements
   * This would include validation of values and ranges of values.
   *
   * @return true if valid
   * @throws Exception
   */
  public boolean validate() throws Exception
  {


    boolean result = true;

    if (getTo() == null)
    {
      log.error(Messages.getString("ReportPlugin.emailToNotProvided")); //$NON-NLS-1$
      return false;

    }
    else if (getFrom() == null)
    {
      log.error(Messages.getString("ReportPlugin.emailFromNotProvided")); //$NON-NLS-1$
      return false;

    }
    else if ((getMessagePlain() == null) && (getMessageHtml() == null) && (getMimeMessage() == null))
    {
      log.error(Messages.getString("ReportPlugin.emailContentNotProvided")); //$NON-NLS-1$
      result = false;
    }

    return result;

  }

  /**
   * Perform the primary function of this component, this is, to execute. This method will be invoked immediately following a successful validate().
   *
   * @return true if successful execution
   * @throws Exception
   */
  public boolean execute() throws Exception
  {

    try
    {

      final Properties props = new Properties();

      try
      {
        final Document configDocument = PentahoSystem.getSystemSettings().getSystemSettingsDocument(
            "smtp-email/email_config.xml"); //$NON-NLS-1$
        final List properties = configDocument.selectNodes("/email-smtp/properties/*"); //$NON-NLS-1$
        final Iterator propertyIterator = properties.iterator();
        while (propertyIterator.hasNext())
        {
          final Node propertyNode = (Node) propertyIterator.next();
          final String propertyName = propertyNode.getName();
          final String propertyValue = propertyNode.getText();
          props.put(propertyName, propertyValue);
        }
      }
      catch (Exception e)
      {
        log.error(Messages.getString("ReportPlugin.emailConfigFileInvalid")); //$NON-NLS-1$
        return false;
      }

      final boolean authenticate = "true".equalsIgnoreCase(props.getProperty("mail.smtp.auth")); //$NON-NLS-1$//$NON-NLS-2$

      // Get a Session object

      final Session session;
      if (authenticate)
      {
        final Authenticator authenticator = new EmailAuthenticator();
        session = Session.getInstance(props, authenticator);
      }
      else
      {
        session = Session.getInstance(props);
      }

      // if debugging is not set in the email config file, match the
      // component debug setting
      if (!props.containsKey("mail.debug"))
      { //$NON-NLS-1$
        session.setDebug(true);
      }

      // construct the message
      final MimeMessage msg;
      if (getMimeMessage() != null)
      {
        msg = new MimeMessage(session, getMimeMessage().getInputStream());
      }
      else
      {
        msg = new MimeMessage(session);
      }

      msg.setFrom(new InternetAddress(from));
      msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));

      if ((cc != null) && (cc.trim().length() > 0))
      {
        msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc, false));
      }
      if ((bcc != null) && (bcc.trim().length() > 0))
      {
        msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc, false));
      }

      if (subject != null)
      {
        msg.setSubject(subject, LocaleHelper.getSystemEncoding());
      }
      if ((messagePlain != null) && (messageHtml == null) && (getAttachmentContent() == null))
      {
        msg.setText(messagePlain, LocaleHelper.getSystemEncoding());
      }
      else if (getAttachmentContent() == null)
      {
        if (messagePlain != null)
        {
          msg.setContent(messagePlain, "text/plain; charset=" + LocaleHelper.getSystemEncoding()); //$NON-NLS-1$
        }
        if (messageHtml != null)
        {
          msg.setContent(messageHtml, "text/html; charset=" + LocaleHelper.getSystemEncoding()); //$NON-NLS-1$
        }
      }
      else
      {
        // need to create a multi-part message...
        // create the Multipart and add its parts to it
        Multipart multipart = new MimeMultipart();

        // Test the message type
        boolean isMimeMultipart = false;
        try
        {
          isMimeMultipart = msg.getContent() instanceof MimeMultipart;
        }
        catch (Exception e)
        {
          // ignore ..
        }

        if (isMimeMultipart)
        {
          if (getAttachmentContent() == null)
          {
            multipart = (Multipart) msg.getContent();
          }
          else
          {
            // encapsulate it
            final MimeBodyPart inlineContent = new MimeBodyPart();
            inlineContent.setContent((Multipart) msg.getContent());
            multipart.addBodyPart(inlineContent);
          }
        }
        else
        {
          multipart = new MimeMultipart();
        }


        // create and fill the first message part
        if (messageHtml != null)
        {
          // create and fill the first message part
          final MimeBodyPart htmlBodyPart = new MimeBodyPart();
          htmlBodyPart.setContent(messageHtml, "text/html; charset=" + LocaleHelper.getSystemEncoding()); //$NON-NLS-1$
          multipart.addBodyPart(htmlBodyPart);
        }

        if (messagePlain != null)
        {
          final MimeBodyPart textBodyPart = new MimeBodyPart();
          textBodyPart.setContent(messagePlain, "text/plain; charset=" + LocaleHelper.getSystemEncoding()); //$NON-NLS-1$
          multipart.addBodyPart(textBodyPart);
        }

        if (getAttachmentContent() != null)
        {
          final MimeBodyPart attachmentBodyPart = new MimeBodyPart(getAttachmentContent().getInputStream());
          attachmentBodyPart.setFileName(getAttachmentName());
          multipart.addBodyPart(attachmentBodyPart);
        }
        if (getAttachmentContent2() != null)
        {
          final MimeBodyPart attachmentBodyPart2 = new MimeBodyPart(getAttachmentContent2().getInputStream());
          attachmentBodyPart2.setFileName(getAttachmentName2());
          multipart.addBodyPart(attachmentBodyPart2);
        }
        if (getAttachmentContent3() != null)
        {
          final MimeBodyPart attachmentBodyPart3 = new MimeBodyPart(getAttachmentContent3().getInputStream());
          attachmentBodyPart3.setFileName(getAttachmentName3());
          multipart.addBodyPart(attachmentBodyPart3);
        }

        // add the Multipart to the message
        msg.setContent(multipart);
      }

      msg.setHeader("X-Mailer", MAILER); //$NON-NLS-1$
      msg.setSentDate(new Date());

      Transport.send(msg);

      return true;
    }
    catch (SendFailedException e)
    {
      log.error(Messages.getString("ReportPlugin.emailSendFailed")); //$NON-NLS-1$
    }
    catch (AuthenticationFailedException e)
    {
      log.error(Messages.getString("ReportPlugin.emailAuthenticationFailed")); //$NON-NLS-1$
    }
    return false;

  }

  /**
   * This method returns the output-type for the streaming output, it is the same as what is returned by getOutputType() for consistency.
   *
   * @return the mime-type for the streaming output
   */
  public String getMimeType()
  {
    return outputType;
  }

  private static class EmailAuthenticator extends Authenticator
  {
    private EmailAuthenticator()
    {
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
      final String user = PentahoSystem.getSystemSetting("smtp-email/email_config.xml", "mail.userid", null); //$NON-NLS-1$ //$NON-NLS-2$
      final String password = PentahoSystem.getSystemSetting("smtp-email/email_config.xml", "mail.password", null); //$NON-NLS-1$ //$NON-NLS-2$
      return new PasswordAuthentication(user, password);
    }
  }
}
