package com.framework.aop.logging;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.*;

/**
 * <p>REST API日志切面类，在访问REST API时，记录日志。</p>
 *
 * @Author xia_xiaojie
 * @CreateDate 2016/10/25
 * @Version 1.0
 */
@Aspect
@Component
public class RestLogAspect extends AbstractLogAspect {
    /** 日志 */
    private static final Logger logger = LoggerFactory.getLogger(RestLogAspect.class);

    private Map<String, Method> methodMap = new HashMap<String, Method>();

    /**
     * 切入点，pointcut注解的方法
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void pointcut() {}

    /**
     * <p>在目标方法执行前，记录方法名、参数类型和参数值。</p>
     *
     * @param joinPoint 切入点
     */
    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        try {
            String methodName = this.getMethodName(joinPoint);
            Method method = this.getMethod(joinPoint);
            methodMap.put(methodName, method);
            List<Map<String, Object>> parameters = this.mappingParamTypeToValue(joinPoint, method);

            Map<String, Object> content = new LinkedHashMap<String, Object>();
            content.put("name", methodName);
            content.put("parameters", parameters);
            this.record(method, content);
        } catch (Exception e) {
            logger.debug("RestLogAspect doBefore Exception: {}", e.toString());
        }
    }

    /**
     * <p>分格式记录日志。</p>
     *
     * @param method 目标方法
     * @param content 日志内容
     */
    private void record(Method method, Map<String, Object> content) {
        LogType logType = this.getLogType(method);
        if (LogType.JSON.equals(logType)) {
            this.toJsonLog(content);
        }
        else if (LogType.XML.equals(logType)) {
            this.toXmlLog(content);
        }
        else {
            this.toPlainLog(content);
        }
    }

    /**
     * <p>记录JSON格式日志。</p>
     *
     * @param content 日志内容
     */
    private void toJsonLog(Map<String, Object> content) {
        logger.info("Logging: {}", JSON.toJSONString(content));
    }

    /**
     * <p>记录XML格式日志。</p>
     *
     * @param content 日志内容
     */
    private void toXmlLog(Map<String, Object> content) {
        try {
            String jsonStr = JSON.toJSONString(content);
            JsonNode jsonNode = new ObjectMapper().readTree(jsonStr);
            String xml = new XmlMapper().writeValueAsString(jsonNode);
            logger.info("Logging: {}", xml);
        } catch (Exception e) {
            logger.debug("RestLogAspect toXmlLog Exception: {}", e.toString());
        }
    }

    /**
     * <p>记录文本格式日志。</p>
     *
     * @param content 日志内容
     */
    private void toPlainLog(Map<String, Object> content) {
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            String str = entry.getKey() + ": " + JSON.toJSONString(entry.getValue());
            logger.info("Logging: {}", str);
        }
    }


    /**
     * <p>根据目标方法返回的数据类型，获得日志类型。</p>
     *
     * @param method 目标方法
     * @return {@code "json"}或{@code "xml"}或{@code "plain"}
     */
    private LogType getLogType(Method method) {
        LogType logType = LogType.JSON;
        if (null == method) {
            return logType;
        }

        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        String[] produces = requestMapping.produces();
        if (produces != null && produces.length == 1) {
            String produce = produces[0];
            if (MediaType.TEXT_PLAIN.equals(produce)) {
                logType = LogType.PLAIN;
            }
            else if (MediaType.APPLICATION_ATOM_XML.equals(produce)
                    || MediaType.APPLICATION_XHTML_XML.equals(produce)
                    || MediaType.APPLICATION_XML.equals(produce)
                    || MediaType.TEXT_HTML.equals(produce)
                    || MediaType.TEXT_XML.equals(produce)) {
                logType = LogType.XML;
            }
        }
        return logType;
    }

    /**
     * <p>参数类型映射参数值。</p>
     *
     * @param joinPoint 切入点
     * @param method 目标方法
     * @return {@code [<消息标题, 消息内容>]}
     */
    private List<Map<String, Object>> mappingParamTypeToValue(JoinPoint joinPoint, Method method) {
        List<Map<String, Object>> parameters = new ArrayList<Map<String, Object>>();
        if (null == method) {
            return parameters;
        }

        String[] types = this.getParameterTypes(method);
        Object[] values = this.getParameters(joinPoint);

        for (int i = 0; i < types.length; ++i) {
            Map<String, Object> parameter = new LinkedHashMap<String, Object>();
            parameter.put("type", types[i]);
            parameter.put("value", values[i]);
            parameters.add(parameter);
        }
        return parameters;
    }

    @Override
    public Object around(ProceedingJoinPoint pjp) {
        return null;
    }
}
