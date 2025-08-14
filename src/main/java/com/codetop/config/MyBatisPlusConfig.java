package com.codetop.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.codetop.config.CustomIllegalSQLInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * MyBatis-Plus configuration for enhanced database operations.
 * 
 * Features:
 * - Pagination support with MySQL optimization
 * - Optimistic locking for concurrent updates
 * - Block attack prevention (prevent full table updates/deletes)
 * - Illegal SQL prevention
 * - Auto-fill audit fields
 * 
 * @author CodeTop Team
 */
@Configuration
@Slf4j
public class MyBatisPlusConfig {

    /**
     * Configure MyBatis-Plus interceptors.
     * 
     * @return MybatisPlusInterceptor with optimized plugins
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // Pagination plugin
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(DbType.MYSQL);
        paginationInterceptor.setOverflow(false);
        paginationInterceptor.setMaxLimit(500L); // Maximum records per page
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        // Optimistic lock plugin
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // Block attack plugin (prevent delete/update without where clause)
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        
        // Custom Illegal SQL plugin with relaxed OR clause restrictions
        interceptor.addInnerInterceptor(createCustomIllegalSQLInterceptor());
        
        log.info("MyBatis-Plus interceptor configured with pagination, optimistic lock, and security plugins");
        return interceptor;
    }
    
    /**
     * Create custom IllegalSQL interceptor with relaxed OR clause restrictions.
     * 
     * @return Customized IllegalSQLInnerInterceptor
     */
    private CustomIllegalSQLInnerInterceptor createCustomIllegalSQLInterceptor() {
        CustomIllegalSQLInnerInterceptor interceptor = new CustomIllegalSQLInnerInterceptor();
        
        // Allow OR clauses in WHERE conditions for common business scenarios
        // This prevents blocking legitimate queries like:
        // - Multiple company filtering
        // - Multiple difficulty level filtering
        // - Multiple status filtering
        // - Multiple tag filtering
        log.info("Custom IllegalSQL interceptor configured with relaxed OR clause restrictions");
        
        return interceptor;
    }
    
    /**
     * Auto-fill handler for audit fields.
     */
    @Component
    public static class AuditMetaObjectHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            
            // Set creation time
            if (metaObject.hasGetter("createdAt")) {
                Object createdAt = getFieldValByName("createdAt", metaObject);
                if (createdAt == null) {
                    setFieldValByName("createdAt", now, metaObject);
                }
            }
            
            // Set update time
            if (metaObject.hasGetter("updatedAt")) {
                Object updatedAt = getFieldValByName("updatedAt", metaObject);
                if (updatedAt == null) {
                    setFieldValByName("updatedAt", now, metaObject);
                }
            }
            
            log.debug("Auto-filled audit fields for insert operation");
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            // Always update the modification time
            if (metaObject.hasGetter("updatedAt")) {
                setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
            }
            
            log.debug("Auto-filled audit fields for update operation");
        }
    }
}