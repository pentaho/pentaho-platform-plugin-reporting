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

package org.pentaho.reporting.platform.plugin.output.util;

enum Operator {
  GREATER_THAN( "Greater Than" ),
  LESS_THAN( "Less Than" ),
  EQUAL( "Equals" ),
  GREATOR_OR_EQUAL( "Greater than Or Equals" ),
  LESS_OR_EQUAL( "Less than Or Equals" ),
  CONTAINS( "Contains" ),
  DOES_NOT_CONTAIN( "Does Not Contain" ),
  BEGINS_WITH( "Begins With" ),
  ENDS_WITH( "Ends With" ),
  IS_NULL( " Is Null" ),
  IS_NOT_NULL( " Is Not Null" ),
  IN( "Includes" ),
  NOT_EQUAL( "Not Equals" ),
  UNKNOWN( "Unknown" ),
  EXCLUDES( "Excludes" );

  private final String display;

  Operator( String display ) {
    this.display = display;
  }

  public static Operator parse( String op ) {
    if ( op == null ) {
      return UNKNOWN;
    }
    switch ( op.trim().toUpperCase() ) {
      case ">":
      case "GREATER_THAN":
        return GREATER_THAN;
      case "<":
      case "LESS_THAN":
        return LESS_THAN;
      case "=":
      case "EQUAL":
      case "EQUALS":
        return EQUAL;
      case "<>":
      case "NOT_EQUAL":
        return NOT_EQUAL;
      case ">=":
      case "GREATOR_OR_EQUAL":
        return GREATOR_OR_EQUAL;
      case "<=":
      case "LESS_OR_EQUAL":
        return LESS_OR_EQUAL;
      case "CONTAINS":
        return CONTAINS;
      case "DOES NOT CONTAIN":
      case "DOES_NOT_CONTAIN":
        return DOES_NOT_CONTAIN;
      case "BEGINS WITH":
      case "BEGINSWITH":
        return BEGINS_WITH;
      case "ENDS WITH":
      case "ENDSWITH":
        return ENDS_WITH;
      case "IS NULL":
      case "ISNA":
        return IS_NULL;
      case "IS NOT NULL":
        return IS_NOT_NULL;
      case "IN":
        return IN;
      default:
        return UNKNOWN;
    }
  }

  @Override
  public String toString() {
    return display;
  }
}
