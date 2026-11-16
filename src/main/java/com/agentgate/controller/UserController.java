package com.agentgate.controller;

import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.Result;
import com.agentgate.dto.UserDTO;
import com.agentgate.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterFormDTO dto) {
        userService.register(dto);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginFormDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "authorization", required = false) String token) {
        userService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<UserDTO> me() {
        return Result.ok(userService.me());
    }
}
