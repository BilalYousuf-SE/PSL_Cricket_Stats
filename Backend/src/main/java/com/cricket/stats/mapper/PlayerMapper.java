package com.cricket.stats.mapper;

import com.cricket.stats.dto.PlayerDTO;
import com.cricket.stats.entity.Player;

public class PlayerMapper {

    public static PlayerDTO toDTO(Player player) {
        if (player == null) return null;

        PlayerDTO dto = new PlayerDTO();
        dto.setId(player.getId());
        dto.setFirstName(player.getFirstName());
        dto.setLastName(player.getLastName());
        dto.setFullName(player.getFullName());
        dto.setAge(player.getAge());
        dto.setRole(player.getRole());
        dto.setNationality(player.getNationality());
        dto.setBattingStyle(player.getBattingStyle());
        dto.setBowlingStyle(player.getBowlingStyle());
        return dto;
    }
}
