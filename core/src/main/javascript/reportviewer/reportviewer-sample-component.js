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


/**
 * This is a sample wrapper component. It requests that two resources be loaded: Report Viewer Application and the
 * sample component from Common UI's Prompting API.
 *
 * See common-ui/prompting/pentaho-prompting-sample-component.js for the custom component implementation.
 */

require([
  'reportviewer/reportviewer-app',
  'common-ui/prompting/pentaho-prompting-sample-component'
]);
