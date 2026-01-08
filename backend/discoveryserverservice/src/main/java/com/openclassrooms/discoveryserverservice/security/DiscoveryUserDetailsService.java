package com.openclassrooms.discoveryserverservice.security;

import com.openclassrooms.discoveryserverservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

/**
 * @author Kardigué MAGASSA
 * @version 1.0
 * @email magassakara@gmail.com
 * @since 2026-05-01
 */

@Service
@AllArgsConstructor
public class DiscoveryUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.getUserByUsername(username);
        //Passing a hard coded true value for credentials expired. Update per your need.
        return new User(user.getUsername(), user.getPassword(), user.isEnabled(), user.isAccountNonExpired(), true, user.isAccountNonLocked(), commaSeparatedStringToAuthorityList(user.getRole() + "," + user.getAuthorities()));
    }

}