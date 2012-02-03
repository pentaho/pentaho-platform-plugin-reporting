package org.pentaho.reporting.platform.plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.google.gwt.i18n.client.DateTimeFormat;
import junit.framework.TestCase;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.platform.plugin.gwt.client.Parameter;
import org.pentaho.reporting.platform.plugin.gwt.client.ParameterSelection;
import org.pentaho.reporting.platform.plugin.gwt.client.TimeZoneOffsets;

/**
 * Todo: Document me!
 * <p/>
 * Date: 28.09.2010
 * Time: 18:37:13
 *
 * @author Thomas Morgner.
 */
public class DateTimeTest extends TestCase
{
  public DateTimeTest()
  {
  }

  public DateTimeTest(final String name)
  {
    super(name);
  }

/** Removing unmaintained unit tests **/
/*
  public void testOffsetCalculation1() throws ParseException
  {
    final String originalTimestamp = "2010-10-01T00:00:00.000+0000";
    final int targetTimeZoneOffsetInMinutes = -120;

    final String dateAsText = normalizeDate(originalTimestamp, targetTimeZoneOffsetInMinutes);

    final long t1 = TestReportViewerUtil.parseWithTimezone(dateAsText).getTime();
    final long t2 = TestReportViewerUtil.parseWithTimezone(originalTimestamp).getTime();
    System.out.println(dateAsText);
    System.out.println(t1 - t2);
    System.out.println((t1 - t2) / 60000);
    assertEquals(t1, t2);
  }

  public void testOffsetCalculation2() throws ParseException
  {
    final String originalTimestamp = "2010-10-01T00:00:00.000+0600";
    final int targetTimeZoneOffsetInMinutes = -60;

    final String dateAsText = normalizeDate(originalTimestamp, targetTimeZoneOffsetInMinutes);

    final long t1 = TestReportViewerUtil.parseWithTimezone(dateAsText).getTime();
    final long t2 = TestReportViewerUtil.parseWithTimezone(originalTimestamp).getTime();
    System.out.println(dateAsText);
    System.out.println(t1 - t2);
    System.out.println((t1 - t2) / 60000);
    assertEquals(t1, t2);
  }
*/
  
  private String normalizeDate(final String originalTimestamp, final int targetTimeZoneOffsetInMinutes)
      throws ParseException
  {
    final Date dateLocal = TestReportViewerUtil.parseWithoutTimezone(originalTimestamp);
    final Date dateUtc = TestReportViewerUtil.parseWithTimezone(originalTimestamp);
    final String offsetText = TimeZoneOffsets.formatOffset(targetTimeZoneOffsetInMinutes);
    final long date = dateLocal.getTime() + (targetTimeZoneOffsetInMinutes * 60000) +
        (dateUtc.getTime() - dateLocal.getTime()) - (TestReportViewerUtil.getNativeTimezoneOffset() * 60000);

    final Date localWithShift = new Date(date);
    final SimpleDateFormat localDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000'");
    final String dateAsText = localDate.format(localWithShift) + offsetText;
    return dateAsText;
  }


  public void testParameterNormalization() throws ParseException
  {
    final String utc = "2010-10-26T06:32:59.000+0000";
    final String gmt = "2010-10-26T06:32:59.000-0100";

    System.out.println(TestReportViewerUtil.getNativeTimezoneOffset());
    final Parameter parameter = new Parameter("UTC");
 //   parameter.setTimezoneHint("+0000");
    parameter.setType("java.util.Date");
    parameter.setAttribute(ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TIMEZONE, "utc");
    parameter.addSelection(new ParameterSelection("java.util.Date", utc, true, null));
    System.out.println (TestReportViewerUtil.normalizeParameterValue(parameter, "java.util.Date", utc));

    final Parameter p2 = new Parameter("GMT");
    p2.setTimezoneHint("-0100");
    p2.setType("java.util.Date");
    p2.setAttribute(ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TIMEZONE, "Etc/GMT+1");
    p2.addSelection(new ParameterSelection("java.util.Date", gmt, true, null));
    System.out.println (TestReportViewerUtil.normalizeParameterValue(p2, "java.util.Date", gmt));
  }

}
