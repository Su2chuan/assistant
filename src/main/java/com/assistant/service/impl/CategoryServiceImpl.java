package com.assistant.service.impl;

import com.assistant.entity.Category;
import com.assistant.entity.Document;
import com.assistant.mapper.CategoryMapper;
import com.assistant.mapper.DocumentMapper;
import com.assistant.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final DocumentMapper documentMapper;

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public Category create(String name) {
        Category category = new Category();
        category.setName(name);
        categoryMapper.insert(category);
        log.info("创建分类: {}", name);
        return category;
    }

    @Override
    @Cacheable(value = "categories")
    public List<Category> list() {
        return categoryMapper.selectList(null);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) {
        // 不允许删除 ID=1 的默认分类
        if (id == 1) {
            throw new IllegalArgumentException("默认分类不能删除");
        }

        // 检查是否有关联文档
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getCategoryId, id);
        long count = documentMapper.selectCount(wrapper);
        if (count > 0) {
            // 将关联文档移到默认分类
            Document updateDoc = new Document();
            updateDoc.setCategoryId(1L);
            documentMapper.update(updateDoc, wrapper);
            log.info("删除分类 {}，{} 个文档移至默认分类", id, count);
        }

        categoryMapper.deleteById(id);
        log.info("删除分类: id={}", id);
    }
}
