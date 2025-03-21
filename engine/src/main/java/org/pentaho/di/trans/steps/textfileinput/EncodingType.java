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


package org.pentaho.di.trans.steps.textfileinput;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.ByteOrderMark;
import org.pentaho.di.core.util.Utils;

/**
 * @deprecated replaced by implementation in the ...steps.fileinput.text package
 */
@Deprecated
public enum EncodingType {
  SINGLE( 1, 0, '\r', '\n' ), DOUBLE_BIG_ENDIAN( 2, 0xFEFF, 0x000d, 0x000a ), DOUBLE_LITTLE_ENDIAN(
    2, 0xFFFE, 0x0d00, 0x0a00 );

  private static final String UTF_8_BOM = new String( ByteOrderMark.UTF_8.getBytes(), StandardCharsets.UTF_8 );

  private int length;

  /**
   * Byte Order Mark (BOM): http://en.wikipedia.org/wiki/Byte_Order_Mark
   */
  private int bom;
  private int carriageReturnChar;
  private int lineFeedChar;

  /**
   * @param length
   * @param bom
   */
  private EncodingType( int length, int bom, int carriageReturnChar, int lineFeedChar ) {
    this.length = length;
    this.bom = bom;
    this.carriageReturnChar = carriageReturnChar;
    this.lineFeedChar = lineFeedChar;
  }

  public int getLength() {
    return length;
  }

  public int getBom() {
    return bom;
  }

  public int getCarriageReturnChar() {
    return carriageReturnChar;
  }

  public int getLineFeedChar() {
    return lineFeedChar;
  }

  public boolean isReturn( int c ) {
    return c == carriageReturnChar || c == '\r';
  }

  public boolean isLinefeed( int c ) {
    return c == lineFeedChar || c == '\n';
  }

  public static EncodingType guessEncodingType( String encoding ) {

    EncodingType encodingType;

    if ( Utils.isEmpty( encoding ) ) {
      encodingType = EncodingType.SINGLE;
    } else if ( encoding.startsWith( "UnicodeBig" ) || encoding.equals( "UTF-16BE" ) ) {
      encodingType = EncodingType.DOUBLE_BIG_ENDIAN;
    } else if ( encoding.startsWith( "UnicodeLittle" ) || encoding.equals( "UTF-16LE" ) ) {
      encodingType = EncodingType.DOUBLE_LITTLE_ENDIAN;
    } else if ( encoding.equals( "UTF-16" ) ) {
      encodingType = EncodingType.DOUBLE_BIG_ENDIAN; // The default, no BOM
    } else {
      encodingType = EncodingType.SINGLE;
    }

    return encodingType;
  }

  public static String removeBOMIfPresent( String string ) {
    if ( string == null ) {
      return null;
    }
    return string.replaceFirst( UTF_8_BOM, "" );
  }

  public byte[] getBytes( String string, String encoding ) throws UnsupportedEncodingException {
    byte[] withBom;
    if ( Utils.isEmpty( encoding ) ) {
      withBom = string.getBytes();
    } else {
      withBom = string.getBytes( encoding );
    }

    switch ( length ) {
      case 1:
        return withBom;
      case 2:
        if ( withBom.length < 2 ) {
          return withBom;
        }
        if ( withBom[0] < 0 && withBom[1] < 0 ) {
          byte[] b = new byte[withBom.length - 2];
          for ( int i = 0; i < withBom.length - 2; i++ ) {
            b[i] = withBom[i + 2];
          }
          return b;
        } else {
          return withBom;
        }
      default:
        return withBom;
    }
  }
}
