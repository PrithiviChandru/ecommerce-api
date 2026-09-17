package com.micro.auth.schema.user;

import com.micro.auth.dto.UserResponse;
import com.micro.auth.dto.response.ApiResponse;
import com.micro.auth.dto.response.PagedResponse;

import java.time.LocalDateTime;
import java.util.List;

public class UserDetailsListSchema extends ApiResponse<PagedResponse<UserResponse>> {
    public UserDetailsListSchema(boolean apiStatus, String message, PagedResponse<UserResponse> data, Object errors, LocalDateTime timeStamp) {
        super(apiStatus, message, data, errors, timeStamp);
    }
}
