// MongoDB索引创建脚本
// MongoDB数据库笔记内容集合索引优化

// 连接到MongoDB数据库
use('codetop_notes');

print('=== MongoDB索引优化脚本 ===');
print('开始优化 problem_note_contents 集合索引...\n');

// 1. 为problemNoteId创建唯一索引（核心关联字段）
print('1. 创建 problemNoteId 唯一索引...');
try {
    db.problem_note_contents.createIndex(
        { "problemNoteId": 1 },
        { 
            name: "idx_problem_note_id_unique",
            unique: true,
            background: true 
        }
    );
    print('✅ problemNoteId 唯一索引创建成功');
} catch (e) {
    print('⚠️  problemNoteId 索引可能已存在: ' + e.message);
}

// 2. 为内容搜索创建全文索引
print('\n2. 创建全文搜索索引...');
try {
    db.problem_note_contents.createIndex(
        { 
            "content": "text", 
            "solutionApproach": "text",
            "tips": "text",
            "pitfalls": "text"
        },
        { 
            name: "idx_content_fulltext_search",
            background: true,
            weights: {
                "content": 10,
                "solutionApproach": 8,
                "tips": 5,
                "pitfalls": 5
            },
            default_language: "none"  // 支持中英文混合
        }
    );
    print('✅ 全文搜索索引创建成功');
} catch (e) {
    print('⚠️  全文搜索索引可能已存在: ' + e.message);
}

// 3. 为标签查询创建索引
print('\n3. 创建标签查询索引...');
try {
    db.problem_note_contents.createIndex(
        { "tags": 1 },
        { 
            name: "idx_tags",
            background: true 
        }
    );
    print('✅ 标签索引创建成功');
} catch (e) {
    print('⚠️  标签索引可能已存在: ' + e.message);
}

// 4. 为最后修改时间创建排序索引
print('\n4. 创建时间排序索引...');
try {
    db.problem_note_contents.createIndex(
        { "lastModified": -1 },
        { 
            name: "idx_last_modified_desc",
            background: true 
        }
    );
    print('✅ 时间排序索引创建成功');
} catch (e) {
    print('⚠️  时间排序索引可能已存在: ' + e.message);
}

// 5. 为复合查询创建复合索引
print('\n5. 创建复合查询索引...');
try {
    // 标签 + 时间复合索引，用于标签筛选后的时间排序
    db.problem_note_contents.createIndex(
        { "tags": 1, "lastModified": -1 },
        { 
            name: "idx_tags_time_compound",
            background: true 
        }
    );
    print('✅ 标签-时间复合索引创建成功');
} catch (e) {
    print('⚠️  标签-时间复合索引可能已存在: ' + e.message);
}

// 6. 为版本号创建索引（文档版本控制）
print('\n6. 创建版本号索引...');
try {
    db.problem_note_contents.createIndex(
        { "version": 1 },
        { 
            name: "idx_version",
            background: true 
        }
    );
    print('✅ 版本号索引创建成功');
} catch (e) {
    print('⚠️  版本号索引可能已存在: ' + e.message);
}

// 7. 为字数统计范围查询创建索引
print('\n7. 创建字数统计索引...');
try {
    db.problem_note_contents.createIndex(
        { "wordCount": 1 },
        { 
            name: "idx_word_count",
            background: true 
        }
    );
    print('✅ 字数统计索引创建成功');
} catch (e) {
    print('⚠️  字数统计索引可能已存在: ' + e.message);
}

// 8. 显示所有索引信息
print('\n=== 索引创建完成，当前所有索引列表 ===');
const indexes = db.problem_note_contents.getIndexes();
indexes.forEach((index, i) => {
    print(`${i + 1}. 索引名: ${index.name}`);
    print(`   字段: ${JSON.stringify(index.key)}`);
    print(`   唯一: ${index.unique || false}`);
    print(`   后台: ${index.background || false}`);
    if (index.textIndexVersion) {
        print(`   全文搜索版本: ${index.textIndexVersion}`);
    }
    print('');
});

// 9. 显示集合统计信息
print('=== 集合统计信息 ===');
const stats = db.problem_note_contents.stats();
print(`文档总数: ${stats.count}`);
print(`平均文档大小: ${Math.round(stats.avgObjSize)} bytes`);
print(`总存储大小: ${Math.round(stats.storageSize / 1024 / 1024 * 100) / 100} MB`);
print(`索引总大小: ${Math.round(stats.totalIndexSize / 1024 / 1024 * 100) / 100} MB`);

// 10. 验证关键索引的效果
print('\n=== 索引效果验证 ===');

// 验证problemNoteId查询
print('验证 problemNoteId 查询效果:');
try {
    const explainResult = db.problem_note_contents.find({"problemNoteId": 123}).explain("executionStats");
    print(`执行时间: ${explainResult.executionStats.executionTimeMillis}ms`);
    print(`使用索引: ${explainResult.executionStats.winningPlan.inputStage?.indexName || '无'}`);
} catch (e) {
    print('验证查询出错: ' + e.message);
}

print('\n✅ MongoDB索引优化完成！');
print('建议定期监控索引使用情况，清理未使用的索引以节省存储空间。');