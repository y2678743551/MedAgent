package com.springai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.springai.entity.Schedule;

public interface ScheduleService extends IService<Schedule> {


    boolean claim(Long scheduleId);


    boolean release(Long scheduleId);
}
