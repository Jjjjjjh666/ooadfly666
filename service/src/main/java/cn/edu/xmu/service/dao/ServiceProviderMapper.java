package cn.edu.xmu.service.dao;

import cn.edu.xmu.service.dao.po.ServiceProviderPo;
import org.apache.ibatis.annotations.*;

/**
 * 服务商Mapper - 对应表 service_provider
 */
@Mapper
public interface ServiceProviderMapper {

    /**
     * 根据ID查询服务商
     */
    @Select("SELECT * FROM service_provider WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "name", column = "name"),
            @Result(property = "consignee", column = "consignee"),
            @Result(property = "address", column = "address"),
            @Result(property = "mobile", column = "mobile"),
            @Result(property = "status", column = "status"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    ServiceProviderPo findById(@Param("id") Long id);

    /**
     * 更新服务商信息
     */
    @Update("UPDATE service_provider SET name = #{name}, consignee = #{consignee}, " +
            "address = #{address}, mobile = #{mobile}, status = #{status}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int update(ServiceProviderPo po);

    /**
     * 插入服务商（用于测试）
     */
    @Insert("INSERT INTO service_provider (name, consignee, address, mobile, status, created_at, updated_at) " +
            "VALUES (#{name}, #{consignee}, #{address}, #{mobile}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ServiceProviderPo po);
}
