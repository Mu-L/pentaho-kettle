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


package org.pentaho.di.connections.common.basic;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.provider.local.GenericFileNameParser;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class TestBasicFileProvider extends AbstractOriginatingFileProvider {

  public static String SCHEME = "test4";
  public static String SCHEME_NAME = "Test File System";

  protected static final Collection<Capability>
    capabilities = Collections.unmodifiableCollection( Arrays.asList( Capability.CREATE, Capability.DELETE,
    Capability.RENAME, Capability.GET_TYPE, Capability.LIST_CHILDREN, Capability.READ_CONTENT, Capability.URI,
    Capability.WRITE_CONTENT, Capability.GET_LAST_MODIFIED, Capability.RANDOM_ACCESS_READ ) );

  public TestBasicFileProvider() {
    setFileNameParser( new GenericFileNameParser() );
  }

  @Override protected FileSystem doCreateFileSystem( FileName fileName, FileSystemOptions fileSystemOptions )
    throws FileSystemException {
    return new TestBasicFileSystem( fileName, fileSystemOptions );
  }

  @Override public Collection<Capability> getCapabilities() {
    return capabilities;
  }
}
