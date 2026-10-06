package com.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springai.entity.Schedule;
import com.springai.mapper.ScheduleMapper;
import com.springai.service.ScheduleService;
import org.springframework.stereotype.Service;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Override
    public boolean claim(Long scheduleId) {
        if (scheduleId == null) {
            return false;
        }

        return baseMapper.claim(scheduleId) == 1;
    }

    @Override
    public boolean release(Long scheduleId) {
        if (scheduleId == null) {
            return false;
        }
        return baseMapper.release(scheduleId) == 1;
    }
}
