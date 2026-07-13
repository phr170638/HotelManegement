package com.hotel.module.user.service.impl;

import com.hotel.module.user.entity.User;
import com.hotel.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 尝试用手机号或邮箱查找
        User user = userMapper.selectByPhone(username);
        if (user == null) {
            user = userMapper.selectByEmail(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用");
        }

        List<String> roles = userMapper.selectRoleNamesByUserId(user.getId());
        List<String> perms = userMapper.selectPermissionNamesByUserId(user.getId());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        }
        if (perms != null) {
            perms.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));
        }

        return new org.springframework.security.core.userdetails.User(
                String.valueOf(user.getId()),
                user.getPassword(),
                user.getStatus() == 1,
                true, true, true,
                authorities
        );
    }
}
