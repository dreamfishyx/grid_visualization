package com.dreamfish.sea.oldbook.service.impl;

import com.dreamfish.sea.oldbook.dao.DiaryMapper;
import com.dreamfish.sea.oldbook.entity.Diary;
import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.service.DiaryService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/20 15:52
 */
@Service
@Slf4j
public class DiaryServiceImpl implements DiaryService {
    @Autowired
    public DiaryMapper diaryMapper;

    @Override
    public void createDiary(Diary diary) {
        diaryMapper.createDiary(diary);
    }

    @Override
    public Diary getDiaryByDiary(Diary diary) {

        return diaryMapper.getDiaryById(diary);
    }

    @Override
    public PageInfo<Diary> getDiaryPageInfo(User user, Integer pageNum, Integer dataSize, Integer pageSize) {
        //分页
        PageHelper.startPage(pageNum, dataSize);
//        PageHelper.orderBy("column desc");// 根据 column 列降序排序
        List<Diary> diary = diaryMapper.getDiaryByUserId(user);
        PageInfo<Diary> pageInfo = new PageInfo<>(diary, pageSize);
        return pageInfo;
    }

    @Override
    public void deleteDiaryById(Diary diary) {
        diaryMapper.deleteDiaryById(diary);
    }

    @Override
    public int[] getDiaryCountByMonth(User user) {
        int[] ints = new int[12];


        List<Diary> diaries = diaryMapper.getDiaryCountByMonth(user);

        for (Diary diary : diaries) {
            ints[diary.getMonth() - 1] = diary.getCount();
        }
        return ints;
    }

    @Override
    public int[] getDiaryCountByMood(User user) {

        List<Diary> countByMood = diaryMapper.getDiaryCountByMood(user);

        int[] ints = new int[3];
        //===遍历并赋值===
        for (Diary diary : countByMood) {
            ints[diary.getMood()] = diary.getCount();
        }

        return ints;
    }

}
