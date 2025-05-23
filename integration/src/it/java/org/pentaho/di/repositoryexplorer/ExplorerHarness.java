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


package org.pentaho.di.repositoryexplorer;

import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;
import org.pentaho.di.repository.kdr.KettleDatabaseRepositoryMeta;
import org.pentaho.di.ui.repository.repositoryexplorer.RepositoryExplorerCallback;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryContent;

public class ExplorerHarness {

  /**
   * @param args
   */
  @SuppressWarnings( "nls" )
  public static void main( String[] args ) {
    KettleDatabaseRepositoryMeta repositoryMeta;
    KettleDatabaseRepository repository;
    @SuppressWarnings( "unused" )
    UserInfo userInfo;

    repositoryMeta = new KettleDatabaseRepositoryMeta();
    repositoryMeta.setName( "Kettle Database Repository" );
    repositoryMeta.setDescription( "Kettle database test repository" );

    DatabaseMeta connection = new DatabaseMeta();
    connection.setDatabaseType( "Hypersonic" );
    connection.setHostname( "localhost" );
    connection.setDBName( "kettle_repository_4x" );
    connection.setDBPort( "9002" );
    connection.setUsername( "sa" );

    repositoryMeta.setConnection( connection );

    userInfo = new UserInfo( "admin", "admin", "Administrator", "The system administrator", true );

    repository = new KettleDatabaseRepository();
    repository.init( repositoryMeta );

    @SuppressWarnings( "unused" )
    RepositoryExplorerCallback cb = new RepositoryExplorerCallback() {

      public boolean open( UIRepositoryContent element, String revision ) throws Exception {
        System.out.println( "Name: ".concat( element.getName() ) );
        System.out.println( "Type: ".concat( element.getRepositoryElementType().name() ) );
        System.out.println( "Directory: ".concat( element.getRepositoryDirectory().toString() ) );
        System.out.println( "Revision: ".concat( revision == null ? "null" : revision ) );
        return false; // do not close explorer
      }
      
      @Override
      public boolean error ( String message ) throws Exception {
        System.out.println( "Error message: ".concat( message ) );
        return true;
      }
    };

    /*
     * try { repository.connect(userInfo.getLogin(), userInfo.getPassword()); //RepositoryExplorer explorer = new
     * RepositoryExplorer(new Shell(), repository, cb, null); //explorer.show(); } catch (XulException e) {
     * e.printStackTrace(); } catch (KettleSecurityException e) { e.printStackTrace(); } catch (KettleException e) {
     * e.printStackTrace(); }
     */

  }

}
