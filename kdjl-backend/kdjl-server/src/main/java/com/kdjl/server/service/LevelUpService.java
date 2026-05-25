package com.kdjl.server.service;

import com.kdjl.common.entity.*;
import com.kdjl.server.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class LevelUpService {
    private static final Logger log = LoggerFactory.getLogger(LevelUpService.class);
    private static final int MAX_LEVEL = 130;

    private final UserPetRepository userPetRepo;
    private final WxRepository wxRepo;
    private final ExpToLvRepository expToLvRepo;
    private final PlayerRepository playerRepo;

    public LevelUpService(UserPetRepository userPetRepo, WxRepository wxRepo,
                          ExpToLvRepository expToLvRepo, PlayerRepository playerRepo) {
        this.userPetRepo = userPetRepo;
        this.wxRepo = wxRepo;
        this.expToLvRepo = expToLvRepo;
        this.playerRepo = playerRepo;
    }

    /** Generate growth rate from bb.czl range string like "1.0,1.3" */
    public double generateCzl(String czlRange) {
        if (czlRange == null || czlRange.isEmpty()) return 1.0;
        String cleaned = czlRange.replace(".", "");
        String[] parts = cleaned.split(",");
        if (parts.length != 2) return 1.0;
        try {
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            return ThreadLocalRandom.current().nextInt(min, max + 1) / 10.0;
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }

    /**
     * Apply experience to a pet. Handles level-ups including recursive multi-level.
     * Matches PHP saveGetOther() logic.
     * @return true if at least one level-up occurred
     */
    @Transactional
    public boolean addExp(UserPet pet, long expGained) {
        if (pet.getLevel() != null && pet.getLevel() >= MAX_LEVEL) return false;
        return addExpInternal(pet, expGained);
    }

    private boolean addExpInternal(UserPet pet, long expGained) {
        if (pet.getLevel() != null && pet.getLevel() >= MAX_LEVEL) return false;

        long currentExp = pet.getNowexp() != null ? pet.getNowexp() : 0;
        long neededExp = pet.getLexp() != null ? pet.getLexp() : 100;
        long totalExp = currentExp + expGained;

        if (totalExp < neededExp) {
            pet.setNowexp(totalExp);
            userPetRepo.save(pet);
            return false;
        }

        // Level up!
        long overflow = totalExp - neededExp;
        doLevelUp(pet);
        userPetRepo.save(pet);

        // Recursive: apply overflow exp (PHP task.v1.php pattern)
        if (overflow > 0 && (pet.getLevel() == null || pet.getLevel() < MAX_LEVEL)) {
            // Re-read fresh pet from DB
            UserPet fresh = userPetRepo.findById(pet.getId()).orElse(null);
            if (fresh != null) {
                return addExpInternal(fresh, overflow);
            }
        }
        return true;
    }

    private void doLevelUp(UserPet pet) {
        int newLevel = (pet.getLevel() != null ? pet.getLevel() : 1) + 1;
        pet.setLevel(newLevel);

        // Get wx coefficients for this pet's element
        Wx wxRow = null;
        if (pet.getWx() != null) {
            wxRow = wxRepo.findByWx(pet.getWx());
        }
        if (wxRow == null) {
            // Fallback: use neutral growth
            pet.setSrchp((pet.getSrchp() != null ? pet.getSrchp() : 100) + 10);
            pet.setSrcmp((pet.getSrcmp() != null ? pet.getSrcmp() : 50) + 3);
            pet.setAc((pet.getAc() != null ? pet.getAc() : 10) + 2);
            pet.setMc((pet.getMc() != null ? pet.getMc() : 10) + 1);
            pet.setHits((pet.getHits() != null ? pet.getHits() : 100) + 2);
            pet.setMiss((pet.getMiss() != null ? pet.getMiss() : 0) + 1);
            pet.setSpeed((pet.getSpeed() != null ? pet.getSpeed() : 10) + 1);
            pet.setHp(pet.getSrchp());
            pet.setMp(pet.getSrcmp());
        } else {
            double czl = parseCzl(pet.getCzl());
            long curSrchp = pet.getSrchp() != null ? pet.getSrchp() : 100;
            long curSrcmp = pet.getSrcmp() != null ? pet.getSrcmp() : 50;
            long curAc = pet.getAc() != null ? pet.getAc() : 10;
            long curMc = pet.getMc() != null ? pet.getMc() : 10;
            long curHits = pet.getHits() != null ? pet.getHits() : 100;
            long curMiss = pet.getMiss() != null ? pet.getMiss() : 0;
            long curSpeed = pet.getSpeed() != null ? pet.getSpeed() : 10;

            // Formula: newStat = int(wxCoeff * czl) + currentStat
            pet.setSrchp(curSrchp + (long)(wxRow.getHp() * czl));
            pet.setSrcmp(curSrcmp + (long)(wxRow.getMp() * czl));
            pet.setAc(curAc + (long)(wxRow.getAc() * czl));
            pet.setMc(curMc + (long)(wxRow.getMc() * czl));
            pet.setHits(curHits + (long)(wxRow.getHits() * czl));
            pet.setMiss(curMiss + (long)(wxRow.getMiss() * czl));
            pet.setSpeed(curSpeed + (long)(wxRow.getSpeed() * czl));

            // Reset HP/MP to new max
            pet.setHp(pet.getSrchp());
            pet.setMp(pet.getSrcmp());

            // Update 5-element resistances (kx field)
            String kx = pet.getKx();
            if (kx != null && !kx.isEmpty()) {
                String[] resist = kx.split(",");
                if (resist.length >= 5) {
                    try {
                        int jk = (int)(wxRow.getJ() * czl) + Integer.parseInt(resist[0].trim());
                        int mk = (int)(wxRow.getM() * czl) + Integer.parseInt(resist[1].trim());
                        int sk = (int)(wxRow.getS() * czl) + Integer.parseInt(resist[2].trim());
                        int hk = (int)(wxRow.getH() * czl) + Integer.parseInt(resist[3].trim());
                        int tk = (int)(wxRow.getT() * czl) + Integer.parseInt(resist[4].trim());
                        pet.setKx(jk + "," + mk + "," + sk + "," + hk + "," + tk);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Get EXP required for next level
        ExpToLv expRow = expToLvRepo.findByLevel(newLevel);
        if (expRow != null) {
            pet.setLexp(expRow.getNextLevelExp());
        } else {
            pet.setLexp(100L + newLevel * 20L); // fallback
        }
        pet.setNowexp(0L);
    }

    private double parseCzl(String czl) {
        if (czl == null || czl.isEmpty()) return 1.0;
        try { return Double.parseDouble(czl); }
        catch (NumberFormatException e) { return 1.0; }
    }

    /** Calculate double EXP multiplier based on player's dblexpflag */
    public double getExpMultiplier(Player player) {
        if (player == null || player.getDblExpFlag() == null || player.getDblExpFlag() <= 1) {
            return 1.0;
        }
        // Check if double-exp time has expired
        if (player.getDblsTime() != null && player.getMaxDblExpTime() != null) {
            long now = System.currentTimeMillis() / 1000;
            if (player.getDblsTime() + player.getMaxDblExpTime() <= now) {
                return 1.0; // expired
            }
        }
        return switch (player.getDblExpFlag()) {
            case 2 -> 1.5; case 3 -> 2.0; case 4 -> 2.5; case 5 -> 3.0;
            default -> 1.0;
        };
    }
}
