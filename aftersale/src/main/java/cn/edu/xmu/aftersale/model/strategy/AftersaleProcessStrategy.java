package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.model.AftersaleOrder;

/**
 * 已验收商品的后续处理策略（已验收 -> 完成/继续发货等）
 */
public interface AftersaleProcessStrategy {

    /**
     * 处理已验收的售后商品
     * @param order 售后单（状态需为已验收）
     * @param conclusion 处理备注
     */
    void process(AftersaleOrder order, String conclusion);

    /** 是否支持当前类型 */
    boolean support(Integer type);
}

