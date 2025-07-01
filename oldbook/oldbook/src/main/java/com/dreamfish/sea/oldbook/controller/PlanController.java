package com.dreamfish.sea.oldbook.controller;

import com.dreamfish.sea.oldbook.component.Result;
import com.dreamfish.sea.oldbook.entity.Plan;
import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.service.PlanService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/24 22:14
 */
@Controller
@Slf4j
@RequestMapping("/plan")
public class PlanController {
    @Autowired
    public PlanService planService;

    @RequestMapping("/create")
    @ResponseBody
    public Object createPlan(Plan plan, HttpSession session) {
        User user = (User) session.getAttribute("user");

        log.info("(●—●)new a plan,content:" + plan.getContent());


        if (plan.getContent().isEmpty()) {
            plan.setContent("ヽ(ー_ー)ノ:皇帝的计划(无内容)");
        }


        if (plan.getDeadline() == null) {
            //默认截止时间为明天
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            plan.setDeadline(tomorrow.atStartOfDay());
        }

        plan.setUserId(user.getUserId());
        planService.createPlan(plan);
        return Result.success();
    }

    @RequestMapping("/complete")
    @ResponseBody
    public Object completePlan(Plan plan, HttpSession session) {
        User user = (User) session.getAttribute("user");
        plan.setUserId(user.getUserId());

        //===设置完成时间为当前时间===
        plan.setCompleteTime(LocalDateTime.now());
        planService.completePlan(plan);

        return Result.success();
    }

    @RequestMapping("/delete")
    public String deletePlan(Plan plan, HttpSession session) {
        User user = (User) session.getAttribute("user");
        plan.setUserId(user.getUserId());

        planService.deletePlan(plan);
        return "redirect:/home";
    }
}
