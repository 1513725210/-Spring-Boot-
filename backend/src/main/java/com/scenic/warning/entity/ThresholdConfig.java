package com.scenic.warning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警阈值配置表
 */
@Data
@TableName("threshold_config")
public class ThresholdConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scenicId;

    private BigDecimal yellowPercent;

    private BigDecimal redPercent;

    private Integer enableWarning;

    private String notifyMethod;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
