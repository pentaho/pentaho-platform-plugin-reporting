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
* Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
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
      _isReportHtmlPagebleOutputFormat : null,
      _reportUrl : null,
      _handlerRegistration : null,

      _bindPromptEvents: function() {
        var baseShowGlassPane = this.reportPrompt.showGlassPane.bind(this.reportPrompt);
        var baseHideGlassPane = this.reportPrompt.hideGlassPane.bind(this.reportPrompt);

        this.reportPrompt.api.event.ready(this.view.promptReady.bind(this.view));
        this.reportPrompt.showGlassPane = this.view.showGlassPane.bind(this.view,  baseShowGlassPane);
        this.reportPrompt.hideGlassPane = this.view.hideGlassPane.bind(this.view,  baseHideGlassPane);
        this.reportPrompt.api.event.submit(this.submitReport.bind(this));
        this.reportPrompt.api.event.beforeUpdate(this.beforeUpdateCallback.bind(this));
        this.reportPrompt.api.event.afterUpdate(this.afterUpdateCallback.bind(this));
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

        window.onbeforeunload = function(e) {
          this.cancel(this._currentReportStatus, this._currentReportUuid);
          return;
        }.bind(this);

        $("#reportContent")[0].contentWindow.onbeforeunload = function(e) {
          if($("#reportContent")[0].contentWindow._isFirstIframeUrlSet == true) {
            //user clicking a link in the report
            this.cancel(this._currentReportStatus, this._currentReportUuid);
          } else {
            //content is writing in the reportContent iframe first time
            $("#reportContent")[0].contentWindow._isFirstIframeUrlSet = true;
          }
          return;
        }.bind(this);

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
          registry.byId('pageControl').registerLocalizationLookup(_Messages.getString);
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
              $('#hiddenReportContentForm').attr("action", 'about:blank');
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

        _hasReportContent: function() {
          var src = $('#reportContent').attr('data-src');
          return src !== undefined && src !== 'about:blank';
        },

        _isReportContentVisible: function() {
          return !$('body').hasClass('contentHidden');
        },

        _isHtmlPagebleOutputFormat : function(outputFormat) {
          return outputFormat.indexOf('table/html;page-mode=page') !== -1;
        },

        // Called on page load and every time the prompt panel is refreshed
        updateLayout: function() {
          if (!this.reportPrompt._getStateProperty('showParameterUI')) {
            this._hideToolbarPromptControls();
          }

          // The following call is important for clearing the report content when autoSubmit=false and the user has changed a value.
          if(!this._calcReportContentVisibility()) {
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

            if (typeof window.parameterValidityCallback !== 'undefined') {
              var isValid = !this.reportPrompt._getStateProperty('promptNeeded');
              window.parameterValidityCallback(isValid);
            }
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
          var isToolbarEmpty = !this.reportPrompt._getStateProperty("paginate") && !showParamUI && !window.viewer._isReportHtmlPagebleOutputFormat;
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

          var mb = {h: vp.h - tp.h - 2};

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
          win2.withDoc(t.contentWindow.document, function(){
            // add overflow hidden to prevent scrollbars on ie9 inside the report
            domStyle.set(win2.doc.getElementsByTagName("html")[0], {'overflow': 'hidden'});

            var dimensions = geometry.getMarginBox(win2.doc.getElementsByTagName("body")[0]);

            // changing width to jquery due to problems with dojo getting the width correcty
            // although, dojo is used to get the height due to issues on ie8 and 9
            dimensions.w = $('#reportContent').contents().width();

            logger && logger.log("Styled page - polled dimensions = (" + dimensions.w + ", " + dimensions.h + ")");

            // In case the styled report content is too small, assume 2/3 of the width.
            // This may happen when there are no results.
            if(dimensions.w <= POLL_SIZE) {
              // Most certainly this indicates that the loaded report content
              // does not have a fixed width, and, instead, adapts to the imposed size (like width: 100%).

              var vp;
              win2.withDoc(outerDoc, function() { vp = win.getBox(); });

              dimensions.w = Math.round(2 * vp.w / 3);
              logger && logger.log("Width is too small - assuming a default width of " + dimensions.w);
            }

            geometry.setContentSize(t, {w: dimensions.w, h: dimensions.h});
          });
        }
      }), // end view

      onTabCloseEvent: function (event) {
        if (window && event.eventSubType == 'tabClosing' && event.stringParam == window.frameElement.id) {
          this.cancel(this._currentReportStatus, this._currentReportUuid);
          if(top.mantle_removeHandler) {
            top.mantle_removeHandler(this._handlerRegistration);
          }
        }
      },

      cancel: function(status, uuid) {
        var url = window.location.href.split('?')[0];
        if(status == 'WORKING' || status == 'QUEUED' || status == 'CONTENT_AVAILABLE') {
          pentahoGet(url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + uuid + '/cancel', "");
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

        var isRunningIFrameInSameOrigin = null;
        try {
          var ignoredCheckCanReachOutToTop = window.top.mantle_initialized;
          isRunningIFrameInSameOrigin = true;
        } catch (ignoredSameOriginPolicyViolation) {
          // IFrame is running embedded in a web page in another domain
          isRunningIFrameInSameOrigin = false;
        }

        if(isRunningIFrameInSameOrigin) {
          if (!top.mantle_initialized) {
            top.mantle_openTab = function(name, title, url) {
              window.open(url, '_blank');
            }
          }

          if (top.mantle_initialized) {
            top.reportViewer_openUrlInDialog = function(title, url, width, height) {
              top.urlCommand(url, title, true, width, height);
            };
          } else {
            top.reportViewer_openUrlInDialog = this.openUrlInDialog.bind(this);
          }

          window.reportViewer_openUrlInDialog = top.reportViewer_openUrlInDialog;
        }

        window.reportViewer_hide = this.hide.bind(this);

        if(top.mantle_addHandler) {
          this._handlerRegistration = top.mantle_addHandler("GenericEvent", this.onTabCloseEvent.bind(this));
        }

        var localThis = this;

        if (isRunningIFrameInSameOrigin && typeof window.top.addGlassPaneListener !== 'undefined') {
          window.top.addGlassPaneListener({
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
        this.reportPrompt._isUpdatingPrompting = true;
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

          if (!this.reportPrompt._getStateProperty("autoSubmit") && !this.reportPrompt._isAsync) {
            this.reportPrompt.mode = 'MANUAL';
            this.reportPrompt.api.operation.refreshPrompt();
          } else if (!this.reportPrompt._isUpdatingPrompting) { // no need updating report content during submit because we have afterUpdate event subscription
            this._updateReportContentCore();
          }
        } catch(ex) {
          this._submitReportEnded();
          throw ex;
        }
      },

      _updatedIFrameSrc: false,
      _updateReportTimeout: -1,

      reportContentUpdating: function() {
        return this._updateReportTimeout >= 0;
      },

      _updateReportContent: function() {
        if (this.reportPrompt._isSubmitPromptPhaseActivated || this.reportPrompt._getStateProperty("autoSubmit")) {
          this._updateReportContentCore();
        }
      },

      _updateReportContentCore: function() {
        var me = this;

        // PRD-3962 - remove glass pane after 5 seconds in case iframe onload/onreadystatechange was not detected
        me._updateReportTimeout = setTimeout(logged('updateReportTimeout', function() {
          me._submitReportEnded(/*isTimeout*/true);
        }), 5000);

        // PRD-3962 - show glass pane on submit, hide when iframe is loaded.
        // Must be done AFTER _updateReportTimeout has been set, cause it's used to know
        // that the report content is being updated.
        me.reportPrompt.showGlassPane();

        var options = me._buildReportContentOptions();
        var url = me._buildReportContentUrl(options);
        this._isReportHtmlPagebleOutputFormat=me.view._isHtmlPagebleOutputFormat(options['output-target']);

        //BISERVER-1225
        var isFirstContAvStatus = true;
        var isIframeContentSet = false;
        var isFinished = false;
        this._requestedPage = me.view._getAcceptedPage();

        if(this.reportPrompt._isAsync) {
          var dlg = registry.byId('feedbackScreen');
          dlg.setTitle(_Messages.getString('ScreenTitle'));
          dlg.setText(_Messages.getString('FeedbackScreenActivity'));
          dlg.setText2(_Messages.getString('FeedbackScreenPage'));
          dlg.setText3(_Messages.getString('FeedbackScreenRow'));
          dlg.setCancelText(_Messages.getString('ScreenCancel'));

          if(this._isReportHtmlPagebleOutputFormat){
            dlg.hideBackgroundBtn();
            dlg.callbacks = [function feedbackscreenDone() {
              this.cancel(this._currentReportStatus, this._currentReportUuid);
              dlg.hide();
            }.bind(this)
            ];
          } else {
            dlg.showBackgroundBtn(_Messages.getString('FeedbackScreenBackground'));
            dlg.callbacks = [
              function scheduleReport() {
                var urlSchedule = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + this._currentReportUuid + '/schedule';
                pentahoGet( urlSchedule, "");
                dlg.hide();
              }.bind(this),
              function feedbackscreenDone() {
                this.cancel(this._currentReportStatus, this._currentReportUuid);
                dlg.hide();
              }.bind(this)
            ];
          }


          setTimeout(function () {
            if(!isFinished){
              dlg.show();
            }
          }, this.reportPrompt._dialogThreshold);
        }

        var outputFormat = options['output-target'];
        var isHtml = outputFormat.indexOf('html') != -1;
        var isProportionalWidth = isHtml && options['htmlProportionalWidth'] == "true";
        var isReportAlone = domClass.contains('toppanel', 'hidden');

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

        if(this.reportPrompt._isAsync) {
          var handleResultCallback = dojo.hitch(this, function(result) {
              var resultJson;
              try {
                resultJson = JSON.parse(result);
              } catch(e) {
                var errorMessage = "Invalid request";
                this.reportPrompt.showMessageBox(
                  errorMessage,
                  _Messages.getString('FatalErrorTitle'));
                dlg.hide();
                logger && logger.log("ERROR" + String(e));
                return;
              }
              if(resultJson.status != null) {
                if(resultJson.activity != null) {
                  dlg.setText(_Messages.getString(resultJson.activity) + '...');
                }
                dlg.setText2(_Messages.getString('FeedbackScreenPage') + ': ' + resultJson.page);
                dlg.setText3(_Messages.getString('FeedbackScreenRow') + ': ' + resultJson.row + ' / ' + resultJson.totalRows);
                this._currentReportStatus = resultJson.status;
                this._currentReportUuid = resultJson.uuid;
                this._currentStoredPagesCount = resultJson.generatedPage;

                progressBar.set({value: resultJson.progress});

                var handleContAvailCallback = dojo.hitch(this, function(result2) {
                  resultJson2 = JSON.parse(result2);
                  if(resultJson2.status == "QUEUED" || resultJson2.status == "WORKING") {
                    var urlStatus2 = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + resultJson2.uuid + '/status';
                    setTimeout(function(){ pentahoGet(urlStatus2, "", handleContAvailCallback); }, this.reportPrompt._pollingInterval);
                  } else if (resultJson2.status == "FINISHED") {
                    var urlContent2 = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + resultJson2.uuid + '/content';
                    logger && logger.log("Will set iframe url to " + urlContent2.substr(0, 50) + "... ");

                    $('#hiddenReportContentForm').attr("action", urlContent2);
                    $('#hiddenReportContentForm').submit();
                    $('#reportContent').attr("data-src", urlContent2);
                    this._updatedIFrameSrc = true;
                    dlg.hide();
                    isIframeContentSet = true;
                    $('#notification-message').html(_Messages.getString('LoadingPage'));
                    if(this._currentReportStatus && this._currentReportStatus!='FINISHED' && this._currentReportStatus!='FAILED' && this._currentReportStatus!='CANCELED'){
                      domClass.remove('notification-screen', 'hidden');
                    }
                    this._previousPage = resultJson2.page;
                  }
                });

                if(resultJson.status == "QUEUED" || resultJson.status == "WORKING") {
                  var urlStatus = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + resultJson.uuid + '/status';
                  setTimeout(function(){ pentahoGet(urlStatus, "", handleResultCallback); }, this.reportPrompt._pollingInterval);
                } else if (resultJson.status == "CONTENT_AVAILABLE") {
                  if(isFirstContAvStatus) {
                    isFirstContAvStatus = false;

                    if(this._currentStoredPagesCount > this._requestedPage){
                      pentahoGet('reportjob', url.substring(url.lastIndexOf("/report?")+"/report?".length, url.length), handleContAvailCallback, 'text/text');
                    }

                    var urlStatus = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + resultJson.uuid + '/status';
                    setTimeout(function(){ pentahoGet(urlStatus, "", handleResultCallback); }, this.reportPrompt._pollingInterval);
                  } else {

                    if( (this._cachedReportCanceled && this._requestedPage == 0)  || ((this._requestedPage > 0) && (this._currentStoredPagesCount > this._requestedPage))) {
                      //adjust accepted page in url
                      var newUrl =url.substring(url.lastIndexOf("/report?")+"/report?".length, url.length);
                      newUrl = newUrl.replace(/(accepted-page=)\d*?(&)/,'$1' + this._requestedPage + '$2');
                      this._requestedPage = 0;
                      this._cachedReportCanceled = false;
                      pentahoGet('reportjob', newUrl , handleContAvailCallback, 'text/text');
                      registry.byId('reportGlassPane').hide();
                    }

                    //update page number
                    var pageContr = registry.byId('pageControl');
                    pageContr.setPageCount(resultJson.totalPages);

                    $('#notification-message').html(_Messages.getString('LoadingPage') + " " + resultJson.page + " " + _Messages.getString('Of') + " " + resultJson.totalPages);
                    registry.byId('reportGlassPane').setText(_Messages.getString('LoadingPage') + " " + resultJson.page + " " + _Messages.getString('Of') + " " + resultJson.totalPages);

                    var urlStatus = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + resultJson.uuid + '/status';
                    setTimeout(function(){ pentahoGet(urlStatus, "", handleResultCallback); }, this.reportPrompt._pollingInterval);
                  }
                } else if (resultJson.status == "FINISHED") {
                  if(!isIframeContentSet) {
                    var urlContent = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + resultJson.uuid + '/content';
                    logger && logger.log("Will set iframe url to " + urlContent.substr(0, 50) + "... ");

                    $('#hiddenReportContentForm').attr("action", urlContent);
                    $('#hiddenReportContentForm').submit();
                    $('#reportContent').attr("data-src", urlContent);
                    this._updatedIFrameSrc = true;

                    dlg.hide();
                  }
                  if( (this._requestedPage > 0) && (this._currentStoredPagesCount > this._requestedPage)) {
                    // main request finished before requested page was stored in cache
                    var newUrl = url.substring(url.lastIndexOf("/report?") + "/report?".length, url.length);
                    var match = newUrl.match(/(^.*accepted-page=)(\d*?)(&.*$)/);
                    //if not handled by another job
                    if(match[2] != this._requestedPage){
                      newUrl = match[1] + this._requestedPage + match[3];
                      this._requestedPage = 0;
                      pentahoGet('reportjob', newUrl, handleContAvailCallback, 'text/text');
                    }
                  }

                  isFinished = true;
                  if (this._isReportHtmlPagebleOutputFormat) {
                    var pageContr = registry.byId('pageControl');
                    pageContr.setPageCount(resultJson.totalPages);
                    domClass.add('notification-screen', 'hidden');
                    registry.byId('reportGlassPane').hide();
                  }
                } else if (resultJson.status == "FAILED") {
                  this.reportPrompt.showMessageBox(
                    "Request failed",
                    _Messages.getString('FatalErrorTitle'));
                  dlg.hide();
                  isFinished = true;
                  logger && logger.log("ERROR: Request status - FAILED");
                } else  if ( resultJson.status == 'SCHEDULED'){
                  var dlgBackground = registry.byId('scheduleScreen');
                  dlgBackground.setTitle(_Messages.getString('ScheduleTitle'));
                  dlgBackground.setText(_Messages.getString('ScheduleText'));
                  dlgBackground.setSkipBtnText(_Messages.getString('OK'));
                  dlgBackground.setOkBtnText(_Messages.getString('ScheduleSkipAlert'));
                  dlgBackground.callbacks = [

                    function hide() {
                      dlgBackground.hide();
                    }.bind(this),

                    function skipAndHide() {
                      dlgBackground.skip();
                      dlgBackground.hide();
                    }.bind(this)
                  ];
                  if(!dlgBackground.isSkipped()){
                    dlgBackground.show();
                  }
                }
              }
              return resultJson;
          });

          //Navigation on report in progress section


          var reportUrl = url.substring(url.lastIndexOf("/report?")+"/report?".length, url.length);
          if(this._currentReportStatus && this._currentReportStatus!='FINISHED' && this._currentReportStatus!='FAILED'
              && this._currentReportStatus!='CANCELED' && this._currentReportStatus!='SCHEDULED'){
             //In progress
            if(this._currentStoredPagesCount > this._requestedPage){
              //Page available
              var current = reportUrl.match(/(^.*accepted-page=)(\d*?)(&.*$)/);
              var running = this._reportUrl.match(/(^.*accepted-page=)(\d*?)(&.*$)/);
              //parameters besides accepted-page have been changed
              if( current[1] != running[1] ){
                this._reportUrl = reportUrl;
                domClass.add('notification-screen', 'hidden');
                this.cancel(this._currentReportStatus, this._currentReportUuid);
                pentahoGet('reportjob', reportUrl, handleResultCallback, 'text/text');
              } else {
                //just different page, as far as requested page is updated 
                //nothing to do here
                isFinished = true;
              }
            } else {
              //Need to wait for page
              var urlRequestPage = url.substring(0, url.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/' + this._currentReportUuid
                  + '/requestPage/' + this._requestedPage ;
              pentahoGet( urlRequestPage, "");
              isFinished = true;

              var dlg2 = registry.byId('reportGlassPane');
              dlg2.setTitle(_Messages.getString('ScreenTitle'));
              dlg2.setText(_Messages.getString('LoadingPage'));
              dlg2.setButtonText(_Messages.getString('ScreenCancel'));
              dlg2.callbacks = [function reportGlassPaneDone() {
                this._requestedPage = this._previousPage;
                var pageContr = registry.byId('pageControl');
                pageContr.setPageNumber(this._previousPage);
                this._cachedReportCanceled = true;
                dlg2.hide();
              }.bind(this)];
              dlg2.show();
            }
          } else {
            //Not started or finished
            this._reportUrl = reportUrl;
            pentahoGet('reportjob', reportUrl, handleResultCallback, 'text/text');
          }
        } else {
          logger && logger.log("Will set iframe url to " + url.substr(0, 50) + "... ");

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
        this.reportPrompt.hideGlassPane();
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
      }
    }); // end of: var v = {

    // Replace default prompt load
    reportPrompt.load = v.load.bind(v);
    return v;
  };
});
