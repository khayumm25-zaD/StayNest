package com.staynest.userservice.service;

import com.staynest.userservice.dto.AuthResponse;
import com.staynest.userservice.dto.LoginRequest;
import com.staynest.userservice.dto.RegisterRequest;
import com.staynest.userservice.dto.UpdateProfileRequest;
import com.staynest.userservice.dto.UserResponse;
import com.staynest.userservice.entity.Role;

import java.util.List;
import java.util.Set;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser(String email);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse updateProfile(String email, UpdateProfileRequest request);
    UserResponse updateRoles(Long id, Set<Role> roles);
}
