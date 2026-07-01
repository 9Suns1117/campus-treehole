package com.treehole.service.impl;

import com.treehole.common.PasswordUtil;
import com.treehole.dao.AuthDao;
import com.treehole.pojo.TreeholeUser;
import com.treehole.service.AuthService;

public class AuthServiceImpl implements AuthService {

    private AuthDao authDao = new AuthDao();

    @Override
    public TreeholeUser login(String username, String password, Integer role) {
        TreeholeUser user = authDao.getUserByUsername(username);
        if (user == null) return null;
        if (user.getStatus() != null && user.getStatus() != 1) return null;
        if (role == null || user.getRole() == null || !user.getRole().equals(role)) return null;
        if (!PasswordUtil.matches(password, user.getPassword())) return null;

        if (!PasswordUtil.isSha256Hash(user.getPassword())) {
            String hash = PasswordUtil.sha256(password);
            authDao.updatePassword(user.getUserId(), hash);
            user.setPassword(hash);
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public boolean register(String username, String nickname, String password) {
        TreeholeUser existingUser = authDao.getUserByUsername(username);
        if (existingUser != null) {
            return false;
        }
        TreeholeUser newUser = new TreeholeUser();
        newUser.setUsername(username);
        newUser.setPassword(PasswordUtil.sha256(password));
        newUser.setNickname(nickname == null || nickname.trim().isEmpty() ? "匿名同学" : nickname.trim());
        newUser.setRole(1);
        newUser.setStatus(1);
        newUser.setIsDeleted(0);
        return authDao.insertUser(newUser);
    }
}
