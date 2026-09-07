package cn.edu.xmu.service.service;

import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.controller.dto.ServiceProviderBrief;
import cn.edu.xmu.service.controller.dto.ServiceProviderDraftHistoryResponse;
import cn.edu.xmu.service.controller.dto.ServiceProviderDraftListItem;
import cn.edu.xmu.service.controller.dto.ServiceProviderDraftListResult;
import cn.edu.xmu.service.dao.ServiceProviderDraftRepository;
import cn.edu.xmu.service.dao.ServiceProviderRepository;
import cn.edu.xmu.service.model.ServiceProvider;
import cn.edu.xmu.service.model.ServiceProviderDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务商服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceProviderService {

    private final ServiceProviderDraftRepository repository;
    private final ServiceProviderRepository serviceProviderRepository;

    private static final DateTimeFormatter HISTORY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 平台管理员审核服务商变更
     * @param draftId 草稿ID
     * @param conclusion 审核结果 0-拒绝 1-通过
     * @param opinion 审核意见
     */
    @Transactional
    public void reviewDraft(Long draftId, Integer conclusion, String opinion) {
        log.info("开始审核服务商变更: draftId={}, conclusion={}", draftId, conclusion);

        // 1. 查询草稿
        ServiceProviderDraft draft = repository.findById(draftId);

        // 2. 检查状态
        draft.checkPendingStatus();

        // 3. 执行审核
        if (Integer.valueOf(1).equals(conclusion)) {
            // 审核通过
            draft.approve(opinion != null ? opinion : "审核通过");
            log.info("服务商变更审核通过: draftId={}", draftId);

            // 取出正式服务商并让草稿自行应用
            ServiceProvider provider = serviceProviderRepository.findById(draft.getServiceProviderId());
            draft.applyTo(provider);
            serviceProviderRepository.save(provider);
        } else if (Integer.valueOf(0).equals(conclusion)) {
            // 审核拒绝
            draft.reject(opinion != null ? opinion : "审核拒绝");
            log.info("服务商变更审核拒绝: draftId={}, opinion={}", draftId, opinion);
        } else {
            throw new cn.edu.xmu.javaee.core.exception.BusinessException(ReturnNo.BAD_REQUEST,
                    "无效的审核结果: " + conclusion);
        }

        // 4. 保存更新
        repository.save(draft);

        log.info("服务商变更审核完成: draftId={}, status={}", draftId, draft.getStatus());
    }

    /**
     * 平台管理员查询服务商变更申请
     */
    public ServiceProviderDraftListResult listDrafts(String serviceProviderName, String contactPerson,
                                                     String serviceArea, String phone, int page, int pageSize) {
        List<ServiceProviderDraft> drafts = repository.search(serviceProviderName, contactPerson, phone, serviceArea, page, pageSize);
        long total = repository.count(serviceProviderName, contactPerson, phone, serviceArea);

        List<ServiceProviderDraftListItem> items = drafts.stream()
                .map(this::toListItem)
                .toList();

        return ServiceProviderDraftListResult.builder()
                .list(items)
                .page(page)
                .pageSize(pageSize)
                .total(total)
                .build();
    }

    /**
     * 平台管理员查看变更历史
     */
    public ServiceProviderDraftHistoryResponse getDraftHistory(Long draftId) {
        ServiceProviderDraft draft = repository.findById(draftId);
        List<String> history = new ArrayList<>();

        if (draft.getCreatedAt() != null) {
            history.add(formatHistory(draft.getCreatedAt(), "提交变更申请"));
        } else {
            history.add("提交变更申请");
        }

        String statusDesc = draft.getStatus() != null ? draft.getStatus().getDescription() : "状态未知";
        String opinion = draft.getOpinion();
        LocalDateTime statusTime = draft.getUpdatedAt() != null ? draft.getUpdatedAt() : draft.getCreatedAt();
        StringBuilder detail = new StringBuilder(statusDesc);
        if (opinion != null && !opinion.isBlank()) {
            detail.append("：").append(opinion);
        }
        if (statusTime != null) {
            history.add(formatHistory(statusTime, detail.toString()));
        } else {
            history.add(detail.toString());
        }

        return new ServiceProviderDraftHistoryResponse(history);
    }

    private ServiceProviderDraftListItem toListItem(ServiceProviderDraft draft) {
        ServiceProviderBrief brief = ServiceProviderBrief.builder()
                .id(draft.getServiceProviderId())
                .name(draft.getProviderName())
                .providerName(draft.getContactPerson())
                .phone(draft.getContactPhone())
                .build();

        return ServiceProviderDraftListItem.builder()
                .draftId(draft.getId())
                .serviceProvider(brief)
                .serviceArea(draft.getAddress())
                .changeTime(draft.getCreatedAt())
                .change(buildChangeDesc(draft))
                .operation(draft.getStatus() != null ? draft.getStatus().getDescription() : null)
                .build();
    }

    private String buildChangeDesc(ServiceProviderDraft draft) {
        List<String> fields = new ArrayList<>();
        if (draft.getProviderName() != null && !draft.getProviderName().isBlank()) {
            fields.add("服务商名称");
        }
        if (draft.getContactPerson() != null && !draft.getContactPerson().isBlank()) {
            fields.add("联系人");
        }
        if (draft.getContactPhone() != null && !draft.getContactPhone().isBlank()) {
            fields.add("联系电话");
        }
        if (draft.getAddress() != null && !draft.getAddress().isBlank()) {
            fields.add("服务区域");
        }
        return fields.isEmpty() ? "无" : String.join("/", fields);
    }

    private String formatHistory(LocalDateTime time, String content) {
        return time == null ? content : HISTORY_TIME_FORMATTER.format(time) + " " + content;
    }
}

