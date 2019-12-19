package com.makswinner.phototrivia.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * @author Dr Maksym Chernolevskyi
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private static String USER_ADMIN = "admin";
    public static String ROLE_ADMIN = "ROLE_ADMIN";
    public static String ROLE_GUEST = "ROLE_GUEST";

    @Value(value = "${admin.user.password}")
    private String adminPassword;

    private List<GuestUsersConfig.UserAlbumAccess> guestUsers;

    @Autowired
    public SecurityConfig(GuestUsersConfig guestUsersConfig) {
        this.guestUsers = guestUsersConfig.getUsers();
    }

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> authConfigurer
                = auth.inMemoryAuthentication();
        authConfigurer.passwordEncoder(passwordEncoder)
                .withUser(USER_ADMIN).password(passwordEncoder.encode(adminPassword)).authorities(ROLE_ADMIN);

        guestUsers.stream().forEach(guestUser -> {
            try {
                authConfigurer
                        .withUser(guestUser.getName())
                        .password(passwordEncoder.encode(guestUser.getPassword()))
                        .authorities(ROLE_GUEST, guestUser.getAlbums());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
