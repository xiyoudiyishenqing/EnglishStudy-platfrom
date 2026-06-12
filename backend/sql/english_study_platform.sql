CREATE DATABASE IF NOT EXISTS english_study_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE english_study_platform;

DROP TABLE IF EXISTS qa_answers;
DROP TABLE IF EXISTS qa_questions;
DROP TABLE IF EXISTS forum_replies;
DROP TABLE IF EXISTS forum_posts;
DROP TABLE IF EXISTS user_feedback;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS homework_submissions;
DROP TABLE IF EXISTS homeworks;
DROP TABLE IF EXISTS exam_records;
DROP TABLE IF EXISTS exam_sessions;
DROP TABLE IF EXISTS exam_paper_questions;
DROP TABLE IF EXISTS exam_questions;
DROP TABLE IF EXISTS exams;
DROP TABLE IF EXISTS student_word_records;
DROP TABLE IF EXISTS vocabulary_words;
DROP TABLE IF EXISTS study_plans;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS system_configs;
DROP TABLE IF EXISTS operation_logs;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    class_name VARCHAR(100),
    bio TEXT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255),
    stored_name VARCHAR(255),
    content_type VARCHAR(100),
    file_size BIGINT,
    url VARCHAR(255) NOT NULL,
    creator_id BIGINT NOT NULL,
    creator_name VARCHAR(50) NOT NULL,
    creator_role VARCHAR(20) NOT NULL,
    downloadable TINYINT(1) NOT NULL DEFAULT 1,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    class_name VARCHAR(100),
    audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    online_status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE study_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    target_content TEXT,
    progress INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    due_date DATE,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE vocabulary_words (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL,
    phonetic VARCHAR(100),
    meaning VARCHAR(255) NOT NULL,
    example_sentence VARCHAR(500),
    example_translation VARCHAR(500),
    difficulty VARCHAR(20),
    sort_order INT,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_vocabulary_words_word (word),
    KEY idx_vocabulary_words_sort_order (sort_order)
);

CREATE TABLE student_word_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    review_count INT NOT NULL DEFAULT 0,
    known_count INT NOT NULL DEFAULT 0,
    unknown_count INT NOT NULL DEFAULT 0,
    last_result VARCHAR(20),
    last_reviewed_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    UNIQUE KEY uk_student_word_record (student_id, word_id),
    KEY idx_student_word_records_student_id (student_id),
    KEY idx_student_word_records_word_id (word_id),
    KEY idx_student_word_records_status (status)
);

CREATE TABLE exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL,
    question_count INT NOT NULL DEFAULT 0,
    teacher_id BIGINT,
    teacher_name VARCHAR(50),
    class_name VARCHAR(100),
    duration_minutes INT NOT NULL,
    auto_submit TINYINT(1) NOT NULL DEFAULT 1,
    total_score INT NOT NULL,
    published TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE homeworks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    class_name VARCHAR(100) NOT NULL,
    teacher_id BIGINT NOT NULL,
    teacher_name VARCHAR(50) NOT NULL,
    due_time DATETIME,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE homework_submissions (
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
    updated_at DATETIME
);

CREATE TABLE exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT,
    exam_type VARCHAR(50) NOT NULL DEFAULT '大学四级',
    question_type VARCHAR(30) NOT NULL DEFAULT 'SINGLE_CHOICE',
    content TEXT NOT NULL,
    option_a VARCHAR(255),
    option_b VARCHAR(255),
    option_c VARCHAR(255),
    option_d VARCHAR(255),
    correct_answer VARCHAR(255) NOT NULL,
    analysis TEXT,
    audio_file_name VARCHAR(255),
    audio_stored_name VARCHAR(255),
    audio_content_type VARCHAR(100),
    audio_file_size BIGINT,
    score INT NOT NULL,
    sort_order INT,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    KEY idx_exam_questions_exam_type (exam_type),
    KEY idx_exam_questions_question_type (question_type),
    KEY idx_exam_questions_enabled (enabled)
);

