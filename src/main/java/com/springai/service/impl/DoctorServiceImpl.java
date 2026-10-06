package com.springai.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springai.entity.Appointment;
import com.springai.entity.Department;
import com.springai.entity.Doctor;
import com.springai.entity.Schedule;
import com.springai.mapper.DoctorMapper;
import com.springai.service.DepartmentService;
import com.springai.service.DoctorService;
import com.springai.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private DepartmentService departmentService;

    @Override
    public Schedule getAvailableSchedule(String departmentName, String date, String time, String doctorName) {
        if (!StringUtils.hasText(departmentName) || !StringUtils.hasText(date) || !StringUtils.hasText(time)) {
            return null;
        }
        Department department = departmentService.query()
                .eq("name", departmentName)
                .last("LIMIT 1")
                .one();
        if (department == null) {
            return null;
        }

        QueryChainWrapper<Schedule> wrapper = scheduleService.query()
                .eq("department_id", department.getId())
                .eq("work_date", date)
                .eq("time_slot", time)
                .eq("status", 1)
                .apply("booked_slots < total_slots");
        if (StringUtils.hasText(doctorName)) {
            Doctor doctor = getDoctorByName(doctorName, departmentName);
            if (doctor == null) {
                return null;
            }
            wrapper.eq("doctor_id", doctor.getId());
        }

        return wrapper.orderByAsc("booked_slots").last("LIMIT 1").one();
    }

    @Override
    public Doctor getDoctorByName(String doctorName, String departmentName) {

        if (!StringUtils.hasText(doctorName) || !StringUtils.hasText(departmentName)) {
            return null;
        }
        Department department = departmentService.query()
                .eq("name", departmentName)
                .last("LIMIT 1")
                .one();
        if (department == null) {
            return null;
        }
        return query()
                .eq("name", doctorName)
                .eq("department_id", department.getId())
                .eq("enabled", 1)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public boolean registerIfAvailable(Appointment appointment) {
        Schedule schedule = getAvailableSchedule(
                appointment.getDepartment(), appointment.getDate(), appointment.getTime(), appointment.getDoctorName());
        if (schedule == null) {
            return false;
        }

        if (!scheduleService.claim(schedule.getId())) {
            return false;
        }
        appointment.setScheduleId(schedule.getId());

        if (!StringUtils.hasText(appointment.getDoctorName())) {
            Doctor doctor = getById(schedule.getDoctorId());
            if (doctor != null) {
                appointment.setDoctorName(doctor.getName());
            }
        }
        return true;
    }

    @Override
    public List<Doctor> getDoctorByDepartment(String departmentName, String date, String time) {
        if (!StringUtils.hasText(departmentName)) {
            return null;
        }
        Department department = departmentService.query()
                .eq("name", departmentName)
                .last("LIMIT 1")
                .one();
        if (department == null) {
            return null;
        }

        return baseMapper.getAvailableDoctors(department.getId(), date,time);
    }




}
