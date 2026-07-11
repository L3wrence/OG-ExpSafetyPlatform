package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.ResourceSubmissionDTO;
import com.cupk.dto.ReviewDTO;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceSubmissionVO;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface ResourceSubmissionService {
    Long submit(ResourceSubmissionDTO dto, MultipartFile file);
    PageResult<ResourceSubmissionVO> my(Long pageNum, Long pageSize);
    PageResult<ResourceSubmissionVO> page(String status, Long pageNum, Long pageSize, String keyword);
    void approve(Long id, ReviewDTO dto);
    void reject(Long id, ReviewDTO dto);
    ResourcePreviewVO preview(Long id);
    Path filePath(Long id);
}
