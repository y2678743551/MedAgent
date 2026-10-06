package com.springai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.springai.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {


    @Update("UPDATE schedule SET booked_slots = booked_slots + 1 "
            + "WHERE id = #{id} AND status = 1 AND booked_slots < total_slots")
    int claim(@Param("id") Long id);


    @Update("UPDATE schedule SET booked_slots = booked_slots - 1 "
            + "WHERE id = #{id} AND booked_slots > 0")
    int release(@Param("id") Long id);
}
