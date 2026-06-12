package com.englishstudy.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.englishstudy.backend.entity.ResourceItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceMapper extends BaseMapper<ResourceItem> {
}
