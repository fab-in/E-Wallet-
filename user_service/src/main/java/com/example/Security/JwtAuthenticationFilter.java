package com.example.Security;

import com.example.Repository.UserRepo;
import com.example.Model.User;
import com.example.Util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepo userRepo;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            token = authHeader.substring(7).trim(); // Remove "Bearer " prefix and trim whitespace

            try {

                email = jwtUtil.getEmailFromToken(token);
                logger.debug("Extracted email from token: " + email);
            } catch (Exception e) {
                logger.warn("Could not extract email from token: " + e.getMessage());

            }
        } else if (authHeader != null) {
            logger.warn("Authorization header present but doesn't start with 'Bearer ': " + authHeader);
        }

        if (token != null && email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Validate the token
            if (jwtUtil.validateToken(token)) {

                Optional<User> userOptional = userRepo.findByEmail(email);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (user.getRole() != null) {

                        String roleName = "ROLE_" + user.getRole().toUpperCase();
                        authorities.add(new SimpleGrantedAuthority(roleName));
                    }

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            user, // Principal
                            null, // Credentials
                            authorities // Authorities
                    );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                    logger.info("User authenticated successfully: " + email + " with role: " + user.getRole());
                } else {
                    logger.warn("User not found in database for email: " + email);
                }
            } else {
                logger.warn("JWT token validation failed for email: " + email);
            }
        } else {
            if (token == null) {
                logger.debug("No JWT token found in request to: " + request.getRequestURI());
            } else if (email == null) {
                logger.warn("Could not extract email from token for request: " + request.getRequestURI());
            }
        }
        filterChain.doFilter(request, response);
    }
}