CREATE TABLE exam_paper_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    score INT NOT NULL,
    sort_order INT NOT NULL,
    UNIQUE KEY uk_exam_paper_question (exam_id, question_id),
    KEY idx_exam_paper_questions_exam_id (exam_id),
    KEY idx_exam_paper_questions_question_id (question_id)
);

CREATE TABLE exam_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    exam_title VARCHAR(100) NOT NULL,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(50) NOT NULL,
    class_name VARCHAR(100),
    score INT NOT NULL,
    total_score INT NOT NULL,
    answers_json TEXT,
    teacher_feedback TEXT,
    submitted_at DATETIME NOT NULL,
    UNIQUE KEY uk_exam_records_student_exam (student_id, exam_id),
    KEY idx_exam_records_exam_id (exam_id),
    KEY idx_exam_records_student_id (student_id)
);

CREATE TABLE exam_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    answers_json TEXT,
    remaining_seconds INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    target_role VARCHAR(20) NOT NULL,
    created_by_id BIGINT NOT NULL,
    created_by_name VARCHAR(50) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE user_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    reply_content TEXT,
    handled_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE qa_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    student_name VARCHAR(50) NOT NULL,
    class_name VARCHAR(100),
    resource_id BIGINT,
    resource_title VARCHAR(100),
    course_title VARCHAR(100),
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE qa_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    teacher_name VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE forum_posts (
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

CREATE TABLE forum_replies (
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

CREATE TABLE operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    detail TEXT,
    created_at DATETIME NOT NULL
);

CREATE TABLE system_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(50) NOT NULL UNIQUE,
    config_value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME,
    updated_at DATETIME
);

