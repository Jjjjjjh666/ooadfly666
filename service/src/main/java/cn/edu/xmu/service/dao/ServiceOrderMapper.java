package cn.edu.xmu.service.dao;

import cn.edu.xmu.service.dao.po.ServiceOrderPo;
import org.apache.ibatis.annotations.*;

/**
 * 服务单Mapper - 对应表 service_order
 */
@Mapper
public interface ServiceOrderMapper {

    /**
     * 插入服务单
     */
    @Insert("INSERT INTO service_order (type, consignee, address, mobile, status, description, " +
            "service_staff_id, service_provider_id, service_contract_id, service_id, " +
            "customer_id, region_id, product_id, aftersale_id, express_id, return_express_id, created_at, updated_at) " +
            "VALUES (#{type}, #{consignee}, #{address}, #{mobile}, #{status}, #{description}, " +
            "#{serviceStaffId}, #{serviceProviderId}, #{serviceContractId}, #{serviceId}, " +
            "#{customerId}, #{regionId}, #{productId}, #{aftersaleId}, #{expressId}, #{returnExpressId}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ServiceOrderPo po);

    /**
     * 根据ID查询服务单
     */
    @Select("SELECT * FROM service_order WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "type", column = "type"),
            @Result(property = "consignee", column = "consignee"),
            @Result(property = "address", column = "address"),
            @Result(property = "mobile", column = "mobile"),
            @Result(property = "status", column = "status"),
            @Result(property = "description", column = "description"),
            @Result(property = "serviceStaffId", column = "service_staff_id"),
            @Result(property = "serviceProviderId", column = "service_provider_id"),
            @Result(property = "serviceContractId", column = "service_contract_id"),
            @Result(property = "serviceId", column = "service_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "aftersaleId", column = "aftersale_id"),
            @Result(property = "expressId", column = "express_id"),
            @Result(property = "returnExpressId", column = "return_express_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    ServiceOrderPo findById(@Param("id") Long id);

    /**
     * 根据售后单ID查询服务单
     */
    @Select("SELECT * FROM service_order WHERE aftersale_id = #{aftersaleId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "type", column = "type"),
            @Result(property = "consignee", column = "consignee"),
            @Result(property = "address", column = "address"),
            @Result(property = "mobile", column = "mobile"),
            @Result(property = "status", column = "status"),
            @Result(property = "description", column = "description"),
            @Result(property = "serviceStaffId", column = "service_staff_id"),
            @Result(property = "serviceProviderId", column = "service_provider_id"),
            @Result(property = "serviceContractId", column = "service_contract_id"),
            @Result(property = "serviceId", column = "service_id"),
            @Result(property = "customerId", column = "customer_id"),
            @Result(property = "regionId", column = "region_id"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "aftersaleId", column = "aftersale_id"),
            @Result(property = "expressId", column = "express_id"),
            @Result(property = "returnExpressId", column = "return_express_id"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    ServiceOrderPo findByAftersaleId(@Param("aftersaleId") Long aftersaleId);

    /**
     * 更新服务单
     */
    @Update("UPDATE service_order SET type = #{type}, consignee = #{consignee}, address = #{address}, " +
            "mobile = #{mobile}, status = #{status}, description = #{description}, " +
            "service_staff_id = #{serviceStaffId}, service_provider_id = #{serviceProviderId}, " +
            "service_contract_id = #{serviceContractId}, service_id = #{serviceId}, " +
            "customer_id = #{customerId}, region_id = #{regionId}, product_id = #{productId}, " +
            "aftersale_id = #{aftersaleId}, express_id = #{expressId}, return_express_id = #{returnExpressId}, updated_at = #{updatedAt} " +
            "WHERE id = #{id}")
    int updateStatus(ServiceOrderPo po);
}

