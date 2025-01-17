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


package org.pentaho.di.plugins.fileopensave.providers.vfs.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.plugins.fileopensave.api.providers.Directory;
import org.pentaho.di.plugins.fileopensave.api.providers.EntityType;
import org.pentaho.di.plugins.fileopensave.providers.vfs.VFSFileProvider;
import org.pentaho.di.plugins.fileopensave.util.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by bmorrise on 2/13/19.
 */
public class VFSDirectory extends VFSFile implements Directory {

  public static final String DIRECTORY = "folder";

  private boolean hasChildren;
  private boolean canAddChildren;
  private List<VFSFile> children = new ArrayList<>();

  @Override public String getType() {
    return DIRECTORY;
  }

  public boolean hasChildren() {
    return hasChildren;
  }

  public void setHasChildren( boolean hasChildren ) {
    this.hasChildren = hasChildren;
  }

  public List<VFSFile> getChildren() {
    return children;
  }

  public void setChildren( List<VFSFile> children ) {
    this.children = children;
  }

  public void addChild( VFSFile file ) {
    this.children.add( file );
  }

  public boolean isHasChildren() {
    return hasChildren;
  }

  @Override public boolean isCanAddChildren() {
    return canAddChildren;
  }

  public void setCanAddChildren( boolean canAddChildren ) {
    this.canAddChildren = canAddChildren;
  }

  /**
   * Create a VFSDirectory.  Note that this creates an VFSDirectory object, it does not physically create the
   * directory.
   * <p/>
   * NOTE: logic for {@link #setRoot(String)} and {@link #setConnection(String)} is only used in
   * {@link org.pentaho.di.plugins.fileopensave.dragdrop.ElementDragListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)} for the
   * scenario of "Recent Repository File".
   *
   * @param parent The path to the parent folder
   * @param fileObject A VFSFileObject for the actual folder being created
   * @param connection The VFS connection associated with the directory
   * @param domain The domain associated with the directory
   * @return VFSDirectory
   */
  public static VFSDirectory create( String parent, FileObject fileObject, String connection, String domain ) {
    VFSDirectory vfsDirectory = new VFSDirectory();
    if ( fileObject.getName().toString().endsWith("/") ) {
      // Adls2 has a bug that delivers directories with an ending slash.  Others do not
      String adjustedName = fileObject.getName().toString();
      vfsDirectory.setName( Util.getName( adjustedName.substring( 0, adjustedName.length() - 1 ) ) ); //remove the trailing slash
    } else {
      vfsDirectory.setName( fileObject.getName().getBaseName() );
    }
    vfsDirectory.setPath( fileObject.getName().getURI() );
    vfsDirectory.setParent( parent );
    if ( connection != null ) {
      vfsDirectory.setConnection( connection );
      vfsDirectory.setRoot( VFSFileProvider.NAME );
    }
    vfsDirectory.setDomain( domain != null ? domain : "" );
    vfsDirectory.setCanEdit( true );
    vfsDirectory.setHasChildren( true );
    vfsDirectory.setCanAddChildren( true );
    try {
      vfsDirectory.setDate( new Date( fileObject.getContent().getLastModifiedTime() ) );
    } catch ( Exception e ) {
      vfsDirectory.setDate( new Date() );
    }
    return vfsDirectory;
  }

  @Override
  public int hashCode() {
    return Objects.hash( getProvider(), getConnection(), getPath() );
  }

  @Override public boolean equals( Object obj ) {
    // If the object is compared with itself then return true
    if ( obj == this ) {
      return true;
    }

    if ( !( obj instanceof VFSDirectory ) ) {
      return false;
    }

    VFSDirectory compare = (VFSDirectory) obj;
    // This comparison depends on `getProvider()` to always return a hardcoded value
    return compare.getProvider().equals( getProvider() )
      && StringUtils.equals( compare.getConnection(), getConnection() )
      && StringUtils.equals( compare.getPath(), getPath() );
  }

  @Override
  public EntityType getEntityType(){
    return EntityType.VFS_DIRECTORY;
  }
}
