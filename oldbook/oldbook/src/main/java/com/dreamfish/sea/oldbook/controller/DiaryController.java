package com.dreamfish.sea.oldbook.controller;

import com.dreamfish.sea.oldbook.component.Result;
import com.dreamfish.sea.oldbook.entity.Diary;
import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.entity.Word;
import com.dreamfish.sea.oldbook.service.DiaryService;
import com.dreamfish.sea.oldbook.service.WordService;
import com.dreamfish.sea.oldbook.util.MyFileUtil;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/20 15:20
 */
@Controller
@Slf4j
@RequestMapping("/diary")
public class DiaryController {
    @Autowired
    public DiaryService diaryService;

    @Autowired
    public WordService wordService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${my.setting.diary-location}")
    public String diaryLocation = "diary"; // 日记文件存放位置


    @RequestMapping("/save")
    @ResponseBody
    public Object save(@Validated Diary diary, BindingResult result, Model model, HttpSession session) {

        log.info("diary: " + diary.toString());

        //===校验参数===
        if (result.hasErrors()) {
            Result<Object> res = Result.error();
            HashMap<String, String> map = new HashMap<>();
            result.getFieldErrors().forEach(error -> {  //===遍历错误===
                log.info("{}:{}", error.getObjectName(), error.getDefaultMessage());
                map.put(error.getField(), error.getDefaultMessage());
            });
            res.setData(map);
            return res; //返回错误状态码以及错误信息
        }

        //===将content写入文件===
        Integer uid = ((User) session.getAttribute("user")).getUserId();
        Writer out = null;
        String link = "";
        try {
            //String path = new File(ResourceUtils.getURL("classpath:").getPath()).getAbsolutePath();
            String path = System.getProperty("user.dir");
            String directory = path + MyFileUtil.getFileSeparator() + diaryLocation + MyFileUtil.getFileSeparator();
            //===创建文件夹(不存在时)===
            File dir = new File(directory);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();
            }
            log.info("directory: " + directory);
            link = MyFileUtil.getFileName(uid);
            File file = new File(directory + link);
            out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            out.write(diary.getContent());
            out.close();
        } catch (Exception e) {
            log.error("保存出错: " + e.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("关闭流出错: " + e.getMessage());
                }
            }
        }

        //===将文件路径写入数据库===
        diary.setLink(link);
        diary.setUserId(uid);
        diaryService.createDiary(diary);

        return Result.success(); //返回成功状态码
    }

    @RequestMapping("/show")
    public String show(Integer diaryId, Model model, HttpSession session) throws RuntimeException {

        User user = (User) session.getAttribute("user");

        Diary d = new Diary();
        d.setDiaryId(diaryId);
        d.setUserId(user.getUserId());

        Diary diary = diaryService.getDiaryByDiary(d);
        if (diary == null || !diary.getUserId().equals(user.getUserId())) {
            return "redirect:/home";
        }

        log.info("=========>diaryPath: " + diary.getLink());

        //===通过link读取文件===
        StringBuilder content;
        InputStreamReader reader = null;
        BufferedReader br = null;
        try {
            // String path = new File(ResourceUtils.getURL("classpath:").getPath()).getAbsolutePath();
            String path = System.getProperty("user.dir");
            log.info("============path: " + path);

            String location = path + MyFileUtil.getFileSeparator() + diaryLocation + MyFileUtil.getFileSeparator() + diary.getLink();

            log.info("============location: " + location);

            //读取文件
            File file = new File(location);
            reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            br = new BufferedReader(reader);
            String line = "";
            content = new StringBuilder();
            while (true) {
                try {
                    if ((line = br.readLine()) == null) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                content.append(line).append("\n");
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
//            log.error("读取文件出错: ",e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
//        log.info("content: " + content.toString());
        diary.setContent(content.toString());
        model.addAttribute("diary", diary);

        //===获取word===
        Word aWord = wordService.getAWord(user.getUserId());
        model.addAttribute("word", aWord);

        return "show";
    }

    @RequestMapping("/to/create")
    public String toCreate(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        //===获取word===
        Word aWord = wordService.getAWord(user.getUserId());
        model.addAttribute("word", aWord);

        return "create";
    }

    @RequestMapping("/delete")
    public String delete(Integer diaryId, HttpSession session) {
        User user = (User) session.getAttribute("user");

        Diary d = new Diary();
        d.setDiaryId(diaryId);

        diaryService.deleteDiaryById(d);

        return "redirect:/home";
    }

//    @RequestMapping("/byMonth")
//    @ResponseBody
//    public Object statistics(
//            Model model,
//            HttpSession session
//    ) {
//        User user = (User) session.getAttribute("user");
//        int[] diaryCountByMonth = diaryService.getDiaryCountByMonth(user);
//        return Result.success(diaryCountByMonth);
//    }
//
//    @RequestMapping("/byMood")
//    @ResponseBody
//    public Object statisticsByMood(
//            Model model,
//            HttpSession session
//    ) {
//        User user = (User) session.getAttribute("user");
//        int[] diaryCountByMood = diaryService.getDiaryCountByMood(user);
//        return Result.success(diaryCountByMood);
//    }
}
