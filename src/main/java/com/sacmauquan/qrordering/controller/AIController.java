package com.sacmauquan.qrordering.controller;

import com.sacmauquan.qrordering.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping(value = "/analyze-dish", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> analyzeDish(@RequestParam("file") MultipartFile file) throws IOException {
        String result = aiService.analyzeDishImage(file);
        return ResponseEntity.ok(result);
    }
}
