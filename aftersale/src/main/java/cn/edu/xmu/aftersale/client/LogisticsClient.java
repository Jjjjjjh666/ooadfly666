package cn.edu.xmu.aftersale.client;

import cn.edu.xmu.aftersale.client.dto.CreatePackageRequest;
import cn.edu.xmu.aftersale.client.dto.CreatePackageResponse;
import cn.edu.xmu.javaee.core.model.InternalReturnObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * 物流模块OpenFeign客户端
 * 调用oomall/logistics模块的API
 */
@FeignClient(
    name = "logistics-module",
    url = "${logistics.url:http://localhost:8080}"
)
public interface LogisticsClient {

    /**
     * 创建运单
     * POST /internal/shops/{shopId}/packages
     * 
     * @param shopId 商户ID
     * @param authorization 用户token
     * @param request 创建运单请求
     * @return 运单信息
     */
    @PostMapping("/internal/shops/{shopId}/packages")
    InternalReturnObject<CreatePackageResponse> createPackage(
            @PathVariable("shopId") Long shopId,
            @RequestHeader("authorization") String authorization,
            @RequestBody CreatePackageRequest request
    );

    /**
     * 取消运单
     * PUT /internal/shops/{shopId}/packages/{id}/cancel
     * 
     * @param shopId 商户ID
     * @param id 运单ID
     * @param authorization 用户token
     * @return 操作结果
     */
    @PutMapping("/internal/shops/{shopId}/packages/{id}/cancel")
    InternalReturnObject<Void> cancelPackage(
            @PathVariable("shopId") Long shopId,
            @PathVariable("id") Long id,
            @RequestHeader("authorization") String authorization
    );
}

