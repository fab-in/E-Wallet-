package com.example.Controllers;

import com.example.DTO.AuthResponseDTO;
import com.example.DTO.LoginRequestDTO;
import com.example.DTO.UserCreateDTO;
import com.example.DTO.UserDTO;
import com.example.Exceptions.DuplicateResourceException;
import com.example.Exceptions.ResourceNotFoundException;
import com.example.Exceptions.ValidationException;
import com.example.Repository.UserRepo;
import com.example.Service.UserService;
import com.example.Security.JwtAuthenticationFilter;
import com.example.Util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController REST API Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepo userRepo;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDTO testUserDTO;
    private UserCreateDTO testUserCreateDTO;
    private LoginRequestDTO testLoginRequestDTO;
    private AuthResponseDTO testAuthResponseDTO;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        testUserDTO = new UserDTO();
        testUserDTO.setId(testUserId);
        testUserDTO.setName("John Doe");
        testUserDTO.setEmail("john.doe@example.com");
        testUserDTO.setPhoneNumber("1234567890");
        testUserDTO.setRole("USER");

        testUserCreateDTO = new UserCreateDTO();
        testUserCreateDTO.setName("John Doe");
        testUserCreateDTO.setEmail("john.doe@example.com");
        testUserCreateDTO.setPassword("Password123");
        testUserCreateDTO.setPhoneNumber("1234567890");
        testUserCreateDTO.setRole("USER");

        testLoginRequestDTO = new LoginRequestDTO();
        testLoginRequestDTO.setEmail("john.doe@example.com");
        testLoginRequestDTO.setPassword("Password123");

        testAuthResponseDTO = new AuthResponseDTO();
        testAuthResponseDTO.setToken("test-jwt-token");
        testAuthResponseDTO.setTokenType("Bearer");
        testAuthResponseDTO.setExpiresAt(new Date(System.currentTimeMillis() + 1800000));
        testAuthResponseDTO.setIssuedAt(new Date());
        testAuthResponseDTO.setMessage("Login successful");
        testAuthResponseDTO.setUser(testUserDTO);
    }

    @Test
    @DisplayName("GET /users - Should return list of users")
    void testGetUsers_Success() throws Exception {
        List<UserDTO> users = Arrays.asList(testUserDTO);
        when(userService.getUsers()).thenReturn(users);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testUserId.toString()))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));

        verify(userService, times(1)).getUsers();
    }

    @Test
    @DisplayName("GET /users/{id} - Should return user by id")
    void testGetUserById_Success() throws Exception {
        when(userService.getUserById(testUserId)).thenReturn(testUserDTO);

        mockMvc.perform(get("/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(userService, times(1)).getUserById(testUserId);
    }

    @Test
    @DisplayName("GET /users/{id} - Should return 404 when user not found")
    void testGetUserById_NotFound() throws Exception {
        when(userService.getUserById(testUserId))
                .thenThrow(new ResourceNotFoundException("User not found with id: " + testUserId));

        mockMvc.perform(get("/users/{id}", testUserId))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).getUserById(testUserId);
    }

    @Test
    @DisplayName("POST /users - Should create user successfully")
    void testCreateUser_Success() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class))).thenReturn(testUserDTO);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User added successfully"));

        verify(userService, times(1)).createUser(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /users - Should return 400 when validation fails")
    void testCreateUser_ValidationError() throws Exception {
        testUserCreateDTO.setEmail(null);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /users - Should return 409 when email already exists")
    void testCreateUser_DuplicateEmail() throws Exception {
        when(userService.createUser(any(UserCreateDTO.class)))
                .thenThrow(new DuplicateResourceException("User with email already exists"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isConflict());

        verify(userService, times(1)).createUser(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("PUT /users/{id} - Should update user successfully")
    void testUpdateUser_Success() throws Exception {
        when(userService.updateUser(eq(testUserId), any(UserCreateDTO.class))).thenReturn(testUserDTO);

        mockMvc.perform(put("/users/{id}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User updated successfully"));

        verify(userService, times(1)).updateUser(eq(testUserId), any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("PUT /users/{id} - Should return 404 when user not found")
    void testUpdateUser_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: " + testUserId))
                .when(userService).updateUser(eq(testUserId), any(UserCreateDTO.class));

        mockMvc.perform(put("/users/{id}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).updateUser(eq(testUserId), any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("DELETE /users/{id} - Should delete user successfully")
    void testDeleteUser_Success() throws Exception {
        doNothing().when(userService).deleteUser(testUserId);

        mockMvc.perform(delete("/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));

        verify(userService, times(1)).deleteUser(testUserId);
    }

    @Test
    @DisplayName("DELETE /users/{id} - Should return 404 when user not found")
    void testDeleteUser_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).deleteUser(testUserId);

        mockMvc.perform(delete("/users/{id}", testUserId))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteUser(testUserId);
    }

    @Test
    @DisplayName("POST /auth/signup - Should signup successfully")
    void testSignup_Success() throws Exception {
        testAuthResponseDTO.setMessage("Signup successful");
        when(userService.signup(any(UserCreateDTO.class))).thenReturn(testAuthResponseDTO);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.message").value("Signup successful"))
                .andExpect(jsonPath("$.user.id").value(testUserId.toString()));

        verify(userService, times(1)).signup(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /auth/signup - Should return 400 when validation fails")
    void testSignup_ValidationError() throws Exception {
        testUserCreateDTO.setEmail(null);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUserCreateDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).signup(any(UserCreateDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should login successfully")
    void testLogin_Success() throws Exception {
        when(userService.login(any(LoginRequestDTO.class))).thenReturn(testAuthResponseDTO);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.user.id").value(testUserId.toString()));

        verify(userService, times(1)).login(any(LoginRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 400 when credentials are invalid")
    void testLogin_InvalidCredentials() throws Exception {
        when(userService.login(any(LoginRequestDTO.class)))
                .thenThrow(new ValidationException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequestDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, times(1)).login(any(LoginRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login - Should return 400 when validation fails")
    void testLogin_ValidationError() throws Exception {
        testLoginRequestDTO.setEmail(null);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testLoginRequestDTO)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).login(any(LoginRequestDTO.class));
    }
}
