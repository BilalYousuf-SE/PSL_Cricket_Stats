package com.cricket.stats.mapper;

import com.cricket.stats.dto.PlayerDTO;
import com.cricket.stats.entity.BowlingStyle;
import com.cricket.stats.entity.Player;
import com.cricket.stats.entity.PlayingStyle;
import com.cricket.stats.entity.Roles;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PlayerMapperTest {
    @Test
    void keepsPlayingHandSeparateFromBowlingTechnique() {
        Player player = new Player();
        player.setId(42L);
        player.setRole(Roles.Baller);
        player.setPlayingStyle(PlayingStyle.Right);
        player.setBowlingStyle(BowlingStyle.LeftArmWristSpin);
        PlayerDTO dto = PlayerMapper.toDTO(player);
        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getRole()).isEqualTo(Roles.Baller);
        assertThat(dto.getPlayingStyle()).isEqualTo(PlayingStyle.Right);
        assertThat(dto.getBowlingStyle()).isEqualTo(BowlingStyle.LeftArmWristSpin);
    }

    @Test
    void unknownStylesRemainNull() {
        PlayerDTO dto = PlayerMapper.toDTO(new Player());
        assertThat(dto.getPlayingStyle()).isNull();
        assertThat(dto.getBowlingStyle()).isNull();
    }
}