INSERT INTO users (id, username, password, real_name, role, email, class_name, bio, status, created_at, updated_at) VALUES
(1, 'student1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '张同学', 'STUDENT', 'student1@example.com', '英语2301班', '喜欢背单词和刷题。', 'ACTIVE', NOW(), NOW()),
(2, 'teacher1', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '李老师', 'TEACHER', 'teacher1@example.com', '英语2301班', '负责大学英语课程。', 'ACTIVE', NOW(), NOW()),
(3, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', '系统管理员', 'ADMIN', 'admin@example.com', NULL, '平台管理员账号。', 'ACTIVE', NOW(), NOW());

INSERT INTO resources (id, title, description, type, file_name, stored_name, content_type, file_size, url, creator_id, creator_name, creator_role, downloadable, visibility, class_name, audit_status, online_status, created_at, updated_at) VALUES
(1, '英语四级高频词汇表', '适合大学生日常记忆的核心词汇资料。', '文档', 'cet4-words.docx', NULL, 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 0, 'https://example.com/cet4-words.docx', 2, '李老师', 'TEACHER', 1, 'CLASS', '英语2301班', 'APPROVED', 'PUBLISHED', NOW(), NOW()),
(2, '英语2301班听力训练视频', '基础听力训练视频，仅英语2301班学生可见。', '视频', 'listening.mp4', NULL, 'video/mp4', 0, 'https://example.com/listening.mp4', 2, '李老师', 'TEACHER', 0, 'CLASS', '英语2301班', 'APPROVED', 'PUBLISHED', NOW(), NOW());

INSERT INTO study_plans (id, student_id, title, target_content, progress, status, due_date, created_at, updated_at) VALUES
(1, 1, '本周单词计划', '每天记忆30个四级词汇。', 40, 'IN_PROGRESS', DATE_ADD(CURDATE(), INTERVAL 7 DAY), NOW(), NOW());

INSERT INTO vocabulary_words (id, word, phonetic, meaning, example_sentence, example_translation, difficulty, sort_order, created_at, updated_at) VALUES
(1, 'abandon', '/abandən/', '放弃；抛弃', 'Never abandon your dream when things get difficult.', '遇到困难时不要放弃你的梦想。', 'CET4', 1, NOW(), NOW()),
(2, 'ability', '/əbiləti/', '能力；才能', 'Reading every day can improve your language ability.', '每天阅读可以提高你的语言能力。', 'CET4', 2, NOW(), NOW()),
(3, 'absorb', '/əbsɔːrb/', '吸收；理解', 'Students need time to absorb new knowledge.', '学生需要时间吸收新知识。', 'CET4', 3, NOW(), NOW()),
(4, 'academic', '/akədemik/', '学术的；学院的', 'Academic writing requires clear logic and evidence.', '学术写作需要清晰的逻辑和证据。', 'CET4', 4, NOW(), NOW()),
(5, 'access', '/akses/', '进入；使用权', 'Online courses give students easy access to resources.', '在线课程让学生方便地获取资源。', 'CET4', 5, NOW(), NOW()),
(6, 'achieve', '/ətʃiːv/', '实现；达到', 'You can achieve better results with steady practice.', '坚持练习可以取得更好的结果。', 'CET4', 6, NOW(), NOW()),
(7, 'adapt', '/ədapt/', '适应；改编', 'Freshmen must adapt to college life quickly.', '大一新生需要快速适应大学生活。', 'CET4', 7, NOW(), NOW()),
(8, 'advantage', '/ədvantidʒ/', '优势；有利条件', 'Good pronunciation is an advantage in oral English.', '良好的发音是英语口语中的优势。', 'CET4', 8, NOW(), NOW()),
(9, 'affect', '/əfekt/', '影响', 'Sleep quality can affect learning efficiency.', '睡眠质量会影响学习效率。', 'CET4', 9, NOW(), NOW()),
(10, 'analysis', '/ənaləsis/', '分析', 'The teacher gave a detailed analysis of the passage.', '老师对文章进行了详细分析。', 'CET4', 10, NOW(), NOW()),
(11, 'approach', '/əprəutʃ/', '方法；接近', 'This approach helps students remember words faster.', '这种方法帮助学生更快记单词。', 'CET4', 11, NOW(), NOW()),
(12, 'benefit', '/benefit/', '好处；受益', 'You will benefit from regular listening practice.', '你会从规律的听力练习中受益。', 'CET4', 12, NOW(), NOW()),
(13, 'challenge', '/tʃalindʒ/', '挑战', 'Speaking English in public is a challenge for many students.', '公开说英语对许多学生来说是挑战。', 'CET4', 13, NOW(), NOW()),
(14, 'communicate', '/kəmjuːnikeit/', '交流；沟通', 'Group work helps students communicate in English.', '小组活动帮助学生用英语交流。', 'CET4', 14, NOW(), NOW()),
(15, 'concentrate', '/kɔnsəntreit/', '集中注意力', 'Please concentrate on the key words in the sentence.', '请关注句子中的关键词。', 'CET4', 15, NOW(), NOW()),
(16, 'confidence', '/kɔnfidəns/', '信心', 'Practice can build your confidence in speaking.', '练习可以增强你的口语信心。', 'CET4', 16, NOW(), NOW()),
(17, 'consider', '/kənsidər/', '考虑；认为', 'You should consider the context before choosing an answer.', '选择答案前应该考虑上下文。', 'CET4', 17, NOW(), NOW()),
(18, 'create', '/krieit/', '创造；创建', 'Teachers can create more tasks for vocabulary review.', '教师可以创建更多词汇复习任务。', 'CET4', 18, NOW(), NOW()),
(19, 'culture', '/kʌltʃər/', '文化', 'Learning a language also means learning its culture.', '学习一门语言也意味着学习它的文化。', 'CET4', 19, NOW(), NOW()),
(20, 'develop', '/diveləp/', '发展；培养', 'Students should develop good learning habits.', '学生应该培养良好的学习习惯。', 'CET4', 20, NOW(), NOW()),
(21, 'efficient', '/ifiʃnt/', '高效的', 'An efficient plan saves time and energy.', '高效的计划可以节省时间和精力。', 'CET4', 21, NOW(), NOW()),
(22, 'environment', '/invairənmənt/', '环境', 'A quiet environment is helpful for reading.', '安静的环境有助于阅读。', 'CET4', 22, NOW(), NOW()),
(23, 'essential', '/isenʃl/', '必要的；本质的', 'Vocabulary is essential for English learning.', '词汇对英语学习是必不可少的。', 'CET4', 23, NOW(), NOW()),
(24, 'evaluate', '/ivaljueit/', '评价；评估', 'The system can evaluate your exam performance.', '系统可以评估你的考试表现。', 'CET4', 24, NOW(), NOW()),
(25, 'evidence', '/evidəns/', '证据', 'Use evidence from the text to support your answer.', '用文中的证据支持你的答案。', 'CET4', 25, NOW(), NOW()),
(26, 'experience', '/ikspiriəns/', '经验；经历', 'Reading original texts is a valuable experience.', '阅读原文是一种宝贵的经历。', 'CET4', 26, NOW(), NOW()),
(27, 'improve', '/impruːv/', '提高；改善', 'Listening to news can improve your English listening.', '听新闻可以提高英语听力。', 'CET4', 27, NOW(), NOW()),
(28, 'increase', '/inkriːs/', '增加；增长', 'Daily review can increase your vocabulary size.', '每日复习可以扩大词汇量。', 'CET4', 28, NOW(), NOW()),
(29, 'influence', '/influəns/', '影响', 'A good teacher has a strong influence on students.', '好老师会对学生产生很大影响。', 'CET4', 29, NOW(), NOW()),
(30, 'knowledge', '/nɔlidʒ/', '知识', 'Knowledge grows through practice and reflection.', '知识在实践和反思中增长。', 'CET4', 30, NOW(), NOW()),
(31, 'method', '/meθəd/', '方法', 'Find a method that matches your learning style.', '找到适合自己学习风格的方法。', 'CET4', 31, NOW(), NOW()),
(32, 'opportunity', '/ɔpətjuːnəti/', '机会', 'College gives you many opportunities to use English.', '大学给你很多使用英语的机会。', 'CET4', 32, NOW(), NOW()),
(33, 'participate', '/pɑːrtisipeit/', '参加；参与', 'Everyone should participate in the discussion.', '每个人都应该参与讨论。', 'CET4', 33, NOW(), NOW()),
(34, 'practice', '/praktis/', '练习；实践', 'Regular practice is the key to fluency.', '规律练习是流利表达的关键。', 'CET4', 34, NOW(), NOW()),
(35, 'prepare', '/pripeər/', '准备', 'Please prepare your presentation before class.', '请在课前准备好展示。', 'CET4', 35, NOW(), NOW()),
(36, 'progress', '/prəugres/', '进步；进展', 'Small progress every day leads to big changes.', '每天一点进步会带来巨大变化。', 'CET4', 36, NOW(), NOW()),
(37, 'pronunciation', '/prənʌnsieiʃn/', '发音', 'Clear pronunciation makes communication easier.', '清晰的发音让交流更容易。', 'CET4', 37, NOW(), NOW()),
(38, 'purpose', '/pɜːrpəs/', '目的；用途', 'The purpose of this task is vocabulary review.', '这个任务的目的是复习词汇。', 'CET4', 38, NOW(), NOW()),
(39, 'recommend', '/rekəmend/', '推荐；建议', 'I recommend reading the article twice.', '我建议把这篇文章读两遍。', 'CET4', 39, NOW(), NOW()),
(40, 'reduce', '/ridjuːs/', '减少；降低', 'Good planning can reduce exam stress.', '良好的计划可以减轻考试压力。', 'CET4', 40, NOW(), NOW()),
(41, 'reflect', '/riflekt/', '反映；反思', 'You should reflect on your mistakes after practice.', '练习后应该反思自己的错误。', 'CET4', 41, NOW(), NOW()),
(42, 'require', '/rikwaiər/', '需要；要求', 'This course requires active participation.', '这门课程要求积极参与。', 'CET4', 42, NOW(), NOW()),
(43, 'resource', '/riːsɔːrs/', '资源', 'The platform provides many learning resources.', '平台提供了很多学习资源。', 'CET4', 43, NOW(), NOW()),
(44, 'review', '/rivjuː/', '复习；评论', 'Review the words before taking the test.', '考试前复习这些单词。', 'CET4', 44, NOW(), NOW()),
(45, 'significant', '/signifikənt/', '重要的；显著的', 'Vocabulary has a significant effect on reading speed.', '词汇量对阅读速度有显著影响。', 'CET4', 45, NOW(), NOW()),
(46, 'strategy', '/stratədʒi/', '策略', 'A good strategy makes memorizing words easier.', '好的策略让记单词更轻松。', 'CET4', 46, NOW(), NOW()),
(47, 'summary', '/sʌməri/', '总结；摘要', 'Write a short summary after reading the passage.', '读完文章后写一个简短总结。', 'CET4', 47, NOW(), NOW()),
(48, 'translate', '/tranzleit/', '翻译', 'Try to translate the sentence into Chinese.', '试着把这个句子翻译成中文。', 'CET4', 48, NOW(), NOW()),
(49, 'understand', '/ʌndərstand/', '理解', 'It is important to understand the main idea first.', '先理解主旨很重要。', 'CET4', 49, NOW(), NOW()),
(50, 'valuable', '/valjuəbl/', '有价值的', 'Mistakes are valuable if you learn from them.', '如果能从错误中学习，错误也是有价值的。', 'CET4', 50, NOW(), NOW());

INSERT INTO exams (id, title, description, type, question_count, teacher_id, teacher_name, class_name, duration_minutes, auto_submit, total_score, published, created_at, updated_at) VALUES
(1, '大学四级随机练习卷', '系统从大学四级题库中随机抽取题目生成，覆盖词汇、语法、阅读和基础写作表达。', '大学四级', 8, 2, '李老师', '英语2301班', 35, 1, 80, 1, NOW(), NOW()),
(2, '大学六级随机练习卷', '系统从大学六级题库中随机抽取题目生成，题目难度高于四级。', '大学六级', 8, 2, '李老师', '英语2301班', 40, 1, 80, 1, NOW(), NOW()),
(3, '考研英语随机练习卷', '系统从考研英语题库中随机抽取题目生成，偏重长难句、逻辑衔接和学术表达。', '考研英语', 8, 2, '李老师', '英语2301班', 45, 1, 80, 1, NOW(), NOW());

INSERT INTO homeworks (id, title, description, class_name, teacher_id, teacher_name, due_time, status, created_at, updated_at) VALUES
(1, '四级高频词汇默写', '完成本周 30 个四级高频词汇默写，并提交错词总结。', '英语2301班', 2, '李老师', DATE_ADD(NOW(), INTERVAL 7 DAY), 'PUBLISHED', NOW(), NOW());

INSERT INTO homework_submissions (id, homework_id, student_id, student_name, content, score, teacher_feedback, status, submitted_at, graded_at, created_at, updated_at) VALUES
(1, 1, 1, '张同学', '已完成词汇默写，错词主要集中在 improve、participate、collect。', 88, '完成情况较好，继续加强词义辨析。', 'GRADED', NOW(), NOW(), NOW(), NOW());

INSERT INTO exam_questions (id, exam_id, exam_type, question_type, content, option_a, option_b, option_c, option_d, correct_answer, analysis, score, sort_order, enabled) VALUES
(1, NULL, '大学四级', 'SINGLE_CHOICE', 'Choose the correct word: She ____ to school every day.', 'go', 'goes', 'going', 'gone', 'B', '第三人称单数主语后一般现在时动词加 s。', 10, NULL, 1),
(2, NULL, '大学四级', 'SINGLE_CHOICE', 'Which one is a noun?', 'quickly', 'beautiful', 'student', 'write', 'C', 'student 是名词。', 10, NULL, 1),
(3, NULL, '大学四级', 'SINGLE_CHOICE', 'Translate "improve" into Chinese.', '提高', '完成', '参加', '收集', 'A', 'improve 表示提高、改善。', 10, NULL, 1),
(4, NULL, '大学四级', 'TRUE_FALSE', 'The word "collect" can mean 收集.', '正确', '错误', NULL, NULL, 'A', 'collect 常见含义是收集。', 10, NULL, 1),
(5, NULL, '大学四级', 'FILL_BLANK', 'Fill in the blank: I am interested ____ English.', NULL, NULL, NULL, NULL, 'in', 'be interested in 是固定搭配。', 10, NULL, 1),
(6, NULL, '大学四级', 'MULTIPLE_CHOICE', 'Which words can describe a learning plan?', 'weekly', 'random', 'measurable', 'silent', 'A,C', 'weekly 和 measurable 都可以描述学习计划。', 10, NULL, 1),
(7, NULL, '大学四级', 'SINGLE_CHOICE', 'Choose the best answer: We often ____ English in the morning.', 'read', 'reads', 'reading', 'is read', 'A', '主语 we 后用动词原形。', 10, NULL, 1),
(8, NULL, '大学四级', 'SINGLE_CHOICE', 'What is the opposite of "easy"?', 'hard', 'early', 'near', 'slow', 'A', 'hard 可以表示困难的。', 10, NULL, 1),
(9, NULL, '大学四级', 'TRUE_FALSE', 'The sentence "He likes English" is grammatically correct.', '正确', '错误', NULL, NULL, 'A', 'he 是第三人称单数，likes 正确。', 10, NULL, 1),
(10, NULL, '大学四级', 'MULTIPLE_CHOICE', 'Which expressions can be used to give advice?', 'You should...', 'Why not...', 'Yesterday was...', 'It is raining...', 'A,B', 'You should 和 Why not 都可用于提出建议。', 10, NULL, 1),
(11, NULL, '大学四级', 'FILL_BLANK', 'Fill in the blank: Please write ____ your answer clearly.', NULL, NULL, NULL, NULL, 'down', 'write down 表示写下、记录。', 10, NULL, 1),
(12, NULL, '大学四级', 'SHORT_ANSWER', 'Write one English phrase that means 参加.', NULL, NULL, NULL, NULL, 'take part in', 'take part in 表示参加。', 10, NULL, 1),
(13, NULL, '大学六级', 'SINGLE_CHOICE', 'The phrase "in terms of" is closest in meaning to ____.', 'because of', 'with regard to', 'instead of', 'in spite of', 'B', 'in terms of 表示就……而言。', 10, NULL, 1),
(14, NULL, '大学六级', 'SINGLE_CHOICE', 'Choose the word closest in meaning to "significant".', 'important', 'ordinary', 'silent', 'narrow', 'A', 'significant 表示重要的、显著的。', 10, NULL, 1),
(15, NULL, '大学六级', 'SINGLE_CHOICE', 'The report ____ that online learning can improve flexibility.', 'suggests', 'suggest', 'suggesting', 'to suggest', 'A', '主语 report 为第三人称单数，谓语用 suggests。', 10, NULL, 1),
(16, NULL, '大学六级', 'MULTIPLE_CHOICE', 'Which words can describe academic writing?', 'formal', 'logical', 'random', 'evidence-based', 'A,B,D', '学术写作通常要求正式、逻辑清晰、有证据支撑。', 10, NULL, 1),
(17, NULL, '大学六级', 'TRUE_FALSE', 'The word "decline" can be used as both a noun and a verb.', '正确', '错误', NULL, NULL, 'A', 'decline 可作名词“下降”，也可作动词“下降、拒绝”。', 10, NULL, 1),
(18, NULL, '大学六级', 'FILL_BLANK', 'Fill in the blank: The experiment is based ____ reliable data.', NULL, NULL, NULL, NULL, 'on', 'be based on 表示基于。', 10, NULL, 1),
(19, NULL, '大学六级', 'SHORT_ANSWER', 'Write one English word that means 有效的.', NULL, NULL, NULL, NULL, 'effective', 'effective 表示有效的。', 10, NULL, 1),
(20, NULL, '大学六级', 'SINGLE_CHOICE', 'Choose the best transition: The method is simple; ____, it is highly practical.', 'however', 'moreover', 'instead', 'otherwise', 'B', 'moreover 表示进一步补充，符合语义。', 10, NULL, 1),
(21, NULL, '大学六级', 'MULTIPLE_CHOICE', 'Which phrases can express cause and effect?', 'as a result', 'therefore', 'on the contrary', 'because of', 'A,B,D', 'as a result、therefore、because of 都可表达因果关系。', 10, NULL, 1),
(22, NULL, '大学六级', 'TRUE_FALSE', 'The phrase "on the contrary" is used to introduce a similar idea.', '正确', '错误', NULL, NULL, 'B', 'on the contrary 用于引出相反观点。', 10, NULL, 1),
(23, NULL, '大学六级', 'FILL_BLANK', 'Fill in the blank: Students should focus ____ long-term improvement.', NULL, NULL, NULL, NULL, 'on', 'focus on 表示专注于。', 10, NULL, 1),
(24, NULL, '大学六级', 'SHORT_ANSWER', 'Write one English phrase that means 换句话说.', NULL, NULL, NULL, NULL, 'in other words', 'in other words 表示换句话说。', 10, NULL, 1),
(25, NULL, '考研英语', 'SINGLE_CHOICE', 'The author is most likely to agree that critical thinking is ____.', 'unnecessary', 'essential', 'temporary', 'automatic', 'B', 'critical thinking 通常被视为重要能力。', 10, NULL, 1),
(26, NULL, '考研英语', 'SINGLE_CHOICE', 'Choose the closest meaning of "consequently".', 'as a result', 'in contrast', 'for example', 'at random', 'A', 'consequently 表示因此、结果是。', 10, NULL, 1),
(27, NULL, '考研英语', 'MULTIPLE_CHOICE', 'Which expressions are suitable for argumentative writing?', 'it can be argued that', 'evidence suggests that', 'once upon a time', 'from this perspective', 'A,B,D', '议论文常使用论证、证据和视角表达。', 10, NULL, 1),
(28, NULL, '考研英语', 'TRUE_FALSE', 'The phrase "rather than" is often used to show contrast or preference.', '正确', '错误', NULL, NULL, 'A', 'rather than 表示而不是，常体现对比或取舍。', 10, NULL, 1),
(29, NULL, '考研英语', 'FILL_BLANK', 'Fill in the blank: The conclusion is consistent ____ the evidence.', NULL, NULL, NULL, NULL, 'with', 'be consistent with 表示与……一致。', 10, NULL, 1),
(30, NULL, '考研英语', 'SHORT_ANSWER', 'Write one English phrase that means 总的来说.', NULL, NULL, NULL, NULL, 'in general', 'in general 可表示总的来说。', 10, NULL, 1),
(31, NULL, '考研英语', 'SINGLE_CHOICE', 'Which word best completes the sentence: The policy aims to ____ innovation.', 'promote', 'prevent', 'ignore', 'delay', 'A', 'promote innovation 表示促进创新。', 10, NULL, 1),
(32, NULL, '考研英语', 'MULTIPLE_CHOICE', 'Which words can signal contrast?', 'however', 'nevertheless', 'therefore', 'whereas', 'A,B,D', 'however、nevertheless、whereas 都可表达转折或对比。', 10, NULL, 1),
(33, NULL, '考研英语', 'TRUE_FALSE', 'The word "whereas" can introduce a contrast between two clauses.', '正确', '错误', NULL, NULL, 'A', 'whereas 常用于连接对比关系的分句。', 10, NULL, 1),
(34, NULL, '考研英语', 'FILL_BLANK', 'Fill in the blank: The study sheds light ____ the causes of anxiety.', NULL, NULL, NULL, NULL, 'on', 'shed light on 表示阐明、揭示。', 10, NULL, 1),
(35, NULL, '考研英语', 'SHORT_ANSWER', 'Write one English phrase that means 与……有关.', NULL, NULL, NULL, NULL, 'be related to', 'be related to 表示与……有关。', 10, NULL, 1),
(36, NULL, '考研英语', 'SINGLE_CHOICE', 'Choose the best transition: The sample size was small; ____, the findings remain valuable.', 'nevertheless', 'therefore', 'for instance', 'similarly', 'A', 'nevertheless 表示尽管如此，符合语义转折。', 10, NULL, 1);

INSERT INTO exam_paper_questions (exam_id, question_id, score, sort_order) VALUES
(1, 1, 10, 1),
(1, 2, 10, 2),
(1, 3, 10, 3),
(1, 4, 10, 4),
(1, 5, 10, 5),
(1, 6, 10, 6),
(1, 7, 10, 7),
(1, 8, 10, 8),
(2, 13, 10, 1),
(2, 14, 10, 2),
(2, 15, 10, 3),
(2, 16, 10, 4),
(2, 17, 10, 5),
(2, 18, 10, 6),
(2, 19, 10, 7),
(2, 20, 10, 8),
(3, 25, 10, 1),
(3, 26, 10, 2),
(3, 27, 10, 3),
(3, 28, 10, 4),
(3, 29, 10, 5),
(3, 30, 10, 6),
(3, 31, 10, 7),
(3, 32, 10, 8);

INSERT INTO notifications (id, title, content, target_role, created_by_id, created_by_name, created_at, updated_at) VALUES
(1, '欢迎使用平台', '请同学们先完成入学测试，再制定学习计划。', 'STUDENT', 3, '系统管理员', NOW(), NOW()),
(2, '教师资源上传提醒', '请教师及时上传本周课程资料。', 'TEACHER', 3, '系统管理员', NOW(), NOW()),
(3, '系统维护说明', '平台已完成基础数据初始化，可直接用于演示。', 'ALL', 3, '系统管理员', NOW(), NOW());

INSERT INTO user_feedback (id, user_id, username, role, content, status, reply_content, handled_at, created_at, updated_at) VALUES
(1, 1, 'student1', 'STUDENT', '希望增加更多听力练习资源。', 'PENDING', NULL, NULL, NOW(), NOW());

INSERT INTO qa_questions (id, student_id, student_name, class_name, resource_id, resource_title, course_title, title, content, status, created_at, updated_at) VALUES
(1, 1, '张同学', '英语2301班', 1, '英语四级高频词汇表', '英语四级高频词汇表', '过去式和现在完成时有什么区别？', '做题时总是分不清两个时态，想请老师简单讲一下。', 'OPEN', NOW(), NOW());

INSERT INTO operation_logs (id, user_id, username, module, action, detail, created_at) VALUES
(1, 3, 'admin', '系统初始化', '导入演示数据', '初始化平台基础数据', NOW());

INSERT INTO system_configs (id, config_key, config_value, description, created_at, updated_at) VALUES
(1, 'open_register', 'true', '是否开放学生注册', NOW(), NOW()),
(2, 'max_plan_count', '20', '单个学生建议学习计划数量', NOW(), NOW()),
(3, 'last_backup_time', '', '最近一次手动备份时间', NOW(), NOW());
