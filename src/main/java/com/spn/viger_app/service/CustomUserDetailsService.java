package com.spn.viger_app.service;

import com.spn.viger_app.entity.Users;
import com.spn.viger_app.repository.UsersRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Service xác thực người dùng với cơ sở dữ liệu.
 * Spring Security sử dụng service này để tìm và xác thực tài khoản admin.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    public CustomUserDetailsService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản: " + username));

        if (!user.getActive()) {
            throw new UsernameNotFoundException("Tài khoản đã bị vô hiệu hóa: " + username);
        }

        return new User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
