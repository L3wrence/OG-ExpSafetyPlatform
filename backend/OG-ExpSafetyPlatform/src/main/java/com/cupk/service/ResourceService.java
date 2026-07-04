package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.ResourceCreateDTO;
import com.cupk.dto.ResourceQueryDTO;
import com.cupk.dto.ResourceUpdateDTO;
import com.cupk.pojo.TeachingResource;

public interface ResourceService {
    PageResult<TeachingResource> page(ResourceQueryDTO dto);
    Long create(ResourceCreateDTO dto);
    void update(Long id, ResourceUpdateDTO dto);
    void delete(Long id);
    void changeStatus(Long id, Integer status);
}
