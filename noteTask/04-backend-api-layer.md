# 04-后端API层 (Backend API Layer)

## 任务目标
实现RESTful API接口，提供完整的笔记管理HTTP服务，集成认证授权和请求验证。

## 前置条件
- 服务层实现完成并测试通过
- ProblemNoteService可用
- JWT认证系统正常工作

## 任务清单

### 控制器实现
- [x] 创建ProblemNoteController类
- [x] 实现笔记CRUD的REST endpoints
- [x] 添加请求参数验证
- [x] 实现分页和排序支持
- [x] 添加API文档注解(Swagger)

### 认证和授权
- [x] 集成JWT认证验证
- [x] 实现用户权限检查
- [x] 添加资源所有权验证
- [x] 实现访问控制
- [x] 添加操作审计日志

### 请求处理优化
- [x] 实现请求幂等性支持
- [x] 添加请求频率限制
- [x] 实现请求参数清理
- [x] 添加响应数据压缩
- [x] 实现错误处理统一化

### API安全增强
- [x] 实现输入验证和清理
- [x] 添加XSS防护
- [x] 实现CSRF保护
- [x] 添加SQL注入防护
- [x] 实现敏感数据脱敏

## 实施详情

### 1. 控制器主体结构

```java
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Problem Notes", description = "算法题笔记管理API")
@SecurityRequirement(name = "Bearer Authentication")
public class ProblemNoteController {
    
    private final ProblemNoteService problemNoteService;
    
    // API endpoints implementation
}
```

### 2. 核心API端点实现

#### 创建/更新笔记
```java
@PostMapping
@Operation(summary = "创建或更新笔记", description = "为指定题目创建或更新用户笔记")
@SimpleIdempotent(key = "#userId + '-' + #request.problemId")
public ResponseEntity<ProblemNoteDTO> createOrUpdateNote(
        @CurrentUserId Long userId,
        @Valid @RequestBody CreateNoteRequestDTO request) {
    
    log.info("Creating/updating note: userId={}, problemId={}", userId, request.getProblemId());
    
    try {
        ProblemNoteDTO result = problemNoteService.createOrUpdateNote(userId, request);
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        log.error("Failed to create/update note: userId={}, problemId={}", 
                 userId, request.getProblemId(), e);
        throw e;
    }
}
```

#### 获取用户笔记
```java
@GetMapping("/problem/{problemId}")
@Operation(summary = "获取用户笔记", description = "获取当前用户对指定题目的笔记")
public ResponseEntity<ProblemNoteDTO> getUserNote(
        @CurrentUserId Long userId,
        @PathVariable @Parameter(description = "题目ID") Long problemId) {
    
    return problemNoteService.getUserNote(userId, problemId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
}
```

#### 获取公开笔记列表
```java
@GetMapping("/public/{problemId}")
@Operation(summary = "获取公开笔记", description = "获取指定题目的公开笔记列表")
public ResponseEntity<Page<PublicNoteViewDTO>> getPublicNotes(
        @PathVariable @Parameter(description = "题目ID") Long problemId,
        @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
        @RequestParam(defaultValue = "20") @Parameter(description = "每页大小") int size,
        @RequestParam(defaultValue = "helpful_votes,desc") @Parameter(description = "排序方式") String sort) {
    
    // 验证和限制页面大小
    size = Math.min(size, 100);
    
    Pageable pageable = PageRequest.of(page, size, parseSort(sort));
    Page<PublicNoteViewDTO> result = problemNoteService.getPublicNotes(problemId, pageable);
    
    return ResponseEntity.ok(result);
}
```

#### 笔记投票
```java
@PutMapping("/{noteId}/vote")
@Operation(summary = "笔记投票", description = "对笔记进行有用投票")
@SimpleIdempotent(key = "#userId + '-vote-' + #noteId")
public ResponseEntity<Void> voteNote(
        @CurrentUserId Long userId,
        @PathVariable @Parameter(description = "笔记ID") Long noteId,
        @RequestParam @Parameter(description = "是否有用") boolean helpful) {
    
    problemNoteService.voteHelpful(userId, noteId, helpful);
    return ResponseEntity.ok().build();
}
```

