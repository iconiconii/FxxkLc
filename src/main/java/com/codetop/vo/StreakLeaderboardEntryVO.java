package com.codetop.vo;

import com.codetop.dto.StreakLeaderboardEntryDTO;
import lombok.Data;

/**
 * Streak leaderboard entry VO with computed badge logic.
 * This class handles badge assignment without modifying the original DTO,
 * making it safe for Redis caching.
 * 
 * @author CodeTop Team
 */
@Data
public class StreakLeaderboardEntryVO {
    private Long rank;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Integer currentStreak;
    private Integer longestStreak;
    private Long totalActiveDays;
    private String badge;
    
    /**
     * Create VO from DTO with computed badge.
     */
    public static StreakLeaderboardEntryVO fromDTO(StreakLeaderboardEntryDTO dto) {
        StreakLeaderboardEntryVO vo = new StreakLeaderboardEntryVO();
        vo.rank = dto.getRank();
        vo.userId = dto.getUserId();
        vo.username = dto.getUsername();
        vo.avatarUrl = dto.getAvatarUrl();
        vo.currentStreak = dto.getCurrentStreak();
        vo.longestStreak = dto.getLongestStreak();
        vo.totalActiveDays = dto.getTotalActiveDays();
        vo.badge = computeBadge(dto);
        return vo;
    }
    
    /**
     * Compute badge based on streak performance without modifying the original DTO.
     */
    private static String computeBadge(StreakLeaderboardEntryDTO dto) {
        if (dto.getRank() == null) return null;
        
        long rank = dto.getRank();
        int currentStreak = dto.getCurrentStreak() != null ? dto.getCurrentStreak() : 0;
        int longestStreak = dto.getLongestStreak() != null ? dto.getLongestStreak() : 0;
        
        if (rank == 1) {
            return "🔥 坚持之王";
        } else if (rank <= 3) {
            return "💪 毅力超群";
        } else if (rank <= 10) {
            return "⚡ 坚持达人";
        } else if (longestStreak >= 30) {
            return "🏆 长跑冠军";
        } else if (longestStreak >= 14) {
            return "🎯 稳定发挥";
        } else if (longestStreak >= 7) {
            return "🌟 坚持不懈";
        } else if (currentStreak >= 3) {
            return "🚀 积极向上";
        } else {
            return "🎖️ 精英";
        }
    }
}