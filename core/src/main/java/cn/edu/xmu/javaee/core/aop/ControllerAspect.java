//School of Informatics Xiamen University, GPL-3.0 license
package cn.edu.xmu.javaee.core.aop;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import cn.edu.xmu.javaee.core.model.ReturnObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

import static cn.edu.xmu.javaee.core.model.Constants.BEGIN_TIME;
import static cn.edu.xmu.javaee.core.model.Constants.END_TIME;

/**
 * 用于控制器方面的Aspect
 */
@Aspect
@Component
@Order(10)
public class ControllerAspect {
    private final Logger logger = LoggerFactory.getLogger(ControllerAspect.class);

    @Value("${oomall.core.page-size.max}")
    private int max_page_size;

    @Value("${oomall.core.page-size.default}")
    private int default_page_size;

    /**
     * 所有返回值为ReturnObject的Controller
     *
     * @param jp
     * @return
     * @throws Throwable
     */
    @Around("cn.edu.xmu.javaee.core.aop.CommonPointCuts.controllers()")
    public Object doAround(ProceedingJoinPoint jp) throws Throwable {
        ReturnObject retVal = null;

        MethodSignature ms = (MethodSignature) jp.getSignature();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();

        String[] paramNames = ms.getParameterNames();
        logger.debug("doAround: method = {}, paramNames = {}", ms.getName(), paramNames);
        Object[] args = jp.getArgs();
        try {
            Object[] newArgs = checkPageTimeLimit(request, paramNames, args);
            retVal = (ReturnObject) jp.proceed(newArgs);
        } catch (BusinessException exception) {
            logger.info("doAround: BusinessException， errno = {}", exception.getErrno());
            retVal = new ReturnObject(exception.getErrno(), exception.getMessage());
        }

        ReturnNo code = retVal.getCode();
        logger.debug("doAround: jp = {}, code = {}", jp.getSignature().getName(), code);
        response.setStatus(code.getHttpStatus());
        return retVal;
    }

    /**
     * 设置默认的page = 1和pageSize = 10
     * 防止客户端发过来pagesize过大的请求
     *
     * @author maguoqi
     *
     * @param request
     * @param paramNames
     * @param args
     */
    private Object[] checkPageTimeLimit(HttpServletRequest request, String[] paramNames, Object[] args) {
        Integer page = 1, pageSize = default_page_size;
        LocalDateTime beginTime = BEGIN_TIME, endTime = END_TIME;

        if (request != null) {

            String pageString = request.getParameter("page");
            String pageSizeString = request.getParameter("pageSize");
            String beginTimeString = request.getParameter("beginTime");
            String endTimeString = request.getParameter("endTime");

            if (null != pageString && !pageString.isEmpty() && pageString.matches("\\d+")) {
                page = Integer.valueOf(pageString);
                if (page <= 0) {
                    page = 1;
                }
            }

            if (null != pageSizeString && !pageSizeString.isEmpty() && pageSizeString.matches("\\d+")) {
                pageSize = Integer.valueOf(pageSizeString);
                if (pageSize <= 0 || pageSize > max_page_size) {
                    pageSize = default_page_size;
                }
            }

            try {
                if (null != beginTimeString && null != endTimeString && !beginTimeString.isEmpty() && !endTimeString.isEmpty()) {
                    beginTime = LocalDateTime.parse(beginTimeString);
                    endTime = LocalDateTime.parse(endTimeString);
                    if (beginTime.isAfter(endTime)) {
                        beginTime = BEGIN_TIME;
                        endTime = END_TIME;
                    }
                }
            } catch (Exception e) {
                logger.debug("Exception occurs in time checking: {}", e.getMessage());
            }
        }

        for (int i = 0; i < paramNames.length; i++) {
            logger.debug("checkPageTimeLimit: paramNames[{}] = {}", i, paramNames[i]);
            if (paramNames[i].equals("page")) {
                logger.debug("checkPageTimeLimit: set {} to {}",paramNames[i], page);
                args[i] = page;
                continue;
            }

            if (paramNames[i].equals("pageSize")) {
                logger.debug("checkPageTimeLimit: set {} to {}",paramNames[i], pageSize);
                args[i] = pageSize;
                continue;
            }

            if (paramNames[i].equals("beginTime") && (args[i] == null)){
                logger.debug("checkPageTimeLimit: set {} to {}",paramNames[i], BEGIN_TIME);
                args[i] = beginTime;
                continue;
            }

            if (paramNames[i].equals("endTime") && (args[i] == null)){
                logger.debug("checkPageTimeLimit: set {} to {}",paramNames[i], END_TIME);
                args[i] = endTime;
            }
        }
        return args;
    }
}
