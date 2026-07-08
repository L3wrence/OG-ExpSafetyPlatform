package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.ReviewDTO;
import com.cupk.dto.TeacherCertificationApplyDTO;
import com.cupk.vo.TeacherCertificationVO;

public interface TeacherCertificationService {
    TeacherCertificationVO my();
    Long apply(TeacherCertificationApplyDTO dto);
    PageResult<TeacherCertificationVO> page(String status, Long pageNum, Long pageSize);
    void approve(Long id, ReviewDTO dto);
    void reject(Long id, ReviewDTO dto);
}
