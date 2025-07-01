package com.dreamfish.sea.oldbook.service;

import com.dreamfish.sea.oldbook.entity.Word;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 20:16
 */
public interface WordService {
    void addWord(Word word);

    Word getAWord(Integer userId);

}
