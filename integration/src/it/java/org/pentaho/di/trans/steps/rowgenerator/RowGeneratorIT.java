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


package org.pentaho.di.trans.steps.rowgenerator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBoolean;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.row.value.ValueMetaTimestamp;
import org.pentaho.di.trans.RowStepCollector;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.dummytrans.DummyTransMeta;

import junit.framework.TestCase;

/**
 * Test class for the RowGenerator step.
 *
 * TODO For the moment only the basic stuff is verified. Formats, lengths, precision should best also be tested.
 *
 * @author Sven Boden
 */
public class RowGeneratorIT extends TestCase {
  public RowMetaInterface createRowMetaInterface() {
    RowMetaInterface rm = new RowMeta();

    ValueMetaInterface[] valuesMeta =
    {
      new ValueMetaString( "string" ), new ValueMetaBoolean( "boolean" ),
      new ValueMetaInteger( "integer" ),
      new ValueMetaTimestamp( "timestamp" ) };

    for ( int i = 0; i < valuesMeta.length; i++ ) {
      rm.addValueMeta( valuesMeta[i] );
    }

    return rm;
  }

  public List<RowMetaAndData> createData() {
    List<RowMetaAndData> list = new ArrayList<RowMetaAndData>();

    RowMetaInterface rm = createRowMetaInterface();

    Object[] r1 =
      new Object[] {
        "string_value", Boolean.TRUE, new Long( 20L ), Timestamp.valueOf( "1970-01-01 00:00:00.000" ) };

    list.add( new RowMetaAndData( rm, r1 ) );
    list.add( new RowMetaAndData( rm, r1 ) );
    list.add( new RowMetaAndData( rm, r1 ) );

    return list;
  }

  /**
   * Check the 2 lists comparing the rows in order. If they are not the same fail the test.
   */
  public void checkRows( List<RowMetaAndData> rows1, List<RowMetaAndData> rows2 ) {
    int idx = 1;
    if ( rows1.size() != rows2.size() ) {
      fail( "Number of rows is not the same: " + rows1.size() + " and " + rows2.size() );
    }
    Iterator<RowMetaAndData> it1 = rows1.iterator();
    Iterator<RowMetaAndData> it2 = rows2.iterator();

    while ( it1.hasNext() && it2.hasNext() ) {
      RowMetaAndData rm1 = it1.next();
      RowMetaAndData rm2 = it2.next();

      Object[] r1 = rm1.getData();
      Object[] r2 = rm2.getData();

      if ( rm1.size() != rm2.size() ) {
        fail( "row nr " + idx + " is not equal" );
      }
      int[] fields = new int[rm1.size()];
      for ( int ydx = 0; ydx < rm1.size(); ydx++ ) {
        fields[ydx] = ydx;
      }
      try {
        if ( rm1.getRowMeta().compare( r1, r2, fields ) != 0 ) {
          fail( "row nr " + idx + "is not equal" );
        }
      } catch ( KettleValueException e ) {
        fail( "row nr " + idx + "is not equal" );
      }

      idx++;
    }
  }

  /**
   * Test case for Row Generator step.
   */
  public void testRowGenerator() throws Exception {
    KettleEnvironment.init();

    //
    // Create a new transformation...
    //
    TransMeta transMeta = new TransMeta();
    transMeta.setName( "row generatortest" );

    PluginRegistry registry = PluginRegistry.getInstance();

    //
    // create a row generator step...
    //
    String rowGeneratorStepname = "row generator step";
    RowGeneratorMeta rm = new RowGeneratorMeta();

    // Set the information of the row generator.
    String rowGeneratorPid = registry.getPluginId( StepPluginType.class, rm );
    StepMeta rowGeneratorStep = new StepMeta( rowGeneratorPid, rowGeneratorStepname, rm );
    transMeta.addStep( rowGeneratorStep );

    //
    // Do the following specs 3 times.
    //
    String[] fieldName = { "string", "boolean", "integer", "timestamp" };
    String[] type = { "String", "Boolean", "Integer", "Timestamp" };
    String[] value = { "string_value", "true", "20", "1970-01-01 00:00:00.000" };
    String[] fieldFormat = { "", "", "", "" };
    String[] group = { "", "", "", "" };
    String[] decimal = { "", "", "", "" };
    String[] currency = { "", "", "", "" };
    int[] intDummies = { -1, -1, -1, -1 };
    boolean[] setEmptystring = { false, false, false, false };

    rm.setDefault();
    rm.setFieldName( fieldName );
    rm.setFieldType( type );
    rm.setValue( value );
    rm.setFieldLength( intDummies );
    rm.setFieldPrecision( intDummies );
    rm.setRowLimit( "3" );
    rm.setFieldFormat( fieldFormat );
    rm.setGroup( group );
    rm.setDecimal( decimal );
    rm.setCurrency( currency );
    rm.setEmptyString( setEmptystring );

    //
    // Create a dummy step
    //
    String dummyStepname = "dummy step";
    DummyTransMeta dm = new DummyTransMeta();

    String dummyPid = registry.getPluginId( StepPluginType.class, dm );
    StepMeta dummyStep = new StepMeta( dummyPid, dummyStepname, dm );
    transMeta.addStep( dummyStep );

    TransHopMeta hi = new TransHopMeta( rowGeneratorStep, dummyStep );
    transMeta.addTransHop( hi );

    // Now execute the transformation
    Trans trans = new Trans( transMeta );

    trans.prepareExecution( null );

    StepInterface si = trans.getStepInterface( dummyStepname, 0 );
    RowStepCollector rc = new RowStepCollector();
    si.addRowListener( rc );

    trans.startThreads();
    trans.waitUntilFinished();

    List<RowMetaAndData> checkList = createData();
    List<RowMetaAndData> resultRows = rc.getRowsWritten();
    checkRows( resultRows, checkList );
  }
}
