package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final DateTimeFormatter YMDHMS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_ACCEPTED_TASKS = 15;

    private final PlayerRepository playerRepo;
    private final UserBagRepository bagRepo;
    private final PropsRepository propsRepo;
    private final UserPetRepository userPetRepo;
    private final TaskDefRepository taskDefRepo;
    private final TaskAcceptRepository taskAcceptRepo;
    private final MonsterRepository monsterRepo;
    private final LevelUpService levelUpService;
    private final PlayerExtRepository playerExtRepo;
    private final PetRepository petRepo;

    public TaskService(PlayerRepository playerRepo, UserBagRepository bagRepo,
                       PropsRepository propsRepo, UserPetRepository userPetRepo,
                       TaskDefRepository taskDefRepo, TaskAcceptRepository taskAcceptRepo,
                       MonsterRepository monsterRepo, LevelUpService levelUpService,
                       PlayerExtRepository playerExtRepo, PetRepository petRepo) {
        this.playerRepo = playerRepo;
        this.bagRepo = bagRepo;
        this.propsRepo = propsRepo;
        this.userPetRepo = userPetRepo;
        this.taskDefRepo = taskDefRepo;
        this.taskAcceptRepo = taskAcceptRepo;
        this.monsterRepo = monsterRepo;
        this.levelUpService = levelUpService;
        this.playerExtRepo = playerExtRepo;
        this.petRepo = petRepo;
    }

    // ==================== List Available Tasks ====================

    public List<Map<String, Object>> listTasks(Integer playerId) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return List.of();

        String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
        Set<String> completedEntries = new HashSet<>(Arrays.asList(tasklog.split(",")));

        UserPet mainPet = getMainPet(playerId);
        int playerLv = mainPet != null && mainPet.getLevel() != null ? mainPet.getLevel() : 1;
        double playerCzl = parseCzl(mainPet);
        int mainPetWx = mainPet != null && mainPet.getWx() != null ? mainPet.getWx() : 0;

        // Get accepted tasks
        List<TaskAccept> accepts = taskAcceptRepo.findByPlayerId(playerId.longValue());
        Map<Long, TaskAccept> acceptMap = new HashMap<>();
        Set<Long> acceptedIds = new HashSet<>();
        for (TaskAccept a : accepts) {
            acceptMap.put(a.getTaskId(), a);
            acceptedIds.add(a.getTaskId());
        }

        String nowTime = LocalDateTime.now().format(YMDHMS);
        int playerVip = player.getVip() != null ? player.getVip() : 0;
        int playerScore = player.getScore() != null ? player.getScore() : 0;
        int playerPaihang = player.getPaiHang() != null ? player.getPaiHang() : 0;

        List<TaskDef> all = taskDefRepo.findAll().stream()
            .filter(t -> t.getHide() != null && t.getHide() == 1)
            .collect(Collectors.toList());

        // Build xulie/rwl chain progress map
        Map<Integer, Set<Long>> rwlCompleted = buildRwlCompletedMap(all, completedEntries, acceptedIds, accepts);

        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskDef t : all) {
            // Check flags: time-limited tasks
            if (t.getFlags() != null && t.getFlags() > 0) {
                // Task is only visible during its time window
                if (!isTaskTimeActive(t.getFlags(), nowTime)) continue;
            }

            // Check series prerequisite
            if (t.getXulie() != null && t.getXulie() > 0) {
                if (!canSeeTaskInSeries(t, all, completedEntries, acceptedIds, rwlCompleted)) continue;
            }

            // Check limitlv conditions for visibility
            if (!meetsLimitLvForVisibility(t.getLimitlv(), playerLv, mainPetWx, playerCzl, mainPet,
                playerScore, playerVip, playerPaihang, player)) continue;

            // Handle rwl chain visibility
            String cid = t.getCid();
            if (cid != null && !cid.isEmpty() && !"0".equals(cid) && !"self".equals(cid)) {
                if (!isTaskVisibleByCid(t, cid, completedEntries, acceptedIds, accepts, all)) continue;
            } else if (cid == null || "0".equals(cid) || "self".equals(cid)) {
                // Single-completion task: skip if already completed
                if (completedEntries.contains("task:" + t.getId())) continue;
            }

            Map<String, Object> m = buildTaskMap(t, acceptMap, completedEntries, playerLv);
            result.add(m);
        }
        return result;
    }

    // ==================== List Accepted Tasks ====================

    public List<Map<String, Object>> listAcceptedTasks(Integer playerId) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return List.of();

        List<TaskAccept> accepts = taskAcceptRepo.findByPlayerId(playerId.longValue());
        List<Map<String, Object>> result = new ArrayList<>();
        String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";

        for (TaskAccept a : accepts) {
            TaskDef t = taskDefRepo.findById(a.getTaskId()).orElse(null);
            if (t == null) continue;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("title", t.getTitle());
            m.put("fromnpc", t.getFromnpc());
            m.put("frommsg", t.getFrommsg());
            m.put("okneed", t.getOkneed());
            m.put("okneedDesc", resolveNames(t.getOkneed()));
            m.put("result", t.getResult());
            m.put("resultDesc", resolveNames(t.getResult()));
            m.put("limitlv", t.getLimitlv());
            m.put("limitlvDesc", resolveLimitlvNames(t.getLimitlv()));
            m.put("color", t.getColor());
            m.put("oknpc", t.getOknpc());
            m.put("accepted", true);
            m.put("acceptId", a.getId());
            m.put("state", a.getState());

            // Check if completable
            boolean completable = checkTaskCompletable(player, t, a, tasklog);
            m.put("canComplete", completable);

            // Parse kill progress from state
            if (a.getState() != null && a.getState().startsWith("kill:")) {
                m.put("killProgress", parseKillProgress(a.getState()));
            }
            result.add(m);
        }
        return result;
    }

    // ==================== Accept Task ====================

    @Transactional
    public Map<String, Object> acceptTask(Integer playerId, Long taskId) {
        Player player = playerRepo.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));

        TaskDef task = taskDefRepo.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        UserPet mainPet = getMainPet(playerId);
        int playerLv = mainPet != null && mainPet.getLevel() != null ? mainPet.getLevel() : 1;
        double playerCzl = parseCzl(mainPet);
        int mainPetWx = mainPet != null && mainPet.getWx() != null ? mainPet.getWx() : 0;

        // Check already accepted
        List<TaskAccept> existingAccepts = taskAcceptRepo.findByPlayerId(playerId.longValue());
        if (existingAccepts.size() >= MAX_ACCEPTED_TASKS) {
            throw new IllegalArgumentException("您已经接受了" + MAX_ACCEPTED_TASKS + "个任务，超过了最大限制！");
        }
        for (TaskAccept a : existingAccepts) {
            if (a.getTaskId().equals(taskId)) {
                throw new IllegalArgumentException("您已经接受此任务！");
            }
        }

        // Check single-completion (no cid or cid=self)
        String cid = task.getCid();
        if (cid == null || "0".equals(cid) || "self".equals(cid)) {
            String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
            if (tasklog.contains("task:" + taskId + ",") || tasklog.endsWith("task:" + taskId)) {
                throw new IllegalArgumentException("该任务只能接受一次！");
            }
        }

        // Check rwl chain: must have completed the previous task in chain
        if (cid != null && cid.startsWith("rwl:")) {
            checkRwlAccept(player, task);
        }

        // Check limitlv conditions
        String limitlvError = checkLimitLvForAccept(task.getLimitlv(), player, mainPet,
            playerLv, mainPetWx, playerCzl, playerVip());
        if (limitlvError != null) throw new IllegalArgumentException(limitlvError);

        // Check okneed online time (zx)
        String okneed = task.getOkneed();
        if (okneed != null && okneed.contains("zx:")) {
            for (String part : okneed.split(",")) {
                if (part.startsWith("zx:")) {
                    int hours = Integer.parseInt(part.split(":")[1]);
                    PlayerExt ext = playerExtRepo.findById(playerId).orElse(null);
                    int onlineSeconds = ext != null && ext.getOnlineTime() != null ? ext.getOnlineTime() : 0;
                    if (onlineSeconds < hours * 3600) {
                        throw new IllegalArgumentException("此任务需要在线" + hours + "小时才可接受，您目前在线时间还不够！");
                    }
                }
            }
        }

        // Create accept record
        TaskAccept accept = new TaskAccept();
        accept.setPlayerId(playerId.longValue());
        accept.setTaskId(taskId);
        accept.setTime(System.currentTimeMillis() / 1000);

        // Initialize kill tracking state
        if (okneed != null && okneed.contains("killmon")) {
            accept.setState("," + buildKillInitState(okneed));
        } else {
            accept.setState("0");
        }

        // Track comself pet for tasks with no:1 (OR groups)
        if (okneed != null && okneed.contains("no:1")) {
            String comselfBid = String.valueOf(player.getMbid() != null ? player.getMbid() : 0);
            accept.setComself(comselfBid);
        }

        taskAcceptRepo.save(accept);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", task.getTitle());
        result.put("taskId", taskId);
        result.put("frommsg", task.getFrommsg());
        return result;
    }

    // ==================== Complete Task ====================

    @Transactional
    public Map<String, Object> completeTask(Integer playerId, Long taskId) {
        Player player = playerRepo.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));

        TaskDef task = taskDefRepo.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("任务不存在"));

        TaskAccept accept = taskAcceptRepo.findByPlayerIdAndTaskId(playerId.longValue(), taskId)
            .orElseThrow(() -> new IllegalArgumentException("未接受该任务"));

        String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
        UserPet mainPet = getMainPet(playerId);
        int playerLv = mainPet != null && mainPet.getLevel() != null ? mainPet.getLevel() : 1;
        int mainPetWx = mainPet != null && mainPet.getWx() != null ? mainPet.getWx() : 0;
        int playerVip = player.getVip() != null ? player.getVip() : 0;
        int playerScore = player.getScore() != null ? player.getScore() : 0;
        int playerPrestige = player.getPrestige() != null ? player.getPrestige() : 0;
        int playerMoney = player.getMoney() != null ? player.getMoney() : 0;
        int playerPaihang = player.getPaiHang() != null ? player.getPaiHang() : 0;
        PlayerExt ext = playerExtRepo.findById(playerId).orElse(null);

        // Check single-completion
        String cid = task.getCid();
        if (cid == null || "0".equals(cid) || "self".equals(cid)) {
            if (tasklog.contains("task:" + taskId)) {
                throw new IllegalArgumentException("该任务只能完成一次！");
            }
        }

        // Check limitlv conditions for completion
        String limitlvError = checkLimitLvForComplete(task.getLimitlv(), player, mainPet,
            playerLv, mainPetWx, playerVip, playerScore, playerPaihang, accept);
        if (limitlvError != null) throw new IllegalArgumentException(limitlvError);

        // Check bag space (need at least 3 slots)
        int bagUsed = countBagUsed(playerId);
        if (bagUsed + 3 > (player.getMaxBag() != null ? player.getMaxBag() : 20)) {
            throw new IllegalArgumentException("您的背包空间不足，请预留至少3个背包格子！");
        }

        // Check okneed conditions
        String okneed = task.getOkneed();
        String state = accept.getState() != null ? accept.getState() : "";
        String combinedLog = ",see:" + task.getOknpc() + state;

        if (okneed != null && !okneed.isEmpty() && !"0".equals(okneed)) {
            String conditionError = checkOkneedForComplete(okneed, player, accept, combinedLog,
                playerLv, mainPetWx, mainPet);
            if (conditionError != null) throw new IllegalArgumentException(conditionError);
        }

        // Deduct consumable resources
        deductTaskCosts(okneed, player, playerId, accept);

        // Clear task items
        clearTaskItems(okneed, playerId);

        // Grant rewards
        String rewardMsg = grantRewards(player, task, mainPet);

        // Handle rwl chain completion
        if (cid != null && cid.startsWith("rwl:")) {
            handleRwlCompletion(player, task, cid);
        } else if (cid == null || "0".equals(cid) || "self".equals(cid)) {
            // Single-completion: mark in tasklog
            String completeEntry = "task:" + taskId;
            player.setTaskLog(tasklog.isEmpty() ? completeEntry : tasklog + "," + completeEntry);
        }

        player.setTask("");
        playerRepo.save(player);

        // Update cishu counter
        updateCishuCounter(player, task);

        // Remove accept record
        taskAcceptRepo.delete(accept);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completed", task.getTitle());
        result.put("reward", rewardMsg);
        result.put("okmsg", task.getOkmsg());
        return result;
    }

    // ==================== Abandon Task ====================

    @Transactional
    public Map<String, Object> abandonTask(Integer playerId, Long taskId) {
        TaskAccept accept = taskAcceptRepo.findByPlayerIdAndTaskId(playerId.longValue(), taskId)
            .orElseThrow(() -> new IllegalArgumentException("未接受该任务"));
        taskAcceptRepo.delete(accept);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("abandoned", true);
        result.put("taskId", taskId);
        return result;
    }

    // ==================== Visit NPC ====================

    @Transactional
    public Map<String, Object> visitNpc(Integer playerId, String npcId) {
        Player player = playerRepo.findById(playerId)
            .orElseThrow(() -> new IllegalArgumentException("玩家不存在"));
        String seeEntry = "see:" + npcId;
        String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
        if (!tasklog.contains(seeEntry)) {
            player.setTaskLog(tasklog.isEmpty() ? seeEntry : tasklog + "," + seeEntry);
            playerRepo.save(player);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("visited", npcId);
        return result;
    }

    // ==================== Kill Monster Progress ====================

    public void onMonsterKilled(Integer playerId, Long monsterId) {
        List<TaskAccept> accepts = taskAcceptRepo.findByPlayerId(playerId.longValue());
        for (TaskAccept a : accepts) {
            String state = a.getState();
            if (state == null || !state.contains("killmon:")) continue;

            Map<Long, KillTarget> targets = parseKillState(state);
            boolean updated = false;
            for (KillTarget kt : targets.values()) {
                if (kt.monsterIds.contains(monsterId) && kt.current < kt.needed) {
                    kt.current++;
                    updated = true;
                }
            }
            if (updated) {
                a.setState("," + serializeKillState(targets));
                taskAcceptRepo.save(a);
            }
        }
    }

    // ==================== Limitlv: Accept Checks ====================

    private String checkLimitLvForAccept(String limitlv, Player player, UserPet mainPet,
                                          int playerLv, int mainPetWx, double playerCzl, int playerVip) {
        if (limitlv == null || limitlv.isEmpty() || "0".equals(limitlv)) return null;
        for (String part : limitlv.split(",")) {
            if (part.trim().isEmpty()) continue;
            String[] kv = part.split(":");
            String key = kv[0];
            switch (key) {
                case "lv" -> {
                    if (kv.length < 2) break;
                    String[] range = kv[1].split("\\|");
                    int min = Integer.parseInt(range[0]);
                    int max = range.length > 1 ? Integer.parseInt(range[1]) : 0;
                    if (max == 0 && playerLv < min) return "您的等级不够接受此任务！";
                    if (max > 0 && (playerLv < min || playerLv > max)) return "您的等级不在可接此任务范围之内！";
                }
                case "level" -> {
                    if (kv.length < 2) break;
                    String[] range = kv[1].split("\\|");
                    int min = Integer.parseInt(range[0]);
                    int max = range.length > 1 ? Integer.parseInt(range[1]) : 0;
                    if (max == 0 && playerLv < min) return "您的等级不够接受此任务！";
                    if (max > 0 && (playerLv < min || playerLv > max)) return "您的等级不在可接此任务范围之内！";
                }
                case "wx" -> {
                    if (mainPetWx == 0) return "请先到牧场设置主战！";
                    String[] wxs = kv[1].split("\\|");
                    boolean match = false;
                    for (String w : wxs) {
                        if (Integer.parseInt(w) == mainPetWx) { match = true; break; }
                    }
                    if (!match) return "主战宠物五行与任务不符合任务要求！";
                }
                case "cz" -> {
                    String[] range = kv[1].split("\\|");
                    double min = Double.parseDouble(range[0]);
                    double max = range.length > 1 ? Double.parseDouble(range[1]) : 0;
                    if (max == 0 && playerCzl < min) return "该宠物成长值为" + playerCzl + "，无法领取该任务！";
                    if (max > 0 && (playerCzl < min || playerCzl > max)) return "该宠物成长值不在此任务范围内！";
                }
                case "comself" -> {
                    if (mainPet == null) return "请先设置主战宠物！";
                    String[] petIds = kv[1].split("\\|");
                    boolean match = false;
                    for (String pid : petIds) {
                        Pet pet = petRepo.findById(Long.parseLong(pid)).orElse(null);
                        if (pet != null && pet.getName() != null && pet.getName().equals(mainPet.getName())) {
                            match = true; break;
                        }
                    }
                    if (!match) return "您的当前主宠不能接受此任务！";
                }
                case "jifen" -> {
                    int required = Integer.parseInt(kv[1]);
                    int score = player.getScore() != null ? player.getScore() : 0;
                    if (score < required) return "您的当前积分不够接此任务！";
                }
                case "vip" -> {
                    int required = Integer.parseInt(kv[1]);
                    if (playerVip < required) return "您的vip积分不够接此任务！";
                }
                case "merge" -> {
                    PlayerExt ext = playerExtRepo.findById(player.getId()).orElse(null);
                    if (ext == null || ext.getMerge() == null || ext.getMerge() < 1)
                        return "您目前未婚，不能接受此任务！";
                }
                case "cishu" -> {
                    if (kv.length < 3) break;
                    int maxCount = Integer.parseInt(kv[1]);
                    int days = Integer.parseInt(kv[2]);
                    long count = countTaskCompletions(player, -1L, days);
                    if (count >= maxCount) return "在" + days + "天内您只能完成" + maxCount + "次此任务！";
                }
                case "xfyb" -> {
                    if (kv.length < 2) break;
                    String[] parts = kv[1].split(";");
                    if (parts.length < 2) return "领取任务出错！";
                    return "此任务领取条件复杂，暂不支持！"; // xfyb requires yblog queries
                }
                case "xfsj" -> {
                    if (kv.length < 2) break;
                    String[] dates = kv[1].split("\\|");
                    if (dates.length >= 2) {
                        int now = Integer.parseInt(LocalDate.now().format(YMD));
                        int start = Integer.parseInt(dates[0]);
                        int end = Integer.parseInt(dates[1]);
                        if (now < start || now > end) return "你未进行消费，无法领取任务！";
                    }
                }
            }
        }
        return null;
    }

    // ==================== Limitlv: Complete Checks ====================

    private String checkLimitLvForComplete(String limitlv, Player player, UserPet mainPet,
                                            int playerLv, int mainPetWx, int playerVip,
                                            int playerScore, int playerPaihang, TaskAccept accept) {
        if (limitlv == null || limitlv.isEmpty() || "0".equals(limitlv)) return null;
        for (String part : limitlv.split(",")) {
            if (part.trim().isEmpty()) continue;
            String[] kv = part.split(":");
            String key = kv[0];
            switch (key) {
                case "lv" -> {
                    if (kv.length < 2) break;
                    String[] range = kv[1].split("\\|");
                    int min = Integer.parseInt(range[0]);
                    int max = range.length > 1 ? Integer.parseInt(range[1]) : 0;
                    if (max == 0 && playerLv < min) return "您的等级不够完成此任务！";
                    if (max > 0 && (playerLv < min || playerLv > max)) return "您的等级不在完成此任务范围之内！";
                }
                case "wx" -> {
                    if (mainPetWx == 0) return "请先到牧场设置主战！";
                    String[] wxs = kv[1].split("\\|");
                    boolean match = false;
                    for (String w : wxs) {
                        if (Integer.parseInt(w) == mainPetWx) { match = true; break; }
                    }
                    if (!match) return "主战宠物五行与任务不符合任务要求！";
                }
                case "comself" -> {
                    if (mainPet == null) return "请先设置主战宠物！";
                    String[] petIds = kv[1].split("\\|");
                    boolean match = false;
                    for (String pid : petIds) {
                        Pet pet = petRepo.findById(Long.parseLong(pid)).orElse(null);
                        if (pet != null && pet.getName() != null && pet.getName().equals(mainPet.getName())) {
                            match = true; break;
                        }
                    }
                    if (!match) return "您的当前主宠不能完成此任务！";
                }
                case "jifen" -> {
                    int required = Integer.parseInt(kv[1]);
                    if (playerScore < required) return "您的当前积分不够完成此任务！";
                }
                case "vip" -> {
                    int required = Integer.parseInt(kv[1]);
                    if (playerVip < required) return "您的vip积分不够完成此任务！";
                }
                case "cishu" -> {
                    if (kv.length < 3) break;
                    int maxCount = Integer.parseInt(kv[1]);
                    int days = Integer.parseInt(kv[2]);
                    long count = countTaskCompletions(player, -1L, days);
                    if (count >= maxCount) return "在" + days + "天内您只能完成" + maxCount + "次此任务！";
                }
                case "timelimit" -> {
                    int hours = Integer.parseInt(kv[1]);
                    long acceptTime = accept.getTime() != null ? accept.getTime() : 0;
                    long now = System.currentTimeMillis() / 1000;
                    if (acceptTime > 0 && (now - acceptTime) > hours * 3600L)
                        return "超过时间限时，该任务必须在" + hours + "小时内完成！";
                }
                case "paihang" -> {
                    int required = Integer.parseInt(kv[1]);
                    if (playerPaihang != required) return "您不能完成此任务！";
                }
                case "xfsj" -> {
                    // Just passes through — the real check is at accept time
                }
            }
        }
        return null;
    }

    // ==================== Okneed Condition Checking ====================

    /**
     * Check all conditions in okneed. Returns error message or null if all pass.
     * Supports OR groups separated by ",no:1," within the okneed string.
     */
    private String checkOkneedForComplete(String okneed, Player player, TaskAccept accept,
                                           String combinedLog, int playerLv, int mainPetWx,
                                           UserPet mainPet) {
        // Split into OR groups by "no:1"
        List<String> groups = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String part : okneed.split(",")) {
            String trimmed = part.trim();
            if ("no:1".equals(trimmed)) {
                if (current.length() > 0) {
                    groups.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                if (current.length() > 0) current.append(",");
                current.append(trimmed);
            }
        }
        if (current.length() > 0) groups.add(current.toString());

        // Any one group must be fully satisfied
        List<String> errors = new ArrayList<>();
        for (String group : groups) {
            String groupError = checkOkneedGroup(group, player, accept, playerLv, mainPetWx, mainPet);
            if (groupError == null) return null; // This group passed
            errors.add(groupError);
        }
        return "让你做的事还没做完，是不能获得奖励的噢！" + " (" + String.join(" 或 ", errors) + ")";
    }

    private String checkOkneedGroup(String group, Player player, TaskAccept accept,
                                     int playerLv, int mainPetWx, UserPet mainPet) {
        for (String cond : group.split(",")) {
            if (cond.trim().isEmpty()) continue;
            String[] parts = cond.split(":");
            String type = parts[0];

            switch (type) {
                case "see" -> {
                    // NPC visit check — verified via NPC completion flow
                }
                case "giveitem" -> {
                    if (parts.length < 2) break;
                    int needCount = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                    String[] propIds = parts[1].split("\\|");
                    long totalHave = 0;
                    for (String pidStr : propIds) {
                        int pid = Integer.parseInt(pidStr);
                        totalHave += bagRepo.findByPlayerId(player.getId().longValue()).stream()
                            .filter(b -> b.getPropId() != null && b.getPropId() == pid
                                && (b.getSums() != null && b.getSums() > 0)
                                && (b.getZbing() == null || b.getZbing() == 0))
                            .mapToLong(b -> b.getSums() != null ? b.getSums() : 0)
                            .sum();
                    }
                    if (totalHave < needCount) {
                        Props p = propsRepo.findById((long) Integer.parseInt(propIds[0])).orElse(null);
                        return "需要" + (p != null ? p.getName() : "道具#" + propIds[0]) + " x" + needCount;
                    }
                }
                case "killmon" -> {
                    if (parts.length < 2) break;
                    String[] monsterIds = parts[1].split("\\|");
                    int needed = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                    int killed = getKillProgress(accept.getState(), monsterIds);
                    if (killed < needed)
                        return "需要击杀怪物 " + needed + "只 (已击杀" + killed + "只)";
                }
                case "givevip" -> {
                    int required = Integer.parseInt(parts[1]);
                    int playerVip = player.getVip() != null ? player.getVip() : 0;
                    if (playerVip < required)
                        return "当月VIP反馈积分不够！";
                }
                case "givejifen" -> {
                    int required = Integer.parseInt(parts[1]);
                    int score = player.getScore() != null ? player.getScore() : 0;
                    if (score < required) return "积分不够！";
                }
                case "giveww" -> {
                    int required = Integer.parseInt(parts[1]);
                    int prestige = player.getPrestige() != null ? player.getPrestige() : 0;
                    if (prestige < required) return "威望不够！";
                }
                case "givemoney" -> {
                    int required = Integer.parseInt(parts[1]);
                    int money = player.getMoney() != null ? player.getMoney() : 0;
                    if (money < required) return "金币不够！";
                }
                case "givedianjuan" -> {
                    int required = Integer.parseInt(parts[1]);
                    int activeScore = player.getActiveScore() != null ? player.getActiveScore() : 0;
                    if (activeScore < required) return "点卷不够！";
                }
                case "giveml" -> {
                    int required = Integer.parseInt(parts[1]);
                    PlayerExt ext = playerExtRepo.findById(player.getId()).orElse(null);
                    int ml = ext != null && ext.getMl() != null ? ext.getMl() : 0;
                    if (ml < required) return "您的魅力值不足！";
                }
                case "monself" -> {
                    if (mainPet == null) return "请先设置主战宠物！";
                    String[] petIds = parts[1].split("\\|");
                    boolean match = false;
                    for (String pid : petIds) {
                        Pet pet = petRepo.findById(Long.parseLong(pid)).orElse(null);
                        if (pet != null && pet.getName() != null && pet.getName().equals(mainPet.getName())) {
                            match = true; break;
                        }
                    }
                    if (!match) return "您不能用该主战宠物交此任务！";
                }
                case "lv" -> {
                    if (parts.length < 2) break;
                    String[] range = parts[1].split("\\|");
                    int min = Integer.parseInt(range[0]);
                    int max = range.length > 1 ? Integer.parseInt(range[1]) : 0;
                    if (max == 0 && playerLv < min) return "您的等级不够完成此任务！";
                    if (max > 0 && (playerLv < min || playerLv > max)) return "您的等级不在完成此任务范围之内！";
                }
                case "wx" -> {
                    if (mainPetWx == 0) return "请先设置主战宠物！";
                    String[] wxs = parts[1].split("\\|");
                    boolean match = false;
                    for (String w : wxs) {
                        if (Integer.parseInt(w) == mainPetWx) { match = true; break; }
                    }
                    if (!match) return "主战宠物五行与任务不符合任务要求！";
                }
                case "zx" -> {
                    // Online time — already checked at accept time, skip for complete
                }
                default -> {
                    // Unknown condition, treat as passed
                }
            }
        }
        return null; // All conditions in this group passed
    }

    // ==================== Deduct Task Costs ====================

    private void deductTaskCosts(String okneed, Player player, Integer playerId, TaskAccept accept) {
        if (okneed == null || okneed.isEmpty()) return;
        int scoreDeduct = 0, prestigeDeduct = 0, moneyDeduct = 0, vipDeduct = 0, mlDeduct = 0, activeScoreDeduct = 0;

        for (String part : okneed.split(",")) {
            if (part.trim().isEmpty() || "no:1".equals(part.trim())) continue;
            String[] kv = part.split(":");
            switch (kv[0]) {
                case "givejifen" -> scoreDeduct += Integer.parseInt(kv[1]);
                case "giveww" -> prestigeDeduct += Integer.parseInt(kv[1]);
                case "givemoney" -> moneyDeduct += Integer.parseInt(kv[1]);
                case "givevip" -> vipDeduct += Integer.parseInt(kv[1]);
                case "giveml" -> mlDeduct += Integer.parseInt(kv[1]);
                case "givedianjuan" -> activeScoreDeduct += Integer.parseInt(kv[1]);
            }
        }

        if (scoreDeduct > 0) {
            player.setScore(Math.max(0, (player.getScore() != null ? player.getScore() : 0) - scoreDeduct));
        }
        if (prestigeDeduct > 0) {
            player.setPrestige(Math.max(0, (player.getPrestige() != null ? player.getPrestige() : 0) - prestigeDeduct));
        }
        if (moneyDeduct > 0) {
            player.setMoney(Math.max(0, (player.getMoney() != null ? player.getMoney() : 0) - moneyDeduct));
        }
        if (vipDeduct > 0) {
            player.setVip(Math.max(0, (player.getVip() != null ? player.getVip() : 0) - vipDeduct));
        }
        if (activeScoreDeduct > 0) {
            player.setActiveScore(Math.max(0, (player.getActiveScore() != null ? player.getActiveScore() : 0) - activeScoreDeduct));
        }
        if (mlDeduct > 0) {
            PlayerExt ext = playerExtRepo.findById(playerId).orElse(null);
            if (ext != null && ext.getMl() != null && ext.getMl() >= mlDeduct) {
                ext.setMl(ext.getMl() - mlDeduct);
                playerExtRepo.save(ext);
            }
        }
    }

    // ==================== Clear Task Items ====================

    private void clearTaskItems(String okneed, Integer playerId) {
        if (okneed == null || okneed.isEmpty()) return;
        Set<String> processed = new HashSet<>();
        for (String part : okneed.split(",")) {
            if (part.trim().isEmpty() || "no:1".equals(part.trim())) continue;
            if (!part.startsWith("giveitem:")) continue;
            if (processed.contains(part)) continue;
            processed.add(part);
            String[] kv = part.split(":");
            if (kv.length < 2) continue;
            int needCount = kv.length >= 3 ? Integer.parseInt(kv[2]) : 1;
            for (String pidStr : kv[1].split("\\|")) {
                int pid = Integer.parseInt(pidStr);
                int remaining = needCount;
                List<UserBag> stacks = bagRepo.findByPlayerId(playerId.longValue()).stream()
                    .filter(b -> b.getPropId() != null && b.getPropId() == pid
                        && (b.getSums() != null && b.getSums() > 0)
                        && (b.getZbing() == null || b.getZbing() == 0))
                    .sorted((a, b) -> Integer.compare(b.getSums() != null ? b.getSums() : 0,
                        a.getSums() != null ? a.getSums() : 0))
                    .collect(Collectors.toList());
                for (UserBag stack : stacks) {
                    if (remaining <= 0) break;
                    int have = stack.getSums() != null ? stack.getSums() : 0;
                    int deduct = Math.min(remaining, have);
                    int newSum = have - deduct;
                    if (newSum <= 0) {
                        bagRepo.delete(stack);
                    } else {
                        stack.setSums(newSum);
                        bagRepo.save(stack);
                    }
                    remaining -= deduct;
                }
                if (remaining > 0) break;
            }
        }
    }

    // ==================== Reward Granting ====================

    private String grantRewards(Player player, TaskDef task, UserPet mainPet) {
        StringBuilder rewardMsg = new StringBuilder();
        String rewardStr = task.getResult();
        if (rewardStr == null || rewardStr.isEmpty() || "0".equals(rewardStr)) return "";

        int playerId = player.getId();
        int jPrestige = player.getJPrestige() != null ? player.getJPrestige() : 0;

        for (String reward : rewardStr.split(",")) {
            if (reward.trim().isEmpty()) continue;
            String[] parts = reward.split(":");
            String type = parts[0];

            switch (type) {
                case "props" -> {
                    if (parts.length < 2) break;
                    int count = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                    for (String pidStr : parts[1].split("\\|")) {
                        int pid = Integer.parseInt(pidStr);
                        giveItem(player, (long) pid, count);
                        Props p = propsRepo.findById((long) pid).orElse(null);
                        if (rewardMsg.length() > 0) rewardMsg.append("，");
                        rewardMsg.append(p != null ? p.getName() : "道具").append(" x").append(count);
                    }
                }
                case "bprops" -> {
                    if (parts.length < 2) break;
                    int count = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                    for (String pidStr : parts[1].split("\\|")) {
                        int pid = Integer.parseInt(pidStr);
                        giveItem(player, (long) pid, count);
                        Props p = propsRepo.findById((long) pid).orElse(null);
                        if (rewardMsg.length() > 0) rewardMsg.append("，");
                        rewardMsg.append(p != null ? p.getName() : "道具(绑)").append(" x").append(count);
                    }
                }
                case "exp" -> {
                    int exp = getPrestigeScaledAmount(parts, jPrestige, 1);
                    if (exp <= 0) break;
                    if (mainPet != null) {
                        long oldLv = mainPet.getLevel() != null ? mainPet.getLevel() : 1;
                        levelUpService.addExp(mainPet, exp);
                        userPetRepo.save(mainPet);
                        long newLv = mainPet.getLevel() != null ? mainPet.getLevel() : oldLv;
                        if (rewardMsg.length() > 0) rewardMsg.append("，");
                        rewardMsg.append("经验 +").append(exp);
                        if (newLv > oldLv) {
                            rewardMsg.append(" (升级至").append(newLv).append("级)");
                        }
                    }
                }
                case "money" -> {
                    int money = getPrestigeScaledAmount(parts, jPrestige, 1);
                    if (money <= 0) break;
                    player.setMoney((player.getMoney() != null ? player.getMoney() : 0) + money);
                    if (rewardMsg.length() > 0) rewardMsg.append("，");
                    rewardMsg.append("金币 +").append(money);
                }
                case "addyb" -> {
                    int yb = Integer.parseInt(parts[1]);
                    player.setYb((player.getYb() != null ? player.getYb() : 0) + yb);
                    playerRepo.save(player);
                    if (rewardMsg.length() > 0) rewardMsg.append("，");
                    rewardMsg.append("元宝 +").append(yb);
                }
                case "addsj" -> {
                    int sj = Integer.parseInt(parts[1]);
                    PlayerExt ext = playerExtRepo.findById(player.getId()).orElse(null);
                    if (ext != null) {
                        ext.setSj((ext.getSj() != null ? ext.getSj() : 0) + sj);
                        playerExtRepo.save(ext);
                    }
                    if (rewardMsg.length() > 0) rewardMsg.append("，");
                    rewardMsg.append("水晶 +").append(sj);
                }
                case "addmoney" -> {
                    int money = Integer.parseInt(parts[1]);
                    player.setMoney((player.getMoney() != null ? player.getMoney() : 0) + money);
                    if (rewardMsg.length() > 0) rewardMsg.append("，");
                    rewardMsg.append("金币 +").append(money);
                }
                case "itemrand" -> {
                    // itemrand:X:Y:Z|A:B:C — pick one item from pool
                    // Format: itemrand:PROPID:RANDCHANCE:COUNT|PROPID:RANDCHANCE:COUNT
                    String pool = reward.replace("itemrand:", "");
                    String[] options = pool.split("\\|");
                    for (String opt : options) {
                        String[] optParts = opt.split(":");
                        if (optParts.length >= 3) {
                            int randChance = Integer.parseInt(optParts[1]);
                            if (new Random().nextInt(randChance) == 0) {
                                int propId = Integer.parseInt(optParts[0]);
                                int cnt = Integer.parseInt(optParts[2]);
                                giveItem(player, (long) propId, cnt);
                                Props p = propsRepo.findById((long) propId).orElse(null);
                                if (rewardMsg.length() > 0) rewardMsg.append("，");
                                rewardMsg.append("获得 ").append(p != null ? p.getName() : "道具").append(" x").append(cnt);
                                break;
                            }
                        }
                    }
                }
                case "lvprops" -> {
                    // lvprops:PROPID:COUNT:MINLVL|MAXLVL
                    if (mainPet == null) break;
                    int lv = mainPet.getLevel() != null ? mainPet.getLevel() : 1;
                    if (parts.length >= 4) {
                        String[] lvRange = parts[3].split("\\|");
                        int minLv = Integer.parseInt(lvRange[0]);
                        int maxLv = lvRange.length > 1 ? Integer.parseInt(lvRange[1]) : 0;
                        if (lv >= minLv && (maxLv == 0 || lv <= maxLv)) {
                            int propId = Integer.parseInt(parts[1]);
                            int cnt = Integer.parseInt(parts[2]);
                            giveItem(player, (long) propId, cnt);
                            Props p = propsRepo.findById((long) propId).orElse(null);
                            if (rewardMsg.length() > 0) rewardMsg.append("，");
                            rewardMsg.append(p != null ? p.getName() : "道具").append(" x").append(cnt);
                        }
                    }
                }
                case "paihang" -> {
                    // Reset paihang — the PHP sets user['paihang'] = 0
                    // This is handled by the result: paihang:0
                }
                case "gonggao" -> {
                    // System announcement — logged but not implemented here
                }
                case "fksj" -> {
                    // Feedback crystal for yb consumption — complex, skip for now
                }
            }
        }
        return rewardMsg.toString();
    }

    private int getPrestigeScaledAmount(String[] parts, int jPrestige, int defaultVal) {
        int amount = defaultVal;
        if (parts.length >= 2) {
            try { amount = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { return 0; }
        }
        if (parts.length >= 3) {
            String[] range = parts[2].split("\\|");
            if (range.length >= 2) {
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);
                if (min == 0 && jPrestige > max) return 0;
                if (max == 0 && jPrestige < min) return 0;
                if (min > 0 && max > 0 && (jPrestige < min || jPrestige > max)) return 0;
            }
        }
        return amount;
    }

    // ==================== Task Chain (rwl) Handling ====================

    private void checkRwlAccept(Player player, TaskDef task) {
        String cid = task.getCid();
        if (cid == null || !cid.startsWith("rwl:")) return;
        String[] rwlParts = cid.substring(4).split("\\|");
        if (rwlParts.length < 2) return;
        long prevTaskId = Long.parseLong(rwlParts[0]);

        // Check if the previous task was completed
        String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
        if (!tasklog.contains("task:" + prevTaskId)) {
            // Check if previous task was accepted in the chain via tasklog table
            boolean prevAccepted = false;
            List<TaskAccept> accepts = taskAcceptRepo.findByPlayerId(player.getId().longValue());
            for (TaskAccept a : accepts) {
                if (a.getTaskId().equals(prevTaskId)) { prevAccepted = true; break; }
            }
            if (!prevAccepted) {
                throw new IllegalArgumentException("数据错误1!");
            }
        }
    }

    private void handleRwlCompletion(Player player, TaskDef task, String cid) {
        if (cid == null || !cid.startsWith("rwl:")) return;
        String[] rwlParts = cid.substring(4).split("\\|");
        long fromnpc = 0;
        if (task.getFromnpc() != null) {
            String[] fn = task.getFromnpc().split("\\|");
            fromnpc = Long.parseLong(fn[0]);
        }

        // Check if there's an existing tasklog entry for this xulie+fromnpc
        // Update if exists, insert if not
        String sql = "SELECT taskid FROM tasklog WHERE uid = " + player.getId()
            + " AND fromnpc = " + fromnpc + " AND xulie = " + task.getXulie();
        // Use existing tasklog mechanism for now
        String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
        String rwlEntry = "rwl:" + task.getXulie() + ":" + task.getId();
        if (!tasklog.contains(rwlEntry)) {
            player.setTaskLog(tasklog.isEmpty() ? rwlEntry : tasklog + "," + rwlEntry);
        }
    }

    // ==================== Task Visibility Helpers ====================

    private boolean isTaskTimeActive(Integer flags, String nowTime) {
        // Flags references timed events in time_config
        // For now, just pass through visible tasks
        return true;
    }

    private boolean canSeeTaskInSeries(TaskDef t, List<TaskDef> all, Set<String> completed,
                                        Set<Long> acceptedIds, Map<Integer, Set<Long>> rwlCompleted) {
        // If this task is the first in its series (no previous task in series), show it
        boolean hasPrev = false;
        boolean prevDoneOrAccepted = false;
        for (TaskDef other : all) {
            if (other.getXulie() != null && other.getXulie().equals(t.getXulie())
                && other.getId() < t.getId()) {
                hasPrev = true;
                if (completed.contains("task:" + other.getId()) || acceptedIds.contains(other.getId())) {
                    prevDoneOrAccepted = true;
                }
            }
        }
        if (!hasPrev) return true;
        return prevDoneOrAccepted;
    }

    private boolean isTaskVisibleByCid(TaskDef t, String cid, Set<String> completed,
                                        Set<Long> acceptedIds, List<TaskAccept> accepts,
                                        List<TaskDef> all) {
        if (cid.startsWith("rwl:")) {
            String[] rwlParts = cid.substring(4).split("\\|");
            if (rwlParts.length < 2) return false;
            long prevTaskId = Long.parseLong(rwlParts[0]);
            // Visible if previous task is completed
            if (completed.contains("task:" + prevTaskId)) return true;
            // Or if previous task is currently accepted
            for (TaskAccept a : accepts) {
                if (a.getTaskId().equals(prevTaskId)) return true;
            }
            // Or if this is the first task in rwl chain (hide==1, no completed needed)
            if (t.getHide() != null && t.getHide() == 1) {
                // Check if the player has any progress in this series
                String rwlEntry = "rwl:" + t.getXulie() + ":";
                for (String e : completed) {
                    if (e.startsWith(rwlEntry)) return true;
                }
                return false;
            }
            return false;
        }
        if (cid.startsWith("paihang:")) {
            return true; // Ranking-based — already filtered in limitlv
        }
        // For other cid patterns, show if hide==1
        return t.getHide() != null && t.getHide() == 1;
    }

    // ==================== Visibility Limitlv Checks ====================

    private boolean meetsLimitLvForVisibility(String limitlv, int playerLv, int mainPetWx,
                                               double playerCzl, UserPet mainPet, int playerScore,
                                               int playerVip, int playerPaihang, Player player) {
        if (limitlv == null || limitlv.isEmpty() || "0".equals(limitlv)) return true;
        for (String part : limitlv.split(",")) {
            if (part.trim().isEmpty()) continue;
            String[] kv = part.split(":");
            String key = kv[0];
            switch (key) {
                case "level" -> {
                    if (kv.length < 2) break;
                    String[] range = kv[1].split("\\|");
                    int min = Integer.parseInt(range[0]);
                    int max = range.length > 1 ? Integer.parseInt(range[1]) : 0;
                    if (max == 0 && playerLv < min) return false;
                    if (max > 0 && (playerLv < min || playerLv > max)) return false;
                }
                case "czl" -> {
                    if (kv.length < 2) break;
                    String[] range = kv[1].split("\\|");
                    double min = Double.parseDouble(range[0]);
                    double max = range.length > 1 ? Double.parseDouble(range[1]) : 0;
                    if (max == 0 && playerCzl < min) return false;
                    if (max > 0 && (playerCzl < min || playerCzl > max)) return false;
                }
                case "paihang" -> {
                    if (kv.length >= 2 && playerPaihang != Integer.parseInt(kv[1])) return false;
                }
            }
        }
        return true;
    }

    // ==================== Task Completable Check (for listAcceptedTasks) ====================

    private boolean checkTaskCompletable(Player player, TaskDef task, TaskAccept accept, String tasklog) {
        try {
            UserPet mainPet = getMainPet(player.getId());
            int playerLv = mainPet != null && mainPet.getLevel() != null ? mainPet.getLevel() : 1;
            int mainPetWx = mainPet != null && mainPet.getWx() != null ? mainPet.getWx() : 0;
            int playerVip = player.getVip() != null ? player.getVip() : 0;
            int playerScore = player.getScore() != null ? player.getScore() : 0;
            int playerPaihang = player.getPaiHang() != null ? player.getPaiHang() : 0;

            String limitLvError = checkLimitLvForComplete(task.getLimitlv(), player, mainPet,
                playerLv, mainPetWx, playerVip, playerScore, playerPaihang, accept);
            if (limitLvError != null) return false;

            String okneed = task.getOkneed();
            if (okneed != null && !okneed.isEmpty() && !"0".equals(okneed)) {
                String combinedLog = ",see:" + task.getOknpc() + (accept.getState() != null ? accept.getState() : "");
                String err = checkOkneedForComplete(okneed, player, accept, combinedLog,
                    playerLv, mainPetWx, mainPet);
                if (err != null) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== RWL Completed Map ====================

    private Map<Integer, Set<Long>> buildRwlCompletedMap(List<TaskDef> all, Set<String> completed,
                                                          Set<Long> acceptedIds, List<TaskAccept> accepts) {
        Map<Integer, Set<Long>> result = new HashMap<>();
        for (String entry : completed) {
            if (entry.startsWith("rwl:")) {
                String[] parts = entry.split(":");
                if (parts.length >= 3) {
                    int series = Integer.parseInt(parts[1]);
                    long taskId = Long.parseLong(parts[2]);
                    result.computeIfAbsent(series, k -> new HashSet<>()).add(taskId);
                }
            }
        }
        // Also add directly completed tasks
        for (TaskDef t : all) {
            if (completed.contains("task:" + t.getId())) {
                if (t.getXulie() != null && t.getXulie() > 0) {
                    result.computeIfAbsent(t.getXulie(), k -> new HashSet<>()).add(t.getId());
                }
            }
        }
        return result;
    }

    // ==================== Kill Progress Tracking ====================

    private static class KillTarget {
        final Set<Long> monsterIds;
        final int needed;
        int current;
        KillTarget(Set<Long> monsterIds, int needed) {
            this.monsterIds = monsterIds; this.needed = needed; this.current = 0;
        }
    }

    private String buildKillInitState(String okneed) {
        Map<Long, KillTarget> targets = new LinkedHashMap<>();
        long killIdx = 0;
        for (String cond : okneed.split(",")) {
            if (cond.trim().isEmpty() || "no:1".equals(cond.trim())) continue;
            if (cond.startsWith("killmon:")) {
                String[] parts = cond.split(":");
                Set<Long> mids = new LinkedHashSet<>();
                for (String mid : parts[1].split("\\|")) {
                    try { mids.add(Long.parseLong(mid)); } catch (NumberFormatException ignored) {}
                }
                int needed = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
                targets.put(killIdx++, new KillTarget(mids, needed));
            }
        }
        return serializeKillState(targets);
    }

    private String serializeKillState(Map<Long, KillTarget> targets) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, KillTarget> e : targets.entrySet()) {
            if (sb.length() > 0) sb.append(",");
            KillTarget kt = e.getValue();
            sb.append("killmon:");
            sb.append(kt.monsterIds.stream().map(String::valueOf).collect(Collectors.joining("|")));
            sb.append(":").append(kt.current).append("/").append(kt.needed);
        }
        return sb.toString();
    }

    private Map<Long, KillTarget> parseKillState(String state) {
        Map<Long, KillTarget> result = new LinkedHashMap<>();
        if (state == null || !state.contains("killmon:")) return result;
        long idx = 0;
        for (String segment : state.split(",")) {
            if (!segment.contains("killmon:")) continue;
            segment = segment.substring(segment.indexOf("killmon:"));
            String[] parts = segment.split(":");
            if (parts.length < 3) continue;
            // parts[0] = "killmon", parts[1] = monsterIds, parts[2] = "current/needed"
            Set<Long> mids = new LinkedHashSet<>();
            for (String mid : parts[1].split("\\|")) {
                try { mids.add(Long.parseLong(mid)); } catch (NumberFormatException ignored) {}
            }
            String[] progress = parts[2].split("/");
            int current = Integer.parseInt(progress[0]);
            int needed = progress.length > 1 ? Integer.parseInt(progress[1]) : 1;
            KillTarget kt = new KillTarget(mids, needed);
            kt.current = current;
            result.put(idx++, kt);
        }
        return result;
    }

    private int getKillProgress(String state, String[] monsterIds) {
        if (state == null || !state.contains("killmon:")) return 0;
        Map<Long, KillTarget> targets = parseKillState(state);
        int total = 0;
        Set<Long> targetIds = new HashSet<>();
        for (String mid : monsterIds) {
            try { targetIds.add(Long.parseLong(mid)); } catch (NumberFormatException ignored) {}
        }
        for (KillTarget kt : targets.values()) {
            Set<Long> overlap = new HashSet<>(kt.monsterIds);
            overlap.retainAll(targetIds);
            if (!overlap.isEmpty()) total += kt.current;
        }
        return total;
    }

    Map<String, Integer> parseKillProgress(String state) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (state == null || !state.contains("killmon:")) return result;
        Map<Long, KillTarget> targets = parseKillState(state);
        for (KillTarget kt : targets.values()) {
            String monsterList = kt.monsterIds.stream().map(String::valueOf)
                .collect(Collectors.joining(","));
            result.put(monsterList, kt.current);
            result.put(monsterList + "_needed", kt.needed);
        }
        return result;
    }

    // ==================== Helper Methods ====================

    private int playerVip() { return 0; } // Placeholder

    private UserPet getMainPet(Integer playerId) {
        Player player = playerRepo.findById(playerId).orElse(null);
        if (player == null || player.getMbid() == null) return null;
        return userPetRepo.findById(player.getMbid().longValue()).orElse(null);
    }

    private double parseCzl(UserPet mainPet) {
        if (mainPet == null || mainPet.getCzl() == null) return 0;
        try { return Double.parseDouble(mainPet.getCzl()); }
        catch (NumberFormatException e) { return 0; }
    }

    private int countBagUsed(Integer playerId) {
        return (int) bagRepo.findByPlayerId(playerId.longValue()).stream()
            .filter(b -> (b.getSums() != null && b.getSums() > 0) && (b.getZbing() == null || b.getZbing() == 0))
            .count();
    }

    private long countTaskCompletions(Player player, long taskId, int days) {
        String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
        String prefix = "task:";
        long count = 0;
        for (String entry : tasklog.split(",")) {
            if (taskId < 0) {
                if (entry.startsWith(prefix)) count++;
            } else if (entry.equals(prefix + taskId)) {
                count++;
            }
        }
        // Also check ctask counters
        String ctaskPrefix = taskId >= 0 ? "ctask:" + taskId + ":" : "ctask:";
        for (String entry : tasklog.split(",")) {
            if (entry.startsWith(ctaskPrefix)) {
                try { count = Integer.parseInt(entry.substring(ctaskPrefix.length())); }
                catch (NumberFormatException ignored) {}
            }
        }
        return count;
    }

    private void updateCishuCounter(Player player, TaskDef task) {
        String limitlv = task.getLimitlv();
        if (limitlv == null) return;
        for (String part : limitlv.split(",")) {
            if (part.startsWith("cishu:")) {
                String tasklog = player.getTaskLog() != null ? player.getTaskLog() : "";
                String ctaskPrefix = "ctask:" + task.getId() + ":";
                long currentCount = 0;
                String existingEntry = null;
                for (String entry : tasklog.split(",")) {
                    if (entry.startsWith(ctaskPrefix)) {
                        existingEntry = entry;
                        try { currentCount = Integer.parseInt(entry.substring(ctaskPrefix.length())); }
                        catch (NumberFormatException ignored) {}
                        break;
                    }
                }
                long newCount = currentCount + 1;
                String newEntry = ctaskPrefix + newCount;
                if (existingEntry != null) {
                    tasklog = tasklog.replace(existingEntry, newEntry);
                } else {
                    tasklog = tasklog.isEmpty() ? newEntry : tasklog + "," + newEntry;
                }
                player.setTaskLog(tasklog);
            }
        }
    }

    private void giveItem(Player player, Long propId, int count) {
        Props props = propsRepo.findById(propId).orElse(null);
        UserBag existing = bagRepo.findByPlayerId(player.getId().longValue()).stream()
            .filter(b -> b.getPropId() != null && b.getPropId().equals(propId)
                && (props == null || props.getVary() == null || props.getVary() == 1))
            .findFirst().orElse(null);
        if (existing != null) {
            existing.setSums((existing.getSums() != null ? existing.getSums() : 0) + count);
            existing.setStime(System.currentTimeMillis() / 1000);
            bagRepo.save(existing);
        } else {
            UserBag newItem = new UserBag();
            newItem.setPlayerId(player.getId().longValue());
            newItem.setPropId(propId);
            newItem.setSums(count);
            newItem.setVary(props != null ? props.getVary() : 1);
            newItem.setSell(props != null ? props.getSell() : 0);
            newItem.setZbing(0);
            newItem.setPyb(0); newItem.setPsell(0); newItem.setPstime(0L);
            newItem.setBsum(0); newItem.setPetime(0L); newItem.setPsum(0);
            newItem.setStime(System.currentTimeMillis() / 1000);
            bagRepo.save(newItem);
        }
    }

    // ==================== Build Task Map ====================

    private Map<String, Object> buildTaskMap(TaskDef t, Map<Long, TaskAccept> acceptMap,
                                              Set<String> completed, int playerLv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("fromnpc", t.getFromnpc());
        m.put("frommsg", t.getFrommsg());
        m.put("okneed", t.getOkneed());
        m.put("okneedDesc", resolveNames(t.getOkneed()));
        m.put("result", t.getResult());
        m.put("resultDesc", resolveNames(t.getResult()));
        m.put("limitlv", t.getLimitlv());
            m.put("limitlvDesc", resolveLimitlvNames(t.getLimitlv()));
        m.put("color", t.getColor() != null ? t.getColor() : 0);
        m.put("xulie", t.getXulie());
        m.put("oknpc", t.getOknpc());

        TaskAccept accepted = acceptMap.get(t.getId());
        m.put("accepted", accepted != null);
        if (accepted != null) {
            m.put("acceptId", accepted.getId());
            m.put("state", accepted.getState());
            if (accepted.getState() != null && accepted.getState().contains("killmon:")) {
                m.put("killProgress", parseKillProgress(accepted.getState()));
            }
        }
        return m;
    }

    // ==================== Name Resolution for Display ====================

    private String resolveLimitlvNames(String limitlv) {
        if (limitlv == null || limitlv.isEmpty() || "0".equals(limitlv)) return "无限制";
        StringBuilder sb = new StringBuilder();
        for (String part : limitlv.split(",")) {
            if (part.trim().isEmpty()) continue;
            String[] kv = part.split(":");
            String key = kv[0];
            switch (key) {
                case "lv" -> {
                    if (kv.length >= 2) {
                        String[] range = kv[1].split("\\|");
                        int min = Integer.parseInt(range[0]);
                        int max = range.length > 1 ? Integer.parseInt(range[1]) : 0;
                        if (max == 0) sb.append("等级≥").append(min);
                        else sb.append("等级").append(min).append("-").append(max);
                    }
                }
                case "level" -> {
                    if (kv.length >= 2) {
                        String[] range = kv[1].split("\\|");
                        int min = Integer.parseInt(range[0]);
                        int max = range.length > 1 ? Integer.parseInt(range[1]) : 0;
                        if (max == 0) sb.append("等级≥").append(min);
                        else sb.append("等级").append(min).append("-").append(max);
                    }
                }
                case "wx" -> {
                    if (kv.length >= 2) {
                        String[] wxs = {"?", "金", "木", "水", "火", "土", "神", "神圣"};
                        String[] ids = kv[1].split("\\|");
                        List<String> names = new ArrayList<>();
                        for (String id : ids) {
                            int idx = Integer.parseInt(id);
                            names.add(idx < wxs.length ? wxs[idx] : "五行#" + id);
                        }
                        sb.append("五行:").append(String.join("/", names));
                    }
                }
                case "czl" -> {
                    if (kv.length >= 2) {
                        String[] range = kv[1].split("\\|");
                        double min = Double.parseDouble(range[0]);
                        double max = range.length > 1 ? Double.parseDouble(range[1]) : 0;
                        if (max == 0) sb.append("成长≥").append(min);
                        else sb.append("成长").append(min).append("-").append(max);
                    }
                }
                case "cz" -> {
                    if (kv.length >= 2) {
                        String[] range = kv[1].split("\\|");
                        double min = Double.parseDouble(range[0]);
                        double max = range.length > 1 ? Double.parseDouble(range[1]) : 0;
                        if (max == 0) sb.append("成长≥").append(min);
                        else sb.append("成长").append(min).append("-").append(max);
                    }
                }
                case "comself" -> {
                    if (kv.length >= 2) {
                        String[] ids = kv[1].split("\\|");
                        List<String> names = new ArrayList<>();
                        for (String id : ids) {
                            Pet pet = petRepo.findById(Long.parseLong(id)).orElse(null);
                            names.add(pet != null ? pet.getName() : ("宠物#" + id));
                        }
                        sb.append("主宠:").append(String.join("/", names));
                    }
                }
                case "jifen" -> sb.append("积分≥").append(kv.length >= 2 ? kv[1] : "?");
                case "vip" -> sb.append("VIP≥").append(kv.length >= 2 ? kv[1] : "?");
                case "merge" -> sb.append("需要结婚");
                case "cishu" -> {
                    if (kv.length >= 3) {
                        sb.append(kv[2]).append("天内限").append(kv[1]).append("次");
                    }
                }
                case "timelimit" -> sb.append(kv.length >= 2 ? kv[1] + "小时内完成" : "限时完成");
                case "paihang" -> sb.append("排行榜第").append(kv.length >= 2 ? kv[1] : "?").append("名");
                case "xfsj" -> sb.append("需要消费记录");
                case "xfyb" -> sb.append("需要消费元宝");
                default -> sb.append(part);
            }
            sb.append("，");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private String resolveNames(String raw) {
        if (raw == null || raw.isEmpty() || "0".equals(raw)) return "无";
        StringBuilder sb = new StringBuilder();
        for (String part : raw.split(",")) {
            if (part.trim().isEmpty() || "no:1".equals(part.trim())) {
                if ("no:1".equals(part.trim())) sb.append(" 或 ");
                continue;
            }
            String[] kv = part.split(":");
            String type = kv[0];
            switch (type) {
                case "props" -> {
                    if (kv.length >= 2) {
                        String name = resolvePropNames(kv[1]);
                        int cnt = kv.length >= 3 ? Integer.parseInt(kv[2]) : 1;
                        sb.append("获得 ").append(name).append(" x").append(cnt);
                    }
                }
                case "bprops" -> {
                    if (kv.length >= 2) {
                        String name = resolvePropNames(kv[1]);
                        int cnt = kv.length >= 3 ? Integer.parseInt(kv[2]) : 1;
                        sb.append("获得 ").append(name).append("(绑) x").append(cnt);
                    }
                }
                case "exp" -> {
                    int amt = Integer.parseInt(kv[1]);
                    sb.append("经验 +").append(amt);
                    if (kv.length >= 3) sb.append(" (威望" + kv[2].replace("|", "~") + ")");
                }
                case "money" -> {
                    int amt = Integer.parseInt(kv[1]);
                    sb.append("金币 +").append(amt);
                    if (kv.length >= 3) sb.append(" (威望" + kv[2].replace("|", "~") + ")");
                }
                case "addmoney" -> sb.append("金币 +").append(kv.length >= 2 ? kv[1] : "?");
                case "addyb" -> sb.append("元宝 +").append(kv.length >= 2 ? kv[1] : "?");
                case "addsj" -> sb.append("水晶 +").append(kv.length >= 2 ? kv[1] : "?");
                case "see" -> sb.append("拜访NPC").append(kv.length >= 2 ? kv[1] : "?");
                case "giveitem" -> {
                    if (kv.length >= 2) {
                        String name = resolvePropNames(kv[1]);
                        int cnt = kv.length >= 3 ? Integer.parseInt(kv[2]) : 1;
                        sb.append("提交 ").append(name).append(" x").append(cnt);
                    }
                }
                case "killmon" -> {
                    if (kv.length >= 2) {
                        String[] mids = kv[1].split("\\|");
                        List<String> names = new ArrayList<>();
                        for (int i = 0; i < Math.min(mids.length, 3); i++) {
                            try {
                                Monster mon = monsterRepo.findById(Long.parseLong(mids[i])).orElse(null);
                                names.add(mon != null ? mon.getName() : ("#" + mids[i]));
                            } catch (NumberFormatException e) { names.add(mids[i]); }
                        }
                        if (mids.length > 3) names.add("等" + mids.length + "种");
                        sb.append("击杀 ").append(String.join("/", names));
                        if (kv.length >= 3) sb.append(" x").append(kv[2]);
                    }
                }
                case "givevip" -> sb.append("VIP≥").append(kv.length >= 2 ? kv[1] : "?");
                case "givejifen" -> sb.append("积分≥").append(kv.length >= 2 ? kv[1] : "?");
                case "giveww" -> sb.append("威望≥").append(kv.length >= 2 ? kv[1] : "?");
                case "givemoney" -> sb.append("金币≥").append(kv.length >= 2 ? kv[1] : "?");
                case "givedianjuan" -> sb.append("点卷≥").append(kv.length >= 2 ? kv[1] : "?");
                case "giveml" -> sb.append("魅力≥").append(kv.length >= 2 ? kv[1] : "?");
                case "monself" -> {
                    if (kv.length >= 2) {
                        String[] pids = kv[1].split("\\|");
                        List<String> pnames = new ArrayList<>();
                        for (String pid : pids) {
                            Pet pet = petRepo.findById(Long.parseLong(pid)).orElse(null);
                            pnames.add(pet != null ? pet.getName() : ("#" + pid));
                        }
                        sb.append("主宠为").append(String.join("/", pnames));
                    }
                }
                case "lv" -> {
                    if (kv.length >= 2) {
                        String[] range = kv[1].split("\\|");
                        if (range.length > 1 && !"0".equals(range[1]))
                            sb.append("等级").append(range[0]).append("-").append(range[1]);
                        else sb.append("等级≥").append(range[0]);
                    }
                }
                case "wx" -> {
                    if (kv.length >= 2) {
                        String[] wxs = kv[1].split("\\|");
                        sb.append("五行").append(String.join("/", wxs));
                    }
                }
                case "zx" -> sb.append("在线≥").append(kv.length >= 2 ? kv[1] : "?").append("小时");
                case "itemrand" -> sb.append("随机道具");
                case "lvprops" -> sb.append("等级道具");
                case "gonggao" -> sb.append(" [公告]");
                case "paihang" -> sb.append(" [排行重置]");
                case "fksj" -> sb.append(" [消费反馈]");
                default -> sb.append(part);
            }
            sb.append("、");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private String resolvePropNames(String idsStr) {
        String[] ids = idsStr.split("\\|");
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            Props p = propsRepo.findById(Long.parseLong(id)).orElse(null);
            names.add(p != null ? p.getName() : ("道具#" + id));
        }
        return String.join("/", names);
    }
}
