/**
 * Copyright (c) All rights reserved.
 */
package com.agilestage.core.common;

/**
 * 定义应用中要用到的常量. 用户可以向constants中添加自定义常量
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月29日
 */
public class Constants {

    /**
     * The name of the ResourceBundle used in this application
     */
    public static final String BUNDLE_KEY = "ApplicationResources";

    /**
     * File separator from System properties
     */
    public static final String FILE_SEP = System.getProperty("file.separator");

    /**
     * User home from System properties
     */
    public static final String USER_HOME = System.getProperty("user.home") + FILE_SEP;

    /**
     * 应用级别的全局配置哈西表变量名.
     */
    public static final String CONFIG = "appConfig";

    /** 默认编码 */
    public static final String DEFAULT_ENCODING = "UTF-8";

}
