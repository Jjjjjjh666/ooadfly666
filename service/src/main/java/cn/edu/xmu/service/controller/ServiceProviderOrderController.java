package cn.edu.xmu.service.controller;

import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import cn.edu.xmu.service.controller.dto.ConfirmServiceOrderRequest;
import cn.edu.xmu.service.controller.dto.AssignServiceOrderRequest;
import cn.edu.xmu.service.service.ServiceOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 服务商服务单控制器
 */
@Slf4j
@RestController
@RequestMapping("/serviceprovider/{did}/serviceorders")
@RequiredArgsConstructor
@Validated
public class ServiceProviderOrderController {

    private final ServiceOrderService serviceOrderService;

    /**
     * 服务商审核服务单
     * PUT /serviceprovider/{did}/serviceorders/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    public ReturnObject confirmServiceOrder(
            @PathVariable("did") Long serviceProviderId,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody ConfirmServiceOrderRequest request) {
        
        log.info("服务商审核服务单API: serviceProviderId={}, orderId={}, confirm={}", 
                serviceProviderId, orderId, request.getConfirm());
        
        serviceOrderService.confirmServiceOrder(serviceProviderId, orderId, request.getConfirm());
        
        return new ReturnObject(ReturnNo.OK);
    }

    /**
     * 服务商派工
     * PUT /serviceprovider/{did}/serviceorders/{id}/assign
     */
    @PutMapping("/{id}/assign")
    public ReturnObject assignServiceOrder(
            @PathVariable("did") Long serviceProviderId,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody AssignServiceOrderRequest request) {
        
        log.info("服务商派工API: serviceProviderId={}, orderId={}, serviceStaffId={}", 
                serviceProviderId, orderId, request.getServiceStaffId());
        
        serviceOrderService.assignServiceOrder(serviceProviderId, orderId, request.getServiceStaffId());
        
        return new ReturnObject(ReturnNo.OK);
    }

    /**
     * 验收寄修商品
     * PUT /serviceprovider/{did}/serviceorders/{id}/receive
     */
    @PutMapping("/{id}/receive")
    public ReturnObject receiveServiceOrder(
            @PathVariable("did") Long serviceProviderId,
            @PathVariable("id") Long orderId) {
        
        log.info("验收寄修商品API: serviceProviderId={}, orderId={}", serviceProviderId, orderId);
        
        serviceOrderService.receiveServiceOrder(serviceProviderId, orderId);
        
        return new ReturnObject(ReturnNo.OK);
    }

    /**
     * 完成服务单
     * PUT /serviceprovider/{did}/serviceorders/{id}/complete
     */
    @PutMapping("/{id}/complete")
    public ReturnObject completeServiceOrder(
            @PathVariable("did") Long serviceProviderId,
            @PathVariable("id") Long orderId) {
        
        log.info("完成服务单API: serviceProviderId={}, orderId={}", serviceProviderId, orderId);
        
        serviceOrderService.completeServiceOrder(serviceProviderId, orderId);
        
        return new ReturnObject(ReturnNo.OK);
    }

    /**
     * 服务商取消服务单
     * DELETE /serviceprovider/{did}/serviceorders/{id}/cancel
     */
    @DeleteMapping("/{id}/cancel")
    public ReturnObject cancelServiceOrder(
            @PathVariable("did") Long serviceProviderId,
            @PathVariable("id") Long orderId) {
        
        log.info("服务商取消服务单API: serviceProviderId={}, orderId={}", serviceProviderId, orderId);
        
        serviceOrderService.cancelServiceOrder(serviceProviderId, orderId);
        
        return new ReturnObject(ReturnNo.OK);
    }
}

