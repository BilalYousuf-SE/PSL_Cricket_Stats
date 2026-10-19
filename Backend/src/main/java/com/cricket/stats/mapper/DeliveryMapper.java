package com.cricket.stats.mapper;

import com.cricket.stats.dto.DeliveryDTO;
import com.cricket.stats.entity.Delivery;

public class DeliveryMapper {

    public static DeliveryDTO toDTO(Delivery delivery) {
        if (delivery == null) return null;

        DeliveryDTO dto = new DeliveryDTO();
        dto.setDeliveryId(delivery.getDeliveryId());
        dto.setInnings(delivery.getInnings());
        dto.setOverNum(delivery.getOverNum());
        dto.setBallNum(delivery.getBallNum());

        Integer runsBatter = delivery.getRunsBatter() != null ? delivery.getRunsBatter() : 0;
        Integer runsExtras = delivery.getRunsExtras() != null ? delivery.getRunsExtras() : 0;
        dto.setRunsBatter(runsBatter);
        dto.setRunsExtras(runsExtras);
        dto.setTotalRuns(runsBatter + runsExtras);

        dto.setExtrasType(delivery.getExtraType());
        dto.setWicketType(delivery.getWicketType());

        // Match
        if (delivery.getMatch() != null) {
            dto.setMatchId(delivery.getMatch().getMatchId());
        }

        // Players — just names, no nested objects
        if (delivery.getBatter() != null) {
            dto.setBatterId(delivery.getBatter().getId());
            dto.setBatterName(delivery.getBatter().getFullName());
        }
        if (delivery.getNonStriker() != null) {
            dto.setNonStrikerId(delivery.getNonStriker().getId());
            dto.setNonStrikerName(delivery.getNonStriker().getFullName());
        }
        if (delivery.getBowler() != null) {
            dto.setBowlerId(delivery.getBowler().getId());
            dto.setBowlerName(delivery.getBowler().getFullName());
        }
        if (delivery.getPlayerDismissed() != null) {
            dto.setPlayerDismissedId(delivery.getPlayerDismissed().getId());
            dto.setPlayerDismissedName(delivery.getPlayerDismissed().getFullName());
        }

        return dto;
    }
}
