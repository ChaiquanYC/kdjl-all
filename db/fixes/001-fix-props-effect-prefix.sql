-- Fix: double giveitems prefix in chest effect field
-- Bug: 45级礼包 (id=2038) had "giveitems:giveitems:" causing Java parsing to fail
-- PHP str_replace() handled it fine, Java substring+indexOf() missed the second prefix
-- Date: 2026-05-26

UPDATE props
SET effect = REPLACE(effect, 'giveitems:giveitems:', 'giveitems:')
WHERE effect LIKE 'giveitems:giveitems:%';

-- Verify
SELECT id, name, effect FROM props WHERE id = 2038;
-- Expected: giveitems:1463:2,1217:10,1225:5,2029:2,2037:1,2780:5
