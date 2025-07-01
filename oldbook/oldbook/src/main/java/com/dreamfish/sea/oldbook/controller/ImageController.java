package com.dreamfish.sea.oldbook.controller;

/**
 * @Author: Dream fish
 * @Date: 2023/9/26 17:05
 * @Description: TODO
 * @Version: 1.0
 **/

import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.util.MyFileUtil;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class ImageController {
    @Value("${my.setting.img-location}")
    public String imgLocation = "img"; // 图片文件存放位置

    @Value("${my.setting.web-root}")
    public String webRoot = ""; // 图片文件存放位置

    @PostMapping("/upload-image")
    public Map<String, Object> uploadImage(@RequestParam("editormd-image-file") MultipartFile image, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        User user = (User) session.getAttribute("user");

        try {

            //String path = new File(ResourceUtils.getURL("classpath:").getPath()).getAbsolutePath();
            String path = System.getProperty("user.dir");
            String directory = path + MyFileUtil.getFileSeparator() + imgLocation + MyFileUtil.getFileSeparator();

            //===创建文件夹(不存在时)===
            File dir = new File(directory);
            if (!dir.exists()) {
                boolean mkdirs = dir.mkdirs();
            }
            String fileName = MyFileUtil.getPicName(user.getUserId());
            String imgUrl = webRoot + "/" + "img" + "/" + fileName;

            // 保存图片到服务器的指定位置
            image.transferTo(new File(directory + fileName));

            // 返回图片的URL
            response.put("success", 1);
            response.put("message", "上传成功");
            log.info("上传成功:" + imgUrl);
            response.put("url", imgUrl);
            log.info("(●—●)图片上传成功:" + imgUrl);
        } catch (IOException e) {
            log.error("上传失败", e);
            // 处理上传失败的情况
            response.put("success", "0");
            response.put("message", "上传失败");
        }
        return response;
    }

    //返回读取的图片内容
    @RequestMapping(value = "/img/{filename}", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public byte[] getPic(@PathVariable("filename") String filename) throws IOException {
        String path = System.getProperty("user.dir");
        String directory = path + MyFileUtil.getFileSeparator() + imgLocation + MyFileUtil.getFileSeparator();

        log.info("img_path: " + directory + filename);

        File file = new File(directory + filename);
        if (!file.exists()) {
            log.error("图片不存在");
            return null;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

}

