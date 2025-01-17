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

package org.pentaho.di.trans.steps.setvalueconstant;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.junit.rules.RestorePDIEngineEnvironment;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.loadsave.LoadSaveTester;
import org.pentaho.di.trans.steps.loadsave.initializer.InitializerInterface;
import org.pentaho.di.trans.steps.loadsave.validator.FieldLoadSaveValidator;
import org.pentaho.di.trans.steps.loadsave.validator.ListLoadSaveValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SetValueConstantMetaTest implements InitializerInterface<StepMetaInterface> {
  @ClassRule public static RestorePDIEngineEnvironment env = new RestorePDIEngineEnvironment();
  LoadSaveTester loadSaveTester;
  Class<SetValueConstantMeta> testMetaClass = SetValueConstantMeta.class;

  @Before
  public void setUpLoadSave() throws Exception {
    KettleEnvironment.init();
    PluginRegistry.init( false );
    List<String> attributes =
        Arrays.asList( "fields", "usevar" );

    Map<String, String> getterMap = new HashMap<String, String>() {
      {
        put( "fields", "getFields" );
        put( "usevar", "isUseVars" );
      }
    };
    Map<String, String> setterMap = new HashMap<String, String>() {
      {
        put( "fields", "setFields" );
        put( "usevar", "setUseVars" );
      }
    };

    Map<String, FieldLoadSaveValidator<?>> attrValidatorMap = new HashMap<String, FieldLoadSaveValidator<?>>();
    attrValidatorMap.put( "fields", new ListLoadSaveValidator<>( new SetValueConstantMetaFieldLoadSaveValidator(), 5 )  );
    Map<String, FieldLoadSaveValidator<?>> typeValidatorMap = new HashMap<String, FieldLoadSaveValidator<?>>();

    loadSaveTester =
        new LoadSaveTester( testMetaClass, attributes, new ArrayList<String>(), new ArrayList<String>(),
            getterMap, setterMap, attrValidatorMap, typeValidatorMap, this );
  }

  @Override
  public void modify( StepMetaInterface someMeta ) {
  }

  @Test
  public void testSerialization() throws KettleException {
    loadSaveTester.testSerialization();
  }

  public class SetValueConstantMetaFieldLoadSaveValidator implements FieldLoadSaveValidator<SetValueConstantMeta.Field> {
    final Random rand = new Random();
    @Override
    public SetValueConstantMeta.Field getTestObject() {
      SetValueConstantMeta.Field field = new SetValueConstantMeta.Field();
      field.setReplaceMask( UUID.randomUUID().toString() );
      field.setReplaceValue( UUID.randomUUID().toString() );
      field.setEmptyString( rand.nextBoolean() );
      field.setFieldName( UUID.randomUUID().toString() );
      return field;
    }

    @Override
    public boolean validateTestObject( SetValueConstantMeta.Field testObject, Object actual ) {
      if ( !( actual instanceof SetValueConstantMeta.Field) ) {
        return false;
      }
      SetValueConstantMeta.Field actualInput = (SetValueConstantMeta.Field) actual;
      return ( actualInput.equals( testObject ) );
    }
  }

}
