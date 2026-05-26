package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_ext")
public class PlayerExt {

    @Id
    @Column(name = "uid")
    private Integer playerId;

    @Column(name = "bbshow")
    private Integer petShow;

    @Column(name = "hecheng_nums")
    private Integer mergeCount;

    @Column(name = "onlinetime")
    private Integer onlineTime;

    @Column(name = "logintime")
    private Integer loginTime;

    @Column
    private Integer sj;

    @Column
    private Integer paisj;

    @Column(name = "active_lastvtime")
    private Integer activeLastTime;

    @Column
    private Integer ml;

    @Column(name = "`merge`")
    private Integer merge;

    @Column(name = "request_merge")
    private Integer requestMerge;

    @Column
    private Integer request;

    @Column
    private Integer tgt;

    @Column
    private Integer tgttime;

    @Column
    private Integer tglasttime;

    @Column
    private Integer nomergetime;

    @Column(length = 22)
    private String send;

    @Column(name = "chchengbb")
    private Integer evolvePetId;

    @Column(name = "get_welfare_time", length = 8)
    private String welfareTime;

    @Column(name = "guild_request")
    private Integer guildRequest;

    @Column(name = "team_auto_times")
    private Integer teamAutoTimes;

    @Column(name = "tiaozhan")
    private Integer tiaozhan;

    @Column(name = "new_guide_step")
    private Integer newGuideStep;

    @Column(name = "consumption2exp_day", length = 8)
    private String consumptionDay;

    @Column(name = "reg_add_str", length = 80)
    private String regAddStr;

    @Column(name = "czl_ss")
    private Integer czlSs;

    @Column(name = "exp_got_step")
    private Integer expGotStep;

    @Column(name = "prize_every_day", length = 30)
    private String dailyPrize;

    @Column(name = "onlinetime_today")
    private Integer onlineTimeToday;

    @Column(name = "last_onlinetime")
    private Integer lastOnlineTime;

    @Column(name = "last_online_day")
    private Integer lastOnlineDay;

    @Column(name = "chouqu_chongwu", columnDefinition = "TEXT")
    private String chouquChongwu;

    public Integer getPlayerId() { return playerId; }
    public Integer getPetShow() { return petShow; }
    public Integer getMergeCount() { return mergeCount; }
    public void setMergeCount(Integer mergeCount) { this.mergeCount = mergeCount; }
    public Integer getOnlineTime() { return onlineTime; }
    public Integer getLoginTime() { return loginTime; }
    public Integer getSj() { return sj; }
    public Integer getPaisj() { return paisj; }
    public Integer getActiveLastTime() { return activeLastTime; }
    public Integer getMl() { return ml; }
    public Integer getMerge() { return merge; }
    public Integer getRequestMerge() { return requestMerge; }
    public Integer getRequest() { return request; }
    public Integer getTgt() { return tgt; }
    public void setTgt(Integer tgt) { this.tgt = tgt; }
    public Integer getTgttime() { return tgttime; }
    public void setTgttime(Integer tgttime) { this.tgttime = tgttime; }
    public Integer getTglasttime() { return tglasttime; }
    public Integer getNomergetime() { return nomergetime; }
    public String getSend() { return send; }
    public Integer getEvolvePetId() { return evolvePetId; }
    public String getWelfareTime() { return welfareTime; }
    public Integer getGuildRequest() { return guildRequest; }
    public Integer getTeamAutoTimes() { return teamAutoTimes; }
    public Integer getTiaozhan() { return tiaozhan; }
    public void setTiaozhan(Integer tiaozhan) { this.tiaozhan = tiaozhan; }
    public Integer getNewGuideStep() { return newGuideStep; }
    public String getConsumptionDay() { return consumptionDay; }
    public String getRegAddStr() { return regAddStr; }
    public Integer getCzlSs() { return czlSs; }
    public void setCzlSs(Integer czlSs) { this.czlSs = czlSs; }
    public Integer getExpGotStep() { return expGotStep; }
    public String getDailyPrize() { return dailyPrize; }
    public Integer getOnlineTimeToday() { return onlineTimeToday; }
    public Integer getLastOnlineTime() { return lastOnlineTime; }
    public Integer getLastOnlineDay() { return lastOnlineDay; }
    public void setPlayerId(Integer playerId) { this.playerId = playerId; }
    public void setSj(Integer sj) { this.sj = sj; }
    public void setPetShow(Integer petShow) { this.petShow = petShow; }
    public void setMerge(Integer merge) { this.merge = merge; }
    public void setMl(Integer ml) { this.ml = ml; }
    public void setRequestMerge(Integer requestMerge) { this.requestMerge = requestMerge; }
    public void setRequest(Integer request) { this.request = request; }
    public void setNomergetime(Integer nomergetime) { this.nomergetime = nomergetime; }
    public void setSend(String send) { this.send = send; }
    public void setExpGotStep(Integer expGotStep) { this.expGotStep = expGotStep; }
    public void setLastOnlineDay(Integer lastOnlineDay) { this.lastOnlineDay = lastOnlineDay; }
    public void setOnlineTimeToday(Integer onlineTimeToday) { this.onlineTimeToday = onlineTimeToday; }
    public void setLastOnlineTime(Integer lastOnlineTime) { this.lastOnlineTime = lastOnlineTime; }
    public void setOnlineTime(Integer onlineTime) { this.onlineTime = onlineTime; }
    public void setTeamAutoTimes(Integer teamAutoTimes) { this.teamAutoTimes = teamAutoTimes; }

    public String getChouquChongwu() { return chouquChongwu; }
    public void setChouquChongwu(String chouquChongwu) { this.chouquChongwu = chouquChongwu; }
}
