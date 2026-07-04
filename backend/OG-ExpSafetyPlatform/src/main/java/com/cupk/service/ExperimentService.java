package com.cupk.service;

import com.cupk.common.PageResult;
import com.cupk.dto.ExperimentCreateDTO;
import com.cupk.dto.ExperimentQueryDTO;
import com.cupk.dto.ExperimentStepDTO;
import com.cupk.dto.ExperimentUpdateDTO;
import com.cupk.pojo.Experiment;
import com.cupk.vo.ExperimentDetailVO;

import java.util.List;

public interface ExperimentService {
    Long create(ExperimentCreateDTO dto);
    void update(Long id, ExperimentUpdateDTO dto);
    void delete(Long id);
    void changeStatus(Long id, Integer status);
    void saveSteps(Long id, List<ExperimentStepDTO> steps);
    PageResult<Experiment> page(ExperimentQueryDTO dto);
    ExperimentDetailVO detail(Long id);
}
