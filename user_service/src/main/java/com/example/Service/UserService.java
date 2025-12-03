package com.example.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.Repository.UserRepo;
import com.example.Model.User;
import com.example.DTO.UserDTO;
import com.example.DTO.UserCreateDTO;
import com.example.DTO.LoginRequestDTO;
import com.example.DTO.AuthResponseDTO;
import com.example.Util.JwtUtil;
import com.example.Exceptions.DuplicateResourceException;
import com.example.Exceptions.ResourceNotFoundException;
import com.example.Exceptions.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JwtUtil jwtUtil;

    // BCrypt password encoder
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public List<UserDTO> getUsers() {
        return userRepo.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO createUser(UserCreateDTO userCreateDTO) {

        if (userCreateDTO.getEmail() == null || userCreateDTO.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required and cannot be empty");
        }

        if (userCreateDTO.getPassword() == null || userCreateDTO.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required and cannot be empty");
        }

        if (userRepo.existsByEmail(userCreateDTO.getEmail())) {
            throw new DuplicateResourceException("User with email '" + userCreateDTO.getEmail() + "' already exists");
        }

        User user = convertToEntity(userCreateDTO);

        User savedUser = userRepo.save(user);
        return convertToDTO(savedUser);
    }

    public UserDTO getUserById(UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    public UserDTO updateUser(UUID id, UserCreateDTO userCreateDTO) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (userCreateDTO.getEmail() != null && !userCreateDTO.getEmail().equals(user.getEmail())) {
            if (userRepo.existsByEmail(userCreateDTO.getEmail())) {
                throw new DuplicateResourceException(
                        "User with email '" + userCreateDTO.getEmail() + "' already exists");
            }
        }

        if (userCreateDTO.getName() != null) {
            user.setName(userCreateDTO.getName());
        }
        if (userCreateDTO.getEmail() != null) {
            user.setEmail(userCreateDTO.getEmail());
        }
        if (userCreateDTO.getPassword() != null) {
            String hashedPassword = passwordEncoder.encode(userCreateDTO.getPassword());
            user.setPassword(hashedPassword);
        }
        if (userCreateDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(userCreateDTO.getPhoneNumber());
        }
        if (userCreateDTO.getRole() != null) {
            user.setRole(userCreateDTO.getRole());
        }

        User updatedUser = userRepo.save(user);
        return convertToDTO(updatedUser);
    }

    public void deleteUser(UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userRepo.delete(user);

    }

    private UserDTO convertToDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setRole(user.getRole());
        return userDTO;

    }

    public AuthResponseDTO login(LoginRequestDTO loginRequest) {

        if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }

        Optional<User> userOptional = userRepo.findByEmail(loginRequest.getEmail());

        if (userOptional.isEmpty()) {
            throw new ValidationException("Invalid email or password");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new ValidationException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        UserDTO userDTO = convertToDTO(user);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresAt(jwtUtil.getExpirationDateFromToken(token));
        response.setIssuedAt(jwtUtil.getIssuedAtDateFromToken(token));
        response.setMessage("Login successful");
        response.setUser(userDTO);

        return response;
    }

    public AuthResponseDTO signup(UserCreateDTO userCreateDTO) {

        UserDTO userDTO = createUser(userCreateDTO);

        User user = userRepo.findById(userDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after creation"));

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setExpiresAt(jwtUtil.getExpirationDateFromToken(token));
        response.setIssuedAt(jwtUtil.getIssuedAtDateFromToken(token));
        response.setMessage("Signup successful");
        response.setUser(userDTO);

        return response;
    }

    private User convertToEntity(UserCreateDTO userCreateDTO) {
        User user = new User();
        user.setName(userCreateDTO.getName());

        user.setEmail(userCreateDTO.getEmail());
        String hashedPassword = passwordEncoder.encode(userCreateDTO.getPassword());
        user.setPassword(hashedPassword);
        user.setPhoneNumber(userCreateDTO.getPhoneNumber());
        user.setRole(userCreateDTO.getRole() != null ? userCreateDTO.getRole() : "USER");
        return user;
    }
}
