package com.agilestage.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.agilestage.core.common.Constants;
import com.agilestage.core.utils.FileUtil;
import com.agilestage.core.utils.WebappPath;
import com.agilestage.core.utils.XmlUtils;

/**
 * 平台信息存储类
 * <p>
 * 在系统启动初始化时一起初始化类中的信息持久化到components-reg.xml中
 * <p>
 * 平台自身的操作包括： 启动 刷新
 * <p>
 * 平台启动的主要工作包括：
 * <ul>
 * <li>释放平台的webapp</li>
 * <li>扫描并发现组件</li>
 * <li>检查组件状态 —— 组件的状态参见{@link Component}</li>
 * <li>安装新发现的组件（释放webapp资源；处理组件定义文件内容）</li>
 * </ul>
 * 平台对组件的管理包括： 激活 禁用 部署 删除
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月29日
 */
public final class Platform {

    private static final Logger log = LoggerFactory.getLogger(Platform.class);

    private static final String VERSION = "0.1";

    /** 组件定义文件名 */
    private static final String COMPONENTS_DEF = "components-def.xml";
    /** 平台配置文件 */
    private static final String CONFIG_LOCATION = "/agilestage.properties";
    /** 平台日志处理文件 */
    private static final String LOG_LOCATION = "/log4j.properties";

    /**
     * 平台支持的所有事件类型
     */
    enum EventType {
        onStartup,
        beforeActive,
        afterActive,
        beforeDeploy,
        afterDeploy,
        beforeRemove,
        afterRemove
    }

    /**
     * 组件列表,此属性中的值不允许在该对象之外进行任何修改，以保证系统的一致性
     */
    private static final Map<String, Component> components = new HashMap<String, Component>();

    private static Platform platform;

    /** 系统配置属性 */
    private PropertiesConfiguration config;

    /** 日志属性 */
    private PropertiesConfiguration logStatus;

    /** 是否启动标识，用户控制start方法只执行一次 */
    private boolean started;

