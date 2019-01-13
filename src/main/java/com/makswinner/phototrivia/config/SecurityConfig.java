package com.makswinner.phototrivia.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Dr Maksym Chernolevskyi
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    public static String ROLE_ADMIN = "ADMIN";
    public static String ROLE_GUEST = "GUEST";

    @Value(value = "${user.admin.name}")
    private String adminUsername;

    @Value(value = "${user.admin.password}")
    private String adminPassword;

    @Value(value = "${user.guest.name}")
    private String guestUsername;

    @Value(value = "${user.guest.password}")
    private String guestPassword;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().passwordEncoder(passwordEncoder)
                .withUser(adminUsername).password(passwordEncoder.encode(adminPassword)).roles(ROLE_ADMIN)
                .and()
                .withUser(guestUsername).password(passwordEncoder.encode(guestPassword)).roles(ROLE_GUEST);
    }
}
