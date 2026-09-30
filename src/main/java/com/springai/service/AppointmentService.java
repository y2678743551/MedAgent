package com.springai.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.springai.entity.Appointment;
import org.springframework.stereotype.Service;


public interface AppointmentService extends IService<Appointment> {

    Appointment getOne(Appointment appointment);
}
