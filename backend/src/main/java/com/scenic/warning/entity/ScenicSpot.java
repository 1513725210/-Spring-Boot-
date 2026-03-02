package com.scenic.warning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 景区信息表
 */
@Data
@TableName("scenic_spot")
public class ScenicSpot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String province;

    private String city;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer maxCapacity;

    private Integer currentCount;

    private String level;

    private String imageUrl;

    private String openTime;

    private BigDecimal ticketPrice;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
