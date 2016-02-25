package org.pentaho.reporting.platform.plugin.async;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by dima.prokopenko@gmail.com on 2/12/2016.
 */
public interface IAsyncReportState extends Serializable {

  /**
   * @return Report path to be shown on ui
   */
  String getPath();

  /**
   * @return Identifier of async task
   */
  UUID getUuid();

  /**
   * @return Status of running task
   */
  AsyncExecutionStatus getStatus();

  /**
   * @return Progress from 0 to 100
   */
  int getProgress();

  /**
   *
   * @return Page is currently being processed
   */
  int getPage();

  /**
   *
   * @return Activity code is currently being processed
   */
  String getActivity();

  /**
   * @return mime type advice of report content that will be generated at the end.
   */
  String getMimeType();
}
