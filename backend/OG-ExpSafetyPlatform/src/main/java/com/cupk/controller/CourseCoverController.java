package com.cupk.controller;

import com.cupk.service.CourseService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class CourseCoverController {
    private final CourseService courseService;
    public CourseCoverController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping("/api/public/courses/{courseId}/cover")
    public ResponseEntity<UrlResource> cover(@PathVariable Long courseId) throws MalformedURLException {
        Path path = courseService.coverFilePath(courseId);
        String type;
        try { type = Files.probeContentType(path); } catch (Exception e) { type = MediaType.APPLICATION_OCTET_STREAM_VALUE; }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type))
                .body(new UrlResource(path.toUri()));
    }
}
