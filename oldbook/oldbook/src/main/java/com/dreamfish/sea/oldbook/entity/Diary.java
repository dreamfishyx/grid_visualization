package com.dreamfish.sea.oldbook.entity;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.sql.Date;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 13:41
 */
@Data
public class Diary {

    Integer diaryId;

    Integer userId;
    @NotEmpty(message = "标题不能为空")
    String title;
    String link;
    @NotEmpty(message = "天气不能为空")
    String weather;
    String content;
    Integer mood;
    Date createTime;
    Integer month; // 月份
    Integer count; // 该月日记数量

}
