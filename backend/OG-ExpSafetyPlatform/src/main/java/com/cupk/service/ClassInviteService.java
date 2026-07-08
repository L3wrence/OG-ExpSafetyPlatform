package com.cupk.service;

import com.cupk.dto.ClassInviteCreateDTO;
import com.cupk.dto.ClassJoinDTO;
import com.cupk.vo.ClassInviteVO;
import com.cupk.vo.CourseJoinVO;

import java.util.List;

public interface ClassInviteService {
    Long create(Long courseId, ClassInviteCreateDTO dto);
    List<ClassInviteVO> list(Long courseId);
    void disable(Long inviteId);
    CourseJoinVO join(ClassJoinDTO dto);
}
