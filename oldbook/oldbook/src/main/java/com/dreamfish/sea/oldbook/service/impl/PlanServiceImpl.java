package com.dreamfish.sea.oldbook.service.impl;

import com.dreamfish.sea.oldbook.dao.PlanMapper;
import com.dreamfish.sea.oldbook.entity.Plan;
import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/24 22:13
 */
@Service
public class PlanServiceImpl implements PlanService {
    @Autowired
    public PlanMapper planMapper;

    @Override
    public void createPlan(Plan plan) {
        planMapper.createPlan(plan);
    }

    @Override
    public List<Plan> getUnCompleteToday(User user) {
        return planMapper.getUnCompleteToday(user);
    }

    @Override
    public void completePlan(Plan plan) {
        planMapper.completePlan(plan);
    }

    @Override
    public int[] getPlanCount(User user) {
        int[] ints = new int[12];
        List<Plan> planCount = planMapper.getPlanCount(user);

        for (Plan plan : planCount) {
            ints[plan.getMonth() - 1] = plan.getCount();
        }
        return ints;
    }

    @Override
    public List<Plan> getCompleteToday(User user) {
        return planMapper.getCompleteToday(user);
    }

    @Override
    public void deletePlan(Plan plan) {
        planMapper.deletePlanById(plan);
    }
}
