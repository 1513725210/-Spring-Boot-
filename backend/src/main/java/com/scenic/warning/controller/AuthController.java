package com.scenic.warning.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scenic.warning.common.Result;
import com.scenic.warning.entity.SysUser;
import com.scenic.warning.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证登录 Controller
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }

        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (user.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error("密码错误");
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        sysUserService.updateById(user);

        // 简化的Token（生产环境应使用JWT）
        Map<String, Object> result = new HashMap<>();
        result.put("token", "token_" + user.getId() + "_" + System.currentTimeMillis());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());

        return Result.success("登录成功", result);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<SysUser> getUserInfo(@RequestParam Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user != null) {
            user.setPassword(null); // 不返回密码
        }
        return Result.success(user);
    }
}
