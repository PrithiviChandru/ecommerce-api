package com.micro.auth.service.impl;

import com.micro.auth.dto.UserResponse;
import com.micro.auth.dto.request.auth.UpdateProfileRequest;
import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;
import com.micro.auth.dto.response.user.DeleteResponse;
import com.micro.auth.entity.User;
import com.micro.auth.exception.ApiException;
import com.micro.auth.repository.UserRepository;
import com.micro.auth.security.JwtService;
import com.micro.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public ApiResponse<UserResponse> updateProfile(String accessToken, UpdateProfileRequest request) {
        String email = jwtService.extractEmail(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        if (null != request.firstName())
            user.setFirstName(request.firstName());
        if (null != request.lastName())
            user.setLastName(request.lastName());
        if (null != request.phone())
            user.setPhone(request.phone());
        if (null != request.timeZone())
            user.setTimeZone(request.timeZone());
        userRepository.save(user);

        UserResponse userInfo = mapToUserDetails(user);
        return ApiResponse.success("Profile updated successfully", userInfo);
    }

    @Override
    public ApiResponse<UserResponse> getProfile(String accessToken) {
        String email = jwtService.extractEmail(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        UserResponse userInfo = mapToUserDetails(user);
        return ApiResponse.success("Profile retrieved successful", userInfo);
    }

    @Override
    public ApiResponse<PagedResponse<UserResponse>> getAllUsers(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<User> pagedUsers = userRepository.findAll(pageable);

        List<UserResponse> userInfos = pagedUsers.getContent().stream()
                .map(this::mapToUserDetails)
                .collect(Collectors.toUnmodifiableList());
        PagedResponse response = PagedResponse.<UserResponse>builder()
                .content(userInfos)
                .page(pagedUsers.getNumber())
                .size(pagedUsers.getSize())
                .totalElements(pagedUsers.getTotalElements())
                .totalPages(pagedUsers.getTotalPages())
                .last(pagedUsers.isLast())
                .build();

        return ApiResponse.success(
                "User list fetched",
                response
        );
    }

    @Override
    public ApiResponse<UserResponse> getUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Entity not found"));
        UserResponse userInfo = mapToUserDetails(user);
        return ApiResponse.success("User date fetched", userInfo);
    }

    @Override
    public ApiResponse<DeleteResponse> deleteUser(Long id) {
        userRepository.deleteById(id);
        DeleteResponse response = new DeleteResponse(
                true,
                id,
                "User deleted successfully"
        );
        return ApiResponse.success("User deletion successful", response);
    }

    private UserResponse mapToUserDetails(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .timeZone(user.getTimeZone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
