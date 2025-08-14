package com.codetop.config;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * Custom IllegalSQL interceptor with relaxed OR clause restrictions for business scenarios.
 * 
 * This interceptor allows legitimate business queries that use OR clauses while still
 * preventing dangerous SQL operations by implementing custom validation logic.
 * 
 * @author CodeTop Team
 */
@Slf4j
public class CustomIllegalSQLInnerInterceptor implements InnerInterceptor {

    /**
     * List of allowed OR clause patterns for business scenarios
     */
    private static final List<String> ALLOWED_OR_PATTERNS = Arrays.asList(
        // Authentication queries (email OR username)
        "EMAIL =",
        "USERNAME =",
        "EMAIL IN",
        "USERNAME IN",
        // Multiple company filtering
        "COMPANY_ID IN",
        "COMPANY_ID =",
        // Multiple difficulty level filtering  
        "DIFFICULTY IN",
        "DIFFICULTY =",
        // Multiple status filtering
        "STATUS IN",
        "STATUS =",
        // Multiple tag filtering
        "TAG_ID IN",
        "TAG_ID =",
        // User ID filtering
        "USER_ID IN",
        "USER_ID =",
        // Problem ID filtering
        "PROBLEM_ID IN",
        "PROBLEM_ID =",
        // FSRS state filtering
        "CARD_STATE IN",
        "CARD_STATE =",
        // Review status filtering
        "REVIEW_STATUS IN",
        "REVIEW_STATUS ="
    );

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, 
                          RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        
        String sql = boundSql.getSql();
        
        // Check if SQL contains OR clauses
        if (sql.toUpperCase().contains(" OR ")) {
            if (isAllowedOrClause(sql)) {
                log.debug("Allowed OR clause in SQL: {}", sql);
                return; // Allow the query
            } else {
                log.warn("Blocked potentially dangerous OR clause in SQL: {}", sql);
                throw new RuntimeException("Potentially dangerous SQL with OR clause detected. " +
                    "Only specific business OR patterns are allowed.");
            }
        }
        
        // For non-OR queries, allow without validation
        log.debug("Allowed non-OR query: {}", sql);
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) {
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();
        
        // Check if SQL contains OR clauses
        if (sql.toUpperCase().contains(" OR ")) {
            if (isAllowedOrClause(sql)) {
                log.debug("Allowed OR clause in update SQL: {}", sql);
                return; // Allow the update
            } else {
                log.warn("Blocked potentially dangerous OR clause in update SQL: {}", sql);
                throw new RuntimeException("Potentially dangerous SQL with OR clause detected. " +
                    "Only specific business OR patterns are allowed.");
            }
        }
        
        // For non-OR queries, allow without validation
        log.debug("Allowed non-OR update: {}", sql);
    }

    /**
     * Check if OR clause is allowed based on business patterns.
     * 
     * @param sql The SQL statement to validate
     * @return true if OR clause is allowed, false otherwise
     */
    private boolean isAllowedOrClause(String sql) {
        String upperSql = sql.toUpperCase();
        
        // Quick check for WHERE clause
        if (!upperSql.contains("WHERE")) {
            return false;
        }
        
        // Extract WHERE clause portion
        String whereClause = extractWhereClause(upperSql);
        if (whereClause == null) {
            return false;
        }
        
        // Check if any allowed patterns are present
        for (String pattern : ALLOWED_OR_PATTERNS) {
            if (whereClause.contains(pattern.toUpperCase())) {
                return true;
            }
        }
        
        // Additional validation: Check if OR clauses are part of IN statements
        // This allows queries like "WHERE field IN (value1, value2, value3)"
        if (whereClause.contains(" IN (") && hasMultipleValuesInInClause(whereClause)) {
            return true;
        }
        
        return false;
    }

    /**
     * Extract the WHERE clause from SQL statement.
     * 
     * @param sql The SQL statement
     * @return WHERE clause portion or null if not found
     */
    private String extractWhereClause(String sql) {
        int whereIndex = sql.indexOf("WHERE");
        if (whereIndex == -1) {
            return null;
        }
        
        int endIndex = sql.length();
        
        // Find the end of WHERE clause
        int groupByIndex = sql.indexOf("GROUP BY", whereIndex);
        int orderByIndex = sql.indexOf("ORDER BY", whereIndex);
        int limitIndex = sql.indexOf("LIMIT", whereIndex);
        
        if (groupByIndex != -1) {
            endIndex = Math.min(endIndex, groupByIndex);
        }
        if (orderByIndex != -1) {
            endIndex = Math.min(endIndex, orderByIndex);
        }
        if (limitIndex != -1) {
            endIndex = Math.min(endIndex, limitIndex);
        }
        
        return sql.substring(whereIndex + 5, endIndex).trim();
    }

    /**
     * Check if IN clause contains multiple values (which is safe).
     * 
     * @param whereClause The WHERE clause to check
     * @return true if IN clause has multiple values
     */
    private boolean hasMultipleValuesInInClause(String whereClause) {
        int inIndex = whereClause.indexOf(" IN (");
        if (inIndex == -1) {
            return false;
        }
        
        int startIndex = inIndex + 5; // " IN (".length()
        int endIndex = whereClause.indexOf(')', startIndex);
        
        if (endIndex == -1) {
            return false;
        }
        
        String values = whereClause.substring(startIndex, endIndex).trim();
        
        // Count values separated by commas
        String[] valueArray = values.split(",");
        return valueArray.length > 1;
    }
}