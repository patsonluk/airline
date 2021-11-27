INSERT INTO user_modifier(user, modifier_name, creation) SELECT id, status, 0 FROM user WHERE status <> 'ACTIVE' AND status <> 'INACTIVE';
UPDATE user SET status='ACTIVE' WHERE status <> 'INACTIVE';