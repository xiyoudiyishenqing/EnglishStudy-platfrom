USE english_study_platform;

-- 本脚本用于已有数据库的增量升级；全量重建可直接执行 english_study_platform.sql。
-- 覆盖本次需求补齐涉及的资源权限、考试规则、考试会话、班级作业和作业提交表。
-- 使用 information_schema 判断字段是否存在，兼容不支持 ADD COLUMN IF NOT EXISTS 的 MySQL 版本。

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN column_definition_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND COLUMN_NAME = column_name_value
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `',
            table_name_value,
            '` ADD COLUMN `',
            column_name_value,
            '` ',
            column_definition_value
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_column_if_missing('resources', 'file_name', 'VARCHAR(255) NULL AFTER `type`');
CALL add_column_if_missing('resources', 'stored_name', 'VARCHAR(255) NULL AFTER `file_name`');
CALL add_column_if_missing('resources', 'content_type', 'VARCHAR(100) NULL AFTER `stored_name`');
CALL add_column_if_missing('resources', 'file_size', 'BIGINT NULL AFTER `content_type`');
CALL add_column_if_missing('resources', 'visibility', 'VARCHAR(20) NOT NULL DEFAULT ''PUBLIC'' AFTER `downloadable`');
CALL add_column_if_missing('resources', 'class_name', 'VARCHAR(100) NULL AFTER `visibility`');
CALL add_column_if_missing('resources', 'audit_status', 'VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' AFTER `class_name`');
CALL add_column_if_missing('resources', 'online_status', 'VARCHAR(20) NOT NULL DEFAULT ''PUBLISHED'' AFTER `audit_status`');

CALL add_column_if_missing('exams', 'question_count', 'INT NOT NULL DEFAULT 0 AFTER `type`');
CALL add_column_if_missing('exams', 'class_name', 'VARCHAR(100) NULL AFTER `teacher_name`');
CALL add_column_if_missing('exams', 'auto_submit', 'TINYINT(1) NOT NULL DEFAULT 1 AFTER `duration_minutes`');
CALL add_column_if_missing('exam_records', 'class_name', 'VARCHAR(100) NULL AFTER `student_name`');
CALL add_column_if_missing('exam_questions', 'exam_type', 'VARCHAR(20) NOT NULL DEFAULT ''大学四级'' AFTER `exam_id`');
CALL add_column_if_missing('exam_questions', 'question_type', 'VARCHAR(30) NOT NULL DEFAULT ''SINGLE_CHOICE'' AFTER `exam_type`');
CALL add_column_if_missing('exam_questions', 'analysis', 'TEXT NULL AFTER `correct_answer`');
CALL add_column_if_missing('exam_questions', 'audio_file_name', 'VARCHAR(255) NULL AFTER `analysis`');
CALL add_column_if_missing('exam_questions', 'audio_stored_name', 'VARCHAR(255) NULL AFTER `audio_file_name`');
CALL add_column_if_missing('exam_questions', 'audio_content_type', 'VARCHAR(100) NULL AFTER `audio_stored_name`');
CALL add_column_if_missing('exam_questions', 'audio_file_size', 'BIGINT NULL AFTER `audio_content_type`');
CALL add_column_if_missing('exam_questions', 'enabled', 'TINYINT(1) NOT NULL DEFAULT 1 AFTER `sort_order`');
CALL add_column_if_missing('qa_questions', 'class_name', 'VARCHAR(100) NULL AFTER `student_name`');
CALL add_column_if_missing('qa_questions', 'resource_id', 'BIGINT NULL AFTER `class_name`');
CALL add_column_if_missing('qa_questions', 'resource_title', 'VARCHAR(100) NULL AFTER `resource_id`');

ALTER TABLE exam_questions MODIFY COLUMN exam_id BIGINT NULL;
ALTER TABLE exam_questions MODIFY COLUMN exam_type VARCHAR(50) NOT NULL DEFAULT '大学四级';
ALTER TABLE exam_questions MODIFY COLUMN option_a VARCHAR(255) NULL;
ALTER TABLE exam_questions MODIFY COLUMN option_b VARCHAR(255) NULL;
ALTER TABLE exam_questions MODIFY COLUMN option_c VARCHAR(255) NULL;
ALTER TABLE exam_questions MODIFY COLUMN option_d VARCHAR(255) NULL;
ALTER TABLE exam_questions MODIFY COLUMN correct_answer VARCHAR(255) NOT NULL;
ALTER TABLE exam_questions MODIFY COLUMN sort_order INT NULL;

DROP PROCEDURE IF EXISTS add_column_if_missing;

CREATE TABLE IF NOT EXISTS exam_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answers_json TEXT,
    remaining_seconds INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_exam_session_student (exam_id, student_id),
    KEY idx_exam_sessions_student_id (student_id)
);

