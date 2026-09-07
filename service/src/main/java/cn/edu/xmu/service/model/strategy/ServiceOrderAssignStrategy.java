package cn.edu.xmu.service.model.strategy;

import cn.edu.xmu.service.model.ServiceOrder;

/**
 * 服务单派工策略接口
 * 不同类型的服务单派工时需要执行不同的逻辑
 */
public interface ServiceOrderAssignStrategy {
    
    /**
     * 派工
     * @param order 服务单
     * @param serviceStaffId 维修师傅ID
     */
    void assign(ServiceOrder order, Long serviceStaffId);
    
    /**
     * 是否支持该类型的服务单
     * @param type 服务单类型
     * @return 是否支持
     */
    boolean support(Integer type);
}

