package com.assistant.service;

import com.assistant.entity.Category;

import java.util.List;

public interface CategoryService {

    Category create(String name);

    List<Category> list();

    void delete(Long id);
}
