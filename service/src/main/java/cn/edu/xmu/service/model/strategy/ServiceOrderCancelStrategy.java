package cn.edu.xmu.service.model.strategy;

import cn.edu.xmu.service.model.ServiceOrder;

/**
 * 服务单取消策略接口（多态核心）
 * 不同类型的服务单有不同的取消策略
 */
public interface ServiceOrderCancelStrategy {
    
    /**
     * 取消服务单
     * @param order 服务单
     */
    void cancel(ServiceOrder order);
    
    /**
     * 判断当前策略是否支持该类型
     * @param type 服务类型
     * @return 是否支持
     */
    boolean support(Integer type);
}

