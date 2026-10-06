package com.springai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String location;
    private String description;
    private Integer enabled;
}
