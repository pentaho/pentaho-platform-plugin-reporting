pen.define(['common-ui/util/util','reportviewer/reportviewer-prompt', 'reportviewer/reportviewer-timeutil', 'reportviewer/reportviewer-formatting'], function(util) {
  return function(reportPrompt) {
    if (!reportPrompt) {
    	alert("report prompt is required");
    	return;
    }
    var v = {
      prompt: reportPrompt,

      load: function() {
        dojo.require('pentaho.common.Messages');
        pentaho.common.Messages.addUrlBundle('reportviewer', CONTEXT_PATH+'i18n?plugin=Pentaho Reporting Plugin&name=reportviewer/messages/messages');
        this.view.localize();

        this.createRequiredHooks();

        this.view.updatePageBackground();

        dojo.connect(dijit.byId('toolbar-parameterToggle'), "onClick", this, function() {
          this.view.togglePromptPanel();
        }.bind(this));

        this.view.resize();
        var viewResizeIframe = this.view.resizeIframe.bind(this.view);
        $('#reportContent').load(function() {
      	var iframe = this;
          // Schedule the resize after the document has been rendered and CSS applied
          setTimeout(function() {
            viewResizeIframe(iframe);
          });
        });

        this.prompt.schedule = this.scheduleReport.bind(this);
        this.prompt.submit = this.submitReport.bind(this);

        var decorated = this.prompt.initPromptPanel.bind(this.prompt);

        this.prompt.initPromptPanel = function() {
          // Decorate the original init to first initialize our view then the panel
          var init = this.prompt.panel.init;
            this.prompt.panel.init = function() {
            this.view.init(init, this.prompt.panel);
          }.bind(this);
          decorated();
        }.bind(this);

        this.prompt.createPromptPanel();
      },

      view: {
        // The last known report width so we can set an empty page to a decent width so it doesn't change drastically between refreshes.
        lastWidth: 700,

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
          if(!top.mantle_initialized || top !== self) {
            dojo.addClass(document.body, 'pentaho-page-background');
          }
        },

        /**
         * Sets the report content visible if it should be:
         *  - Prompts are valid
         *  - The prompt panel allows auto-submit or the prompts were changed by the user
         *
         * We must always show the report content if we're not in 'REPORT' render mode because this area is used to provide
         * feedback to the user.
         *
         * @param promptPanel The current prompt panel
         * @param renderMode Current render mode: 'REPORT' or 'SUBSCRIBE'
         */
        updateReportContentVisibility: function (promptPanel, renderMode) {
          this.showReportContent(renderMode === 'SUBSCRIBE' || (!promptPanel.paramDefn.promptNeeded && (promptPanel.paramDefn.allowAutoSubmit() || prompt.mode === 'MANUAL')));
        },

        init: function(init, promptPanel) {
          if (!promptPanel.paramDefn.showParameterUI()) {
            // Hide the toolbar elements
            dojo.addClass('toolbar-parameter-separator', 'hidden');
            dojo.addClass('toolbar-parameterToggle', 'hidden');
          }

          init.call(promptPanel);
          this.configureLayout(promptPanel);
        },

        /**
         * Configure the report viewer's layout based on the loaded parameter definition.
         *
         * @param promptPanel A prompt panel whose settings should be used to configure the report viewer
         */
        configureLayout: function(promptPanel) {
          this.showPromptPanel(promptPanel.paramDefn.showParameterUI());
          this.updateReportContentVisibility(promptPanel, 'REPORT');
          this.refreshPageControl(promptPanel);

          if (!promptPanel.paramDefn.paginate && !promptPanel.paramDefn.showParameterUI()) {
            // Hide the toolbar when it would be empty and unstyle the report so it's the only visible element when both the
            // pagination controls and the parameter UI are hidden
            dojo.addClass('toppanel', 'hidden');
            this.updatePageStyling(false);
          } else {
            // Make sure the toolbar is visible
            dojo.removeClass('toppanel', 'hidden');
          }

          this.resize();
        },

        showReportContent: function(visible) {
          var toggle = visible ? dojo.removeClass : dojo.addClass;
          var selector = this.isPageStyled() ? 'reportArea' : 'reportContent';
          toggle(selector, 'hidden');
          if (!visible) {
            $('#reportContent').attr("src", 'about:blank');
          }
        },

        refreshPageControl: function(promptPanel) {
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
          promptPanel.submit(promptPanel);
        },

        togglePromptPanel: function() {
          this.showPromptPanel(dijit.byId('toolbar-parameterToggle').checked);
          this.resize();
        },

        showPromptPanel: function(visible) {
          if (visible) {
            dijit.byId('toolbar-parameterToggle').set('checked', true);
            dojo.removeClass('reportControlPanel', 'hidden');
          } else {
            dijit.byId('toolbar-parameterToggle').set('checked', false);
            dojo.addClass('reportControlPanel', 'hidden');
          }
        },

        isPageStyled: function() {
          return $('#reportArea').length === 1;
        },

		isPentahoMobileEnv: function() {
		  return (typeof window.top.PentahoMobile !== 'undefined');
		},

        updatePageStyling: function(styled) {
          var currentlyStyled = this.isPageStyled();
          if (styled) {          
            if (this.isPentahoMobileEnv()) {
              $('body').addClass('styled');
              var iframe = $('#reportContent');
              iframe.wrap('<div id="reportContentWrapper" class="webkitScroller"/>');            
              $('reportContentWrapper').css('width', window.innerWidth);
            } else {
              // Style the report iframe if it's not already styled
              if (!currentlyStyled) {
                var iframe = $('#reportContent');
                $('body').addClass('styled');
                iframe.wrap('<div id="reportArea" class="pentaho-transparent" scrollexception="true"/>');
                iframe.wrap('<div id="reportPageOutline" class="pentaho-rounded-panel2-shadowed pentaho-padding-lg pentaho-background"/>');
              }
            }
          } else {
            if (currentlyStyled) {
              $('body').removeClass('styled');
              var iframe = $('#reportContent');
              if (this.isPentahoMobileEnv()) {
                iframe.css('width', window.innerWidth);
                $('reportContentWrapper').css('width', window.innerWidth);
                iframe.unwrap();
              } else {
                iframe.css('width', window.innerWidth);
                iframe.unwrap().unwrap();
              }
            }
          }
          this.resize();
        },

        resize: function() {
          if (this.isPentahoMobileEnv()) {
            var rcw = dojo.byId('reportContentWrapper');
            if (rcw != null) {
              var c = dojo.coords(rcw);
              var windowHeight = dojo.dnd.getViewport().h;
              var height = windowHeight - c.y - 2;
              dojo.marginBox(rcw, {h: height});
            }
          } else {
            var ra = dojo.byId(this.isPageStyled() ? 'reportArea' : 'reportContent');
            var c = dojo.coords(ra);
            var windowHeight = dojo.dnd.getViewport().h;
            dojo.marginBox(ra, {h: windowHeight - c.y});
          }
        },

		frameUpdateCount: 0,
        updateFrameWebkitScrollCss : function(iframe) {
          var _this = this;
          _this.frameUpdateCount++;
          if (_this.frameUpdateCount < 2) {
            return;
          }
		  setTimeout(function() {
            try {
		      var element = document.getElementById('reportContent').contentDocument.body;
		      _this.updateElementWebkitScrollCss(element);
            } catch (e) {alert(e);}			
		  },1);
        },
	  
	    updateElementWebkitScrollCss : function(element) {
          if (typeof element.getAttribute !== 'function') {
            return;
          } 
          var style = element.getAttribute('style');
          if (typeof style == 'undefined' || style == null) {
            style = '-webkit-transform:translate3d(0,0,0);';
          } else {
            style+= ';-webkit-transform:translate3d(0,0,0);';
          }
          style+='margin:20px;padding:0px';
          element.setAttribute('style', style);		
          if (element.children != null && element.children.length > 0) {
		    for (var i=0;i<element.children.length;i++) {
			  var child = element.children[i];
			  //updateElementCss(child);
			}
		  }
	    },

        resizeIframe: function(iframe) {
          var t = $(iframe);
          
          if (this.isPentahoMobileEnv()) {
            this.updateFrameWebkitScrollCss(iframe);
          }
          
          if (!this.isPageStyled()) {
            return;
          }

          if (t.attr('src') === 'about:blank') {
            // use the last known report width (or the default) so we don't drastically change the width between refreshes
            t.width(this.lastWidth); // matches report.css: .styled >* #reportContent
            t.height(200);

            $('#reportPageOutline').width(t.outerWidth() + 14);
            this.resize();
          } else {
          // Reset the iframe height before polling its contents so the size is correct.
          t.width(0);
          t.height(0);

          var d = $(iframe.contentWindow.document);
          t.height(d.height());

            this.lastWidth = d.width();
            t.width(this.lastWidth);

            if (this.isPentahoMobileEnv()) {
              $('#reportContent').width(window.innerWidth);
            } else {
              $('#reportPageOutline').width(t.outerWidth());
            }
          this.resize();
        }
        }
      },

      createRequiredHooks: function(promptPanel) {
    	
    	// [PIR-543] - Allow new/refreshed reports to re-attach or override instance functions in the top window object
    	// Top window functions may become orphaned due to content linking refresh or when a report tab in PUC is closed
    	/*
        try{
          if (window.reportViewer_openUrlInDialog || top.reportViewer_openUrlInDialog) {
            return;
          }
        }
        catch(err){
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
          }
        } else {
          top.reportViewer_openUrlInDialog = this.openUrlInDialog.bind(this);
        }
        window.reportViewer_openUrlInDialog = top.reportViewer_openUrlInDialog;
        window.reportViewer_hide = this.hide.bind(this);
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

      _updateReport: function(promptPanel, renderMode) {
        if (promptPanel.paramDefn.promptNeeded) {
          $('#' + this.htmlObject).attr('src', 'about:blank');
          return; // Don't do anything if we need to prompt
        }
        var options = util.getUrlParameters();
        $.extend(options, promptPanel.getParameterValues());
        options['renderMode'] = renderMode;

        // SimpleReportingComponent expects name to be set
        if (options['name'] === undefined) {
          options['name'] = options['action'];
        }

        // Never send the session back. This is generated by the server.
        delete options['::session'];

        var url = CONTEXT_PATH + "content/reporting?";
        var params = [];
        var addParam = function(encodedKey, value) {
          if(value.length > 0) {
            params.push(encodedKey + '=' + encodeURIComponent(value));
          }
        }
        $.each(options, function(key, value) {
          if (value === null || typeof value == 'undefined') {
            return; // continue
          }
          var encodedKey = encodeURIComponent(key);
          if ($.isArray(value)) {
            var val = [];
            $.each(value, function(i, v) {
              addParam(encodedKey, v);
            });
          } else {
            addParam(encodedKey, value);
          }
        });

        url += params.join("&");

        // Update page styling based on HTML output or not and if we're not scheduling
        var proportionalWidth = options['htmlProportionalWidth'] == "true";
        var isHtml = options['output-target'].indexOf('html') != -1;
        var isSubscribe = renderMode === 'SUBSCRIBE';
        this.view.updateReportContentVisibility(promptPanel, renderMode)
        this.view.updatePageStyling(!isSubscribe && isHtml && !proportionalWidth);


        var iframe = $('#reportContent');
        iframe.attr("src", url);
      },

      submitReport: function(promptPanel) {
        if (!promptPanel.getAutoSubmitSetting()) {
          // FETCH page info before rendering report
          prompt.fetchParameterDefinition(promptPanel, function(newParamDefn) {
            promptPanel.refresh(newParamDefn);
            this._updateReport(promptPanel, 'REPORT');
          }.bind(this), 'MANUAL');
          return;
        }
        this._updateReport(promptPanel, 'REPORT');
      },

      scheduleReport: function(promptPanel) {
        this._updateReport(promptPanel, 'SUBSCRIBE');
      }
    }

    reportPrompt.load = v.load.bind(v);
    return v;
  };
});