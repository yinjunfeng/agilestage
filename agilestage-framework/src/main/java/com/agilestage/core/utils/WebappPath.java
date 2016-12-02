/**
 * Copyright (c) All rights reserved.
 */
package com.agilestage.core.utils;

import java.io.File;

/**
 * <b>取WEB应用资源的物理路径</b>
 * <p>
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月29日
 */
public final class WebappPath {
    private static String contextPath;

    public static String getContextPath() {
        return contextPath;
    }

    public static void setContextPath(String ctxPath) {
        contextPath = ctxPath;
    }

    /**
     * 获取当前WEB应用部署的根路径
     * 
     * @return 路径结尾无"/"，如: <code>"D:/Tomcat5/webapps/itsm"（windows）或"/usr/Tomcat5/webapps/itsm"（*nix）</code>
     */
    public static String getRootPath() {
        return getRealPath();
    }

    /**
     * 获取当前WEB应用部署的根文件
     */
    public static File getRootFile() {
        return new File(getRealPath());
    }

    /**
     * 获取当前WEB应用中指定资源的实际路径
     * 
     * @param pathes 未指定时取根路径
     * @return 路径结尾无"/"
     * @throws IllegalArgumentException 当资源路径中包含".."导致其位置超出WEB应用的根路径时抛出
     */
    public static String getRealPath(String... pathes) {
        // getPathFile在拼接路径时已为绝对路径，因此，无需调用getAbsolutePath
        return getPathFile(pathes).getPath();
    }

    /**
     * 获取当前WEB应用中指定路径的文件
     * 
     * @param pathes 未指定时取根路径
     * @throws IllegalArgumentException 当资源路径中包含".."导致其位置超出WEB应用的根路径时抛出
     */
    public static File getPathFile(String... pathes) {
        String realPath = null;
        String rootPath = "/";

        realPath = WebappPath.class.getResource(rootPath).getPath();

        int index = realPath.indexOf("WEB-INF");
        if (index < 0) {
            index = realPath.indexOf("bin");
        }
        if (index > 0) {
            realPath = realPath.substring(0, index);
        }

        return FileUtil.subfile(realPath, pathes);
    }
}
