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
        WITH flags AS (
            SELECT
                EXISTS (
                    SELECT 1
                    FROM t_user_role ur
                    JOIN t_role r ON r.id = ur.role_id
                    WHERE ur.user_id = #{userId}
                      AND UPPER(r.role_code) = 'ADMIN'
                ) AS is_admin,
                EXISTS (
                    SELECT 1
                    FROM t_teacher_certification tc
                    WHERE tc.user_id = #{userId}
                      AND tc.status = 'APPROVED'
                      AND tc.deleted = 0
                ) AS is_certified_teacher
        ),
        base_permissions AS (
            SELECT 'portal:view' code UNION ALL
            SELECT 'portal:search' UNION ALL
            SELECT 'portal:message' UNION ALL
            SELECT 'profile:update' UNION ALL
            SELECT 'profile:password' UNION ALL
            SELECT 'course:view' UNION ALL
            SELECT 'course:join' UNION ALL
            SELECT 'experiment:view' UNION ALL
            SELECT 'resource:view' UNION ALL
            SELECT 'learning:update:self' UNION ALL
            SELECT 'exam:take' UNION ALL
            SELECT 'reservation:view' UNION ALL
            SELECT 'report:view' UNION ALL
            SELECT 'report:submit' UNION ALL
            SELECT 'ai:ask' UNION ALL
            SELECT 'teacher-certification:apply' UNION ALL
            SELECT 'resource-submission:create'
        ),
        teacher_permissions AS (
            SELECT 'dashboard:view' code UNION ALL
            SELECT 'course:view' UNION ALL
            SELECT 'course:create' UNION ALL
            SELECT 'course:update' UNION ALL
            SELECT 'course:delete' UNION ALL
            SELECT 'course:publish' UNION ALL
            SELECT 'course:archive' UNION ALL
            SELECT 'course:copy' UNION ALL
            SELECT 'course:class:manage' UNION ALL
            SELECT 'course:student:manage' UNION ALL
            SELECT 'course:invite:manage' UNION ALL
            SELECT 'experiment:view' UNION ALL
            SELECT 'experiment:create' UNION ALL
            SELECT 'experiment:update' UNION ALL
            SELECT 'experiment:delete' UNION ALL
            SELECT 'resource:view' UNION ALL
            SELECT 'resource:create' UNION ALL
            SELECT 'resource:update' UNION ALL
            SELECT 'resource:delete' UNION ALL
            SELECT 'question:view' UNION ALL
            SELECT 'question:create' UNION ALL
            SELECT 'question:update' UNION ALL
            SELECT 'question:delete' UNION ALL
            SELECT 'exam-paper:view' UNION ALL
            SELECT 'exam:create' UNION ALL
            SELECT 'exam:update' UNION ALL
            SELECT 'exam:delete' UNION ALL
            SELECT 'exam:grade' UNION ALL
            SELECT 'exam:statistics' UNION ALL
            SELECT 'reservation:view' UNION ALL
            SELECT 'reservation:manage' UNION ALL
            SELECT 'reservation:review' UNION ALL
            SELECT 'report:view' UNION ALL
            SELECT 'report:review' UNION ALL
            SELECT 'report:grade' UNION ALL
            SELECT 'recommend:view' UNION ALL
            SELECT 'ai:ask' UNION ALL
            SELECT 'resource-submission:review' UNION ALL
            SELECT 'resource-submission:create' UNION ALL
            SELECT 'teacher-certification:apply'
        )
        SELECT DISTINCT code
        FROM (
            SELECT p.code
            FROM t_permission p
            CROSS JOIN flags f
            WHERE f.is_admin = 1
            OR p.id IN (
                SELECT rp.permission_id
                FROM t_role_permission rp
                JOIN t_role r ON r.id = rp.role_id
                WHERE UPPER(r.role_code) = 'USER'
            )
            OR (f.is_certified_teacher = 1 AND p.code IN (SELECT code FROM teacher_permissions))

            UNION ALL
            SELECT bp.code
            FROM base_permissions bp
            CROSS JOIN flags f
            WHERE f.is_admin = 0

            UNION ALL
            SELECT tp.code
            FROM teacher_permissions tp
            CROSS JOIN flags f
            WHERE f.is_certified_teacher = 1
        ) effective_permissions
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
