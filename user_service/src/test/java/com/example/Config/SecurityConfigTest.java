package com.example.Config;

import com.example.Security.JwtAuthenticationFilter;
import com.example.Security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig Bean Tests")
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private HttpSecurity httpSecurity;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("securityFilterChain configures http and returns chain")
    void securityFilterChain_Configured() throws Exception {
        SecurityFilterChain filterChain = mock(SecurityFilterChain.class);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class))
                .thenReturn(httpSecurity);
        doReturn(filterChain).when(httpSecurity).build();

        SecurityFilterChain result = securityConfig.securityFilterChain(httpSecurity);

        assertNotNull(result);
        assertEquals(filterChain, result);
        verify(httpSecurity).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        verify(httpSecurity).build();
    }

    @Test
    @DisplayName("passwordEncoder provides BCrypt encoder")
    void passwordEncoder_Bcrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder instanceof BCryptPasswordEncoder);
        assertTrue(encoder.matches("secret", encoder.encode("secret")));
    }

    @Test
    @DisplayName("authenticationManager returns manager from configuration")
    void authenticationManager_ProvidedByConfiguration() throws Exception {
        AuthenticationManager expected = mock(AuthenticationManager.class);
        AuthenticationConfiguration configuration = mock(AuthenticationConfiguration.class);
        when(configuration.getAuthenticationManager()).thenReturn(expected);

        AuthenticationManager manager = securityConfig.authenticationManager(configuration);

        assertEquals(expected, manager);
    }
}
