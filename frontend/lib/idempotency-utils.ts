/**
 * 幂等性工具函数
 */

/**
 * 生成UUID用于幂等性requestId
 */
export function generateRequestId(): string {
  // 使用crypto.randomUUID()如果可用，否则回退到自定义实现
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  
  // 回退实现：基于时间戳和随机数
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

/**
 * 为需要幂等性的请求添加requestId
 */
export function withIdempotency<T extends Record<string, any>>(request: T): T & { requestId: string } {
  return {
    ...request,
    requestId: generateRequestId()
  }
}