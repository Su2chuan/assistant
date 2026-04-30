package com.assistant.controller;

import com.assistant.dto.DocumentDTO;
import com.assistant.entity.Document;
import com.assistant.service.DocumentService;
import com.assistant.util.RateLimiter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final RateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<Document> create(@Valid @RequestBody DocumentDTO dto) {
        Document document = documentService.create(dto);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/upload")
    public ResponseEntity<Document> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "summary", required = false) String summary) {

        DocumentDTO dto = new DocumentDTO();
        dto.setCategoryId(categoryId);
        dto.setTags(tags);
        dto.setSummary(summary);

        Document document = documentService.upload(file, dto);
        return ResponseEntity.ok(document);
    }

    @PostMapping("/import-url")
    public ResponseEntity<Document> importUrl(@RequestBody Map<String, Object> body, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        if (!rateLimiter.tryAcquire("rate:import:" + ip, 5, 60)) {
            return ResponseEntity.status(429).build();
        }

        String url = (String) body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Long categoryId = body.get("categoryId") != null ? Long.valueOf(body.get("categoryId").toString()) : null;
        String tags = (String) body.get("tags");

        Document document = documentService.importFromUrl(url, categoryId, tags);
        return ResponseEntity.ok(document);
    }

    @GetMapping
    public ResponseEntity<Page<Document>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Document> result = documentService.list(categoryId, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> getById(@PathVariable Long id) {
        Document document = documentService.getById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(document);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/category")
    public ResponseEntity<Void> updateCategory(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long categoryId = body.get("categoryId");
        documentService.updateCategory(id, categoryId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestParam String keyword) {
        List<Document> documents = documentService.search(keyword);
        return ResponseEntity.ok(documents);
    }
}