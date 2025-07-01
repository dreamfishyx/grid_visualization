package com.dreamfish.sea.oldbook.controller;

import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.entity.Word;
import com.dreamfish.sea.oldbook.service.DiaryService;
import com.dreamfish.sea.oldbook.service.PlanService;
import com.dreamfish.sea.oldbook.service.WordService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 20:30
 */
@Controller
@Slf4j
public class HomeController {

    @Autowired
    WordService wordService;

    @Autowired
    DiaryService diaryService;


    @Autowired
    PlanService planService;

    @Value("${my.default.page-size}")
    public Integer pageSize = 7;

    @Value("${my.default.data-size}")
    public Integer dataSize = 5;


    @RequestMapping("/home")
    public String home(
            HttpSession session,
            @RequestParam(name = "pageNum", defaultValue = "1", required = false) Integer pageNum,
            @RequestParam(name = "mode", defaultValue = "undo", required = false) String mode,
            Model model) {
        User user = (User) session.getAttribute("user");
        //===获取word===
        Word aWord = wordService.getAWord(user.getUserId());
        model.addAttribute("word", aWord);

        //===获取日记信息===
        model.addAttribute("pageInfo", diaryService.getDiaryPageInfo(user, pageNum, dataSize, pageSize));
        // log.info("pageInfo: " + model.getAttribute("pageInfo"));

        //===获取日记统计信息===
        model.addAttribute("diaryCountByMonth", diaryService.getDiaryCountByMonth(user));
        model.addAttribute("diaryCountByMood", diaryService.getDiaryCountByMood(user));

        //===获取计划统计信息===
        model.addAttribute("planCount", planService.getPlanCount(user));

        //===获取计划相关信息===
        if ("did".equals(mode)) {
            model.addAttribute("planList", planService.getCompleteToday(user));
            model.addAttribute("mode", "did");
        } else if ("undo".equals(mode)) {
            model.addAttribute("planList", planService.getUnCompleteToday(user));
            model.addAttribute("mode", "undo");
        }

        return "home";
    }
}
