package cn.edu.xmu.service.controller;

import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import cn.edu.xmu.service.controller.dto.ReviewDraftRequest;
import cn.edu.xmu.service.service.ServiceProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 服务商管理控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class ServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    @Value("${oomall.core.page-size.default:10}")
    private int defaultPageSize = 10;

    @Value("${oomall.core.page-size.max:1000}")
    private int maxPageSize = 1000;

    /**
     * 平台管理员查询服务商变更申请
     * GET /drafts
     */
    @GetMapping("/drafts")
    public ReturnObject listDrafts(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "pagesize", required = false) Integer pageSizeAlias,
            @RequestParam(value = "serviceprovidername", required = false) String serviceProviderName,
            @RequestParam(value = "providername", required = false) String providerName,
            @RequestParam(value = "servicearea", required = false) String serviceArea,
            @RequestParam(value = "phone", required = false) String phone) {

        int resolvedPage = (page == null || page <= 0) ? 1 : page;
        Integer sizeParam = pageSizeAlias != null ? pageSizeAlias : pageSize;
        int resolvedPageSize = (sizeParam == null || sizeParam <= 0) ? defaultPageSize : sizeParam;
        resolvedPageSize = Math.min(resolvedPageSize, maxPageSize);

        log.info("平台管理员查询服务商变更申请: providerName={}, contactPerson={}, area={}, phone={}, page={}, pageSize={}",
                serviceProviderName, providerName, serviceArea, phone, resolvedPage, resolvedPageSize);

        return new ReturnObject(serviceProviderService.listDrafts(
                serviceProviderName,
                providerName,
                serviceArea,
                phone,
                resolvedPage,
                resolvedPageSize));
    }

    /**
     * 平台管理员查看变更历史
     * GET /drafts/{draftid}/history
     */
    @GetMapping("/drafts/{draftid}/history")
    public ReturnObject getDraftHistory(@PathVariable("draftid") Long draftId) {
        log.info("平台管理员查看变更历史: draftId={}", draftId);
        return new ReturnObject(serviceProviderService.getDraftHistory(draftId));
    }

    /**
     * 平台管理员审核服务商变更
     * PUT /draft/{draftid}/review
     */
    @PutMapping("/draft/{draftid}/review")
    public ReturnObject reviewDraft(
            @PathVariable("draftid") Long draftId,
            @Valid @RequestBody ReviewDraftRequest request) {

        log.info("平台管理员审核服务商变更API: draftId={}, conclusion={}",
                draftId, request.getConclusion());

        serviceProviderService.reviewDraft(draftId, request.getConclusion(), request.getOpinion());

        return new ReturnObject(ReturnNo.OK);
    }
}

