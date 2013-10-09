/*!
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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.springframework.dao.DataAccessException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;

public class MockUserDetailsService implements UserDetailsService {
  private static class DummyUserDetails implements UserDetails {
    private final String userName;

    public DummyUserDetails( final String userName ) {
      this.userName = userName;
    }

    public GrantedAuthority[] getAuthorities() {
      return new GrantedAuthority[0];
    }

    public String getPassword() {
      return null;
    }

    public String getUsername() {
      return userName;
    }

    public boolean isAccountNonExpired() {
      return true;
    }

    public boolean isAccountNonLocked() {
      return true;
    }

    public boolean isCredentialsNonExpired() {
      return true;
    }

    public boolean isEnabled() {
      return true;
    }
  }

  public MockUserDetailsService() {
  }

  public UserDetails loadUserByUsername( final String userName ) throws UsernameNotFoundException, DataAccessException {
    return new DummyUserDetails( userName );
  }
}
