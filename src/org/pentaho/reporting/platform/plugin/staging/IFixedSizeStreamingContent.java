package org.pentaho.reporting.platform.plugin.staging;

import java.io.InputStream;

/**
 * Created by dima.prokopenko@gmail.com on 3/31/2016.
 */
public interface IFixedSizeStreamingContent {

  /**
   * Creates input stream from staging content.
   * This stream must be closed manually.
   *
   * @return
   */
  InputStream getStream();
  long getContentSize();
  boolean cleanContent();

}
