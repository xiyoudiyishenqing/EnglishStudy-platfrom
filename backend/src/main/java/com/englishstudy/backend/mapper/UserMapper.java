package com.englishstudy.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.englishstudy.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
