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


package org.pentaho.di.trans.steps.analyticquery;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Performs analytic queries (LEAD/LAG, etc) based on a group
 *
 * @author ngoodman
 * @since 27-jan-2009
 */
public class AnalyticQuery extends BaseStep implements StepInterface {
  private static Class<?> PKG = AnalyticQuery.class; // for i18n purposes, needed by Translator2!!

  private AnalyticQueryMeta meta;
  private AnalyticQueryData data;

  public AnalyticQuery( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
    Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );

    meta = (AnalyticQueryMeta) getStepMeta().getStepMetaInterface();
    data = (AnalyticQueryData) stepDataInterface;
  }

  @Override
  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    meta = (AnalyticQueryMeta) smi;
    data = (AnalyticQueryData) sdi;

    Object[] r = getRow(); // get row!

    if ( first ) {
      // What is the output looking like?
      //
      data.inputRowMeta = getInputRowMeta();

      // In case we have 0 input rows, we still want to send out a single row aggregate
      // However... the problem then is that we don't know the layout from receiving it from the previous step over the
      // row set.
      // So we need to calculated based on the metadata...
      //
      if ( data.inputRowMeta == null ) {
        data.inputRowMeta = getTransMeta().getPrevStepFields( getStepMeta() );
      }

      data.outputRowMeta = data.inputRowMeta.clone();
      meta.getFields( getTransMeta().getBowl(), data.outputRowMeta, getStepname(), null, null, this, repository,
        metaStore );

      data.groupnrs = new int[meta.getGroupField().length];
      for ( int i = 0; i < meta.getGroupField().length; i++ ) {
        data.groupnrs[i] = data.inputRowMeta.indexOfValue( meta.getGroupField()[i] );
        if ( data.groupnrs[i] < 0 ) {
          logError( BaseMessages.getString(
            PKG, "AnalyticQuery.Log.GroupFieldCouldNotFound", meta.getGroupField()[i] ) );
          setErrors( 1 );
          stopAll();
          return false;
        }
      }

      // Setup of "window size" and "queue_size"
      int max_offset = 0;
      for ( int i = 0; i < meta.getNumberOfFields(); i++ ) {
        if ( meta.getValueField()[i] > max_offset ) {
          max_offset = meta.getValueField()[i];
        }
      }
      data.window_size = max_offset;
      data.queue_size = ( max_offset * 2 ) + 1;

      // After we've processed the metadata we're all set
      first = false;

    }

    /* If our row is null we're done, clear the queue and end otherwise process the row */
    if ( r == null ) {
      clearQueue();
      setOutputDone();
      return false;
    } else {
      /* First with every group change AND the first row */
      if ( !sameGroup( this.data.previous, r ) ) {
        clearQueue();
        resetGroup();
      }
      /* Add this row to the end of the queue */
      data.data.add( r );
      /* Push the extra records off the end of the queue */
      while ( data.data.size() > data.queue_size ) {
        data.data.poll();
      }

      data.previous = r.clone();

      processQueue();
    }

    if ( log.isBasic() && checkFeedback( getLinesRead() ) ) {
      logBasic( BaseMessages.getString( PKG, "LineNr", getLinesRead() ) );
    }

    return true;
  }

  public void processQueue() throws KettleStepException {

    // If we've filled up our queue for processing
    if ( data.data.size() == data.queue_size ) {
      // Bring current cursor "up to current"
      if ( data.queue_cursor <= data.window_size ) {
        while ( data.queue_cursor <= data.window_size ) {
          processQueueObjectAt( data.queue_cursor + 1 );
          data.queue_cursor++;
        }
      } else {
        processQueueObjectAt( data.window_size + 1 );
      }
    }
  }

  public void clearQueue() throws KettleStepException {

    if ( data.data == null ) {
      return;
    }

    int number_of_rows = data.data.size();

    for ( int i = data.queue_cursor; i < number_of_rows; i++ ) {
      processQueueObjectAt( i + 1 );
    }

  }

  public void processQueueObjectAt( int i ) throws KettleStepException {
    int index = i - 1;
    Object[] rows = data.data.toArray();

    Object[] fields = new Object[meta.getNumberOfFields()];
    for ( int j = 0; j < meta.getNumberOfFields(); j++ ) {
      // field_index is the location inside a row of the subject of this
      // ie, ORDERTOTAL might be the subject ofthis field lag or lead
      // so we determine that ORDERTOTAL's index in the row
      int field_index = data.inputRowMeta.indexOfValue( meta.getSubjectField()[j] );
      int row_index = 0;
      switch ( meta.getAggregateType()[j] ) {
        case AnalyticQueryMeta.TYPE_FUNCT_LAG:
          row_index = index - meta.getValueField()[j];
          break;
        case AnalyticQueryMeta.TYPE_FUNCT_LEAD:
          row_index = index + meta.getValueField()[j];
          break;
        default:
          break;
      }
      if ( row_index < rows.length && row_index >= 0 ) {
        Object[] singleRow = (Object[]) rows[row_index];
        if ( singleRow != null && singleRow[field_index] != null ) {
          fields[j] = ( (Object[]) rows[row_index] )[field_index];
        } else {
          // set default
          fields[j] = null;
        }
      } else {
        // set default
        fields[j] = null;
      }
    }

    Object[] newRow = RowDataUtil.addRowData( (Object[]) rows[index], data.inputRowMeta.size(), fields );

    putRow( data.outputRowMeta, newRow );

  }

  public void resetGroup() {
    data.data = new ConcurrentLinkedQueue<Object[]>();
    data.queue_cursor = 0;
  }

  // Is the row r of the same group as previous?
  private boolean sameGroup( Object[] previous, Object[] r ) throws KettleValueException {
    if ( ( r == null && previous != null ) || ( previous == null && r != null ) ) {
      return false;
    } else {
      return data.inputRowMeta.compare( previous, r, data.groupnrs ) == 0;
    }

  }

  @Override
  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (AnalyticQueryMeta) smi;
    data = (AnalyticQueryData) sdi;

    if ( super.init( smi, sdi ) ) {
      return true;
    } else {
      return false;
    }

  }

}
