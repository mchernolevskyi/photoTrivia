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
    @Value(value = "${user.admin.name}")
    private String adminUsername;

    @Value(value = "${user.admin.password}")
    private String adminPassword;

    @Value(value = "${user.guest.name}")
    private String guestUsername;

    @Value(value = "${user.guest.password}")
    private String guestPassword;

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static String ADMIN_ROLE = "ADMIN";
    private static String GUEST_ROLE = "GUEST";

    //TODO roles and albums
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .authorizeRequests()
//                    .antMatchers("/css/**").permitAll()
//                    .antMatchers("/**").hasRole(ADMIN_ROLE);
//                //.and()
//                //.httpBasic();
////                .formLogin()
////                    .loginPage("/login")
////                    .permitAll()
////                    .and()
////                .logout()
////                    .permitAll();
//    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().passwordEncoder(passwordEncoder)
                .withUser(adminUsername).password(passwordEncoder.encode(adminPassword)).roles(ADMIN_ROLE)
                .and()
                .withUser(guestUsername).password(passwordEncoder.encode(guestPassword)).roles(GUEST_ROLE);
    }
}
