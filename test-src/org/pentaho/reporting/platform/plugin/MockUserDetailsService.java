package org.pentaho.reporting.platform.plugin;

import org.springframework.dao.DataAccessException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;

public class MockUserDetailsService implements UserDetailsService
{
  private static class DummyUserDetails implements UserDetails
  {
    private final String userName;

    public DummyUserDetails(final String userName)
    {
      this.userName = userName;
    }

    public GrantedAuthority[] getAuthorities()
    {
      return new GrantedAuthority[0];
    }

    public String getPassword()
    {
      return null;
    }

    public String getUsername()
    {
      return userName;
    }

    public boolean isAccountNonExpired()
    {
      return true;
    }

    public boolean isAccountNonLocked()
    {
      return true;
    }

    public boolean isCredentialsNonExpired()
    {
      return true;
    }

    public boolean isEnabled()
    {
      return true;
    }
  }

  public MockUserDetailsService()
  {
  }

  public UserDetails loadUserByUsername(final String userName) throws UsernameNotFoundException, DataAccessException
  {
    return new DummyUserDetails(userName);
  }
}
