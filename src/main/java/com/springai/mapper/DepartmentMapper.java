package com.springai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springai.entity.Department;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
