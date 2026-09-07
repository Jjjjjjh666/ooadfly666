package cn.edu.xmu.aftersale.dao.po;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 售后单持久化对象 - 对应数据库表 aftersales
 */
@Data
public class AftersaleOrderPo {
    private Long id;
    private Long shopId;
    private Long orderId;
    private Long orderItemId;
    private Long customerId;
    private Long productId;
    private Integer type; // 0-换货 1-退货 2-维修
    private String reason;
    // 0-待审核 1-待寄回 2-待完成 3-待收货 4-待处理 5-已拒绝 6-已完成 7-已取消
    private Integer status;
    private String conclusion;
    /** 客户寄回商品的运单ID */
    private Long expressId;
    /** 商家发货（换货）的运单ID */
    private Long returnExpressId;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}

