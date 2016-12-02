/**
 * Copyright (c) All rights reserved.
 */
package com.agilestage.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

/**
 * 平台启动监听
 * <p>
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月29日
 */
@Service
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupListener.class);

    private static final long TOSEC_RATE = 1000;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(event.getApplicationContext().getParent() == null){
			log.info("Initializing platform...");

	        long start = System.currentTimeMillis();

	        // 启动平台
	        Platform.getInstance().start();

	        log.info("platform started in {} sec.", (System.currentTimeMillis() - start) / TOSEC_RATE);
		}
	}
   
}
