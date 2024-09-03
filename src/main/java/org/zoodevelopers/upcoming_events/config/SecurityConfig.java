package org.zoodevelopers.upcoming_events.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zoodevelopers.upcoming_events.services.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("/api/v1/register")
    String endpoint;

    UserService service;

    MyBasicAuthenticationEntryPoint myBasicAuthenticationEntryPoint;

    public SecurityConfig(UserService service, MyBasicAuthenticationEntryPoint basicEntryPoint) {
        this.service = service;
        this.myBasicAuthenticationEntryPoint = basicEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfiguration()))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .logout(out -> out
                    .logoutUrl(endpoint + "/logout")
                    .deleteCookies("JSESSIONID"))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.GET, endpoint + "/login").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.GET, endpoint + "/events").hasAnyRole( "USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST, endpoint + "/events").hasRole("ADMIN")
                    .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()
                    .requestMatchers(HttpMethod.POST, endpoint + "/register").permitAll()
                    .requestMatchers(HttpMethod.POST, endpoint + "/event-registrations/{eventId}/register").authenticated()
                    .requestMatchers(HttpMethod.POST, endpoint + "/event-registrations/{evendId}/cancel").authenticated()
                    .requestMatchers(HttpMethod.GET, endpoint + "/event-registrations/user/{userId}/registered").authenticated()
                    // .requestMatchers(HttpMethod.GET, endpoint + "/**").permitAll() // todo is it needed? 
                    .anyRequest().authenticated())
                .userDetailsService(service)
                .httpBasic(basic -> basic.authenticationEntryPoint(myBasicAuthenticationEntryPoint))
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));

        http.headers(header -> header.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowCredentials(true);
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
