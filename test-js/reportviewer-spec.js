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

define(["reportviewer/reportviewer", "reportviewer/reportviewer-logging", "reportviewer/reportviewer-prompt", "common-ui/jquery-clean",
      "text!./parameterDefinition.xml!strip", "dojo/dom-class", 'common-ui/util/util', "./utils/registryMock"],
    function (ReportViewer, Logging, Prompt, $, parameterDefinition, domClass, util, registryMock) {

      describe("Report Viewer", function () {
        var reportViewer;

        beforeAll(function() {
          var mockGlassPane = jasmine.createSpyObj("glassPane", ["show", "hide"]);
          mockGlassPane.id = "glassPane";
          registryMock.mock(mockGlassPane);
        });

        afterAll(function() {
          registryMock.unMock("glassPane");
        });

        beforeEach(function () {
          window._isTopReportViewer = true;
          window.inMobile = true;
          var options = {parent: window.parent.logger};
          window.logged = Logging.create(window.name, options);

          var reportPrompt = new Prompt();

          spyOn(reportPrompt, 'initPromptPanel').and.callFake(function () {});

          spyOn($, 'ajax').and.callFake(function (params) {
            params.success(parameterDefinition);
          });

          spyOn(domClass, 'add').and.callFake(function() {});
          spyOn(domClass, 'remove').and.callFake(function() {});

          reportPrompt.createPromptPanel();
          reportViewer = new ReportViewer(reportPrompt);
        });

        describe("state properties", function() {

          beforeEach(function () {
            window.inSchedulerDialog = true;
            spyOn(reportViewer.reportPrompt, '_getStateProperty').and.callThrough();
          });

          it("should get the promtpNeeded value", function() {
            expect(reportViewer.reportPrompt._getStateProperty('promptNeeded')).toBeFalsy();

            // Force new value for promptNeeded on the parameter definition
            reportViewer.reportPrompt.panel.paramDefn.promptNeeded = true;
            expect(reportViewer.reportPrompt._getStateProperty('promptNeeded')).toBeTruthy();
          });

          it("should get promtpNeeded on initPrompt", function() {
            spyOn(reportViewer.view, '_initLayout').and.callFake(function() {});
            spyOn(reportViewer.view, '_showReportContent').and.callFake(function() {});
            spyOn(reportViewer.view, '_hasReportContent').and.returnValue(true);

            reportViewer.view.initPrompt(function() {});
            expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
          });

          it("should get promtpNeeded on promptReady", function() {
            window.parameterValidityCallback = function() {};

            spyOn(reportViewer.view, 'showPromptPanel').and.callFake(function() {});

            reportViewer.view.promptReady(function() {});
            expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
          });

          it("should get promtpNeeded on initLayout", function() {
            spyOn(reportViewer.view, 'updatePageControl').and.callFake(function() {});
            spyOn(reportViewer.view, 'showPromptPanel').and.callFake(function() {});

            reportViewer.view._initLayout();
            expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
          });

          it("should get promtpNeeded on submitReport", function() {
            window.inSchedulerDialog = false;
            spyOn(reportViewer.view, '_initLayout').and.callFake(function() {});
            spyOn(reportViewer, '_updateReportContent').and.callFake(function() {});

            reportViewer.submitReport();
            expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('promptNeeded');
          });

          it("should get autoSubmit on _updateReportContent", function() {
            spyOn(reportViewer.reportPrompt.panel, 'refresh').and.callFake(function() {});
            spyOn(reportViewer, '_updateReportContentCore').and.callFake(function() {});
            
            reportViewer._updateReportContent();
            expect(reportViewer.reportPrompt._getStateProperty).toHaveBeenCalledWith('autoSubmit');
          });
        });

        describe("accepted page", function () {

          describe("set", function () {

            beforeEach(function () {
              spyOn(reportViewer.reportPrompt.api.operation, "setParameterValue").and.callThrough();
            });

            it("was called correctly", function () {
              reportViewer.view._setAcceptedPage("2");
              expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "2");
            });

            it("was called correctly on pageChanged", function () {
              spyOn(reportViewer.view, "pageChanged").and.callThrough();
              reportViewer.view.pageChanged("20");
              expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "19");
            });

            describe("was called correctly on updatePageControl with paginate as", function () {
              beforeEach(function () {
                var pageControlMock = jasmine.createSpyObj("pageControl", ["registerPageNumberChangeCallback", "setPageCount", "setPageNumber"]);
                pageControlMock.id = "pageControl";
                registryMock.mock(pageControlMock);

                spyOn(reportViewer.view, "_setAcceptedPage").and.callThrough();
              });

              afterEach(function() {
                registryMock.unMock("pageControl");
              });

              it("false", function () {
                reportViewer.reportPrompt.panel.paramDefn.paginate = false;
                reportViewer.view.updatePageControl();
                expect(reportViewer.view._setAcceptedPage).toHaveBeenCalledWith("-1");
                expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "-1");
              });

              it("true", function () {
                reportViewer.reportPrompt.panel.paramDefn.paginate = true;
                reportViewer.reportPrompt.panel.paramDefn.page = 5;
                reportViewer.reportPrompt.panel.paramDefn.totalPages = 8;
                reportViewer.view.updatePageControl();
                expect(reportViewer.view._setAcceptedPage).toHaveBeenCalledWith("5");
                expect(reportViewer.reportPrompt.api.operation.setParameterValue).toHaveBeenCalledWith("accepted-page", "5");
              });
            });
          });

          describe("_buildReportContentOptions", function() {

            it("should call the _buildReportContentOptions on the report viewer prompt", function() {
              var options = {"option1": "option1", 'action': 'testAction'};
              spyOn(reportViewer.reportPrompt, "_buildReportContentOptions").and.returnValue(options);

              var result = reportViewer._buildReportContentOptions();

              expect(reportViewer.reportPrompt._buildReportContentOptions).toHaveBeenCalledWith('REPORT');

              expect(result).toBe(options);
              expect(result['name']).toBe(options['action']);
            });
          });
        });
      });
    });
