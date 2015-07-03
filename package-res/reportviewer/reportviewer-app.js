/*
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
 * Copyright 2014 Pentaho Corporation. All rights reserved.
 */

define(["reportviewer/reportviewer-main-module", 'dojo/parser',"reportviewer/reportviewer-prompt",'reportviewer/reportviewer','reportviewer/reportviewer-logging', 'dojo/cookie'], function(_module, parser, Prompt, Viewer, logging, cookie){
  "use strict";
  parser.parse();

  window._isReportViewer = true;
  window._isTopReportViewer = true;
  try { _window.isTopReportViewer = ((window.parent === window) || !window.parent._isReportViewer); } catch(ex){ /*XSS*/ }

  var inMobile = false;
  try { inMobile = !!window.top.PentahoMobile; } catch(ex) { /*XSS*/ }

 // TODO: REVIEW!!!!!!
 // Dashboards.blockUIwithDrag = function() {
    // blockUI has concurrency issues (see BISERVER-8124)
    // forcing no-op with override
 // }
  var options;
  if(_isTopReportViewer) {
    var qs;
    try       { qs = window.top.location.search; }
    catch(ex) { qs = window.location.search;     } /*XSS*/

    options = {enabled: !!qs && ("&" + qs.substr(1)).indexOf("&debug=true") >= 0};
  } else {
    options = {parent: window.parent.logger};
  }

  window.logged = logging.create(/*logger id*/window.name, options);
  window.logger = window.logged.logger; // may be null



  window.prompt = new Prompt();
  window.viewer = new Viewer(prompt);
  window.prompt.load();

  $(window).resize(logged('window.resize', function() {
    viewer.view.onViewportResize();
  }));

  $(document).ready(function ()
	{
		cookie('scrollValue', "", { expires: -1 });
		$("iframe#reportContent").load(function()
		{
			var scrollVal = 0;
			scrollVal = cookie('scrollValue');
			if(scrollVal)
			{
				$("#promptPanel").contents().find("div.parameter-wrapper").animate({scrollLeft: scrollVal},'slow');
			}

			$("#promptPanel").contents().find("button").click(function()
			{
				cookie('scrollValue', "", { expires: -1 });
				cookie('scrollValue', $('#promptPanel').contents().find("div.parameter-wrapper").scrollLeft(), { expires: 5 });
			});
		});
	});
});
