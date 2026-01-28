package com.example.helloworld.dto.church.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String username;
    private String accessToken;
    private String tokenType;
    private String refreshToken; // 可選
}
