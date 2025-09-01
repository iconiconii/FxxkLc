#!/usr/bin/env python3
"""
Comprehensive CodeTop Problems SQL Generator
Generates SQL INSERT statements for problems, companies, and problem_companies tables
"""

import json

# Combined data from all 5 pages we collected
codetop_data = {
    "page_1": {
        "count": 1134,
        "list": [
            {"codetop_id": 1681, "time": "2025-08-31T04:15:59.408000Z", "value": 976, "leetcode": {"frontend_question_id": 3, "title": "无重复字符的最长子串", "level": 2, "slug_title": "longest-substring-without-repeating-characters"}, "company": {"company_name": "string"}},
            {"codetop_id": 1538, "time": "2025-08-22T16:00:00Z", "value": 790, "leetcode": {"frontend_question_id": 146, "title": "LRU缓存机制", "level": 2, "slug_title": "lru-cache"}, "company": {"company_name": "string"}},
            {"codetop_id": 1478, "time": "2025-08-25T09:17:09.601000Z", "value": 694, "leetcode": {"frontend_question_id": 206, "title": "反转链表", "level": 1, "slug_title": "reverse-linked-list"}, "company": {"company_name": "string"}},
            {"codetop_id": 1469, "time": "2025-08-29T07:53:35.661000Z", "value": 545, "leetcode": {"frontend_question_id": 215, "title": "数组中的第K个最大元素", "level": 2, "slug_title": "kth-largest-element-in-an-array"}, "company": {"company_name": "string"}},
            {"codetop_id": 1659, "time": "2025-08-27T12:16:44.554000Z", "value": 454, "leetcode": {"frontend_question_id": 25, "title": "K 个一组翻转链表", "level": 3, "slug_title": "reverse-nodes-in-k-group"}, "company": {"company_name": "string"}},
            {"codetop_id": 1669, "time": "2025-08-27T16:00:00Z", "value": 426, "leetcode": {"frontend_question_id": 15, "title": "三数之和", "level": 2, "slug_title": "3sum"}, "company": {"company_name": "string"}},
            {"codetop_id": 1631, "time": "2025-08-30T11:43:25.687000Z", "value": 349, "leetcode": {"frontend_question_id": 53, "title": "最大子数组和", "level": 2, "slug_title": "maximum-subarray"}, "company": {"company_name": "string"}},
            {"codetop_id": 1906, "time": "2025-08-27T13:59:31.816000Z", "value": 316, "leetcode": {"frontend_question_id": 99990004, "title": "手撕快速排序", "level": 2, "slug_title": "sort-an-array"}, "company": {"company_name": "string"}},
            {"codetop_id": 1663, "time": "2025-08-18T12:36:51.059000Z", "value": 304, "leetcode": {"frontend_question_id": 21, "title": "合并两个有序链表", "level": 1, "slug_title": "merge-two-sorted-lists"}, "company": {"company_name": "string"}},
            {"codetop_id": 1679, "time": "2025-08-13T05:56:31.713000Z", "value": 296, "leetcode": {"frontend_question_id": 5, "title": "最长回文子串", "level": 2, "slug_title": "longest-palindromic-substring"}, "company": {"company_name": "string"}},
            {"codetop_id": 1683, "time": "2025-08-26T08:33:00.939000Z", "value": 285, "leetcode": {"frontend_question_id": 1, "title": "两数之和", "level": 1, "slug_title": "two-sum"}, "company": {"company_name": "string"}},
            {"codetop_id": 1582, "time": "2025-08-19T16:00:00Z", "value": 285, "leetcode": {"frontend_question_id": 102, "title": "二叉树的层序遍历", "level": 2, "slug_title": "binary-tree-level-order-traversal"}, "company": {"company_name": "string"}},
            {"codetop_id": 1651, "time": "2025-08-28T16:00:00Z", "value": 281, "leetcode": {"frontend_question_id": 33, "title": "搜索旋转排序数组", "level": 2, "slug_title": "search-in-rotated-sorted-array"}, "company": {"company_name": "string"}},
            {"codetop_id": 1484, "time": "2025-08-16T12:32:49.027000Z", "value": 276, "leetcode": {"frontend_question_id": 200, "title": "岛屿数量", "level": 2, "slug_title": "number-of-islands"}, "company": {"company_name": "string"}},
            {"codetop_id": 1638, "time": "2025-08-18T16:00:00Z", "value": 270, "leetcode": {"frontend_question_id": 46, "title": "全排列", "level": 2, "slug_title": "permutations"}, "company": {"company_name": "string"}},
            {"codetop_id": 1596, "time": "2025-08-26T13:00:42.008000Z", "value": 263, "leetcode": {"frontend_question_id": 88, "title": "合并两个有序数组", "level": 1, "slug_title": "merge-sorted-array"}, "company": {"company_name": "string"}},
            {"codetop_id": 1664, "time": "2025-08-23T13:10:12.333000Z", "value": 262, "leetcode": {"frontend_question_id": 20, "title": "有效的括号", "level": 1, "slug_title": "valid-parentheses"}, "company": {"company_name": "string"}},
            {"codetop_id": 1563, "time": "2025-08-26T12:12:31.749000Z", "value": 252, "leetcode": {"frontend_question_id": 121, "title": "买卖股票的最佳时机", "level": 1, "slug_title": "best-time-to-buy-and-sell-stock"}, "company": {"company_name": "string"}},
            {"codetop_id": 1581, "time": "2025-08-22T02:50:22.790000Z", "value": 247, "leetcode": {"frontend_question_id": 103, "title": "二叉树的锯齿形层次遍历", "level": 2, "slug_title": "binary-tree-zigzag-level-order-traversal"}, "company": {"company_name": "string"}},
            {"codetop_id": 1448, "time": "2025-08-22T10:13:34.686000Z", "value": 246, "leetcode": {"frontend_question_id": 236, "title": "二叉树的最近公共祖先", "level": 2, "slug_title": "lowest-common-ancestor-of-a-binary-tree"}, "company": {"company_name": "string"}}
        ]
    },
    "page_2": {
        "count": 1134,
        "list": [
            {"codetop_id": 1592, "time": "2025-08-18T16:00:00Z", "value": 243, "leetcode": {"frontend_question_id": 92, "title": "反转链表 II", "level": 2, "slug_title": "reverse-linked-list-ii"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1543, "time": "2025-08-25T14:54:48.211000Z", "value": 239, "leetcode": {"frontend_question_id": 141, "title": "环形链表", "level": 1, "slug_title": "linked-list-cycle"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1384, "time": "2025-08-19T13:51:12.804000Z", "value": 230, "leetcode": {"frontend_question_id": 300, "title": "最长上升子序列", "level": 2, "slug_title": "longest-increasing-subsequence"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1630, "time": "2025-08-20T01:27:56.645000Z", "value": 229, "leetcode": {"frontend_question_id": 54, "title": "螺旋矩阵", "level": 2, "slug_title": "spiral-matrix"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1541, "time": "2025-08-29T11:34:23.418000Z", "value": 219, "leetcode": {"frontend_question_id": 143, "title": "重排链表", "level": 2, "slug_title": "reorder-list"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1661, "time": "2025-08-24T16:00:00Z", "value": 218, "leetcode": {"frontend_question_id": 23, "title": "合并K个排序链表", "level": 3, "slug_title": "merge-k-sorted-lists"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1269, "time": "2025-08-20T06:33:32.478000Z", "value": 209, "leetcode": {"frontend_question_id": 415, "title": "字符串相加", "level": 1, "slug_title": "add-strings"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1628, "time": "2025-08-24T16:00:00Z", "value": 200, "leetcode": {"frontend_question_id": 56, "title": "合并区间", "level": 2, "slug_title": "merge-intervals"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1524, "time": "2025-08-13T08:16:10.586000Z", "value": 192, "leetcode": {"frontend_question_id": 160, "title": "相交链表", "level": 1, "slug_title": "intersection-of-two-linked-lists"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1642, "time": "2025-08-07T08:49:17.173000Z", "value": 181, "leetcode": {"frontend_question_id": 42, "title": "接雨水", "level": 3, "slug_title": "trapping-rain-water"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1612, "time": "2025-08-21T11:43:13.162000Z", "value": 178, "leetcode": {"frontend_question_id": 72, "title": "编辑距离", "level": 3, "slug_title": "edit-distance"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1560, "time": "2025-08-28T07:14:55.055000Z", "value": 171, "leetcode": {"frontend_question_id": 124, "title": "二叉树中的最大路径和", "level": 3, "slug_title": "binary-tree-maximum-path-sum"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 485, "time": "2025-08-21T09:47:40.330000Z", "value": 169, "leetcode": {"frontend_question_id": 1143, "title": "最长公共子序列", "level": 2, "slug_title": "longest-common-subsequence"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1591, "time": "2025-08-19T06:47:17.839000Z", "value": 167, "leetcode": {"frontend_question_id": 93, "title": "复原IP地址", "level": 2, "slug_title": "restore-ip-addresses"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1602, "time": "2025-08-25T06:03:45.320000Z", "value": 164, "leetcode": {"frontend_question_id": 82, "title": "删除排序链表中的重复元素 II", "level": 2, "slug_title": "remove-duplicates-from-sorted-list-ii"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1542, "time": "2025-08-16T12:33:56.912000Z", "value": 164, "leetcode": {"frontend_question_id": 142, "title": "环形链表 II", "level": 2, "slug_title": "linked-list-cycle-ii"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1665, "time": "2025-08-20T16:00:00Z", "value": 163, "leetcode": {"frontend_question_id": 19, "title": "删除链表的倒数第N个节点", "level": 2, "slug_title": "remove-nth-node-from-end-of-list"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1680, "time": "2025-08-27T16:00:00Z", "value": 152, "leetcode": {"frontend_question_id": 4, "title": "寻找两个正序数组的中位数", "level": 3, "slug_title": "median-of-two-sorted-arrays"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1485, "time": "2025-07-24T15:00:48.726000Z", "value": 145, "leetcode": {"frontend_question_id": 199, "title": "二叉树的右视图", "level": 2, "slug_title": "binary-tree-right-side-view"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1519, "time": "2025-08-28T10:51:59.738000Z", "value": 141, "leetcode": {"frontend_question_id": 165, "title": "比较版本号", "level": 2, "slug_title": "compare-version-numbers"}, "company": {"company_name": "leetcode"}}
        ]
    },
    "page_3": {
        "count": 1134,
        "list": [
            {"codetop_id": 1590, "time": "2025-05-26T10:12:30.261000Z", "value": 139, "leetcode": {"frontend_question_id": 94, "title": "二叉树的中序遍历", "level": 1, "slug_title": "binary-tree-inorder-traversal"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 925, "time": "2025-07-28T16:00:00Z", "value": 136, "leetcode": {"frontend_question_id": 704, "title": "二分查找", "level": 1, "slug_title": "binary-search"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1662, "time": "2025-08-27T16:00:00Z", "value": 134, "leetcode": {"frontend_question_id": 22, "title": "括号生成", "level": 2, "slug_title": "generate-parentheses"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1452, "time": "2025-08-10T15:44:56.464000Z", "value": 134, "leetcode": {"frontend_question_id": 232, "title": "用栈实现队列", "level": 1, "slug_title": "implement-queue-using-stacks"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1445, "time": "2025-08-28T07:11:37.359000Z", "value": 132, "leetcode": {"frontend_question_id": 239, "title": "滑动窗口最大值", "level": 3, "slug_title": "sliding-window-maximum"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1536, "time": "2025-08-12T09:07:30.462000Z", "value": 132, "leetcode": {"frontend_question_id": 148, "title": "排序链表", "level": 2, "slug_title": "sort-list"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1653, "time": "2025-08-18T16:00:00Z", "value": 130, "leetcode": {"frontend_question_id": 31, "title": "下一个排列", "level": 2, "slug_title": "next-permutation"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1615, "time": "2025-08-14T12:40:33.430000Z", "value": 128, "leetcode": {"frontend_question_id": 69, "title": "x 的平方根", "level": 1, "slug_title": "sqrtx"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1652, "time": "2025-08-25T09:34:39.486000Z", "value": 125, "leetcode": {"frontend_question_id": 32, "title": "最长有效括号", "level": 3, "slug_title": "longest-valid-parentheses"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1676, "time": "2025-08-05T12:58:19.303000Z", "value": 124, "leetcode": {"frontend_question_id": 8, "title": "字符串转换整数 (atoi)", "level": 2, "slug_title": "string-to-integer-atoi"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1682, "time": "2025-08-01T02:09:46.231000Z", "value": 124, "leetcode": {"frontend_question_id": 2, "title": "两数相加", "level": 2, "slug_title": "add-two-numbers"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1614, "time": "2025-08-06T12:13:08.757000Z", "value": 122, "leetcode": {"frontend_question_id": 70, "title": "爬楼梯", "level": 1, "slug_title": "climbing-stairs"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1362, "time": "2025-08-26T12:12:31.749000Z", "value": 120, "leetcode": {"frontend_question_id": 322, "title": "零钱兑换", "level": 2, "slug_title": "coin-change"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1641, "time": "2025-08-13T16:00:00Z", "value": 115, "leetcode": {"frontend_question_id": 43, "title": "字符串相乘", "level": 2, "slug_title": "multiply-strings"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1608, "time": "2025-08-20T16:00:00Z", "value": 114, "leetcode": {"frontend_question_id": 76, "title": "最小覆盖子串", "level": 3, "slug_title": "minimum-window-substring"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1643, "time": "2025-05-19T16:00:00Z", "value": 108, "leetcode": {"frontend_question_id": 41, "title": "缺失的第一个正数", "level": 3, "slug_title": "first-missing-positive"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1579, "time": "2025-06-03T16:00:00Z", "value": 103, "leetcode": {"frontend_question_id": 105, "title": "从前序与中序遍历序列构造二叉树", "level": 2, "slug_title": "construct-binary-tree-from-preorder-and-inorder-traversal"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 122, "time": "2025-03-18T09:56:51.560000Z", "value": 102, "leetcode": {"frontend_question_id": 22, "title": "链表中倒数第k个节点", "level": 1, "slug_title": "lian-biao-zhong-dao-shu-di-kge-jie-dian-lcof"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1533, "time": "2025-06-10T02:53:17.237000Z", "value": 100, "leetcode": {"frontend_question_id": 151, "title": "翻转字符串里的单词", "level": 2, "slug_title": "reverse-words-in-a-string"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1606, "time": "2025-08-14T07:09:45.415000Z", "value": 99, "leetcode": {"frontend_question_id": 78, "title": "子集", "level": 2, "slug_title": "subsets"}, "company": {"company_name": "leetcode"}}
        ]
    },
    "page_4": {
        "count": 1134,
        "list": [
            {"codetop_id": 1555, "time": "2025-08-12T05:22:01.214000Z", "value": 96, "leetcode": {"frontend_question_id": 129, "title": "求根到叶子节点数字之和", "level": 2, "slug_title": "sum-root-to-leaf-numbers"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1529, "time": "2025-06-09T16:00:00Z", "value": 94, "leetcode": {"frontend_question_id": 155, "title": "最小栈", "level": 1, "slug_title": "min-stack"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1650, "time": "2025-07-31T12:51:27.976000Z", "value": 91, "leetcode": {"frontend_question_id": 34, "title": "在排序数组中查找元素的第一个和最后一个位置", "level": 2, "slug_title": "find-first-and-last-position-of-element-in-sorted-array"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1290, "time": "2025-08-10T16:00:00Z", "value": 90, "leetcode": {"frontend_question_id": 394, "title": "字符串解码", "level": 2, "slug_title": "decode-string"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1583, "time": "2025-06-03T16:00:00Z", "value": 90, "leetcode": {"frontend_question_id": 101, "title": "对称二叉树", "level": 1, "slug_title": "symmetric-tree"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 821, "time": "2025-08-19T11:43:51.596000Z", "value": 86, "leetcode": {"frontend_question_id": 470, "title": "用 Rand7() 实现 Rand10()", "level": 2, "slug_title": "implement-rand10-using-rand7"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1645, "time": "2025-06-19T10:36:23.762000Z", "value": 86, "leetcode": {"frontend_question_id": 39, "title": "组合总和", "level": 2, "slug_title": "combination-sum"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1580, "time": "2025-04-28T11:59:36.137000Z", "value": 86, "leetcode": {"frontend_question_id": 104, "title": "二叉树的最大深度", "level": 1, "slug_title": "maximum-depth-of-binary-tree"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1620, "time": "2025-08-21T13:39:12.457000Z", "value": 85, "leetcode": {"frontend_question_id": 64, "title": "最小路径和", "level": 2, "slug_title": "minimum-path-sum"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1540, "time": "2025-04-17T16:00:00Z", "value": 84, "leetcode": {"frontend_question_id": 144, "title": "二叉树的前序遍历", "level": 1, "slug_title": "binary-tree-preorder-traversal"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1574, "time": "2025-08-14T07:12:57.093000Z", "value": 83, "leetcode": {"frontend_question_id": 110, "title": "平衡二叉树", "level": 1, "slug_title": "balanced-binary-tree"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1562, "time": "2025-08-26T12:12:31.749000Z", "value": 81, "leetcode": {"frontend_question_id": 122, "title": "买卖股票的最佳时机 II", "level": 1, "slug_title": "best-time-to-buy-and-sell-stock-ii"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1636, "time": "2025-08-05T09:47:49.343000Z", "value": 81, "leetcode": {"frontend_question_id": 48, "title": "旋转图像", "level": 2, "slug_title": "rotate-image"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1556, "time": "2025-07-21T13:42:38.220000Z", "value": 81, "leetcode": {"frontend_question_id": 128, "title": "最长连续序列", "level": 2, "slug_title": "longest-consecutive-sequence"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1463, "time": "2025-08-28T07:11:37.359000Z", "value": 80, "leetcode": {"frontend_question_id": 221, "title": "最大正方形", "level": 2, "slug_title": "maximal-square"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1450, "time": "2025-08-26T15:55:21.788000Z", "value": 80, "leetcode": {"frontend_question_id": 234, "title": "回文链表", "level": 1, "slug_title": "palindrome-linked-list"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1444, "time": "2025-07-23T12:44:23.816000Z", "value": 79, "leetcode": {"frontend_question_id": 240, "title": "搜索二维矩阵 II", "level": 2, "slug_title": "search-a-2d-matrix-ii"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1586, "time": "2025-07-17T12:11:50.527000Z", "value": 79, "leetcode": {"frontend_question_id": 98, "title": "验证二叉搜索树", "level": 2, "slug_title": "validate-binary-search-tree"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1156, "time": "2025-06-11T16:00:00Z", "value": 79, "leetcode": {"frontend_question_id": 543, "title": "二叉树的直径", "level": 1, "slug_title": "diameter-of-binary-tree"}, "company": {"company_name": "leetcode"}},
            {"codetop_id": 1011, "time": "2025-07-30T16:00:00Z", "value": 78, "leetcode": {"frontend_question_id": 695, "title": "岛屿的最大面积", "level": 2, "slug_title": "max-area-of-island"}, "company": {"company_name": "leetcode"}}
        ]
    },
    "page_5": {
        "count": 1134,
        "list": [
            {"codetop_id": 1522, "time": "2025-07-24T15:00:48.726000Z", "value": 76, "leetcode": {"frontend_question_id": 162, "title": "寻找峰值", "level": 2, "slug_title": "find-peak-element"}, "company": {"company_name": "null"}},
            {"codetop_id": 1670, "time": "2025-06-16T16:00:00Z", "value": 76, "leetcode": {"frontend_question_id": 14, "title": "最长公共前缀", "level": 1, "slug_title": "longest-common-prefix"}, "company": {"company_name": "null"}},
            {"codetop_id": 1044, "time": "2025-08-07T12:38:42.614000Z", "value": 74, "leetcode": {"frontend_question_id": 662, "title": "二叉树最大宽度", "level": 2, "slug_title": "maximum-width-of-binary-tree"}, "company": {"company_name": "null"}},
            {"codetop_id": 1571, "time": "2025-08-03T16:00:00Z", "value": 74, "leetcode": {"frontend_question_id": 113, "title": "路径总和 II", "level": 2, "slug_title": "path-sum-ii"}, "company": {"company_name": "null"}},
            {"codetop_id": 1505, "time": "2025-07-23T16:00:00Z", "value": 74, "leetcode": {"frontend_question_id": 179, "title": "最大数", "level": 2, "slug_title": "largest-number"}, "company": {"company_name": "null"}},
            {"codetop_id": 1622, "time": "2025-08-26T13:47:47.289000Z", "value": 72, "leetcode": {"frontend_question_id": 62, "title": "不同路径", "level": 2, "slug_title": "unique-paths"}, "company": {"company_name": "null"}},
            {"codetop_id": 1486, "time": "2025-07-02T07:16:28.572000Z", "value": 70, "leetcode": {"frontend_question_id": 198, "title": "打家劫舍", "level": 2, "slug_title": "house-robber"}, "company": {"company_name": "null"}},
            {"codetop_id": 1532, "time": "2025-04-23T06:38:18.200000Z", "value": 70, "leetcode": {"frontend_question_id": 152, "title": "乘积最大子数组", "level": 2, "slug_title": "maximum-product-subarray"}, "company": {"company_name": "null"}},
            {"codetop_id": 1572, "time": "2025-04-07T08:22:19.264000Z", "value": 68, "leetcode": {"frontend_question_id": 112, "title": "路径总和", "level": 1, "slug_title": "path-sum"}, "company": {"company_name": "null"}},
            {"codetop_id": 1142, "time": "2025-07-08T11:24:38.176000Z", "value": 67, "leetcode": {"frontend_question_id": 560, "title": "和为K的子数组", "level": 2, "slug_title": "subarray-sum-equals-k"}, "company": {"company_name": "null"}},
            {"codetop_id": 1515, "time": "2025-08-01T16:00:00Z", "value": 66, "leetcode": {"frontend_question_id": 169, "title": "多数元素", "level": 1, "slug_title": "majority-element"}, "company": {"company_name": "null"}},
            {"codetop_id": 1457, "time": "2025-05-24T07:28:06.406000Z", "value": 66, "leetcode": {"frontend_question_id": 227, "title": "基本计算器 II", "level": 2, "slug_title": "basic-calculator-ii"}, "company": {"company_name": "null"}},
            {"codetop_id": 1458, "time": "2025-06-29T13:42:41.724000Z", "value": 65, "leetcode": {"frontend_question_id": 226, "title": "翻转二叉树", "level": 1, "slug_title": "invert-binary-tree"}, "company": {"company_name": "null"}},
            {"codetop_id": 1475, "time": "2025-06-15T16:00:00Z", "value": 65, "leetcode": {"frontend_question_id": 209, "title": "长度最小的子数组", "level": 2, "slug_title": "minimum-size-subarray-sum"}, "company": {"company_name": "null"}},
            {"codetop_id": 1545, "time": "2025-08-18T04:18:33.372000Z", "value": 64, "leetcode": {"frontend_question_id": 139, "title": "单词拆分", "level": 2, "slug_title": "word-break"}, "company": {"company_name": "null"}},
            {"codetop_id": 1601, "time": "2025-07-31T16:00:00Z", "value": 64, "leetcode": {"frontend_question_id": 83, "title": "删除排序链表中的重复元素", "level": 1, "slug_title": "remove-duplicates-from-sorted-list"}, "company": {"company_name": "null"}},
            {"codetop_id": 999, "time": "2025-05-26T16:00:00Z", "value": 64, "leetcode": {"frontend_question_id": 718, "title": "最长重复子数组", "level": 2, "slug_title": "maximum-length-of-repeated-subarray"}, "company": {"company_name": "null"}},
            {"codetop_id": 1660, "time": "2025-07-17T07:20:22.441000Z", "value": 62, "leetcode": {"frontend_question_id": 24, "title": "两两交换链表中的节点", "level": 2, "slug_title": "swap-nodes-in-pairs"}, "company": {"company_name": "null"}},
            {"codetop_id": 1401, "time": "2025-07-14T08:39:22.025000Z", "value": 60, "leetcode": {"frontend_question_id": 283, "title": "移动零", "level": 1, "slug_title": "move-zeroes"}, "company": {"company_name": "null"}},
            {"codetop_id": 1908, "time": "2025-08-04T06:02:24.952000Z", "value": 59, "leetcode": {"frontend_question_id": 99990006, "title": "手撕堆排序", "level": 2, "slug_title": "sort-an-array"}, "company": {"company_name": "null"}}
        ]
    }
}

def difficulty_level_to_name(level):
    """Convert difficulty level to enum name"""
    difficulty_map = {1: 'EASY', 2: 'MEDIUM', 3: 'HARD'}
    return difficulty_map.get(level, 'MEDIUM')

def generate_comprehensive_sql():
    """Generate comprehensive SQL with all collected problems"""
    sql_statements = []
    
    # SQL header
    sql_statements.append("-- CodeTop Problems Data - Comprehensive Dataset (100 High-Frequency Problems)")
    sql_statements.append("-- Generated from CodeTop API: https://codetop.cc/api/questions")
    sql_statements.append("-- Data includes problems from pages 1-5 with frequencies ranging from 976 to 59")
    sql_statements.append("-- Generated on: 2025-08-31")
    sql_statements.append("")
    
    # Disable foreign key checks and set character set
    sql_statements.append("SET FOREIGN_KEY_CHECKS = 0;")
    sql_statements.append("SET NAMES utf8mb4;")
    sql_statements.append("")
    
    # Clear existing data
    sql_statements.append("-- Clear existing data")
    sql_statements.append("DELETE FROM problem_companies WHERE id > 0;")
    sql_statements.append("DELETE FROM companies WHERE id > 0;")  
    sql_statements.append("DELETE FROM problems WHERE id > 0;")
    sql_statements.append("")
    sql_statements.append("-- Reset auto increment")
    sql_statements.append("ALTER TABLE problems AUTO_INCREMENT = 1;")
    sql_statements.append("ALTER TABLE companies AUTO_INCREMENT = 1;")
    sql_statements.append("ALTER TABLE problem_companies AUTO_INCREMENT = 1;")
    sql_statements.append("")
    
    # Collect all unique companies
    companies = set()
    all_problems = []
    
    # Process all pages
    for page_key, page_data in codetop_data.items():
        for problem in page_data["list"]:
            company_name = problem["company"]["company_name"]
            if company_name and company_name != "null":
                companies.add(company_name)
            all_problems.append(problem)
    
    # Insert companies
    sql_statements.append("-- Insert companies")
    company_id_map = {}
    for i, company in enumerate(sorted(companies), 1):
        if company in ['string', 'leetcode']:  # Handle generic company names
            if company == 'string':
                display_name = 'Generic Tech Company'
                description = 'Various technology companies'
            else:
                display_name = 'LeetCode Platform'
                description = 'LeetCode official problems'
        else:
            display_name = company
            description = f'Problems from {company}'
        
        sql_statements.append(
            f"INSERT INTO companies (id, name, description, created_at, updated_at) VALUES "
            f"({i}, '{display_name}', '{description}', NOW(), NOW());"
        )
        company_id_map[company] = i
    
    sql_statements.append("")
    
    # Insert problems
    sql_statements.append("-- Insert problems (sorted by frequency - highest first)")
    
    # Sort problems by frequency (value) in descending order
    sorted_problems = sorted(all_problems, key=lambda x: x["value"], reverse=True)
    
    for i, problem in enumerate(sorted_problems, 1):
        leetcode = problem["leetcode"]
        title = leetcode["title"].replace("'", "\\'")
        difficulty = difficulty_level_to_name(leetcode["level"])
        slug_title = leetcode["slug_title"]
        problem_url = f"https://leetcode.cn/problems/{slug_title}"
        leetcode_id = str(leetcode["frontend_question_id"])
        frequency = problem["value"]
        
        # Create tags array with frequency and difficulty info
        tags = json.dumps([
            {"name": "frequency", "value": frequency},
            {"name": "difficulty", "value": difficulty},
            {"name": "source", "value": "codetop"}
        ], ensure_ascii=False)
        
        sql_statements.append(
            f"INSERT INTO problems (id, title, difficulty, problem_url, tags, leetcode_id, is_premium, deleted, created_at, updated_at) VALUES "
            f"({i}, '{title}', '{difficulty}', '{problem_url}', '{tags}', '{leetcode_id}', FALSE, 0, NOW(), NOW());"
        )
    
    sql_statements.append("")
    
    # Insert problem-company associations
    sql_statements.append("-- Insert problem-company associations")
    association_id = 1
    
    for i, problem in enumerate(sorted_problems, 1):
        company_name = problem["company"]["company_name"]
        if company_name and company_name != "null" and company_name in company_id_map:
            company_id = company_id_map[company_name]
            frequency = problem["value"]
            
            sql_statements.append(
                f"INSERT INTO problem_companies (id, problem_id, company_id, frequency_score, created_at, updated_at) VALUES "
                f"({association_id}, {i}, {company_id}, {frequency}, NOW(), NOW());"
            )
            association_id += 1
    
    sql_statements.append("")
    
    # Re-enable foreign key checks
    sql_statements.append("SET FOREIGN_KEY_CHECKS = 1;")
    sql_statements.append("")
    
    # Add summary comment
    total_problems = len(all_problems)
    total_companies = len(companies)
    max_frequency = max(p["value"] for p in all_problems)
    min_frequency = min(p["value"] for p in all_problems)
    
    sql_statements.append(f"-- Summary: {total_problems} problems, {total_companies} companies")
    sql_statements.append(f"-- Frequency range: {max_frequency} (highest) to {min_frequency} (lowest)")
    sql_statements.append(f"-- Problems sorted by frequency for optimal FSRS algorithm training")
    
    return "\n".join(sql_statements)

if __name__ == "__main__":
    print("Generating comprehensive CodeTop problems SQL...")
    sql_content = generate_comprehensive_sql()
    
    # Save to file
    with open("codetop_comprehensive_problems_100.sql", "w", encoding="utf-8") as f:
        f.write(sql_content)
    
    print("✅ Generated codetop_comprehensive_problems_100.sql")
    print("📊 Dataset includes 100 high-frequency problems")
    print("🎯 Problems sorted by frequency (976 to 59)")
    print("🏢 Includes company associations and metadata")
    print("🚀 Ready for production deployment!")