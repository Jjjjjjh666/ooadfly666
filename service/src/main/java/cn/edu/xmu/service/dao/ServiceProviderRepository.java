package cn.edu.xmu.service.dao;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.service.dao.po.ServiceProviderPo;
import cn.edu.xmu.service.model.ServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 服务商仓储
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ServiceProviderRepository {

    private final ServiceProviderMapper mapper;

    /**
     * 根据ID查询服务商
     */
    public ServiceProvider findById(Long id) {
        ServiceProviderPo po = mapper.findById(id);
        if (po == null) {
            throw new BusinessException(ReturnNo.RESOURCE_NOT_FOUND, "服务商不存在: " + id);
        }
        return ServiceProvider.fromPo(po);
    }

    /**
     * 保存服务商
     */
    public void save(ServiceProvider provider) {
        ServiceProviderPo po = provider.toPo();
        int rows = mapper.update(po);
        if (rows == 0) {
            throw new BusinessException(ReturnNo.RESOURCE_NOT_FOUND, "服务商不存在: " + provider.getId());
        }
        log.info("服务商更新成功: id={}, name={}", provider.getId(), provider.getName());
    }

    /**
     * 创建服务商（用于测试）
     */
    public ServiceProvider create(ServiceProvider provider) {
        ServiceProviderPo po = provider.toPo();
        mapper.insert(po);
        provider.setId(po.getId());
        log.info("服务商创建成功: id={}", po.getId());
        return provider;
    }
}

