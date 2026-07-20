package com.hotel.module.user.service;

import com.hotel.module.user.vo.SignInStatusVO;

public interface UserSignInService {

    SignInStatusVO signIn(Long userId);

    SignInStatusVO getSignInStatus(Long userId);
}
