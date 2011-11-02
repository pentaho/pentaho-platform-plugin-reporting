package org.pentaho.reporting.platform.plugin;

import java.util.ResourceBundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;

/**
 * This class makes a message bundle available as a JSON string. 
 * This is designed to be used as a web service to allow thin-clients
 * to retrieve message bundles from the server
 * <p>
 * Note: This was copied from PIR and now resides in at least 3 places: PIR, Common-UI, Platform Reporting Plugin. http://jira.pentaho.com/browse/BISERVER-6718
 * </p>
 * @author Jordan Ganoff (jganoff@pentaho.com)
 *
 */
public class LocalizationService {
  private static final String PLUGIN_ID = "Pentaho Reporting Plugin"; //$NON-NLS-1$

  private static final String DEFAULT_BUNDLE_NAME = "reportviewer/messages/messages"; //$NON-NLS-1$

  private static final String DEFAULT_CACHE_MESSAGES_SETTING = "false"; //$NON-NLS-1$

  private ResourceBundle getBundle(String name) {
    IPluginManager pm = PentahoSystem.get(IPluginManager.class);
    ClassLoader pluginClassLoader = pm.getClassLoader(PLUGIN_ID);

    ResourceBundle bundle = ResourceBundle.getBundle(name, LocaleHelper.getLocale(), pluginClassLoader);
    String cache = PentahoSystem.getSystemSetting("cache-messages", DEFAULT_CACHE_MESSAGES_SETTING); //$NON-NLS-1$
    // Check whether we want to clear the bundle cache which is useful to test resource file changes
    if (cache != null && cache.equals(DEFAULT_CACHE_MESSAGES_SETTING)) {
      ResourceBundle.clearCache();
    }
    return bundle;
  }

  /**
   * @return the default message bundle as a JSON string
   */
  public String getJSONBundle() {
    return getJSONBundle(DEFAULT_BUNDLE_NAME);
  }

  public String getJSONBundle(String name) {
    try {
      return getJsonForBundle(getBundle(name));
    } catch (Exception e) {
      throw new RuntimeException(e.toString(), e);
    }
  }

  /**
   * Convert a {@see ResourceBundle} into a JSON string.
   * 
   * @param bundle
   * @return Bundle with all key/value pairs as entries in a hash, returned as a JSON string.
   * @throws JSONException
   */
  private String getJsonForBundle(ResourceBundle bundle) throws JSONException {
    JSONObject cat = new JSONObject();
    for (String key : bundle.keySet()) {
      cat.put(key, bundle.getString(key));
    }
    return cat.toString();
  }
}
