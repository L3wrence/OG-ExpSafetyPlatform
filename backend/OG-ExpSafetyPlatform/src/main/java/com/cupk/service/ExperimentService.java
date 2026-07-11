package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.ExperimentCreateDTO;
import com.cupk.dto.ExperimentQueryDTO;
import com.cupk.dto.ExperimentStepDTO;
import com.cupk.dto.ExperimentUpdateDTO;
import com.cupk.pojo.Experiment;
import com.cupk.vo.ExperimentDetailVO;

import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface ExperimentService {
    Long create(ExperimentCreateDTO dto);
    void update(Long id, ExperimentUpdateDTO dto);
    void delete(Long id);
    void changeStatus(Long id, Integer status);
    void saveSteps(Long id, List<ExperimentStepDTO> steps, Map<Integer, MultipartFile> files);
    Path stepFilePath(Long stepId);
    PageResult<Experiment> page(ExperimentQueryDTO dto);
    ExperimentDetailVO detail(Long id);
}