CREATE TABLE IF NOT EXISTS exam_paper_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score INT NOT NULL,
    sort_order INT NOT NULL,
    UNIQUE KEY uk_exam_paper_question (exam_id, question_id),
    KEY idx_exam_paper_questions_exam_id (exam_id),
    KEY idx_exam_paper_questions_question_id (question_id)
);

CREATE TABLE IF NOT EXISTS homeworks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    class_name VARCHAR(100) NOT NULL,
    teacher_id BIGINT NOT NULL,
    teacher_name VARCHAR(50) NOT NULL,
    due_time DATETIME,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_homeworks_teacher_id (teacher_id),
    KEY idx_homeworks_class_name (class_name)
);

CREATE TABLE IF NOT EXISTS homework_submissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    homework_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(50) NOT NULL,
    content TEXT,
    file_name VARCHAR(255),
    stored_name VARCHAR(255),
    content_type VARCHAR(100),
    file_size BIGINT,
    score INT,
    teacher_feedback TEXT,
    status VARCHAR(20) NOT NULL,
    submitted_at DATETIME,
    graded_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_homework_submission_student (homework_id, student_id),
    KEY idx_homework_submissions_student_id (student_id)
);

CREATE TABLE IF NOT EXISTS forum_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(50) NOT NULL,
    class_name VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    reply_count INT NOT NULL DEFAULT 0,
    last_reply_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_forum_posts_class_name (class_name),
    KEY idx_forum_posts_student_id (student_id),
    KEY idx_forum_posts_last_reply_at (last_reply_at)
);

CREATE TABLE IF NOT EXISTS forum_replies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_forum_replies_post_id (post_id),
    KEY idx_forum_replies_student_id (student_id)
);

UPDATE resources
SET visibility = COALESCE(NULLIF(visibility, ''), 'PUBLIC'),
    audit_status = COALESCE(NULLIF(audit_status, ''), 'APPROVED'),
    online_status = COALESCE(NULLIF(online_status, ''), 'PUBLISHED');

UPDATE users
SET class_name = '英语2301班'
WHERE role IN ('STUDENT', 'TEACHER')
  AND (class_name IS NULL OR class_name = '');

UPDATE resources r
JOIN users u ON r.creator_id = u.id
SET r.class_name = u.class_name,
    r.visibility = 'CLASS'
WHERE r.creator_role = 'TEACHER'
  AND u.class_name IS NOT NULL
  AND u.class_name <> '';

UPDATE exams e
JOIN users u ON e.teacher_id = u.id
SET e.class_name = u.class_name
WHERE (e.class_name IS NULL OR e.class_name = '')
  AND u.class_name IS NOT NULL
  AND u.class_name <> '';

UPDATE qa_questions q
JOIN users u ON q.student_id = u.id
SET q.class_name = u.class_name
WHERE (q.class_name IS NULL OR q.class_name = '')
  AND u.class_name IS NOT NULL
  AND u.class_name <> '';

UPDATE qa_questions q
JOIN resources r ON r.id = (
    SELECT r2.id
    FROM resources r2
    WHERE r2.class_name = q.class_name
       OR r2.visibility = 'PUBLIC'
    ORDER BY
        CASE WHEN r2.class_name = q.class_name THEN 0 ELSE 1 END,
        r2.id
    LIMIT 1
)
SET q.resource_id = r.id,
    q.resource_title = r.title,
    q.course_title = COALESCE(NULLIF(q.course_title, ''), r.title)
WHERE q.resource_id IS NULL;

UPDATE exam_records r
JOIN users u ON r.student_id = u.id
SET r.class_name = u.class_name
WHERE (r.class_name IS NULL OR r.class_name = '')
  AND u.class_name IS NOT NULL
  AND u.class_name <> '';

UPDATE exam_questions q
LEFT JOIN exams e ON q.exam_id = e.id
SET q.exam_type = COALESCE(NULLIF(q.exam_type, ''), NULLIF(e.type, ''), '大学四级'),
    q.question_type = COALESCE(NULLIF(q.question_type, ''), 'SINGLE_CHOICE'),
    q.enabled = COALESCE(q.enabled, 1);

INSERT IGNORE INTO exam_paper_questions (exam_id, question_id, score, sort_order)
SELECT q.exam_id,
       q.id,
       COALESCE(q.score, 10),
       COALESCE(q.sort_order, q.id)
FROM exam_questions q
WHERE q.exam_id IS NOT NULL;

UPDATE exams e
LEFT JOIN (
    SELECT exam_id, COUNT(*) AS question_count, SUM(score) AS total_score
    FROM exam_paper_questions
    GROUP BY exam_id
) paper ON paper.exam_id = e.id
SET e.question_count = COALESCE(paper.question_count, e.question_count, 0),
    e.total_score = COALESCE(paper.total_score, e.total_score, 0),
    e.auto_submit = COALESCE(e.auto_submit, 1);
