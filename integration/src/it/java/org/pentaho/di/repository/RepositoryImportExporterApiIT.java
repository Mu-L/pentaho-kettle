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


package org.pentaho.di.repository;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.pentaho.di.core.ProgressMonitorListener;

public class RepositoryImportExporterApiIT extends TestCase {

  /**
   * Validate the the repository export api hasn't changed from what we use in example files.
   *
   * @see RepositoryExporter#exportAllObjects(ProgressMonitorListener, String, RepositoryDirectoryInterface, String)
   */
  public void testExportApi() throws Exception {
    Class<RepositoryExporter> exporter = RepositoryExporter.class;

    // Make sure we the expected constructor that takes a repository
    Constructor<RepositoryExporter> c = exporter.getConstructor( Repository.class );
    assertNotNull( c );

    // Make sure we have the correct signature for exporting objects
    // RepositoryExporter.exportAllObjects(ProgressMonitorListener, String, RepositoryDirectoryInterface, String)
    Class<?> param1 = ProgressMonitorListener.class;
    Class<?> param2 = String.class;
    Class<?> param3 = RepositoryDirectoryInterface.class;
    Class<?> param4 = String.class;
    Method m = exporter.getMethod( "exportAllObjects", param1, param2, param3, param4 );
    assertNotNull( m );
  }

  /**
   * Validate the the repository import api hasn't changed from what we use in example files.
   *
   * @see RepositoryImporter#importAll(RepositoryImportFeedbackInterface, String, String[],
   *      RepositoryDirectoryInterface, boolean, boolean, String)
   */
  public void testImportApi() throws Exception {
    Class<RepositoryImporter> importer = RepositoryImporter.class;

    // Make sure we the expected constructor that takes a repository
    Constructor<RepositoryImporter> c = importer.getConstructor( Repository.class );
    assertNotNull( c );

    // Make sure we have the correct signature for importing objects
    // RepositoryImporter.importAll(RepositoryImportFeedbackInterface, String, String[], RepositoryDirectoryInterface,
    // boolean, boolean, String)
    Class<?> param1 = RepositoryImportFeedbackInterface.class;
    Class<?> param2 = String.class;
    Class<?> param3 = String[].class;
    Class<?> param4 = RepositoryDirectoryInterface.class;
    Class<?> param5 = boolean.class;
    Class<?> param6 = boolean.class;
    Class<?> param7 = String.class;
    importer.getMethod( "importAll", param1, param2, param3, param4, param5, param6, param7 );
  }
}
