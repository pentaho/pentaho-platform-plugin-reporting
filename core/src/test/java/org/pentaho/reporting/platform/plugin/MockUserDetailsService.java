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


package org.pentaho.reporting.platform.plugin;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;

public class MockUserDetailsService implements UserDetailsService {
  private static class DummyUserDetails implements UserDetails {
    private final String userName;

    public DummyUserDetails( final String userName ) {
      this.userName = userName;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
      return new ArrayList<GrantedAuthority>();
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

  public UserDetails loadUserByUsername( final String userName ) throws UsernameNotFoundException {
    return new DummyUserDetails( userName );
  }
}
