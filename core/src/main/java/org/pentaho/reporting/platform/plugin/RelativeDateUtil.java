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
 * Copyright (c) 2002-2023 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import com.cronutils.utils.VisibleForTesting;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class RelativeDateUtil {
  public enum ThisLast {
    THIS,
    LAST
  }

  public enum Unit {
    DAY,
    WEEK,
    CALENDAR_WEEK,
    MONTH,
    CALENDAR_MONTH,
    CALENDAR_QUARTER,
    YEAR,
    CALENDAR_YEAR,
    FISCAL_YEAR,
    FISCAL_QUARTER
  }


  /**
   * Goes from a relative date range as specified in the UI, to an absolute date for start of that range. Note: "Days"
   * are special here, as per the spec. We always include the current day in our count
   *
   * @param thisLast the "this"/"last" field in the UI
   * @param value    the number of days/weeks/months/etc
   * @param unit     the unit used to specify the range
   * @return The start of the range
   * @throws IllegalArgumentException
   */
  public static LocalDate relativeDateToAbsoluteStartDate( ThisLast thisLast, int value, Unit unit ) throws IllegalArgumentException {
    return relativeDateToAbsoluteStartDate( thisLast, value, unit, Clock.systemDefaultZone() );
  }

  // We need an overload with a Clock object so that we can get a constant value from LocalDate.now() in the unit tests
  @VisibleForTesting
  protected static LocalDate relativeDateToAbsoluteStartDate( ThisLast thisLast, int value, Unit unit, Clock clock )
    throws IllegalArgumentException {
    // Negative or 0 "values" don't make sense with this representation. We allow them for "this" though, because
    // "value" gets ignored in that case.
    if ( value < 1 && thisLast == ThisLast.LAST ) {
      throw new IllegalArgumentException( "Cannot use relative dates with a \"value\" below 1" );
    }

    LocalDate today = LocalDate.now( clock );

    LocalDate fiscalYearStart = LocalDate.parse( ReportContentUtil.fiscalYearStartString );
    // If we haven't hit the start of the fiscal year yet, we need to subtract an extra calendar year in a couple of
    // different places. We'll hold onto an offset of either 0 or 1 for brevity.
    int fiscalYearOffset = fiscalYearStart.getDayOfYear() > today.getDayOfYear() ? 1 : 0;

    // Work around scoping for java switch
    long currentQuarter;
    LocalDate firstDayOfCurrentQuarter;

    if ( thisLast == ThisLast.LAST ) {
      switch ( unit ) {
        case DAY:
          return today.minusDays( value - 1 );
        case WEEK:
          return today.minusWeeks( value )
                      .plusDays( 1 );
        case MONTH:
          return today.minusMonths( value )
                      .plusDays( 1 );
        case YEAR:
          return today.minusYears( value )
                      .plusDays( 1 );
        case CALENDAR_WEEK:
          // Since Java's LocalDate uses Monday as the first of the week, we need to go back one extra week if
          // it's Sunday in this locale
          DayOfWeek firstDayOfWeek = localeAwareFirstDayOfWeek();
          return today.minusWeeks( value + ( firstDayOfWeek == DayOfWeek.SUNDAY ? 1 : 0 ) )
                      .with( firstDayOfWeek );
        case CALENDAR_MONTH:
          return today.minusMonths( value )
                      .withDayOfMonth( 1 );
        case CALENDAR_QUARTER:
          currentQuarter = today.withDayOfYear( 1 )
                                .until( today, IsoFields.QUARTER_YEARS );
          firstDayOfCurrentQuarter = today.withDayOfYear( 1 )
                                          .plus( currentQuarter, IsoFields.QUARTER_YEARS );
          return firstDayOfCurrentQuarter.minus( value, IsoFields.QUARTER_YEARS );
        case CALENDAR_YEAR:
          return today.minusYears( value )
                      .withDayOfYear( 1 );
        // Fiscal years behave like Calendar weeks/months/years (otherwise it would just be minus 365 (or 366) days
        // and be no different from years
        case FISCAL_YEAR:
          // Need to go back an extra year if we haven't hit the start of the fiscal year yet
          return today.minusYears( value + fiscalYearOffset )
                      .withMonth( fiscalYearStart.getMonthValue() )
                      .withDayOfMonth( fiscalYearStart.getDayOfMonth() );
        // Fiscal quarters also behave like Calendar weeks/months/years
        default: // FISCAL_QUARTER
          currentQuarter = fiscalYearStart.minusYears( fiscalYearOffset )
                                          .until( today, IsoFields.QUARTER_YEARS );
          firstDayOfCurrentQuarter = fiscalYearStart.minusYears( fiscalYearOffset )
                                                    .plus( currentQuarter, IsoFields.QUARTER_YEARS );
          return firstDayOfCurrentQuarter.minus( value, IsoFields.QUARTER_YEARS );
      }
    } else { // RelativeDateThisLast.THIS
      switch ( unit ) {
        case DAY:
          return today;
        case WEEK:
          DayOfWeek firstDayOfWeek = localeAwareFirstDayOfWeek();
          return today.minusWeeks( firstDayOfWeek == DayOfWeek.SUNDAY ? 1 : 0 )
                      .with( firstDayOfWeek );
        case MONTH:
          return today.withDayOfMonth( 1 );
        case YEAR:
          return today.withDayOfYear( 1 );
        case CALENDAR_QUARTER:
          currentQuarter = today.withDayOfYear( 1 )
                                .until( today, IsoFields.QUARTER_YEARS );
          return today.withDayOfYear( 1 )
                      .plus( currentQuarter, IsoFields.QUARTER_YEARS );
        case FISCAL_YEAR:
          return today.minusYears( fiscalYearOffset )
                      .withMonth( fiscalYearStart.getMonthValue() )
                      .withDayOfMonth( fiscalYearStart.getDayOfMonth() );
        case FISCAL_QUARTER:
          currentQuarter = fiscalYearStart.minusYears( fiscalYearOffset )
                                          .until( today, IsoFields.QUARTER_YEARS );
          return fiscalYearStart.minusYears( fiscalYearOffset )
                                .plus( currentQuarter, IsoFields.QUARTER_YEARS );
        default:
          throw new IllegalArgumentException( "Cannot use \"This\" in relative dates with \"Calendar Week,\" "
            + " \"Calendar Month,\" or \"Calendar Year\"" );
      }
    }
  }

  /**
   * Goes from a relative date range as specified in the UI, to an absolute date for end of that range. Based on our
   * spec, the "value" never matters for the end of the range, so we skip passing that in.
   *
   * @param thisLast the "this"/"last" field in the UI
   * @param unit     the unit used to specify the range
   * @return The end of the range
   * @throws IllegalArgumentException
   */
  public static LocalDate relativeDateToAbsoluteEndDate( ThisLast thisLast, Unit unit )
    throws IllegalArgumentException {
    return relativeDateToAbsoluteEndDate( thisLast, unit, Clock.systemDefaultZone() );
  }

  // We need an overload with a Clock object so that we can get a constant value from LocalDate.now() in the unit tests
  @VisibleForTesting
  protected static LocalDate relativeDateToAbsoluteEndDate( ThisLast thisLast, Unit unit,
                                                            Clock clock ) throws IllegalArgumentException {
    LocalDate today = LocalDate.now( clock );

    LocalDate fiscalYearStart = LocalDate.parse( ReportContentUtil.fiscalYearStartString );
    // If we haven't hit the start of the fiscal year yet, we need to subtract an extra calendar year in many
    // different places. We'll hold onto that offset for brevity.
    int fiscalYearOffset = fiscalYearStart.getDayOfYear() > today.getDayOfYear() ? 1 : 0;
    long currentQuarter;

    if ( thisLast == ThisLast.LAST ) {
      switch ( unit ) {
        case DAY:
        case WEEK:
        case MONTH:
        case YEAR:
          return today;
        case CALENDAR_WEEK:
          // Since Java's LocalDate uses Monday as the first of the week, we need to go back one extra week if
          // it's Sunday in this locale
          DayOfWeek firstDayOfWeek = localeAwareFirstDayOfWeek();
          return today.minusWeeks( firstDayOfWeek == DayOfWeek.SUNDAY ? 1 : 0 )
                      .with( firstDayOfWeek )
                      .minusDays( 1 );
        case CALENDAR_MONTH:
          return today.withDayOfMonth( 1 )
                      .minusDays( 1 );
        case CALENDAR_QUARTER:
          currentQuarter = today.withDayOfYear( 1 )
                                .until( today, IsoFields.QUARTER_YEARS );
          return today.withDayOfYear( 1 )
                      .plus( currentQuarter, IsoFields.QUARTER_YEARS )
                      .minusDays( 1 );
        case CALENDAR_YEAR:
          return today.withDayOfYear( 1 )
                      .minusDays( 1 );
        case FISCAL_YEAR:
          // Need to go back a year if we haven't hit the start of the fiscal year yet
          return today.minusYears( fiscalYearOffset )
                      .withMonth( fiscalYearStart.getMonthValue() )
                      .withDayOfMonth( fiscalYearStart.getDayOfMonth() )
                      .minusDays( 1 );
        default: //FISCAL_QUARTER
          currentQuarter = fiscalYearStart.minusYears( fiscalYearOffset )
                                          .until( today, IsoFields.QUARTER_YEARS );
          return fiscalYearStart.minusYears( fiscalYearOffset )
                                .plus( currentQuarter, IsoFields.QUARTER_YEARS )
                                .minusDays( 1 );
      }
    } else { // RelativeDateThisLast.THIS
      if ( unit == Unit.CALENDAR_WEEK ||
        unit == Unit.CALENDAR_MONTH ||
        unit == Unit.CALENDAR_YEAR ) {
        throw new IllegalArgumentException( "Cannot use \"This\" in relative dates with \"Calendar Week,\" "
          + " \"Calendar Month,\" or \"Calendar Year\"" );
      }
      return today;
    }
  }

  @VisibleForTesting
  protected static DayOfWeek localeAwareFirstDayOfWeek() {
    GregorianCalendar calendar = new GregorianCalendar();
    int dayOfWeek = calendar.getFirstDayOfWeek(); // This is locale aware, when java.time.LocalDate isn't

    // java.util.Calendar and the newer java.time APIs use different representations for days of the week and they're
    // indexed differently. Since there are only 7 cases, converting manually isn't too bad and is hopefully less
    // brittle than doing math on the ordinals.
    switch ( dayOfWeek ) {
      case Calendar.MONDAY:
        return DayOfWeek.MONDAY;
      // I don't think there are actually any locales with Tues-Sat as the first day,
      // but it feels wrong to exclude them...
      case Calendar.TUESDAY:
        return DayOfWeek.TUESDAY;
      case Calendar.WEDNESDAY:
        return DayOfWeek.WEDNESDAY;
      case Calendar.THURSDAY:
        return DayOfWeek.THURSDAY;
      case Calendar.FRIDAY:
        return DayOfWeek.FRIDAY;
      case Calendar.SATURDAY:
        return DayOfWeek.SATURDAY;
      default: // Sunday as default
        return DayOfWeek.SUNDAY;
    }
  }
}
