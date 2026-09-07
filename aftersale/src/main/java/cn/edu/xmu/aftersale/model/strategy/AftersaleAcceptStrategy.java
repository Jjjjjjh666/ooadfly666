package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.model.AftersaleOrder;

/**
 * 售后验收策略（待验收 -> 已验收/已拒绝）
 */
public interface AftersaleAcceptStrategy {

    /**
     * 验收售后单
     * @param order 售后单
     * @param accept 是否通过
     * @param conclusion 备注/结论
     */
    void accept(AftersaleOrder order, Boolean accept, String conclusion);

    /** 是否支持类型 */
    boolean support(Integer type);
}