#### 删除笔记
```java
@DeleteMapping("/{noteId}")
@Operation(summary = "删除笔记", description = "删除用户的笔记")
public ResponseEntity<Void> deleteNote(
        @CurrentUserId Long userId,
        @PathVariable @Parameter(description = "笔记ID") Long noteId) {
    
    // 验证笔记所有权
    validateNoteOwnership(userId, noteId);
    
    problemNoteService.deleteNote(userId, noteId);
    return ResponseEntity.noContent().build();
}
```

### 3. 权限验证实现

```java
@Component
@RequiredArgsConstructor
public class NotePermissionValidator {
    
    private final ProblemNoteService problemNoteService;
    
    public void validateNoteOwnership(Long userId, Long noteId) {
        if (!problemNoteService.isNoteOwner(userId, noteId)) {
            throw new UnauthorizedException("User does not own this note");
        }
    }
    
    public void validateNoteAccess(Long userId, Long noteId) {
        if (!problemNoteService.canAccessNote(userId, noteId)) {
            throw new UnauthorizedException("User cannot access this note");
        }
    }
}
```

### 4. 请求验证DTO

```java
@Data
@Builder
public class CreateNoteRequestDTO {
    
    @NotNull(message = "题目ID不能为空")
    @Positive(message = "题目ID必须为正数")
    private Long problemId;
    
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;
    
    @Size(max = 50000, message = "内容长度不能超过50000字符")
    private String content;
    
    @Size(max = 5000, message = "解题思路长度不能超过5000字符")
    private String solutionApproach;
    
    @Size(max = 100, message = "时间复杂度描述不能超过100字符")
    private String timeComplexity;
    
    @Size(max = 100, message = "空间复杂度描述不能超过100字符")
    private String spaceComplexity;
    
    @Size(max = 10, message = "标签数量不能超过10个")
    private List<@Size(max = 50) String> tags;
    
    private Boolean isPublic = false;
}
```

### 5. 全局异常处理

```java
@RestControllerAdvice
public class NoteControllerAdvice extends GlobalExceptionHandler {
    
    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoteNotFound(NoteNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .code("NOTE_NOT_FOUND")
                        .message("笔记不存在")
                        .build());
    }
    
    @ExceptionHandler(NoteAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(NoteAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .code("ACCESS_DENIED")
                        .message("无权访问该笔记")
                        .build());
    }
}
```

## 测试验证

### API单元测试
- [x] 控制器方法单元测试
- [x] 请求参数验证测试
- [x] 权限验证测试
- [x] 异常处理测试

### API集成测试
- [x] 完整请求流程测试
- [x] 认证授权集成测试
- [x] 数据库操作集成测试
- [x] 并发请求测试

### API契约测试
- [x] Swagger文档生成测试
- [x] API响应格式验证
- [x] 错误响应格式测试
- [x] API版本兼容性测试

### 性能测试
- [x] 单个API响应时间测试
- [x] 高并发负载测试
- [x] 大数据量分页测试
- [x] API限流功能测试

## 完成标准
- [x] 所有API端点实现完成
- [x] 认证授权正确集成
- [x] 请求验证规则完整
- [x] Swagger文档生成正确
- [x] 单元测试覆盖率>90%
- [x] 集成测试全部通过
- [x] API性能满足要求

## 相关文件
- `src/main/java/com/codetop/controller/ProblemNoteController.java` (新建)
- `src/main/java/com/codetop/dto/CreateNoteRequestDTO.java` (新建)
- `src/main/java/com/codetop/dto/PublicNoteViewDTO.java` (新建)
- `src/main/java/com/codetop/exception/NoteNotFoundException.java` (新建)
- `src/main/java/com/codetop/validation/NotePermissionValidator.java` (新建)
- `src/test/java/com/codetop/controller/ProblemNoteControllerTest.java` (新建)

## 注意事项
- 确保所有API都有适当的认证保护
- 实现合适的输入验证和清理
- 注意API的向后兼容性
- 监控API的性能和错误率
- 实现合适的日志记录策略