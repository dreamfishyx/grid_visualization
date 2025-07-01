package com.dreamfish.sea.oldbook.entity;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 13:43
 */
@Data
public class Plan {
    Integer planId;
    Integer userId;
    String content;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    LocalDateTime deadline;
    Integer complete;
    LocalDateTime completeTime;

    Integer month; // 月份
    Integer count; // 该月完成计划数量
}
