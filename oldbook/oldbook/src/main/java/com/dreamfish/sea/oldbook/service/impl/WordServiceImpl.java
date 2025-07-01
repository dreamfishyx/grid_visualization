package com.dreamfish.sea.oldbook.service.impl;

import com.dreamfish.sea.oldbook.dao.WordMapper;
import com.dreamfish.sea.oldbook.entity.Word;
import com.dreamfish.sea.oldbook.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 20:17
 */
@Service
public class WordServiceImpl implements WordService {
    @Autowired
    WordMapper wordMapper;

    @Value("${my.default.word}")
    String defaultWord;

    @Override
    public void addWord(Word word) {
        wordMapper.addWord(word);
    }

    @Override
    public Word getAWord(Integer userId) {
        List<Word> allWord = wordMapper.getAllWord(userId);
        //==使用默认的word==
        if (allWord == null) {
            Word word = new Word();
            word.setContent(defaultWord);
            return word;
        }
        //获取随机数
        int random = (int) (Math.random() * allWord.size());
        return allWord.get(random);
    }
}
