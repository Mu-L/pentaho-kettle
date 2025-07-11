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


package org.pentaho.di.job.entries.pgpdecryptfiles;

import org.pentaho.di.job.entry.validator.AbstractFileValidator;
import org.pentaho.di.job.entry.validator.AndValidator;
import org.pentaho.di.job.entry.validator.JobEntryValidatorUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileType;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.util.Utils;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.IKettleVFS;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.pgpencryptfiles.GPG;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.job.entry.validator.ValidatorContext;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * This defines a 'PGP decrypt files' job entry.
 *
 * @author Samatar Hassan
 * @since 25-02-2008
 */
public class JobEntryPGPDecryptFiles extends JobEntryBase implements Cloneable, JobEntryInterface {
  private static Class<?> PKG = JobEntryPGPDecryptFiles.class; // for i18n purposes, needed by Translator2!!

  private SimpleDateFormat daf;
  private GPG gpg;

  public boolean arg_from_previous;
  public boolean include_subfolders;
  public boolean add_result_filesname;
  public boolean destination_is_a_file;
  public boolean create_destination_folder;
  public String[] source_filefolder;
  public String[] passphrase;
  public String[] destination_filefolder;
  public String[] wildcard;
  private String nr_errors_less_than;

  private String success_condition;
  public String SUCCESS_IF_AT_LEAST_X_FILES_UN_ZIPPED = "success_when_at_least";
  public String SUCCESS_IF_ERRORS_LESS = "success_if_errors_less";
  public String SUCCESS_IF_NO_ERRORS = "success_if_no_errors";

  private boolean add_date;
  private boolean add_time;
  private boolean SpecifyFormat;
  private String date_time_format;
  private boolean AddDateBeforeExtension;
  private boolean DoNotKeepFolderStructure;
  private String iffileexists;
  private String destinationFolder;
  private String ifmovedfileexists;
  private String moved_date_time_format;
  private boolean AddMovedDateBeforeExtension;
  private boolean add_moved_date;
  private boolean add_moved_time;
  private boolean SpecifyMoveFormat;
  public boolean create_move_to_folder;
  private String gpglocation;

  private int NrErrors = 0;
  private int NrSuccess = 0;
  private boolean successConditionBroken = false;
  private boolean successConditionBrokenExit = false;
  private int limitFiles = 0;

  public JobEntryPGPDecryptFiles( String n ) {
    super( n, "" );
    create_move_to_folder = false;
    SpecifyMoveFormat = false;
    add_moved_date = false;
    add_moved_time = false;
    AddMovedDateBeforeExtension = false;
    moved_date_time_format = null;
    gpglocation = null;
    ifmovedfileexists = "do_nothing";
    destinationFolder = null;
    DoNotKeepFolderStructure = false;
    arg_from_previous = false;
    source_filefolder = null;
    passphrase = null;
    destination_filefolder = null;
    wildcard = null;
    include_subfolders = false;
    add_result_filesname = false;
    destination_is_a_file = false;
    create_destination_folder = false;
    nr_errors_less_than = "10";
    success_condition = SUCCESS_IF_NO_ERRORS;
    add_date = false;
    add_time = false;
    SpecifyFormat = false;
    date_time_format = null;
    AddDateBeforeExtension = false;
    iffileexists = "do_nothing";
  }

  public JobEntryPGPDecryptFiles() {
    this( "" );
  }

  public void allocate( int nrFields ) {
    source_filefolder = new String[nrFields];
    passphrase = new String[nrFields];
    destination_filefolder = new String[nrFields];
    wildcard = new String[nrFields];
  }

  public Object clone() {
    JobEntryPGPDecryptFiles je = (JobEntryPGPDecryptFiles) super.clone();
    if ( source_filefolder != null ) {
      int nrFields = source_filefolder.length;
      je.allocate( nrFields );
      System.arraycopy( source_filefolder, 0, je.source_filefolder, 0, nrFields );
      System.arraycopy( destination_filefolder, 0, je.destination_filefolder, 0, nrFields );
      System.arraycopy( wildcard, 0, je.wildcard, 0, nrFields );
      System.arraycopy( passphrase, 0, je.passphrase, 0, nrFields );
    }
    return je;
  }

  public String getXML() {
    StringBuilder retval = new StringBuilder( 300 );

    retval.append( super.getXML() );
    retval.append( "      " ).append( XMLHandler.addTagValue( "gpglocation", gpglocation ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "arg_from_previous", arg_from_previous ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "include_subfolders", include_subfolders ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "add_result_filesname", add_result_filesname ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "destination_is_a_file", destination_is_a_file ) );
    retval.append( "      " ).append(
      XMLHandler.addTagValue( "create_destination_folder", create_destination_folder ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "add_date", add_date ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "add_time", add_time ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "SpecifyFormat", SpecifyFormat ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "date_time_format", date_time_format ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "nr_errors_less_than", nr_errors_less_than ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "success_condition", success_condition ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "AddDateBeforeExtension", AddDateBeforeExtension ) );
    retval.append( "      " ).append(
      XMLHandler.addTagValue( "DoNotKeepFolderStructure", DoNotKeepFolderStructure ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "iffileexists", iffileexists ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "destinationFolder", destinationFolder ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "ifmovedfileexists", ifmovedfileexists ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "moved_date_time_format", moved_date_time_format ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "create_move_to_folder", create_move_to_folder ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "add_moved_date", add_moved_date ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "add_moved_time", add_moved_time ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "SpecifyMoveFormat", SpecifyMoveFormat ) );
    retval.append( "      " ).append(
      XMLHandler.addTagValue( "AddMovedDateBeforeExtension", AddMovedDateBeforeExtension ) );

    retval.append( "      <fields>" ).append( Const.CR );
    if ( source_filefolder != null ) {
      for ( int i = 0; i < source_filefolder.length; i++ ) {
        retval.append( "        <field>" ).append( Const.CR );
        retval.append( "          " ).append( XMLHandler.addTagValue( "source_filefolder", source_filefolder[i] ) );
        retval.append( "          " ).append(
          XMLHandler.addTagValue( "passphrase", Encr.encryptPasswordIfNotUsingVariables( passphrase[i] ) ) );
        retval.append( "          " ).append(
          XMLHandler.addTagValue( "destination_filefolder", destination_filefolder[i] ) );
        retval.append( "          " ).append( XMLHandler.addTagValue( "wildcard", wildcard[i] ) );
        retval.append( "        </field>" ).append( Const.CR );
      }
    }
    retval.append( "      </fields>" ).append( Const.CR );

    return retval.toString();
  }

