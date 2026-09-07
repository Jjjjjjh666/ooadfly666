package cn.edu.xmu.service.dao;

import cn.edu.xmu.service.dao.po.ServiceProviderDraftPo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 服务商草稿Mapper
 */
@Mapper
public interface ServiceProviderDraftMapper {

    /**
     * 根据ID查询草稿
     */
    @Select("SELECT * FROM service_provider_draft WHERE id = #{id}")
    @Results(id = "draftResult", value = {
            @Result(property = "id", column = "id"),
            @Result(property = "serviceProviderId", column = "service_provider_id"),
            @Result(property = "providerName", column = "provider_name"),
            @Result(property = "contactPerson", column = "contact_person"),
            @Result(property = "contactPhone", column = "contact_phone"),
            @Result(property = "address", column = "address"),
            @Result(property = "status", column = "status"),
            @Result(property = "opinion", column = "opinion"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    ServiceProviderDraftPo findById(@Param("id") Long id);

    /**
     * 条件查询草稿列表（分页）
     */
    @Select("<script>" +
            "SELECT * FROM service_provider_draft " +
            "WHERE 1=1 " +
            "<if test='providerName != null and providerName != \"\"'> " +
            "AND provider_name LIKE CONCAT('%', #{providerName}, '%') " +
            "</if> " +
            "<if test='contactPerson != null and contactPerson != \"\"'> " +
            "AND contact_person LIKE CONCAT('%', #{contactPerson}, '%') " +
            "</if> " +
            "<if test='contactPhone != null and contactPhone != \"\"'> " +
            "AND contact_phone LIKE CONCAT('%', #{contactPhone}, '%') " +
            "</if> " +
            "<if test='serviceArea != null and serviceArea != \"\"'> " +
            "AND address LIKE CONCAT('%', #{serviceArea}, '%') " +
            "</if> " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit} OFFSET #{offset}" +
            "</script>")
    @ResultMap("draftResult")
    List<ServiceProviderDraftPo> search(@Param("providerName") String providerName,
                                        @Param("contactPerson") String contactPerson,
                                        @Param("contactPhone") String contactPhone,
                                        @Param("serviceArea") String serviceArea,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 统计条件查询总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM service_provider_draft " +
            "WHERE 1=1 " +
            "<if test='providerName != null and providerName != \"\"'> " +
            "AND provider_name LIKE CONCAT('%', #{providerName}, '%') " +
            "</if> " +
            "<if test='contactPerson != null and contactPerson != \"\"'> " +
            "AND contact_person LIKE CONCAT('%', #{contactPerson}, '%') " +
            "</if> " +
            "<if test='contactPhone != null and contactPhone != \"\"'> " +
            "AND contact_phone LIKE CONCAT('%', #{contactPhone}, '%') " +
            "</if> " +
            "<if test='serviceArea != null and serviceArea != \"\"'> " +
            "AND address LIKE CONCAT('%', #{serviceArea}, '%') " +
            "</if> " +
            "</script>")
    long count(@Param("providerName") String providerName,
               @Param("contactPerson") String contactPerson,
               @Param("contactPhone") String contactPhone,
               @Param("serviceArea") String serviceArea);

    /**
     * 更新草稿状态
     */
    @Update("UPDATE service_provider_draft SET status = #{status}, opinion = #{opinion}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int updateStatus(ServiceProviderDraftPo po);

    /**
     * 插入草稿（用于测试）
     */
    @Insert("INSERT INTO service_provider_draft (service_provider_id, provider_name, contact_person, " +
            "contact_phone, address, status, created_at, updated_at) " +
            "VALUES (#{serviceProviderId}, #{providerName}, #{contactPerson}, #{contactPhone}, " +
            "#{address}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ServiceProviderDraftPo po);
}

