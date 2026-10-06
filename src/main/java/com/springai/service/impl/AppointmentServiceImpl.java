package com.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springai.entity.Appointment;
import com.springai.mapper.AppointmentMapper;
import com.springai.service.AppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {


    @Override
    public Appointment getOne(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        boolean hasUsername = StringUtils.hasText(appointment.getUsername());
        boolean hasIdCard = StringUtils.hasText(appointment.getIdCard());
        boolean hasDepartment = StringUtils.hasText(appointment.getDepartment());
        boolean hasDate = StringUtils.hasText(appointment.getDate());
        boolean hasTime = StringUtils.hasText(appointment.getTime());

        if (!hasUsername && !hasIdCard && !hasDepartment && !hasDate && !hasTime) {
            return null;
        }
        return query()
                .eq(hasUsername, "username", appointment.getUsername())
                .eq(hasIdCard, "id_card", appointment.getIdCard())
                .eq(hasDepartment, "department", appointment.getDepartment())
                .eq(hasDate, "date", appointment.getDate())
                .eq(hasTime, "time", appointment.getTime())
                .last("LIMIT 1")
                .one();
    }
}