    /**
     * 单例类，不允许实例化 在构造时加载平台配置参数
     */
    private Platform() {

        this.config = new PropertiesConfiguration();
        this.logStatus = new PropertiesConfiguration();

        try {
            URL configLocation = getClass().getResource(CONFIG_LOCATION);
            this.config.setPath(URLDecoder.decode(configLocation.getPath(), Constants.DEFAULT_ENCODING));
            this.config.setAutoSave(true);

            URL logLocation = getClass().getResource(LOG_LOCATION);
            this.logStatus.setPath(URLDecoder.decode(logLocation.getPath(), Constants.DEFAULT_ENCODING));
            this.logStatus.setAutoSave(true);

            this.config.load();
            this.logStatus.load();

            initSystemProperty();
        } catch (ConfigurationException e) {
            log.info(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            log.info(e.getMessage(), e);
        }

    }

    /**
     * 将写入配置文件中的内容加载为系统变量，以便进行启动控制。注意：在系统启动后配置文件的更新不会同步到系统变量中来。
     */
    private void initSystemProperty() {
        InputStream is = getClass().getResourceAsStream(CONFIG_LOCATION);
        Properties prop = new Properties();
        try {
            prop.load(is);
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }
        for (String key : prop.stringPropertyNames()) {
        	if(StringUtils.isNotBlank(key)) {
        		 System.setProperty(key, prop.getProperty(key));
        	}
        }
    }

    /**
     * 单例
     * 
     * @return Platform
     */
    public static Platform getInstance() {
        if (null == platform) {
            platform = new Platform();
        }
        return platform;
    }

    /**
     * 激活组件 目前仅修改了注册状态，未做任何其他操作
     */
    public void active(final String code) {

        log.info("activating component：{} ...", code);

        Component cmp = components.get(code);

        fireEvent(cmp, EventType.beforeActive);

        changeComponentState(cmp, Component.STATE_ACTIVE);

        fireEvent(cmp, EventType.afterActive);
    }

    /**
     * 更新组件的状态
     * 
     * @param cmp
     * @param state
     */
    private void changeComponentState(final Component cmp, final String state) {
        cmp.setState(state);
        this.config.setProperty(cmp.getStateKey(), state);
    }

    /**
     * 部署组件 1、处理settings 2、处理webapp
     * 
     * @param cmp
     */
    public void deploy(final Component cmp) {

        fireEvent(cmp, EventType.beforeDeploy);

        log.info("deploying component: {}...", cmp.getCode());

        if (!cmp.equals(components.get(cmp.getCode()))) {
            components.put(cmp.getCode(), cmp);
        }

        if (cmp.getLocal() != null) {
            // 处理settings
            regSettings(cmp);

            // 释放webapp
            deployWebResource(cmp);
        }

        changeComponentState(cmp, Component.STATE_DISABLE);

        log.info("deploy component：{} completed.", cmp.getCode());

        fireEvent(cmp, EventType.afterDeploy);
    }

    /**
     * 取消激活
     * 
     * @param code
     */
    public void disable(final String code) {

        Component cmp = components.get(code);

        // TODO 暂时未定义处理

        if (null != cmp) {
            changeComponentState(cmp, Component.STATE_DISABLE);
        }

    }

    /**
     * 根据code 获取组件描述
     * 
     * @param code
     * @return
     */
    public Component getComponent(final String code) {

        return components.get(code);
    }

    /**
     * 只读方式获取平台中的全部组件
     * 
     * @return
     */
    public Map<String, Component> getComponents() {

        return Collections.unmodifiableMap(components);
    }

    /**
     * 获取平台的配置信息
     * 
     * @return
     */
    public PropertiesConfiguration getConfiguration() {
        return this.config;
    }

    /**
     * 获取日志属性信息
     * 
     * @return
     */
    public PropertiesConfiguration getLogStatus() {
        return this.logStatus;
    }

    /**
     * 获取平台中的全部组件的列表
     * <p>
     * 
     * @return
     */
    public List<Component> getComponentList() {
        Component[] cmps = components.values().toArray(new Component[] {});

        List<Component> cmpList = new ArrayList<Component>();
        for (Component cmp : cmps) {
            cmpList.add(cmp);
        }

        return cmpList;
    }

    /**
     * 刷新平台，重新加载组件
     * <ol>
     * <li>释放平台的webapp</li>
     * <li>扫描classpath将组件信息加载到平台中</li>
     * <li>读取组件注册信息</li>
     * <li>检查组件状态，并自动处理新发现的组件</li>
     * </ol>
     */
    public void refresh() {

        // 扫描classpath，发现组件
        scanComponentsInClasspath();

        // 初始化组件状态
        stateCheck();
    }

    /**
     * 注册配置参数
     * 
     * @param cmp 需要注册配置属性的组件
     */
    private void regSettings(final Component cmp) {
        log.info("registering settings for component {}", cmp.getCode());

        String cfgName = cmp.getConfigFile();
        if (StringUtils.isNotBlank(cfgName)) {

            try {

                URL url = this.getClass().getResource("/" + cfgName);
                if (null != url) {
                    log.info("read component config from {}", url);

                    this.config.copy(new PropertiesConfiguration(url));
                    this.config.save();

                    log.info("regist success!");
                } else {
                    log.warn("config file error, read config from config.item");
                }

            } catch (ConfigurationException e) {
                log.info(e.getMessage(), e);
            }
        } else if (null != cmp.getConfigProps()) {

            log.info("regist config in component define file");

            Properties props = cmp.getConfigProps();
            for (Entry<?, ?> entry : props.entrySet()) {
                if (!this.config.containsKey((String) entry.getKey())) {
                    this.config.setProperty((String) entry.getKey(), entry.getValue());
                }
            }
            try {
                this.config.save();
            } catch (ConfigurationException e) {
                log.info(e.getMessage(), e);
            }
        }
    }

    /**
     * 释放组件的webapp目录到webroot中 拒绝web-inf目录的释放 拒绝meta-inf目录的是否
     */
    private void deployWebResource(final Component cmp) {

        log.info("releasing resources in  {} ...", cmp.getCode());
        try {
            // 复制前台资源文件到webRoot下
            URL url = cmp.getLocal().getURL();
            url = new URL(url, ".././webapp");

            copyWebapp(url);

        } catch (IOException e) {
            log.error("failed to release resources in component!\n" + e.getMessage(), e);
        }
    }

    /**
     * 复制指定url中的webapp到webroot中
     * 
     * @param url
     */
    private void copyWebapp(final URL url) {
        String rootPath = WebappPath.getRootPath();

        try {
            if (null != url && "jar".equals(url.getProtocol())) {
                JarURLConnection jarConn = (JarURLConnection) url.openConnection();
                JarFile jarFile = jarConn.getJarFile();
                String jarPath = jarFile.getName();

                log.info("releasing resources from jar to {}", rootPath);

                FileUtil.extractJar(jarPath, rootPath, "webapp/", "", null, true);

            } else if (null != url) {
                log.info("copy from {} to {}...", url.getPath(), rootPath);

                FileUtil.copyDirectory(url.getPath(), rootPath, false);
            } else {
                log.info("nothing to release!");
            }
        } catch (IOException e) {
        	e.printStackTrace();
            log.info("failed to release webapp!");
        }
    }

    /**
     * 删除组件 注意：删除组件目前不能把组件对于的jar包删除
     * 
     * @param cmp
     */
    public void remove(final Component cmp) {

        fireEvent(cmp, EventType.beforeRemove);

        // 删除释放出来的webapp
        removeWebapp(cmp);

        // 删除注册的settings
        removeSettings(cmp);

        // 在组件列表中将其删除
        components.remove(cmp.getCode());

        this.config.clearProperty(cmp.getStateKey());

        fireEvent(cmp, EventType.afterRemove);
    }

    /**
     * 删除组件:
     * <ol>
     * <li>删除释放出来的webapp</li>
     * <li>删除注册的setting</li>
     * <li>删除平台中此组件的注册信息</li>
     * <li>更新组件注册表</li>
     * </ol>
     * 注意：删除组件目前不能把组件对于的jar包删除
     * 
     * @param cmpCode
     */
    public void remove(final String cmpCode) {

        if (null != getComponent(cmpCode)) {
            remove(getComponent(cmpCode));
        }

    }

    /**
     * 删除组件注册时添加的配置属性
     * 
     * @param cmp
     */
    private void removeSettings(final Component cmp) {

        if (null == cmp) {
            return;
        }

        log.info("removing settings");

        String cfgName = cmp.getConfigFile();
        if (!StringUtils.isBlank(cfgName) && null != cmp.getLocal()) {

            try {

                URL url = cmp.getLocal().getURL();
                url = new URL(url, ".././" + cfgName);// 组件的默认配置文件位置

                PropertiesConfiguration propsCfg = new PropertiesConfiguration(url);

                this.config.setAutoSave(false);
                Iterator<String> itKeys = propsCfg.getKeys();
                while (itKeys.hasNext()) {
                    this.config.clearProperty(itKeys.next());
                }
                this.config.save();
                this.config.setAutoSave(true);

                log.info("settings remove success!");
            } catch (IOException e) {
                log.info("failed, canot find the default settings file for settings-remove!");
            } catch (ConfigurationException e) {
                log.info("failed, remove settings failed!");
            }
        }
    }

    /**
     * 清除组件部署时释放到webroot目录中的文件 （仅删除文件和空目录）
     * 
     * @param cmp
     */
    private void removeWebapp(final Component cmp) {
        if (null == cmp) {
            return;
        }

        log.info("removing webapp in component...");

        try {
            if (null != cmp.getLocal()) {
                URL url = cmp.getLocal().getURL();
                url = new URL(url, ".././webapp");

                if (null != url && "jar".equals(url.getProtocol())) {// webapp在jar包中
                    JarURLConnection jarConn = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarConn.getJarFile();

                    String destDir = WebappPath.getRootPath();

                    Enumeration<JarEntry> entrys = jarFile.entries();
                    JarEntry jarEntry = null;
                    String jarEntryName = "";
                    while (entrys.hasMoreElements()) {
                        jarEntry = entrys.nextElement();
                        jarEntryName = jarEntry.getName();

                        if (jarEntryName.startsWith("webapp/") && !jarEntry.isDirectory()) {
                            FileUtil.removeFile(destDir + jarEntryName.replaceFirst("webapp/", ""));
                        }
                    }

                } else { // webapp不在jar包中
                    log.info("do nothing for webapp in folder.");
                }
            }

        } catch (IOException e) {
            log.info("failed in remove webapp in component : {}.", cmp.getName());
            log.debug(e.getMessage(), e);
        }

        log.info("completed removing webapp for component.");
    }

    /**
     * 扫描classpath下的组件,并将组件添加到平台中
     */
    @SuppressWarnings("unchecked")
    private void scanComponentsInClasspath() {

        log.info("scanning for components...");

        String path = new StringBuilder("classpath*:/META-INF/").append(COMPONENTS_DEF).toString();

        PathMatchingResourcePatternResolver pathResolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] cmponentsResArr = pathResolver.getResources(path);

            Component cmp = null;
            InputStream is = null;
            Document doc = null;

            for (Resource cmponentsRes : cmponentsResArr) {
                is = cmponentsRes.getInputStream();
                doc = XmlUtils.createDoc(is);
                for (Element el : (List<Element>) doc.getRootElement().elements("component")) {
                    cmp = new Component(el);

                    cmp.setLocal(cmponentsRes);

                    components.put(cmp.getCode(), cmp);
                }
                is.close();
            }
        } catch (IOException e2) {
            log.error("error in reading component jar file info!", e2);
        } catch (DocumentException e) {
            log.error("error in reading component definition file info!", e);
        }

