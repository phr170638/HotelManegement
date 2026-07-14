package com.hotel.module.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.user.dto.RegisterRequest;
import com.hotel.module.user.service.UserService;
import com.hotel.module.user.vo.SendCodeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
    }

    @Test
    void registerShouldReturnOk() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000");
        request.setPassword("123456");
        request.setCode("654321");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    void sendCodeShouldReturnOk() throws Exception {
        when(userService.sendCode("13800000000", "register"))
                .thenReturn(new SendCodeVO(300, 60, "654321"));

        mockMvc.perform(post("/api/user/send-code")
                        .param("phone", "13800000000")
                        .param("type", "register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("验证码已发送"))
                .andExpect(jsonPath("$.data.expireInSeconds").value(300))
                .andExpect(jsonPath("$.data.debugCode").value("654321"));

        verify(userService).sendCode("13800000000", "register");
    }
}
