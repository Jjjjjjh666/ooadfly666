package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.model.AftersaleOrder;

/**
 * 售后单审核策略接口（多态核心）
 * 不同类型的售后单有不同的审核策略
 */
public interface AftersaleConfirmStrategy {
    
    /**
     * 审核售后单
     * @param order 售后单
     * @param confirm 是否同意
     * @param conclusion 审核结论
     */
    void confirm(AftersaleOrder order, Boolean confirm, String conclusion);
    
    /**
     * 判断当前策略是否支持该类型
     * @param type 售后类型
     * @return 是否支持
     */
    boolean support(Integer type);
}

