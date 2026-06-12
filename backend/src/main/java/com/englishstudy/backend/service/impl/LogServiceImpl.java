package com.englishstudy.backend.service.impl;

import com.englishstudy.backend.context.CurrentUser;
import com.englishstudy.backend.context.UserContext;
import com.englishstudy.backend.entity.OperationLog;
import com.englishstudy.backend.mapper.OperationLogMapper;
import com.englishstudy.backend.service.LogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogServiceImpl implements LogService {

    private final OperationLogMapper operationLogMapper;

    public LogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    public void save(String module, String action, String detail) {
        CurrentUser currentUser = UserContext.get();
        OperationLog log = new OperationLog();
        if (currentUser != null) {
            log.setUserId(currentUser.getUserId());
            log.setUsername(currentUser.getUsername());
        }
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    @Override
    public void saveByUser(Long userId, String username, String module, String action, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setModule(module);
        log.setAction(action);
        log.setDetail(detail);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}
