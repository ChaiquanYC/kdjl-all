package com.kdjl.server.service;

import com.kdjl.common.entity.PlayerActionLog;
import com.kdjl.server.repository.PlayerActionLogRepository;
import org.springframework.stereotype.Service;

@Service
public class PlayerLogService {

    private final PlayerActionLogRepository logRepo;

    public PlayerLogService(PlayerActionLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    public void log(Integer playerId, String playerName, String action, String targetType, Long targetId, String targetName, String detail) {
        PlayerActionLog log = new PlayerActionLog();
        log.setPlayerId(playerId);
        log.setPlayerName(playerName);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setTargetName(targetName);
        log.setDetail(detail);
        logRepo.save(log);
    }

    public void log(Integer playerId, String playerName, String action, String detail) {
        log(playerId, playerName, action, null, null, null, detail);
    }
}
