package cn.edu.xmu.aftersale.client;

import cn.edu.xmu.aftersale.client.dto.CreateServiceOrderRequest;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 服务模块OpenFeign客户端
 */
@FeignClient(name = "service-module", url = "${service.url:http://localhost:8082}")
public interface ServiceClient {

    /**
     * 创建服务单
     */
    @PostMapping("/internal/shops/{shopId}/aftersales/{aftersalesId}/serviceorders")
    ReturnObject createServiceOrder(
            @PathVariable("shopId") Long shopId,
            @PathVariable("aftersalesId") Long aftersalesId,
            @RequestBody CreateServiceOrderRequest request);

    /**
     * 取消服务单
     *
     */
    @DeleteMapping("/internal/shops/{shopId}/aftersales/{aftersalesId}/serviceorders/cancel")
    ReturnObject cancelServiceOrder(
            @PathVariable("shopId") Long shopId,
            @PathVariable("aftersalesId") Long aftersalesId,
            @RequestParam("reason") String reason);
}

