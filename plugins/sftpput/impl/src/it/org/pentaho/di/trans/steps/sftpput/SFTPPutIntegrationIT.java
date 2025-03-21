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


package org.pentaho.di.trans.steps.sftpput;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.pentaho.di.TestUtilities;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.job.entries.sftp.SftpServer;
import org.pentaho.di.trans.Trans;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrey Khayrutdinov
 */
public class SFTPPutIntegrationIT {

  private static TemporaryFolder folder;

  private static SftpServer server;

  @BeforeClass
  public static void startServer() throws Exception {
    KettleEnvironment.init();

    folder = new TemporaryFolder();
    folder.create();

    server = SftpServer.createDefaultServer( folder );
    server.start();
  }

  @AfterClass
  public static void stopServer() throws Exception {
    server.stop();
    server = null;

    folder.delete();
    folder = null;
  }


  private Session session;
  private ChannelSftp channel;

  @Before
  public void setUp() throws Exception {
    session = server.createJschSession();
    session.connect();

    channel = (ChannelSftp) session.openChannel( "sftp" );
  }

  @After
  public void tearDown() throws Exception {
    if ( channel != null && channel.isConnected() ) {
      channel.disconnect();
    }
    channel = null;

    if ( session.isConnected() ) {
      session.disconnect();
    }
    session = null;
  }


  /**
   * This case relates to <a href="http://jira.pentaho.com/browse/PDI-13897">PDI-13897</a>.
   * It executes a transformation with two steps: data grid and sftp put.
   * The latter uploads to an SFTP server a file <tt>pdi-13897/uploaded.txt</tt>, that contains a
   * <tt>qwerty</tt> string.<br/>
   *
   * Parameters of the transformation are:
   * <ul>
   *  <li>server</li>
   *  <li>port</li>
   *  <li>username</li>
   *  <li>password</li>
   * </ul>
   * @throws Exception
   */
  @Test
  public void putFileStreamingContentFromField() throws Exception {
    // prepare a directory for transformation's execution
    channel.connect();
    channel.mkdir( "pdi-13897" );

    // execute the transformation
    Trans trans = TestUtilities.loadAndRunTransformation(
      "src/it/resources/org/pentaho/di/trans/steps/sftpput/pdi-13897.ktr",
      "server", "localhost",
      "port", server.getPort(),
      "username", server.getUsername(),
      "password", server.getPassword()
    );
    assertEquals( 0, trans.getErrors() );

    // verify the results
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    channel.cd( "pdi-13897" );
    channel.get( "uploaded.txt", os );
    String content = new String( os.toByteArray() );
    assertEquals( "qwerty", content );
  }
}
