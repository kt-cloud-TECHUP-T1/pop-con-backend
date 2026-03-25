package com.t1.popcon.auth.signup.client.dto;

public record UserCreateResponse(
    Long id,
    String name,
    String email
) {}
