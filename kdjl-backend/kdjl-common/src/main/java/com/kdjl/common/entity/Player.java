package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 255)
    private String username;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "nickname", length = 255)
    private String nickname;

    @Column(length = 255)
    private String ip;

    @Column
    private Integer vip;

    @Column
    private Integer money;

    @Column
    private Integer yb;

    @Column(name = "score")
    private Integer score;

    @Column(name = "prestige")
    private Integer prestige;

    @Column(name = "active_score")
    private Integer activeScore;

    @Column(name = "regtime")
    private Integer regtime;

    @Column(name = "lastvtime")
    private Integer lastVisitTime;

    @Column(name = "headimg")
    private Integer headImg;

    @Column(name = "openmap", length = 255)
    private String openMap;

    @Column(name = "inmap")
    private Integer inMap;

    @Column(name = "fighttop")
    private Integer fightTop;

    @Column(name = "maxbag")
    private Integer maxBag;

    @Column(name = "sex", length = 255)
    private String sex;

    @Column(name = "mbid")
    private Integer mbid;

    @Column(name = "fightbb")
    private Integer fightBb;

    @Column(name = "fieldpwd", length = 255)
    private String fieldPwd;

    @Column(name = "tgtime")
    private Integer tgTime;

    @Column(name = "tgmax")
    private Integer tgMax;

    @Column(name = "ckpwd", length = 255)
    private String ckPwd;

    @Column(name = "maxbase")
    private Integer maxBase;

    @Column(name = "useyb")
    private Integer useYb;

    @Column(name = "active_useyb")
    private Integer activeUseYb;

    @Column(name = "mapinfo", length = 255)
    private String mapInfo;

    @Column(name = "paimoney")
    private Integer paiMoney;

    @Column(name = "bot_map_id")
    private Integer botMapId;

    @Column(name = "paihang")
    private Integer paiHang;

    @Column(name = "dblexpflag")
    private Integer dblExpFlag;

    @Column(name = "autofitflag")
    private Integer autoFitFlag;

    @Column(name = "tasklog", length = 255)
    private String taskLog;

    @Column(name = "friendlist", length = 255)
    private String friendList;

    @Column(name = "secret", length = 255)
    private String secret;

    @Column(name = "secid")
    private Integer secId;

    @Column(name = "task", length = 255)
    private String task;

    @Column(name = "sysautosum")
    private Integer sysAutoSum;

    @Column(name = "sysautotime")
    private Integer sysAutoTime;

    @Column(name = "maxautofitsum")
    private Integer maxAutoFitSum;

    @Column(name = "maxdblexptime")
    private Integer maxDblExpTime;

    @Column(name = "maxmc")
    private Integer maxMc;

    @Column(name = "dblstime")
    private Integer dblsTime;

    @Column(name = "jprestige")
    private Integer jPrestige;

    @Column(name = "viplast")
    private Integer vipLast;

    @Column(name = "vipyb")
    private Integer vipYb;

    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
    public String getIp() { return ip; }
    public Integer getVip() { return vip; }
    public Integer getMoney() { return money; }
    public Integer getYb() { return yb; }
    public Integer getScore() { return score; }
    public Integer getPrestige() { return prestige; }
    public Integer getActiveScore() { return activeScore; }
    public Integer getRegtime() { return regtime; }
    public Integer getLastVisitTime() { return lastVisitTime; }
    public Integer getHeadImg() { return headImg; }
    public String getOpenMap() { return openMap; }
    public Integer getInMap() { return inMap; }
    public Integer getFightTop() { return fightTop; }
    public Integer getMaxBag() { return maxBag; }
    public String getSex() { return sex; }
    public Integer getMbid() { return mbid; }
    public Integer getFightBb() { return fightBb; }
    public String getFieldPwd() { return fieldPwd; }
    public Integer getTgTime() { return tgTime; }
    public void setTgTime(Integer tgTime) { this.tgTime = tgTime; }
    public Integer getTgMax() { return tgMax; }
    public void setTgMax(Integer tgMax) { this.tgMax = tgMax; }
    public String getCkPwd() { return ckPwd; }
    public Integer getMaxBase() { return maxBase; }
    public Integer getUseYb() { return useYb; }
    public Integer getActiveUseYb() { return activeUseYb; }
    public String getMapInfo() { return mapInfo; }
    public Integer getPaiMoney() { return paiMoney; }
    public Integer getBotMapId() { return botMapId; }
    public Integer getPaiHang() { return paiHang; }
    public Integer getDblExpFlag() { return dblExpFlag; }
    public Integer getAutoFitFlag() { return autoFitFlag; }
    public String getTaskLog() { return taskLog; }
    public String getFriendList() { return friendList; }
    public void setFriendList(String friendList) { this.friendList = friendList; }
    public String getSecret() { return secret; }
    public Integer getSecId() { return secId; }
    public void setSecId(Integer secId) { this.secId = secId; }
    public String getTask() { return task; }
    public Integer getSysAutoSum() { return sysAutoSum; }
    public Integer getSysAutoTime() { return sysAutoTime; }
    public Integer getMaxAutoFitSum() { return maxAutoFitSum; }
    public Integer getMaxDblExpTime() { return maxDblExpTime; }
    public Integer getMaxMc() { return maxMc; }
    public Integer getDblsTime() { return dblsTime; }
    public Integer getJPrestige() { return jPrestige; }
    public Integer getVipLast() { return vipLast; }
    public Integer getVipYb() { return vipYb; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setSecret(String secret) { this.secret = secret; }
    public void setVip(Integer vip) { this.vip = vip; }
    public void setScore(Integer score) { this.score = score; }
    public void setPrestige(Integer prestige) { this.prestige = prestige; }
    public void setMaxBag(Integer maxBag) { this.maxBag = maxBag; }
    public void setMaxBase(Integer maxBase) { this.maxBase = maxBase; }
    public void setMaxMc(Integer maxMc) { this.maxMc = maxMc; }
    public void setOpenMap(String openMap) { this.openMap = openMap; }
    public void setMoney(Integer money) { this.money = money; }
    public void setYb(Integer yb) { this.yb = yb; }
    public void setInMap(Integer inMap) { this.inMap = inMap; }
    public void setSex(String sex) { this.sex = sex; }
    public void setRegtime(Integer regtime) { this.regtime = regtime; }
    public void setLastVisitTime(Integer lastVisitTime) { this.lastVisitTime = lastVisitTime; }
    public void setMbid(Integer mbid) { this.mbid = mbid; }
    public void setFightBb(Integer fightBb) { this.fightBb = fightBb; }
    public void setTask(String task) { this.task = task; }
    public void setTaskLog(String taskLog) { this.taskLog = taskLog; }
    public void setAutoFitFlag(Integer autoFitFlag) { this.autoFitFlag = autoFitFlag; }
    public void setSysAutoSum(Integer sysAutoSum) { this.sysAutoSum = sysAutoSum; }
    public void setMaxAutoFitSum(Integer maxAutoFitSum) { this.maxAutoFitSum = maxAutoFitSum; }
    public void setDblExpFlag(Integer dblExpFlag) { this.dblExpFlag = dblExpFlag; }
    public void setDblsTime(Integer dblsTime) { this.dblsTime = dblsTime; }
    public void setMaxDblExpTime(Integer maxDblExpTime) { this.maxDblExpTime = maxDblExpTime; }
    public void setActiveScore(Integer activeScore) { this.activeScore = activeScore; }
    public void setJPrestige(Integer jPrestige) { this.jPrestige = jPrestige; }
}
