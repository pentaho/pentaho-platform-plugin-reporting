package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository.ISchedule;
import org.pentaho.platform.api.repository.ISubscribeContent;
import org.pentaho.platform.api.repository.ISubscription;
import org.pentaho.platform.api.repository.ISubscriptionRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.repository.messages.Messages;
import org.pentaho.platform.repository.subscription.Subscription;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 16:15:52
 *
 * @author Thomas Morgner.
 */
public class SubscribeContentHandler
{
  private ReportContentGenerator contentGenerator;
  private IPentahoSession userSession;

  public SubscribeContentHandler(final ReportContentGenerator contentGenerator)
  {
    this.contentGenerator = contentGenerator;
    this.userSession = contentGenerator.getUserSession();
  }

  public void createSubscribeContent(final OutputStream outputStream,
                                     final String reportDefinitionPath)
      throws ResourceException, IOException
  {
    final MasterReport report = ReportCreator.createReport(reportDefinitionPath, userSession);
    final ParameterDefinitionEntry parameterDefinitions[] = report.getParameterDefinition().getParameterDefinitions();
    final String result = saveSubscription(parameterDefinitions, reportDefinitionPath);
    outputStream.write(result.getBytes());
    outputStream.flush();
  }


  private String saveSubscription(final ParameterDefinitionEntry parameterDefinitions[],
                                  final String actionReference)
  {

    if ((userSession == null) || (userSession.getName() == null))
    {
      return Messages.getInstance().getString("SubscriptionHelper.USER_LOGIN_NEEDED"); //$NON-NLS-1$
    }

    final IParameterProvider parameterProvider = contentGenerator.getRequestParameters();
    final String subscriptionName = (String) parameterProvider.getParameter("subscription-name"); //$NON-NLS-1$

    final ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);

    ISubscription subscription = contentGenerator.getSubscription();
    if (subscription == null)
    {
      final boolean isUniqueName = subscriptionRepository.checkUniqueSubscriptionName(subscriptionName, userSession.getName(), actionReference);
      if (!isUniqueName)
      {
        return Messages.getInstance().getString("SubscriptionHelper.USER_SUBSCRIPTION_NAME_ALREADY_EXISTS", subscriptionName); //$NON-NLS-1$
      }
    }

    final ISubscribeContent content = subscriptionRepository.getContentByActionReference(actionReference);
    if (content == null)
    {
      return (Messages.getInstance().getString("SubscriptionHelper.ACTION_SEQUENCE_NOT_ALLOWED", parameterProvider.getStringParameter("name", ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    final HashMap<String, Object> parameters = new HashMap<String, Object>();

    for (final ParameterDefinitionEntry parameter : parameterDefinitions)
    {
      final String parameterName = parameter.getName();
      final Object parameterValue = parameterProvider.getParameter(parameterName);
      if (parameterValue != null)
      {
        parameters.put(parameterName, parameterValue);
      }
    }
    parameters.put(SimpleReportingComponent.OUTPUT_TARGET, parameterProvider.getParameter(SimpleReportingComponent.OUTPUT_TARGET));

    final String destination = (String) parameterProvider.getParameter("destination"); //$NON-NLS-1$
    if (subscription == null)
    {
      // create a new subscription
      final String subscriptionId = UUIDUtil.getUUIDAsString();
      subscription = new Subscription(subscriptionId, userSession.getName(), subscriptionName, content, destination, Subscription.TYPE_PERSONAL, parameters);
    }
    else
    {
      subscription.setTitle(subscriptionName);
      subscription.setDestination(destination);
      subscription.getParameters().clear();
      subscription.getParameters().putAll(parameters);
      subscription.getSchedules().clear();
    }

    // now add the schedules
    final List schedules = subscriptionRepository.getSchedules();
    for (int i = 0; i < schedules.size(); i++)
    {
      final ISchedule schedule = (ISchedule) schedules.get(i);
      final String scheduleId = schedule.getId();
      final String scheduleIdParam = (String) parameterProvider.getParameter("schedule-id"); //$NON-NLS-1$
      if (scheduleId.equals(scheduleIdParam))
      { //$NON-NLS-1$
        subscription.addSchedule(schedule);
      }
    }

    if (subscriptionRepository.addSubscription(subscription))
    {
      return Messages.getInstance().getString("SubscriptionHelper.USER_SUBSCRIPTION_CREATED"); //$NON-NLS-1$
    }
    else
    {
      // TODO log an error
      return Messages.getInstance().getString("SubscriptionHelper.USER_SUBSCRIPTION_NOT_CREATE"); //$NON-NLS-1$
    }
  }

}
