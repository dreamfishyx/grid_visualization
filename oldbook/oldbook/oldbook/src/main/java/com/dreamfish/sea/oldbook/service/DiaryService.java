package com.dreamfish.sea.oldbook.service;

import com.dreamfish.sea.oldbook.entity.Diary;
import com.dreamfish.sea.oldbook.entity.User;
import com.github.pagehelper.PageInfo;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/20 15:51
 */
public interface DiaryService {
    void createDiary(Diary diary);

    Diary getDiaryByDiary(Diary diary);

    PageInfo<Diary> getDiaryPageInfo(User user, Integer pageNum, Integer dateSize, Integer pageSize);

    void deleteDiaryById(Diary diary);

    int[] getDiaryCountByMonth(User user);

    int[] getDiaryCountByMood(User user);
}
