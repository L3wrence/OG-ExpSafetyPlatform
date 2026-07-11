package com.cupk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoredFileVO {
    private String filePath;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
}
