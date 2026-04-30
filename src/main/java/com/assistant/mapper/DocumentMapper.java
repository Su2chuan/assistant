package com.assistant.mapper;

import com.assistant.entity.Document;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档 Mapper 接口
 *
 * @Mapper 标记这是一个 MyBatis Mapper，Spring 会自动生成实现类
 * 继承 BaseMapper<Document> 后，自动拥有以下方法（无需写 SQL）：
 * - insert(Document)          插入一条记录
 * - deleteById(id)            按 ID 删除
 * - updateById(Document)      按 ID 更新
 * - selectById(id)            按 ID 查询
 * - selectList(wrapper)       条件查询列表
 * - selectPage(page, wrapper) 分页查询
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}