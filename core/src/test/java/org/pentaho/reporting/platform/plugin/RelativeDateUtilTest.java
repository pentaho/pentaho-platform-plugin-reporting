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

import org.junit.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class RelativeDateUtilTest {
  @Test
  public void validateLocaleAwareDayOfWeek() {
    Locale defaultLocale = Locale.getDefault();

    Locale.setDefault( Locale.US );
    assertEquals( RelativeDateUtil.localeAwareFirstDayOfWeek(), DayOfWeek.SUNDAY );
    Locale.setDefault( Locale.FRANCE );
    assertEquals( RelativeDateUtil.localeAwareFirstDayOfWeek(), DayOfWeek.MONDAY );

    Locale.setDefault( defaultLocale );
  }

  @Test(expected = IllegalArgumentException.class)
  public void testStartThisLastUnitException() {
    RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.THIS, 1, RelativeDateUtil.Unit.CALENDAR_MONTH);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEndThisLastUnitException() {
    RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.THIS, RelativeDateUtil.Unit.CALENDAR_MONTH);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIllegalValueException() {
    RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, -999, RelativeDateUtil.Unit.CALENDAR_MONTH);
  }

  @Test
  public void validateRelativeStartDates() throws Exception {
    // Sanity checks:
    // - Last 1 Day -> today
    // - This Day -> today
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 1, RelativeDateUtil.Unit.DAY ),
      LocalDate.now() );
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.THIS, 0, RelativeDateUtil.Unit.DAY ),
      LocalDate.now() );


    // With "Today" as the arbitrarily chosen day June 1, 2023 -- so these tests don't depend on the day then run
    Instant testInstant = LocalDateTime.of( 2023, 6, 1, 0, 0 )
      .atZone( ZoneId.systemDefault() )
      .toInstant();
    Clock fixedClock = Clock.fixed( testInstant, ZoneId.systemDefault());

    // ===== Tests for "Last" =====
    // Days
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.DAY, fixedClock ),
      LocalDate.of(2023, 5, 23) );

    // Weeks
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.WEEK, fixedClock ),
      LocalDate.of(2023, 3, 24) );

    // Months
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.MONTH, fixedClock ),
      LocalDate.of(2022, 8, 2) );

    // Years (long enough to test leap years)
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.YEAR, fixedClock ),
      LocalDate.of(2013, 6, 2) );


    // Calendar Weeks
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault( Locale.US );
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 1, RelativeDateUtil.Unit.CALENDAR_WEEK, fixedClock ),
      LocalDate.of(2023, 5, 21) );
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10,
        RelativeDateUtil.Unit.CALENDAR_WEEK, fixedClock ),
      LocalDate.of(2023, 3, 19) );
    Locale.setDefault( defaultLocale );

    // Calendar Months
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 1, RelativeDateUtil.Unit.CALENDAR_MONTH, fixedClock ),
      LocalDate.of(2023, 5, 1) );
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.CALENDAR_MONTH, fixedClock ),
      LocalDate.of(2022, 8, 1) );
    Locale.setDefault( defaultLocale );

    // Calendar Years
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 1, RelativeDateUtil.Unit.CALENDAR_YEAR, fixedClock ),
      LocalDate.of(2022, 1, 1) );
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.CALENDAR_YEAR, fixedClock ),
      LocalDate.of(2013, 1, 1) );
    Locale.setDefault( defaultLocale );

    // Fiscal Quarters
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 1, RelativeDateUtil.Unit.FISCAL_QUARTER, fixedClock ),
      LocalDate.of(2023, 1, 1) );

    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.FISCAL_QUARTER, fixedClock ),
      LocalDate.of(2020, 10, 1) );

    // Calendar Quarters
    // We were using the default fiscal quarter (Jan 1) -- so these tests don't look any different, but they hit a different path
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 1, RelativeDateUtil.Unit.CALENDAR_QUARTER,
        fixedClock ),
      LocalDate.of(2023, 1, 1) );

    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.LAST, 10, RelativeDateUtil.Unit.CALENDAR_QUARTER, fixedClock ),
      LocalDate.of(2020, 10, 1) );

    // ===== Tests for "THIS" =====
    // using a different test date for this because the first of the month doesn't work well here...
    testInstant = LocalDateTime.of( 2023, 6, 15, 0, 0 )
      .atZone( ZoneId.systemDefault() )
      .toInstant();
    fixedClock = Clock.fixed( testInstant, ZoneId.systemDefault());

    // Days
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.THIS, -999, RelativeDateUtil.Unit.DAY, fixedClock ),
      LocalDate.of(2023, 6, 15) );

    // Weeks
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.THIS, -999, RelativeDateUtil.Unit.WEEK, fixedClock ),
      LocalDate.of(2023, 6, 11) );

    // Months
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.THIS, -999, RelativeDateUtil.Unit.MONTH, fixedClock ),
      LocalDate.of(2023, 6, 1) );

    // Years
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteStartDate( RelativeDateUtil.ThisLast.THIS, -999, RelativeDateUtil.Unit.YEAR, fixedClock ),
      LocalDate.of(2023, 1, 1) );
  }

  // This doesn't really need all these cases (a lot of these boil down to just "today"), but this way, each assert
  // lines up with the ones in validateRelativeStartDates(). It makes this test very repetitive, but hopefully it
  // can serve as examples for anyone who needs to understand this date math in the future
  @Test
  public void validateRelativeEndDates() throws Exception {
    // Sanity checks:
    // - Last 1 Day -> today
    // - This Day -> today
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.DAY ),
      LocalDate.now() );
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.THIS, RelativeDateUtil.Unit.DAY ),
      LocalDate.now() );

    // With "Today" as the arbitrarily chosen day June 1, 2023 -- so these tests don't depend on the day then run
    Instant testInstant = LocalDateTime.of( 2023, 6, 1, 0, 0 )
      .atZone( ZoneId.systemDefault() )
      .toInstant();
    Clock fixedClock = Clock.fixed( testInstant, ZoneId.systemDefault());

    // ===== Tests for "Last" =====
    // Days
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.DAY, fixedClock ),
      LocalDate.of(2023, 6, 1) );

    // Weeks
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.WEEK, fixedClock ),
      LocalDate.of(2023, 6, 1) );

    // Months
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.MONTH, fixedClock ),
      LocalDate.of(2023, 6, 1) );

    // Years (long enough to test leap years)
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.YEAR, fixedClock ),
      LocalDate.of(2023, 6, 1) );


    // Calendar Weeks
    Locale defaultLocale = Locale.getDefault();
    Locale.setDefault( Locale.US );
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.CALENDAR_WEEK, fixedClock ),
      LocalDate.of(2023, 5, 27) );
    Locale.setDefault( defaultLocale );

    // Calendar Months
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.CALENDAR_MONTH, fixedClock ),
      LocalDate.of(2023, 5, 31) );
    Locale.setDefault( defaultLocale );

    // Calendar Years
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.CALENDAR_YEAR, fixedClock ),
      LocalDate.of(2022, 12, 31) );
    Locale.setDefault( defaultLocale );

    // Fiscal Quarters
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.FISCAL_QUARTER, fixedClock ),
      LocalDate.of(2023, 3, 31) );

    // Calendar Quarters
    // We were using the default fiscal quarter (Jan 1) -- so these tests don't look any different, but they hit a different path
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.LAST, RelativeDateUtil.Unit.CALENDAR_QUARTER, fixedClock ),
      LocalDate.of(2023, 3, 31) );

    // ===== Tests for "THIS" =====
    // using a different test date for this because the first of the month doesn't work well here...
    testInstant = LocalDateTime.of( 2023, 6, 15, 0, 0 )
      .atZone( ZoneId.systemDefault() )
      .toInstant();
    fixedClock = Clock.fixed( testInstant, ZoneId.systemDefault());

    // Days
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.THIS, RelativeDateUtil.Unit.DAY, fixedClock ),
      LocalDate.of(2023, 6, 15) );

    // Weeks
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.THIS, RelativeDateUtil.Unit.WEEK, fixedClock ),
      LocalDate.of(2023, 6, 15) );

    // Months
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.THIS, RelativeDateUtil.Unit.MONTH, fixedClock ),
      LocalDate.of(2023, 6, 15) );

    // Years
    assertEquals(
      RelativeDateUtil.relativeDateToAbsoluteEndDate( RelativeDateUtil.ThisLast.THIS, RelativeDateUtil.Unit.YEAR, fixedClock ),
      LocalDate.of(2023, 6, 15) );
  }
}
