package com.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.springai.entity.Department;
import com.springai.mapper.DepartmentMapper;
import com.springai.service.DepartmentService;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService{
}
