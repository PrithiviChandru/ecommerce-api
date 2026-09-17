package com.micro.auth.service;

import com.micro.auth.dto.UserResponse;
import com.micro.auth.dto.request.auth.UpdateProfileRequest;
import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;
import com.micro.auth.dto.response.user.DeleteResponse;

import java.util.List;

public interface UserService {
    ApiResponse<UserResponse> updateProfile(String accessToken, UpdateProfileRequest request);

    ApiResponse<UserResponse> getProfile(String accessToken);

    ApiResponse<PagedResponse<UserResponse>> getAllUsers(int page, int size, String sortBy, String sortDir);

    ApiResponse<UserResponse> getUser(Long id);

    ApiResponse<DeleteResponse> deleteUser(Long id);
}
