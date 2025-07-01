package com.dreamfish.sea.oldbook.controller;

import com.dreamfish.sea.oldbook.util.VerifyCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/29 16:07
 */
@Controller
@Slf4j
@RequestMapping("/verifyCode")
public class VerifyCodeController {

    @RequestMapping("/get")
    public void verifyCode(
            HttpSession session,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        int width = 200;
        int height = 69;
        BufferedImage verifyImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        String code = VerifyCode.drawRandomText(width, height, verifyImg);  // 获取验证码,并将验证码绘制到图片上

        request.getSession().setAttribute("code", code);  // 将验证码存入session

        response.setContentType("image/png");
        response.setHeader("Pragma", "No-cache");
        try {
            OutputStream os = response.getOutputStream();  // 输出流
            ImageIO.write(verifyImg, "png", os);  // 输出验证码
            os.flush();
            os.close();
        } catch (IOException e) {
            // e.printStackTrace();
            log.error("验证码生成失败", e);

        }
    }

}
