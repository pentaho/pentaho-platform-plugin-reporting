/*
 * This program is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software 
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this 
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html 
 * or from the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2006 - 2008 Pentaho Corporation.  All rights reserved. 
 * 
 * Created Apr 18, 2006
 *
 * @author mbatchel
 */
package org.pentaho.platform.engine.security.userrole.ws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IUserDetailsRoleListService;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.UserSession;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.engine.security.messages.Messages;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

public class MockUserDetailsRoleListService implements IUserDetailsRoleListService {
  
  private IUserRoleListService userRoleListService;

  public MockUserDetailsRoleListService() {
    userRoleListService = new MockUserRoleListService();
  }

  public void setUserRoleListService(final IUserRoleListService value) {
    this.userRoleListService = value;
  }

  public IUserRoleListService getUserRoleListService() {
    return this.userRoleListService;
  }

  public void afterPropertiesSet() throws Exception {
    if (this.userRoleListService == null) {
      throw new Exception(Messages.getInstance().getString("UserDetailsRoleListService.ERROR_0001_USERROLELISTSERVICE_NOT_SET")); //$NON-NLS-1$
    }
  }

  public List getAllRoles() {
    return Arrays.asList(userRoleListService.getAllAuthorities());
  }

  public List getAllUsers() {
    return Arrays.asList(userRoleListService.getAllUsernames());
  }

  public List getAllUsersInRole(final String role) {
    String[] users = userRoleListService.getUsernamesInRole(new GrantedAuthorityImpl(role));
    List rtn = new ArrayList();
    for (String element : users) {
      rtn.add(element);
    }
    return rtn;

  }

  public List getRolesForUser(final String userName) {
    List<String> rtn = new ArrayList<String>();
    GrantedAuthority[] auths = userRoleListService.getAuthoritiesForUser(userName);
    for (GrantedAuthority element : auths) {
      rtn.add(element.getAuthority());
    }
    return rtn;
  }

  public IPentahoSession getEffectiveUserSession(final String userName, final IParameterProvider paramProvider) {
    // Create user session object as un-authenticated so 
    // we can setup the roles before doing the startup actions.
    UserSession session = new UserSession(userName, null, false, paramProvider);
    session.setAuthenticated(userName);
    // Get roles into the session
    GrantedAuthority[] auths = userRoleListService.getAuthoritiesForUser(userName);
    Authentication auth = new UsernamePasswordAuthenticationToken(userName, null, auths);
    session.setAttribute(SecurityHelper.SESSION_PRINCIPAL, auth);
    // Now that roles are in place, do startup actions
    session.doStartupActions(paramProvider);
    // OK - Return back to the user.
    return session;
  }
  
  

}
