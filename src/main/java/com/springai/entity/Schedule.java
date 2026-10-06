package com.springai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long doctorId;
    private Long departmentId;
    /** 出诊日期 yyyy-MM-dd，与表里的 VARCHAR(10) 一致 */
    private String workDate;
    /** 时段：上午 / 下午 */
    private String timeSlot;
    /** 放号总数 */
    private Integer totalSlots;
    /** 已挂号数 */
    private Integer bookedSlots;
    /** 1正常出诊 0停诊 */
    private Integer status;
    /** 乐观锁备用字段，暂未启用（当前用原子 UPDATE 扣号） */
    private Integer version;
}
