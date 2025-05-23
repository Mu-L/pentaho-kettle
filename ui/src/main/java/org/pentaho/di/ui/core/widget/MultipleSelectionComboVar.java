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

package org.pentaho.di.ui.core.widget;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;

import java.util.ArrayList;
import java.util.Arrays;

import static org.pentaho.di.ui.trans.step.BaseStepDialog.getModifyListenerTooltipText;

@SuppressWarnings( "java:S110" ) // Suppress the waring about too many inheritance levels.
public class MultipleSelectionComboVar extends MultipleSelectionCombo {

  @SuppressWarnings( "java:S3008" ) // PKG is the standard name used thought out Pentaho code base
  protected static Class<?> PKG = MultipleSelectionComboVar.class; // for i18n purposes, needed by Translator2!!

  protected VariableSpace variables;

  public MultipleSelectionComboVar( Composite parent, int style, VariableSpace variableSpace ) {
    super( parent, style );
    variables = variableSpace;
    initializeParameterFunctionality();
  }

  public void initializeParameterFunctionality() {
    // Add the Diamond "S" Icon to the Text Field
    ControlDecoration controlDecoration = new ControlDecoration( getDisplayText(), SWT.CENTER | SWT.RIGHT, getTopRowComposite() );
    Image image = GUIResource.getInstance().getImageVariable();
    controlDecoration.setImage( image );
    controlDecoration.setDescriptionText( BaseMessages.getString( PKG, "TextVar.tooltip.InsertVariable" ) );
    PropsUI.getInstance().setLook( controlDecoration.getControl() );

    // Move the Add button next to Diamond "S"
    getAddButton().moveBelow( controlDecoration.getControl() );
    getArrowButton().moveBelow( getAddButton() );

    // Enable tool tips for selecting parameters
    ModifyListener modifyListenerTooltipText = getModifyListenerTooltipText( getDisplayText() );
    getDisplayText().addModifyListener( modifyListenerTooltipText );

    // Enable the display of Parameters when control space key is pressed
    ControlSpaceKeyAdapter controlSpaceKeyAdapter =
      new ControlSpaceKeyAdapter( variables, getDisplayText(), null, null );
    getDisplayText().addKeyListener( controlSpaceKeyAdapter );

    // Add a parameter field when the user selects it.
    getAddButton().addMouseListener( new MouseAdapter() {
      @Override
      public void mouseUp( MouseEvent e ) {
        super.mouseUp( e );
        String parameter = getDisplayText().getText();
        ArrayList<String> selectedItemLabels = new ArrayList<>( Arrays.asList( getSelectedItemLabels() ) );
        // If we have new parameter that does not match existing parameters
        if ( !Utils.isEmpty( parameter ) && parameter.startsWith( "$" ) && !selectedItemLabels.contains( parameter ) ) {
          selectedItemLabels.add( parameter );
          String[] arr = new String[ selectedItemLabels.size() ];
          setSelectedItemLabels( selectedItemLabels.toArray( arr ) );
          // Redisplay Items
          SelectionLabel ref = new SelectionLabel( getBottomRow(), SWT.BORDER, parameter, getExitAction() );
          updateTagsUI( calculateTotalHeight( ref )  );
          // Clear parameter from GUI
        }
        // Always clear even if the user tries to add and existing parameter
        getDisplayText().setText( "" );

      }
    } );
  }
}
