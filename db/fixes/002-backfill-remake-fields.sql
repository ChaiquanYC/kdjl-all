-- Fix: pets created via egg/capture missing evolution chain fields
-- Bug: BagService.openpet and PetService.createPetFromCapture didn't copy
--       remakelevel/remakeid/remakepid from bb template, so Pet Shrine showed "不可进化"
-- Code fix: BagService.java and PetService.java now copy these fields on creation
-- This script backfills existing pets that have NULL/empty evolution fields
-- Date: 2026-05-26

UPDATE userbb u
JOIN bb b ON u.name = b.name
SET u.remakelevel = b.remakelevel,
    u.remakeid   = b.remakeid,
    u.remakepid  = b.remakepid
WHERE u.remakelevel IS NULL OR u.remakelevel = '';

-- Verify: should return 0 rows
SELECT id, name, uid, remakelevel, remakeid, remakepid
FROM userbb
WHERE remakelevel IS NULL OR remakelevel = '';
