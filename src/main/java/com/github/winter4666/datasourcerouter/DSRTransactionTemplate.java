package com.github.winter4666.datasourcerouter;

import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.github.winter4666.datasourcerouter.DataSourceRouter.DataSourceCallback;

/**
 * 继承 org.springframework.transaction.support.TransactionTemplate，支持动态地选择数据源
 * @author wutian
 */
@SuppressWarnings("serial")
public class DSRTransactionTemplate extends TransactionTemplate {
	
	private DataSourceRouter dataSourceRouter;
	
	public void setDataSourceRouter(DataSourceRouter dataSourceRouter) {
		this.dataSourceRouter = dataSourceRouter;
	}

	public <T> T execute(String lookupKey,final TransactionCallback<T> action) throws TransactionException {
		return dataSourceRouter.accessDb(lookupKey, new DataSourceCallback<T>() {

			@Override
			public T doWithDataSource() {
				return DSRTransactionTemplate.super.execute(action);
			}
			
		});
	}
	
	public <T> T execute(Object param,Class<? extends LookupKeyBuilder> lookupKeyBuilderClass,final TransactionCallback<T> action) throws TransactionException {
		return dataSourceRouter.accessDb(param,lookupKeyBuilderClass, new DataSourceCallback<T>() {

			@Override
			public T doWithDataSource() {
				return DSRTransactionTemplate.super.execute(action);
			}
			
		});
	}

}
