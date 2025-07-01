package com.dreamfish.sea.oldbook.controller;

import com.dreamfish.sea.oldbook.entity.User;
import com.dreamfish.sea.oldbook.service.UserService;
import com.dreamfish.sea.oldbook.util.LoginEnv;
import com.dreamfish.sea.oldbook.util.MD5;
import com.dreamfish.sea.oldbook.util.RegisterEnv;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;

/**
 * @author Dream fish
 * @version 1.0
 * @description: TODO
 * @date 2023/11/13 14:18
 */
@Controller
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    UserService userService;

    @RequestMapping("/login") //登录
    public String login(@Validated(LoginEnv.class) User user, BindingResult result, Model model, HttpSession session) {

        //===存储错误信息===
        HashMap<String, String> map = new HashMap<>();

        if (result.hasErrors()) { //通过jsr303校验,获取错误信息
            result.getFieldErrors().forEach(error -> {  //===遍历错误===
                log.info("{}:{}", error.getObjectName(), error.getDefaultMessage());
                map.put(error.getField(), error.getDefaultMessage());
            });
            model.addAttribute("errors", map); //将错误信息传递给前端
            //===数据回显===
            model.addAttribute("userName", user.getUserName());

            return "login"; //返回登录页面
        }

        log.info("(●—●)" + user.getUserName() + "登录," + "密码为" + MD5.encrypt(user.getPassword()));

        User userByName = userService.getUserByName(user.getUserName(), MD5.encrypt(user.getPassword()));
        if (userByName == null) {
            map.put("password", "用户名或密码错误");
            model.addAttribute("errors", map);
            model.addAttribute("userName", user.getUserName());
            return "login";
        }


        //===保存用户信息===
        session.setAttribute("user", userByName);

        return "forward:/home";
        //MD5:d76c4747305e45e7b917e4a880a4ee01()
    }

    @RequestMapping("/register") //注册
    public String register(@Validated(RegisterEnv.class) User user, BindingResult result, @RequestParam(value = "confirm", defaultValue = "false") Boolean confirm, Model model) {
        HashMap<String, String> map = new HashMap<>();
        //===校验参数===
        if (result.hasErrors()) { //通过jsr303校验,获取错误信息
            result.getFieldErrors().forEach(error -> {  //===遍历错误===
                log.info("{}:{}", error.getObjectName(), error.getDefaultMessage());
                map.put(error.getField(), error.getDefaultMessage());
            });
            result.getAllErrors().forEach(error -> {
                if (error.getObjectName().equals("user") & !map.containsKey("matchingPassword")) {
                    log.info("(●—●){}:{}", "matchingPassword", error.getDefaultMessage());
                    map.put("matchingPassword", error.getDefaultMessage());
                }
            });
            model.addAttribute("errors", map); //将错误信息传递给前端
            model.addAttribute("userName", user.getUserName());
            return "register"; //返回注册页面
        }
        //===校验用户名是否存在===
        Boolean exist = userService.exist(user.getUserName());
        if (exist) {
            map.put("userName", "用户名已存在");
            model.addAttribute("userName", user.getUserName());
            model.addAttribute("errors", map); //将错误信息传递给前端
            return "register";
        }
        //===校验是否同意协议===略(confirm参数)
        //===注册===
        user.setPassword(MD5.encrypt(user.getPassword()));
        Boolean res = userService.addUser(user);
        log.info("(●—●)注册结果:{}", res);
        return "redirect:/to/login";
    }

    @RequestMapping("/logout") //登出
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:/to/login";
    }

}
