package org.pentaho.reporting.platform.plugin.async;

/**
 * Created by dima.prokopenko@gmail.com on 2/12/2016.
 */
public enum AsyncExecutionStatus {
  QUEUED, WORKING, CONTENT_AVAILABLE, FINISHED, FAILED, CANCELED
}
