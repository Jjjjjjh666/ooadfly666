package cn.edu.xmu.aftersale.model.strategy;

import cn.edu.xmu.aftersale.model.AftersaleOrder;

/**
 * 售后验收策略（退货/换货）
 */
public interface AftersaleReceiveStrategy {

    /**
     * 商户验收已寄回的商品
     * @param order 售后单
     * @param accept 是否验收通过
     * @param conclusion 结论/备注
     */
    void receive(AftersaleOrder order, Boolean accept, String conclusion);

    /**
     * 是否支持当前售后类型
     */
    boolean support(Integer type);
}

