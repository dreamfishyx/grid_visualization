package com.dreamfish.sea.oldbook.util;

/**
 * @author Dream fish
 * @version 1.0
 * @description: 生成文件名、图片名
 * @date 2023/11/20 15:36
 */
public class MyFileUtil {

    public static String getFileName(Integer uid) {
        return uid + "_" + getRoot() + ".md";
    }

    public static String getPicName(Integer uid) {
        return uid + "_" + getRoot() + ".jpg";
    }

    public static String getRoot() {
        //===获取三位随机数===
        int random = (int) (Math.random() * 900) + 100;
        //===获取当前时间戳===
        long time = System.currentTimeMillis();
        //====拼接文件名===
        return random + "_" + time;
    }

    public static String getFileSeparator() {
        return System.getProperty("file.separator");
    }
}
