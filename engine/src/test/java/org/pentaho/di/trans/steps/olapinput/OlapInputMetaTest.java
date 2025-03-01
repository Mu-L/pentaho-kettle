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


package org.pentaho.di.trans.steps.olapinput;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.junit.rules.RestorePDIEngineEnvironment;
import org.pentaho.di.trans.steps.loadsave.LoadSaveTester;
import org.w3c.dom.Node;

public class OlapInputMetaTest {
  @ClassRule public static RestorePDIEngineEnvironment env = new RestorePDIEngineEnvironment();

  @BeforeClass
  public static void setUpBeforeClass() throws KettleException {
    KettleEnvironment.init( false );
  }

  @Test
  public void testRoundTrip() throws KettleException {
    List<String> attributes = Arrays.asList( "url", "username", "password", "mdx", "catalog", "variables_active" );

    Map<String, String> getterMap = new HashMap<String, String>();
    getterMap.put( "url", "getOlap4jUrl" );
    getterMap.put( "username", "getUsername" );
    getterMap.put( "password", "getPassword" );
    getterMap.put( "mdx", "getMdx" );
    getterMap.put( "catalog", "getCatalog" );
    getterMap.put( "variables_active", "isVariableReplacementActive" );

    Map<String, String> setterMap = new HashMap<String, String>();
    setterMap.put( "url", "setOlap4jUrl" );
    setterMap.put( "username", "setUsername" );
    setterMap.put( "password", "setPassword" );
    setterMap.put( "mdx", "setMdx" );
    setterMap.put( "catalog", "setCatalog" );
    setterMap.put( "variables_active", "setVariableReplacementActive" );

    LoadSaveTester loadSaveTester = new LoadSaveTester( OlapInputMeta.class, attributes, getterMap, setterMap );

    loadSaveTester.testSerialization();
  }

  @Test
  public void checkPasswordEncrypted() throws KettleXMLException {
    OlapInputMeta meta = new OlapInputMeta();
    meta.setPassword( "qwerty" );
    Node stepXML = XMLHandler.getSubNode(
      XMLHandler.loadXMLString( "<step>" + meta.getXML() + "</step>" ), "step" );
    assertFalse( "qwerty".equals( XMLHandler.getTagValue( stepXML, "password" ) ) );
  }
}
