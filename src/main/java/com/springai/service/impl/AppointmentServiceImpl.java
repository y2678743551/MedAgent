package com.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springai.entity.Appointment;
import com.springai.mapper.AppointmentMapper;
import com.springai.service.AppointmentService;
import org.springframework.stereotype.Service;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {
    @Override
    public Appointment getOne(Appointment appointment) {
        return query()
                .eq("username", appointment.getUsername())
                .eq("id_card", appointment.getIdCard())
                .eq("department", appointment.getDepartment())
                .eq("date", appointment.getDate())
                .eq("time", appointment.getTime())
                .one() ;

    }


}
