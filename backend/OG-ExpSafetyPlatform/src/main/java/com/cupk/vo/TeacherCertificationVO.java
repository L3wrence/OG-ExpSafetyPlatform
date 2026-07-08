package com.cupk.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TeacherCertificationVO {
    private Long id;
    private Long userId;
    private String userName;
    private String username;
    private String school;
    private String employeeNo;
    private String educationEmail;
    private String status;
    private String reviewComment;
    private LocalDateTime reviewTime;
    private LocalDateTime createTime;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
