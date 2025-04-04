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


package org.pentaho.di.trans.steps.fileinput.text;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.trans.steps.file.BaseFileField;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TextFileInputUtilsTest {
  @Test
  public void guessStringsFromLine() throws Exception {
    TextFileInputMeta inputMeta = Mockito.mock( TextFileInputMeta.class );
    inputMeta.content = new TextFileInputMeta.Content();
    inputMeta.content.fileType = "CSV";

    String line = "\"\\\\valueA\"|\"valueB\\\\\"|\"val\\\\ueC\""; // "\\valueA"|"valueB\\"|"val\\ueC"

    String[] strings = TextFileInputUtils
      .guessStringsFromLine( Mockito.mock( VariableSpace.class ), Mockito.mock( LogChannelInterface.class ),
        line, inputMeta, "|", "\"", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "\\valueA", strings[ 0 ] );
    Assert.assertEquals( "valueB\\", strings[ 1 ] );
    Assert.assertEquals( "val\\ueC", strings[ 2 ] );
  }

  @Test
  public void convertLineToStrings() throws Exception {
    TextFileInputMeta inputMeta = Mockito.mock( TextFileInputMeta.class );
    inputMeta.content = new TextFileInputMeta.Content();
    inputMeta.content.fileType = "CSV";
    inputMeta.inputFields = new BaseFileField[ 3 ];
    inputMeta.content.escapeCharacter = "\\";

    String line = "\"\\\\fie\\\\l\\dA\"|\"fieldB\\\\\"|\"fie\\\\ldC\""; // ""\\fie\\l\dA"|"fieldB\\"|"Fie\\ldC""

    String[] strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, "|", "\"", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "\\fie\\l\\dA", strings[ 0 ] );
    Assert.assertEquals( "fieldB\\", strings[ 1 ] );
    Assert.assertEquals( "fie\\ldC", strings[ 2 ] );
  }

  @Test
  public void convertCSVLinesToStrings() throws Exception {
    TextFileInputMeta inputMeta = Mockito.mock( TextFileInputMeta.class );
    inputMeta.content = new TextFileInputMeta.Content();
    inputMeta.content.fileType = "CSV";
    inputMeta.inputFields = new BaseFileField[ 2 ];
    inputMeta.content.escapeCharacter = "\\";

    String line = "A\\\\,B"; // A\\,B

    String[] strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "A\\", strings[ 0 ] );
    Assert.assertEquals( "B", strings[ 1 ] );

    line = "\\,AB"; // \,AB

    strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( ",AB", strings[ 0 ] );
    Assert.assertEquals( null, strings[ 1 ] );

    line = "\\\\\\,AB"; // \\\,AB

    strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "\\,AB", strings[ 0 ] );
    Assert.assertEquals( null, strings[ 1 ] );

    line = "AB,\\"; // AB,\

    strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "AB", strings[ 0 ] );
    Assert.assertEquals( "\\", strings[ 1 ] );

    line = "AB,\\\\\\"; // AB,\\\

    strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "AB", strings[ 0 ] );
    Assert.assertEquals( "\\\\", strings[ 1 ] );

    line = "A\\B,C"; // A\B,C

    strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "A\\B", strings[ 0 ] );
    Assert.assertEquals( "C", strings[ 1 ] );
  }

  @Test
  public void convertCSVLinesToStringsWithEnclosure() throws Exception {
    TextFileInputMeta inputMeta = Mockito.mock( TextFileInputMeta.class );
    inputMeta.content = new TextFileInputMeta.Content();
    inputMeta.content.fileType = "CSV";
    inputMeta.inputFields = new BaseFileField[ 2 ];
    inputMeta.content.escapeCharacter = "\\";
    inputMeta.content.enclosure = "\"";

    String line = "\"A\\\\\",\"B\""; // "A\\","B"

    String[] strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "\"", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "A\\", strings[ 0 ] );
    Assert.assertEquals( "B", strings[ 1 ] );

    line = "\"\\\\\",\"AB\""; // "\\","AB"

    strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "\"", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "\\", strings[ 0 ] );
    Assert.assertEquals( "AB", strings[ 1 ] );

    line = "\"A\\B\",\"C\""; // "A\B","C"

    strings = TextFileInputUtils
      .convertLineToStrings( Mockito.mock( LogChannelInterface.class ), line, inputMeta, ",", "\"", "\\" );
    Assert.assertNotNull( strings );
    Assert.assertEquals( "A\\B", strings[ 0 ] );
    Assert.assertEquals( "C", strings[ 1 ] );
  }

  @Test
  public void getLineWithEnclosureTest() throws Exception {
    String text = "\"firstLine\"\n\"secondLine\"";
    StringBuilder linebuilder = new StringBuilder( "" );
    InputStream is = new ByteArrayInputStream( text.getBytes() );
    BufferedInputStreamReader isr = new BufferedInputStreamReader( new InputStreamReader( is ) );
    TextFileLine line = TextFileInputUtils.getLine( Mockito.mock( LogChannelInterface.class ), isr, EncodingType.SINGLE, 1, linebuilder, "\"", "", 0 );
    Assert.assertEquals( "\"firstLine\"", line.getLine() );
  }

  @Test
  public void getLineBrokenByEnclosureTest() throws Exception {
    String text = "\"firstLine\n\"\"secondLine\"";
    StringBuilder linebuilder = new StringBuilder( "" );
    InputStream is = new ByteArrayInputStream( text.getBytes() );
    BufferedInputStreamReader isr = new BufferedInputStreamReader( new InputStreamReader( is ) );
    TextFileLine line = TextFileInputUtils.getLine( Mockito.mock( LogChannelInterface.class ), isr, EncodingType.SINGLE, 1, linebuilder, "\"", "", 0 );
    Assert.assertEquals( text, line.getLine() );
  }

  @Test
  public void getLineBrokenByEnclosureLenientTest() throws Exception {
    System.setProperty( "KETTLE_COMPATIBILITY_TEXT_FILE_INPUT_USE_LENIENT_ENCLOSURE_HANDLING", "Y" );
    String text = "\"firstLine\n\"\"secondLine\"";
    StringBuilder linebuilder = new StringBuilder( "" );
    InputStream is = new ByteArrayInputStream( text.getBytes() );
    BufferedInputStreamReader isr = new BufferedInputStreamReader( new InputStreamReader( is ) );
    TextFileLine line = TextFileInputUtils.getLine( Mockito.mock( LogChannelInterface.class ), isr, EncodingType.SINGLE, 1, linebuilder, "\"", "", 0 );
    Assert.assertEquals( "\"firstLine", line.getLine() );
    System.clearProperty( "KETTLE_COMPATIBILITY_TEXT_FILE_INPUT_USE_LENIENT_ENCLOSURE_HANDLING" );
  }

  @Test
  public void testCheckPattern() {
    // Check more information in:
    // https://docs.oracle.com/javase/tutorial/essential/regex/literals.html
    String metacharacters = "<([{\\^-=$!|]})?*+.>";
    for( int i = 0; i < metacharacters.length(); i++ ) {
      int matches = TextFileInputUtils.checkPattern( metacharacters, String.valueOf( metacharacters.charAt( i ) ), null );
      Assert.assertEquals( 1, matches );
    }
  }

  @Test
  public void testCheckPatternWithEscapeCharacter() {
    List<String> texts = new ArrayList<>();
    texts.add( "\"valueA\"|\"valueB\\\\\"|\"valueC\"" );
    texts.add( "\"valueA\"|\"va\\\"lueB\"|\"valueC\"" );

    for ( String text : texts ) {
      int matches = TextFileInputUtils.checkPattern( text, "\"", "\\" );
      Assert.assertEquals( 6, matches );
    }

  }

  @Test
  public void convertCSVLinesToStringsWithSameEnclosureAndEscape() throws Exception {
    TextFileInputMeta inputMeta = Mockito.mock(TextFileInputMeta.class);
    inputMeta.content = new TextFileInputMeta.Content();
    inputMeta.content.fileType = "CSV";
    inputMeta.inputFields = new BaseFileField[2];
    inputMeta.content.escapeCharacter = "\"";
    inputMeta.content.enclosure = "\"";
    //the escape character is same as enclosure
    String line = "\"\"\"\"\"\""; // """"""

    String[] strings = TextFileInputUtils
            .convertLineToStrings(Mockito.mock(LogChannelInterface.class), line, inputMeta, ",", "\"", "\"");
    Assert.assertNotNull(strings);
    Assert.assertEquals("\"\"", strings[0]);//""""

    line = "\"{\"\"Example1\"\":\"\"\"\",\"\"Example\"\":\"\"Test\"\"}\""; // """"""

    strings = TextFileInputUtils
            .convertLineToStrings(Mockito.mock(LogChannelInterface.class), line, inputMeta, ",", "\"", "\"");
    Assert.assertNotNull(strings);
    Assert.assertEquals("{\"Example1\":\"\",\"Example\":\"Test\"}", strings[0]);//""""
  }
}
