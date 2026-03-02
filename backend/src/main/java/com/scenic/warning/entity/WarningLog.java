package com.scenic.warning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报警历史记录表
 */
@Data
@TableName("warning_log")
public class WarningLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scenicId;

    private String scenicName;

    private String warningLevel;

    private Integer currentCount;

    private Integer maxCapacity;

    private BigDecimal thresholdPercent;

    private BigDecimal congestionRate;

    private String message;

    private Integer handled;

    private String handleUser;

    private LocalDateTime handleTime;

    private String handleRemark;

    private LocalDateTime warningTime;
    
    /**
     * AI生成的应急预案 (HTML片段)
     */
    private String plan;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
