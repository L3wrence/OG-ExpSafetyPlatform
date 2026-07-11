package com.cupk.controller;

import com.cupk.common.RequirePermission;
import com.cupk.service.ResourceService;
import com.cupk.service.ResourceSubmissionService;
import com.cupk.service.ExperimentService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final ResourceService resourceService;
    private final ResourceSubmissionService submissionService;
    private final ExperimentService experimentService;

    public FileController(ResourceService resourceService, ResourceSubmissionService submissionService,
                          ExperimentService experimentService) {
        this.resourceService = resourceService;
        this.submissionService = submissionService;
        this.experimentService = experimentService;
    }

    @GetMapping("/experiment-steps/{stepId}")
    @RequirePermission("experiment:view")
    public ResponseEntity<UrlResource> experimentStep(@PathVariable Long stepId) throws MalformedURLException {
        return inline(experimentService.stepFilePath(stepId));
    }

    @GetMapping("/resource-submissions/{submissionId}")
    public ResponseEntity<UrlResource> submission(@PathVariable Long submissionId) throws MalformedURLException {
        Path path = submissionService.filePath(submissionId);
        return inline(path);
    }

    private ResponseEntity<UrlResource> inline(Path path) throws MalformedURLException {
        UrlResource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(probeContentType(path)))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(path.getFileName().toString()).build().toString())
                .body(resource);
    }

    @GetMapping("/resources/{resourceId}")
    @RequirePermission("resource:view")
    public ResponseEntity<UrlResource> resource(@PathVariable Long resourceId) throws MalformedURLException {
        Path path = resourceService.resourceFilePath(resourceId);
        UrlResource resource = new UrlResource(path.toUri());
        String contentType = probeContentType(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(path.getFileName().toString())
                        .build()
                        .toString())
                .body(resource);
    }

    private String probeContentType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }
}
