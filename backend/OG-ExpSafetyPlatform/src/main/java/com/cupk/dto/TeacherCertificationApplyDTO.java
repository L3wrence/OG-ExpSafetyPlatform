package com.cupk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TeacherCertificationApplyDTO {
    @NotBlank(message = "所属学校不能为空")
    @Size(max = 120, message = "所属学校不能超过120字")
    private String school;
    @NotBlank(message = "工号不能为空")
    @Size(max = 80, message = "工号不能超过80字")
    private String employeeNo;
    @NotBlank(message = "教育邮箱不能为空")
    @Email(message = "教育邮箱格式不正确")
    @Size(max = 120, message = "教育邮箱不能超过120字")
    private String educationEmail;
}
