package com.dreamfish.sea.oldbook.service;

import com.dreamfish.sea.oldbook.entity.Plan;
import com.dreamfish.sea.oldbook.entity.User;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/24 22:12
 */
public interface PlanService {
    void createPlan(Plan plan);

    List<Plan> getUnCompleteToday(User user);


    void completePlan(Plan plan);

    int[] getPlanCount(User user);

    List<Plan> getCompleteToday(User user);

    void deletePlan(Plan plan);
}