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
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

public interface CourseService {
    Long create(CourseCreateDTO dto, MultipartFile cover);
    void update(Long id, CourseUpdateDTO dto, MultipartFile cover, Boolean removeCover);
    Path coverFilePath(Long id);
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
