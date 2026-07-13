package com.hotel.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE phone = #{phone}")
    User selectByPhone(String phone);

    @Select("SELECT * FROM t_user WHERE email = #{email}")
    User selectByEmail(String email);

    @Select("SELECT r.name FROM t_role r INNER JOIN t_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRoleNamesByUserId(Long userId);

    @Select("SELECT p.name FROM t_permission p " +
            "INNER JOIN t_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN t_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectPermissionNamesByUserId(Long userId);
}
