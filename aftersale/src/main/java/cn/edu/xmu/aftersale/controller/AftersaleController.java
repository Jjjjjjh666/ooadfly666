package cn.edu.xmu.aftersale.controller;

import cn.edu.xmu.aftersale.controller.dto.AcceptAftersaleRequest;
import cn.edu.xmu.aftersale.controller.dto.CancelAftersaleRequest;
import cn.edu.xmu.aftersale.controller.dto.ConfirmAftersaleRequest;
import cn.edu.xmu.aftersale.controller.dto.ProcessReceivedAftersaleRequest;
import cn.edu.xmu.aftersale.service.AftersaleService;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 售后控制器
 */
@Slf4j
@RestController
@RequestMapping("/shops/{shopid}")
@RequiredArgsConstructor
@Validated
public class AftersaleController {

    private final AftersaleService aftersaleService;

    /** 商户审核售后 */
    @PutMapping("/aftersaleorders/{id}/confirm")
    public ReturnObject confirmAftersale(
            @PathVariable("shopid") Long shopId,
            @PathVariable("id") Long id,
            @Valid @RequestBody ConfirmAftersaleRequest request) {

        log.info("商户审核售后API: shopId={}, id={}, confirm={}", shopId, id, request.getConfirm());

        String status = aftersaleService.confirmAftersale(
                shopId, id, request.getConfirm(), request.getConclusion());

        Map<String, Object> data = new HashMap<>();
        data.put("errno", ReturnNo.OK.getCode());
        data.put("errmsg", ReturnNo.OK.getMessage());
        data.put("status", status);

        return new ReturnObject(data);
    }

    /** 商户取消售后单 */
    @DeleteMapping("/aftersaleorders/{id}/cancel")
    public ReturnObject cancelAftersale(
            @PathVariable("shopid") Long shopId,
            @PathVariable("id") Long id,
            @Valid @RequestBody CancelAftersaleRequest request) {

        log.info("商户取消售后API: shopId={}, id={}, reason={}", shopId, id, request.getReason());

        if (!Boolean.TRUE.equals(request.getConfirm())) {
            return ReturnObject.error(ReturnNo.BAD_REQUEST, "confirm必须为true");
        }

        String status = aftersaleService.cancelAftersale(shopId, id, request.getReason());

        Map<String, Object> data = new HashMap<>();
        data.put("errno", ReturnNo.OK.getCode());
        data.put("errmsg", ReturnNo.OK.getMessage());
        data.put("status", status);

        return new ReturnObject(data);
    }

    /** 商户验收（待验收 -> 已验收/已拒绝） */
    @PutMapping("/aftersaleorders/{id}/accept")
    public ReturnObject acceptAftersale(
            @PathVariable("shopid") Long shopId,
            @PathVariable("id") Long id,
            @Valid @RequestBody AcceptAftersaleRequest request) {

        log.info("商户验收售后API: shopId={}, id={}, accept={}", shopId, id, request.getAccept());

        String status = aftersaleService.acceptAftersale(
                shopId, id, request.getAccept(), request.getConclusion());

        Map<String, Object> data = new HashMap<>();
        data.put("errno", ReturnNo.OK.getCode());
        data.put("errmsg", ReturnNo.OK.getMessage());
        data.put("status", status);

        return new ReturnObject(data);
    }

    /** 商户处理已验收商品（已验收 -> 完成/发货） */
    @PutMapping("/aftersaleorders/{id}/receive")
    public ReturnObject processReceivedAftersale(
            @PathVariable("shopid") Long shopId,
            @PathVariable("id") Long id,
            @Valid @RequestBody ProcessReceivedAftersaleRequest request) {

        log.info("商户处理已验收商品API: shopId={}, id={}", shopId, id);

        String status = aftersaleService.processReceivedAftersale(
                shopId, id, request.getConclusion());

        Map<String, Object> data = new HashMap<>();
        data.put("errno", ReturnNo.OK.getCode());
        data.put("errmsg", ReturnNo.OK.getMessage());
        data.put("status", status);

        return new ReturnObject(data);
    }
}

