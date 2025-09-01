package com.codetop.service.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 统一缓存服务接口
 * 
 * 提供统一的缓存操作抽象，支持多种数据类型的缓存读写、
 * TTL管理、批量操作和模式匹配删除等功能。
 * 
 * @author CodeTop FSRS Team
 */
public interface CacheService {
    
    /**
     * 存储对象到缓存，使用指定的TTL
     * 
     * @param key 缓存键
     * @param value 要缓存的对象
     * @param ttl 存活时间
     * @param <T> 对象类型
     */
    <T> void put(String key, T value, Duration ttl);
    
    /**
     * 存储对象到缓存，使用默认TTL (1小时)
     * 
     * @param key 缓存键
     * @param value 要缓存的对象
     * @param <T> 对象类型
     */
    <T> void put(String key, T value);
    
    /**
     * 从缓存获取对象
     * 
     * @param key 缓存键
     * @param type 对象类型
     * @param <T> 对象类型
     * @return 缓存的对象，如果不存在返回null
     */
    <T> T get(String key, Class<T> type);
    
    /**
     * 从缓存获取列表
     * 
     * @param key 缓存键
     * @param typeRef 列表类型引用
     * @param <T> 列表元素类型
     * @return 缓存的列表，如果不存在返回null
     */
    <T> List<T> getList(String key, TypeReference<List<T>> typeRef);
    
    /**
     * 从缓存获取列表 - 通过Class类型
     * 
     * @param key 缓存键
     * @param elementType 列表元素类型
     * @param <T> 列表元素类型
     * @return 缓存的列表，如果不存在返回null
     */
    <T> List<T> getList(String key, Class<T> elementType);
    
    /**
     * 删除缓存键
     * 
     * @param key 缓存键
     * @return 是否删除成功
     */
    boolean delete(String key);
    
    /**
     * 批量删除缓存键
     * 
     * @param keys 缓存键集合
     * @return 删除成功的键数量
     */
    long delete(Set<String> keys);
    
    /**
     * 根据模式删除缓存键
     * 
     * @param pattern 键模式，支持 * 和 ? 通配符
     * @return 删除的键数量
     */
    long deleteByPattern(String pattern);
    
    /**
     * 检查缓存键是否存在
     * 
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 设置缓存键的过期时间
     * 
     * @param key 缓存键
     * @param ttl 存活时间
     * @return 是否设置成功
     */
    boolean expire(String key, Duration ttl);
    
    /**
     * 获取缓存键的剩余TTL
     * 
     * @param key 缓存键
     * @return 剩余TTL，-1表示永不过期，-2表示键不存在
     */
    long getExpire(String key);
    
    /**
     * 获取所有匹配模式的键
     * 
     * @param pattern 键模式
     * @return 匹配的键集合
     */
    Set<String> keys(String pattern);
    
    /**
     * 获取缓存统计信息
     * 
     * @return 缓存统计信息
     */
    CacheStats getStats();
    
    /**
     * 缓存统计信息
     */
    interface CacheStats {
        long getHitCount();
        long getMissCount();
        double getHitRate();
        long getTotalRequests();
        long getEvictionCount();
    }
}