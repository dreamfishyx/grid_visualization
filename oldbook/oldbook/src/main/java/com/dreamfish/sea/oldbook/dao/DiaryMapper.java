package com.dreamfish.sea.oldbook.dao;

import com.dreamfish.sea.oldbook.entity.Diary;
import com.dreamfish.sea.oldbook.entity.User;

import java.util.List;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/20 15:46
 */

public interface DiaryMapper {

    void createDiary(Diary diary);

    Diary getDiaryById(Diary diary);

    List<Diary> getDiaryByUserId(User user);

    void deleteDiaryById(Diary diary);

    List<Diary> getDiaryCountByMonth(User user);

    List<Diary> getDiaryCountByMood(User user);
}
