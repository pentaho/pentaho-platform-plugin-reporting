/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


define(["dijit/registry"], function(registry) {
  var real = {};
  return {
    mock: function(widget) {
      if (typeof registry.byId(widget.id) !== "undefined") {
        real[widget.id] = registry.byId(widget.id);
      }
      registry.remove(widget.id);
      registry.add(widget);
    },
    unMock: function(id) {
      registry.remove(id);
      if (typeof real[id] !== "undefined") {
        registry.add(real[id]);
        delete real[id];
      }
    }
  };
});
