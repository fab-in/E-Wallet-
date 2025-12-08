package com.example.Service;

import com.example.DTO.AuthResponseDTO;
import com.example.DTO.LoginRequestDTO;
import com.example.DTO.UserCreateDTO;
import com.example.DTO.UserDTO;
import com.example.Exceptions.DuplicateResourceException;
import com.example.Exceptions.ResourceNotFoundException;
import com.example.Exceptions.ValidationException;
import com.example.Model.User;
import com.example.Repository.UserRepo;
import com.example.Util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    private BCryptPasswordEncoder passwordEncoder;
    private User testUser;
    private UserCreateDTO testUserCreateDTO;
    private LoginRequestDTO testLoginRequestDTO;
    private UUID testUserId;
    private String testToken;
    private Date testExpirationDate;
    private Date testIssuedAtDate;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        testUserId = UUID.randomUUID();
        testToken = "test-jwt-token";
        testExpirationDate = new Date(System.currentTimeMillis() + 1800000); // 30 mins
        testIssuedAtDate = new Date();

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("John Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword(passwordEncoder.encode("Password123"));
        testUser.setPhoneNumber("1234567890");
        testUser.setRole("USER");

        testUserCreateDTO = new UserCreateDTO();
        testUserCreateDTO.setName("John Doe");
        testUserCreateDTO.setEmail("john.doe@example.com");
        testUserCreateDTO.setPassword("Password123");
        testUserCreateDTO.setPhoneNumber("1234567890");
        testUserCreateDTO.setRole("USER");

        testLoginRequestDTO = new LoginRequestDTO();
        testLoginRequestDTO.setEmail("john.doe@example.com");
        testLoginRequestDTO.setPassword("Password123");
    }

    @Test
    @DisplayName("Should return all users when getUsers is called")
    void testGetUsers_Success() {
        List<User> users = Arrays.asList(testUser, createAnotherUser());
        when(userRepo.findAll()).thenReturn(users);

        List<UserDTO> result = userService.getUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testUser.getId(), result.get(0).getId());
        assertEquals(testUser.getEmail(), result.get(0).getEmail());
        verify(userRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no users exist")
    void testGetUsers_EmptyList() {
        when(userRepo.findAll()).thenReturn(Collections.emptyList());

        List<UserDTO> result = userService.getUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(userRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("Should create user successfully with valid data")
    void testCreateUser_Success() {
        when(userRepo.existsByEmail(testUserCreateDTO.getEmail())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.createUser(testUserCreateDTO);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getEmail(), result.getEmail());
        assertEquals(testUser.getPhoneNumber(), result.getPhoneNumber());
        assertEquals(testUser.getRole(), result.getRole());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo, times(1)).existsByEmail(testUserCreateDTO.getEmail());
        verify(userRepo, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertTrue(passwordEncoder.matches(testUserCreateDTO.getPassword(), savedUser.getPassword()));
        assertEquals(testUserCreateDTO.getEmail(), savedUser.getEmail());
    }

    @Test
    @DisplayName("Should throw ValidationException when email is null")
    void testCreateUser_EmailNull() {
        testUserCreateDTO.setEmail(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.createUser(testUserCreateDTO);
        });

        assertEquals("Email is required and cannot be empty", exception.getMessage());
        verify(userRepo, never()).existsByEmail(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when email is empty")
    void testCreateUser_EmailEmpty() {
        testUserCreateDTO.setEmail("   ");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.createUser(testUserCreateDTO);
        });

        assertEquals("Email is required and cannot be empty", exception.getMessage());
        verify(userRepo, never()).existsByEmail(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when password is null")
    void testCreateUser_PasswordNull() {
        testUserCreateDTO.setPassword(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.createUser(testUserCreateDTO);
        });

        assertEquals("Password is required and cannot be empty", exception.getMessage());
        verify(userRepo, never()).existsByEmail(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw ValidationException when password is empty")
    void testCreateUser_PasswordEmpty() {
        testUserCreateDTO.setPassword("   ");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.createUser(testUserCreateDTO);
        });

        assertEquals("Password is required and cannot be empty", exception.getMessage());
        verify(userRepo, never()).existsByEmail(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void testCreateUser_DuplicateEmail() {
        when(userRepo.existsByEmail(testUserCreateDTO.getEmail())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(testUserCreateDTO);
        });

        assertEquals("User with email '" + testUserCreateDTO.getEmail() + "' already exists", exception.getMessage());
        verify(userRepo, times(1)).existsByEmail(testUserCreateDTO.getEmail());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should set default role as USER when role is null")
    void testCreateUser_DefaultRole() {
        testUserCreateDTO.setRole(null);
        when(userRepo.existsByEmail(testUserCreateDTO.getEmail())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.createUser(testUserCreateDTO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        assertEquals("USER", userCaptor.getValue().getRole());
    }

    @Test
    @DisplayName("Should return user by id when user exists")
    void testGetUserById_Success() {
        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));

        UserDTO result = userService.getUserById(testUserId);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepo, times(1)).findById(testUserId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user does not exist")
    void testGetUserById_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepo.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserById(nonExistentId);
        });

        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
        verify(userRepo, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("Should update user successfully with valid data")
    void testUpdateUser_Success() {
        UserCreateDTO updateDTO = new UserCreateDTO();
        updateDTO.setName("Jane Doe");
        updateDTO.setEmail("jane.doe@example.com");
        updateDTO.setPassword("NewPassword123");
        updateDTO.setPhoneNumber("9876543210");
        updateDTO.setRole("ADMIN");

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepo.existsByEmail(updateDTO.getEmail())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        UserDTO result = userService.updateUser(testUserId, updateDTO);

        assertNotNull(result);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo, times(1)).findById(testUserId);
        verify(userRepo, times(1)).existsByEmail(updateDTO.getEmail());
        verify(userRepo, times(1)).save(userCaptor.capture());

        User updatedUser = userCaptor.getValue();
        assertEquals(updateDTO.getName(), updatedUser.getName());
        assertEquals(updateDTO.getEmail(), updatedUser.getEmail());
        assertTrue(passwordEncoder.matches(updateDTO.getPassword(), updatedUser.getPassword()));
        assertEquals(updateDTO.getPhoneNumber(), updatedUser.getPhoneNumber());
        assertEquals(updateDTO.getRole(), updatedUser.getRole());
    }

    @Test
    @DisplayName("Should update user with partial data")
    void testUpdateUser_PartialUpdate() {
        UserCreateDTO updateDTO = new UserCreateDTO();
        updateDTO.setName("Jane Doe");

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(testUserId, updateDTO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals(updateDTO.getName(), updatedUser.getName());
        assertEquals(testUser.getEmail(), updatedUser.getEmail());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent user")
    void testUpdateUser_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepo.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser(nonExistentId, testUserCreateDTO);
        });

        assertEquals("User not found with id: " + nonExistentId, exception.getMessage());
        verify(userRepo, times(1)).findById(nonExistentId);
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when updating to existing email")
    void testUpdateUser_DuplicateEmail() {
        UserCreateDTO updateDTO = new UserCreateDTO();
        updateDTO.setEmail("existing@example.com");

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepo.existsByEmail(updateDTO.getEmail())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            userService.updateUser(testUserId, updateDTO);
        });

        assertEquals("User with email '" + updateDTO.getEmail() + "' already exists", exception.getMessage());
        verify(userRepo, times(1)).existsByEmail(updateDTO.getEmail());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should allow updating to same email")
    void testUpdateUser_SameEmail() {
        UserCreateDTO updateDTO = new UserCreateDTO();
        updateDTO.setEmail(testUser.getEmail()); // Same email
        updateDTO.setName("Updated Name");

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(testUserId, updateDTO);

        verify(userRepo, times(1)).findById(testUserId);
        verify(userRepo, never()).existsByEmail(anyString());
        verify(userRepo, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should delete user successfully when user exists")
    void testDeleteUser_Success() {
        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepo).delete(testUser);

        assertDoesNotThrow(() -> userService.deleteUser(testUserId));

        verify(userRepo, times(1)).findById(testUserId);
        verify(userRepo, times(1)).delete(testUser);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent user")
    void testDeleteUser_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepo.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(nonExistentId);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepo, times(1)).findById(nonExistentId);
        verify(userRepo, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() {
        when(userRepo.findByEmail(testLoginRequestDTO.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUserId, testUser.getEmail(), testUser.getRole())).thenReturn(testToken);
        when(jwtUtil.getExpirationDateFromToken(testToken)).thenReturn(testExpirationDate);
        when(jwtUtil.getIssuedAtDateFromToken(testToken)).thenReturn(testIssuedAtDate);

        AuthResponseDTO result = userService.login(testLoginRequestDTO);

        assertNotNull(result);
        assertEquals(testToken, result.getToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(testExpirationDate, result.getExpiresAt());
        assertEquals(testIssuedAtDate, result.getIssuedAt());
        assertEquals("Login successful", result.getMessage());
        assertNotNull(result.getUser());
        assertEquals(testUser.getId(), result.getUser().getId());
        assertEquals(testUser.getEmail(), result.getUser().getEmail());

        verify(userRepo, times(1)).findByEmail(testLoginRequestDTO.getEmail());
        verify(jwtUtil, times(1)).generateToken(testUserId, testUser.getEmail(), testUser.getRole());
    }

    @Test
    @DisplayName("Should throw ValidationException when email is null in login")
    void testLogin_EmailNull() {
        testLoginRequestDTO.setEmail(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login(testLoginRequestDTO);
        });

        assertEquals("Email is required", exception.getMessage());
        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when email is empty in login")
    void testLogin_EmailEmpty() {
        testLoginRequestDTO.setEmail("   ");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login(testLoginRequestDTO);
        });

        assertEquals("Email is required", exception.getMessage());
        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when password is null in login")
    void testLogin_PasswordNull() {
        testLoginRequestDTO.setPassword(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login(testLoginRequestDTO);
        });

        assertEquals("Password is required", exception.getMessage());
        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when password is empty in login")
    void testLogin_PasswordEmpty() {
        testLoginRequestDTO.setPassword("   ");

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login(testLoginRequestDTO);
        });

        assertEquals("Password is required", exception.getMessage());
        verify(userRepo, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when user does not exist in login")
    void testLogin_UserNotFound() {
        when(userRepo.findByEmail(testLoginRequestDTO.getEmail())).thenReturn(Optional.empty());

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login(testLoginRequestDTO);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepo, times(1)).findByEmail(testLoginRequestDTO.getEmail());
        verify(jwtUtil, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when password is incorrect in login")
    void testLogin_WrongPassword() {
        testLoginRequestDTO.setPassword("WrongPassword123");
        when(userRepo.findByEmail(testLoginRequestDTO.getEmail())).thenReturn(Optional.of(testUser));

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.login(testLoginRequestDTO);
        });

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepo, times(1)).findByEmail(testLoginRequestDTO.getEmail());
        verify(jwtUtil, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should signup successfully with valid data")
    void testSignup_Success() {
        when(userRepo.existsByEmail(testUserCreateDTO.getEmail())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUserId, testUser.getEmail(), testUser.getRole())).thenReturn(testToken);
        when(jwtUtil.getExpirationDateFromToken(testToken)).thenReturn(testExpirationDate);
        when(jwtUtil.getIssuedAtDateFromToken(testToken)).thenReturn(testIssuedAtDate);

        AuthResponseDTO result = userService.signup(testUserCreateDTO);

        assertNotNull(result);
        assertEquals(testToken, result.getToken());
        assertEquals("Bearer", result.getTokenType());
        assertEquals(testExpirationDate, result.getExpiresAt());
        assertEquals(testIssuedAtDate, result.getIssuedAt());
        assertEquals("Signup successful", result.getMessage());
        assertNotNull(result.getUser());
        assertEquals(testUser.getId(), result.getUser().getId());

        verify(userRepo, times(1)).existsByEmail(testUserCreateDTO.getEmail());
        verify(userRepo, times(1)).save(any(User.class));
        verify(userRepo, times(1)).findById(testUserId);
        verify(jwtUtil, times(1)).generateToken(testUserId, testUser.getEmail(), testUser.getRole());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found after creation in signup")
    void testSignup_UserNotFoundAfterCreation() {
        when(userRepo.existsByEmail(testUserCreateDTO.getEmail())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        when(userRepo.findById(testUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            userService.signup(testUserCreateDTO);
        });

        assertEquals("User not found after creation", exception.getMessage());
        verify(userRepo, times(1)).save(any(User.class));
        verify(userRepo, times(1)).findById(testUserId);
        verify(jwtUtil, never()).generateToken(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw ValidationException when email is null in signup")
    void testSignup_EmailNull() {
        testUserCreateDTO.setEmail(null);

        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.signup(testUserCreateDTO);
        });

        assertEquals("Email is required and cannot be empty", exception.getMessage());
        verify(userRepo, never()).existsByEmail(anyString());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists in signup")
    void testSignup_DuplicateEmail() {
        when(userRepo.existsByEmail(testUserCreateDTO.getEmail())).thenReturn(true);

        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            userService.signup(testUserCreateDTO);
        });

        assertEquals("User with email '" + testUserCreateDTO.getEmail() + "' already exists", exception.getMessage());
        verify(userRepo, times(1)).existsByEmail(testUserCreateDTO.getEmail());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update user without changing password when password is null")
    void testUpdateUser_PasswordNull() {
        UserCreateDTO updateDTO = new UserCreateDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setPassword(null);

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(testUserId, updateDTO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("Updated Name", updatedUser.getName());
        assertEquals(testUser.getPassword(), updatedUser.getPassword());
    }

    @Test
    @DisplayName("Should update only phone number when other fields are null")
    void testUpdateUser_OnlyPhoneNumber() {
        UserCreateDTO updateDTO = new UserCreateDTO();
        updateDTO.setPhoneNumber("9999999999");

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(testUserId, updateDTO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("9999999999", updatedUser.getPhoneNumber());
        assertEquals(testUser.getName(), updatedUser.getName());
        assertEquals(testUser.getEmail(), updatedUser.getEmail());
    }

    @Test
    @DisplayName("Should update only role when other fields are null")
    void testUpdateUser_OnlyRole() {
        UserCreateDTO updateDTO = new UserCreateDTO();
        updateDTO.setRole("ADMIN");

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(userRepo.save(any(User.class))).thenReturn(testUser);

        userService.updateUser(testUserId, updateDTO);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("ADMIN", updatedUser.getRole());
        assertEquals(testUser.getName(), updatedUser.getName());
    }

    @Test
    @DisplayName("Should convert User entity to DTO correctly")
    void testConvertToDTO_AllFields() {
        User user = new User();
        user.setId(testUserId);
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPhoneNumber("1234567890");
        user.setRole("ADMIN");

        when(userRepo.findById(testUserId)).thenReturn(Optional.of(user));

        UserDTO dto = userService.getUserById(testUserId);

        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getName(), dto.getName());
        assertEquals(user.getEmail(), dto.getEmail());
        assertEquals(user.getPhoneNumber(), dto.getPhoneNumber());
        assertEquals(user.getRole(), dto.getRole());
    }

    @Test
    @DisplayName("Should handle login with ADMIN role")
    void testLogin_AdminRole() {
        testUser.setRole("ADMIN");
        when(userRepo.findByEmail(testLoginRequestDTO.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUserId, testUser.getEmail(), "ADMIN")).thenReturn(testToken);
        when(jwtUtil.getExpirationDateFromToken(testToken)).thenReturn(testExpirationDate);
        when(jwtUtil.getIssuedAtDateFromToken(testToken)).thenReturn(testIssuedAtDate);

        AuthResponseDTO result = userService.login(testLoginRequestDTO);

        assertNotNull(result);
        assertEquals("ADMIN", result.getUser().getRole());
        verify(jwtUtil, times(1)).generateToken(testUserId, testUser.getEmail(), "ADMIN");
    }

    @Test
    @DisplayName("Should handle signup with ADMIN role")
    void testSignup_AdminRole() {
        testUserCreateDTO.setRole("ADMIN");
        testUser.setRole("ADMIN");
        when(userRepo.existsByEmail(testUserCreateDTO.getEmail())).thenReturn(false);
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        when(userRepo.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(testUserId, testUser.getEmail(), "ADMIN")).thenReturn(testToken);
        when(jwtUtil.getExpirationDateFromToken(testToken)).thenReturn(testExpirationDate);
        when(jwtUtil.getIssuedAtDateFromToken(testToken)).thenReturn(testIssuedAtDate);

        AuthResponseDTO result = userService.signup(testUserCreateDTO);

        assertNotNull(result);
        assertEquals("ADMIN", result.getUser().getRole());
        verify(jwtUtil, times(1)).generateToken(testUserId, testUser.getEmail(), "ADMIN");
    }

    private User createAnotherUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Jane Doe");
        user.setEmail("jane.doe@example.com");
        user.setPassword(passwordEncoder.encode("Password456"));
        user.setPhoneNumber("0987654321");
        user.setRole("ADMIN");
        return user;
    }
}
