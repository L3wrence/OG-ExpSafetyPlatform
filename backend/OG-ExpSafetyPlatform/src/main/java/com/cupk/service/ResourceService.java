package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceInteractionDTO;
import com.cupk.dto.ResourceQueryDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.pojo.TeachingResource;
import com.cupk.vo.ResourcePreviewVO;
import com.cupk.vo.ResourceStatsVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface ResourceService {
    PageResult<TeachingResource> page(ResourceQueryDTO dto);
    Long createCourseResource(Long courseId, ResourceCreateDTO dto, MultipartFile file);
    void updateCourseResource(Long courseId, Long id, ResourceUpdateDTO dto, MultipartFile file);
    void delete(Long id);
    void changeStatus(Long id, Integer status);
    TeachingResource detail(Long id);
    void markDownload(Long id);
    void interact(Long id, ResourceInteractionDTO dto);
    void markInvalid(Long id, Integer invalidFlag);
    ResourceStatsVO stats(Long id);
    ResourcePreviewVO preview(Long id);
    Path resourceFilePath(Long id);
}
