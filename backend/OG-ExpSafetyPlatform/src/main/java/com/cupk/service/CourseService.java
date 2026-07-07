package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.CourseCreateDTO;
import com.cupk.dto.CourseQueryDTO;
import com.cupk.dto.CourseStudentImportDTO;
import com.cupk.dto.CourseStudentRemoveDTO;
import com.cupk.dto.CourseUpdateDTO;
import com.cupk.dto.TeachingClassCreateDTO;
import com.cupk.dto.TeachingClassUpdateDTO;
import com.cupk.vo.CourseDetailVO;
import com.cupk.vo.CourseListVO;
import com.cupk.vo.CourseStudentImportResultVO;
import com.cupk.vo.CourseStudentVO;
import com.cupk.vo.TeachingClassVO;

import java.util.List;

public interface CourseService {
    Long create(CourseCreateDTO dto);   //创建课程
    void update(Long id, CourseUpdateDTO dto);  //修改课程
    void delete(Long id);   //根据id删除课程
    void changeStatus(Long id, Integer status);     //修改课程状态
    void publish(Long id, Boolean allowEmpty);
    void archive(Long id);
    Long copy(Long id);
    PageResult<CourseListVO> page(CourseQueryDTO dto);      //
    CourseDetailVO detail(Long id);
    List<TeachingClassVO> listClasses(Long courseId);
    Long createClass(Long courseId, TeachingClassCreateDTO dto);
    void updateClass(Long courseId, Long classId, TeachingClassUpdateDTO dto);
    void deleteClass(Long courseId, Long classId);
    List<CourseStudentVO> listStudents(Long courseId, Long teachingClassId, String keyword, String groupName);
    CourseStudentImportResultVO importStudents(Long courseId, CourseStudentImportDTO dto);
    void removeStudents(Long courseId, CourseStudentRemoveDTO dto);
}
