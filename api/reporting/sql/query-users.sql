-- Fetch all users from this environment who are not


CREATE VIEW vw_researcher AS select u.user_id, u.email AS username, u.given_name AS first_name, u.family_name AS last_name,
       u.creation_time, uvia.institution_id, uvia.institutional_role_enum, uvia.institutional_role_other_text
from user AS u
LEFT OUTER JOIN  user_verified_institutional_affiliation uvia on u.user_id = uvia.user_id
LEFT OUTER JOIN institution i on uvia.institution_id = i.institution_id
where disabled = false AND family_name NOT IN ('w123', 'lastname123', 'Puppeteerdriver')
ORDER BY u.email;
