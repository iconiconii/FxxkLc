package com.codetop.logging;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MyBatis interceptor for SQL logging with execution metrics.
 * Logs SQL statements, parameters, and execution time for performance monitoring.
 * 
 * @author CodeTop Team
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
@Component
@Slf4j
public class SqlLoggingInterceptor implements Interceptor {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs().length > 1 ? invocation.getArgs()[1] : null;
        
        String sqlId = mappedStatement.getId();
        String sqlType = mappedStatement.getSqlCommandType().name();
        
        // Get the actual SQL with parameters
        BoundSql boundSql = mappedStatement.getBoundSql(parameter);
        String sql = formatSql(boundSql.getSql());
        
        TraceContext.setOperation("SQL_" + sqlType);
        
        log.debug("SQL execution started: sqlId={}, type={}, sql={}", sqlId, sqlType, sql);
        
        try {
            // Execute the SQL
            Object result = invocation.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            int affectedRows = getAffectedRowCount(result, sqlType);
            
            // Log successful SQL execution
            log.info("SQL executed successfully: sqlId={}, type={}, duration={}ms, affectedRows={}", 
                    sqlId, sqlType, duration, affectedRows);
            
            // Log the formatted SQL with parameters for debugging
            if (log.isDebugEnabled()) {
                String sqlWithParams = getSqlWithParameters(boundSql, mappedStatement.getConfiguration());
                log.debug("SQL with parameters: sqlId={}, sql={}", sqlId, sqlWithParams);
            }
            
            // Performance warning for slow queries
            if (duration > 1000) { // More than 1 second
                log.warn("Slow SQL query detected: sqlId={}, type={}, duration={}ms, sql={}", 
                        sqlId, sqlType, duration, sql);
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("SQL execution failed: sqlId={}, type={}, duration={}ms, error={}, sql={}", 
                    sqlId, sqlType, duration, e.getMessage(), sql, e);
            
            throw e;
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(java.util.Properties properties) {
        // No configuration properties needed
    }

    /**
     * Format SQL for better readability in logs.
     */
    private String formatSql(String sql) {
        if (sql == null) {
            return null;
        }
        
        // Remove extra whitespace and newlines
        return WHITESPACE_PATTERN.matcher(sql.trim()).replaceAll(" ");
    }

    /**
     * Get the count of affected rows from the result.
     */
    private int getAffectedRowCount(Object result, String sqlType) {
        if (result == null) {
            return 0;
        }
        
        if (result instanceof Integer) {
            return (Integer) result;
        }
        
        if (result instanceof List) {
            return ((List<?>) result).size();
        }
        
        // For SELECT queries, we typically get a single result
        if ("SELECT".equals(sqlType)) {
            return 1;
        }
        
        return 0;
    }

    /**
     * Replace parameter placeholders with actual values for debugging.
     */
    private String getSqlWithParameters(BoundSql boundSql, Configuration configuration) {
        try {
            Object parameterObject = boundSql.getParameterObject();
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
            
            if (parameterMappings.size() > 0 && parameterObject != null) {
                TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
                if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
                } else {
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    for (ParameterMapping parameterMapping : parameterMappings) {
                        String propertyName = parameterMapping.getProperty();
                        if (metaObject.hasGetter(propertyName)) {
                            Object obj = metaObject.getValue(propertyName);
                            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                        } else if (boundSql.hasAdditionalParameter(propertyName)) {
                            Object obj = boundSql.getAdditionalParameter(propertyName);
                            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                        }
                    }
                }
            }
            
            return sql;
        } catch (Exception e) {
            log.debug("Error formatting SQL with parameters: {}", e.getMessage());
            // Return the original SQL if parameter replacement fails
            return boundSql.getSql().replaceAll("[\\s]+", " ");
        }
    }

    /**
     * Convert parameter value to string for logging.
     */
    private String getParameterValue(Object obj) {
        String value;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format((Date) obj) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "null";
            }
        }
        
        // Truncate very long values
        if (value.length() > 100) {
            value = value.substring(0, 97) + "...";
        }
        
        return value;
    }
}