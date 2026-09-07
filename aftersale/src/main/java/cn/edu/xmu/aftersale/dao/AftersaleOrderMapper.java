package cn.edu.xmu.aftersale.dao;

import cn.edu.xmu.aftersale.dao.po.AftersaleOrderPo;
import org.apache.ibatis.annotations.*;

/**
 * 售后单Mapper - 对应表 aftersales
 */
@Mapper
public interface AftersaleOrderMapper {

    /**
     * 根据ID查询售后单
     */
    @Select("SELECT * FROM aftersales WHERE id = #{id} AND shop_id = #{shopId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "shopId", column = "shop_id"),
            @Result(property = "orderId", column = "order_id"),
            @Result(property = "orderItemId", column = "order_item_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "type", column = "type"),
            @Result(property = "status", column = "status"),
            @Result(property = "reason", column = "reason"),
            @Result(property = "conclusion", column = "conclusion"),
            @Result(property = "expressId", column = "express_id"),
            @Result(property = "returnExpressId", column = "return_express_id"),
            @Result(property = "gmtCreate", column = "gmt_create"),
            @Result(property = "gmtModified", column = "gmt_modified")
    })
    AftersaleOrderPo findById(@Param("shopId") Long shopId, @Param("id") Long id);

    /**
     * 更新售后单状态
     */
    @Update("UPDATE aftersales SET status = #{status}, conclusion = #{conclusion}, express_id = #{expressId}, " +
            "return_express_id = #{returnExpressId}, gmt_modified = #{gmtModified} WHERE id = #{id} AND shop_id = #{shopId}")
    int updateStatus(AftersaleOrderPo po);

    /**
     * 插入售后单（用于测试）
     */
    @Insert("INSERT INTO aftersales (shop_id, order_id, customer_id, product_id, type, reason, status, express_id, return_express_id, gmt_create, gmt_modified) " +
            "VALUES (#{shopId}, #{orderId}, #{customerId}, #{productId}, #{type}, #{reason}, #{status}, #{expressId}, #{returnExpressId}, #{gmtCreate}, #{gmtModified})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AftersaleOrderPo po);
}

