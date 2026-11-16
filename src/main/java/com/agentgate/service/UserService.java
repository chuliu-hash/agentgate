package com.agentgate.service;

import com.agentgate.dto.LoginFormDTO;
import com.agentgate.dto.LoginResultDTO;
import com.agentgate.dto.RegisterFormDTO;
import com.agentgate.dto.UserDTO;

public interface UserService {

    void register(RegisterFormDTO dto);

    LoginResultDTO login(LoginFormDTO dto);

    void logout(String token);

    UserDTO me();
}
