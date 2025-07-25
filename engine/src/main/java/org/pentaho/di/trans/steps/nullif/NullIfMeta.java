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


package org.pentaho.di.trans.steps.nullif;

import java.util.List;

import org.pentaho.di.core.bowl.Bowl;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.injection.Injection;
import org.pentaho.di.core.injection.InjectionDeep;
import org.pentaho.di.core.injection.InjectionSupported;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBase;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/*
 * Created on 05-aug-2003
 *
 */

@InjectionSupported( localizationPrefix = "Injection.NullIf.", groups = { "FIELDS" } )
public class NullIfMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = NullIfMeta.class; // for i18n purposes, needed by Translator2!!

  public static class Field implements Cloneable {

    @Injection( name = "FIELDNAME", group = "FIELDS" )
    private String fieldName;
    @Injection( name = "FIELDVALUE", group = "FIELDS" )
    private String fieldValue;

    /**
     * @return Returns the fieldName.
     */
    public String getFieldName() {
      return fieldName;
    }

    /**
     * @param fieldName
     *          The fieldName to set.
     */
    public void setFieldName( String fieldName ) {
      this.fieldName = fieldName;
    }

    /**
     * @return Returns the fieldValue.
     */
    public String getFieldValue() {
      return fieldValue;
    }

    /**
     * @param fieldValue
     *          The fieldValue to set.
     */
    public void setFieldValue( String fieldValue ) {
      Boolean isEmptyAndNullDiffer = ValueMetaBase.convertStringToBoolean(
        Const.NVL( System.getProperty( Const.KETTLE_EMPTY_STRING_DIFFERS_FROM_NULL, "N" ), "N" ) );

      this.fieldValue = fieldValue == null && isEmptyAndNullDiffer ? Const.EMPTY_STRING : fieldValue;
    }

    public Field clone() {
      try {
        return (Field) super.clone();
      } catch ( CloneNotSupportedException e ) {
        throw new RuntimeException( e );
      }
    }
  }

  @InjectionDeep
  private Field[] fields;

  public NullIfMeta() {
    super(); // allocate BaseStepMeta
  }

  public Field[] getFields() {
    return fields;
  }

  public void setFields( Field[] fields ) {
    this.fields = fields;
  }

  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode );
  }

  public void allocate( int count ) {
    fields = new Field[count];
    for ( int i = 0; i < count; i++ ) {
      fields[i] = new Field();
    }
  }

  public Object clone() {
    NullIfMeta retval = (NullIfMeta) super.clone();

    int count = fields.length;

    retval.allocate( count );

    for ( int i = 0; i < count; i++ ) {
      retval.getFields()[i] = fields[i].clone();
    }
    return retval;
  }

  private void readData( Node stepnode ) throws KettleXMLException {
    try {
      Node fieldNodes = XMLHandler.getSubNode( stepnode, "fields" );
      int count = XMLHandler.countNodes( fieldNodes, "field" );

      allocate( count );

      for ( int i = 0; i < count; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fieldNodes, "field", i );

        fields[i].setFieldName( XMLHandler.getTagValue( fnode, "name" ) );
        fields[i].setFieldValue( XMLHandler.getTagValue( fnode, "value" ) );
      }
    } catch ( Exception e ) {
      throw new KettleXMLException( BaseMessages.getString( PKG, "NullIfMeta.Exception.UnableToReadStepInfoFromXML" ),
          e );
    }
  }

  public void setDefault() {
    int count = 0;

    allocate( count );

    for ( int i = 0; i < count; i++ ) {
      fields[i].setFieldName( "field" + i );
      fields[i].setFieldValue( "" );
    }
  }

  @Override
  public void getFields( Bowl bowl, RowMetaInterface r, String name, RowMetaInterface[] info, StepMeta nextStep,
      VariableSpace space, Repository repository, IMetaStore metaStore ) {
    if ( r == null ) {
      r = new RowMeta(); // give back values
      // Meta-data doesn't change here, only the value possibly turns to NULL
    }

    return;
  }

  public String getXML() {
    StringBuilder retval = new StringBuilder();

    retval.append( "    <fields>" + Const.CR );

    for ( int i = 0; i < fields.length; i++ ) {
      retval.append( "      <field>" + Const.CR );
      retval.append( "        " + XMLHandler.addTagValue( "name", fields[i].getFieldName() ) );
      retval.append( "        " + XMLHandler.addTagValue( "value", fields[i].getFieldValue() ) );
      retval.append( "        </field>" + Const.CR );
    }
    retval.append( "      </fields>" + Const.CR );

    return retval.toString();
  }

  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases )
    throws KettleException {
    try {
      int nrfields = rep.countNrStepAttributes( id_step, "field_name" );

      allocate( nrfields );

      for ( int i = 0; i < nrfields; i++ ) {
        fields[i].setFieldName( rep.getStepAttributeString( id_step, i, "field_name" ) );
        fields[i].setFieldValue( rep.getStepAttributeString( id_step, i, "field_value" ) );
      }
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG,
          "NullIfMeta.Exception.UnexpectedErrorReadingStepInfoFromRepository" ), e );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step )
    throws KettleException {
    try {
      for ( int i = 0; i < fields.length; i++ ) {
        rep.saveStepAttribute( id_transformation, id_step, i, "field_name", fields[i].getFieldName() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_value", fields[i].getFieldValue() );
      }
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG, "NullIfMeta.Exception.UnableToSaveStepInfoToRepository" )
          + id_step, e );
    }

  }

  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev,
      String[] input, String[] output, RowMetaInterface info, VariableSpace space, Repository repository,
      IMetaStore metaStore ) {
    CheckResult cr;
    if ( prev == null || prev.size() == 0 ) {
      cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString( PKG,
              "NullIfMeta.CheckResult.NoReceivingFieldsError" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "NullIfMeta.CheckResult.StepReceivingFieldsOK", prev.size() + "" ), stepMeta );
      remarks.add( cr );
    }

    // See if we have input streams leading to this step!
    if ( input.length > 0 ) {
      cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "NullIfMeta.CheckResult.StepRecevingInfoFromOtherSteps" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
              "NullIfMeta.CheckResult.NoInputReceivedError" ), stepMeta );
      remarks.add( cr );
    }
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
      Trans trans ) {
    return new NullIf( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  public StepDataInterface getStepData() {
    return new NullIfData();
  }

}