        log.info("scanning completed.");
    }

    /**
     * 启动platform <br/>
     * started 属性控制平台只被启动一次<br/>
     * 非public方法，限制其只能在本包内被访问到<br/>
     */
    void start() {
        if (!this.started) {
            this.started = true;
            refresh();

        } else {
            log.info("platform is already started.");
        }
    }

    /**
     * 在web环境下平台的启动方法
     * 
     * @param context
     */
    void start(ServletContext context) {
        start();
    }

    /**
     * 检查组件的状态，判断是否需要部署或重新部署
     */
    private void stateCheck() {

        log.info("checking component status...");

        String stateKey;
        for (Component cmp : getComponentList()) {
            stateKey = cmp.getStateKey();

            if (!StringUtils.isBlank(this.config.getString(stateKey))) {
                cmp.setState(this.config);
            } else {
                // 自动部署并激活未部署组件
                log.info("find new component：{}", cmp.getCode());
                deploy(cmp);
                active(cmp.getCode());
            }

            if (StringUtils.equals(Component.STATE_ACTIVE, cmp.getState())) {
                fireEvent(cmp, EventType.onStartup);
            }
        }
    }

    /**
     * 触发组件的平台事件监听
     * 
     * @param cmp
     * @param event
     */
    private void fireEvent(final Component cmp, final EventType event) {
        if (null != cmp && null != cmp.getCmpListener()) {
            ComponentListener listener = cmp.getCmpListener();

            switch (event) {
                case beforeActive:
                    listener.beforeActive();
                    break;
                case afterActive:
                    listener.afterActive();
                    break;
                case beforeDeploy:
                    listener.beforeDeploy();
                    break;
                case afterDeploy:
                    listener.afterDeploy();
                    break;
                case beforeRemove:
                    listener.beforeRemove();
                    break;
                case afterRemove:
                    listener.afterRemove();
                    break;
                case onStartup:
                    listener.onStartup();
                    break;
                default:
                    log.info("platform event {} is not supported!", event);
            }

        }
    }

    public String getVersion() {
        return VERSION;
    }
}