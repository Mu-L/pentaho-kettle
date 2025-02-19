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


package org.pentaho.libformula.editor.function;

import org.pentaho.di.core.xml.XMLHandler;
import org.w3c.dom.Node;

/**
 * Describes a lib formula function example in a generic way.
 *
 * @author matt
 *
 *         <pre>
 *    <example><expression>"Hi " &amp; "there"</expression> <result>"Hi there"</result>
 *      <level>1</level> <comment>Simple concatenation.</comment></example>
 *    <example><expression>"H" &amp; ""</expression> <result>"H"</result>
 *      <level>1</level> <comment>Concatenating an empty string produces no change.</comment></example>
 *    <example><expression>-5&amp;"b"</expression> <result>-5b</result>
 *      <level>1</level> <comment>Unary - has higher precedence than &amp;</comment></example>
 *    <example><expression>3&amp;2-1</expression> <result>31</result>
 *      <level>1</level> <comment>Binary - has higher precedence than &amp;</comment></example>
 * </pre>
 */
public class FunctionExample {
  public static final String XML_TAG = "example";

  private String expression;
  private String result;
  private String level;
  private String comment;

  public FunctionExample( String expression, String result, String level, String comment ) {
    this.expression = expression;
    this.result = result;
    this.level = level;
    this.comment = comment;
  }

  public FunctionExample( Node node ) {
    this.expression = XMLHandler.getTagValue( node, "expression" );
    this.result = XMLHandler.getTagValue( node, "result" );
    this.level = XMLHandler.getTagValue( node, "level" );
    this.comment = XMLHandler.getTagValue( node, "comment" );
  }

  /**
   * @return the expression
   */
  public String getExpression() {
    return expression;
  }

  /**
   * @param expression
   *          the expression to set
   */
  public void setExpression( String expression ) {
    this.expression = expression;
  }

  /**
   * @return the result
   */
  public String getResult() {
    return result;
  }

  /**
   * @param result
   *          the result to set
   */
  public void setResult( String result ) {
    this.result = result;
  }

  /**
   * @return the level
   */
  public String getLevel() {
    return level;
  }

  /**
   * @param level
   *          the level to set
   */
  public void setLevel( String level ) {
    this.level = level;
  }

  /**
   * @return the comment
   */
  public String getComment() {
    return comment;
  }

  /**
   * @param comment
   *          the comment to set
   */
  public void setComment( String comment ) {
    this.comment = comment;
  }

}
