package com.springai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springai.entity.Doctor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
    List<Doctor> getAvailableDoctors(Long departmentId, String date, String time);
}
