/*!
 * Copyright 2010 - 2016 Pentaho Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
define(["dojo/_base/declare", "dijit/_WidgetBase", "dijit/_TemplatedMixin", "dijit/_WidgetsInTemplateMixin", "dijit/_Templated", "dojo/on", "dojo/keys", "dojo/_base/lang", "dojo/query",
        "pentaho/common/button", "pentaho/common/Dialog", "dijit/form/Select", "dijit/focus", "dijit/form/TextBox"],
    function (declare, _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, _Templated, on, keys, lang, query, button, Dialog, select, focusUtil, TextBox) {
        return declare("pentaho.reportviewer.RowLimitControl", [_WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin], {

                _onSelectCallback: undefined,
                _onRowLimitSubmitCallback: undefined,
                _defaultRowLimit: undefined,
                _previousRowLimit: undefined,

                templateString: "<div class='rl_rowLimitControlContainer'><div id='row-limit-label' class='rl_rowLimitLabel' data-dojo-attach-point='rowLimitLabel'></div><div class='rl_rowLimitRestrictionsSelect'><select id='row-limit-restrictions' data-dojo-type='dijit.form.Select' data-dojo-attach-point='rowLimitRestrictions'></select></div><div class='rl_rowsNumberInput'><input id='rowsNumberInput' dojoType='dijit.form.TextBox' value='' data-dojo-attach-point='rowsNumberInput'></input></div></div>",

                postCreate: function () {
                    this.inherited(arguments);
                    on(this.rowLimitRestrictions, 'change', lang.hitch(this, '_onSelect'));
                    on(this.rowsNumberInput, 'keydown, focusout', lang.hitch(this, '_onRowLimitSubmit'));
                },

                registerLocalizationLookup: function (f) {
                    this.getLocaleString = f;
                    this._localize();
                },

                _localize: function () {
                    this.rowLimitLabel.innerHTML = this.getLocaleString("RowLimitLabel");
                    this.rowLimitRestrictions.addOption({
                        label: this.getLocaleString("RowLimitNoMoreThanTitle"),
                        value: 'NO_MORE_THAN'
                    });
                    this.rowLimitRestrictions.addOption({
                        label: this.getLocaleString("RowLimitMaximumTitle"),
                        value: 'MAXIMUM'
                    });
                },

                registerOnSelectCallback: function (f) {
                    this._onSelectCallback = f;
                },

                registerOnRowLimitSubmitCallback: function (f) {
                    this._onRowLimitSubmitCallback = f;
                },

                setRowLimit: function (rowLimit) {
                    this._previousRowLimit = this.getRowLimit();
                    this.rowsNumberInput.set("value", rowLimit);
                },

                getRowLimit: function (rowLimit) {
                    return this.rowsNumberInput.get('value');
                },

                setRowLimitRestrictions: function (rowLimitRestrictionsValue) {
                    this.rowLimitRestrictions.set("value", rowLimitRestrictionsValue);
                },

                setRowsNumberInputDisabled: function (isDisabled) {
                    this.rowsNumberInput.set('disabled', isDisabled);
                },

                setDefaultRowLimit: function (defaultRowLimit) {
                    this._defaultRowLimit = defaultRowLimit;
                },

                _onSelect: function (event) {
                    if (this._onSelectCallback) {
                        try {
                            this._onSelectCallback(event);
                        } catch (e) {
                            console.warn("Error in onSelectCallback of Row Limit Control: " + e);
                        }
                    }
                },

                _onRowLimitSubmit: function (event) {
                    if (typeof event != 'undefined' && event.keyCode === keys.ENTER) {
                        focusUtil.curNode && focusUtil.curNode.blur();
                        return;
                    }

                    if (typeof event != 'undefined' && event.type === 'blur') {
                        if (isNaN(this.getRowLimit()) || this.getRowLimit() < 1) {
                            this.setRowLimit(this._defaultRowLimit);
                            return;
                        }

                        if (this._onRowLimitSubmitCallback) {
                            try {
                                if (this._previousRowLimit === this.getRowLimit()) {
                                    return;
                                }
                                this._onRowLimitSubmitCallback(event);
                            } catch (e) {
                                console.warn("Error in onRowLimitSubmitCallback of Row Limit Control: " + e);
                            }
                        }
                    }

                    // Allow: backspace, delete, tab, escape, enter and .
                    if ($.inArray(event.keyCode, [46, 8, 9, 27, 13]) !== -1 ||
                        // Allow: Ctrl+A
                        (event.keyCode == 65 && event.ctrlKey === true) ||
                        // Allow: Ctrl+C
                        (event.keyCode == 67 && event.ctrlKey === true) ||
                        // Allow: Ctrl+X
                        (event.keyCode == 88 && event.ctrlKey === true) ||
                        // Allow: home, end, left, right
                        (event.keyCode >= 35 && event.keyCode <= 39)) {
                        // let it happen, don't do anything
                        return;
                    }
                    // Ensure that it is a number and stop the keypress
                    if ((event.shiftKey || (event.keyCode < 48 || event.keyCode > 57)) && (event.keyCode < 96 || event.keyCode > 105)) {
                        event.preventDefault();
                    }
                }

            }
        );

    });

