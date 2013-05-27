pen.define(function() {
  var levelIndentText = "&nbsp;&nbsp;&nbsp;&nbsp;";
  
  var S = function(s) { return s == null ? "" : String(s); };
  var htmlEscape = function(s) { 
    return S(s).replace('&', '&amp;').replace('<', '&lt;'); // order is relevant 
  };
  
  // Log-decorates a function
  var logWrap = function (p, f, logger) {
    if(f.__logwrapper__) { return f; }
    
    var g = function() {
      logger.group(p);
      var v0 = logger.count();
      var result, error;
      try {
        return (result = f.apply(this, arguments));
      } catch(ex) {
        error = ex;
        logger.raw("$*? " + htmlEscape(ex), null, v0);
        throw ex;
      } finally {
        if(!error && result !== undefined) {
          logger.raw("&rarr; " + htmlEscape(result), null, v0);
        }
        logger.groupEnd(p);
      }
    };
    
    g.__logwrapper__ = true;
    
    return g;
  };
  
  // Create a new logger
  var createLogger = function(id, options) {
    if(!options) { options= {}; }
    if(!id) { id = "unnamed"; }
    
    var parent = options.parent;
    if(parent) { // => enabled
      return {
          id:       id,
          raw:      function(s, lid, v) { parent.raw(s, lid || id, v);   },
          log:      function(s, lid, v) { parent.log(s, lid || id, v);   },
          group:    function(s, lid, v) { parent.group(s, lid || id, v); },
          groupEnd: function(         ) { parent.groupEnd();             },
          count:    function(         ) { return parent.count();         }
      };
    }
    
    var enabled = !!options.enabled;
    if(!enabled) { return null; }
    
    // May be null in case popups blocked
    var logWin = window.open('', options.winname || 'report_viewer_log');
    if(!logWin) { return null; }
    
    var logDoc = logWin.document;
    
    // Clear existing content, in case it's not a new window.
    logDoc.open();
    logDoc.write('<h1>LOG</h1><table id="logTable"><tbody /></table>');
    logDoc.close();
    var logTBody = $(logDoc).find('#logTable > tbody');
    
    var levels = [];
    var counter = 1;
    var loggerColors = {};
    var colors = ['green', 'blue', 'red'];
    var nextColor = 0;
    var getLoggerColor = function(lid) {
      return loggerColors[lid] || (loggerColors[lid] = colors[nextColor++ % colors.length]);
    };
    
    var write = function(s, lid, v) {
      if(lid == null) { lid = id; }
      var append = v != null && (v === counter);
      counter++;
      try {
        if(append) {
          $('<span>&nbsp;' + S(s) + '</span>', logDoc)
            .appendTo(
              logTBody
              .children().last()   // last tr
              .children().last()); // last td
        } else {
          $('<tr><td style="color:' + getLoggerColor(lid) + ';">' + lid + '</td>' + 
                '<td>' + levels.join('') + S(s) + '</td>' + 
            '</tr>', logDoc)
            .appendTo(logTBody);
        }
      } catch(ex) {
        alert("[" + lid + "] log - " + ex);
      }
    };
    
    return {
      raw:      function(s, lid, v) { write(s, lid, v); },
      log:      function(s, lid, v) { write("&bull; " + htmlEscape(s), lid, v); },
      group:    function(s, lid, v) {
        write("&rsaquo; " + htmlEscape(s), lid, v);
        levels.push(levelIndentText);
      },
      groupEnd: function() { counter++; levels.pop(); },
      count:    function() { return counter; }
    };
  };
  
  var createLogged = function(logger) {
    var logged;
    if(logger) {
      logged = function(o, f) {
        switch(typeof o) {
          case 'string':   return logWrap(/*name*/o, f, logger);
          case 'function': return logWrap(/*name*/'anonymous', o, logger);
        }
        
        for(var p in o) {
          var f = o[p];
          if(typeof f === 'function') {
            o[p] = logWrap(p, f, logger);
          }
        }
        return o;
      };
    } else {
      logged = function(o, f) {
        return (typeof o === 'string') ? f : o; 
      };
    }
    logged.logger = logger;
    return logged;
  };

  return {
    create: function(id, options) {
      // logger may be null
      // but logged is always not null
      var logger = createLogger(id, options);
      return createLogged(logger);
    }
  };
});
