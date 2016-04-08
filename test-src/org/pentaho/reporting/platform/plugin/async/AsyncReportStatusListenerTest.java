package org.pentaho.reporting.platform.plugin.async;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class AsyncReportStatusListenerTest {

  @Test
  public void setStatus() throws Exception {
    final  AsyncReportStatusListener listener = new AsyncReportStatusListener(  "", UUID.randomUUID(), "" );
    listener.setStatus( AsyncExecutionStatus.QUEUED );
    assertEquals( AsyncExecutionStatus.QUEUED, listener.getState().getStatus());
    listener.setStatus( AsyncExecutionStatus.CANCELED );
    assertEquals( AsyncExecutionStatus.CANCELED, listener.getState().getStatus());
    listener.setStatus( AsyncExecutionStatus.FINISHED );
    assertEquals( AsyncExecutionStatus.CANCELED, listener.getState().getStatus());
  }
}