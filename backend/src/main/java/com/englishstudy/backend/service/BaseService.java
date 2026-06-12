package com.englishstudy.backend.service;

import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.common.BaseEntity;
import com.englishstudy.backend.context.CurrentUser;
import com.englishstudy.backend.context.UserContext;

import java.util.Arrays;

public abstract class BaseService {

    protected CurrentUser currentUser() {
        CurrentUser currentUser = UserContext.get();
        if (currentUser == null) {
            throw new BusinessException("请先登录");
        }
        return currentUser;
    }

    protected void requireRole(String... roles) {
        CurrentUser currentUser = currentUser();
        boolean matched = Arrays.stream(roles).anyMatch(role -> role.equals(currentUser.getRole()));
        if (!matched) {
            throw new BusinessException("没有访问权限");
        }
    }

    protected String requireCurrentClassName() {
        String className = currentUser().getClassName();
        if (className == null || className.trim().isEmpty()) {
            throw new BusinessException("当前账号未分配班级，请联系管理员");
        }
        return className.trim();
    }

    protected boolean sameClassName(String first, String second) {
        return first != null && second != null
                && !first.trim().isEmpty()
                && first.trim().equals(second.trim());
    }

    protected <T extends BaseEntity> T markForInsert(T entity) {
        entity.markCreateTime();
        return entity;
    }

    protected <T extends BaseEntity> T markForUpdate(T entity) {
        entity.markUpdateTime();
        return entity;
    }
}
