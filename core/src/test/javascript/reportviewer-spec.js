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
 * Copyright (c) 2017 Hitachi Vantara..  All rights reserved.
 */

define(["reportviewer/reportviewer", "reportviewer/reportviewer-logging", "reportviewer/reportviewer-prompt", "common-ui/jquery-clean",
    "text!./parameterDefinition.xml!strip", "dojo/dom-class", 'common-ui/util/util', "./mocks/registryMock"
  ],
  function(ReportViewer, Logging, Prompt, $, parameterDefinition, domClass, util, registryMock) {

    describe("Report Viewer", function() {
      var reportViewer;

      beforeAll(function() {
        var mockGlassPane = jasmine.createSpyObj("glassPane", ["show", "hide"]);
        mockGlassPane.id = "glassPane";
        registryMock.mock(mockGlassPane);
        var messageBox = jasmine.createSpyObj("messageBox", ["setTitle", "setMessage", "setButtons", "show"]);
        messageBox.id = "messageBox";
        registryMock.mock(messageBox);
      });

      afterAll(function() {
        registryMock.unMock("glassPane");
        registryMock.unMock("messageBox");
      });

      beforeEach(function() {
        window._isTopReportViewer = true;
        window.inSchedulerDialog = false;
        window.inMobile = true;
        var options = {
          parent: window.parent.logger
        };
        window.logged = Logging.create(window.name, options);

        var reportPrompt = new Prompt();

        spyOn($, 'ajax').and.callFake(function(params) {
          params.success(parameterDefinition);
        });

        spyOn(domClass, 'add');
        spyOn(domClass, 'remove');
        spyOn(domClass, 'contains');

        logger = jasmine.createSpy("logger");
        logger.log = jasmine.createSpy("log");

        reportPrompt.createPromptPanel();
        reportViewer = new ReportViewer(reportPrompt);
      });

      describe("state properties", function() {
        beforeEach(function() {
          window.inSchedulerDialog = true;
          spyOn(reportViewer.reportPrompt, '_getStateProperty').and.callThrough();
        });

        it("should get the promtpNeeded value", function() {
          expect(reportViewer.reportPrompt._getStateProperty('promptNeeded')).toBeFalsy();

          // Force new value for promptNeeded on the parameter definition
          reportViewer.reportPrompt.api.operation._getPromptPanel().getParamDefn().promptNeeded = true;
          expect(reportViewer.reportPrompt._getStateProperty('promptNeeded')).toBeTruthy();
        });

        it("should get promtpNeeded on updateLayout", function() {
          spyOn(reportViewer.view, '_initLayout');
          spyOn(reportViewer.view, '_showReportContent');
          spyOn(reportViewer.view, '_hasReportContent').and.returnValue(true);

          reportViewer.view.updateLayout();
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
        });

        it("should get promtpNeeded on afterRender", function() {
          window.parameterValidityCallback = function() {};

          reportViewer.view.afterRender(function() {});
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
        });

        it("should get promtpNeeded on initLayout", function() {
          spyOn(reportViewer.view, 'updatePageControl');
          spyOn(reportViewer.view, 'showPromptPanel');

          reportViewer.view._initLayout();
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
        });

        it("should get promtpNeeded on submitReport", function() {
          window.inSchedulerDialog = false;
          spyOn(reportViewer.view, '_initLayout');
          spyOn(reportViewer, "_updateReportContentCore");

          reportViewer.submitReport();
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
        });

        it("should get autoSubmit on _updateReportContent", function() {
          spyOn(reportViewer, '_updateReportContentCore');

          reportViewer._updateReportContent();
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('autoSubmit');
        });

        it("should get showParameterUI on initLayout", function() {
          spyOn(reportViewer.view, 'updatePageControl');
          spyOn(reportViewer.view, 'showPromptPanel');

          reportViewer.view._initLayout();
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('showParameterUI');
        });

        it("should get showParameterUI on updateLayout", function() {
          spyOn(reportViewer.view, '_initLayout');
          spyOn(reportViewer.view, '_showReportContent');
          spyOn(reportViewer.view, '_hasReportContent').and.returnValue(true);

          reportViewer.view.updateLayout();
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('showParameterUI');
        });

        it("should hide prompt when showParameterUI is false", function() {
          spyOn(reportViewer.view, '_initLayout');
          spyOn(reportViewer.view, '_showReportContent');
          spyOn(reportViewer.view, '_hasReportContent').and.returnValue(true);
          spyOn(reportViewer.view, '_hideToolbarPromptControls');
          reportViewer.reportPrompt._getStateProperty.and.callFake(function(param) {
            return (param === "showParameterUI") ? false : true;
          });

          reportViewer.view.updateLayout();
          expect(reportViewer.view._hideToolbarPromptControls).toHaveBeenCalled();
        });

        it("should call allowAutoSubmit on initLayout", function() {
          spyOn(reportViewer.view, 'updatePageControl');
          spyOn(reportViewer.view, 'showPromptPanel');

          reportViewer.view._initLayout();
          expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('allowAutoSubmit');
        });

        it("should return the correct value for allowAutoSubmit on _isAutoSubmitAllowed", function() {
          expect(reportViewer.view._isAutoSubmitAllowed()).toBeTruthy();
          reportViewer.reportPrompt._getStateProperty.and.callFake(function(param) {
            return (param && param === "allowAutoSubmit") ? false : undefined;
          });
          expect(reportViewer.view._isAutoSubmitAllowed()).toBeFalsy();
        });
      });

      describe("page controllers", function() {

        describe("set using accepted-page", function() {
          beforeEach(function() {
            spyOn(reportViewer.reportPrompt.api.operation, "setParameterValue").and.callThrough();
          });

          it("was called correctly", function() {
            reportViewer.view._setAcceptedPage("2");
            expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "2");
          });

          it("was called correctly on pageChanged", function() {
            spyOn(reportViewer.reportPrompt.api.operation, "submit");
            spyOn(reportViewer.view, "pageChanged").and.callThrough();
            reportViewer.view.pageChanged("20");
            expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "19");
            expect(reportViewer.reportPrompt.api.operation.submit).toHaveBeenCalled();
          });

          describe("was called correctly on updatePageControl with paginate as", function() {
            beforeEach(function() {
              var pageControlMock = jasmine.createSpyObj("pageControl", ["registerPageNumberChangeCallback", "setPageCount", "setPageNumber"]);
              pageControlMock.id = "pageControl";
              registryMock.mock(pageControlMock);
              spyOn(reportViewer.view, "_setAcceptedPage").and.callThrough();
              spyOn(reportViewer.reportPrompt, '_getStateProperty').and.callThrough();
            });

            afterEach(function() {
              registryMock.unMock("pageControl");
            });

            it("false", function() {
              reportViewer.reportPrompt.api.operation._getPromptPanel().getParamDefn().paginate = false;
              reportViewer.view.updatePageControl();
              expect(reportViewer.view._setAcceptedPage).toHaveBeenCalledWith("-1");
              expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('paginate');
              expect(reportViewer.reportPrompt._getStateProperty).not.toHaveBeenCalledWith('page');
              expect(reportViewer.reportPrompt._getStateProperty).not.toHaveBeenCalledWith('totalPages');
              expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "-1");
            });

            it("true", function() {
              reportViewer.reportPrompt.api.operation._getPromptPanel().getParamDefn().paginate = true;
              reportViewer.reportPrompt.api.operation._getPromptPanel().getParamDefn().page = 5;
              reportViewer.reportPrompt.api.operation._getPromptPanel().getParamDefn().totalPages = 8;
              reportViewer.view.updatePageControl();
              expect(reportViewer.view._setAcceptedPage).toHaveBeenCalledWith("5");
              expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('paginate');
              expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('page');
              expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('totalPages');
              expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "5");
            });
          });
        });

        describe("on _initLayout handle paginate", function() {
          beforeEach(function() {
            spyOn(reportViewer.view, "updatePageControl");
            spyOn(reportViewer.view, "showPromptPanel");
            spyOn(reportViewer.reportPrompt, '_getStateProperty').and.callThrough();
            spyOn(reportViewer.reportPrompt.api.operation, "state").and.callThrough();
          });

          it("correctly", function() {
            reportViewer._layoutInited = false;
            reportViewer.view._initLayout();
            expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('paginate');
          })
        });
      });

      describe("_buildReportContentOptions", function() {
        it("should call the _buildReportContentOptions on the report viewer prompt", function() {
          var options = {
            "option1": "option1",
            'action': 'testAction'
          };
          spyOn(reportViewer.reportPrompt, "_buildReportContentOptions").and.returnValue(options);

          var result = reportViewer._buildReportContentOptions();

          expect(reportViewer.reportPrompt._buildReportContentOptions).toHaveBeenCalledWith('REPORT');
          expect(result).toBe(options);
          expect(result['name']).toBe(options['action']);
        });
      });

      describe("[EVENTS] checking subscription on prompt events", function() {
        beforeEach(function() {
          spyOn(reportViewer, "afterUpdateCallback");
          spyOn(reportViewer, "submitReport");
          spyOn(reportViewer.view, "promptReady");
          spyOn(reportViewer.view, "afterRender");
          reportViewer._bindPromptEvents();
        });

        it("should check subscription on prompt events (with autoSubmit = true)", function (done) {
          $.ajax.and.callFake(function (params) {
            var changedResult = parameterDefinition.replace("name=\"PROD_LINE\"", "name=\"test\"");
            params.success(changedResult);
          });
          reportViewer.reportPrompt.api.operation.refreshPrompt(true);
          setTimeout(function() {
            expect(reportViewer.afterUpdateCallback).toHaveBeenCalled();
            expect(reportViewer.submitReport).toHaveBeenCalledWith({
              isInit: true
            });
            expect(reportViewer.view.promptReady).toHaveBeenCalled();
            expect(reportViewer.view.afterRender).toHaveBeenCalled();
            done();
          }, 500);
        });

        it("should check subscription on prompt events (with autoSubmit = false)", function(done) {
          reportViewer.reportPrompt.api.operation.state({
            "autoSubmit": false
          });
          reportViewer.reportPrompt.api.operation.refreshPrompt(true);
          setTimeout(function() {
            expect(reportViewer.afterUpdateCallback).toHaveBeenCalled();
            expect(reportViewer.submitReport).not.toHaveBeenCalled();
            expect(reportViewer.view.promptReady).toHaveBeenCalled();
            expect(reportViewer.view.afterRender).not.toHaveBeenCalled();
            done();
          }, 500);
        });
      });

      describe("updateLayout", function() {
        beforeEach(function() {
          spyOn(reportViewer.view, "_hideToolbarPromptControls");
          spyOn(reportViewer.view, "_showReportContent");
          spyOn(reportViewer.view, "_initLayout");
        });

        it("should hide toolbar prompts control and should show report content", function() {
          spyOn(reportViewer.reportPrompt, "_getStateProperty").and.callFake(function(param) {
            return (param === "showParameterUI") ? false : true;
          });
          spyOn(reportViewer.view, "_calcReportContentVisibility").and.returnValue(false);

          reportViewer.view.updateLayout();

          expect(reportViewer.view._hideToolbarPromptControls).toHaveBeenCalled();
          expect(reportViewer.view._showReportContent).toHaveBeenCalledWith(false);
          expect(reportViewer.view._initLayout).toHaveBeenCalled();
        });

        it("should not hide toolbar prompts control and should not show report content", function() {
          spyOn(reportViewer.reportPrompt, "_getStateProperty").and.callFake(function(param) {
            return (param === "showParameterUI") ? true : false;
          });
          spyOn(reportViewer.view, "_calcReportContentVisibility").and.returnValue(true);

          reportViewer.view.updateLayout();

          expect(reportViewer.view._hideToolbarPromptControls).not.toHaveBeenCalled();
          expect(reportViewer.view._showReportContent).not.toHaveBeenCalled();
          expect(reportViewer.view._initLayout).toHaveBeenCalled();
        });
      });

      describe("submitReport", function() {
        beforeEach(function() {
          spyOn(reportViewer, "_submitReportEnded");
          spyOn(reportViewer.view, "_initLayout");
          spyOn(reportViewer, "_updateReportContentCore");
          spyOn(reportViewer.view, "_showReportContent");
          spyOn(reportViewer.reportPrompt.api.operation, "refreshPrompt");
        });

        afterEach(function() {
          window.inSchedulerDialog = false;
        });

        it("should not update report content for scheduler dialog", function() {
          window.inSchedulerDialog = true;
          reportViewer.submitReport();

          expect(reportViewer._submitReportEnded).toHaveBeenCalled();
          expect(reportViewer.view._initLayout).not.toHaveBeenCalled();
          expect(reportViewer._updateReportContentCore).not.toHaveBeenCalled();
          expect(reportViewer.view._showReportContent).not.toHaveBeenCalled();
          expect(reportViewer.reportPrompt.api.operation.refreshPrompt).not.toHaveBeenCalled();
        });

        it("should not update report content if needed prompt", function() {
          //spy for 'promptNeeded' and 'autoSubmit' property
          spyOn(reportViewer.reportPrompt, "_getStateProperty").and.returnValue(true);

          reportViewer.submitReport();

          expect(reportViewer._submitReportEnded).toHaveBeenCalled();
          expect(reportViewer.view._initLayout).toHaveBeenCalled();
          expect(reportViewer._updateReportContentCore).not.toHaveBeenCalled();
          expect(reportViewer.view._showReportContent).toHaveBeenCalledWith(false);
          expect(reportViewer.reportPrompt.api.operation.refreshPrompt).not.toHaveBeenCalled();
        });

        it("should update report content if autoSubmit on", function() {
          spyOn(reportViewer.reportPrompt, "_getStateProperty").and.callFake(function(param) {
            return (param === "autoSubmit") ? true : false;
          });

          reportViewer.submitReport();

          expect(reportViewer._submitReportEnded).not.toHaveBeenCalled();
          expect(reportViewer.view._initLayout).toHaveBeenCalled();
          expect(reportViewer._updateReportContentCore).toHaveBeenCalled();
          expect(reportViewer.view._showReportContent).not.toHaveBeenCalled();
          expect(reportViewer.reportPrompt.api.operation.refreshPrompt).not.toHaveBeenCalled();
        });

        it("should refresh prompt if autoSubmit off", function() {
          //spy for 'promptNeeded' and 'autoSubmit' property
          spyOn(reportViewer.reportPrompt, "_getStateProperty").and.returnValue(false);

          reportViewer.submitReport();

          expect(reportViewer._submitReportEnded).not.toHaveBeenCalled();
          expect(reportViewer.view._initLayout).toHaveBeenCalled();
          expect(reportViewer._updateReportContentCore).not.toHaveBeenCalled();
          expect(reportViewer.view._showReportContent).not.toHaveBeenCalled();
          expect(reportViewer.reportPrompt.api.operation.refreshPrompt).toHaveBeenCalled();
        });

        it("should not update report content if isSuppressSubmit on", function() {
          spyOn(reportViewer.reportPrompt, "_getStateProperty").and.callFake(function(param) {
            return (param === "isSuppressSubmit" || param === "autoSubmit") ? true : false;
          });

          reportViewer.submitReport();

          expect(reportViewer._submitReportEnded).not.toHaveBeenCalled();
          expect(reportViewer.view._initLayout).toHaveBeenCalled();
          expect(reportViewer._updateReportContentCore).not.toHaveBeenCalled();
          expect(reportViewer.view._showReportContent).not.toHaveBeenCalled();
          expect(reportViewer.reportPrompt.api.operation.refreshPrompt).not.toHaveBeenCalled();
        });

      });

      describe("promptReady", function() {
        beforeEach(function() {
          spyOn(reportViewer.reportPrompt, "hideGlassPane");
          spyOn(reportViewer.view, "showPromptPanel");
        });

        it("should execute logic not for scheduler dialog", function() {
          window.inSchedulerDialog = false;

          reportViewer.view.promptReady();

          expect(reportViewer.reportPrompt.hideGlassPane).toHaveBeenCalled();
          expect(reportViewer.view.showPromptPanel).not.toHaveBeenCalled();
        });

        it("should execute logic for scheduler dialog", function() {
          window.inSchedulerDialog = true;

          reportViewer.view.promptReady();

          expect(reportViewer.reportPrompt.hideGlassPane).toHaveBeenCalled();
          expect(reportViewer.view.showPromptPanel).toHaveBeenCalledWith(true);
          expect(domClass.add).toHaveBeenCalledWith('toolbarlinner2', 'hidden');
          expect(domClass.remove).toHaveBeenCalledWith('promptPanel', 'pentaho-rounded-panel-bottom-lr');
          expect(domClass.remove).toHaveBeenCalledWith('reportControlPanel', 'pentaho-shadow');
          expect(domClass.remove).toHaveBeenCalledWith('reportControlPanel', 'pentaho-rounded-panel-bottom-lr');
        });
      });

      describe("_updateReportContentCore sync mode", function() {

        var expectedStatus = "\"FINISHED\"";

        beforeEach(function() {
          // do not share viewer state between executions

          spyOn( reportViewer, "_hideAsyncScreens" );
          spyOn( reportViewer, "_getFeedbackScreen" );

          pentahoPost = jasmine.createSpy("pentahoPost").and.callFake( function(url, query, func, mimeType) {
            if ( url.indexOf("reserveId") !== -1 ) {
              // this is reserve id request
              func("{ \"reservedId\" : \"6660666\" }");
            } else {
              // ok return some status;
              func("{ \"status\" : " + expectedStatus + " }");
            }
          });

          spyOn(reportViewer, "_keepPolling");
          spyOn(reportViewer, "_updateFeedbackScreen");

          reportViewer.reportPrompt._isAsync = true;
          spyOn( reportViewer.reportPrompt, "_getStateProperty").and.returnValue(false);
        });

        it("should set false isFinished when current report status undefined", function() {
          expectedStatus = "\"WORKING\"";

          reportViewer._updateReportContentCore();
          expect(reportViewer._isFinished).toBe(false);
        });
      });
    });
  });
