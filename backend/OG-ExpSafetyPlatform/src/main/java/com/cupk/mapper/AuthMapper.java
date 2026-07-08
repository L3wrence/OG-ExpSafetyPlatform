package com.cupk.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AuthMapper {
    @Select("""
        SELECT 'ADMIN'
        WHERE EXISTS (
            SELECT 1
            FROM t_user_role ur
            JOIN t_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND UPPER(r.role_code) = 'ADMIN'
        )
        UNION
        SELECT 'USER'
        WHERE NOT EXISTS (
            SELECT 1
            FROM t_user_role ur
            JOIN t_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND UPPER(r.role_code) = 'ADMIN'
        )
        """)
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("""
        SELECT DISTINCT p.code
        FROM t_permission p
        WHERE EXISTS (
            SELECT 1
            FROM t_user_role ur
            JOIN t_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND UPPER(r.role_code) = 'ADMIN'
        )
        OR p.id IN (
            SELECT rp.permission_id
            FROM t_role_permission rp
            JOIN t_role r ON r.id = rp.role_id
            WHERE UPPER(r.role_code) = 'USER'
        )
        OR (
            EXISTS (
                SELECT 1
                FROM t_teacher_certification tc
                WHERE tc.user_id = #{userId}
                  AND tc.status = 'APPROVED'
                  AND tc.deleted = 0
            )
            AND p.code IN (
                'dashboard:view',
                'course:view', 'course:create', 'course:update', 'course:delete',
                'course:publish', 'course:archive', 'course:copy', 'course:class:manage',
                'course:student:manage', 'course:invite:manage',
                'experiment:view', 'experiment:create', 'experiment:update', 'experiment:delete',
                'resource:view', 'resource:create', 'resource:update', 'resource:delete',
                'exam-paper:view', 'exam-paper:create', 'exam-paper:update', 'exam-paper:delete',
                'exam:grade',
                'reservation:review',
                'report:review', 'report:grade',
                'ai:ask',
                'resource-submission:review', 'resource-submission:create',
                'teacher-certification:apply'
            )
        )
        """)
    List<String> selectPermissionCodes(@Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM t_teacher_certification
        WHERE user_id = #{userId}
          AND status = 'APPROVED'
          AND deleted = 0
        """)
    Long countApprovedTeacherCertification(@Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM t_user_role ur
        JOIN t_role r ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
          AND UPPER(r.role_code) = UPPER(#{roleCode})
        """)
    Long countRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);
}
