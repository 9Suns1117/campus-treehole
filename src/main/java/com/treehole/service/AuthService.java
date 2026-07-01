package com.treehole.service;

import com.treehole.pojo.TreeholeUser;

public interface AuthService {
    TreeholeUser login(String username, String password, Integer role);
    boolean register(String username, String nickname, String password);
}
