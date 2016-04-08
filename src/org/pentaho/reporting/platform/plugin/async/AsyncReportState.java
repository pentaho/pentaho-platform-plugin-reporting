package org.pentaho.reporting.platform.plugin.async;

import java.util.UUID;

public class AsyncReportState implements IAsyncReportState {
  private final String path;
  private final UUID uuid;
  private final AsyncExecutionStatus status;
  private final int progress;
  private final int page;
  private final int totalPages;
  private final int row;
  private final int totalRows;
  private final String activity;
  private final String mimeType;
  private final int generatedPage;

  public AsyncReportState( final UUID id, final String path ) {
    this.status = AsyncExecutionStatus.QUEUED;
    this.uuid = id;
    this.path = path;
    this.progress = 0;
    this.page = 0;
    this.totalPages = 0;
    this.generatedPage = 0;
    this.row = 0;
    this.totalRows = 0;
    this.activity = null;
    this.mimeType = null;
  }

  public AsyncReportState( final UUID uuid,
                           final String path,
                           final AsyncExecutionStatus status,
                           final int progress,
                           final int row,
                           final int totalRows,
                           final int page,
                           final int totalPages,
                           final int generatedPage,
                           final String activity,
                           final String mimeType ) {
    this.uuid = uuid;
    this.path = path;
    this.status = status;
    this.progress = progress;
    this.row = row;
    this.totalRows = totalRows;
    this.page = page;
    this.totalPages = totalPages;
    this.generatedPage = generatedPage;
    this.activity = activity;
    this.mimeType = mimeType;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public UUID getUuid() {
    return uuid;
  }

  @Override
  public AsyncExecutionStatus getStatus() {
    return status;
  }

  @Override
  public int getProgress() {
    return progress;
  }

  @Override
  public int getPage() {
    return page;
  }

  @Override
  public int getTotalPages() {
    return totalPages;
  }

  @Override public int getGeneratedPage() {
    return generatedPage;
  }

  @Override
  public int getRow() {
    return row;
  }

  @Override
  public int getTotalRows() {
    return totalRows;
  }

  @Override
  public String getActivity() {
    return activity;
  }

  @Override
  public String getMimeType() {
    return mimeType;
  }

}
