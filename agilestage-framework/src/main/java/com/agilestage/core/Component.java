package com.agilestage.core;

import java.text.MessageFormat;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.agilestage.core.utils.SpringBeanUtils;

/**
 * Component定义为安装到平台上的一个组件 <br/>
 * Component类为平台内部的信息存储类，不允许随意创建和删除<br/>
 * 开发者可以通过getter读取组件的相关信息。<br/>
 * 对Component的操作参考{@link Platform}
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月28日
 */
public class Component {

    /** 平台支持的监听器类型：Java Bean */
    public static final String LISTENERTYPE_JAVABEAN = "javabean";
    /** 平台支持的监听器类型：Spring Bean */
    public static final String LISTENERTYPE_SPRING = "spring";

    /** 组件状态：激活 */
    public static final String STATE_ACTIVE = "active";
    /** 组件状态：禁用 */
    public static final String STATE_DISABLE = "disable";

    private static final Logger log = LoggerFactory.getLogger(Component.class);

    /** 名称——必须配置 */
    private String name;
    /** 编码——必须配置 */
    private String code;
    /** 版本——必须配置 */
    private String version;
    /** 描述——可选配置 */
    private String description;
    /** 组件状态——必须配置，状态不是从组件的定义文件中读取的，而是根据组件的注册文件来判断的 */
    private String state = STATE_DISABLE;
    /** 组件定义文件(components-def.xml)的物理位置，此属性通常用于定位平台对资源文件的操作 */
    private Resource local;
    /** 入口url——必需配置 */
    private String enter;
    /** 配置文件名——必需配置 */
    private String configFile;

    private Properties configProps;
    /** 组件事件监听接口——可选配置 */
    private ComponentListener cmpListener;
    /** 扩展属性——可选配置 */
    private Properties properties;

    Component() {
    }

    Component(String name, String code, String version) {
        this.name = name;
        this.code = code;
        this.version = version;
    }

    Component(final Element cmpElement) {

        this.name = StringUtils.trim(cmpElement.elementText("name"));
        this.code = StringUtils.trim(cmpElement.elementText("code"));
        this.version = StringUtils.trim(cmpElement.elementText("version"));
        this.description = StringUtils.trim(cmpElement.elementText("description"));

        this.enter = StringUtils.trim(cmpElement.elementText("enter"));

        // 兼容的 老式 config 处理
        this.configFile = StringUtils.trim(cmpElement.elementText("config-file"));

        // 处理 config
        initCfg(cmpElement.element("config"));

        // 处理listener
        initListener(cmpElement.element("listener"));

        // 处理 扩展属性
        initExtProperties(cmpElement.element("properties"));
    }

    @SuppressWarnings("unchecked")
    private void initCfg(Element el) {
        if (null != el) {
            this.configFile = el.attributeValue("file");
            List<Element> itemElList = el.elements("item");
            this.configProps = new Properties();
            for (int i = 0; i < itemElList.size(); i++) {
                this.configProps.setProperty(itemElList.get(i).attributeValue("name"), itemElList.get(i).getTextTrim());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void initListener(Element el) {
        if (null != el) {
            String listenerName = el.getTextTrim();
            String listenerType = el.attributeValue("type");

            ComponentListener listener = null;
            if (LISTENERTYPE_SPRING.equals(listenerType)) {
                listener = (ComponentListener) SpringBeanUtils.getBean(listenerName);
            } else if (LISTENERTYPE_JAVABEAN.equals(listenerType)) {
                try {
                    Class<ComponentListener> adapterClass = (Class<ComponentListener>) Class.forName(listenerName);

                    listener = SpringBeanUtils.createBean(adapterClass);
                } catch (Exception e) {
                    log.error("Exception while creating component listner '{}': {}", listenerName,
                              ExceptionUtils.getRootCauseMessage(e));
                }
            } else {
                log.error("Unknown listener type '{}' for component {}.", listenerType, this.code);
            }

            this.cmpListener = listener;
        }
    }

    @SuppressWarnings("unchecked")
    private void initExtProperties(Element el) {
        String propsElName = "property";
        if (null != el && null != el.elements(propsElName)) {
            this.properties = new Properties();
            List<Element> propElList = el.elements(propsElName);
            for (Element entry : propElList) {
                this.properties.put(entry.attributeValue("name"), entry.getTextTrim());
            }
        }
    }

    public String getName() {
        return this.name;
    }

    void setName(final String name) {
        this.name = name;
    }

    public String getCode() {
        return this.code;
    }

    void setCode(final String code) {
        this.code = code;
    }

    public String getVersion() {
        return this.version;
    }

    void setVersion(final String version) {
        this.version = version;
    }

    public String getEnter() {
        return this.enter;
    }

    void setEnter(final String enter) {
        this.enter = enter;
    }

    public String getConfigFile() {
        return this.configFile;
    }

    void setConfigFile(final String configFile) {
        this.configFile = configFile;
    }

    public Properties getConfigProps() {
        return this.configProps;
    }

    public String getDescription() {
        return this.description;
    }

    void setDescription(final String description) {
        this.description = description;
    }

    public Properties getProperties() {
        return this.properties;
    }

    void setProperties(final Properties properties) {
        this.properties = properties;
    }

    public String getState() {
        return this.state;
    }

    void setState(final String state) {
        this.state = state;
    }

    void setState(final Configuration config) {
        this.state = config.getString(getStateKey());
    }

    public Resource getLocal() {
        return this.local;
    }

    void setLocal(final Resource local) {
        this.local = local;
    }

    public ComponentListener getCmpListener() {
        return this.cmpListener;
    }

    void setCmpListener(final ComponentListener cmpListener) {
        this.cmpListener = cmpListener;
    }

    public String getStateKey() {

        String keyPatten = "agilestage.component.{0}.state";

        return MessageFormat.format(keyPatten, this.code);
    }

    @Override
    public String toString() {
        return "Component[name=" + getName() + ", code=" + getCode() + ", version=" + getVersion() + "]";
    }
}
