/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.core.playlist;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.step.errorhandling.AbstractFileErrorHandler;

public class FilePlayListReplay implements FilePlayList {

  private final Date replayDate;

  private final String encoding;

  private final String lineNumberDirectory;

  private final String lineNumberExtension;

  private final String errorDirectory;

  private final String errorExtension;

  private FilePlayListReplayFile currentLineNumberFile;

  private FilePlayListReplayFile currentErrorFile;

  public FilePlayListReplay( Date replayDate, String lineNumberDirectory, String lineNumberExtension,
    String errorDirectory, String errorExtension, String encoding ) {
    this.replayDate = replayDate;
    this.errorDirectory = errorDirectory;
    this.errorExtension = errorExtension;
    this.encoding = encoding;
    this.lineNumberDirectory = lineNumberDirectory;
    this.lineNumberExtension = lineNumberExtension;

  }

  private FileObject getCurrentProcessingFile() {
    FileObject result = null;
    if ( currentLineNumberFile != null ) {
      result = currentLineNumberFile.getProcessingFile();
    }
    return result;
  }

  private String getCurrentProcessingFilePart() {
    String result = null;
    if ( currentLineNumberFile != null ) {
      result = currentLineNumberFile.getProcessingFilePart();
    }
    return result;
  }

  public boolean isProcessingNeeded( FileObject file, long lineNr, String filePart ) throws KettleException {
    initializeCurrentIfNeeded( file, filePart );
    return currentLineNumberFile.isProcessingNeeded( file, lineNr, filePart )
      || currentErrorFile.isProcessingNeeded( file, lineNr, filePart );
  }

  private void initializeCurrentIfNeeded( FileObject file, String filePart ) throws KettleException {
    if ( !( file.equals( getCurrentProcessingFile() ) && filePart.equals( getCurrentProcessingFilePart() ) ) ) {
      initializeCurrent( file, filePart );
    }
  }

  private void initializeCurrent( FileObject file, String filePart ) throws KettleException {
    try {
      FileObject lineFile =
        AbstractFileErrorHandler.getReplayFilename(
          lineNumberDirectory, file.getName().getBaseName(), replayDate, lineNumberExtension, filePart );
      if ( lineFile.exists() ) {
        currentLineNumberFile = new FilePlayListReplayLineNumberFile( lineFile, encoding, file, filePart );
      } else {
        currentLineNumberFile = new FilePlayListReplayFile( file, filePart );
      }

      FileObject errorFile =
        AbstractFileErrorHandler.getReplayFilename(
          errorDirectory, file.getName().getBaseName(), replayDate, errorExtension,
          AbstractFileErrorHandler.NO_PARTS );
      if ( errorFile.exists() ) {
        currentErrorFile = new FilePlayListReplayErrorFile( errorFile, file );
      } else {
        currentErrorFile = new FilePlayListReplayFile( file, AbstractFileErrorHandler.NO_PARTS );
      }
    } catch ( IOException e ) {
      throw new KettleException( e );
    }
  }
}
