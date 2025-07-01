package com.dreamfish.sea.oldbook.dao;

import com.dreamfish.sea.oldbook.entity.Word;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 20:02
 */
public interface WordMapper {
    void addWord(Word word);

    List<Word> getAllWord(Integer userId);
}
