//School of Informatics Xiamen University, GPL-3.0 license
package cn.edu.xmu.javaee.core.aop;

import cn.edu.xmu.javaee.core.exception.BusinessException;
import cn.edu.xmu.javaee.core.model.UserToken;
import cn.edu.xmu.javaee.core.util.JwtHelper;
import cn.edu.xmu.javaee.core.model.ReturnNo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @auther mingqiu
 * @date 2020/6/26 下午2:16
 *      modifiedBy Ming Qiu 2020/11/3 22:59
 *
 */
@Aspect
@Component
@Order(20)
@Slf4j
public class AuditAspect {

    /**
     * @Audit的Around Advice
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("cn.edu.xmu.javaee.core.aop.CommonPointCuts.auditAnnotation()")
    public Object aroundAudit(JoinPoint joinPoint) throws  Throwable{

        MethodSignature ms = (MethodSignature) joinPoint.getSignature();
        Method method = ms.getMethod();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        Object[] args = joinPoint.getArgs();
        Annotation[][] annotations = method.getParameterAnnotations();

        Object obj = null;
        try {
            obj = ((ProceedingJoinPoint) joinPoint).proceed(args);
        } catch (Throwable e) {
            throw e;
        }
        return obj;
    }
}
