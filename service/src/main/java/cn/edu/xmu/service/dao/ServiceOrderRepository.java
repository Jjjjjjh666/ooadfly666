package cn.edu.xmu.service.dao;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.dao.po.ServiceOrderPo;
import cn.edu.xmu.service.model.ServiceOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 服务单仓储
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ServiceOrderRepository {

    private final ServiceOrderMapper mapper;

    /**
     * 根据ID查询服务单
     */
    public ServiceOrder findById(Long id) {
        ServiceOrderPo po = mapper.findById(id);
        if (po == null) {
            throw new BusinessException(ReturnNo.RESOURCE_NOT_FOUND, "服务单不存在");
        }
        return ServiceOrder.fromPo(po);
    }

    /**
     * 保存服务单
     */
    public void save(ServiceOrder order) {
        ServiceOrderPo po = order.toPo();
        int rows = mapper.updateStatus(po);
        if (rows == 0) {
            throw new BusinessException(ReturnNo.RESOURCE_NOT_FOUND, "服务单不存在");
        }
        log.info("服务单更新成功: id={}, status={}", order.getId(), order.getStatus());
    }

    /**
     * 创建服务单
     */
    public ServiceOrder create(ServiceOrder order) {
        ServiceOrderPo po = order.toPo();
        mapper.insert(po);
        order.setId(po.getId());
        log.info("服务单创建成功: id={}", po.getId());
        return order;
    }

    /**
     * 根据售后单ID查询服务单
     */
    public ServiceOrder findByAftersaleId(Long aftersaleId) {
        ServiceOrderPo po = mapper.findByAftersaleId(aftersaleId);
        if (po == null) {
            return null;
        }
        return ServiceOrder.fromPo(po);
    }
}

