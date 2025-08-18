package com.codetop.config;

import com.codetop.logging.SqlLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Configuration for MyBatis logging interceptors.
 * Registers SQL logging interceptor with all SqlSessionFactory instances.
 * 
 * @author CodeTop Team
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class MyBatisLoggingConfig {

    private final SqlLoggingInterceptor sqlLoggingInterceptor;
    
    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @PostConstruct
    public void addSqlLoggingInterceptor() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(sqlLoggingInterceptor);
        }
        log.info("SQL logging interceptor registered with {} SqlSessionFactory instances", 
                sqlSessionFactoryList.size());
    }
}