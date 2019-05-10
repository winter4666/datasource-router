package com.github.winter4666.datasourcerouter;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.github.winter4666.datasourcerouter.DataSourceRouter.DataSourceCallback;

/**
 * 切换数据源切面
 * @author wutian
 */
@Aspect
public class DSRAspect  {
	
	private DataSourceRouter dataSourceRouter;
	
	private DSRTransactionTemplate dsrTransactionTemplate;

	public void setDataSourceRouter(DataSourceRouter dataSourceRouter) {
		this.dataSourceRouter = dataSourceRouter;
	}
	
	public void setDsrTransactionTemplate(DSRTransactionTemplate dsrTransactionTemplate) {
		this.dsrTransactionTemplate = dsrTransactionTemplate;
	}

	@Around("@annotation(com.github.winter4666.datasourcerouter.DataSource) || @within(com.github.winter4666.datasourcerouter.DataSource)")
    public Object switchDatasource(final ProceedingJoinPoint pjp) throws Throwable {
    	Object object = pjp.getTarget();
    	//优先读取方法上的注解
    	MethodSignature methodSignature = (MethodSignature)pjp.getSignature();    
    	Method targetMethod = object.getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());   
    	DataSource dataSource = targetMethod.getAnnotation(DataSource.class);
    	if(dataSource == null) {
    		dataSource = object.getClass().getAnnotation(DataSource.class);
    	}
		if(dataSource.value() != null && !"".equals(dataSource.value())) {
			//value非空的时候，直接使用value的值作为lookupKey访问数据源
			return dataSourceRouter.accessDb(dataSource.value(), new DataSourceCallback<Object>() {

				@Override
				public Object doWithDataSource() {
					try {
						return pjp.proceed();
					} catch (Throwable e) {
						throw new RuntimeException("ProceedingJoinPoint proceed error",e);
					}
				}
			});
		} else {
			if(dataSource.param() != null && !"".equals(dataSource.param())) {
				//param非空，调用LookupKeyBuilder组装lookupKey访问数据源
				ExpressionParser parser = new SpelExpressionParser();
				Expression exp = parser.parseExpression(dataSource.param());
				EvaluationContext context = new StandardEvaluationContext();
				for (int i = 0; i < pjp.getArgs().length; i++) {
					context.setVariable("a" + i, pjp.getArgs()[i]);
					context.setVariable("p" + i, pjp.getArgs()[i]);
				}
				Object param = exp.getValue(context);
				return dataSourceRouter.accessDb(param, dataSource.builderClass(), new DataSourceCallback<Object>() {

					@Override
					public Object doWithDataSource() {
						try {
							return pjp.proceed();
						} catch (Throwable e) {
							throw new RuntimeException("ProceedingJoinPoint proceed error",e);
						}
					}
				});
			} else {
				//param为空，访问所有数据源获取数据结果
				return dataSourceRouter.accessDbsForOneResult(dataSource.builderClass(), new DataSourceCallback<Object>() {

					@Override
					public Object doWithDataSource() {
						try {
							return pjp.proceed();
						} catch (Throwable e) {
							throw new RuntimeException("ProceedingJoinPoint proceed error",e);
						}
					}
				});
			}
		}
    }
    
    @Around("@annotation(com.github.winter4666.datasourcerouter.DSRTransactional) || @within(com.github.winter4666.datasourcerouter.DSRTransactional)")
    public Object executeTransaction(final ProceedingJoinPoint pjp) throws Throwable {
    	Object object = pjp.getTarget();
    	//优先读取方法上的注解
    	MethodSignature methodSignature = (MethodSignature)pjp.getSignature();    
    	Method targetMethod = object.getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());   
    	DSRTransactional dsrTransactional = targetMethod.getAnnotation(DSRTransactional.class);
    	if(dsrTransactional == null) {
    		dsrTransactional = object.getClass().getAnnotation(DSRTransactional.class);
    	}
		if(dsrTransactional.value() != null && !"".equals(dsrTransactional.value())) {
			//value非空的时候，直接使用value的值作为lookupKey访问数据源
			return dsrTransactionTemplate.execute(dsrTransactional.value(), new TransactionCallback<Object>() {
				
				public Object doInTransaction(TransactionStatus status) {
					try {
						return pjp.proceed();
					} catch (Throwable e) {
						throw new RuntimeException("ProceedingJoinPoint proceed error",e);
					}
				}
			});
		} else {
			if(dsrTransactional.param() != null && !"".equals(dsrTransactional.param())) {
				//param非空，调用LookupKeyBuilder组装lookupKey访问数据源
				ExpressionParser parser = new SpelExpressionParser();
				Expression exp = parser.parseExpression(dsrTransactional.param());
				EvaluationContext context = new StandardEvaluationContext();
				for (int i = 0; i < pjp.getArgs().length; i++) {
					context.setVariable("a" + i, pjp.getArgs()[i]);
					context.setVariable("p" + i, pjp.getArgs()[i]);
				}
				Object param = exp.getValue(context);
				return dsrTransactionTemplate.execute(param, dsrTransactional.builderClass(), new TransactionCallback<Object>() {
					
					public Object doInTransaction(TransactionStatus status) {
						try {
							return pjp.proceed();
						} catch (Throwable e) {
							throw new RuntimeException("ProceedingJoinPoint proceed error",e);
						}
					}
				});
			} else {
				throw new RuntimeException("can not confirm data source");
			}
		}
    }
	
}
