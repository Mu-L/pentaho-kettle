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


package org.pentaho.di.trans.steps.xmlinput;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.w3c.dom.Node;

/**
 * Describes an XML field and the position in an XML file
 *
 * @author Matt
 * @since 16-12-2005
 *
 */
public class XMLInputField implements Cloneable {
  private static Class<?> PKG = XMLInputMeta.class; // for i18n purposes, needed by Translator2!!

  public static final int TYPE_TRIM_NONE = 0;
  public static final int TYPE_TRIM_LEFT = 1;
  public static final int TYPE_TRIM_RIGHT = 2;
  public static final int TYPE_TRIM_BOTH = 3;

  public static final String[] trimTypeCode = { "none", "left", "right", "both" };

  public static final String[] trimTypeDesc = {
    BaseMessages.getString( PKG, "XMLInputField.TrimType.None" ),
    BaseMessages.getString( PKG, "XMLInputField.TrimType.Left" ),
    BaseMessages.getString( PKG, "XMLInputField.TrimType.Right" ),
    BaseMessages.getString( PKG, "XMLInputField.TrimType.Both" ) };

  public static final String POSITION_MARKER = ",";

  private String name;
  private XMLInputFieldPosition[] fieldPosition;

  private int type;
  private int length;
  private String format;
  private int trimtype;
  private int precision;
  private String currencySymbol;
  private String decimalSymbol;
  private String groupSymbol;
  private boolean repeat;

  private String[] samples;

  public XMLInputField( String fieldname, XMLInputFieldPosition[] xmlInputFieldPositions ) {
    this.name = fieldname;
    this.fieldPosition = xmlInputFieldPositions;
    this.length = -1;
    this.type = ValueMeta.TYPE_STRING;
    this.format = "";
    this.trimtype = TYPE_TRIM_NONE;
    this.groupSymbol = "";
    this.decimalSymbol = "";
    this.currencySymbol = "";
    this.precision = -1;
    this.repeat = false;
  }

  public XMLInputField() {
    this( null, null );
  }

  public String getXML() {
    String retval = "";

    retval += "      <field>" + Const.CR;
    retval += "        " + XMLHandler.addTagValue( "name", getName() );
    retval += "        " + XMLHandler.addTagValue( "type", getTypeDesc() );
    retval += "        " + XMLHandler.addTagValue( "format", getFormat() );
    retval += "        " + XMLHandler.addTagValue( "currency", getCurrencySymbol() );
    retval += "        " + XMLHandler.addTagValue( "decimal", getDecimalSymbol() );
    retval += "        " + XMLHandler.addTagValue( "group", getGroupSymbol() );
    retval += "        " + XMLHandler.addTagValue( "length", getLength() );
    retval += "        " + XMLHandler.addTagValue( "precision", getPrecision() );
    retval += "        " + XMLHandler.addTagValue( "trim_type", getTrimTypeCode() );
    retval += "        " + XMLHandler.addTagValue( "repeat", isRepeated() );

    retval += "        <positions>";
    for ( int i = 0; i < fieldPosition.length; i++ ) {
      retval += XMLHandler.addTagValue( "position", fieldPosition[i].toString(), false );
    }
    retval += "        </positions>" + Const.CR;

    retval += "        </field>" + Const.CR;

    return retval;
  }

  public XMLInputField( Node fnode ) throws KettleValueException {
    setName( XMLHandler.getTagValue( fnode, "name" ) );
    setType( ValueMeta.getType( XMLHandler.getTagValue( fnode, "type" ) ) );
    setFormat( XMLHandler.getTagValue( fnode, "format" ) );
    setCurrencySymbol( XMLHandler.getTagValue( fnode, "currency" ) );
    setDecimalSymbol( XMLHandler.getTagValue( fnode, "decimal" ) );
    setGroupSymbol( XMLHandler.getTagValue( fnode, "group" ) );
    setLength( Const.toInt( XMLHandler.getTagValue( fnode, "length" ), -1 ) );
    setPrecision( Const.toInt( XMLHandler.getTagValue( fnode, "precision" ), -1 ) );
    setTrimType( getTrimTypeByCode( XMLHandler.getTagValue( fnode, "trim_type" ) ) );
    setRepeated( !"N".equalsIgnoreCase( XMLHandler.getTagValue( fnode, "repeat" ) ) );

    Node positions = XMLHandler.getSubNode( fnode, "positions" );
    int nrPositions = XMLHandler.countNodes( positions, "position" );

    fieldPosition = new XMLInputFieldPosition[nrPositions];

    for ( int i = 0; i < nrPositions; i++ ) {
      Node positionnode = XMLHandler.getSubNodeByNr( positions, "position", i );
      String encoded = XMLHandler.getNodeValue( positionnode );
      fieldPosition[i] = new XMLInputFieldPosition( encoded );
    }
  }

