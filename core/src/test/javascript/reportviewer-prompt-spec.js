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
 * Copyright (c) 2016-2017 Hitachi Vantara..  All rights reserved.
 */

define(["reportviewer/reportviewer-prompt", "reportviewer/reportviewer-logging", "common-ui/jquery-clean",
    "text!./parameterDefinition.xml!strip", "mocks/registryMock", "common-ui/prompting/api/PromptingAPI",
    "dojo/dom-class", 'common-ui/util/util'
  ],
  function(Prompt, Logging, $, parameterDefinition, registryMock, PromptingAPI, domClass, util) {

    describe("Report Viewer Prompt", function() {
      var reportPrompt;

      beforeAll(function() {
        window.inSchedulerDialog = false;
        var mockGlassPane = jasmine.createSpyObj("glassPane", ["show", "hide"]);
        mockGlassPane.id = "glassPane";
        registryMock.mock(mockGlassPane);
        var messageBox = jasmine.createSpyObj("messageBox", ["setTitle", "setMessage", "setButtons"]);
        messageBox.id = "messageBox";
        registryMock.mock(messageBox);
      });

      afterAll(function() {
        registryMock.unMock("glassPane");
        registryMock.unMock("messageBox");
      });

      beforeEach(function() {
        window._isTopReportViewer = true;
        var options = {
          parent: window.parent.logger
        };
        window.logged = Logging.create(window.name, options);

        reportPrompt = new Prompt();
        spyOn(reportPrompt.api.operation, 'init');
      });

      var makeAjaxSpy = function(error) {
        spyOn($, "ajax").and.callFake(function(params) {
          if (error) {
            params.error({
              statusText: "error"
            });
          } else {
            params.success(parameterDefinition);
          }
        });
      };

      var createPromptExpectactions = function createPromptExpectactions(done, options) {
        makeAjaxSpy();
        // hijacking this function to check the post create expectations
        reportPrompt._hideLoadingIndicator = function() {
          expect(reportPrompt.parseParameterDefinition).toHaveBeenCalled();
          expect(reportPrompt.parseParameterDefinition).toHaveBeenCalledWith(parameterDefinition);
          expect(reportPrompt.api.operation.init).toHaveBeenCalled();
          expect($.ajax).toHaveBeenCalled();
          expect($.ajax.calls.count()).toEqual(options.ajaxCalls);
          expect(reportPrompt.mode).toEqual(options.mode);
          done();
        };

        expect(reportPrompt.mode).toEqual("INITIAL");

        reportPrompt.createPromptPanel();
      };

      it("Properly creates a prompt panel with allowAutoSubmit = true (see parameterDefinition.xml)", function(done) {
        spyOn(reportPrompt, "parseParameterDefinition").and.callThrough();
        createPromptExpectactions(done, {
          ajaxCalls: 2,
          mode: "MANUAL"
        });
      });

      it("Properly creates a prompt panel with allowAutoSubmit = false", function(done) {
        var realParseParameterDefinition = reportPrompt.parseParameterDefinition.bind(reportPrompt);
        spyOn(reportPrompt, "parseParameterDefinition").and.callFake(function(xmlString) {
          var paramDefn = realParseParameterDefinition(xmlString);
          spyOn(paramDefn, "allowAutoSubmit").and.returnValue(false);
          return paramDefn;
        });
        createPromptExpectactions(done, {
          ajaxCalls: 1,
          mode: "INITIAL"
        });
      });

      describe("_buildReportContentOptions", function() {
        var parameterValues = {
          '::session': '::sessionVALUE',
          'action': 'testAction'
        };
        beforeEach(function() {
          spyOn(util, "getUrlParameters").and.returnValue({});
          spyOn(reportPrompt.api.operation, "getParameterValues").and.returnValue(parameterValues);

          window.inMobile = true;

          spyOn(domClass, 'add');
          spyOn(domClass, 'remove');
          makeAjaxSpy();

          reportPrompt.createPromptPanel();
        });

        it("should verify the parameter values are being retrieved from the API", function() {
          var renderMode = "renderMode";
          var result = reportPrompt._buildReportContentOptions(renderMode, true);
          expect(util.getUrlParameters).toHaveBeenCalled();
          expect(reportPrompt.api.operation.getParameterValues).toHaveBeenCalled();

          expect(result['::session']).not.toBeDefined();
          expect(result['renderMode']).toBe(renderMode);
        });

        it("should not call _getStateProperty when promptMode is INITIAL", function() {
          spyOn(reportPrompt, '_getStateProperty').and.callThrough();

          reportPrompt._getParameterDefinitionRenderMode("INITIAL");
          expect(reportPrompt._getStateProperty).not.toHaveBeenCalled();
        });

        it("should call _getStateProperty when promptMode is USERINPUT", function() {
          spyOn(reportPrompt, '_getStateProperty');

          reportPrompt._getParameterDefinitionRenderMode("USERINPUT");
          expect(reportPrompt._getStateProperty).toHaveBeenCalledWith('autoSubmit');
        });
      });

      describe("canSkipParameterChange", function() {
        var parameterValues = {
          'output-target': 'table/html;page-mode=page',
          'showParameters': 'false'
        };
        var _oldParameterSet = {
          'output-target': { 
            'value': 'table/html;page-mode=page',
            'group': 'parameters',
            'name': 'output-target'
          },
          'showParameters': { 
            'value': 'true',
            'group': 'system',
            'name': 'showParameters'
          }
        };
        beforeEach(function() {
          spyOn(reportPrompt.api.operation, "getParameterValues").and.returnValue(parameterValues);
          reportPrompt._oldParameterSet = _oldParameterSet;
        });

        it("should verify the system parameter value changes, output target changes and dependent parameter changes trigger server call", function() {
          expect(reportPrompt.canSkipParameterChange(["showParameters"])).toBeFalsy(); // system parameter changes always trigger a server call
          expect(reportPrompt.api.operation.getParameterValues).toHaveBeenCalled();

          reportPrompt._oldParameterSet = {
            'output-target': { 
              'value': 'table/excel;page-mode=flow',
              'group': 'parameters',
              'name': 'output-target'
            },
            'showParameters': { 
              'value': 'false',
              'group': 'system',
              'name': 'showParameters'
            }
          };
          expect(reportPrompt.canSkipParameterChange(["output-target"])).toBeFalsy(); // output target changes always trigger a server call

          reportPrompt._oldParameterSet = {
            'output-target': { 
              'value': 'table/html;page-mode=page',
              'group': 'parameters',
              'name': 'output-target'
            },
            'showParameters': { 
              'value': 'false',
              'group': 'system',
              'name': 'showParameters'
            }
          };
          var realParseParameterDefinition = reportPrompt.parseParameterDefinition.bind(reportPrompt);
          var paramDefn = realParseParameterDefinition(parameterDefinition);
          reportPrompt._oldParameterDefinition = paramDefn;

          spyOn(reportPrompt._oldParameterDefinition, "getParameter").and.callFake(function(myParam) {
            return {'attributes':{'has-downstream-dependent-parameter':'true'}};
          });
          expect(reportPrompt.canSkipParameterChange(["output-target"])).toBeFalsy(); // changes in a parameter with dependent other parameters triggers a server call
        });

        it("should verify the parameter marked as 'always-validate' value changes trigger server call", function() {
          reportPrompt._oldParameterSet = {
            'output-target': { 
              'value': 'table/html;page-mode=page',
              'group': 'parameters',
              'name': 'output-target'
            },
            'showParameters': { 
              'value': 'false',
              'group': 'system',
              'name': 'showParameters'
            }
          };  
          var realParseParameterDefinition = reportPrompt.parseParameterDefinition.bind(reportPrompt);
          var paramDefn = realParseParameterDefinition(parameterDefinition);
          reportPrompt._oldParameterDefinition = paramDefn;

          spyOn(reportPrompt._oldParameterDefinition, "getParameter").and.callFake(function(myParam) {
            return {'attributes':{'must-validate-on-server':'true'}};
          });
          expect(reportPrompt.canSkipParameterChange(["output-target"])).toBeFalsy(); // parameter marked as "always-validate" always trigger a server call
        });
      });

      describe("showMessageBox", function() {
        var messageBox;

        beforeEach(function() {
          messageBox = jasmine.createSpyObj("messageBox", ["hide", "setTitle", "setMessage", "setButtons", "show"]);
          messageBox.id = "messageBox";
          registryMock.mock(messageBox);
        });

        afterEach(function() {
          registryMock.unMock("messageBox");
        });

        it("should call show and hide progress indicator from the PromptingApi", function(done) {
          makeAjaxSpy(true);

          spyOn(reportPrompt.api.ui, "hideProgressIndicator").and.callThrough();
          spyOn(reportPrompt.api.ui, "showProgressIndicator").and.callThrough();
          messageBox.show = function() {
            expect(messageBox.onCancel).toBeDefined();
            expect(reportPrompt.api.ui.showProgressIndicator).toHaveBeenCalled();
            expect(reportPrompt.api.ui.hideProgressIndicator).not.toHaveBeenCalled();
            messageBox.onCancel();
            expect(reportPrompt.api.ui.hideProgressIndicator).toHaveBeenCalled();
            done();
          };
          reportPrompt.createPromptPanel();
        });
      });

      it("initPromptPanel - should call OperationAPI#init", function() {
        makeAjaxSpy();
        reportPrompt.initPromptPanel();
        expect(reportPrompt.api.operation.init).toHaveBeenCalled();
      });

      describe("parseParameterDefinition", function() {
        it("should parse the parameter xml", function() {
          var xmlString = "xmlString";
          var xmlString1 = "xmlString";
          var parseVal = "parseVal";
          spyOn(reportPrompt, "removeControlCharacters").and.returnValue(xmlString1);
          spyOn(reportPrompt.api.util, "parseParameterXml").and.returnValue(parseVal);

          var returnVal = reportPrompt.parseParameterDefinition(xmlString);
          expect(reportPrompt.removeControlCharacters).toHaveBeenCalledWith(xmlString);
          expect(reportPrompt.api.util.parseParameterXml).toHaveBeenCalledWith(xmlString);
          expect(returnVal).toBe(parseVal);
        });
      });

      describe("should toggle page control visibility", function () {
        var pageControlWidget;
        var toolBarSeparatorWidget;

        beforeEach(function() {
          makeAjaxSpy();

          var messageBox = jasmine.createSpyObj("messageBox", ["hide", "setTitle", "setMessage", "setButtons", "show"]);
          messageBox.id = "messageBox";
          registryMock.mock(messageBox);

          pageControlWidget = jasmine.createSpyObj("pageControl", ["domNode"]);
          toolBarSeparatorWidget = jasmine.createSpyObj("toolbar-parameter-separator", ["domNode"]);

          pageControlWidget.id = "pageControl";
          toolBarSeparatorWidget.id = "toolbar-parameter-separator";
          registryMock.mock(pageControlWidget);
          registryMock.mock(toolBarSeparatorWidget);

          spyOn(domClass, 'add');
          spyOn(domClass, 'remove');

          reportPrompt.createPromptPanel();
        });

        afterAll(function() {
          registryMock.unMock("pageControl");
          registryMock.unMock("toolbar-parameter-separator");
          reportPrompt = new Prompt();
          jasmine.reset(domClass);
        });

        it("should show a control if the output is pageable HTML", function () {
          reportPrompt._isReportHtmlPagebleOutputFormat = true;
          reportPrompt.togglePageControl();
          expect(domClass.remove).toHaveBeenCalledWith(pageControlWidget.domNode, "hidden");
          expect(domClass.remove).toHaveBeenCalledWith(toolBarSeparatorWidget.domNode, "hidden");
        });

        it("should hide a control otherwise", function () {
          reportPrompt._isReportHtmlPagebleOutputFormat = false;
          reportPrompt.togglePageControl();
          expect(domClass.add).toHaveBeenCalledWith(pageControlWidget.domNode, "hidden");
          expect(domClass.add).toHaveBeenCalledWith(toolBarSeparatorWidget.domNode, "hidden");
        });

      });

      describe("should respect formula values on user selections", function () {
        beforeAll(function() {
          var messageBox;
          messageBox = jasmine.createSpyObj("messageBox", ["hide", "setTitle", "setMessage", "setButtons", "show"]);
          messageBox.id = "messageBox";
          registryMock.mock(messageBox);
        });

        it("should not do anything if the values are empty", function () {
          var param = {};
          reportPrompt._applyUserInput("anyname", "test", param);
          expect(reportPrompt.forceUpdate).toBe(undefined);
          reportPrompt._applyUserInput("anyname", "test", {values: []});
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the plaintext value is not changed", function () {
          var param = {values: [{value: "test"}]};
          reportPrompt._applyUserInput("anyname", "test", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the single selection value is not changed", function () {
          var param = {values: [{value: "test", selected: true}, {value: "test1"}],
            attributes: {'parameter-render-type': 'list'}};
          reportPrompt._applyUserInput("anyname", "test", param);
          expect(reportPrompt.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the multiple selection value is not changed", function () {
          var parameter = {
            values: [
              {value: "test", selected: true},
              {value: "test1", selected: true},
              {value: "test2"}
            ],
            attributes: {'parameter-render-type': 'list'}
          };
          reportPrompt._applyUserInput("anyname", ["test", "test1"], parameter);
          expect(parameter.forceUpdate).toBe(undefined);
        });

        it("should force an update if the plaintext value is changed", function () {
          var param = {values: [{value: "test"}], attributes: {}};
          reportPrompt._applyUserInput("anyname", "test1", param);
          expect(param.forceUpdate).toBe(true);
        });

        it("should force an update if the single selection value is changed", function () {
          var param = {values: [{value: "test", selected: true}, {value: "test1"}],
            attributes: {'parameter-render-type': 'list'}};
          reportPrompt._applyUserInput("anyname", "test1", param);
          expect(param.forceUpdate).toBe(true);
        });

        it("should force an update if the multiple selection value is changed", function () {
          var parameter = {
            values: [
              {value: "test", selected: true},
              {value: "test1", selected: true},
              {value: "test2"}
            ],
            attributes: {'parameter-render-type': 'list'}
          };
          reportPrompt._applyUserInput("anyname", ["test", "test2"], parameter);
          expect(parameter.forceUpdate).toBe(true);
        });

        it("should force an update if the multiple selection value has different number of selections", function () {
          var parameter = {
            values: [
              {value: "test", selected: true},
              {value: "test1", selected: true},
              {value: "test2"}
            ],
            attributes: {'parameter-render-type': 'list'}
          };
          reportPrompt._applyUserInput("anyname", ["test"], parameter);
          expect(parameter.forceUpdate).toBe(true);
          var parameter = {
            values: [
              {value: "test", selected: true},
              {value: "test1", selected: true},
              {value: "test2"}
            ],
            attributes: {'parameter-render-type': 'list'}
          };
          reportPrompt._applyUserInput("anyname", ["test", "test1", "test2"], parameter);
          expect(parameter.forceUpdate).toBe(true);
        });

        // [BISERVER-14306]
        // These 2 tests only occur in code when there's a default value for a Date object.  The selection goes through
        // A loop to first remove the previously selected date and then adds the new date value. Code was updated to handle the forced
        // deselection of the old date.
        it("should force an update if the date selection value has a different number of selections", function () {
          var dateSelectionParameter = {
            values: [
              {value: "2019-09-13", selected: false},
              {value: "2019-09-13T00:00:00.000-0400", selected: true}
            ],
            attributes: {'parameter-render-type': 'datepicker'}
          };
          spyOn(reportPrompt, "_createFormatter").and.returnValue(undefined);
          spyOn(reportPrompt, "_compareDatesOnly").and.returnValue(false);
          reportPrompt._applyUserInput("anyname", ["2019-09-10T00:00:00.000"], dateSelectionParameter);

          expect(dateSelectionParameter.values[1].selected).toBe(false);
          expect(dateSelectionParameter.forceUpdate).toBe(true);
        });

        it("should not force an update if the date selection value is the same as one of the stored selections", function () {
          var dateSelectionParameter2 = {
            values: [
              {value: "2019-09-13", selected: false},
              {value: "2019-09-10T00:00:00.000-0400", selected: false}
            ],
            attributes: {'parameter-render-type': 'datepicker'}
          };
          spyOn(reportPrompt, "_createFormatter").and.returnValue(undefined);
          spyOn(reportPrompt, "_compareDatesOnly").and.returnValue(true);
          reportPrompt._applyUserInput("anyname", ["2019-09-10T00:00:00.000"], dateSelectionParameter2);

          expect(dateSelectionParameter2.values[1].selected).toBe(true);
          expect(dateSelectionParameter2.forceUpdate).toBe(true);
        });

        it("should keep parameter as is if we have only differnet timezone", function () {
            var _oldValue = "2017-01-01T11:00:00.000";
            var _newValue = "2017-01-01T11:00:00.000"
            var _timezoneHint = "-0800"
            expect( reportPrompt._compareDatesOnly( _oldValue, _newValue, _timezoneHint, undefined ) ).toBe(true);
          });
        
        //This is true because if a string representation is matcheing then no timezone hints can affect the final result
        it("should treat a matching datepicker values as plaintext", function () {
          var param = {values: [{value: "2017-01-01"}], attributes: {"parameter-render-type": "datepicker"}};
          reportPrompt._applyUserInput("anyname", "2017-01-01", param);
          expect(param.forceUpdate).toBe(undefined);
          var param = {
            values: [{value: "2017-01-01T11:00:00.000"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000", param);
          expect(param.forceUpdate).toBe(undefined);
          var param = {values: [{value: "2017-01-01"}], attributes: {"parameter-render-type": "datepicker"}};
          reportPrompt._applyUserInput("anyname", "2017-01-01", param);
          expect(param.forceUpdate).toBe(undefined);
          var param = {
            values: [{value: "2017-01-01T11:00:00.000+1100"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000+1100", param);
          expect(param.forceUpdate).toBe(undefined);
          var param = {
            values: [{value: "2017-01-01T11:00:00.000+09.530"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000+09.530", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the datepicker value is not changed, different timezones", function () {
          var param = {
            values: [{value: "2017-01-01T11:00:00.000-0100"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T10:00:00.000-0200", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the datepicker value is not changed, no timezone", function () {
          var param = {
            values: [{value: "2017-01-01T11:00:00.000-0100"}],
            attributes: {"parameter-render-type": "datepicker"},
            timezoneHint: "/-0100"
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the datepicker value is not changed, a timezone and a hint", function () {
          var param = {
            values: [{value: "2017-01-01T11:00:00.000-0100"}],
            attributes: {"parameter-render-type": "datepicker", timezoneHint: "/-0100"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T10:00:00.000-0200", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the datepicker value is not changed, Ausie timezone and a hint", function () {
          var param = {
            values: [{value: "2017-01-01T11:00:00.000+0930"}],
            attributes: {"parameter-render-type": "datepicker"},
            timezoneHint: "/-0100"
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000+09.530", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if the datepicker value is not changed, Mauri timezone and a hint", function () {
          var param = {
            values: [{value: "2017-01-01T11:00:00.000+1145"}],
            attributes: {"parameter-render-type": "datepicker"},
            timezoneHint: "/-0100"
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000+11.7545", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should not force an update if only seconds and milliseconds changed", function () {
          var param = {
            values: [{value: "2017-01-01T00:0:44.010"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000", param);
          expect(param.forceUpdate).toBe(undefined);
        });

        it("should force an update if the date changed, no hint", function () {
          var param = {
            values: [{value: "2017-01-01T11:00:00.000"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          spyOn(reportPrompt, "_createFormatter").and.returnValue(undefined);
          spyOn(reportPrompt, "_compareDatesOnly").and.returnValue(false);
          reportPrompt._applyUserInput("anyname", "2017-01-02T11:00:00.000", param);
          expect(param.forceUpdate).toBe(true);
          var param = {
            values: [{value: "2017-01-01T00:00:00.000"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-02T11:00:00.000", param);
          expect(param.forceUpdate).toBe(true);
          var param = {
            values: [{value: "2017-01-01T00:01:00.000"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T11:00:00.000", param);
          expect(param.forceUpdate).toBe(true);
        });

        it("should force an update same time, timezones changed", function () {
          var param = {
            values: [{value: "2017-01-01T00:00:00.000-0100"}],
            attributes: {"parameter-render-type": "datepicker"}
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T00:00:00.000+0100", param);
          setTimeout(500, function () {
            expect(param.forceUpdate).toBe(true);
          });
        });

        it("should force an update same time, timezones different, hint is present", function () {
          var param = {
            values: [{value: "2017-01-01T00:00:00.000-0100"}],
            attributes: {"parameter-render-type": "datepicker"},
            timezoneHint: "/-0100"
          };
          reportPrompt._applyUserInput("anyname", "2017-01-01T00:00:00.000+0100", param);
          setTimeout(500, function () {
            expect(param.forceUpdate).toBe(true);
          });
        });
      });
    });
  });
