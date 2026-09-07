package cn.edu.xmu.aftersale.dao;

import cn.edu.xmu.aftersale.dao.po.AftersaleOrderPo;
import cn.edu.xmu.aftersale.model.AftersaleOrder;
import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 售后单仓储
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AftersaleOrderRepository {

    private final AftersaleOrderMapper mapper;

    /**
     * 根据ID查询售后单
     */
    public AftersaleOrder findById(Long shopId, Long id) {
        AftersaleOrderPo po = mapper.findById(shopId, id);
        if (po == null) {
            throw new BusinessException(ReturnNo.AFTERSALE_NOT_FOUND);
        }
        return AftersaleOrder.fromPo(po);
    }

    /**
     * 保存售后单
     */
    public void save(AftersaleOrder order) {
        AftersaleOrderPo po = order.toPo();
        int rows = mapper.updateStatus(po);
        if (rows == 0) {
            throw new BusinessException(ReturnNo.AFTERSALE_NOT_FOUND);
        }
        log.info("售后单更新成功: id={}, status={}", order.getId(), order.getStatus());
    }

    /**
     * 创建售后单（用于测试）
     */
    public AftersaleOrder create(AftersaleOrder order) {
        // 确保时间字段非空，避免 DB 非空约束报错
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        if (order.getUpdatedAt() == null) {
            order.setUpdatedAt(order.getCreatedAt());
        }
        AftersaleOrderPo po = order.toPo();
        mapper.insert(po);
        order.setId(po.getId());
        log.info("售后单创建成功: id={}", po.getId());
        return order;
    }
}

