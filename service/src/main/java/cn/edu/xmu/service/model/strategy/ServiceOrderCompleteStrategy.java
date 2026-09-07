package cn.edu.xmu.service.model.strategy;

import cn.edu.xmu.service.model.ServiceOrder;

/**
 * 服务单完成策略接口（多态核心）
 * 不同类型的服务单有不同的完成策略
 */
public interface ServiceOrderCompleteStrategy {
    
    /**
     * 完成服务单
     * @param order 服务单
     */
    void complete(ServiceOrder order);
    
    /**
     * 判断当前策略是否支持该类型和状态
     * @param type 服务类型
     * @param status 服务单状态码
     * @return 是否支持
     */
    boolean support(Integer type, Integer status);
}

