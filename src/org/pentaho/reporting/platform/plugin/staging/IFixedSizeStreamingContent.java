package org.pentaho.reporting.platform.plugin.staging;

import java.io.InputStream;

/**
 * Created by dima.prokopenko@gmail.com on 3/31/2016.
 */
public interface IFixedSizeStreamingContent {

  InputStream getStream();
  long getContentSize();

}
