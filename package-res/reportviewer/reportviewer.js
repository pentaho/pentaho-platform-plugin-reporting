pen.define(['common-ui/util/util','reportviewer/reportviewer-prompt', 'common-ui/util/timeutil', 'common-ui/util/formatting'], function(util) {
  
  return function(reportPrompt) {
    if (!reportPrompt) {
      alert("report prompt is required");
      return;
    }

    var v = logged({
      prompt: reportPrompt,
      
      load: function() {
        dojo.require('pentaho.common.Messages');
        pentaho.common.Messages.addUrlBundle('reportviewer', CONTEXT_PATH+'i18n?plugin=reporting&name=reportviewer/messages/messages');
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
        
        noUserSelect(dojo.byId('reportArea'));
        noUserSelect(dojo.byId('reportPageOutline'));
        noUserSelect(dojo.byId('reportContent'));
        
        // ------------
        
        dojo.connect(dijit.byId('toolbar-parameterToggle'), "onClick", this, function() {
          this.view.togglePromptPanel();
        }.bind(this));

        var boundOnReportContentLoaded = this._onReportContentLoaded.bind(this);
        
        // Schedule the resize after the document has been rendered and CSS applied
        var onFrameLoaded = logged('onFrameLoaded', function() {
          setTimeout(boundOnReportContentLoaded);
        });
        
        if(dojo.isIE){
          // When a file is downloaded, the "complete" readyState does not occur: "loading", "interactive", and stops. 
          dojo.connect(dojo.byId('reportContent'), "onreadystatechange", function() {
            if(this.readyState === 'complete') { onFrameLoaded(); }
          });
        } else {
          dojo.connect(dojo.byId('reportContent'), "load", onFrameLoaded);
        }
        
        var basePromptReady = this.prompt.ready.bind(this.prompt);
        this.prompt.ready       = this.view.promptReady.bind(this.view, basePromptReady);
        this.prompt.submit      = this.submitReport.bind(this);
        this.prompt.submitStart = this.submitReportStart.bind(this);
        
        $('body')
          .addClass(_isTopReportViewer ? 'topViewer leafViewer' : 'leafViewer')
          .addClass(inMobile ? 'mobile' : 'nonMobile');
        
        logger && $('body').addClass('debug');
        
        // The following is *not* confusing at all :-/
        
        // Default implementation of this.prompt.initPromptPanel
        // calls this.prompt.panel.init();
        var decorated = this.prompt.initPromptPanel.bind(this.prompt);

        this.prompt.initPromptPanel = logged('prompt.initPromptPanel', function() {
          // Decorate the original init to first initialize our view then the panel
          var panel = this.prompt.panel;
          var init  = panel.init;

          panel.init = function(noAutoAutoSubmit) {
            this.view.initPrompt(init, this.prompt.panel, noAutoAutoSubmit);
          }.bind(this);

          decorated();
        }.bind(this));

        this.prompt.createPromptPanel();
      },

      view: logged({
        /**
         * Localize the Report Viewer.
         */
        localize: function() {
          $('#toolbar-parameterToggle').attr('title', pentaho.common.Messages.getString('parameterToolbarItem_title'));
          dijit.byId('pageControl').registerLocalizationLookup(pentaho.common.Messages.getString);
        },

        /**
         * Update the page background when we're not in PUC or we're embedded in an
         * iframe to make sure the translucent styling has some contrast.
         */
        updatePageBackground: function() {
          // If we're not in PUC or we're in an iframe
          var inPuc  = window.top.mantle_initialized;
          var inIFrame = top !== self;
          
          // if we are not in PUC
          if(!inSchedulerDialog && !inMobile && (!inPuc || inIFrame)) {
            dojo.addClass(document.body, 'pentaho-page-background');
          }
        },

        _showReportContent: function(visible, preserveSource) {
          // Force not to show a blank iframe
          var hasContent = this._hasReportContent();
          if(!hasContent) { visible = false; }
          
          if(this._isReportContentVisible() !== !!visible) {
            
            // Don't touch "src" of an already blank iframe, or onload occurs needlessly...
            if (!visible && !preserveSource && hasContent) {
              logger && logger.log("Will clear content iframe.src");
              
              $('#reportContent').attr("src", 'about:blank');
              this._updatedIFrameSrc = true;
            }
            
            $('body')[visible ? 'removeClass' : 'addClass']('contentHidden');
          }
        },
        
        _calcReportContentVisibility: function(promptPanel) {
          var visible = 
            // Anything in the iframe to show? (PRD-4271)
            this._hasReportContent() &&

            // Valid data (although report content should be blank here)
            !promptPanel.paramDefn.promptNeeded &&

            // Hide the report area when in the "New Schedule" dialog
            !inSchedulerDialog &&

            (this._isAutoSubmitAllowed(promptPanel) ||
            prompt.mode === 'MANUAL');
          
          return visible;
        },
        
        _isAutoSubmitAllowed : function(promptPanel) {
          if(promptPanel.forceAutoSubmit ||
             promptPanel.paramDefn.allowAutoSubmit()) { // (BISERVER-6915)
            return true;
          }   

          var iframes = document.getElementsByTagName("IFRAME");
          if(iframes.length > 0) {
            var src = iframes[0].src;
        	  return src != null && src.indexOf('dashboard-mode') !== -1;
          }

          return false;
        },

        _hasReportContent: function() {
          var iframe = dojo.byId('reportContent');
          var src = iframe.src;
          return src !== '' && src !== 'about:blank';
        },
        
        _isReportContentVisible: function() {
          return !$('body').hasClass('contentHidden');
        },
        
        // Called on page load and every time the prompt panel is refreshed
        //  PromptingComponent.postChange -> 
        //  PromptPanel.parameterChanged ->
        //             .refreshPrompt -> 
        //             .getParameterDefinition ->
        //             .refresh ->
        //             .init ->
        initPrompt: function(basePanelInit, promptPanel, noAutoAutoSubmit) {
          if (!promptPanel.paramDefn.showParameterUI()) {
            this._hideToolbarPromptControls();
          }
          
          // The following call is important for clearing
          // the report content when autoSubmit=false and
          // the user has changed a value (prompt.mode === 'USERINPUT').
          if(!this._calcReportContentVisibility(promptPanel)) {
            this._showReportContent(false);
          }
          
          // NOTE: `basePanelInit` may call submit, in which case submitReport 
          // is called without _initLayout having been called...
          // (depends on whether there's a submit button or not).
          // Because of that, `submitReport` calls _initLayout also, 
          // to make sure it has ran at least once.
          // Reset layout inited flag.
          // Note also that initLayout cannot be executed before init.
          this._layoutInited = false;
          basePanelInit.call(promptPanel, noAutoAutoSubmit);
          this._initLayout(promptPanel);
        },

        _hideToolbarPromptControls: function() {
          // Hide the toolbar elements
          // When inMobile, toolbarlinner2 has another structure. See report.html.
          if(!inMobile) { dojo.addClass('toolbar-parameter-separator', 'hidden'); }
          
          // dijit modifies the HTML structure.
          // At least in mobile, the button gets wrapped by a frame, 
          // that needs to be hidden.
          var PARAM_TOGGLE = 'toolbar-parameterToggle';
          var elem = dojo.byId(PARAM_TOGGLE);
          while(elem) {
            if(elem.getAttribute('widgetid') === PARAM_TOGGLE) {
              dojo.addClass(elem, 'hidden');
              break;
            }
            elem = elem.parentNode;
          }
        },
                
        // Called by PromptPanel#postExecution (soon after initPrompt)
        promptReady: function(basePromptReady, promptPanel) {

          basePromptReady(promptPanel); // hides the glass pane...

          if (inSchedulerDialog) {
            // If we are rendering parameters for the "New Schedule" dialog,
            // don't show the report or the submit panel, or the pages toolbar
            this.showPromptPanel(true);

            dijit.byId('glassPane').hide();
            dojo.addClass('reportContent', 'hidden');
            dojo.addClass(dojo.query('.submit-panel')[0], 'hidden');
            dojo.addClass('toolbarlinner2', 'hidden');

            dojo.removeClass('promptPanel', 'pentaho-rounded-panel-bottom-lr');
            dojo.removeClass('reportControlPanel', 'pentaho-shadow');
            dojo.removeClass('reportControlPanel', 'pentaho-rounded-panel-bottom-lr');

            if (typeof window.parameterValidityCallback !== 'undefined') {
              var isValid = !promptPanel.paramDefn.promptNeeded;
              window.parameterValidityCallback(isValid);
            }
          }
        },
                
        /**
         * Initializes the report viewer's layout based on the loaded parameter definition.
         *
         * @param promptPanel A prompt panel whose settings should be used to configure the report viewer
         */
        _initLayout: function(promptPanel) {
          if(this._layoutInited) { return; } // reset on every navigation (see #init)
          
          // Is it the first time, or is the parameter UI
          // being refreshed due to user interaction (which causes "navigation")?
          var navigating  = !!this._initedOnce;
          this._initedOnce = true;
          
          var showParamUI = promptPanel.paramDefn.showParameterUI();
          
          this.updatePageControl(promptPanel);
          
          // Hide the toolbar, 'toppanel',  when it would be empty and 
          // un-style the report so it's the only visible element 
          // when both the pagination controls and the parameter UI are hidden.
          var isToolbarEmpty = !promptPanel.paramDefn.paginate && !showParamUI;
          dojo[isToolbarEmpty ? 'addClass' : 'removeClass']('toppanel', 'hidden');
          
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
                         promptPanel.paramDefn.promptNeeded ||
                         !promptPanel.paramDefn.allowAutoSubmit();
          }
          if(showOrHide != null) { this.showPromptPanel(showOrHide); }
          
          this._layoutInited = true;
        },

        show: function() {
          // Cleans up an issue where sometimes on show the iframe is offset
          this.resize();
        },

        updatePageControl: function(promptPanel) {
          var pc = dijit.byId('pageControl');
          
          pc.registerPageNumberChangeCallback(undefined);
          
          if (!promptPanel.paramDefn.paginate) {
            promptPanel.setParameterValue(promptPanel.paramDefn.getParameter('accepted-page'), '-1');
            pc.setPageCount(1);
            pc.setPageNumber(1);
            // pc.disable();
          } else {
            var total = promptPanel.paramDefn.totalPages;
            var page = promptPanel.paramDefn.page;
            // We can't accept pages out of range. This can happen if we are on a page and then change a parameter value
            // resulting in a new report with less pages. When this happens we'll just reduce the accepted page.
            page = Math.max(0, Math.min(page, total - 1));

            // add our default page, so we can keep this between selections of other parameters, otherwise it will not be on the
            // set of params are default back to zero (page 1)
            promptPanel.setParameterValue(promptPanel.paramDefn.getParameter('accepted-page'), '' + page);
            pc.setPageCount(total);
            pc.setPageNumber(page + 1);
          }
          
          pc.registerPageNumberChangeCallback(function(pageNumber) { 
            this.pageChanged(promptPanel, pageNumber); 
          }.bind(this));
        },

        pageChanged: function(promptPanel, pageNumber) {
          promptPanel.setParameterValue(promptPanel.paramDefn.getParameter('accepted-page'), '' + (pageNumber - 1));
          promptPanel.submit(promptPanel, {isPageChange: true});
        },

        togglePromptPanel: function() {
          this.showPromptPanel(dijit.byId('toolbar-parameterToggle').checked);
          this.resize();
        },

        showPromptPanel: function(visible) {
          dijit.byId('toolbar-parameterToggle').set('checked', !!visible);
          
          dojo[visible ? 'removeClass' : 'addClass']('reportControlPanel', 'hidden');
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
          var vp = dojo.dnd.getViewport();
          var tp = dojo.marginBox('toppanel');
          
          var mb = {h: vp.h - tp.h - 2};
          
          logger && logger.log("viewport=(" + vp.w + ","  + vp.h + ") " + " toppanel=(" + tp.w + ","  + tp.h + ") ");
          
          // Fill all available space
          dojo.marginBox('reportArea', mb);
          
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
          var t = $('#reportContent');
          
          // Set the iframe size to minimum before POLLING its contents, to not affect the polled values. 
          // NOTE: Setting to 0 prevented IE9-Quirks from detecting the correct sizes.
          // Setting here, and polling next, causes ONE resize on the iframe.
          t.css({width: POLL_SIZE, height: POLL_SIZE});
          
          // It's surely HTML content, so the following is valid
          var d = t.contents();
          var w = d.width();
          var h = d.height();
          
          logger && logger.log("Styled page - polled dimensions = (" + w + ", " + h + ")");
          
          // In case the styled report content is too small, assume 2/3 of the width.
          // This may happen when there are no results.
          if(w <= POLL_SIZE) {
            // Most certainly this indicates that the loaded report content
            // does not have a fixed width, and, instead, adapts to the imposed size (like width: 100%).
          
            var vp = dojo.dnd.getViewport();
            w = Math.round(2 * vp.w / 3);
            logger && logger.log("Width is too small - assuming a default width of " + w);
          }
           
          t.css({width: w, height: h});
        }
      }), // end view
      
      createRequiredHooks: function(promptPanel) {
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
        window.reportViewer_hide = this.hide.bind(this);

        var localThis = this;

        if (typeof window.top.addGlassPaneListener !== 'undefined') {
          window.top.addGlassPaneListener({
            glassPaneHidden: function(){
              localThis.view.show();
            }
          });
        }
      },

      openUrlInDialog: function(title, url, width, height) {
        if (this.dialog === undefined) {
          dojo.require('pentaho.reportviewer.ReportDialog');
          this.dialog = new pentaho.reportviewer.ReportDialog();
          this.dialog.setLocalizationLookupFunction(pentaho.common.Messages.getString);
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

      submitReportStart: function() {
        // Submit button was mouse-downed!
        
        // In case that a focusout already issued a "fetchParameterDefinition", and a response is to be received,
        // we upgrade that request to behave like if a button had been clicked, by setting prompt.mode to 'MANUAL'.
        // This way, if the user takes longer to release the mouse button,
        // than the focusout response takes to replace the being clicked button by another one (Dashboards.init recreates everything),
        // causing the click event no be generated,
        // the focusout response processing will behave as if it were the response of a click.
        //
        // Because Dashboards.processChange ends up calling postChange in a setTimeout...
        // this call actually gets executed **before** the logic fired by the focusout path,
        // and fetchParameterDefinition has not been called yet.
        this.prompt.clicking = true;
      },
      
      // Called by SubmitPromptComponent#expression (the submit button's click)
      // Also may be called by PromptPanel#init, when there is no submit button (independently of autoSubmit?).
      submitReport: function(promptPanel, keyArgs) {
        var isInit = keyArgs && keyArgs.isInit;
        if(!isInit) {
          if(this.prompt.ignoreNextClickSubmit) {
            delete this.prompt.ignoreNextClickSubmit;
            logger && logger.log("Ignored submit click");
            return;
          }

          this.prompt.mode = 'MANUAL';
        }
        
        try {
          // If we are rendering parameters for the "New Schedule" dialog,
          // don't show the report, the submit panel and pages toolbar.
          if (inSchedulerDialog) {
            this._submitReportEnded(promptPanel);
            return;
          }
          
          // Make sure that layout is initialized
          this.view._initLayout(promptPanel);
          
          // Don't do anything if we need to prompt
          var isValid = !promptPanel.paramDefn.promptNeeded;
          if (!isValid) {
            logger && logger.log("Prompt is needed. Will clear htmlObject.src");
            
            $('#' + this.htmlObject).attr('src', 'about:blank'); // TODO: why htmlObject? Why not this._showReportContent(false)?
            
            this._submitReportEnded(promptPanel);
            return;
          }
          
          this._updateReportContent(promptPanel, keyArgs);
        
        } catch(ex) {
          this._submitReportEnded(promptPanel);
          throw ex;
        }
      },
      
      _updatedIFrameSrc: false,
      _updateReportTimeout: -1,
      
      _updateReportContent: function(promptPanel, keyArgs) {
        var me = this;

        // PRD-3962 - show glass pane on submit, hide when iframe is loaded
        // Show glass-pane
        dijit.byId('glassPane').show();
        
        // When !AutoSubmit, a renderMode=XML call has not been done yet,
        //  and must be done now so that the page controls have enough info.
        if(!promptPanel.getAutoSubmitSetting()) {
          // FETCH page-count info before rendering report
          var callback = logged("_updateReportContent_fetchParameterCallback", function(newParamDefn) {
            // Recreates the prompt panel's CDF components
            promptPanel.refresh(newParamDefn, /*noAutoAutoSubmit*/true);
            
            me._updateReportContentCore(promptPanel, keyArgs);
          });

          me.prompt.fetchParameterDefinition(promptPanel, callback, /*promptMode*/'MANUAL');
        } else {
          me._updateReportContentCore(promptPanel, keyArgs);
        }
      },

      _updateReportContentCore: function(promptPanel, keyArgs) {
        var me = this;
        
        // PRD-3962 - remove glass pane after 5 seconds in case iframe onload/onreadystatechange was not detected
        me._updateReportTimeout = setTimeout(logged('updateReportTimeout', function() {
          me._submitReportEnded(promptPanel, /*isTimeout*/true);
        }), 5000);
        var options = me._buildReportContentOptions(promptPanel);
        var url = me._buildReportContentUrl(options);
        var outputFormat = options['output-target'];
        var isHtml = outputFormat.indexOf('html') != -1;
        var isProportionalWidth = isHtml && options['htmlProportionalWidth'] == "true";
        var isReportAlone = dojo.hasClass('toppanel', 'hidden');
        
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
        
        logger && logger.log("Will set iframe url to " + url.substr(0, 50) + "... ");
        
        $('#reportContent').attr("src", url);
        this._updatedIFrameSrc = true;
        
        // Continue when iframe is loaded (called by #_onReportContentLoaded)
        me._submitLoadCallback = logged('_submitLoadCallback', function() {
          me._isHtmlReport = me.view._isHtmlReport = isHtml;
          me._outputFormat = outputFormat;
          
          var visible = me.view._calcReportContentVisibility(promptPanel);
          if(visible) {
            // A child viewer forces changing to non-styled 
            me.view.setPageStyled(styled && !this._isParentViewer);
            me.view.resize();
          }
          
          me.view._showReportContent(visible);
          
          me._submitReportEnded(promptPanel);
        });
      },
      
      _submitReportEnded: function(promptPanel, isTimeout) {
        // Clear submit-related control flags
        delete this.prompt.clicking;
        if(promptPanel) { delete promptPanel.forceAutoSubmit; }
        delete this.prompt.ignoreNextClickSubmit;
        
        // Awaiting for update report response?
        if(this._updateReportTimeout >= 0) {
          clearTimeout(this._updateReportTimeout);
          this._updateReportTimeout = -1;
          
          if(isTimeout) {
            // This happens, specifically, when the user selects a downloadable output format.
            // #_onReportContentLoaded callback might not have been called.
            this.view._showReportContent(false, /*preserveSource*/true);
          }
          
          // PRD-3962 - show glass pane on submit, hide when iframe is loaded
          // Hide glass-pane, if it is visible
          dijit.byId('glassPane').hide();
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
          var contentWin = dojo.byId('reportContent').contentWindow;
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
      
      _buildReportContentOptions: function(promptPanel) {
        var options = util.getUrlParameters();

        $.extend(options, promptPanel.getParameterValues());

        options['renderMode'] = 'REPORT';

        // SimpleReportingComponent expects name to be set
        if (options['name'] === undefined) {
          options['name'] = options['action'];
        }

        // Never send the session back. This is generated by the server.
        delete options['::session'];
        
        return options;
      },
      
      _buildReportContentUrl: function(options) {
        var url = window.location.href;
        url = url.substring(0, url.lastIndexOf("/")) + "/report?";

        var params = [];
        var addParam = function(encodedKey, value) {
          if(value.length > 0) {
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
