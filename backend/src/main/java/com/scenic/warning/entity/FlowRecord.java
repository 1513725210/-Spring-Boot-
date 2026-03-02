package com.scenic.warning.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客流流水表
 */
@Data
@TableName("flow_record")
public class FlowRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scenicId;

    private Integer currentCount;

    private Integer inCount;

    private Integer outCount;

    private BigDecimal congestionRate;

    private LocalDateTime recordTime;

    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
