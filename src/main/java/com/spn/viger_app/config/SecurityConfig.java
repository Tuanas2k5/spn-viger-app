package com.spn.viger_app.config;

import com.spn.viger_app.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Spring Security:
 * - Tất cả trang công khai (/,/blog,...) => ai cũng truy cập được
 * - Trang /admin/** => phải đăng nhập với vai trò ADMIN
 * - Trang /login => form đăng nhập tùy chỉnh
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập tĩnh (CSS, JS, ảnh, uploads)
                .requestMatchers(
                    "/css/**", "/js/**", "/img/**", "/image/**",
                    "/styles.css", "/uploads/**", "/fonts/**",
                    "/favicon.ico"
                ).permitAll()
                // Các trang công khai
                .requestMatchers(
                    "/", "/login",
                    "/ve-chung-toi", "/lien-he",
                    "/giao-duc-tai-duc", "/giao-duc-tai-duc/**",
                    "/tu-van", "/tu-van/**",
                    "/chuong-trinh-hoc",
                    "/blog", "/blog/**",
                    "/bai-viet/**"
                ).permitAll()
                // Trang admin => yêu cầu đăng nhập với role ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Mọi request khác cần xác thực
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/admin/posts", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
