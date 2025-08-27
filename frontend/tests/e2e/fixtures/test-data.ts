export const testUser = {
  email: '2441933762@qq.com',
  password: 'password123_',
};

export const testNote = {
  title: '测试笔记标题 - E2E Test Note',
  content: `# 解题思路

这是一个用于E2E测试的笔记内容。

## 算法分析
- 时间复杂度: O(n)
- 空间复杂度: O(1)

## 代码实现
\`\`\`java
public class Solution {
    public int example(int[] nums) {
        // 测试代码
        return nums.length;
    }
}
\`\`\`

## 总结
这是测试笔记的总结部分。`,
  solutionApproach: '使用双指针算法解决问题',
  timeComplexity: 'O(n)',
  spaceComplexity: 'O(1)',
  programmingLanguage: 'JAVA',
  difficulty: 'MEDIUM',
  noteType: 'SOLUTION',
  tags: ['测试', '算法', 'E2E'],
  isPublic: false
};

export const publicTestNote = {
  ...testNote,
  title: '公开测试笔记 - Public E2E Test Note',
  isPublic: true
};

export const testProblem = {
  id: 1,
  title: '两数之和',
  description: '给定一个整数数组和一个目标值，找出数组中和为目标值的两个数。',
  difficulty: 'EASY'
};