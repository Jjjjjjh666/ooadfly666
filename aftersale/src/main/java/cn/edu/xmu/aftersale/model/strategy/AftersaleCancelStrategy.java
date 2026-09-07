package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.model.AftersaleOrder;

/**
 * 售后单取消策略接口（多态核心）
 * 不同类型的售后单有不同的取消策略
 */
public interface AftersaleCancelStrategy {
    
    /**
     * 取消售后单
     * @param order 售后单
     * @param reason 取消理由
     */
    void cancel(AftersaleOrder order, String reason);
    
    /**
     * 判断当前策略是否支持该类型
     * @param type 售后类型
     * @return 是否支持
     */
    boolean support(Integer type);
}

