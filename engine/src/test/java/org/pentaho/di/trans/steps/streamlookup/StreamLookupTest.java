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


package org.pentaho.di.trans.steps.streamlookup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.bowl.Bowl;
import org.pentaho.di.core.QueueRowSet;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.step.StepIOMeta;
import org.pentaho.di.trans.step.StepIOMetaInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.errorhandling.Stream;
import org.pentaho.di.trans.step.errorhandling.StreamIcon;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.di.trans.steps.mock.StepMockHelper;
import org.pentaho.metastore.api.IMetaStore;

import java.util.Collections;
import java.util.HashMap;

/**
 * Test for StreamLookup step
 *
 * @author Pavel Sakun
 * @see StreamLookup
 */
public class StreamLookupTest {
  private StepMockHelper<StreamLookupMeta, StreamLookupData> smh;

  @Before
  public void setUp() {
    smh =
      new StepMockHelper<StreamLookupMeta, StreamLookupData>( "StreamLookup", StreamLookupMeta.class,
        StreamLookupData.class );
    when( smh.logChannelInterfaceFactory.create( any(), any( LoggingObjectInterface.class ) ) ).thenReturn(
      smh.logChannelInterface );
    when( smh.trans.isRunning() ).thenReturn( true );
  }

  @After
  public void cleanUp() {
    smh.cleanUp();
  }

  private void convertDataToBinary( Object[][] data ) {
    for ( int i = 0; i < data.length; i++ ) {
      for ( int j = 0; j < data[i].length; j++ ) {
        data[i][j] = ( (String) data[i][j] ).getBytes();
      }
    }
  }

  private RowSet mockLookupRowSet( boolean binary ) {
    final int storageType = binary ? ValueMetaInterface.STORAGE_TYPE_BINARY_STRING : ValueMetaInterface.STORAGE_TYPE_NORMAL;
    Object[][] data = { { "Value1", "1" }, { "Value2", "2" } };

    if ( binary ) {
      convertDataToBinary( data );
    }

    RowSet lookupRowSet =
      smh.getMockInputRowSet( data );
    doReturn( "Lookup" ).when( lookupRowSet ).getOriginStepName();
    doReturn( "StreamLookup" ).when( lookupRowSet ).getDestinationStepName();

    RowMeta lookupRowMeta = new RowMeta();
    ValueMetaString valueMeta = new ValueMetaString( "Value" );
    valueMeta.setStorageType( storageType );
    valueMeta.setStorageMetadata( new ValueMetaString() );
    lookupRowMeta.addValueMeta( valueMeta );
    ValueMetaString idMeta = new ValueMetaString( "Id" );
    idMeta.setStorageType( storageType );
    idMeta.setStorageMetadata( new ValueMetaString() );
    lookupRowMeta.addValueMeta( idMeta );

    doReturn( lookupRowMeta ).when( lookupRowSet ).getRowMeta();

    return lookupRowSet;
  }

  private RowSet mockDataRowSet( boolean binary ) {
    final int storageType = binary ? ValueMetaInterface.STORAGE_TYPE_BINARY_STRING : ValueMetaInterface.STORAGE_TYPE_NORMAL;
    Object[][] data = { { "Name1", "1" }, { "Name2", "2" } };

    if ( binary ) {
      convertDataToBinary( data );
    }

    RowSet dataRowSet = smh.getMockInputRowSet( data );

    RowMeta dataRowMeta = new RowMeta();
    ValueMetaString valueMeta = new ValueMetaString( "Name" );
    valueMeta.setStorageType( storageType );
    valueMeta.setStorageMetadata( new ValueMetaString() );
    dataRowMeta.addValueMeta( valueMeta );
    ValueMetaString idMeta = new ValueMetaString( "Id" );
    idMeta.setStorageType( storageType );
    idMeta.setStorageMetadata( new ValueMetaString() );
    dataRowMeta.addValueMeta( idMeta );

    doReturn( dataRowMeta ).when( dataRowSet ).getRowMeta();

    return dataRowSet;
  }

