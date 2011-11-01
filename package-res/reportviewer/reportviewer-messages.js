var ReportViewerMessages = function() {
  this._bundle = {
    'submitButtonText': 'View Report',
    'OK': 'Ok'
  }

  /**
   * Get a string from a backing resource bundle.
   * @param key Resource key to look up
   * @param defaultString String to use if the key doesn't exist in the resource bundle.
   * @param argArray Array of arguments for replacements.
   */
  this.getString = function(key, defaultString, argArray) {
    // TODO Implement argArray replacement
    return this._bundle[key] || defaultString || '!' + key + '!';
  }
}