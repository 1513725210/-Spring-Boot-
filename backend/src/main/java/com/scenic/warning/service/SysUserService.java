package com.scenic.warning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scenic.warning.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser getByUsername(String username);
}
