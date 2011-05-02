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

import org.pentaho.platform.api.engine.IUserRoleListService;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

public class MockUserRoleListService implements IUserRoleListService {

  public static final String DEV_ROLE = "dev";
  public static final String ADMIN_ROLE = "Admin";
  public static final String DEV_MGR_ROLE = "devmgr";
  public static final String CEO_ROLE = "ceo";
  public static final String CTO_ROLE = "cto";
  public static final String AUTHENTICATED_ROLE = "Authenticated";
  public static final String IS_ROLE = "is";
  
  public GrantedAuthority[] getAllAuthorities() {
    GrantedAuthority[] allAuths = new GrantedAuthority[7];
    allAuths[0] = new GrantedAuthorityImpl(DEV_ROLE); 
    allAuths[1] = new GrantedAuthorityImpl(ADMIN_ROLE); 
    allAuths[2] = new GrantedAuthorityImpl(DEV_MGR_ROLE); 
    allAuths[3] = new GrantedAuthorityImpl(CEO_ROLE); 
    allAuths[4] = new GrantedAuthorityImpl(CTO_ROLE); 
    allAuths[5] = new GrantedAuthorityImpl(AUTHENTICATED_ROLE); 
    allAuths[6] = new GrantedAuthorityImpl(IS_ROLE); 
    return allAuths;
  }

  public String[] getAllUsernames() {
    String[] allUsers = new String[4];
    allUsers[0] = "pat"; //$NON-NLS-1$
    allUsers[1] = "tiffany"; //$NON-NLS-1$
    allUsers[2] = "joe"; //$NON-NLS-1$
    allUsers[3] = "suzy"; //$NON-NLS-1$
    return allUsers;
  }

  public String[] getUsernamesInRole(GrantedAuthority authority) {
    if (authority.getAuthority().equals(DEV_ROLE)) { //$NON-NLS-1$
      return new String[] { "pat", "tiffany" }; //$NON-NLS-1$ //$NON-NLS-2$
    } else if (authority.getAuthority().equals(ADMIN_ROLE)) { //$NON-NLS-1$
      return new String[] { "joe" };//$NON-NLS-1$
    } else if (authority.getAuthority().equals(DEV_MGR_ROLE)) { //$NON-NLS-1$
      return new String[] { "tiffany" };//$NON-NLS-1$
    } else if (authority.getAuthority().equals(CEO_ROLE)) { //$NON-NLS-1$
      return new String[] { "joe" };//$NON-NLS-1$
    } else if (authority.getAuthority().equals(CTO_ROLE)) { //$NON-NLS-1$
      return new String[] { "suzy" };//$NON-NLS-1$
    } else if (authority.getAuthority().equals(IS_ROLE)) { //$NON-NLS-1$
      return new String[] { "suzy" };//$NON-NLS-1$
    }
    return null;
  }

  public GrantedAuthority[] getAuthoritiesForUser(String userName) {
    if (userName.equals("pat")) { //$NON-NLS-1$
      return new GrantedAuthority[] { new GrantedAuthorityImpl(DEV_ROLE) };//$NON-NLS-1$
    } else if (userName.equals("tiffany")) {//$NON-NLS-1$
      return new GrantedAuthority[] { new GrantedAuthorityImpl(DEV_ROLE), new GrantedAuthorityImpl(DEV_MGR_ROLE) };//$NON-NLS-1$ //$NON-NLS-2$
    } else if (userName.equals("joe")) {//$NON-NLS-1$
      return new GrantedAuthority[] { new GrantedAuthorityImpl(ADMIN_ROLE), new GrantedAuthorityImpl(CEO_ROLE) };//$NON-NLS-1$ //$NON-NLS-2$
    } else if (userName.equals("suzy")) {//$NON-NLS-1$
      return new GrantedAuthority[] { new GrantedAuthorityImpl(CTO_ROLE), new GrantedAuthorityImpl(IS_ROLE) };//$NON-NLS-1$ //$NON-NLS-2$
    }
    return new GrantedAuthority[] {};

  }

}
