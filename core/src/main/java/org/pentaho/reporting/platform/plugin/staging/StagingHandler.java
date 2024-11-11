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


package org.pentaho.reporting.platform.plugin.staging;

import org.pentaho.reporting.engine.classic.core.util.StagingMode;

import java.io.IOException;
import java.io.OutputStream;

public interface StagingHandler {

  StagingMode getStagingMode();

  boolean isFullyBuffered();

  boolean canSendHeaders();

  OutputStream getStagingOutputStream();

  void complete() throws IOException;

  void close();

  int getWrittenByteCount();
}
