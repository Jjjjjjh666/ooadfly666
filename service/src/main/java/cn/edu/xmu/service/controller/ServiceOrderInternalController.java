package cn.edu.xmu.service.controller;

import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import cn.edu.xmu.service.controller.dto.CreateServiceOrderRequest;
import cn.edu.xmu.service.service.ServiceOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务单内部接口控制器
 * 供aftersale模块调用
 */
@Slf4j
@RestController
@RequestMapping("/internal/shops/{shopId}/aftersales/{aftersalesId}")
@RequiredArgsConstructor
public class ServiceOrderInternalController {

    private final ServiceOrderService serviceOrderService;

    /**
     * 创建服务单（内部接口）
     * POST /internal/shops/{shopId}/aftersales/{aftersalesId}/serviceorders
     */
    @PostMapping("/serviceorders")
    public ReturnObject createServiceOrder(
            @PathVariable Long shopId,
            @PathVariable Long aftersalesId,
            @RequestBody(required = false) CreateServiceOrderRequest request) {
        
        log.info("内部接口-创建服务单: shopId={}, aftersalesId={}", shopId, aftersalesId);
        
        Integer type = (request != null && request.getType() != null) ? request.getType() : 0;
        String consignee = "";
        String mobile = "";
        String address = "";
        if (request != null) {
            if (request.getConsignee() != null) {
                consignee = request.getConsignee().getName() != null ? request.getConsignee().getName() : "";
                mobile = request.getConsignee().getMobile() != null ? request.getConsignee().getMobile() : "";
            }
            address = request.getAddress() != null ? request.getAddress() : "";
        }
        
        Long serviceOrderId = serviceOrderService.createServiceOrder(
                shopId, aftersalesId, type, consignee, address, mobile);
        
        Map<String, Object> data = new HashMap<>();
        data.put("id", serviceOrderId);
        data.put("type", type);
        
        return new ReturnObject(data);
    }

    /**
     * 取消服务单（内部接口）
     * DELETE /internal/shops/{shopId}/aftersales/{aftersalesId}/serviceorders/cancel
     */
    @DeleteMapping("/serviceorders/cancel")
    public ReturnObject cancelServiceOrder(
            @PathVariable Long shopId,
            @PathVariable Long aftersalesId,
            @RequestParam("reason") String reason) {
        
        log.info("内部接口-取消服务单: shopId={}, aftersalesId={}, reason={}", shopId, aftersalesId, reason);
        
        serviceOrderService.cancelServiceOrderByAftersale(aftersalesId, reason);
        
        return new ReturnObject(ReturnNo.OK);
    }
}