  public void loadXML( Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers,
    Repository rep, IMetaStore metaStore ) throws KettleXMLException {
    try {
      super.loadXML( entrynode, databases, slaveServers );
      gpglocation = XMLHandler.getTagValue( entrynode, "gpglocation" );
      arg_from_previous = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "arg_from_previous" ) );
      include_subfolders = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "include_subfolders" ) );
      add_result_filesname = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "add_result_filesname" ) );
      destination_is_a_file = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "destination_is_a_file" ) );
      create_destination_folder =
        "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "create_destination_folder" ) );
      add_date = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "add_date" ) );
      add_time = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "add_time" ) );
      SpecifyFormat = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "SpecifyFormat" ) );
      AddDateBeforeExtension =
        "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "AddDateBeforeExtension" ) );
      DoNotKeepFolderStructure =
        "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "DoNotKeepFolderStructure" ) );
      date_time_format = XMLHandler.getTagValue( entrynode, "date_time_format" );
      nr_errors_less_than = XMLHandler.getTagValue( entrynode, "nr_errors_less_than" );
      success_condition = XMLHandler.getTagValue( entrynode, "success_condition" );
      iffileexists = XMLHandler.getTagValue( entrynode, "iffileexists" );
      destinationFolder = XMLHandler.getTagValue( entrynode, "destinationFolder" );
      ifmovedfileexists = XMLHandler.getTagValue( entrynode, "ifmovedfileexists" );
      moved_date_time_format = XMLHandler.getTagValue( entrynode, "moved_date_time_format" );
      AddMovedDateBeforeExtension =
        "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "AddMovedDateBeforeExtension" ) );
      create_move_to_folder = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "create_move_to_folder" ) );
      add_moved_date = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "add_moved_date" ) );
      add_moved_time = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "add_moved_time" ) );
      SpecifyMoveFormat = "Y".equalsIgnoreCase( XMLHandler.getTagValue( entrynode, "SpecifyMoveFormat" ) );

      Node fields = XMLHandler.getSubNode( entrynode, "fields" );

      // How many field arguments?
      int nrFields = XMLHandler.countNodes( fields, "field" );
      allocate( nrFields );

      // Read them all...
      for ( int i = 0; i < nrFields; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );

        source_filefolder[i] = XMLHandler.getTagValue( fnode, "source_filefolder" );
        passphrase[i] = Encr.decryptPasswordOptionallyEncrypted( XMLHandler.getTagValue( fnode, "passphrase" ) );
        destination_filefolder[i] = XMLHandler.getTagValue( fnode, "destination_filefolder" );
        wildcard[i] = XMLHandler.getTagValue( fnode, "wildcard" );
      }
    } catch ( KettleXMLException xe ) {

      throw new KettleXMLException( BaseMessages.getString(
        PKG, "JobPGPDecryptFiles.Error.Exception.UnableLoadXML" ), xe );
    }
  }

  public void loadRep( Repository rep, IMetaStore metaStore, ObjectId id_jobentry, List<DatabaseMeta> databases,
    List<SlaveServer> slaveServers ) throws KettleException {
    try {
      gpglocation = rep.getJobEntryAttributeString( id_jobentry, "gpglocation" );
      arg_from_previous = rep.getJobEntryAttributeBoolean( id_jobentry, "arg_from_previous" );
      include_subfolders = rep.getJobEntryAttributeBoolean( id_jobentry, "include_subfolders" );
      add_result_filesname = rep.getJobEntryAttributeBoolean( id_jobentry, "add_result_filesname" );
      destination_is_a_file = rep.getJobEntryAttributeBoolean( id_jobentry, "destination_is_a_file" );
      create_destination_folder = rep.getJobEntryAttributeBoolean( id_jobentry, "create_destination_folder" );
      nr_errors_less_than = rep.getJobEntryAttributeString( id_jobentry, "nr_errors_less_than" );
      success_condition = rep.getJobEntryAttributeString( id_jobentry, "success_condition" );
      add_date = rep.getJobEntryAttributeBoolean( id_jobentry, "add_date" );
      add_time = rep.getJobEntryAttributeBoolean( id_jobentry, "add_time" );
      SpecifyFormat = rep.getJobEntryAttributeBoolean( id_jobentry, "SpecifyFormat" );
      date_time_format = rep.getJobEntryAttributeString( id_jobentry, "date_time_format" );
      AddDateBeforeExtension = rep.getJobEntryAttributeBoolean( id_jobentry, "AddDateBeforeExtension" );
      DoNotKeepFolderStructure = rep.getJobEntryAttributeBoolean( id_jobentry, "DoNotKeepFolderStructure" );
      iffileexists = rep.getJobEntryAttributeString( id_jobentry, "iffileexists" );
      destinationFolder = rep.getJobEntryAttributeString( id_jobentry, "destinationFolder" );
      ifmovedfileexists = rep.getJobEntryAttributeString( id_jobentry, "ifmovedfileexists" );
      moved_date_time_format = rep.getJobEntryAttributeString( id_jobentry, "moved_date_time_format" );
      AddMovedDateBeforeExtension = rep.getJobEntryAttributeBoolean( id_jobentry, "AddMovedDateBeforeExtension" );
      create_move_to_folder = rep.getJobEntryAttributeBoolean( id_jobentry, "create_move_to_folder" );
      add_moved_date = rep.getJobEntryAttributeBoolean( id_jobentry, "add_moved_date" );
      add_moved_time = rep.getJobEntryAttributeBoolean( id_jobentry, "add_moved_time" );
      SpecifyMoveFormat = rep.getJobEntryAttributeBoolean( id_jobentry, "SpecifyMoveFormat" );

      // How many arguments?
      int argnr = rep.countNrJobEntryAttributes( id_jobentry, "source_filefolder" );
      allocate( argnr );

      // Read them all...
      for ( int a = 0; a < argnr; a++ ) {
        source_filefolder[a] = rep.getJobEntryAttributeString( id_jobentry, a, "source_filefolder" );
        passphrase[a] =
          Encr
            .decryptPasswordOptionallyEncrypted( rep.getJobEntryAttributeString( id_jobentry, a, "passphrase" ) );
        destination_filefolder[a] = rep.getJobEntryAttributeString( id_jobentry, a, "destination_filefolder" );
        wildcard[a] = rep.getJobEntryAttributeString( id_jobentry, a, "wildcard" );
      }
    } catch ( KettleException dbe ) {

      throw new KettleException( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.Exception.UnableLoadRep" )
        + id_jobentry, dbe );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_job ) throws KettleException {
    try {
      rep.saveJobEntryAttribute( id_job, getObjectId(), "gpglocation", gpglocation );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "arg_from_previous", arg_from_previous );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "include_subfolders", include_subfolders );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "add_result_filesname", add_result_filesname );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "destination_is_a_file", destination_is_a_file );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "create_destination_folder", create_destination_folder );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "nr_errors_less_than", nr_errors_less_than );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "success_condition", success_condition );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "add_date", add_date );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "add_time", add_time );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "SpecifyFormat", SpecifyFormat );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "date_time_format", date_time_format );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "AddDateBeforeExtension", AddDateBeforeExtension );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "DoNotKeepFolderStructure", DoNotKeepFolderStructure );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "iffileexists", iffileexists );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "destinationFolder", destinationFolder );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "ifmovedfileexists", ifmovedfileexists );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "moved_date_time_format", moved_date_time_format );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "add_moved_date", add_moved_date );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "add_moved_time", add_moved_time );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "SpecifyMoveFormat", SpecifyMoveFormat );
      rep.saveJobEntryAttribute( id_job, getObjectId(), "create_move_to_folder", create_move_to_folder );
      rep
        .saveJobEntryAttribute(
          id_job, getObjectId(), "AddMovedDateBeforeExtension", AddMovedDateBeforeExtension );

      // save the arguments...
      if ( source_filefolder != null ) {
        for ( int i = 0; i < source_filefolder.length; i++ ) {
          rep.saveJobEntryAttribute( id_job, getObjectId(), i, "source_filefolder", source_filefolder[i] );
          rep.saveJobEntryAttribute( id_job, getObjectId(), i, "passphrase", Encr
            .encryptPasswordIfNotUsingVariables( passphrase[i] ) );
          rep
            .saveJobEntryAttribute(
              id_job, getObjectId(), i, "destination_filefolder", destination_filefolder[i] );
          rep.saveJobEntryAttribute( id_job, getObjectId(), i, "wildcard", wildcard[i] );
        }
      }
    } catch ( KettleDatabaseException dbe ) {

      throw new KettleException( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.Exception.UnableSaveRep" )
        + id_job, dbe );
    }
  }

  public Result execute( Result previousResult, int nr ) throws KettleException {
    try {
      Result result = previousResult;
      List<RowMetaAndData> rows = result.getRows();
      RowMetaAndData resultRow = null;
      result.setNrErrors( 1 );
      result.setResult( false );

      NrErrors = 0;
      NrSuccess = 0;
      successConditionBroken = false;
      successConditionBrokenExit = false;
      limitFiles = Const.toInt( environmentSubstitute( getNrErrorsLessThan() ), 10 );

      if ( include_subfolders ) {
        if ( isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.IncludeSubFoldersOn" ) );
        }
      }

      String MoveToFolder = environmentSubstitute( destinationFolder );
      // Get source and destination files, also wildcard
      String[] vsourcefilefolder = source_filefolder;
      String[] vpassphrase = passphrase;
      String[] vdestinationfilefolder = destination_filefolder;
      String[] vwildcard = wildcard;

      if ( iffileexists.equals( "move_file" ) ) {
        if ( Utils.isEmpty( MoveToFolder ) ) {
          logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.Error.MoveToFolderMissing" ) );
          return result;
        }
        FileObject folder = null;
        try {
          folder = KettleVFS.getInstance( parentJobMeta.getBowl() ).getFileObject( MoveToFolder );
          if ( !folder.exists() ) {
            if ( isDetailed() ) {
              logDetailed( BaseMessages
                .getString( PKG, "JobPGPDecryptFiles.Log.Error.FolderMissing", MoveToFolder ) );
            }
            if ( create_move_to_folder ) {
              folder.createFolder();
            } else {
              logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.Error.FolderMissing", MoveToFolder ) );
              return result;
            }
          }
          if ( !folder.getType().equals( FileType.FOLDER ) ) {
            logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.Error.NotFolder", MoveToFolder ) );
            return result;
          }
        } catch ( Exception e ) {
          logError( BaseMessages.getString(
            PKG, "JobPGPDecryptFiles.Log.Error.GettingMoveToFolder", MoveToFolder, e.getMessage() ) );
          return result;
        } finally {
          if ( folder != null ) {
            try {
              folder.close();
            } catch ( IOException ex ) { /* Ignore */
            }
          }
        }
      }

      gpg = new GPG( parentJobMeta.getBowl(), environmentSubstitute( gpglocation ), log );

      if ( arg_from_previous ) {
        if ( isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.ArgFromPrevious.Found", ( rows != null
            ? rows.size() : 0 )
            + "" ) );
        }
      }
      if ( arg_from_previous && rows != null ) {
        for ( int iteration = 0; iteration < rows.size(); iteration++ ) {
          // Success condition broken?
          if ( successConditionBroken ) {
            if ( !successConditionBrokenExit ) {
              logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.SuccessConditionbroken", ""
                + NrErrors ) );
              successConditionBrokenExit = true;
            }
            result.setNrErrors( NrErrors );
            displayResults();
            return result;
          }

          resultRow = rows.get( iteration );

          // Get source and destination file names, also wildcard
          String vsourcefilefolder_previous = resultRow.getString( 0, null );
          String vwildcard_previous =
            Encr.decryptPasswordOptionallyEncrypted( environmentSubstitute( resultRow.getString( 1, null ) ) );
          String vpassphrase_previous = resultRow.getString( 2, null );
          String vdestinationfilefolder_previous = resultRow.getString( 3, null );

          if ( !Utils.isEmpty( vsourcefilefolder_previous ) && !Utils.isEmpty( vdestinationfilefolder_previous ) ) {
            if ( isDetailed() ) {
              logDetailed( BaseMessages.getString(
                PKG, "JobPGPDecryptFiles.Log.ProcessingRow", vsourcefilefolder_previous,
                vdestinationfilefolder_previous, vwildcard_previous ) );
            }

            if ( !ProcessFileFolder(
              vsourcefilefolder_previous, vpassphrase_previous, vdestinationfilefolder_previous,
              vwildcard_previous, parentJob, result, MoveToFolder ) ) {
              // The move process fail
              // Update Errors
              updateErrors();
            }
          } else {
            if ( isDetailed() ) {
              logDetailed( BaseMessages.getString(
                PKG, "JobPGPDecryptFiles.Log.IgnoringRow", vsourcefilefolder[iteration],
                vdestinationfilefolder[iteration], vwildcard[iteration] ) );
            }
          }
        }
      } else if ( vsourcefilefolder != null && vdestinationfilefolder != null ) {
        for ( int i = 0; i < vsourcefilefolder.length && !parentJob.isStopped(); i++ ) {
          // Success condition broken?
          if ( successConditionBroken ) {
            if ( !successConditionBrokenExit ) {
              logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.SuccessConditionbroken", ""
                + NrErrors ) );
              successConditionBrokenExit = true;
            }
            result.setNrErrors( NrErrors );
            displayResults();
            return result;
          }

          if ( !Utils.isEmpty( vsourcefilefolder[i] ) && !Utils.isEmpty( vdestinationfilefolder[i] ) ) {
            // ok we can process this file/folder
            if ( isDetailed() ) {
              logDetailed( BaseMessages.getString(
                PKG, "JobPGPDecryptFiles.Log.ProcessingRow", vsourcefilefolder[i], vdestinationfilefolder[i],
                vwildcard[i] ) );
            }

            if ( !ProcessFileFolder(
              vsourcefilefolder[i], Encr
                .decryptPasswordOptionallyEncrypted( environmentSubstitute( vpassphrase[i] ) ),
              vdestinationfilefolder[i], vwildcard[i], parentJob, result, MoveToFolder ) ) {
              // Update Errors
              updateErrors();
            }
          } else {
            if ( isDetailed() ) {
              logDetailed( BaseMessages.getString(
                PKG, "JobPGPDecryptFiles.Log.IgnoringRow", vsourcefilefolder[i], vdestinationfilefolder[i],
                vwildcard[i] ) );
            }
          }
        }
      }

      // Success Condition
      result.setNrErrors( NrErrors );
      result.setNrLinesWritten( NrSuccess );
      if ( getSuccessStatus() ) {
        result.setResult( true );
      }

      displayResults();

      return result;
    } finally {
      if ( source_filefolder != null ) {
        source_filefolder = null;
      }
      if ( destination_filefolder != null ) {
        destination_filefolder = null;
      }
    }
  }

  private void displayResults() {
    if ( isDetailed() ) {
      logDetailed( "=======================================" );
      logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.Info.FilesInError", "" + NrErrors ) );
      logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.Info.FilesInSuccess", "" + NrSuccess ) );
      logDetailed( "=======================================" );
    }
  }

  private boolean getSuccessStatus() {
    boolean retval = false;

    if ( ( NrErrors == 0 && getSuccessCondition().equals( SUCCESS_IF_NO_ERRORS ) )
      || ( NrSuccess >= limitFiles && getSuccessCondition().equals( SUCCESS_IF_AT_LEAST_X_FILES_UN_ZIPPED ) )
      || ( NrErrors <= limitFiles && getSuccessCondition().equals( SUCCESS_IF_ERRORS_LESS ) ) ) {
      retval = true;
    }

    return retval;
  }

  private boolean ProcessFileFolder( String sourcefilefoldername, String passPhrase,
    String destinationfilefoldername, String wildcard, Job parentJob, Result result, String MoveToFolder ) {
    boolean entrystatus = false;
    FileObject sourcefilefolder = null;
    FileObject destinationfilefolder = null;
    FileObject movetofolderfolder = null;
    FileObject Currentfile = null;

    // Get real source, destination file and wildcard
    String realSourceFilefoldername = environmentSubstitute( sourcefilefoldername );
    String realDestinationFilefoldername = environmentSubstitute( destinationfilefoldername );
    String realWildcard = environmentSubstitute( wildcard );

    try {
      IKettleVFS vfs = KettleVFS.getInstance( parentJobMeta.getBowl() );

      sourcefilefolder = vfs.getFileObject( realSourceFilefoldername );
      destinationfilefolder = vfs.getFileObject( realDestinationFilefoldername );
      if ( !Utils.isEmpty( MoveToFolder ) ) {
        movetofolderfolder = vfs.getFileObject( MoveToFolder );
      }

      if ( sourcefilefolder.exists() ) {

        // Check if destination folder/parent folder exists !
        // If user wanted and if destination folder does not exist
        // PDI will create it
        if ( CreateDestinationFolder( destinationfilefolder ) ) {

          // Basic Tests
          if ( sourcefilefolder.getType().equals( FileType.FOLDER ) && destination_is_a_file ) {
            // Source is a folder, destination is a file
            // WARNING !!! CAN NOT MOVE FOLDER TO FILE !!!

            logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.Forbidden" ), BaseMessages.getString(
              PKG, "JobPGPDecryptFiles.Log.CanNotMoveFolderToFile", realSourceFilefoldername,
              realDestinationFilefoldername ) );

            // Update Errors
            updateErrors();
          } else {
            if ( destinationfilefolder.getType().equals( FileType.FOLDER )
              && sourcefilefolder.getType().equals( FileType.FILE ) ) {
              // Source is a file, destination is a folder
              // return destination short filename
              String shortfilename = sourcefilefolder.getName().getBaseName();

              try {
                shortfilename = getDestinationFilename( sourcefilefolder.getName().getBaseName() );
              } catch ( Exception e ) {
                logError( BaseMessages.getString(
                  PKG, "JobPGPDecryptFiles.Error.GettingFilename", sourcefilefolder.getName().getBaseName(), e
                    .toString() ) );
                return entrystatus;
              }
              // Move the file to the destination folder

              String destinationfilenamefull =
                destinationfilefolder.toString() + Const.FILE_SEPARATOR + shortfilename;
              FileObject destinationfile = vfs.getFileObject( destinationfilenamefull );

              entrystatus =
                DecryptFile(
                  shortfilename, sourcefilefolder, passPhrase, destinationfile, movetofolderfolder, parentJob,
                  result );

            } else if ( sourcefilefolder.getType().equals( FileType.FILE ) && destination_is_a_file ) {
              // Source is a file, destination is a file

              FileObject destinationfile = vfs.getFileObject( realDestinationFilefoldername );

              // return destination short filename
              String shortfilename = destinationfile.getName().getBaseName();
              try {
                shortfilename = getDestinationFilename( destinationfile.getName().getBaseName() );
              } catch ( Exception e ) {
                logError( BaseMessages.getString(
                  PKG, "JobPGPDecryptFiles.Error.GettingFilename", sourcefilefolder.getName().getBaseName(), e
                    .toString() ) );
                return entrystatus;
              }

              String destinationfilenamefull =
                destinationfilefolder.getParent().toString() + Const.FILE_SEPARATOR + shortfilename;
              destinationfile = vfs.getFileObject( destinationfilenamefull );

              entrystatus =
                DecryptFile(
                  shortfilename, sourcefilefolder, passPhrase, destinationfile, movetofolderfolder, parentJob,
                  result );

            } else {
              // Both source and destination are folders
              if ( isDetailed() ) {
                logDetailed( "  " );
                logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FetchFolder", sourcefilefolder
                  .toString() ) );
              }

              FileObject[] fileObjects = sourcefilefolder.findFiles( new AllFileSelector() {
                public boolean traverseDescendents( FileSelectInfo info ) {
                  return info.getDepth() == 0 || include_subfolders;
                }

                public boolean includeFile( FileSelectInfo info ) {
                  FileObject fileObject = info.getFile();
                  try {
                    if ( fileObject == null ) {
                      return false;
                    }
                  } catch ( Exception ex ) {
                    // Upon error don't process the file.
                    return false;
                  } finally {
                    if ( fileObject != null ) {
                      try {
                        fileObject.close();
                        fileObject = null;
                      } catch ( IOException ex ) { /* Ignore */
                      }
                    }
                  }
                  return true;
                }
              } );

              if ( fileObjects != null ) {
                for ( int j = 0; j < fileObjects.length && !parentJob.isStopped(); j++ ) {
                  // Success condition broken?
                  if ( successConditionBroken ) {
                    if ( !successConditionBrokenExit ) {
                      logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.SuccessConditionbroken", ""
                        + NrErrors ) );
                      successConditionBrokenExit = true;
                    }
                    return false;
                  }
                  // Fetch files in list one after one ...
                  Currentfile = fileObjects[j];

                  if ( !DecryptOneFile(
                    Currentfile, sourcefilefolder, passPhrase, realDestinationFilefoldername, realWildcard,
                    parentJob, result, movetofolderfolder ) ) {
                    // Update Errors
                    updateErrors();
                  }

                }
              }
            }
          }
          entrystatus = true;
        } else {
          // Destination Folder or Parent folder is missing
          logError( BaseMessages.getString(
            PKG, "JobPGPDecryptFiles.Error.DestinationFolderNotFound", realDestinationFilefoldername ) );
        }
      } else {
        logError( BaseMessages.getString(
          PKG, "JobPGPDecryptFiles.Error.SourceFileNotExists", realSourceFilefoldername ) );
      }
    } catch ( Exception e ) {
      logError( BaseMessages.getString(
        PKG, "JobPGPDecryptFiles.Error.Exception.MoveProcess", realSourceFilefoldername.toString(),
        destinationfilefolder.toString(), e.getMessage() ) );
      // Update Errors
      updateErrors();
    } finally {
      if ( sourcefilefolder != null ) {
        try {
          sourcefilefolder.close();
        } catch ( IOException ex ) { /* Ignore */
        }
      }
      if ( destinationfilefolder != null ) {
        try {
          destinationfilefolder.close();
        } catch ( IOException ex ) { /* Ignore */
        }
      }
      if ( Currentfile != null ) {
        try {
          Currentfile.close();
        } catch ( IOException ex ) { /* Ignore */
        }
      }
      if ( movetofolderfolder != null ) {
        try {
          movetofolderfolder.close();
        } catch ( IOException ex ) { /* Ignore */
        }
      }
    }
    return entrystatus;
  }

  private boolean DecryptFile( String shortfilename, FileObject sourcefilename, String passPharse,
    FileObject destinationfilename, FileObject movetofolderfolder, Job parentJob, Result result ) {

    FileObject destinationfile = null;
    boolean retval = false;
    try {
      if ( !destinationfilename.exists() ) {
        gpg.decryptFile( sourcefilename, passPharse, destinationfilename );

        if ( isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileDecrypted", sourcefilename
            .getName().toString(), destinationfilename.getName().toString() ) );
        }

        // add filename to result filename
        if ( add_result_filesname && !iffileexists.equals( "fail" ) && !iffileexists.equals( "do_nothing" ) ) {
          addFileToResultFilenames( destinationfilename.toString(), result, parentJob );
        }

        updateSuccess();

      } else {
        if ( isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileExists", destinationfilename
            .toString() ) );
        }
        if ( iffileexists.equals( "overwrite_file" ) ) {
          gpg.decryptFile( sourcefilename, passPharse, destinationfilename );

          if ( isDetailed() ) {
            logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileOverwrite", destinationfilename
              .getName().toString() ) );
          }

          // add filename to result filename
          if ( add_result_filesname && !iffileexists.equals( "fail" ) && !iffileexists.equals( "do_nothing" ) ) {
            addFileToResultFilenames( destinationfilename.toString(), result, parentJob );
          }

          updateSuccess();

        } else if ( iffileexists.equals( "unique_name" ) ) {
          String short_filename = shortfilename;

          // return destination short filename
          try {
            short_filename = getMoveDestinationFilename( short_filename, "ddMMyyyy_HHmmssSSS" );
          } catch ( Exception e ) {
            logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.GettingFilename", short_filename ), e );
            return retval;
          }

          String movetofilenamefull =
            destinationfilename.getParent().toString() + Const.FILE_SEPARATOR + short_filename;
          destinationfile = KettleVFS.getInstance( parentJobMeta.getBowl() ).getFileObject( movetofilenamefull );

          gpg.decryptFile( sourcefilename, passPharse, destinationfile );

          if ( isDetailed() ) {
            logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileDecrypted", sourcefilename
              .getName().toString(), destinationfile.getName().toString() ) );
          }

          // add filename to result filename
          if ( add_result_filesname && !iffileexists.equals( "fail" ) && !iffileexists.equals( "do_nothing" ) ) {
            addFileToResultFilenames( destinationfile.toString(), result, parentJob );
          }

          updateSuccess();
        } else if ( iffileexists.equals( "delete_file" ) ) {
          destinationfilename.delete();
          if ( isDetailed() ) {
            logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileDeleted", destinationfilename
              .getName().toString() ) );
          }
        } else if ( iffileexists.equals( "move_file" ) ) {
          String short_filename = shortfilename;
          // return destination short filename
          try {
            short_filename = getMoveDestinationFilename( short_filename, null );
          } catch ( Exception e ) {
            logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.GettingFilename", short_filename ), e );
            return retval;
          }

          String movetofilenamefull = movetofolderfolder.toString() + Const.FILE_SEPARATOR + short_filename;
          destinationfile = KettleVFS.getInstance( parentJobMeta.getBowl() ).getFileObject( movetofilenamefull );
          if ( !destinationfile.exists() ) {
            sourcefilename.moveTo( destinationfile );
            if ( isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileDecrypted", sourcefilename
                .getName().toString(), destinationfile.getName().toString() ) );
            }

            // add filename to result filename
            if ( add_result_filesname && !iffileexists.equals( "fail" ) && !iffileexists.equals( "do_nothing" ) ) {
              addFileToResultFilenames( destinationfile.toString(), result, parentJob );
            }

          } else {
            if ( ifmovedfileexists.equals( "overwrite_file" ) ) {
              sourcefilename.moveTo( destinationfile );
              if ( isDetailed() ) {
                logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileOverwrite", destinationfile
                  .getName().toString() ) );
              }

              // add filename to result filename
              if ( add_result_filesname && !iffileexists.equals( "fail" ) && !iffileexists.equals( "do_nothing" ) ) {
                addFileToResultFilenames( destinationfile.toString(), result, parentJob );
              }

              updateSuccess();
            } else if ( ifmovedfileexists.equals( "unique_name" ) ) {
              SimpleDateFormat daf = new SimpleDateFormat();
              Date now = new Date();
              daf.applyPattern( "ddMMyyyy_HHmmssSSS" );
              String dt = daf.format( now );
              short_filename += "_" + dt;

              String destinationfilenamefull =
                movetofolderfolder.toString() + Const.FILE_SEPARATOR + short_filename;
              destinationfile = KettleVFS.getInstance( parentJobMeta.getBowl() )
                .getFileObject( destinationfilenamefull );

              sourcefilename.moveTo( destinationfile );
              if ( isDetailed() ) {
                logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileDecrypted", destinationfile
                  .getName().toString() ) );
              }

              // add filename to result filename
              if ( add_result_filesname && !iffileexists.equals( "fail" ) && !iffileexists.equals( "do_nothing" ) ) {
                addFileToResultFilenames( destinationfile.toString(), result, parentJob );
              }

              updateSuccess();
            } else if ( ifmovedfileexists.equals( "fail" ) ) {
              // Update Errors
              updateErrors();
            }
          }

        } else if ( iffileexists.equals( "fail" ) ) {
          // Update Errors
          updateErrors();
        }

      }
    } catch ( Exception e ) {
      updateErrors();
      logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.Exception.MoveProcessError", sourcefilename
        .toString(), destinationfilename.toString(), e.getMessage() ) );
    } finally {
      if ( destinationfile != null ) {
        try {
          destinationfile.close();
        } catch ( IOException ex ) { /* Ignore */
        }
      }
    }
    return retval;
  }

  private boolean DecryptOneFile( FileObject Currentfile, FileObject sourcefilefolder, String passPhrase,
    String realDestinationFilefoldername, String realWildcard, Job parentJob, Result result,
    FileObject movetofolderfolder ) {
    boolean entrystatus = false;
    FileObject file_name = null;

    try {
      if ( !Currentfile.toString().equals( sourcefilefolder.toString() ) ) {
        // Pass over the Base folder itself

        // return destination short filename
        String sourceshortfilename = Currentfile.getName().getBaseName();
        String shortfilename = sourceshortfilename;
        try {
          shortfilename = getDestinationFilename( sourceshortfilename );
        } catch ( Exception e ) {
          logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.GettingFilename", Currentfile
            .getName().getBaseName(), e.toString() ) );
          return entrystatus;
        }

        int lenCurrent = sourceshortfilename.length();
        String short_filename_from_basefolder = shortfilename;
        if ( !isDoNotKeepFolderStructure() ) {
          short_filename_from_basefolder =
            Currentfile.toString().substring(
              sourcefilefolder.toString().length(), Currentfile.toString().length() );
        }
        short_filename_from_basefolder =
          short_filename_from_basefolder.substring( 0, short_filename_from_basefolder.length() - lenCurrent )
            + shortfilename;

        // Built destination filename
        file_name =
          KettleVFS.getInstance( parentJobMeta.getBowl() ).getFileObject( realDestinationFilefoldername
            + Const.FILE_SEPARATOR + short_filename_from_basefolder );

        if ( !Currentfile.getParent().toString().equals( sourcefilefolder.toString() ) ) {

          // Not in the Base Folder..Only if include sub folders
          if ( include_subfolders ) {
            // Folders..only if include subfolders
            if ( Currentfile.getType() != FileType.FOLDER ) {
              if ( GetFileWildcard( sourceshortfilename, realWildcard ) ) {
                entrystatus =
                  DecryptFile(
                    shortfilename, Currentfile, passPhrase, file_name, movetofolderfolder, parentJob, result );
              }
            }
          }
        } else {
          // In the Base Folder...
          // Folders..only if include subfolders
          if ( Currentfile.getType() != FileType.FOLDER ) {
            // file...Check if exists
            if ( GetFileWildcard( sourceshortfilename, realWildcard ) ) {
              entrystatus =
                DecryptFile(
                  shortfilename, Currentfile, passPhrase, file_name, movetofolderfolder, parentJob, result );

            }
          }

        }

      }
      entrystatus = true;

    } catch ( Exception e ) {
      logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.Error", e.toString() ) );
    } finally {
      if ( file_name != null ) {
        try {
          file_name.close();

        } catch ( IOException ex ) { /* Ignore */
        }
      }

    }
    return entrystatus;
  }

  private void updateErrors() {
    NrErrors++;
    if ( checkIfSuccessConditionBroken() ) {
      // Success condition was broken
      successConditionBroken = true;
    }
  }

  private boolean checkIfSuccessConditionBroken() {
    boolean retval = false;
    if ( ( NrErrors > 0 && getSuccessCondition().equals( SUCCESS_IF_NO_ERRORS ) )
      || ( NrErrors >= limitFiles && getSuccessCondition().equals( SUCCESS_IF_ERRORS_LESS ) ) ) {
      retval = true;
    }
    return retval;
  }

  private void updateSuccess() {
    NrSuccess++;
  }

  private void addFileToResultFilenames( String fileaddentry, Result result, Job parentJob ) {
    try {
      ResultFile resultFile =
        new ResultFile( ResultFile.FILE_TYPE_GENERAL, KettleVFS.getInstance( parentJobMeta.getBowl() )
                        .getFileObject( fileaddentry ), parentJob
          .getJobname(), toString() );
      result.getResultFiles().put( resultFile.getFile().toString(), resultFile );

      if ( isDebug() ) {
        logDebug( " ------ " );
        logDebug( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FileAddedToResultFilesName", fileaddentry ) );
      }

    } catch ( Exception e ) {
      logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Error.AddingToFilenameResult" ), fileaddentry
        + "" + e.getMessage() );
    }
  }

  private boolean CreateDestinationFolder( FileObject filefolder ) {
    FileObject folder = null;
    try {
      if ( destination_is_a_file ) {
        folder = filefolder.getParent();
      } else {
        folder = filefolder;
      }

      if ( !folder.exists() ) {
        if ( create_destination_folder ) {
          if ( isDetailed() ) {
            logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FolderNotExist", folder
              .getName().toString() ) );
          }
          folder.createFolder();
          if ( isDetailed() ) {
            logDetailed( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FolderWasCreated", folder
              .getName().toString() ) );
          }
        } else {
          logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.FolderNotExist", folder
            .getName().toString() ) );
          return false;
        }
      }
      return true;
    } catch ( Exception e ) {
      logError( BaseMessages.getString( PKG, "JobPGPDecryptFiles.Log.CanNotCreateParentFolder", folder
        .getName().toString() ), e );

    } finally {
      if ( folder != null ) {
        try {
          folder.close();
        } catch ( Exception ex ) { /* Ignore */
        }
      }
    }
    return false;
  }

  /**********************************************************
   *
   * @param selectedfile
   * @param wildcard
   * @return True if the selectedfile matches the wildcard
   **********************************************************/
  private boolean GetFileWildcard( String selectedfile, String wildcard ) {
    Pattern pattern = null;
    boolean getIt = true;

    if ( !Utils.isEmpty( wildcard ) ) {
      pattern = Pattern.compile( wildcard );
      // First see if the file matches the regular expression!
      if ( pattern != null ) {
        Matcher matcher = pattern.matcher( selectedfile );
        getIt = matcher.matches();
      }
    }

    return getIt;
  }

  private String getDestinationFilename( String shortsourcefilename ) throws Exception {
    String shortfilename = shortsourcefilename;
    int lenstring = shortsourcefilename.length();
    int lastindexOfDot = shortfilename.lastIndexOf( '.' );
    if ( lastindexOfDot == -1 ) {
      lastindexOfDot = lenstring;
    }

    if ( isAddDateBeforeExtension() ) {
      shortfilename = shortfilename.substring( 0, lastindexOfDot );
    }

    if ( daf == null ) {
      daf = new SimpleDateFormat();
    }
    Date now = new Date();

    if ( isSpecifyFormat() && !Utils.isEmpty( getDateTimeFormat() ) ) {
      daf.applyPattern( getDateTimeFormat() );
      String dt = daf.format( now );
      shortfilename += dt;
    } else {
      if ( isAddDate() ) {
        daf.applyPattern( "yyyyMMdd" );
        String d = daf.format( now );
        shortfilename += "_" + d;
      }
      if ( isAddTime() ) {
        daf.applyPattern( "HHmmssSSS" );
        String t = daf.format( now );
        shortfilename += "_" + t;
      }
    }
    if ( isAddDateBeforeExtension() ) {
      shortfilename += shortsourcefilename.substring( lastindexOfDot, lenstring );
    }

    return shortfilename;
  }

  private String getMoveDestinationFilename( String shortsourcefilename, String DateFormat ) throws Exception {
    String shortfilename = shortsourcefilename;
    int lenstring = shortsourcefilename.length();
    int lastindexOfDot = shortfilename.lastIndexOf( '.' );
    if ( lastindexOfDot == -1 ) {
      lastindexOfDot = lenstring;
    }

    if ( isAddMovedDateBeforeExtension() ) {
      shortfilename = shortfilename.substring( 0, lastindexOfDot );
    }

    SimpleDateFormat daf = new SimpleDateFormat();
    Date now = new Date();

    if ( DateFormat != null ) {
      daf.applyPattern( DateFormat );
      String dt = daf.format( now );
      shortfilename += dt;
    } else {

      if ( isSpecifyMoveFormat() && !Utils.isEmpty( getMovedDateTimeFormat() ) ) {
        daf.applyPattern( getMovedDateTimeFormat() );
        String dt = daf.format( now );
        shortfilename += dt;
      } else {
        if ( isAddMovedDate() ) {
          daf.applyPattern( "yyyyMMdd" );
          String d = daf.format( now );
          shortfilename += "_" + d;
        }
        if ( isAddMovedTime() ) {
          daf.applyPattern( "HHmmssSSS" );
          String t = daf.format( now );
          shortfilename += "_" + t;
        }
      }
    }
    if ( isAddMovedDateBeforeExtension() ) {
      shortfilename += shortsourcefilename.substring( lastindexOfDot, lenstring );
    }

    return shortfilename;
  }

  public void setAddDate( boolean adddate ) {
    this.add_date = adddate;
  }

  public boolean isAddDate() {
    return add_date;
  }

  public boolean isAddMovedDate() {
    return add_moved_date;
  }

  public void setAddMovedDate( boolean add_moved_date ) {
    this.add_moved_date = add_moved_date;
  }

  public boolean isAddMovedTime() {
    return add_moved_time;
  }

  public void setAddMovedTime( boolean add_moved_time ) {
    this.add_moved_time = add_moved_time;
  }

  public void setIfFileExists( String iffileexists ) {
    this.iffileexists = iffileexists;
  }

  public String getIfFileExists() {
    return iffileexists;
  }

  public void setIfMovedFileExists( String ifmovedfileexists ) {
    this.ifmovedfileexists = ifmovedfileexists;
  }

  public String getIfMovedFileExists() {
    return ifmovedfileexists;
  }

  public void setAddTime( boolean addtime ) {
    this.add_time = addtime;
  }

  public boolean isAddTime() {
    return add_time;
  }

  public void setAddDateBeforeExtension( boolean AddDateBeforeExtension ) {
    this.AddDateBeforeExtension = AddDateBeforeExtension;
  }

  public void setAddMovedDateBeforeExtension( boolean AddMovedDateBeforeExtension ) {
    this.AddMovedDateBeforeExtension = AddMovedDateBeforeExtension;
  }

  public boolean isSpecifyFormat() {
    return SpecifyFormat;
  }

  public void setSpecifyFormat( boolean SpecifyFormat ) {
    this.SpecifyFormat = SpecifyFormat;
  }

  public void setSpecifyMoveFormat( boolean SpecifyMoveFormat ) {
    this.SpecifyMoveFormat = SpecifyMoveFormat;
  }

  public boolean isSpecifyMoveFormat() {
    return SpecifyMoveFormat;
  }

  public String getDateTimeFormat() {
    return date_time_format;
  }

  public void setDateTimeFormat( String date_time_format ) {
    this.date_time_format = date_time_format;
  }

  public String getMovedDateTimeFormat() {
    return moved_date_time_format;
  }

  public void setMovedDateTimeFormat( String moved_date_time_format ) {
    this.moved_date_time_format = moved_date_time_format;
  }

  public boolean isAddDateBeforeExtension() {
    return AddDateBeforeExtension;
  }

  public boolean isAddMovedDateBeforeExtension() {
    return AddMovedDateBeforeExtension;
  }

  public boolean isDoNotKeepFolderStructure() {
    return DoNotKeepFolderStructure;
  }

  public void setDestinationFolder( String destinationFolder ) {
    this.destinationFolder = destinationFolder;
  }

  public String getDestinationFolder() {
    return destinationFolder;
  }

  /**
   * @deprecated use {@link #setGPGLocation(String)} instead
   * @param gpglocation
   */
  @Deprecated
  public void setGPGPLocation( String gpglocation ) {
    this.gpglocation = gpglocation;
  }

  public void setGPGLocation( String gpglocation ) {
    this.gpglocation = gpglocation;
  }

  public String getGPGLocation() {
    return gpglocation;
  }

  public void setDoNotKeepFolderStructure( boolean DoNotKeepFolderStructure ) {
    this.DoNotKeepFolderStructure = DoNotKeepFolderStructure;
  }

  public void setIncludeSubfolders( boolean include_subfoldersin ) {
    this.include_subfolders = include_subfoldersin;
  }

  public void setAddresultfilesname( boolean add_result_filesnamein ) {
    this.add_result_filesname = add_result_filesnamein;
  }

  public void setArgFromPrevious( boolean argfrompreviousin ) {
    this.arg_from_previous = argfrompreviousin;
  }

  public void setDestinationIsAFile( boolean destination_is_a_file ) {
    this.destination_is_a_file = destination_is_a_file;
  }

  public void setCreateDestinationFolder( boolean create_destination_folder ) {
    this.create_destination_folder = create_destination_folder;
  }

  public void setCreateMoveToFolder( boolean create_move_to_folder ) {
    this.create_move_to_folder = create_move_to_folder;
  }

  public void setNrErrorsLessThan( String nr_errors_less_than ) {
    this.nr_errors_less_than = nr_errors_less_than;
  }

  public String getNrErrorsLessThan() {
    return nr_errors_less_than;
  }

  public void setSuccessCondition( String success_condition ) {
    this.success_condition = success_condition;
  }

  public String getSuccessCondition() {
    return success_condition;
  }

  public boolean evaluates() {
    return true;
  }

  public void check( List<CheckResultInterface> remarks, JobMeta jobMeta, VariableSpace space,
    Repository repository, IMetaStore metaStore ) {
    boolean res = JobEntryValidatorUtils.andValidator().validate( parentJobMeta.getBowl(), this, "arguments",
      remarks, AndValidator.putValidators( JobEntryValidatorUtils.notNullValidator() ) );

    if ( res == false ) {
      return;
    }

    ValidatorContext ctx = new ValidatorContext();
    AbstractFileValidator.putVariableSpace( ctx, getVariables() );
    AndValidator.putValidators( ctx, JobEntryValidatorUtils.notNullValidator(), JobEntryValidatorUtils.fileExistsValidator() );

    for ( int i = 0; i < source_filefolder.length; i++ ) {
      JobEntryValidatorUtils.andValidator().validate( parentJobMeta.getBowl(), this, "arguments[" + i + "]",
                                                      remarks, ctx );
    }
  }

}
