package com.scs.volunteer.controller;

import com.scs.volunteer.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;

public class BaseController {
    protected CurrentUser currentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }
}
