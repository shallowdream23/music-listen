package com.yi.musiclisten.controller;

import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.utils.JWTUtils;
import com.yi.musiclisten.utils.PasswordUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.vo.LoginVo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class LoginController {

    @Autowired
    private IUserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginVo login, HttpServletRequest request, HttpServletResponse response){
        User userInfo = userService.getByUsername(login.getUsername());
        Result.checkParam(userInfo==null,"用户为空");
        Result.checkParam(userInfo.getStatus()==1,"用户已被冻结，请联系管理员");

        //密码校验
        boolean matches = PasswordUtil.matches(login.getPassword(), userInfo.getPassword());
        Result.checkParam(!matches,"密码错误");

        //生成token
        Map<String,Object> claims= new HashMap<>();
        claims.put("userId",userInfo.getId());
        String token = JWTUtils.generateToken(claims);
        response.setHeader("Authorization",token);
        return Result.success(ResponseEnum.SUCCESS,token);
    }

    /**
     * 用户注册
     */

    @PostMapping("/register")
    public Result<String> register(@RequestParam String username, @RequestParam String password) {
        User existingUser = userService.getByUsername(username);

        if (existingUser != null) {
            return Result.fail(ResponseEnum.FAIL, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encrypt(password));
        user.setIsSinger(0); // 默认角色
        userService.save(user);

        return Result.success(ResponseEnum.SUCCESS, "注册成功");
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        request.removeAttribute("Authorization");
        response.setHeader("Authorization", "");
        return Result.success(ResponseEnum.SUCCESS, "退出成功");
    }
}

