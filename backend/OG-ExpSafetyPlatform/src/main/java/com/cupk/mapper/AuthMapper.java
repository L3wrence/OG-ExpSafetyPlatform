package com.cupk.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuthMapper {
    @Select("""
        SELECT DISTINCT r.role_code
        FROM t_user_role ur
        JOIN t_role r ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
        """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("""
        SELECT DISTINCT p.code
        FROM t_user_role ur
        JOIN t_role_permission rp ON rp.role_id = ur.role_id
        JOIN t_permission p ON p.id = rp.permission_id
        WHERE ur.user_id = #{userId}
        """)
    List<String> selectPermissionCodes(@Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM t_user_role ur
        JOIN t_role r ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND UPPER(r.role_code) = UPPER(#{roleCode})
        """)
    Long countRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
