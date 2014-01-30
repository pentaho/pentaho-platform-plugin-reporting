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
* Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
*/
require(["cdf/jquery",
  "cdf/jquery-impromptu.3.1",
  "cdf/jquery.ui",
  "cdf/jquery.bgiframe",
"cdf/jquery.jdMenu",
"cdf/jquery.positionBy",
"cdf/jquery.tooltip",
"cdf/jquery.blockUI",
"cdf/jquery.eventstack",
"cdf/Base",
"cdf/Dashboards",
"cdf/CoreComponents",
"cdf/simile/ajax/simile-ajax-api",
"cdf/simile/ajax/scripts/json",
"common-ui/repo/pentaho-ajax",
"common-ui/prompting/pentaho-prompting",
"common-ui/prompting/pentaho-prompting-builders",
"common-ui/prompting/pentaho-prompting-components",
"common-ui/prompting/pentaho-prompting-bind",
"reportviewer/reportviewer",
"reportviewer/reportviewer-prompt",
  'reportviewer/reportviewer-logging',
 'reportviewer/formatter',
 'reportviewer/reportviewer',
  'dojo/parser',
  'pentaho/common/Messages',
  'pentaho/common/MessageBox',
  'pentaho/common/Menu',
  'pentaho/common/MenuItem',
  'pentaho/common/PageControl', 'dijit/Toolbar', 'dijit/ToolbarSeparator','dijit/form/ToggleButton', 'pentaho/common/GlassPane'], function(_jquery, _jquery_impromptu, _jquery_ui, _jquery_bgiframe, _jquery_jdmenu, _jquery_positionBy, _jquery_tooltip, _jquery_blockui, _jquery_eventstack,
    _base, _dashboards, _coreComponents, _simple_ajax, _json, _pentaho_ajax, _prompting, _prompting_builders, _prompting_components, _prompting_bind, _reportViewer, Prompt, logging, formatter, Viewer,
    parser, _Messages, _MessageBox, _Menu, _MenuItem, _PageControl){

  _isReportViewer = true;
  _isTopReportViewer = true;
  try { _isTopReportViewer = ((window.parent === window) || !window.parent._isReportViewer); } catch(ex){ /*XSS*/ }

  var inMobile = false;
  try { inMobile = !!window.top.PentahoMobile; } catch(ex) { /*XSS*/ }

  Dashboards.blockUIwithDrag = function() {
    // blockUI has concurrency issues (see BISERVER-8124)
    // forcing no-op with override
  }
  var options;
  if(_isTopReportViewer) {
    var qs;
    try       { qs = window.top.location.search; }
    catch(ex) { qs = window.location.search;     } /*XSS*/

    options = {enabled: !!qs && ("&" + qs.substr(1)).indexOf("&debug=true") >= 0};
  } else {
    options = {parent: window.parent.logger};
  }

  logged = logging.create(/*logger id*/window.name, options);
  logger = logged.logger; // may be null



  window.prompt = new Prompt();
  window.viewer = new Viewer(prompt);
  window.prompt.load();

  $(window).resize(logged('window.resize', function() {
    viewer.view.onViewportResize();
  }));

//  document.getElementById('gwtLocale').content = "locale="+SESSION_LOCALE; // PIR-877 - GWT modules need to know the current locale

});