package cn.edu.xmu.service.model;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.dao.po.ServiceProviderPo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 服务商领域对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProvider {

    private Long id;
    private String name; // 服务商名称
    private String consignee; // 联系人
    private String address; // 地址
    private String mobile; // 电话
    private String status; // 状态：ACTIVE-活跃 INACTIVE-不活跃
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从PO构建领域对象
     */
    public static ServiceProvider fromPo(ServiceProviderPo po) {
        if (po == null) {
            return null;
        }
        return ServiceProvider.builder()
                .id(po.getId())
                .name(po.getName())
                .consignee(po.getConsignee())
                .address(po.getAddress())
                .mobile(po.getMobile())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    /**
     * 转换为PO
     */
    public ServiceProviderPo toPo() {
        ServiceProviderPo po = new ServiceProviderPo();
        po.setId(this.id);
        po.setName(this.name);
        po.setConsignee(this.consignee);
        po.setAddress(this.address);
        po.setMobile(this.mobile);
        po.setStatus(this.status);
        po.setCreatedAt(this.createdAt);
        po.setUpdatedAt(this.updatedAt);
        return po;
    }

    /**
     * 更新服务商信息
     */
    public void updateInfo(String name, String consignee, String address, String mobile) {
        if (name != null) {
            this.name = name;
        }
        if (consignee != null) {
            this.consignee = consignee;
        }
        if (address != null) {
            this.address = address;
        }
        if (mobile != null) {
            this.mobile = mobile;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
