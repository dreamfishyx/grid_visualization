package com.dreamfish.sea.oldbook.dao;

import com.dreamfish.sea.oldbook.entity.Plan;
import com.dreamfish.sea.oldbook.entity.User;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/24 22:09
 */
public interface PlanMapper {
    void createPlan(Plan plan);

    List<Plan> getUnCompleteToday(User user);

    List<Plan> getCompleteToday(User user);

    void completePlan(Plan plan);

    List<Plan> getPlanCount(User user);


    void deletePlanById(Plan plan);

}
