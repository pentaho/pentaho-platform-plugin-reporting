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
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

define([ 'common-ui/util/util', 'common-ui/util/timeutil', 'common-ui/util/formatting', 'pentaho/common/Messages', "dojo/dom", "dojo/on", "dojo/_base/lang",
"dijit/registry", "dojo/has", "dojo/sniff", "dojo/dom-class", 'pentaho/reportviewer/ReportDialog', "dojo/dom-style", "dojo/query", "dojo/dom-geometry", "dojo/parser", "dojo/window", "dojo/_base/window", 'cdf/lib/jquery', 'amd!cdf/lib/jquery.ui', "common-repo/pentaho-ajax", "dijit/ProgressBar", "common-data/xhr"],
    function(util, _timeutil, _formatting, _Messages, dom, on, lang, registry, has, sniff, domClass, ReportDialog, domStyle, query, geometry, parser, win, win2, $) {
  return function(reportPrompt) {
    if (!reportPrompt) {
      alert("report prompt is required");
      return;
    }

    /**
     * ReportViewer Prompt instance
     *
     * @private
     */
    var _reportPrompt = reportPrompt;

    var v = logged({

      /**
       * Gets the ReportViewer Prompt object instance
       *
       * @returns {*}
       * @readonly
       */
      get reportPrompt () {
        return _reportPrompt;
      },
      _currentReportStatus: null,
      _currentReportUuid: null,
      _currentStoredPagesCount: null,
      _cachedReportCanceled: null,
      _requestedPage: 0,
      _previousPage: 0,
      _reportUrl : null,
      _hasSchedulePermissions : false,
      _handlerRegistration : null,
      _glassPaneListenerRegistration : null,
      _locationPromptCancelHandlerRegistration: null,
      _locationPromptOkHandlerRegistration: null,
      _locationPromptFinishHandlerRegistration: null,
      _locationPromptAttachHandlerRegistration: null,
      _locationPromptFinished: null,
      _locationOutputPath: null,
      _manuallyScheduled: null,
      _scheduleScreenBtnCallbacks: null,
      _editModeToggledHandler: null,
      // var to block new report submission in case multiple UI events start generate new
      // report executions without cancelling current running ones.
      _isFinished : true,

      _bindPromptEvents: function() {
        var baseShowGlassPane = this.reportPrompt.showGlassPane.bind(this.reportPrompt);
        var baseHideGlassPane = this.reportPrompt.hideGlassPane.bind(this.reportPrompt);

        this.reportPrompt.api.event.ready(this.view.promptReady.bind(this.view));
        this.reportPrompt.showGlassPane = this.view.showGlassPane.bind(this.view,  baseShowGlassPane);
        this.reportPrompt.hideGlassPane = this.view.hideGlassPane.bind(this.view,  baseHideGlassPane);
        this.reportPrompt.api.event.submit(this.submitReport.bind(this));
        this.reportPrompt.api.event.beforeUpdate(this.beforeUpdateCallback.bind(this));
        this.reportPrompt.api.event.afterUpdate(this.afterUpdateCallback.bind(this));
        this.reportPrompt.api.event.afterRender(this.view.afterRender.bind(this.view));

        if (isRunningIFrameInSameOrigin && typeof parent.pho !== 'undefined' && typeof parent.pho.dashboards !== 'undefined' && typeof parent.pho.dashboards.addEditContentToggledListener !== 'undefined') {
          this._editModeToggledHandler = this.editModeToggledHandler.bind(this);
          parent.pho.dashboards.addEditContentToggledListener(this._editModeToggledHandler);
        }
      },

      load: function() {
        _Messages.addUrlBundle('reportviewer', CONTEXT_PATH+'i18n?plugin=reporting&name=reportviewer/messages/messages');
        this.view.localize();

        this.createRequiredHooks();

        this.view.updatePageBackground();

        // Prevent blinking text cursors
        // This only needs to be done once.
        // Moreover, setting these properties causes browser re-layout (because of setting the style?),
        // so the sooner the better.
        function noUserSelect(g) {
          g.setAttribute("style", "-webkit-touch-callout: none; -webkit-user-select: none; -khtml-user-select: none; -moz-user-select: none; -ms-user-select: none; user-select: none;");
          // IE 9 / 8
          if (typeof g.onselectstart !== 'undefined') {
            g.setAttribute('unselectable', 'on');
            g.onselectstart = function() { return false; };
          }
        }

        noUserSelect(dom.byId('reportArea'));
        noUserSelect(dom.byId('reportPageOutline'));
        noUserSelect(dom.byId('reportContent'));

        // ------------
        on(registry.byId('toolbar-parameterToggle'),  "click", lang.hitch( this,  function() {
          this.view.togglePromptPanel();
        }));

        on($('#notification-close'),  "click", lang.hitch( this,  function() {
          domClass.add('notification-screen', 'hidden');
        }));

        var clearCache = registry.byId('toolbar-clearCache');
        if(clearCache) {
          on(clearCache, "click", lang.hitch(this, function () {
            this.clearReportCache(this.submitReport.bind(this));
          }));
        }

        if(isRunningIFrameInSameOrigin && window.top.mantle_addHandler){
          //When slow connection there is a gap between tab glass pane and prompt glass pane
          //So, let's hide tab glass pane only after widget is attached
          var onAttach = function(event){
            if(event.eventSubType == 'locationPromptAttached'){
              this._forceHideGlassPane();
            }
          };
          this._locationPromptAttachHandlerRegistration = window.top.mantle_addHandler('GenericEvent', onAttach.bind(this) );
        }

        window.onbeforeunload = this.dispose.bind(this);

        var boundOnReportContentLoaded = this._onReportContentLoaded.bind(this);

        // Schedule the resize after the document has been rendered and CSS applied
        var onFrameLoaded = logged('onFrameLoaded', function() {
          setTimeout(boundOnReportContentLoaded);
        });

        if(has("ie")){
          // When a file is downloaded, the "complete" readyState does not occur: "loading", "interactive", and stops.
          on(dom.byId('reportContent'),  "readystatechange",  function() {
            if(this.readyState === 'complete') { onFrameLoaded(); }
          });
        } else {
          on(dom.byId('reportContent'),  "load", lang.hitch( onFrameLoaded));
        }

        $('body')
          .addClass(_isTopReportViewer ? 'topViewer leafViewer' : 'leafViewer')
          .addClass(inMobile ? 'mobile' : 'nonMobile');

        logger && $('body').addClass('debug');

        this._bindPromptEvents();
        this.reportPrompt.createPromptPanel();

        var rowLimitControl = registry.byId('rowLimitControl');

        if(rowLimitControl) {
          rowLimitControl.bindChange(dojo.hitch(this, this._initRowLimitCallback));
          rowLimitControl.bindGetMessage(function () {
            return registry.byId('rowLimitMessage');
          });
          rowLimitControl.bindGetDialog(function () {
            return registry.byId('rowLimitExceededDialog');
          });
          rowLimitControl.bindShowGlassPane(dojo.hitch(this, this._forceShowGlassPane));
          rowLimitControl.bindHideGlassPane(dojo.hitch(this, function () {
            this._forceHideGlassPane();
          }));
        }
        var rowLimitMessage = registry.byId('rowLimitMessage');
        if(rowLimitMessage) {
          rowLimitMessage.bindRun(function () {
            if (isRunningIFrameInSameOrigin) {
              var match = window.location.href.match('.*repos\/(.*)(\.prpt).*');
              if (match && match.length > 1) {
                window.parent.executeCommand("RunInBackgroundCommand", {
                  solutionPath: decodeURIComponent(match[1] + match[2]).replace(/:/g, '/')
                });
              }
            }
          });
          rowLimitMessage.bindSchedule(function () {
            if (isRunningIFrameInSameOrigin) {
              var match = window.location.href.match('.*repos\/(.*)(\.prpt).*');
              if (match && match.length > 1) {
                window.parent.mantle_openRepositoryFile(decodeURIComponent(match[1] + match[2]).replace(/:/g, '/'), "SCHEDULE_NEW");
              }
            }
          });
        }
        var url = this._buildReportContentUrl();
        pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/api/scheduler/canSchedule', "", dojo.hitch( this, function(result) {
          _hasSchedulePermission = "true" == result;
          if(!this.view._isInsideDashboard() && !inMobile && _hasSchedulePermission){
            registry.byId('feedbackScreen').showBackgroundBtn(_Messages.getString('FeedbackScreenBackground'));
          }
        }), "application/json");

      },

      view: logged({

        /**
         * Gets the ReportViewer Prompt object instance
         *
         * @returns {*}
         * @readonly
         */
        get reportPrompt () {
          return _reportPrompt;
        },

        /**
         * Localize the Report Viewer.
         */
        localize: function() {
          $('#toolbar-parameterToggle').attr('title', _Messages.getString('parameterToolbarItem_title'));
          registry.byId('pageControl') && registry.byId('pageControl').registerLocalizationLookup(_Messages.getString);
          registry.byId('rowLimitControl') && registry.byId('rowLimitControl').registerLocalizationLookup(_Messages.getString);
          registry.byId('rowLimitExceededDialog') && registry.byId('rowLimitExceededDialog').registerLocalizationLookup(_Messages.getString);
          registry.byId('rowLimitMessage') && registry.byId('rowLimitMessage').registerLocalizationLookup(_Messages.getString);
          registry.byId('toolbar-clearCache') && registry.byId('toolbar-clearCache').set('title', _Messages.getString('RefreshReportData_Title'));
        },

        /**
         * Update the page background when we're not in PUC or we're embedded in an
         * iframe to make sure the translucent styling has some contrast.
         */
        updatePageBackground: function() {
          // If we're not in PUC or we're in an iframe
          var inPuc;
          try {
            inPuc = window.top.mantle_initialized;
          } catch (e) { // Ignore "Same-origin policy" violation in embedded IFrame
            inPuc = false;
          }

          var inIFrame = top !== self;

          // if we are not in PUC
          if(!inSchedulerDialog && !inMobile && (!inPuc || inIFrame)) {
            domClass.add(document.body, 'pentaho-page-background');
          }
        },

        _showReportContent: function(visible, preserveSource) {
          // Force not to show a blank iframe
          var hasContent = this._hasReportContent();
          if(!hasContent) { visible = false; }

          if(this._isReportContentVisible() !== !!visible) {

            // Don't touch "data-src" of an already blank iframe, or onload occurs needlessly...
            if (!visible && !preserveSource && hasContent) {
              logger && logger.log("Will clear content iframe.data-src");

              //submit hidden form to POST data to iframe
              var url = window.location.href.substring(0, window.location.href.indexOf("/api/repos")) + "/content/reporting/reportviewer/no-content.html";
              $('#hiddenReportContentForm').attr("action", url);
              $('#hiddenReportContentForm').submit();
              //set data attribute so that we know what url is currently displayed in
              //the iframe without actually triggering a GET
              $('#reportContent').attr("data-src", 'about:blank');
              this._updatedIFrameSrc = true;
            }

            $('body')[visible ? 'removeClass' : 'addClass']('contentHidden');
          }
        },

        _calcReportContentVisibility: function() {
          var visible =
            // Anything in the iframe to show? (PRD-4271)
            this._hasReportContent() &&

            // Valid data (although report content should be blank here)
            !(this.reportPrompt._getStateProperty('promptNeeded')) &&

            // Hide the report area when in the "New Schedule" dialog
            !inSchedulerDialog &&

            (this._isAutoSubmitAllowed() || this.reportPrompt._isSubmitPromptPhaseActivated);
          return visible;
        },

        _isAutoSubmitAllowed : function() {
          if(this.reportPrompt._getStateProperty('allowAutoSubmit')) { // (BISERVER-6915)
            return true;
          }

          var iframes = document.getElementsByTagName("IFRAME");
          if(iframes.length > 0) {
            var src = $(iframes[0]).attr('data-src');
            return src != null && src.indexOf('dashboard-mode') !== -1;
          }

          return false;

        },

        _isDashboardEditMode : function(){
          try {
            return window.frameElement.src.indexOf('dashboard-mode') !== -1 && parent.pho.dashboards.editMode;
          } catch (e) {
            return false;
          }
        },

        _isInsideDashboard : function() {
          try {
            return window.frameElement.src.indexOf('dashboard-mode') !== -1;
          } catch (e) {
            return false;
          }
        },

        _hasReportContent: function() {
          var src = $('#reportContent').attr('data-src');
          return src !== undefined && src !== 'about:blank';
        },

        _isReportContentVisible: function() {
          return !$('body').hasClass('contentHidden');
        },

        // Called on page load and every time the prompt panel is refreshed
        updateLayout: function() {
          if (!this.reportPrompt._getStateProperty('showParameterUI')) {
            this._hideToolbarPromptControls();
          }

          // The following call is important for clearing the report content when autoSubmit=false and the user has changed a value.
          if(!this._calcReportContentVisibility() || this.reportPrompt._isAsync) {
            this._showReportContent(false);
          }

          this._layoutInited = false;
          this._initLayout();
        },

        _hideToolbarPromptControls: function() {
          // Hide the toolbar elements
          // When inMobile, toolbarlinner2 has another structure. See report.html.
          if(!inMobile) { domClass.add('toolbar-parameter-separator', 'hidden'); }

          // dijit modifies the HTML structure.
          // At least in mobile, the button gets wrapped by a frame,
          // that needs to be hidden.
          var PARAM_TOGGLE = 'toolbar-parameterToggle';
          var elem = dom.byId(PARAM_TOGGLE);
          while(elem) {
            if(elem.getAttribute('widgetid') === PARAM_TOGGLE) {
              domClass.add(elem, 'hidden');
              break;
            }
            elem = elem.parentNode;
          }
        },

        showGlassPane: function(base) {
          $("#glasspane")
            .css("background", v.reportContentUpdating() ? "" : "transparent");

          base();
        },

        hideGlassPane: function(base) {
          if(!v.reportContentUpdating()) {
            base();
          }

          // Activate bg.
          $("#glasspane").css("background", "");
        },

        promptReady: function() {
          this.reportPrompt.hideGlassPane();

          if (inSchedulerDialog) {
            // If we are rendering parameters for the "New Schedule" dialog,
            // don't show the report or the submit panel, or the pages toolbar
            this.showPromptPanel(true);

            domClass.add('reportContent', 'hidden');
            // SubmitPanel can be absent in DOM
            var submitPanel = query('.submit-panel');
            if (typeof submitPanel !== 'undefined' && submitPanel.length > 0) {
                domClass.add(submitPanel[0], 'hidden');
            }
            domClass.add('toolbarlinner2', 'hidden');

            domClass.remove('promptPanel', 'pentaho-rounded-panel-bottom-lr');
            domClass.remove('reportControlPanel', 'pentaho-shadow');
            domClass.remove('reportControlPanel', 'pentaho-rounded-panel-bottom-lr');

          }
        },

        afterRender: function() {
          if (inSchedulerDialog && typeof window.parameterValidityCallback !== 'undefined') {
            var isValid = !this.reportPrompt._getStateProperty('promptNeeded');
            window.parameterValidityCallback(isValid);
          }
        },

        /**
         * Initializes the report viewer's layout based on the loaded parameter definition.
         */
        _initLayout: function() {
          if(this._layoutInited) { return; } // reset on every navigation (see #init)

          // Is it the first time, or is the parameter UI
          // being refreshed due to user interaction (which causes "navigation")?
          var navigating  = !!this._initedOnce;
          this._initedOnce = true;

          var showParamUI = this.reportPrompt._getStateProperty('showParameterUI');

          this.updatePageControl();

          // Hide the toolbar, 'toppanel',  when it would be empty and
          // un-style the report so it's the only visible element
          // when both the pagination controls and the parameter UI are hidden.
          var isToolbarEmpty = !this.reportPrompt._getStateProperty("paginate") && !showParamUI && !this.reportPrompt._isReportHtmlPagebleOutputFormat;
          domClass[isToolbarEmpty ? 'add' : 'remove']('toppanel', 'hidden');

          // Don't mess with the parameters if we're "navigating".
          // If the user has explicitly hidden the UI,
          // and is going through several pages,
          // we should not keep popping the UI again on each page init...
          // PRD-4001, PRD-4102
          var showOrHide;
          // If not available, always respect the definition and hide
          if(!showParamUI) {
            showOrHide = false;
          } else if(!navigating) {
            // Shown or hidden by default?
            // Don't show parameter panel by default unless prompt needed

            showOrHide = (!inMobile && _isTopReportViewer)  ||
                this.reportPrompt._getStateProperty('promptNeeded') ||
                !this.reportPrompt._getStateProperty('allowAutoSubmit');
          }

          var parameters = util.getUrlParameters();
          var toggleParamName = 'toolbar-parameterToggle';
          if (parameters[toggleParamName] !== undefined) {
            this.showPromptPanel(parameters[toggleParamName] === 'true');
          } else if (showOrHide != null) {
            this.showPromptPanel(showOrHide);
          }

          this._layoutInited = true;
        },

        show: function() {
          // Cleans up an issue where sometimes on show the iframe is offset
          this.resize();
        },

        /**
         *
         * @param pageNumber
         * @private
         */
        _setAcceptedPage: function(pageNumber) {
          this.reportPrompt.api.operation.setParameterValue("accepted-page", pageNumber);
        },

        /**
         *
         * @param pageNumber
         * @private
         */
        _getAcceptedPage: function() {
          return this.reportPrompt.api.operation.getParameterValues()["accepted-page"];
        },

        /**
         * @private
         */
        _getRegistryObjectById: function(id) {
          return registry.byId(id);
        },

        updatePageControl: function() {
          var pc = this._getRegistryObjectById('pageControl');

          pc.registerPageNumberChangeCallback(undefined);

          if (!this.reportPrompt._getStateProperty("paginate")) {
            if(this.reportPrompt._isAsync) {
              this._setAcceptedPage('0');
            } else {
              this._setAcceptedPage('-1');
            }
            pc.setPageCount(1);
            pc.setPageNumber(1);
            // pc.disable();
          } else {
            var total = this.reportPrompt._getStateProperty("totalPages");
            var page = this.reportPrompt._getStateProperty("page");
            // We can't accept pages out of range. This can happen if we are on a page and then change a parameter value
            // resulting in a new report with less pages. When this happens we'll just reduce the accepted page.
            page = Math.max(0, Math.min(page, total - 1));

            // add our default page, so we can keep this between selections of other parameters, otherwise it will not be on the
            // set of params are default back to zero (page 1)
            this._setAcceptedPage('' + page);
            pc.setPageCount(total);
            pc.setPageNumber(page + 1);
          }

          pc.registerPageNumberChangeCallback(function(pageNumber) {
            this.pageChanged(pageNumber);
          }.bind(this));
        },

        pageChanged: function(pageNumber) {
          this._setAcceptedPage('' + (pageNumber - 1));
          this.reportPrompt.api.operation.submit();
        },

        togglePromptPanel: function() {
          this.showPromptPanel(registry.byId('toolbar-parameterToggle').checked);
          this.resize();
        },

        showPromptPanel: function(visible) {
          registry.byId('toolbar-parameterToggle').set('checked', !!visible);

          domClass[visible ? 'remove' : 'add']('reportControlPanel', 'hidden');
        },

        isPageStyled: function() {
          return $('body').hasClass('styled');
        },

        setPageStyled: function(styled) {
          // normalize to boolean
          styled = !!styled;

          // Need to style at least the first time anyway,
          // to ensure the HTML and JS are in sync.
          if(!this._pageStyledOnce || this.isPageStyled() !== styled) {
            this._pageStyledOnce = true;

            var setClass = styled ? 'addClass' : 'removeClass';
            $('body')
            [setClass]('styled')
            [styled ? 'removeClass' : 'addClass']('nonStyled');

            if(!styled) {
              // Clear style values set in JS to let class-imposed styles take effect
              $('#reportArea'   ).css({height: ""});
              $('#reportContent').css({height: "", width:  ""});
            }

            $('#reportPageOutline')[setClass]('pentaho-rounded-panel2-shadowed pentaho-padding-lg pentaho-background');
          }
        },

        onViewportResize: function() {
          this.resizeContentArea();
        },

        // called by #resize and by window.resize -> onViewportResize
        resizeContentArea: function(callBefore) {
          var vp = win.getBox();
          var tp = geometry.getMarginBox('toppanel');

          var rcpHeight = geometry.getMarginBox("rowLimitMessage");

          var mb = {h: vp.h - tp.h - rcpHeight.h - 2};

          logger && logger.log("viewport=(" + vp.w + ","  + vp.h + ") " + " toppanel=(" + tp.w + ","  + tp.h + ") ");

          // Fill all available space
          geometry.setMarginBox('reportArea', mb);

          if(inMobile && this._isHtmlReport) {
            this._resizeMobileHtmlReportHandlesScrolling(mb);
          }
        },

        // In mobile, every report is shown unstyled.
        // Mobile HTML reports handle scrolling itself.
        // iOS Safari does not respect the iframe's style.overflow or the scrollable attribute
        // and desperate measures had to be taken.
        _resizeMobileHtmlReportHandlesScrolling: function(mb) {
          // TODO: HACK: into the report content's HTML so that it handles scrolling correctly in Safari/iOS
          var iframe = $('#reportContent');
          var iframeDoc = iframe.contents();
          var generator = iframeDoc.find('head>meta[name="generator"]').attr('content') || "";
          var isReport = generator.indexOf("Reporting") >= 0;
          if(isReport) {
            iframe.attr('scrolling', 'no');

            // TODO: HACK: Avoid a slightly clipped footer
            var wh = mb.h - 15;
            var scrollWrapper = iframeDoc.find('#reportTableDiv');
            if(scrollWrapper.length) {
              scrollWrapper.css({height: wh, width: (window.innerWidth-10) + 'px', overflow: 'auto'});
            } else {
              iframeDoc.find("body").css({margin: '2px'});
              iframeDoc
              .find("body>table")
              .wrap('<div id="reportTableDiv" style="height:' + wh + 'px; width:' + (window.innerWidth-10) + 'px; overflow:auto;"/>');
            }
          }
        },

        /**
         * Adjusts the report content iframe's width and height.
         * The result is affected by:
         * <ul>
         *   <li>content existing or not</li>
         *   <li>being in a mobile environment or not</li>
         *   <li>the existing content being styled</li>
         *   <li>the viewport size.</li>
         * </ul>
         *
         * Finally, the {@link #resizeContentArea} method is called,
         * that adjusts the report area div,
         * responsible by showing up a scrollbar, when necessary.
         */
        resize: function() {
          if (!this._hasReportContent()) { return; }

          // PRD-4000 Hide iframe before resize
          // If not visible, let be so.
          // If visible, hide it during the operation and show it again at the end.
          var isVisible = this._isReportContentVisible();
          if(isVisible) { this._showReportContent(false, /*preserveSource*/true); }

          if(this.isPageStyled()) { this._pollReportContentSize(); }

          this.resizeContentArea();

          // PRD-4000 Show iframe after resize
          if(isVisible) { this._showReportContent(true); }
        },

        _pollReportContentSize: function() {
          var POLL_SIZE = 10;
          var t = dom.byId('reportContent');

          // Set the iframe size to minimum before POLLING its contents, to not affect the polled values.
          // NOTE: Setting to 0 prevented IE9-Quirks from detecting the correct sizes.
          // Setting here, and polling next, causes ONE resize on the iframe.

          geometry.setContentSize(t, {w: POLL_SIZE, h: POLL_SIZE});

          var outerDoc = document;

          // It's surely HTML content, so the following is valid
          win2.withDoc(t.contentWindow.document, function() {
            setTimeout(function() {
              // add overflow hidden to prevent scrollbars on ie9 inside the report
              domStyle.set(win2.doc.getElementsByTagName("html")[0], {'overflow': 'hidden'});

              var dimensions = geometry.getMarginBox(win2.doc.getElementsByTagName("body")[0]);

              // changing width to jquery due to problems with dojo getting the width correcty
              // although, dojo is used to get the height due to issues on ie8 and 9
              dimensions.w = $('#reportContent').contents().width();
              dimensions.h = $('#reportContent').contents().height();

              logger && logger.log("Styled page - polled dimensions = (" + dimensions.w + ", " + dimensions.h + ")");

              // In case the styled report content is too small, assume 2/3 of the width.
              // This may happen when there are no results.
              if(dimensions.w <= POLL_SIZE) {
                // Most certainly this indicates that the loaded report content
                // does not have a fixed width, and, instead, adapts to the imposed size (like width: 100%).

                var vp;
                win2.withDoc(outerDoc, function() {
                  vp = win.getBox();
                });

                dimensions.w = Math.round(2 * vp.w / 3);
                logger && logger.log("Width is too small - assuming a default width of " + dimensions.w);
              }

              geometry.setContentSize(t, {w: dimensions.w, h: dimensions.h});
              $('#reportContent').hide().fadeIn('fast');
            }, 15);
          });
        }
      }), // end view

      onTabCloseEvent: function (event) {
        if(isRunningIFrameInSameOrigin && window.frameElement.src != null && window.frameElement.src.indexOf('dashboard-mode') !== -1 ){
          if (event.eventSubType == 'tabClosing' && event.stringParam == window.parent.frameElement.id) {
            this.dispose();
          }
        }
        else {
          if (event.eventSubType == 'tabClosing' && event.stringParam == window.frameElement.id) {
            this.dispose();
          }
        }
      },

      dispose: function() {

        this.cancel(this._currentReportStatus, this._currentReportUuid);

        if(isRunningIFrameInSameOrigin) {
          if (this._topMantleOpenTabRegistration) {
            top.mantle_openTab = null;
          }

          if (this._topOpenUrlInDialogRegistration) {
            top.reportViewer_openUrlInDialog = null;
          }

          if (top.mantle_removeHandler) {
            if (this._handlerRegistration) {
              top.mantle_removeHandler(this._handlerRegistration);
            }
            if (this._locationPromptAttachHandlerRegistration) {
              top.mantle_removeHandler(this._locationPromptAttachHandlerRegistration);
            }
            if (this._locationPromptCancelHandlerRegistration) {
              top.mantle_removeHandler(this._locationPromptCancelHandlerRegistration);
            }
            if (this._locationPromptOkHandlerRegistration) {
              top.mantle_removeHandler(this._locationPromptOkHandlerRegistration);
            }
            if (this._locationPromptFinishHandlerRegistration) {
              top.mantle_removeHandler(this._locationPromptFinishHandlerRegistration);
            }
          }

          if (this._glassPaneListenerRegistration) {
            window.top.removeGlassPaneListenerById(this._glassPaneListenerRegistration);
          }

          if (parent.pho && parent.pho.dashboards && parent.pho.dashboards.removeEditContentToggledListener) {
            parent.pho.dashboards.removeEditContentToggledListener(this._editModeToggledHandler);
          }
        }

        document.removeChild(document.documentElement);
      },

      cancel: function(status, uuid, callback) {
        var me = this;
        me._isFinished = true;
        var url = window.location.href.split('?')[0];
        if( uuid && (!status || status == 'WORKING' || status == 'QUEUED' || status == 'CONTENT_AVAILABLE' || status == 'PRE_SCHEDULED') ) {
          pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + uuid + '/cancel', "", callback);
        }
      },

      createRequiredHooks: function() {
        // [PIR-543] - Allow new/refreshed reports to re-attach or override instance functions in the top window object
        // Top window functions may become orphaned due to content linking refresh or when a report tab in PUC is closed
        /*
        try {
          if (window.reportViewer_openUrlInDialog || top.reportViewer_openUrlInDialog) {
            return;
          }
        } catch(err) {
          return; // [PIR-543] - IE 9.0.5 throws a Permission Denied error
        }
        */

        if(isRunningIFrameInSameOrigin) {
          if (!top.mantle_initialized) {
            this._topMantleOpenTabRegistration = top.mantle_openTab = function(name, title, url) {
              window.open(url, '_blank');
            };
          }

          if (top.mantle_initialized) {
            top.reportViewer_openUrlInDialog = function(title, url, width, height) {
              top.urlCommand(url, title, true, width, height);
            };
          } else {
            top.reportViewer_openUrlInDialog = this.openUrlInDialog.bind(this);
          }

          this._topOpenUrlInDialogRegistration = window.reportViewer_openUrlInDialog = top.reportViewer_openUrlInDialog;
        }

        window.reportViewer_hide = this.hide.bind(this);

        if(isRunningIFrameInSameOrigin && window.location.href.indexOf("/parameterUi") == -1 && window.top.mantle_addHandler) {
          // only in case when report is opened in tab('/viewer')
          this._handlerRegistration = window.top.mantle_addHandler("GenericEvent", this.onTabCloseEvent.bind(this));
        }

        var localThis = this;

        if (isRunningIFrameInSameOrigin && typeof window.top.addGlassPaneListener !== 'undefined') {
          this._glassPaneListenerRegistration = window.top.addGlassPaneListener({
            glassPaneHidden: function(){
              localThis.view.show();
            }
          });
        }
      },

      openUrlInDialog: function(title, url, width, height) {
        if (this.dialog === undefined) {
          this.dialog = new pentaho.reportviewer.ReportDialog();
          this.dialog.setLocalizationLookupFunction(_Messages.getString);
        }
        this.dialog.open(title, url, width, height);
      },

      /**
       * Hide the Report Viewer toolbar.
       * TODO: In what world is this a "hide"? empty()?
       * Can't find where this is called from!
       */
      hide: function() {
        $('#toppanel').empty();
        this.view.resize();
      },

      beforeUpdateCallback: function() {
        if(this.reportPrompt.api.operation.state().autoSubmit) {
          this.reportPrompt._isUpdatingPrompting = true;
        }
      },

      afterUpdateCallback: function() {
        this.view.updateLayout();
        this._updateReportContent();
      },

      // Called by SubmitPromptComponent#expression (the submit button's click)
      // Also may be called by PromptPanel#init, when there is no submit button (independently of autoSubmit?).
      submitReport: function(keyArgs) {
        this.reportPrompt._isSubmitPromptPhaseActivated = true;

        try {
          // If we are rendering parameters for the "New Schedule" dialog,
          // don't show the report, the submit panel and pages toolbar.
          if (inSchedulerDialog) {
            this._submitReportEnded();
            return;
          }

          // Make sure that layout is initialized
          this.view._initLayout();

          // Don't do anything if we need prompting (hide report content -> clear iframe data-src)
          var isValid = !this.reportPrompt._getStateProperty('promptNeeded');
          if (!isValid) {
            this.view._showReportContent(false);
            this._submitReportEnded();
            return;
          }

          if (!this.reportPrompt._getStateProperty("autoSubmit") ) {
            if(!this.reportPrompt._isAsync){
              this.reportPrompt.mode = 'MANUAL';
              this.reportPrompt.api.operation.refreshPrompt();
            }else{
              this._updateReportContent();
            }
          } else if (!this.reportPrompt._isUpdatingPrompting ) { // no need updating report content during submit because we have afterUpdate event subscription
            this._updateReportContent();
          }
        } catch(ex) {
          this._submitReportEnded();
          throw ex;
        }
      },

      clearReportCache: function (callback) {
        try {
          var url = window.location.href.split('?')[0];
          this.cancel(this._currentReportStatus, this._currentReportUuid);
          setTimeout(function () {
            pentahoPost(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/cache/clear', '', callback, 'text/text');
          }, this.reportPrompt._pollingInterval);
        } catch (e) {
          logger && logger.log("Can't clear cache.");
          callback();
        }
      },

      _updatedIFrameSrc: false,
      _updateReportTimeout: -1,

      reportContentUpdating: function() {
        return this._updateReportTimeout >= 0;
      },

      _updateReportContent: function() {
        //init row limit UI in any case
        var options = this._buildReportContentOptions();
        var requestLimit = parseInt(options['query-limit'], 0);
        var systemRowLimit = parseInt(options['maximum-query-limit'], 0);
        var isQueryLimitControlEnabled = this.reportPrompt._isAsync && options['query-limit-ui-enabled'] == "true";
        if (isQueryLimitControlEnabled) {
          var rowLimitControl =  registry.byId('rowLimitControl');
          if(rowLimitControl) {
            rowLimitControl.apply(systemRowLimit, requestLimit, false);
            domClass.remove(dom.byId("toolbar-parameter-separator-row-limit"), "hidden");
            domClass.remove(dom.byId('rowLimitControl'), "hidden");
          }
        };
        if (this.reportPrompt._isSubmitPromptPhaseActivated || this.reportPrompt._getStateProperty("autoSubmit")) {
          this._updateReportContentCore();
        }
      },

      _getFeedbackScreen: function (scheduleScreenBtnCallbacks) {
        var dlg = registry.byId('feedbackScreen');
        dlg.setTitle(_Messages.getString('ScreenTitle'));
        dlg.setText(_Messages.getString('FeedbackScreenActivity'));
        dlg.setText2(_Messages.getString('FeedbackScreenPage'));
        dlg.setText3(_Messages.getString('FeedbackScreenRow'));
        dlg.setCancelText(_Messages.getString('ScreenCancel'));

        if(!this.view._isInsideDashboard() && !inMobile && _hasSchedulePermission){
          dlg.showBackgroundBtn(_Messages.getString('FeedbackScreenBackground'));
        }else {
          scheduleScreenBtnCallbacks.shift();
          dlg.hideBackgroundBtn();
        }

        dlg.callbacks = scheduleScreenBtnCallbacks;
        return dlg;
      },

      editModeToggledHandler: function (editMode) {
        var feedbackScreen = registry.byId('feedbackScreen');
        var scheduleScreenBtnCallbacks = _scheduleScreenBtnCallbacks;
        var feedbackScreenHideStatuses = 'CANCELED|FINISHED|SCHEDULED|CONTENT_AVAILABLE';
        if (feedbackScreen) {
          if (editMode) {
            feedbackScreen.hide();
            feedbackScreen.callbacks = [scheduleScreenBtnCallbacks[1]];
            feedbackScreen.hideBackgroundBtn();
            if (feedbackScreenHideStatuses.indexOf(this._currentReportStatus) == -1) {
              feedbackScreen.show();
            }
          } else {
            feedbackScreen.hide();
            feedbackScreen.showBackgroundBtn(_Messages.getString('FeedbackScreenBackground'));
            if (feedbackScreenHideStatuses.indexOf(this._currentReportStatus) == -1) {
              feedbackScreen.show();
            }
            feedbackScreen.callbacks = scheduleScreenBtnCallbacks;
          }
        }
      },


      _getPageLoadingDialog: function (loadingDialogCallbacks) {
        var pageIsloadingDialog = registry.byId('reportGlassPane');
        pageIsloadingDialog.setTitle(_Messages.getString('ScreenTitle'));
        pageIsloadingDialog.setText(_Messages.getString('LoadingPage'));
        pageIsloadingDialog.setButtonText(_Messages.getString('ScreenCancel'));
        pageIsloadingDialog.callbacks = loadingDialogCallbacks;
        return pageIsloadingDialog;
      },

      _requestCacheFlush: function (url) {
        var urlRequestPage = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + this._currentReportUuid
            + '/requestPage/' + this._requestedPage;
        pentahoGet(urlRequestPage, "");


        var loadingDialogCallbacks = [
          //Cancel navigation
          function reportGlassPaneDone() {
            this._requestedPage = this._previousPage;
            var pageContr = registry.byId('pageControl');
            pageContr.setPageNumber(this._previousPage);
            this._cachedReportCanceled = true;
            registry.byId('reportGlassPane').hide();
          }.bind(this)];

        var pageLoadingDialog = this._getPageLoadingDialog(loadingDialogCallbacks);
        pageLoadingDialog.show();
      },

      _hideAsyncScreens: function(){
        registry.byId('reportGlassPane').hide();
        domClass.add('notification-screen', 'hidden');
      },

      _submitRowLimitUpdate: function (selectedLimit) {
        var me = this;

        me.cancel(me._currentReportStatus, me._currentReportUuid);
        me._isFinished = true;
        me._submitReportEnded(true);
        me.reportPrompt.api.operation.setParameterValue("query-limit", selectedLimit);
        me._hideDialogAndPane(registry.byId('feedbackScreen'));
        if (isRunningIFrameInSameOrigin && window.top.mantle_removeHandler) {
          window.top.mantle_removeHandler(this._handlerRegistration);
        }
        me.view.updatePageControl();

        setTimeout(function () {
          me.submitReport(true);
        }, me.reportPrompt._pollingInterval);

      },

      _initRowLimitCallback: function (selectedLimit) {
        this.reportPrompt.api.operation.setParameterValue("query-limit", selectedLimit);
        registry.byId('rowLimitControl') && registry.byId('rowLimitControl').bindChange(dojo.hitch(this, this._submitRowLimitUpdate));
      },

      _updateReportContentCore: function() {

        var me = this;

        //no report generation in scheduling dialog ever!
        //_isFinished avoid create new report job execution if current not finished
        if(inSchedulerDialog || !me._isFinished ){
          return;
        }

        // PRD-3962 - remove glass pane after 5 seconds in case iframe onload/onreadystatechange was not detected
        me._updateReportTimeout = setTimeout(logged('updateReportTimeout', function() {
          //unblock submitting new report executions
          me._isFinished = true;
          //BACKLOG-8070 don't show empty content in async mode
          me._submitReportEnded( !me.reportPrompt._isAsync );
        }), 5000);

        // PRD-3962 - show glass pane on submit, hide when iframe is loaded.
        // Must be done AFTER _updateReportTimeout has been set, cause it's used to know
        // that the report content is being updated.
        me.reportPrompt.showGlassPane();

        me._updateParametersDisabledState(true);

        var options = me._buildReportContentOptions();
        var url = me._buildReportContentUrl(options);

        var outputFormat = options['output-target'];
        var isHtml = outputFormat.indexOf('html') != -1;
        var isProportionalWidth = isHtml && options['htmlProportionalWidth'] == "true";
        var isReportAlone = domClass.contains('toppanel', 'hidden');

        var isQueryLimitControlEnabled = me.reportPrompt._isAsync && options['query-limit-ui-enabled'] == "true";
        var requestLimit = parseInt(options['query-limit'], 0);
        var systemRowLimit = parseInt(options['maximum-query-limit'], 0);

        var styled = _isTopReportViewer && !isReportAlone &&
            isHtml && !isProportionalWidth &&
            !inMobile;

        // If the new output format causes a pageStyle change,
        // and we don't hide the iframe "now",
        // Then, when the iframe loads,
        // the user may temporarily see the new document
        // with the previous page style.
        if(me.view.isPageStyled() !== styled) {
          logger &&
          logger.log("Page style will change to " + (styled ? "" : "non-") +
              "styled. Will hide report before iframe update.");
          me.view._showReportContent(false, /*preserveSource*/true);
        }


        //Async report execution

        //This flag is switched to false after 1st page is ready - specific for paginated html
        var isFirstContAvStatus = true;
        //This flags is changed when report is finished and iframe is updated/ conent is downloaded
        var isIframeContentSet = false;
        //This flag is switched to true after we get total page count and updated page control
        var isPageCountUpdated = false;
        //Report generation finished ( also includes canceled and failed cases)
        //at this moment we block all new reportjob submissions see BACKLOG-11397
        me._isFinished = false;
        //Hides feedback screen and glasspane
        var hideDlgAndPane = this._hideDialogAndPane.bind(me);
        //Tells us if report was manually scheduled from the feedback screen
        me._manuallyScheduled = false;
        //Current requested page - is updated if user navigates after 1st page is shown
        me._requestedPage = me.view._getAcceptedPage();
        me._locationPromptFinished = false;
        me._locationOutputPath = me.reportPrompt._defaultOutputPath;


        //We are in async mode
        if (me.reportPrompt._isAsync) {

          /*Async callbacks code START*/
          var mainReportGeneration = dojo.hitch(me, function (result) {

            var mainJobStatus = me._getAsyncJobStatus(result, hideDlgAndPane);

            if (mainJobStatus && mainJobStatus.status != null) {

              if (isQueryLimitControlEnabled) {
                registry.byId('rowLimitControl') && registry.byId('rowLimitControl').apply(systemRowLimit, requestLimit, mainJobStatus.isQueryLimitReached);
              }

              me._updateFeedbackScreen(mainJobStatus, feedbackDialog);

              //Configure callback for first page
              var firtsPageReadyCallback = dojo.hitch(me, function (result2) {

                if (!me.reportPrompt._isReportHtmlPagebleOutputFormat) {
                  logger && logger.log("ERROR: " + "You are in 1st page callback not for paginated HTML. Something went wrong.");
                  return;
                }

                firstPageJobStatus = JSON.parse(result2);

                switch (firstPageJobStatus.status) {
                  case "QUEUED":
                  case "WORKING":
                    me._keepPolling(firstPageJobStatus.uuid, url, firtsPageReadyCallback);
                    break;
                  case  "FINISHED" :
                    me._isFinished = true;
                    me._hideAsyncScreens();
                    //Let's get first page
                    me._getContent(firstPageJobStatus.uuid, url, null, function () {
                      isIframeContentSet = true;
                    });

                    me._previousPage = firstPageJobStatus.page;

                    hideDlgAndPane(registry.byId('feedbackScreen'));

                    //Show loading screen
                    $('#notification-message').html(_Messages.getString('LoadingPage'));
                    $('#notification-screen').css("z-index", 100);
                    if (me._currentReportStatus == 'CONTENT_AVAILABLE') {
                      domClass.remove('notification-screen', 'hidden');
                    }
                    break;
                }
              });

              switch (mainJobStatus.status) {

                case "CONTENT_AVAILABLE":
                  if (isFirstContAvStatus) {
                    me._hideAsyncScreens();
                    isFirstContAvStatus = false;
                    //1st page is ready - need to spawn another job to get it
                    if (me._currentStoredPagesCount > me._requestedPage) {
                      //Prevent double content update on 1st page
                      me._requestedPage = -1;
                      pentahoPost('reportjob', url.substring(url.lastIndexOf("/report?") + "/report?".length, url.length), firtsPageReadyCallback, 'text/text');
                    }

                    me._keepPolling(mainJobStatus.uuid, url, mainReportGeneration);
                  } else {

                    if ((me._cachedReportCanceled && me._requestedPage == 0) || ((me._requestedPage >= 0) && (me._currentStoredPagesCount > me._requestedPage))) {
                      //adjust accepted page in url
                      var newUrl = url.substring(url.lastIndexOf("/report?") + "/report?".length, url.length);
                      newUrl = newUrl.replace(/(accepted-page=)\d*?(&)/, '$1' + me._requestedPage + '$2');
                      //BACKLOG-9814 distinguish initial 1st page from going back to the 1st page
                      me._requestedPage = -1;
                      me._cachedReportCanceled = false;
                      pentahoPost('reportjob', newUrl, firtsPageReadyCallback, 'text/text');
                      registry.byId('reportGlassPane').hide();
                    }

                    //update page number
                    if (me.reportPrompt._isReportHtmlPagebleOutputFormat && !isPageCountUpdated) {
                      var pageContr = registry.byId('pageControl');
                      pageContr.setPageCount(mainJobStatus.totalPages);
                      isPageCountUpdated = true;
                    }

                    $('#notification-message').html(_Messages.getString('LoadingPage') + " " + mainJobStatus.page + " " + _Messages.getString('Of') + " " + mainJobStatus.totalPages);
                    registry.byId('reportGlassPane').setText(_Messages.getString('LoadingPage') + " " + mainJobStatus.page + " " + _Messages.getString('Of') + " " + mainJobStatus.totalPages);

                    me._keepPolling(mainJobStatus.uuid, url, mainReportGeneration);
                  }
                  break;
                  //note - no break here - w e need to poll
                case "QUEUED":
                case "WORKING":
                  me._hideAsyncScreens();
                  me._keepPolling(mainJobStatus.uuid, url, mainReportGeneration);
                  break;
                case "FINISHED":

                  me._isFinished = true;

                  hideDlgAndPane(registry.byId('feedbackScreen'));
                  me._hideAsyncScreens();

                  //Show report
                  if (!isIframeContentSet) {
                    me._getContent(mainJobStatus.uuid, url, mainJobStatus.mimeType, function () {
                      isIframeContentSet = true;
                    });

                    //Callback for downloadable types because they don't have iframe callback
                    if (me._isDownloadableFormat(mainJobStatus.mimeType)) {
                      setTimeout(function () {
                        me._submitReportEnded(true);
                      }, me.reportPrompt._pollingInterval);
                    }
                  }


                  if (me._requestedPage > 0) {
                    // main request finished before requested page was stored in cache but wee still need to show valif page
                    var newUrl = url.substring(url.lastIndexOf("/report?") + "/report?".length, url.length);
                    var match = newUrl.match(/(^.*accepted-page=)(\d*?)(&.*$)/);
                    //if not handled by another job
                    if (match[2] != me._requestedPage) {
                      newUrl = match[1] + me._requestedPage + match[3];
                      me._requestedPage = 0;
                      pentahoPost('reportjob', newUrl, firtsPageReadyCallback, 'text/text');
                    }
                  }

                  //Set total number of pages for paginated HTML
                  if (me.reportPrompt._isReportHtmlPagebleOutputFormat && !isPageCountUpdated) {
                    var pageContr = registry.byId('pageControl');
                    pageContr.setPageCount(mainJobStatus.totalPages);
                    isPageCountUpdated = true;
                  }

                  break;
                case "FAILED":
                  me._isFinished = true;
                  me._submitReportEnded();

                  //Hide dialogs and show error
                  var errorMsg = _Messages.getString('DefaultErrorMessage');
                  if (mainJobStatus.errorMessage != null) {
                    errorMsg = mainJobStatus.errorMessage;
                  }
                  me.reportPrompt.showMessageBox(
                      errorMsg,
                      _Messages.getString('ErrorPromptTitle'));
                  registry.byId('feedbackScreen').hide();
                  me._hideAsyncScreens();
                  me._updateParametersDisabledState(false);

                  logger && logger.log("ERROR: Request status - FAILED");

                  break;
                case "PRE_SCHEDULED":
                   //Could occur when auto scheduling or manual scheduling + location prompt
                  if(me._locationPromptFinished){
                    me._keepPolling(mainJobStatus.uuid, url, mainReportGeneration);
                  } else if (!me._manuallyScheduled){
                    me._isFinished = true;
                    var autoScheduleDlg = registry.byId('scheduleScreen');
                    autoScheduleDlg.setTitle(_Messages.getString('AutoScheduleTitle'));
                    autoScheduleDlg.setText(_Messages.getString('AutoScheduleText'));
                    autoScheduleDlg.setOkBtnText(_Messages.getString('FeedbackScreenBackground'));
                    autoScheduleDlg.setCancelBtnText(_Messages.getString('ScreenCancel'));

                    autoScheduleDlg.callbacks = me._getAutoScheduleScreenBtnCallbacks(mainReportGeneration, url);

                    registry.byId('feedbackScreen').hide();
                    autoScheduleDlg.show();
                  }
                  break;
                case "SCHEDULED":
                  //Scheduling is confirmed, the task is not cancelable anymore
                  me._isFinished = true;
                  me._submitReportEnded();
                  me._hideAsyncScreens();

                  var successDlg = me._getSuccessSchedulingDlg();

                  registry.byId('feedbackScreen').hide(); // glasspane is still needed
                  successDlg.show();

                  break;
                case "CANCELED":
                  me._submitReportEnded();
                  me._hideAsyncScreens();
                  break;
              }
            }
            return mainJobStatus;
          });

          //Async execution manages this flag in it's own way
          me.reportPrompt._isSubmitPromptPhaseActivated = false;

          var scheduleScreenBtnCallbacks = me._getScheduleScreenBtnCallbacks(mainReportGeneration, url, hideDlgAndPane);
          _scheduleScreenBtnCallbacks = scheduleScreenBtnCallbacks.slice();

          var feedbackDialog = me._getFeedbackScreen(scheduleScreenBtnCallbacks);

          //Don't show dialog if report is ready faster than threshold
          setTimeout(function () {
            if (!me._isFinished) {
              feedbackDialog.show();
            }
          }, me.reportPrompt._dialogThreshold);

          /*Async callbacks code END*/

          //Main report job start and page requests goes below

          var reportUrl = url.substring(url.lastIndexOf("/report?") + "/report?".length, url.length);

          switch (me._currentReportStatus) {
            case 'CONTENT_AVAILABLE':
              //This section should only affect paginated HTML after 1st page is recieved

              //Check if requested page is already persisted to cache
              if (me._currentStoredPagesCount > me._requestedPage) {
                //Page available
                var current = reportUrl.match(/(^.*accepted-page=)(\d*?)(&.*$)/);
                var running = me._reportUrl.match(/(^.*accepted-page=)(\d*?)(&.*$)/);

                if (current && running && current[1] != running[1]) {
                  //Actually user changed not the page but prompts/output target - we need a new job to get it
                  me._reportUrl = reportUrl;
                  me.cancel(me._currentReportStatus, me._currentReportUuid, dojo.hitch(me, function(){
                    me._isFinished = false;
                    pentahoPost('reportjob', reportUrl, mainReportGeneration, 'text/text');
                  }));
                  me._hideAsyncScreens();
                } else {
                  //Page navigation occurred  - callbacks will do the job
                  me._isFinished = true;
                }
              } else {
                //Need to request cache flush
                me._requestCacheFlush(url);
                me._isFinished = true;
              }
              break;
            default:
              me._hideAsyncScreens();
              //Not started or finished
              this._reportUrl = reportUrl;
              var isValid = !me.reportPrompt._getStateProperty('promptNeeded');
              if (isValid) {
                pentahoPost(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/reserveId', "", function (data) {
                  try {
                    me._currentReportUuid = JSON.parse(data).reservedId;
                    // backlog-10041 do not send GET request with overloaded parameters passed via url
                    pentahoPost('reportjob', reportUrl + "&reservedId=" + me._currentReportUuid, mainReportGeneration, 'text/text');
                  } catch (e) {
                    logger && logger.log("Can't reserve id");
                    pentahoPost('reportjob', reportUrl, mainReportGeneration, 'text/text');
                  }
                }, "application/json");
              } else {
                me._isFinished = true;
                hideDlgAndPane();
              }
              break;
          }

        } else {

          me._isFinished = true;
          // now user is able to click on controls after the curtain is hidden.
          me._hideAsyncScreens();

          logger && logger.log("Will set iframe url to " + url.substr(0, 50) + "... ");
          
          if (me._isIE11() && outputFormat === 'pageable/pdf') {
            $('#reportContent').attr("onreadystatechange", "viewer.forceLoadEvent();");
          }
          
          //submit hidden form to POST data to iframe
          $('#hiddenReportContentForm').attr("action", url);
          $('#hiddenReportContentForm').submit();
          //set data attribute so that we know what url is currently displayed in
          //the iframe without actually triggering a GET
          $('#reportContent').attr("data-src", url);
          me._updatedIFrameSrc = true;
        }

        // Continue when iframe is loaded (called by #_onReportContentLoaded)
        me._submitLoadCallback = logged('_submitLoadCallback', function() {
          me._isHtmlReport = me.view._isHtmlReport = isHtml;
          me._outputFormat = outputFormat;

          var visible = me.view._calcReportContentVisibility();
          if(visible) {
            // A child viewer forces changing to non-styled
            me.view.setPageStyled(styled && !me._isParentViewer);
            me.view.resize();
          }

          me.view._showReportContent(visible);

          me._submitReportEnded();
        });
      },

      _buildParameter: function (pathArray, id) {
        var path;
        for (var i = 0; i < pathArray.length; i++) {
          if (pathArray[i].indexOf(".prpt") !== -1) {
            path = decodeURIComponent(pathArray[i]).replace(/:/g, "/");
          }
        }
        return {
          solutionPath: (path == null ? "/" : path ),
          jobId: id,
          recalculateFinished: true == this.reportPrompt._isReportHtmlPagebleOutputFormat
        };
      },

      _getSuccessSchedulingDlg: function () {

        var me = this;
        var successDlg = registry.byId('successScheduleScreen');
        successDlg.setTitle(_Messages.getString('SuccessScheduleTitle'));
        successDlg.setText(_Messages.getString('SuccessScheduleText', '<b><i>' + me._locationOutputPath +'</i></b>'));
        successDlg.setOkBtnText(_Messages.getString('OK'));

        var successScheduleDialogCallbacks = [
          function hide() {
            me._updateParametersDisabledState(false);
            me._forceHideGlassPane();
            successDlg.hide();
          }.bind(me)
        ];

        successDlg.callbacks = successScheduleDialogCallbacks;
        return  successDlg;
      },

      _forceHideGlassPane: function (){
        $("#glasspane").css("background", "transparent");
        $("#glasspane").css("display", "none");
      },

      _forceShowGlassPane: function (){
        $("#glasspane").css("background", "");
        $("#glasspane").css("display", "block");
      },

      _onLocationPromptCancel : function (mainReportGeneration, url) {
        var me = this;
        return function (event) {
          if (event.eventSubType == 'locationPromptCanceled') {
            me._locationPromptFinished = true;
            //Show glass pane and feedback screen
            me._forceShowGlassPane();
            registry.byId('feedbackScreen').show();
            var specialCaseProxy = dojo.hitch(me, function (result) {

              if (me.reportPrompt._isReportHtmlPagebleOutputFormat) {
                var mainJobStatus;
                try {
                  /*If pre scheduled paginated HTML report is finished it will contain all the pages
                   but we need only one. So we spawn a new job and get results from cache.
                   */
                  mainJobStatus = JSON.parse(result);
                  if (mainJobStatus.status == 'FINISHED') {
                    pentahoPost('reportjob', me._reportUrl, mainReportGeneration, 'text/text');
                  } else {
                    mainReportGeneration(result);
                  }
                } catch (e) {
                  mainReportGeneration(result);
                }
              } else {
                mainReportGeneration(result);
              }
            });
            //Keep polling
            setTimeout(function () {
              pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + me._currentReportUuid + '/status',
                "", specialCaseProxy, "application/json");
            }, me.reportPrompt._pollingInterval);
            me._removeLocationPromptHandlers();
          }
        };
      },

      _onLocationPromptOk : function (mainReportGeneration, url) {
        var me = this;
        return function (event) {
          if (event.eventSubType == 'locationPromptOk') {
            try {
              me._locationOutputPath = event.stringParam;
            } catch (e) {
              logger && logger.log("ERROR" + String(e));
            }
            me._locationPromptFinished = true;
            me._forceShowGlassPane();
            var waitForScheduled = dojo.hitch(me, function (result) {
              try {
                mainJobStatus = JSON.parse(result);
                if (mainJobStatus.status == 'SCHEDULED' || mainJobStatus.status == 'FAILED') {
                  mainReportGeneration(result);
                } else {
                  setTimeout(function () {
                    pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + me._currentReportUuid + '/status',
                      "", waitForScheduled, "application/json");
                  }, me.reportPrompt._pollingInterval);
                }
              } catch (e) {
                mainReportGeneration(result);
              }
            });
            //Keep polling
            setTimeout(function () {
              pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + me._currentReportUuid + '/status',
                "", waitForScheduled, "application/json");
            }, me.reportPrompt._pollingInterval);
            me._removeLocationPromptHandlers();
          }
        };
    },

      _onLocationPromptFinish : function(){
      var me = this;
      return function (event) {
        if (event.eventSubType == 'locationPromptFinish') {
          me._currentReportUuid = event.stringParam;
          //A paranoid check, must never be called if no mantle application is available
          if (isRunningIFrameInSameOrigin && window.top.mantle_removeHandler) {
            if (me._locationPromptFinishHandlerRegistration) {
              window.top.mantle_removeHandler(me._locationPromptFinishHandlerRegistration);
            }
          }
        }
      };
      },

      _onLocationPromptCancelAuto : function() {
        var me = this;
        return function (event) {
          if (event.eventSubType == 'locationPromptCanceled') {
            me._updateParametersDisabledState(false);
            me._forceHideGlassPane();
            me.cancel(me._currentReportStatus, me._currentReportUuid);
            me._removeLocationPromptHandlers();
          }
        };
      },

      _onLocationPromptOkAuto : function(mainReportGeneration, url) {
        var me = this;
        return function (event) {
          if (event.eventSubType == 'locationPromptOk') {
            me._locationOutputPath = event.stringParam;
            me._locationPromptFinished = true;
            me._forceShowGlassPane();
            var waitForScheduled = dojo.hitch(me, function (result) {
              try {
                mainJobStatus = JSON.parse(result);
                if (mainJobStatus.status == 'SCHEDULED' || mainJobStatus.status == 'FAILED') {
                  mainReportGeneration(result);
                } else {
                  setTimeout(function () {
                    pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + me._currentReportUuid + '/status',
                      "", waitForScheduled, "application/json");
                  }, me.reportPrompt._pollingInterval);
                }
              } catch (e) {
                mainReportGeneration(result);
              }
            });
            setTimeout(function () {
              pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + me._currentReportUuid + '/status',
                "", waitForScheduled, "application/json");
            }, me.reportPrompt._pollingInterval);

            me._removeLocationPromptHandlers();
          }
        };
      },

      _removeLocationPromptHandlers: function(){
        if (isRunningIFrameInSameOrigin && window.top.mantle_removeHandler) {
          if(this._locationPromptCancelHandlerRegistration) {
            window.top.mantle_removeHandler(this._locationPromptCancelHandlerRegistration);
          }
          if(this._locationPromptOkHandlerRegistration) {
            window.top.mantle_removeHandler(this._locationPromptOkHandlerRegistration);
          }
        }
      },

      _getScheduleScreenBtnCallbacks : function (mainReportGeneration, url, hideDlgAndPane) {
        var me = this;
        return [
        //Schedule button callback
        function scheduleReport() {
          var urlSchedule = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + me._currentReportUuid + '/schedule';
          pentahoGet(urlSchedule, "confirm=" + !me.reportPrompt._promptForLocation);
          me._manuallyScheduled = true;

          registry.byId('feedbackScreen').hide();
          if (me.reportPrompt._promptForLocation) {
            //A paranoid check, must never be called if no mantle application is available
            if (isRunningIFrameInSameOrigin) {
              if (window.top.mantle_addHandler) {
                me._locationPromptCancelHandlerRegistration = window.top.mantle_addHandler("GenericEvent",
                    this._onLocationPromptCancel(mainReportGeneration, url).bind(this));
                me._locationPromptOkHandlerRegistration = window.top.mantle_addHandler("GenericEvent", this._onLocationPromptOk(mainReportGeneration, url).bind(this));
                me._locationPromptFinishHandlerRegistration = window.top.mantle_addHandler("GenericEvent", this._onLocationPromptFinish().bind(this));
              }
              //Open location prompt
              me._locationPromptFinished = false;
              window.top.executeCommand("AdhocRunInBackgroundCommand", me._buildParameter(pathArray, me._currentReportUuid));
            }
          }
        }.bind(me),
        //Cancel report
        function feedbackscreenDone() {
          me.cancel(me._currentReportStatus, me._currentReportUuid);
          hideDlgAndPane(registry.byId('feedbackScreen'));
        }.bind(me)
      ]
      },

      _getAutoScheduleScreenBtnCallbacks : function (mainReportGeneration, url){

        var me = this;

        var autoScheduleDialogCallbacks;

        var  autoScheduleDlg = registry.byId('scheduleScreen');

        if(me.reportPrompt._promptForLocation) {

          autoScheduleDialogCallbacks = [
            function openPrompt() {
              autoScheduleDlg.hide();
              if (isRunningIFrameInSameOrigin) {
                if (window.top.mantle_addHandler) {
                  me._locationPromptCancelHandlerRegistration = window.top.mantle_addHandler("GenericEvent", this._onLocationPromptCancelAuto().bind(this));
                  me._locationPromptOkHandlerRegistration = window.top.mantle_addHandler("GenericEvent", this._onLocationPromptOkAuto(mainReportGeneration, url).bind(this));
                  me._locationPromptFinishHandlerRegistration = window.top.mantle_addHandler("GenericEvent", this._onLocationPromptFinish().bind(this));
                }


                //Open location prompt
                me._locationPromptFinished = false;
                window.top.executeCommand("AdhocRunInBackgroundCommand", me._buildParameter(pathArray, me._currentReportUuid));
              }
            }.bind(me),
            function cancel() {
              autoScheduleDlg.hide();
              me._updateParametersDisabledState(false);
              me._forceHideGlassPane();
              me.cancel(me._currentReportStatus, me._currentReportUuid);
            }.bind(me)
          ];
        }else{
          autoScheduleDialogCallbacks = [
            function confirm() {
              autoScheduleDlg.hide();
              var urlSchedule = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + me._currentReportUuid + '/schedule';
              pentahoGet(urlSchedule, "confirm=true");
              me._locationPromptFinished = true;
              me._keepPolling(me._currentReportUuid, url, mainReportGeneration);
            }.bind(me),
            function cancel() {
              autoScheduleDlg.hide();
              me._updateParametersDisabledState(false);
              me._forceHideGlassPane();
              me.cancel(me._currentReportStatus, me._currentReportUuid);
            }.bind(me)
          ];
        }

        return autoScheduleDialogCallbacks;
      },

      _keepPolling : function (uuid, url, callback){
        var me = this;
        setTimeout(function () {
          pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + uuid + '/status', "", callback, "application/json");
        }, me.reportPrompt._pollingInterval);
      },

      _getContent : function (uuid, url, mimeType, callback) {
        var me = this;
        var urlContent = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + uuid + '/content';
        logger && logger.log("Will set iframe url to " + urlContent.substr(0, 50) + "... ");

        var isPdf = (mimeType === "application/pdf");
        
        if (me._isIE11() && isPdf) {
          $('#reportContent').attr("onreadystatechange", "viewer.forceLoadEvent();");
        }
        
        $('#hiddenReportContentForm').attr("action", urlContent);
        $('#hiddenReportContentForm').submit();
        $('#reportContent').attr("data-src", urlContent);
        me._updatedIFrameSrc = true;
        me.reportPrompt._isSubmitPromptPhaseActivated = true;
        if(callback){
          callback();
        }
      },
      
      //IE11 workaround when no "load" event after Pdf pushed to iframe (BISERVER-12804)
      forceLoadEvent : function(){
        var boundOnReportContentLoaded = this._onReportContentLoaded.bind(this);
        var onFrameLoaded = logged('onFrameLoaded', function() {
          setTimeout(boundOnReportContentLoaded);
        });
        onFrameLoaded();
        $('#reportContent').removeAttr("onreadystatechange");  //should be invoked only once, so remove after the first invocation
      },

      _getAsyncJobStatus : function(result, hideDlgAndPane){
        var me = this;
        var mainJobStatus;
        try {
          mainJobStatus = JSON.parse(result);
          return mainJobStatus;
        } catch (e) {
          var errorMessage = _Messages.getString('DefaultErrorMessage');
          this.reportPrompt.showMessageBox(
              errorMessage,
              _Messages.getString('ErrorPromptTitle'));
          hideDlgAndPane(registry.byId('feedbackScreen'));
          me._hideAsyncScreens();
          logger && logger.log("ERROR" + String(e));
          return;
        }
      },

      _updateFeedbackScreen: function (mainJobStatus, feedbackDialog) {
        var me = this;
        //Update current values
        me._currentReportStatus = mainJobStatus.status;
        me._currentReportUuid = mainJobStatus.uuid;
        me._currentStoredPagesCount = mainJobStatus.generatedPage;

        //Update feedback screen ui
        if (mainJobStatus.activity != null) {
          feedbackDialog.setText(_Messages.getString(mainJobStatus.activity) + '...');
        }
        feedbackDialog.setText2(_Messages.getString('FeedbackScreenPage') + ': ' + mainJobStatus.page);
        feedbackDialog.setText3(_Messages.getString('FeedbackScreenRow') + ': ' + mainJobStatus.row + ' / ' + mainJobStatus.totalRows);

        //Set progress bar %
        progressBar.set({value: mainJobStatus.progress});
      },

      _hideDialogAndPane: function(dlg) {
        if(dlg) {
          dlg.hide();
        }
        if(this._updateReportTimeout >= 0) {
          clearTimeout(this._updateReportTimeout);
          this._updateReportTimeout = -1;
        }
        this._forceHideGlassPane();
        this._updateParametersDisabledState(false);
      },

      _submitReportEnded: function(isTimeout) {
        // Clear submit-related control flags
        delete this.reportPrompt._isSubmitPromptPhaseActivated;
        delete this.reportPrompt._isUpdatingPrompting;

        // Awaiting for update report response?
        if(this._updateReportTimeout >= 0) {
          clearTimeout(this._updateReportTimeout);
          this._updateReportTimeout = -1;

          if(isTimeout) {
            // This happens, specifically, when the user selects a downloadable output format.
            // #_onReportContentLoaded callback might not have been called.
            this.view._showReportContent(false, /*preserveSource*/true);
          }
        }
        // PRD-3962 - show glass pane on submit, hide when iframe is loaded
        // Hide glass-pane, if it is visible
        if(!this.reportPrompt._isAsync) {
          this._updateParametersDisabledState(false);
          this.reportPrompt.hideGlassPane();
        }
      },

      _onReportContentLoaded: function() {
        var hadChildViewer = this._isParentViewer;

        this._detectLoadedContent();

        var view = this.view;
        if(!this._updatedIFrameSrc) {
          if(!view._hasReportContent()) {
            logger && logger.log("Empty IFrame loaded.");
          } else {
            // A link from within the loaded report
            // caused loading something else.
            // It may be a child report viewer.
            if(this._isParentViewer && !hadChildViewer) {
              // A child viewer forces changing to non-styled
              view.setPageStyled(false);
              view.resize();
            } else {
              logger && logger.log("Unrequested IFrame load.");
            }
          }
        } else {
          this._updatedIFrameSrc = false;
          var loadCallback = this._submitLoadCallback;
          if(loadCallback && view._hasReportContent()) {
            delete this._submitLoadCallback;
            loadCallback.call(this);
          } else {
            view.resize();
          }
        }
      },

      _detectLoadedContent: function() {
        // TODO: Should include HTML test here as well?
        var isParentViewer = false;
        try {
          var contentWin = dom.byId('reportContent').contentWindow;
          if(contentWin) {
            if(contentWin._isReportViewer) {
              isParentViewer = true;
            }
            else {
              // For testing in IPads or other clients,
              // remove hardcoded localhost link urls.
              $(contentWin.document)
              .find('body area').each(function() {
                this.href = this.href.replace("http://localhost:8080", "");
              });
            }
          }
        } catch(e) {
          // Permission denied
          logger && logger.log("ERROR" + String(e));
        }

        this._isParentViewer = isParentViewer;
        $('body')
          [ isParentViewer ? 'addClass' : 'removeClass']('parentViewer')
          [!isParentViewer ? 'addClass' : 'removeClass']('leafViewer'  );
      },

      _buildReportContentOptions: function() {
        var options = this.reportPrompt._buildReportContentOptions('REPORT');

        // SimpleReportingComponent expects name to be set
        if (options['name'] === undefined) {
          options['name'] = options['action'];
        }

        return options;
      },

      _buildReportContentUrl: function(options) {
        var url = window.location.href.split('?')[0];
        url = url.substring(0, url.lastIndexOf("/")) + "/report?";

        var params = [];
        var addParam = function(encodedKey, value) {
          if(typeof value !== 'undefined') {
            params.push(encodedKey + '=' + encodeURIComponent(value));
          }
        };

        $.each(options, function(key, value) {
          if (value == null) { return; } // continue

          var encodedKey = encodeURIComponent(key);
          if ($.isArray(value)) {
            $.each(value, function(i, v) { addParam(encodedKey, v); });
          } else {
            addParam(encodedKey, value);
          }
        });

        return url + params.join("&");
      },

      _updateParametersDisabledState: function(disable) {
        var testElements = document.getElementsByClassName('parameter');
        for(var i=0; i<testElements.length; i++) {
          if(testElements[i].getElementsByTagName('select').length > 0) {
            testElements[i].getElementsByTagName('select')[0].disabled = disable;
          } else if(testElements[i].getElementsByTagName('input').length > 0) {
            for(var j=0; j<testElements[i].getElementsByTagName('input').length; j++) {
              testElements[i].getElementsByTagName('input')[0].disabled = disable;
            }
          } else if(testElements[i].getElementsByTagName('button').length > 0) {
            for(var j=0; j<testElements[i].getElementsByTagName('button').length; j++) {
              testElements[i].getElementsByTagName('button')[j].disabled = disable;
            }
          }
        }
      },

      _isDownloadableFormat: function (mime) {
        var mimes = ["application/rtf", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "text/csv", "mime-message/text/html"];
        return mimes.indexOf(mime) > -1;
      },
      
      _isIE11: function(){
        return has("trident") && !has("ie"); //has("ie") in IE11 is undefined
      }

    }); // end of: var v = {

    // Replace default prompt load
    reportPrompt.load = v.load.bind(v);
    return v;
  };
});