  private StreamLookupMeta mockProcessRowMeta( boolean memoryPreservationActive ) throws KettleStepException {
    StreamLookupMeta meta = smh.processRowsStepMetaInterface;

    StepMeta lookupStepMeta = when( mock( StepMeta.class ).getName() ).thenReturn( "Lookup" ).getMock();
    doReturn( lookupStepMeta ).when( smh.transMeta ).findStep( "Lookup" );

    StepIOMeta stepIOMeta = new StepIOMeta( true, true, false, false, false, false );
    stepIOMeta.addStream( new Stream( StreamInterface.StreamType.INFO, lookupStepMeta, null, StreamIcon.INFO, null ) );

    doReturn( stepIOMeta ).when( meta ).getStepIOMeta();
    doReturn( new String[] { "Id" } ).when( meta ).getKeylookup();
    doReturn( new String[] { "Id" } ).when( meta ).getKeystream();
    doReturn( new String[] { "Value" } ).when( meta ).getValue();
    doReturn( memoryPreservationActive ).when( meta ).isMemoryPreservationActive();
    doReturn( false ).when( meta ).isUsingSortedList();
    doReturn( false ).when( meta ).isUsingIntegerPair();
    doReturn( new int[] { -1 } ).when( meta ).getValueDefaultType();
    doReturn( new String[] { "" } ).when( meta ).getValueDefault();
    doReturn( new String[] { "Value" } ).when( meta ).getValueName();
    doReturn( new String[] { "Value" } ).when( meta ).getValue();
    doCallRealMethod().when( meta ).getFields( any( Bowl.class ), any( RowMetaInterface.class ), anyString(),
      any( RowMetaInterface[].class ), any( StepMeta.class ), any( VariableSpace.class ), any( Repository.class ),
      any( IMetaStore.class ) );

    return meta;
  }

  private void doTest( boolean memoryPreservationActive, boolean binaryLookupStream, boolean binaryDataStream ) throws KettleException {
    StreamLookup step = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );
    step.init( smh.initStepMetaInterface, smh.initStepDataInterface );
    step.addRowSetToInputRowSets( mockLookupRowSet( binaryLookupStream ) );
    step.addRowSetToInputRowSets( mockDataRowSet( binaryDataStream ) );
    step.addRowSetToOutputRowSets( new QueueRowSet() );

    StreamLookupMeta meta = mockProcessRowMeta( memoryPreservationActive );
    StreamLookupData data = new StreamLookupData();
    data.readLookupValues = true;

    RowSet outputRowSet = step.getOutputRowSets().get( 0 );

    // Process rows and collect output
    int rowNumber = 0;
    String[] expectedOutput = { "Name", "", "Value" };
    while ( step.processRow( meta, data ) ) {
      Object[] rowData = outputRowSet.getRow();
      if ( rowData != null ) {
        RowMetaInterface rowMeta = outputRowSet.getRowMeta();
        Assert.assertEquals( "Output row is of wrong size", 2, rowMeta.size() );
        rowNumber++;
        // Verify output
        for ( int valueIndex = 0; valueIndex < rowMeta.size(); valueIndex++ ) {
          String expectedValue = expectedOutput[valueIndex] + rowNumber;
          Object actualValue = rowMeta.getValueMeta( valueIndex ).convertToNormalStorageType( rowData[valueIndex] );
          Assert.assertEquals( "Unexpected value at row " + rowNumber + " position " + valueIndex, expectedValue,
            actualValue );
        }
      }
    }

