package com.springai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springai.entity.Appointment;
import com.springai.entity.Doctor;
import com.springai.entity.Schedule;

import java.util.List;

public interface DoctorService extends IService<Doctor> {


    Schedule getAvailableSchedule(String departmentName, String date, String time, String doctorName);


    Doctor getDoctorByName(String doctorName, String departmentName);


    boolean registerIfAvailable(Appointment appointment);



    List<Doctor> getDoctorByDepartment(String departmentName, String date, String time);
}
