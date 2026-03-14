package com.yi.musiclisten.controller;

import com.yi.musiclisten.entity.User;
import com.yi.musiclisten.service.IUserService;
import com.yi.musiclisten.utils.EmailCodeUtil;
import com.yi.musiclisten.utils.JWTUtils;
import com.yi.musiclisten.utils.PasswordUtil;
import com.yi.musiclisten.utils.Result;
import com.yi.musiclisten.enums.ResponseEnum;
import com.yi.musiclisten.vo.LoginVo;
import com.yi.musiclisten.vo.RegisterVo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {

    @Autowired
    private IUserService userService;

    @Resource
    private EmailCodeUtil emailCodeUtil;

    @Resource
    private RedisTemplate redisTemplate;

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
        response.setHeader("token",token);
        return Result.success(ResponseEnum.SUCCESS,token);
    }

    @GetMapping("/sendEmailCode")
    public Result sendEmailCode(@RequestParam String email) {

        // 生成验证码
        String code = emailCodeUtil.generateCode();

        // 发送邮件
        emailCodeUtil.sendCodeEmail(email, code);

        // 存入 Redis（有效期 5 分钟）
        redisTemplate.opsForValue()
                .set("email:code:" + email, code, Duration.ofMinutes(5));

        return Result.success(ResponseEnum.SUCCESS,"验证码已发送，请查收邮箱");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterVo register) {
        User existingUser = userService.getByUsername(register.getUsername());

        // 读取 Redis 中的验证码
        String redisKey = "email:code:" + register.getEmail();
        String redisCode = (String) redisTemplate.opsForValue().get(redisKey);

        Result.checkParam(redisCode == null, "验证码已失效");
        Result.checkParam(!redisCode.equals(register.getCode()), "验证码错误");
        Result.checkParam(existingUser != null, "用户已存在");
        Result.checkParam(register.getPassword() == null || register.getPassword().trim().isEmpty(), "密码不能为空");


        User user = new User();
        user.setUsername(register.getUsername());
        user.setPassword(PasswordUtil.encrypt(register.getPassword()));
        user.setEmail(register.getEmail());
        user.setIsSinger(0); // 默认角色
        userService.save(user);

        return Result.success(ResponseEnum.SUCCESS, "注册成功");
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public Result<String> logout(HttpServletRequest request, HttpServletResponse response) {
        request.removeAttribute("token");
        response.setHeader("token", "");
        return Result.success(ResponseEnum.SUCCESS, "退出成功");
    }
}

