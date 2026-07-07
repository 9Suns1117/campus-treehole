package com.treehole.servlet;

import com.treehole.dao.ListenerDao;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

@WebServlet(urlPatterns = "/startup", loadOnStartup = 1)
public class StartupServlet extends HttpServlet {
    @Override
    public void init() throws ServletException {
        super.init();
        new ListenerDao().ensureStorage();
    }
}
