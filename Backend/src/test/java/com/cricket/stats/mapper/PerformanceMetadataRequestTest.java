package com.cricket.stats.mapper;

import com.cricket.stats.dto.DeliveryRequestDTO;
import com.cricket.stats.dto.MatchRequestDTO;
import com.cricket.stats.entity.MatchStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PerformanceMetadataRequestTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void omittedAndExplicitlyClearedFielderRemainDistinct() throws Exception {
        assertThat(mapper.readValue("{}", DeliveryRequestDTO.class).isFielderIdProvided()).isFalse();
        DeliveryRequestDTO cleared = mapper.readValue("{\"fielderId\":null}", DeliveryRequestDTO.class);
        assertThat(cleared.isFielderIdProvided()).isTrue();
        assertThat(cleared.getFielderId()).isNull();
        assertThat(mapper.readValue("{\"fielderId\":415}", DeliveryRequestDTO.class).getFielderId()).isEqualTo(415L);
    }

    @Test void omittedAndExplicitlyClearedStageRemainDistinct() throws Exception {
        assertThat(mapper.readValue("{}", MatchRequestDTO.class).isStageProvided()).isFalse();
        assertThat(mapper.readValue("{\"stage\":null}", MatchRequestDTO.class).isStageProvided()).isTrue();
        assertThat(mapper.readValue("{\"stage\":\"Final\"}", MatchRequestDTO.class).getStage()).isEqualTo(MatchStage.Final);
    }
}
