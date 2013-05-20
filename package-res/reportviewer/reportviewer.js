pen.define(['common-ui/util/util','reportviewer/reportviewer-prompt', 'common-ui/util/timeutil', 'common-ui/util/formatting'], function(util) {
  
  return function(reportPrompt) {
    if (!reportPrompt) {
      alert("report prompt is required");
      return;
    }

    var v = {
      prompt: reportPrompt,

      load: function() {
        dojo.require('pentaho.common.Messages');
        pentaho.common.Messages.addUrlBundle('reportviewer', CONTEXT_PATH+'i18n?plugin=reporting&name=reportviewer/messages/messages');
        this.view.localize();

        this.createRequiredHooks();

        this.view.updatePageBackground();

        dojo.connect(dijit.byId('toolbar-parameterToggle'), "onClick", this, function() {
          this.view.togglePromptPanel();
        }.bind(this));

        this.view.resize();
        
        var boundOnReportContentLoaded = this._onReportContentLoaded.bind(this);
        
        // Schedule the resize after the document has been rendered and CSS applied
        var onFrameLoaded = function() { setTimeout(boundOnReportContentLoaded); };
        
        if(dojo.isIE){
          // When a file is downloaded, the "complete" readyState does not occur: "loading", "interactive", and stops. 
          dojo.connect(dojo.byId('reportContent'), "onreadystatechange", function() {
            if(this.readyState === 'complete') { onFrameLoaded(); }
          });
        } else {
          dojo.connect(dojo.byId('reportContent'), "load", onFrameLoaded);
        }
        
        this.prompt.ready       = this.promptReady.bind(this);
        this.prompt.submit      = this.submitReport.bind(this);
        this.prompt.submitStart = this.submitReportStart.bind(this);

        
        // The following is *not* consusing :-/
        
        // Default implementation of this.prompt.initPromptPanel
        // calls this.prompt.panel.init();
        var decorated = this.prompt.initPromptPanel.bind(this.prompt);

        this.prompt.initPromptPanel = function() {
          // Decorate the original init to first initialize our view then the panel
          var panel = this.prompt.panel;
          var init  = panel.init;

          panel.init = function() {
            this.view.init(init, this.prompt.panel);
          }.bind(this);

          decorated();
        }.bind(this);

        this.prompt.createPromptPanel();
      },

      view: {
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
          /**
           * If we're not in PUC or we're in an iframe
           */
          var mobile = this.isPentahoMobileEnv();
          var inPuc  = window.top.mantle_initialized;
          var inIFrame = top !== self;
          
          // if we are not in PUC
          if(!inSchedulerDialog && !mobile && (!inPuc || inIFrame)) {
            dojo.addClass(document.body, 'pentaho-page-background');
          }
        },

        updateReportContentVisibility: function(promptPanel) {
          this._showReportContent(this._calcReportContentVisibility(promptPanel));
        },
        
        _showReportContent: function(visible, preserveSource) {
          var area = $('#reportArea');
          
          var content = $('#reportContent');
          if (!visible) {
            if(!preserveSource && this._hasReportContent()) { // Don't touch "src" of a already blank iframe, or onloads occur... 
              content.attr("src", 'about:blank');
            }
            
            // PRD-4271 Don't show white box rectangle...
            // PRD-4018, PRD-4034 FF & IE8 does not resize properly when iframe is hidden
            // So we hide it placing the iframe off-page
            // Also, cannot hide (vibility/display) a parent of the iframe where a pdf is to be shown
            // otherwise loading fails in IE.
            area.css({
                 position: 'absolute',
                 left:   -100000,
                 top:    -100000
              });
          } else {
            // Remove the "visibility:hidden" placed in the HTML
            // to avoid flicker in first time.
            area.css({position: 'static'}).css({visibility: 'inherit'});
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

            (promptPanel.forceAutoSubmit || 
             promptPanel.paramDefn.allowAutoSubmit() || // (BISERVER-6915)
             prompt.mode === 'MANUAL');
          
          return visible;
        },

        _hasReportContent: function() {
          var iframe = dojo.byId('reportContent');
          var src = iframe.src;
          return src !== '' && src !== 'about:blank';
        },

        init: function(init, promptPanel) {
          if (!promptPanel.paramDefn.showParameterUI()) {
            // Hide the toolbar elements
            dojo.addClass('toolbar-parameter-separator', 'hidden');
            dojo.addClass('toolbar-parameterToggle', 'hidden');
          }

          this._layoutInited = false;
          
          // May call submit, in which case submitReport is called without _initLayout...
          init.call(promptPanel);
          this._initLayout(promptPanel);
        },

        /**
         * Initializes the report viewer's layout based on the loaded parameter definition.
         *
         * @param promptPanel A prompt panel whose settings should be used to configure the report viewer
         */
        _initLayout: function(promptPanel) {
          if(this._layoutInited) { return; }
          
          // Don't mess with the parameters if we're "navigating".
          // If the user has explicitly hidden the UI, 
          // and is going through several pages, 
          // we should not keep popping the UI again on each page init...
          // PRD-4001, PRD-4102
          var navigating  = !!this._initedOnce; //this._hasReportContent();
          this._initedOnce = true;
          
          var showParamUI = promptPanel.paramDefn.showParameterUI();
          if(showParamUI && !navigating && this.isPentahoMobileEnv() && 
             !promptPanel.paramDefn.promptNeeded && promptPanel.paramDefn.allowAutoSubmit()) {
            // Don't show parameter panel by default unless prompt needed?
            showParamUI = false;
          }
          
          if(!navigating || !showParamUI) {
            this.showPromptPanel(showParamUI);
          }
          
          this.updateReportContentVisibility(promptPanel);
          this.updatePageControl(promptPanel);

          // Hide the toolbar, 'toppanel',  when it would be empty and 
          // un-style the report so it's the only visible element 
          // when both the pagination controls and the parameter UI are hidden.
          var isToolbarEmpty = !promptPanel.paramDefn.paginate && !showParamUI;
          dojo[isToolbarEmpty ? 'addClass' : 'removeClass']('toppanel', 'hidden');
          
          this._layoutInited = true;
        },

        show: function() {
          // Cleans up an issue where sometimes on show the iframe is offset
          this.resizeIFrame();
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

        isPentahoMobileEnv: function() {
          return (typeof window.top.PentahoMobile !== 'undefined');
        },

        updatePageStyling: function(styled) {
          // normalize to boolean
          styled = !!styled;
          
          // Need to style at least the first time anyway, 
          // to ensure the HTML and JS are in sync.
          if(!this._pageStyledOnce || this.isPageStyled() !== styled) {
            this._pageStyledOnce = true;
            
            var setClass = styled ? 'addClass' : 'removeClass';
            
            $('body')[setClass]('styled');
            
            if(!styled) {
              $('#reportArea').css({width: 'auto', height: 'auto'});
              $('#reportPageOutline').css({width: 'auto', height: 'auto'});
              $('#reportContent').css({width: window.innerWidth});
            }
            
            $('#reportPageOutline')[setClass]('pentaho-rounded-panel2-shadowed pentaho-padding-lg pentaho-background');
            
            this.resize();
          }
        },

        resize: function() {
          // NOTE: reportContent/reportArea are not always correctly positioned: 
          // they are placed off-page, when "hidden", 
          // to avoid flicker and still allow layout and measure poling.
          //
          // That's why the dummy "reportAreaTop" is used to find the desired target's height (mb.h).
          // TODO: possibly, using the box-model & visibility we could determine the y position
          // from the "toppanel" element; couldn't get it to work; 
          // probably wasn't taking all the needed margins/paddings into account.
          // 
          // Target extends to all available width.
          var c  = dojo.coords(dojo.byId('reportAreaTop'));
          var vp = dojo.dnd.getViewport();
          var mb = {h: vp.h - c.y, w: vp.w};
          
          var target;
          if(this.isPageStyled()) { // Mobile is *never* styled
            target = 'reportArea';
          } else {
            // When page is !styled, "reportArea" has height auto.
            // The iframe is sized with the viewport height, 
            // causing the scroll-bar to be provided by iframe's content itself.
            target = 'reportContent';
                        
            // NOTE: care had to be taken to remove the reportArea's paddings/margins in CSS/stylesheet
            // otherwise, the height of reportContent would need to take these into account.
            // Also, as the height would exceed the window height, then had problems
            // in iPad/iOS6/Safari due to show scroll/hide scroll instability, causing repeated
            // resize events to occur.
            
            // NOTE2: There's still a problem (at least) in iPad/iOS6/Safari, and (at least) with HTML, 
            // where showing the prompts (when initially hidden), and even though the "resize" gets called,
            // the height is not correctly changed, and 
            // a part of the report gets cut-off at the bottom.
          }
          
          dojo.marginBox(target, mb);
          
          if(this.isPentahoMobileEnv()) {
            $('#reportControlPanel').css('width', vp.w);
            
            // HACK into the report content's HTML so that it handles scrolling directly
            if(this._isHtmlReport) {
              var iframe   = dojo.byId('reportContent');
              var frameDoc = iframe.contentWindow.document;
              var reportTable = frameDoc.body.childNodes[1];
              if (reportTable) {
                iframe.setAttribute('scrolling', 'no');
                
                // TODO: HACK: Avoid a slightly clipped footer (heuristic value)
                var wh = mb.h - 15;
                
                // Find wrapper element
                var reportTableDiv = frameDoc.getElementById('reportTableDiv');
                if (reportTableDiv != null) {
                  dojo.style(reportTableDiv, 'height', wh + 'px');
                  dojo.style(reportTableDiv, 'overflow', 'auto');
                } else {
                  $(reportTable).wrap('<div id="reportTableDiv" style="height:' + wh + 'px; overflow:auto;"/>');
                }
              }
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
         * Finally, the {@link #resize} method is called,
         * that adjusts the report area div, 
         * responsible by showing up a scrollbar, when necessary.  
         */ 
        resizeIFrame: function() {
          if (this._hasReportContent()) {
            // PRD-4000 Hide iframe before resize
            this._showReportContent(false, /*preserveSource*/true);
            
            if (this.isPageStyled()) {
              var t = $('#reportContent');
              
              // Reset the iframe size before polling its contents so the size is correct.
              // NOTE: Setting to 0 prevented IE9-Quirks from detecting the correct sizes.
              t.width(10).height(10);
              
              // It's HTML content, so the following is valid
              var d = $(t[0].contentWindow.document);
              var w = d.width();
              var h = d.height();
              
              // TODO: HACK: IE8 requires some additional space to not show scrollbars inside
              t.width(w+20).height(h+20);
              $('#reportPageOutline').width(t.outerWidth());
              
              //else if(!isPageStyled()) -> IFrame Height and Width are controlled in #resize, below
            }
            
            this.resize();
            
            // PRD-4000 Show iframe after resize
            this._showReportContent(true);
          }
        }
      }, // end view

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
       */
      hide: function() {
        $('#toppanel').empty();
        this.view.resize();
      },

      promptReady: function(promptPanel) {
        if (inSchedulerDialog) {
          // If we are rendering parameters for the "New Schedule" dialog,
          // don't show the report or the submit panel, or the pages toolbar
          this.view.showPromptPanel(true);

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

      submitReport: function(promptPanel, keyArgs) {
        var isInit = keyArgs && keyArgs.isInit;
        if(!isInit) {
          if(this.prompt.ignoreNextClickSubmit) {
            delete this.prompt.ignoreNextClickSubmit;
            return;
          }

          this.prompt.mode = 'MANUAL';
        }
        
        // Make sure that layout is initialized
        this.view._initLayout(promptPanel);
        
        try {
          // If we are rendering parameters for the "New Schedule" dialog,
          // don't show the report, the submit panel and pages toolbar.
          if (inSchedulerDialog) {
            this._submitReportEnded(promptPanel);
            return;
          }
            
          // Don't do anything if we need to prompt
          var isValid = !promptPanel.paramDefn.promptNeeded;
          if (!isValid) {
            $('#' + this.htmlObject).attr('src', 'about:blank'); // TODO: why htmlObject? Why not this._showReportContent(false)?
            this._submitReportEnded(promptPanel);
            return;
          }
        
          this._updateReportContent(promptPanel, keyArgs);
        
        } catch(ex) {
          this._submitReportEnded(promptPanel);
        }
      },
      
      _updateReportTimeout: -1,
      
      _updateReportContent: function(promptPanel, keyArgs) {
        var me = this;
        
        // PRD-3962 - show glass pane on submit, hide when iframe is loaded
        // Show glass-pane
        dijit.byId('glassPane').show();
        
        // PRD-3962 - remove glass pane after 5 seconds in case iframe onload/onreadystatechange was not detected
        me._updateReportTimeout = setTimeout(function() {
          me._submitReportEnded(promptPanel, /*isTimeout*/true);
        }, 5000);
        
        var options = me._buildReportContentOptions(promptPanel);
        var url = me._buildReportContentUrl(options);
        
        // If the iframe isn't hidden here,
        // when it loads,
        // the user may see a bit of the new document
        // but in a different page format..
        // So, it's just better to hide when "styled" will change.
        var isHtml = options['output-target'].indexOf('html') != -1;
        var isProportionalWidth = isHtml && options['htmlProportionalWidth'] == "true";
        var isReportAlone = dojo.hasClass('toppanel', 'hidden');
        var styled = !isReportAlone && isHtml && !isProportionalWidth && !this.view.isPentahoMobileEnv();
        
        if(me.view.isPageStyled() !== styled) {
          me.view._showReportContent(false, /*preserveSource*/true);
        }
        
        $('#reportContent').attr("src", url);
        
        // Continue when iframe is loaded (called by #_onReportContentLoaded)
        me._submitLoadCallback = function() {
          me._isHtmlReport = me.view._isHtmlReport = isHtml;
          
          var visible = me.view._calcReportContentVisibility(promptPanel);
          if(visible) {
            me.view.updatePageStyling(styled);
            me.view.resizeIFrame();
          }
          
          me.view._showReportContent(visible);
          
          me._submitReportEnded(promptPanel);
        };
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
            // #_onReportContentLoaded callback might not been called.
            this.view._showReportContent(false, /*preserveSource*/true);
          }
          
          // PRD-3962 - show glass pane on submit, hide when iframe is loaded
          // Hide glass-pane, if it is visible
          dijit.byId('glassPane').hide();
        }
      },
      
      _onReportContentLoaded: function() {
        var callback = this._submitLoadCallback;
        if(callback && this.view._hasReportContent()) {
          delete this._submitLoadCallback;
          callback.call(this);
        } else {
          this.view.resizeIFrame();
        }
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
    }

    reportPrompt.load = v.load.bind(v);
    return v;
  };
});
