package com.disabilityclaim.security;

import com.disabilityclaim.domain.entity.UserAccount;
import com.disabilityclaim.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return User.builder()
                .username(account.getUsername())
                .password(account.getPasswordHash())
                .disabled(!account.isEnabled())
                .authorities(account.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode()))
                        .toList())
                .build();
    }
}