    Assert.assertEquals( "Incorrect output row number", 2, rowNumber );
  }

  @Test
  public void testWithNormalStreams() throws KettleException {
    doTest( false, false, false );
  }

  @Test
  public void testWithBinaryLookupStream() throws KettleException {
    doTest( false, true, false );
  }

  @Test
  public void testWithBinaryDateStream() throws KettleException {
    doTest( false, false, true );
  }

  @Test
  public void testWithBinaryStreams() throws KettleException {
    doTest( false, false, true );
  }

  @Test
  public void testMemoryPreservationWithNormalStreams() throws KettleException {
    doTest( true, false, false );
  }

  @Test
  public void testMemoryPreservationWithBinaryLookupStream() throws KettleException {
    doTest( true, true, false );
  }

  @Test
  public void testMemoryPreservationWithBinaryDateStream() throws KettleException {
    doTest( true, false, true );
  }

  @Test
  public void testMemoryPreservationWithBinaryStreams() throws KettleException {
    doTest( true, false, true );
  }

  @Test
  public void lookupFieldsTest() throws KettleStepException {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );
    RowMeta rowMeta = new RowMeta();
    ValueMetaString valueMetaString = new ValueMetaString( "value" );
    rowMeta.setValueMetaList( Collections.singletonList( valueMetaString ) );

    Stream stream = mock( Stream.class );
    StepMeta stepMeta = mock( StepMeta.class );
    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );

    when( streamLookupMeta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    when( stream.getStepname() ).thenReturn( "stepName" );
    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( Collections.singletonList( stream ) );
    when( smh.transMeta.findStep( "stepName" ) ).thenReturn( stepMeta );
    when( smh.transMeta.getStepFields( stepMeta ) ).thenReturn( rowMeta );

    JSONObject jsonObject = streamLookup.doAction( "lookupFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.SUCCESS_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
    JSONArray jsonArray = (JSONArray) jsonObject.get( "lookupFields" );
    assertEquals( 1, jsonArray.size() );
  }

  @Test
  public void lookupFieldsTest_withEmptyStepName() {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );

    Stream stream = mock( Stream.class );
    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );

    when( streamLookupMeta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    when( stream.getStepname() ).thenReturn( "" );
    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( Collections.singletonList( stream ) );

    JSONObject jsonObject = streamLookup.doAction( "lookupFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.FAILURE_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
    String errorMessage = (String) jsonObject.get( "errorMessage" );
    assertNotNull( errorMessage );
  }

  @Test
  public void lookupFieldsTest_throwsExceptionWhileFetchingFields() throws KettleStepException {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );

    Stream stream = mock( Stream.class );
    StepMeta stepMeta = mock( StepMeta.class );
    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );

    when( streamLookupMeta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    when( stream.getStepname() ).thenReturn( "stepName" );
    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( Collections.singletonList( stream ) );
    when( smh.transMeta.findStep( "stepName" ) ).thenReturn( stepMeta );
    when( smh.transMeta.getStepFields( stepMeta ) ).thenThrow( new KettleStepException( "errorMessage" ) );

    JSONObject jsonObject = streamLookup.doAction( "lookupFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.FAILURE_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
  }

  @Test
  public void getFieldsTest_fromPrevFields() throws KettleStepException {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );
    RowMeta rowMeta = new RowMeta();
    ValueMetaString valueMetaString = new ValueMetaString( "value" );
    rowMeta.setValueMetaList( Collections.singletonList( valueMetaString ) );

    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepMeta stepMeta = mock( StepMeta.class );
    when( streamLookupMeta.getParentStepMeta() ).thenReturn( stepMeta );
    when( stepMeta.getName() ).thenReturn( "stepName" );
    when( smh.transMeta.getPrevStepFields( "stepName" ) ).thenReturn( rowMeta );

    JSONObject jsonObject = streamLookup.doAction( "getFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.SUCCESS_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
    JSONArray jsonArray = (JSONArray) jsonObject.get( "fields" );
    assertEquals( 1, jsonArray.size() );
  }

  @Test
  public void getFieldsTest_fromStream() throws KettleStepException {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );
    RowMeta rowMeta = new RowMeta();
    ValueMetaString valueMetaString = new ValueMetaString( "value" );
    rowMeta.setValueMetaList( Collections.singletonList( valueMetaString ) );

    Stream stream = mock( Stream.class );
    StepMeta stepMeta = mock( StepMeta.class );
    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );

    when( streamLookupMeta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    when( stream.getStepname() ).thenReturn( "stepName" );
    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( Collections.singletonList( stream ) );

    when( streamLookupMeta.getParentStepMeta() ).thenReturn( stepMeta );
    when( stepMeta.getName() ).thenReturn( "stepName" );
    when( smh.transMeta.getPrevStepFields( "stepName" ) ).thenReturn( null );
    when( smh.transMeta.findStep( "stepName" ) ).thenReturn( stepMeta );
    when( smh.transMeta.getStepFields( "stepName" ) ).thenReturn( rowMeta );

    JSONObject jsonObject = streamLookup.doAction( "getFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.SUCCESS_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
    JSONArray jsonArray = (JSONArray) jsonObject.get( "fields" );
    assertEquals( 1, jsonArray.size() );
  }

  @Test
  public void getFieldsTest_fromStreamWithEmptyStepName() throws KettleStepException {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );

    Stream stream = mock( Stream.class );
    StepMeta stepMeta = mock( StepMeta.class );
    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );

    when( streamLookupMeta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    when( stream.getStepname() ).thenReturn( "" );
    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( Collections.singletonList( stream ) );

    when( streamLookupMeta.getParentStepMeta() ).thenReturn( stepMeta );
    when( stepMeta.getName() ).thenReturn( "stepName" );
    when( smh.transMeta.getPrevStepFields( "stepName" ) ).thenReturn( null );
    when( smh.transMeta.findStep( "stepName" ) ).thenReturn( stepMeta );

    JSONObject jsonObject = streamLookup.doAction( "getFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.FAILURE_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
    String errorMessage = (String) jsonObject.get( "errorMessage" );
    assertNotNull( errorMessage );
  }

  @Test
  public void getFieldsTest_fromStreamWithEmptyFields() throws KettleStepException {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );

    Stream stream = mock( Stream.class );
    StepMeta stepMeta = mock( StepMeta.class );
    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );

    when( streamLookupMeta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    when( stream.getStepname() ).thenReturn( "stepName" );
    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( Collections.singletonList( stream ) );

    when( streamLookupMeta.getParentStepMeta() ).thenReturn( stepMeta );
    when( stepMeta.getName() ).thenReturn( "stepName" );
    when( smh.transMeta.getPrevStepFields( "stepName" ) ).thenReturn( null );
    when( smh.transMeta.findStep( "stepName" ) ).thenReturn( stepMeta );
    when( smh.transMeta.getStepFields( "stepName" ) ).thenReturn( null );

    JSONObject jsonObject = streamLookup.doAction( "getFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.FAILURE_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
    String errorMessage = (String) jsonObject.get( "errorMessage" );
    assertNotNull( errorMessage );
  }

  @Test
  public void getFieldsTest_fromStreamThrowsExceptionWhenFetchingFields() throws KettleStepException {
    StreamLookup streamLookup = new StreamLookup( smh.stepMeta, smh.stepDataInterface, 0, smh.transMeta, smh.trans );

    Stream stream = mock( Stream.class );
    StepMeta stepMeta = mock( StepMeta.class );
    StreamLookupMeta streamLookupMeta = mock( StreamLookupMeta.class );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );

    when( streamLookupMeta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    when( stream.getStepname() ).thenReturn( "stepName" );
    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( Collections.singletonList( stream ) );

    when( streamLookupMeta.getParentStepMeta() ).thenReturn( stepMeta );
    when( stepMeta.getName() ).thenReturn( "stepName" );
    when( smh.transMeta.getPrevStepFields( "stepName" ) ).thenReturn( null );
    when( smh.transMeta.findStep( "stepName" ) ).thenReturn( stepMeta );
    when( smh.transMeta.getStepFields( "stepName" ) ).thenThrow( new KettleStepException( "errorMessage" ) );

    JSONObject jsonObject = streamLookup.doAction( "getFields", streamLookupMeta,
        smh.transMeta, smh.trans, new HashMap<>() );

    assertNotNull( jsonObject );
    assertEquals( StepInterface.FAILURE_RESPONSE, jsonObject.get( StepInterface.ACTION_STATUS ) );
    String errorMessage = (String) jsonObject.get( "errorMessage" );
    assertNotNull( errorMessage );
  }
}
