/**
 * Copyright (c) All rights reserved.
 */
package com.agilestage.core;

/**
 * <p>
 * 组件监听接口，平台中的组件可通过在component-def.xml中注册ComponentListener的实现类 来实现在组件安装、卸载和启动时添加额外的操作，如在卸载组件时执行其预定义的数据库删除脚本.
 * </p>
 * 在组件的运行生命周期中，如果需要在某个过程中进行额外的处理，需要在组件的定义文件中为组件注册 相应的生命周期事件监听器。如下所示：
 * 
 * <pre>
 *  &lt;listener type ="javabean"&gt;com.otu.platform.runtime.RuntimeEnvDetectListener&lt;/listener&gt;
 * </pre>
 * 
 * 当监听器为一个springbean时:
 * 
 * <pre>
 *  &lt;listener type ="spring"&gt;runtimeEnvDetectListener&lt;/listener&gt;
 * </pre>
 * 
 * 注册的监听器必须实现此接口中的所有方法。
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月28日
 */
public interface ComponentListener {

    /**
     * 平台在激活组件前会触发beforeActive事件 此方法由组件的beforeActive事件触发
     */
    void beforeActive();

    /**
     * 平台在激活组件后会触发afterActive事件 此方法由组件的afterActive事件触发
     */
    void afterActive();

    /**
     * 平台在部署组件前会触发beforeDeploy事件 此方法由组件的beforeDeploy事件触发
     */
    void beforeDeploy();

    /**
     * 平台在部署组件前会触发afterDeploy事件 此方法由组件的afterDeploy事件触发
     */
    void afterDeploy();

    /**
     * 平台在删除组件前会触发beforeRemove事件 此方法由组件的beforeRemove事件触发
     */
    void beforeRemove();

    /**
     * 平台在删除组件后会触发afterRemove事件 此方法由组件的afterRemove事件触发
     */
    void afterRemove();

    /**
     * 在平台每次启动时触发startup事件，此方法监听平台组件的startup事件
     */
    void onStartup();
}
