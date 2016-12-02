/**
 * Copyright (c) All rights reserved.
 */
package com.agilestage.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ConfigurableWebApplicationContext;

/**
 * spring bean 相关操作工具类
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月28日
 */
@Component
public class SpringBeanUtils implements ApplicationContextAware{

	private Logger log = LoggerFactory.getLogger(SpringBeanUtils.class);
	
	private static ApplicationContext applicationContext = null;
	 
    @Override
    public void setApplicationContext(ApplicationContext applicationContext){
       if(SpringBeanUtils.applicationContext == null){
    	   SpringBeanUtils.applicationContext  = applicationContext;
       }
       log.info("========ApplicationContext配置成功,在普通类可以通过调用SpringUtils.getAppContext()获取applicationContext对象,applicationContext="+SpringBeanUtils.applicationContext+"========");
    }
   
    /**
     * 获取applicationContext
     * @return
     */
    public static ApplicationContext getApplicationContext() {
       return applicationContext;
    }
   
    /**
     * 通过name获取 Bean.
     * @param name
     * @return
     */
    public static Object getBean(String name){
       return getApplicationContext().getBean(name);
    }
   
    /**
     * 通过class获取Bean.
     * 
     * @param clazz
     * @return
     */
    public static <T> T getBean(Class<T> clazz){
       return getApplicationContext().getBean(clazz);
    }
   
    /**
     * 通过name,以及Clazz返回指定的Bean
     * @param name
     * @param clazz
     * @return
     */
    public static <T> T getBean(String name,Class<T> clazz){
       return getApplicationContext().getBean(name, clazz);
    }


    /**
     * 构建Spring Bean实例
     * <p>
     * 实体所依赖的其他对象也将被自动注入
     * 
     * @param clazz 所要实例化的类型
     * @throws BeansException 实例化异常时抛出
     */
    public static <T> T createBean(Class<T> clazz) {
        return getApplicationContext().getAutowireCapableBeanFactory().createBean(clazz);
    }

    /**
     * 加载或刷新spring配置文件
     */
    public static void refreshContext() {
        ((ConfigurableWebApplicationContext) getApplicationContext()).refresh();
    }
}