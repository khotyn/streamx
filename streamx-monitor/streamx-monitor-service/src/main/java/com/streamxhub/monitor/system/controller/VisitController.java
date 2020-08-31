package com.streamxhub.monitor.system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamxhub.monitor.base.utils.DateUtil;
import com.streamxhub.monitor.system.dao.LoginLogMapper;
import com.streamxhub.monitor.system.entity.User;
import com.streamxhub.monitor.base.domain.ActiveUser;
import com.streamxhub.monitor.base.domain.RestResponse;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author benjobs
 */
@Validated
@RestController
public class VisitController {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("index/{username}")
    public RestResponse index(@NotBlank(message = "{required}") @PathVariable String username) {
        Map<String, Object> data = new HashMap<>();
        // 获取系统访问记录
        Long totalVisitCount = loginLogMapper.findTotalVisitCount();
        data.put("totalVisitCount", totalVisitCount);
        Long todayVisitCount = loginLogMapper.findTodayVisitCount();
        data.put("todayVisitCount", todayVisitCount);
        Long todayIp = loginLogMapper.findTodayIp();
        data.put("todayIp", todayIp);
        // 获取近期系统访问记录
        List<Map<String, Object>> lastSevenVisitCount = loginLogMapper.findLastSevenDaysVisitCount(null);
        data.put("lastSevenVisitCount", lastSevenVisitCount);
        User param = new User();
        param.setUsername(username);
        List<Map<String, Object>> lastSevenUserVisitCount = loginLogMapper.findLastSevenDaysVisitCount(param);
        data.put("lastSevenUserVisitCount", lastSevenUserVisitCount);
        return new RestResponse().data(data);
    }

    @RequiresPermissions("user:online")
    @GetMapping("online")
    public RestResponse userOnline(String username) throws Exception {
        String now = DateUtil.formatFullTime(LocalDateTime.now());
        List<ActiveUser> activeUsers = new ArrayList<>();
        return new RestResponse().data(activeUsers);
    }

}