  public static final int getTrimTypeByCode( String tt ) {
    if ( tt == null ) {
      return 0;
    }

    for ( int i = 0; i < trimTypeCode.length; i++ ) {
      if ( trimTypeCode[i].equalsIgnoreCase( tt ) ) {
        return i;
      }
    }
    return 0;
  }

  public static final int getTrimTypeByDesc( String tt ) {
    if ( tt == null ) {
      return 0;
    }

    for ( int i = 0; i < trimTypeDesc.length; i++ ) {
      if ( trimTypeDesc[i].equalsIgnoreCase( tt ) ) {
        return i;
      }
    }
    return 0;
  }

  public static final String getTrimTypeCode( int i ) {
    if ( i < 0 || i >= trimTypeCode.length ) {
      return trimTypeCode[0];
    }
    return trimTypeCode[i];
  }

  public static final String getTrimTypeDesc( int i ) {
    if ( i < 0 || i >= trimTypeDesc.length ) {
      return trimTypeDesc[0];
    }
    return trimTypeDesc[i];
  }

  public Object clone() {
    try {
      XMLInputField retval = (XMLInputField) super.clone();

      if ( fieldPosition != null ) {
        retval.setFieldPosition( new XMLInputFieldPosition[fieldPosition.length] );
        for ( int i = 0; i < fieldPosition.length; i++ ) {
          //CHECKSTYLE:Indentation:OFF
          retval.getFieldPosition()[i] = (XMLInputFieldPosition) fieldPosition[i].clone();
        }
      }

      return retval;
    } catch ( CloneNotSupportedException e ) {
      return null;
    }
  }

  /**
   * @return Returns the xmlInputFieldPositions.
   */
  public XMLInputFieldPosition[] getFieldPosition() {
    return fieldPosition;
  }

  /**
   * @param xmlInputFieldPositions
   *          The xmlInputFieldPositions to set.
   */
  public void setFieldPosition( XMLInputFieldPosition[] xmlInputFieldPositions ) {
    this.fieldPosition = xmlInputFieldPositions;
  }

  public int getLength() {
    return length;
  }

  public void setLength( int length ) {
    this.length = length;
  }

  public String getName() {
    return name;
  }

  public void setName( String fieldname ) {
    this.name = fieldname;
  }

  public int getType() {
    return type;
  }

  public String getTypeDesc() {
    return ValueMeta.getTypeDesc( type );
  }

  public void setType( int type ) {
    this.type = type;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat( String format ) {
    this.format = format;
  }

  public void setSamples( String[] samples ) {
    this.samples = samples;
  }

  public String[] getSamples() {
    return samples;
  }

  public int getTrimType() {
    return trimtype;
  }

  public String getTrimTypeCode() {
    return getTrimTypeCode( trimtype );
  }

  public String getTrimTypeDesc() {
    return getTrimTypeDesc( trimtype );
  }

  public void setTrimType( int trimtype ) {
    this.trimtype = trimtype;
  }

  public String getGroupSymbol() {
    return groupSymbol;
  }

  public void setGroupSymbol( String group_symbol ) {
    this.groupSymbol = group_symbol;
  }

  public String getDecimalSymbol() {
    return decimalSymbol;
  }

  public void setDecimalSymbol( String decimal_symbol ) {
    this.decimalSymbol = decimal_symbol;
  }

  public String getCurrencySymbol() {
    return currencySymbol;
  }

  public void setCurrencySymbol( String currency_symbol ) {
    this.currencySymbol = currency_symbol;
  }

  public int getPrecision() {
    return precision;
  }

  public void setPrecision( int precision ) {
    this.precision = precision;
  }

  public boolean isRepeated() {
    return repeat;
  }

  public void setRepeated( boolean repeat ) {
    this.repeat = repeat;
  }

  public void flipRepeated() {
    repeat = !repeat;
  }

  public String getFieldPositionsCode() {
    String enc = "";

    for ( int i = 0; i < fieldPosition.length; i++ ) {
      XMLInputFieldPosition pos = fieldPosition[i];
      if ( i > 0 ) {
        enc += POSITION_MARKER;
      }
      enc += pos.toString();
    }

    return enc;
  }

  public void guess() {
  }

  public void setFieldPosition( String encoded ) throws KettleException {
    try {
      String[] codes = encoded.split( POSITION_MARKER );
      fieldPosition = new XMLInputFieldPosition[codes.length];
      for ( int i = 0; i < codes.length; i++ ) {
        fieldPosition[i] = new XMLInputFieldPosition( codes[i] );
      }
    } catch ( Exception e ) {
      throw new KettleException( "Unable to parse the field positions because of an error"
        + Const.CR + "Please use E=element or A=attribute in a comma separated list (code: " + encoded + ")", e );
    }
  }
}
