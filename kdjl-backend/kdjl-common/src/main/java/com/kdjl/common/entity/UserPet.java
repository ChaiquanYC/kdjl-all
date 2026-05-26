package com.kdjl.common.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "userbb")
public class UserPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String name;

    @Column(name = "uid")
    private Long playerId;

    @Column(length = 20)
    private String username;

    @Column
    private Integer level;

    @Column
    private Integer wx; // element: 金1木2水3火4土5

    @Column
    private Long ac; // physical attack

    @Column
    private Long mc; // magic attack

    @Column
    private Long srchp; // base HP

    @Column
    private Long addhp; // bonus HP

    @Column
    private Long hp; // current HP

    @Column
    private Long mp;

    @Column
    private Long srcmp;

    @Column
    private Long addmp;

    @Column(name = "skillist", length = 255)
    private String skillList;

    @Column
    private Long stime;

    @Column
    private Long nowexp;

    @Column
    private Long lexp; // exp needed for next level

    @Column(length = 50)
    private String imgstand;

    @Column(length = 50)
    private String imgack;

    @Column(length = 50)
    private String imgdie;

    @Column(length = 15)
    private String headimg;

    @Column(length = 15)
    private String cardimg;

    @Column(length = 15)
    private String effectimg;

    @Column
    private Long hits;

    @Column
    private Long miss;

    @Column
    private Long speed;

    @Column
    private Integer subyl; // element growth

    @Column
    private Integer subsl;

    @Column
    private Integer subxl;

    @Column
    private Integer subdl;

    @Column
    private Integer subfl;

    @Column
    private Integer subhl;

    @Column
    private Integer subkl;

    @Column(name = "kx", length = 255)
    private String kx; // 5-element resistances: "j,m,s,h,t"

    @Column(name = "czl", length = 255)
    private String czl;

    @Column(length = 100)
    private String zb; // equipment: "pos:bagId,pos:bagId"

    @Column(name = "muchang")
    private Integer muchang; // 1=normal 3=joined 4=paired 5=waiting 6=breeding 7=done
    @Column private Long chchengbb;
    @Column(name = "chchengtime") private Long chchengtime;
    @Column(name = "chchengsx", length = 255) private String chchengsx;
    @Column(name = "chchengwp", length = 50) private String chchengwp;
    @Column(name = "chchengcolor", length = 10) private String chchengcolor;
    @Column(name = "chchengcz", length = 50) private String chchengcz;

    @Column(name = "remakelevel", length = 30)
    private String remakelevel;

    @Column(name = "remakeid", length = 30)
    private String remakeid;

    @Column(name = "remakepid", length = 30)
    private String remakepid;

    @Column(name = "remaketimes")
    private Integer remaketimes;

    @Column(name = "old_bid")
    private Integer oldBid;

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getPlayerId() { return playerId; }
    public String getUsername() { return username; }
    public Integer getLevel() { return level; }
    public Integer getWx() { return wx; }
    public Long getAc() { return ac; }
    public Long getMc() { return mc; }
    public Long getSrchp() { return srchp; }
    public Long getAddhp() { return addhp; }
    public Long getHp() { return hp; }
    public Long getMp() { return mp; }
    public Long getSrcmp() { return srcmp; }
    public Long getAddmp() { return addmp; }
    public String getSkillList() { return skillList; }
    public Long getStime() { return stime; }
    public Long getNowexp() { return nowexp; }
    public Long getLexp() { return lexp; }
    public String getImgstand() { return imgstand; }
    public String getImgack() { return imgack; }
    public String getImgdie() { return imgdie; }
    public String getHeadimg() { return headimg; }
    public String getCardimg() { return cardimg; }
    public String getEffectimg() { return effectimg; }
    public Long getHits() { return hits; }
    public Long getMiss() { return miss; }
    public Long getSpeed() { return speed; }
    public Integer getSubyl() { return subyl; }
    public Integer getSubsl() { return subsl; }
    public Integer getSubxl() { return subxl; }
    public Integer getSubdl() { return subdl; }
    public Integer getSubfl() { return subfl; }
    public Integer getSubhl() { return subhl; }
    public Integer getSubkl() { return subkl; }
    public String getKx() { return kx; }
    public void setKx(String kx) { this.kx = kx; }
    public String getCzl() { return czl; }
    public String getZb() { return zb; }
    public void setZb(String zb) { this.zb = zb; }

    public Integer getMuchang() { return muchang; }
    public void setMuchang(Integer muchang) { this.muchang = muchang; }
    public Long getChchengbb() { return chchengbb; }
    public void setChchengbb(Long chchengbb) { this.chchengbb = chchengbb; }
    public Long getChchengtime() { return chchengtime; }
    public void setChchengtime(Long chchengtime) { this.chchengtime = chchengtime; }
    public String getChchengsx() { return chchengsx; }
    public void setChchengsx(String chchengsx) { this.chchengsx = chchengsx; }
    public String getChchengwp() { return chchengwp; }
    public void setChchengwp(String chchengwp) { this.chchengwp = chchengwp; }
    public String getChchengcolor() { return chchengcolor; }
    public void setChchengcolor(String chchengcolor) { this.chchengcolor = chchengcolor; }
    public String getChchengcz() { return chchengcz; }
    public void setChchengcz(String chchengcz) { this.chchengcz = chchengcz; }

    public void setHp(Long hp) { this.hp = hp; }
    public void setMp(Long mp) { this.mp = mp; }
    public void setNowexp(Long nowexp) { this.nowexp = nowexp; }
    public void setLevel(Integer level) { this.level = level; }
    public void setAc(Long ac) { this.ac = ac; }
    public void setMc(Long mc) { this.mc = mc; }
    public void setSpeed(Long speed) { this.speed = speed; }
    public void setHits(Long hits) { this.hits = hits; }
    public void setMiss(Long miss) { this.miss = miss; }
    public void setSrchp(Long srchp) { this.srchp = srchp; }
    public void setAddhp(Long addhp) { this.addhp = addhp; }
    public void setSrcmp(Long srcmp) { this.srcmp = srcmp; }
    public void setAddmp(Long addmp) { this.addmp = addmp; }
    public void setLexp(Long lexp) { this.lexp = lexp; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
    public void setUsername(String username) { this.username = username; }
    public void setName(String name) { this.name = name; }
    public void setWx(Integer wx) { this.wx = wx; }
    public void setStime(Long stime) { this.stime = stime; }
    public void setImgstand(String imgstand) { this.imgstand = imgstand; }
    public void setImgack(String imgack) { this.imgack = imgack; }
    public void setImgdie(String imgdie) { this.imgdie = imgdie; }
    public void setHeadimg(String headimg) { this.headimg = headimg; }
    public void setCardimg(String cardimg) { this.cardimg = cardimg; }
    public void setEffectimg(String effectimg) { this.effectimg = effectimg; }
    public void setSkillList(String skillList) { this.skillList = skillList; }
    public void setCzl(String czl) { this.czl = czl; }
    public void setSubyl(Integer subyl) { this.subyl = subyl; }
    public void setSubsl(Integer subsl) { this.subsl = subsl; }
    public void setSubxl(Integer subxl) { this.subxl = subxl; }
    public void setSubdl(Integer subdl) { this.subdl = subdl; }
    public void setSubfl(Integer subfl) { this.subfl = subfl; }
    public void setSubhl(Integer subhl) { this.subhl = subhl; }
    public void setSubkl(Integer subkl) { this.subkl = subkl; }

    public String getRemakelevel() { return remakelevel; }
    public void setRemakelevel(String remakelevel) { this.remakelevel = remakelevel; }
    public String getRemakeid() { return remakeid; }
    public void setRemakeid(String remakeid) { this.remakeid = remakeid; }
    public String getRemakepid() { return remakepid; }
    public void setRemakepid(String remakepid) { this.remakepid = remakepid; }
    public Integer getRemaketimes() { return remaketimes; }
    public void setRemaketimes(Integer remaketimes) { this.remaketimes = remaketimes; }
    public Integer getOldBid() { return oldBid; }
    public void setOldBid(Integer oldBid) { this.oldBid = oldBid; }
}
