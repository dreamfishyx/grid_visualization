package com.dreamfish.sea.oldbook.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 13:46
 */
@Data
@NoArgsConstructor
public class Word {
    Integer wordId;
    Integer userId;
    String content;
}
