package com.assistant.service.impl;

import com.assistant.dto.DocumentDTO;
import com.assistant.entity.Document;
import com.assistant.mapper.DocumentMapper;
import com.assistant.service.DocumentService;
import com.assistant.util.FileParserUtil;
import com.assistant.util.TextChunker;
import com.assistant.util.UrlFetcher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentMapper documentMapper;
    private final VectorStore vectorStore;

    @Override
    public Document create(DocumentDTO dto) {
        Document document = new Document();
        document.setTitle(dto.getTitle());
        document.setContent(dto.getContent());
        document.setSummary(dto.getSummary());
        document.setCategoryId(dto.getCategoryId() != null ? dto.getCategoryId() : 1L);
        document.setTags(dto.getTags());
        document.setSourceUrl(dto.getSourceUrl());
        document.setCreatedAt(LocalDateTime.now());

        documentMapper.insert(document);

        if (dto.getContent() != null && !dto.getContent().isBlank()) {
            indexToVectorStore(document);
        }

        return document;
    }

    @Override
    public Document upload(MultipartFile file, DocumentDTO dto) {
        String content;
        try {
            content = FileParserUtil.parse(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("文件解析失败: " + e.getMessage(), e);
        }

        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            dto.setTitle(file.getOriginalFilename());
        }

        Document document = new Document();
        document.setTitle(dto.getTitle());
        document.setContent(content);
        document.setSummary(dto.getSummary());
        document.setCategoryId(dto.getCategoryId() != null ? dto.getCategoryId() : 1L);
        document.setTags(dto.getTags());
        document.setSourceUrl(dto.getSourceUrl());
        document.setFilePath(file.getOriginalFilename());
        document.setCreatedAt(LocalDateTime.now());

        documentMapper.insert(document);
        log.info("文档已保存: id={}, title={}", document.getId(), document.getTitle());

        indexToVectorStore(document);

        return document;
    }

    @Override
    public Document importFromUrl(String url, Long categoryId, String tags) {
        UrlFetcher.FetchResult result = UrlFetcher.fetch(url);

        Document document = new Document();
        document.setTitle(result.title());
        document.setContent(result.content());
        document.setCategoryId(categoryId != null ? categoryId : 1L);
        document.setTags(tags);
        document.setSourceUrl(url);
        document.setCreatedAt(LocalDateTime.now());

        documentMapper.insert(document);
        log.info("URL 导入文档已保存: id={}, title={}, url={}", document.getId(), document.getTitle(), url);

        if (!result.content().isBlank() && !result.content().contains("抓取失败") && !result.content().contains("未能提取")) {
            indexToVectorStore(document);
        }

        return document;
    }

    @Override
    public Page<Document> list(Long categoryId, int page, int size) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(Document::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Document::getCreatedAt);
        return documentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Document getById(Long id) {
        return documentMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        documentMapper.deleteById(id);
    }

    @Override
    public void updateCategory(Long id, Long categoryId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }
        document.setCategoryId(categoryId != null ? categoryId : 1L);
        documentMapper.updateById(document);
    }

    @Override
    public List<Document> search(String keyword) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Document::getTitle, keyword)
                .or()
                .like(Document::getContent, keyword)
                .orderByDesc(Document::getCreatedAt);
        return documentMapper.selectList(wrapper);
    }

    private void indexToVectorStore(Document document) {
        try {
            List<String> chunks = TextChunker.chunk(document.getContent());
            log.info("文档分块完成: id={}, 总块数={}", document.getId(), chunks.size());

            List<org.springframework.ai.document.Document> vectorDocs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("docId", document.getId());
                metadata.put("title", document.getTitle());
                metadata.put("categoryId", document.getCategoryId());
                metadata.put("chunkIndex", i);

                vectorDocs.add(new org.springframework.ai.document.Document(chunks.get(i), metadata));
            }

            vectorStore.add(vectorDocs);
            log.info("文档已向量化存入 Qdrant: id={}, 块数={}", document.getId(), vectorDocs.size());

        } catch (Exception e) {
            log.warn("文档向量化失败: id={}, error={}", document.getId(), e.getMessage());
        }
    }
}