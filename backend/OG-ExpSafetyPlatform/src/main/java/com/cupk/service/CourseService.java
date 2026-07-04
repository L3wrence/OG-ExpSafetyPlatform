package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.CourseCreateDTO;
import com.cupk.dto.CourseQueryDTO;
import com.cupk.dto.CourseUpdateDTO;
import com.cupk.vo.CourseDetailVO;
import com.cupk.vo.CourseListVO;

public interface CourseService {
    Long create(CourseCreateDTO dto);   //创建课程
    void update(Long id, CourseUpdateDTO dto);  //修改课程
    void delete(Long id);   //根据id删除课程
    void changeStatus(Long id, Integer status);     //修改课程状态
    PageResult<CourseListVO> page(CourseQueryDTO dto);      //
    CourseDetailVO detail(Long id);
}
