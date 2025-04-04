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

/*
 * Created on 7-apr-2004
 *
 */

public class UserInfo implements IUser {
  // private static Class<?> PKG = UserInfo.class; // for i18n purposes,
  // needed by Translator2!!

  public static final String REPOSITORY_ELEMENT_TYPE = "user";

  private ObjectId id;

  private String login; // Login ID
  private String password; // Password
  private String username; // Long name
  private String description; // Description
  private boolean enabled; // Enabled: yes or no
  private Boolean admin; // Admin: yes or no

  /**
   * copy constructor
   *
   * @param copyFrom
   */
  public UserInfo( IUser copyFrom ) {
    this.id = copyFrom.getObjectId();
    this.login = copyFrom.getLogin();
    this.password = copyFrom.getPassword();
    this.username = copyFrom.getUsername();
    this.description = copyFrom.getDescription();
    this.enabled = copyFrom.isEnabled();
    this.admin = copyFrom.isAdmin();
  }

  public UserInfo( String login, String password, String username, String description, boolean enabled ) {
    this.login = login;
    this.password = password;
    this.username = username;
    this.description = description;
    this.enabled = enabled;
  }

  public UserInfo( String login ) {
    this();
    this.login = login;
  }

  public UserInfo() {
    this.login = null;
    this.password = null;
    this.username = null;
    this.description = null;
    this.enabled = true;
    this.admin = null;
  }

  public void setLogin( String login ) {
    this.login = login;
  }

  public String getLogin() {
    return login;
  }

  public void setPassword( String password ) {
    this.password = password;
  }

  public String getPassword() {
    return password;
  }

  public void setUsername( String username ) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public void setEnabled() {
    setEnabled( true );
  }

  public void setDisabled() {
    setEnabled( false );
  }

  public void setEnabled( boolean enabled ) {
    this.enabled = enabled;
  }

  public ObjectId getObjectId() {
    return id;
  }

  public void setObjectId( ObjectId id ) {
    this.id = id;
  }

  /**
   * @return the enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Not used in this case, simply return root /
   */
  public RepositoryDirectory getRepositoryDirectory() {
    return new RepositoryDirectory();
  }

  public void setRepositoryDirectory( RepositoryDirectory repositoryDirectory ) {
    throw new RuntimeException( "Setting a directory on a database connection is not supported" );
  }

  public String getRepositoryElementType() {
    return REPOSITORY_ELEMENT_TYPE;
  }

  /**
   * The name of the user maps to the login id
   */
  public String getName() {
    return login;
  }

  /**
   * Set the name of the user.
   *
   * @param name
   *          The name of the user maps to the login id.
   */
  public void setName( String name ) {
    this.login = name;
  }

  @Override
  public Boolean isAdmin() {
    return admin;
  }

  @Override
  public void setAdmin( Boolean admin ) {
    this.admin = admin;
  }
}
