package com.assistant.service;

import com.assistant.dto.DocumentDTO;
import com.assistant.entity.Document;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    Document create(DocumentDTO dto);

    Document upload(MultipartFile file, DocumentDTO dto);

    Document importFromUrl(String url, Long categoryId, String tags);

    Page<Document> list(Long categoryId, int page, int size);

    Document getById(Long id);

    void delete(Long id);

    void updateCategory(Long id, Long categoryId);

    List<Document> search(String keyword);
}
