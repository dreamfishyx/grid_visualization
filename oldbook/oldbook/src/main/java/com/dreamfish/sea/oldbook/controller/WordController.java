package com.dreamfish.sea.oldbook.controller;

import com.dreamfish.sea.oldbook.component.Result;
import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.entity.Word;
import com.dreamfish.sea.oldbook.service.WordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 20:10
 */
@RequestMapping("/word")
@Controller
public class WordController {
    @Autowired
    WordService wordService;

    @RequestMapping("/create")
    @ResponseBody
    public Object addWord(Word word, HttpSession session) {
        User user = (User) session.getAttribute("user");

        word.setUserId(user.getUserId());
        wordService.addWord(word);
        return Result.success();
    }

    @ResponseBody
    @RequestMapping("/getOne")
    public Object getWord(String word, HttpSession session) {
        User user = (User) session.getAttribute("user");
        //===获取word===
        Word aWord = wordService.getAWord(user.getUserId());
        return Result.success(aWord);
    }

}
