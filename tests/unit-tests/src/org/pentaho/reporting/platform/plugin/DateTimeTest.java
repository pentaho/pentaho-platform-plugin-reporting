package org.pentaho.reporting.platform.plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import junit.framework.TestCase;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewerUtil;
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

  public void testOffsetCalculation1() throws ParseException
  {
    final String originalTimestamp = "2010-10-01T00:00:00.000+0000";
    final int targetTimeZoneOffsetInMinutes = -120;

    final String dateAsText = normalizeDate(originalTimestamp, targetTimeZoneOffsetInMinutes);

    final long t1 = parseWithTimezone(dateAsText).getTime();
    final long t2 = parseWithTimezone(originalTimestamp).getTime();
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

    final long t1 = parseWithTimezone(dateAsText).getTime();
    final long t2 = parseWithTimezone(originalTimestamp).getTime();
    System.out.println(dateAsText);
    System.out.println(t1 - t2);
    System.out.println((t1 - t2) / 60000);
    assertEquals(t1, t2);
  }

  private String normalizeDate(final String originalTimestamp, final int targetTimeZoneOffsetInMinutes)
      throws ParseException
  {
    final Date dateLocal = parseWithoutTimezone(originalTimestamp);
    final Date dateUtc = parseWithTimezone(originalTimestamp);
    final String offsetText = TimeZoneOffsets.formatOffset(targetTimeZoneOffsetInMinutes);
    final long date = dateLocal.getTime() + (targetTimeZoneOffsetInMinutes * 60000) +
        (dateUtc.getTime() - dateLocal.getTime()) - (getNativeTimezoneOffset() * 60000);

    final Date localWithShift = new Date(date);
    final SimpleDateFormat localDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000'");
    final String dateAsText = localDate.format(localWithShift) + offsetText;
    return dateAsText;
  }


  public static Date parseWithTimezone(final String dateString) throws ParseException
  {
    if (dateString.length() != 28)
    {
      throw new IllegalArgumentException("This is not a valid ISO-date with timezone: " + dateString);
    }
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000'Z").parse(dateString);
  }

  public static Date parseWithoutTimezone(String dateString) throws ParseException
  {
    if (dateString.length() == 28)
    {
      dateString = dateString.substring(0, 23);
    }
    if (dateString.length() != 23)
    {
      throw new IllegalArgumentException("This is not a valid ISO-date without timezone: " + dateString);
    }
    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000'").parse(dateString);
  }

  private int getNativeTimezoneOffset()
  {
    return TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000;
  }
}
