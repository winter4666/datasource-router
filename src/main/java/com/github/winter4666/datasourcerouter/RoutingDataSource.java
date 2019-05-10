package com.github.winter4666.datasourcerouter;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 实现AbstractRoutingDataSource
 * @author wutian
 */
public class RoutingDataSource extends AbstractRoutingDataSource{

	@Override
	protected Object determineCurrentLookupKey() {
		return ContextHolder.getLookupKey();
	}

}
