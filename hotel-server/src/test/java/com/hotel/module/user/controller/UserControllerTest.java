package com.hotel.module.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.module.user.dto.RegisterRequest;
import com.hotel.module.user.service.UserSignInService;
import com.hotel.module.user.service.UserService;
import com.hotel.module.user.vo.SendCodeVO;
import com.hotel.module.user.vo.SignInStatusVO;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserSignInService userSignInService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, userSignInService)).build();
    }

    @Test
    void registerShouldReturnOk() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setPhone("13800000000");
        request.setEmail("user@example.com");
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
        when(userService.sendCode("user@example.com", "register"))
                .thenReturn(new SendCodeVO(300, 60, "654321"));

        mockMvc.perform(post("/api/user/send-code")
                        .param("email", "user@example.com")
                        .param("type", "register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("验证码已发送"))
                .andExpect(jsonPath("$.data.expireInSeconds").value(300))
                .andExpect(jsonPath("$.data.debugCode").value("654321"));

        verify(userService).sendCode("user@example.com", "register");
    }

    @Test
    void signInShouldReturnOk() throws Exception {
        SignInStatusVO statusVO = new SignInStatusVO();
        statusVO.setCheckedInToday(true);
        statusVO.setJustSignedIn(true);
        statusVO.setCurrentMonthSignInDays(6);
        statusVO.setContinuousSignInDays(3);
        when(userSignInService.signIn(any())).thenReturn(statusVO);

        mockMvc.perform(post("/api/user/sign-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("签到成功"))
                .andExpect(jsonPath("$.data.checkedInToday").value(true))
                .andExpect(jsonPath("$.data.currentMonthSignInDays").value(6));

        verify(userSignInService).signIn(null);
    }

    @Test
    void signInStatusShouldReturnOk() throws Exception {
        SignInStatusVO statusVO = new SignInStatusVO();
        statusVO.setCheckedInToday(false);
        statusVO.setJustSignedIn(false);
        statusVO.setCurrentMonthSignInDays(5);
        statusVO.setContinuousSignInDays(0);
        when(userSignInService.getSignInStatus(any())).thenReturn(statusVO);

        mockMvc.perform(get("/api/user/sign-in/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInToday").value(false))
                .andExpect(jsonPath("$.data.currentMonthSignInDays").value(5));

        verify(userSignInService).getSignInStatus(null);
    }
}
