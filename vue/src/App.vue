<template>
  <div class="app-shell">
    <div v-if="!state.token" class="auth-wrap">
      <section class="hero-card">
        <span class="hero-chip">College English Platform</span>
        <h1>大学生英语学习平台</h1>
        <p>围绕学生学习、教师教学和管理员运维三类角色，提供资源学习、考试评估、反馈答疑和平台管理等核心功能。</p>
        <p>学生端内置 AI 词典，直接调用本地 Ollama 接口，不需要单独维护词库数据。</p>
        <div class="hero-list">
          <div class="hero-item">学生默认可注册；演示账号也已在 SQL 中准备好。</div>
          <div class="hero-item">教师可发布资源、创建考试、查看成绩并进行答疑。</div>
          <div class="hero-item">管理员可管理用户、公告、反馈、系统参数和运营数据。</div>
        </div>
      </section>

      <section class="auth-card">
        <h2>{{ state.authMode === 'login' ? '登录系统' : '学生注册' }}</h2>
        <p>演示账号：`student1 / 123456`、`teacher1 / 123456`、`admin / 123456`</p>

        <el-segmented
          v-model="state.authMode"
          class="auth-segment"
          :options="[
            { label: '登录', value: 'login' },
            { label: '学生注册', value: 'register' }
          ]"
        />

        <form v-if="state.authMode === 'login'" class="form-grid" @submit.prevent="handleLogin">
          <div class="field">
            <label>用户名</label>
            <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable />
          </div>
          <div class="field">
            <label>密码</label>
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
          </div>
          <el-button type="primary" native-type="submit" :loading="state.loading">登录</el-button>
        </form>

        <form v-else class="form-grid two-col" @submit.prevent="handleRegister">
          <div class="field">
            <label>用户名</label>
            <el-input v-model="registerForm.username" placeholder="请输入用户名" clearable />
          </div>
          <div class="field">
            <label>姓名</label>
            <el-input v-model="registerForm.realName" placeholder="请输入姓名" clearable />
          </div>
          <div class="field">
            <label>密码</label>
            <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
          </div>
          <div class="field">
            <label>邮箱</label>
            <el-input v-model="registerForm.email" placeholder="请输入邮箱" clearable />
          </div>
          <div class="field" style="grid-column: 1 / -1;">
            <label>班级</label>
            <el-input v-model="registerForm.className" placeholder="例如：英语2301班" clearable />
          </div>
          <el-button type="primary" native-type="submit" style="grid-column: 1 / -1;" :loading="state.loading">注册学生账号</el-button>
        </form>

        <el-alert v-if="state.error" class="form-alert" type="error" :title="state.error" show-icon :closable="false" />
      </section>
    </div>

    <div v-else class="main-layout">
      <aside class="sidebar">
        <div class="brand-block">
          <div class="brand-mark">ES</div>
          <h2>英语学习平台</h2>
          <p>简单版毕设实现，覆盖学生、教师、管理员三类核心流程。</p>
        </div>

        <div class="user-panel">
          <strong>{{ state.user.realName }}（{{ roleLabel(state.user.role) }}）</strong>
          <div class="muted">账号：{{ state.user.username }}</div>
          <div class="muted" v-if="state.user.className">班级：{{ state.user.className }}</div>
        </div>

        <div class="menu-list">
          <button
            v-for="menu in currentMenus"
            :key="menu.key"
            class="menu-btn"
            :class="{ active: state.currentView === menu.key }"
            @click="changeView(menu.key)"
          >
            <span class="menu-index">{{ String(currentMenus.findIndex((item) => item.key === menu.key) + 1).padStart(2, '0') }}</span>
            <span>{{ menu.label }}</span>
          </button>
        </div>

        <div class="action-row" style="margin-top: 20px;">
          <el-button plain @click="changeView('profile')">个人资料</el-button>
          <el-button type="danger" plain @click="logout">退出登录</el-button>
        </div>
      </aside>

      <main class="content-area">
        <section class="content-card">
          <div class="section-head">
            <div>
              <h3>{{ currentTitle }}</h3>
              <p>{{ currentDescription }}</p>
            </div>
            <div class="badge-row">
              <el-tag effect="plain">{{ roleLabel(state.user.role) }}</el-tag>
              <el-tag type="info" effect="plain">{{ nowText }}</el-tag>
            </div>
          </div>

          <el-alert v-if="state.message" class="form-alert" type="success" :title="state.message" show-icon :closable="false" />
          <el-alert v-if="state.error" class="form-alert" type="error" :title="state.error" show-icon :closable="false" />

          <template v-if="state.currentView === 'student-dashboard'">
            <div class="analytics-grid">
              <div v-for="item in studentDashboardCards" :key="item.label" class="metric-card metric-card--visual">
                <div class="metric-top">
                  <div class="metric-label">{{ item.label }}</div>
                  <span class="metric-dot" :style="{ backgroundColor: item.color }"></span>
                </div>
                <div class="metric-value">{{ item.value }}</div>
                <div class="metric-progress">
                  <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                </div>
                <div class="metric-hint">{{ item.hint }}</div>
              </div>
            </div>
            <div class="split-grid" style="margin-top: 18px;">
              <div class="panel-card">
                <h4>最近通知</h4>
                <div v-if="!(state.studentDashboard.notifications || []).length" class="empty-state">暂无通知</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.studentDashboard.notifications" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.content }}</div>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>学习概览</h4>
                <div class="donut-layout">
                  <div class="donut-chart" :style="{ background: studentLearningChart.gradient }">
                    <div class="donut-center">
                      <strong>{{ studentLearningChart.total }}</strong>
                      <span>事项</span>
                    </div>
                  </div>
                  <div class="chart-legend">
                    <div v-for="item in studentLearningChart.items" :key="item.name" class="legend-row">
                      <span class="legend-left">
                        <i class="legend-swatch" :style="{ backgroundColor: item.color }"></i>
                        {{ item.name }}
                      </span>
                      <strong>{{ item.value }} / {{ item.percent }}%</strong>
                    </div>
                  </div>
                </div>
                <div class="bar-list compact">
                  <div v-for="item in studentDashboardBars" :key="item.label" class="bar-row">
                    <div class="bar-meta">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                    </div>
                    <div class="bar-track">
                      <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'resources'">
            <div class="field" style="margin-bottom: 16px;">
              <label>资源搜索</label>
              <el-input v-model="state.resourceKeyword" placeholder="输入标题或描述关键词后点击刷新" clearable />
            </div>
            <div class="action-row" style="margin-bottom: 16px;">
              <el-button type="primary" @click="loadStudentResources">刷新资源</el-button>
            </div>
            <div v-if="!state.resources.length" class="empty-state">暂无资源</div>
            <div v-else class="card-list">
              <div v-for="item in state.resources" :key="item.id" class="card-item">
                <h4>{{ item.title }}</h4>
                <div class="muted">{{ item.description || '暂无描述' }}</div>
                <div class="badge-row">
                  <span class="badge">{{ item.type }}</span>
                  <span class="badge">发布人：{{ item.creatorName }}</span>
                  <span class="badge" v-if="item.fileName">文件：{{ item.fileName }}</span>
                  <span class="badge">{{ item.downloadable ? '可下载' : '在线查看' }}</span>
                </div>
                <div class="action-row" style="margin-top: 12px;">
                  <el-button plain @click="previewResource(item)">预览</el-button>
                  <el-button type="primary" @click="downloadResource(item)">下载</el-button>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'plans'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>新增 / 编辑学习计划</h4>
                <form class="form-grid" @submit.prevent="savePlan">
                  <div class="field">
                    <label>计划标题</label>
                    <input v-model="planForm.title" />
                  </div>
                  <div class="field">
                    <label>目标内容</label>
                    <textarea v-model="planForm.targetContent"></textarea>
                  </div>
                  <div class="form-grid three-col">
                    <div class="field">
                      <label>完成进度</label>
                      <input v-model.number="planForm.progress" type="number" min="0" max="100" />
                    </div>
                    <div class="field">
                      <label>状态</label>
                      <select v-model="planForm.status">
                        <option value="TODO">待开始</option>
                        <option value="IN_PROGRESS">进行中</option>
                        <option value="DONE">已完成</option>
                      </select>
                    </div>
                  </div>
                  <div class="field">
                    <label>截止日期</label>
                    <input v-model="planForm.dueDate" type="date" />
                  </div>
                  <div class="action-row">
                    <button class="primary-btn">{{ planForm.id ? '更新计划' : '新增计划' }}</button>
                    <button type="button" class="ghost-btn" @click="resetPlanForm">清空</button>
                  </div>
                </form>
              </div>
              <div class="panel-card">
                <h4>我的学习计划</h4>
                <div v-if="!state.plans.length" class="empty-state">还没有学习计划</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.plans" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.targetContent || '暂无目标说明' }}</div>
                    <div class="badge-row">
                      <span class="badge">进度 {{ item.progress }}%</span>
                      <span class="badge">{{ item.status }}</span>
                      <span class="badge" v-if="item.dueDate">截止 {{ item.dueDate }}</span>
                    </div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="mini-btn" @click="editPlan(item)">编辑</button>
                      <button class="danger-btn" @click="deletePlan(item.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'student-homeworks'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>本班作业</h4>
                <div v-if="!state.studentHomeworks.length" class="empty-state">暂无班级作业</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.studentHomeworks" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.description || '暂无说明' }}</div>
                    <div class="badge-row">
                      <span class="badge">班级：{{ item.className }}</span>
                      <span class="badge">教师：{{ item.teacherName }}</span>
                      <span class="badge" v-if="item.dueTime">截止：{{ formatTime(item.dueTime) }}</span>
                      <span class="badge">{{ resourceStatusLabel(item.mySubmission?.status || '未提交') }}</span>
                    </div>
                    <div v-if="item.mySubmission" class="muted" style="margin-top: 10px;">
                      已提交：{{ item.mySubmission.content || '暂无文字内容' }}
                      <span v-if="item.mySubmission.score !== null && item.mySubmission.score !== undefined">，得分 {{ item.mySubmission.score }}</span>
                      <span v-if="item.mySubmission.teacherFeedback">，反馈：{{ item.mySubmission.teacherFeedback }}</span>
                    </div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="primary-btn" @click="editHomeworkSubmission(item)">填写 / 修改提交</button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>提交作业</h4>
                <form class="form-grid" @submit.prevent="submitHomework">
                  <div class="field">
                    <label>作业</label>
                    <select v-model="homeworkSubmitForm.homeworkId">
                      <option :value="null">请选择作业</option>
                      <option v-for="item in state.studentHomeworks" :key="item.id" :value="item.id">{{ item.title }}</option>
                    </select>
                  </div>
                  <div class="field">
                    <label>提交内容</label>
                    <textarea v-model="homeworkSubmitForm.content" placeholder="填写答案、作文内容或完成说明"></textarea>
                  </div>
                  <div class="action-row">
                    <button class="primary-btn">提交作业</button>
                    <button type="button" class="ghost-btn" @click="resetHomeworkSubmitForm">清空</button>
                  </div>
                </form>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'student-exams'">
            <div class="split-grid">
              <div class="panel-card">
                <h4>可参加考试</h4>
                <div v-if="!availableStudentExams.length" class="empty-state">暂无可参加考试</div>
                <div v-else class="simple-list">
                  <div v-for="item in availableStudentExams" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.description || '暂无说明' }}</div>
                    <div class="badge-row">
                      <span class="badge">{{ item.type }}</span>
                      <span class="badge">{{ item.durationMinutes }} 分钟</span>
                      <span class="badge">题目 {{ item.questionCount || 0 }} 道</span>
                      <span class="badge">{{ item.autoSubmit ? '到时自动交卷' : '手动交卷' }}</span>
                      <span class="badge">总分 {{ item.totalScore }}</span>
                    </div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="primary-btn" @click="openExam(item.id)">进入考试</button>
                    </div>
                  </div>
                </div>
                <h4 style="margin-top: 22px;">已完成考试</h4>
                <div v-if="!completedStudentExams.length" class="empty-state">暂无已完成考试</div>
                <div v-else class="simple-list">
                  <div v-for="item in completedStudentExams" :key="item.id" class="simple-list-item exam-completed-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.description || '暂无说明' }}</div>
                    <div class="badge-row">
                      <span class="badge">{{ item.type }}</span>
                      <span class="badge">已交卷</span>
                      <span class="badge">得分 {{ item.score }} / {{ item.totalScore }}</span>
                      <span class="badge" v-if="item.submittedAt">提交：{{ formatTime(item.submittedAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>考试作答</h4>
                <div v-if="!state.currentExam" class="empty-state">请选择左侧考试</div>
                <div v-else class="simple-list">
                  <div class="simple-list-item">
                    <h4>{{ state.currentExam.title }}</h4>
                    <div class="muted">{{ state.currentExam.description }}</div>
                    <div class="badge-row">
                      <span class="badge">剩余时间：{{ examRemainingText }}</span>
                      <span class="badge">{{ state.examSessionStatus || 'NEW' }}</span>
                      <span class="badge">{{ state.currentExam.type }}</span>
                    </div>
                  </div>
                  <div v-for="question in state.currentExam.questions" :key="question.id" class="exam-question">
                    <strong>{{ question.sortOrder }}. [{{ question.questionTypeLabel }}] {{ question.content }}</strong>
                    <div v-if="question.hasAudio" class="audio-question-player">
                      <audio v-if="question.audioSrc" :src="question.audioSrc" controls preload="metadata"></audio>
                      <span v-else>音频加载中...</span>
                      <span>{{ question.audioFileName || '听力音频' }}</span>
                    </div>
                    <div v-if="isChoiceQuestion(question)" class="option-grid">
                      <label v-for="option in examOptions(question)" :key="option.key" class="option-item">
                        <input
                          v-if="question.questionType === 'MULTIPLE_CHOICE'"
                          type="checkbox"
                          :value="option.key"
                          :checked="multiAnswerSelected(question.id, option.key)"
                          @change="toggleMultiAnswer(question.id, option.key)"
                        />
                        <input
                          v-else
                          v-model="examAnswers[question.id]"
                          type="radio"
                          :value="option.key"
                          @change="persistExamSessionLocal"
                        />
                        <span>{{ option.key }}. {{ option.value }}</span>
                      </label>
                    </div>
                    <textarea
                      v-else-if="question.questionType === 'SHORT_ANSWER'"
                      v-model="examAnswers[question.id]"
                      class="answer-textarea"
                      placeholder="请输入答案"
                      @input="persistExamSessionLocal"
                    ></textarea>
                    <input
                      v-else
                      v-model="examAnswers[question.id]"
                      class="answer-input"
                      placeholder="请输入答案"
                      @input="persistExamSessionLocal"
                    />
                  </div>
                  <div class="action-row">
                    <button class="ghost-btn" @click="saveExamSessionRemote">保存进度</button>
                    <button class="primary-btn" @click="submitExam">提交试卷</button>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'student-records'">
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>考试名称</th>
                    <th>得分</th>
                    <th>教师反馈</th>
                    <th>提交时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in state.records" :key="item.id">
                    <td>{{ item.examTitle }}</td>
                    <td>{{ item.score }} / {{ item.totalScore }}</td>
                    <td>{{ item.teacherFeedback || '暂无反馈' }}</td>
                    <td>{{ formatTime(item.submittedAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <template v-else-if="state.currentView === 'vocabulary'">
            <div class="analytics-grid">
              <div v-for="item in vocabularyMetricCards" :key="item.label" class="metric-card metric-card--visual">
                <div class="metric-top">
                  <div class="metric-label">{{ item.label }}</div>
                  <span class="metric-dot" :style="{ backgroundColor: item.color }"></span>
                </div>
                <div class="metric-value">{{ item.value }}</div>
                <div class="metric-progress">
                  <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                </div>
                <div class="metric-hint">{{ item.hint }}</div>
              </div>
            </div>
            <div class="vocabulary-layout" style="margin-top: 18px;">
              <div class="panel-card vocabulary-card-panel">
                <div class="vocabulary-head">
                  <div>
                    <h4>单词闯关</h4>
                    <div class="muted">逐个记忆单词，点击认识或不认识后自动保存个人学习记录。</div>
                  </div>
                  <div class="action-row">
                    <button class="mini-btn" :class="{ active: state.vocabularyMode === 'all' }" @click="setVocabularyMode('all')">全部单词</button>
                    <button class="mini-btn" :class="{ active: state.vocabularyMode === 'review' }" @click="setVocabularyMode('review')">错词复习</button>
                  </div>
                </div>
                <div v-if="!state.vocabularyWords.length" class="empty-state">当前没有需要学习的单词</div>
                <div v-else-if="currentVocabularyWord" class="vocabulary-study">
                  <div class="vocabulary-progress-line">
                    <span>{{ state.vocabularyIndex + 1 }} / {{ state.vocabularyWords.length }}</span>
                    <strong>{{ wordStatusLabel(currentVocabularyWord.status) }}</strong>
                  </div>
                  <div class="metric-progress vocabulary-progress">
                    <span :style="{ width: `${vocabularyProgressPercent}%`, background: chartColors[0] }"></span>
                  </div>
                  <div class="word-card">
                    <div class="word-kicker">{{ currentVocabularyWord.difficulty || 'CET4' }}</div>
                    <h2>{{ currentVocabularyWord.word }}</h2>
                    <div class="word-phonetic">{{ currentVocabularyWord.phonetic || '暂无音标' }}</div>
                    <div class="word-meaning">{{ currentVocabularyWord.meaning }}</div>
                    <div class="word-example">
                      <strong>{{ currentVocabularyWord.exampleSentence }}</strong>
                      <span>{{ currentVocabularyWord.exampleTranslation }}</span>
                    </div>
                    <div class="badge-row">
                      <span class="badge">学习 {{ currentVocabularyWord.reviewCount || 0 }} 次</span>
                      <span class="badge">认识 {{ currentVocabularyWord.knownCount || 0 }} 次</span>
                      <span class="badge">不认识 {{ currentVocabularyWord.unknownCount || 0 }} 次</span>
                    </div>
                  </div>
                  <div class="action-row vocabulary-actions">
                    <button class="ghost-btn" @click="previousVocabularyWord">上一个</button>
                    <button class="danger-btn" @click="reviewVocabulary(false)">不认识</button>
                    <button class="primary-btn" @click="reviewVocabulary(true)">认识</button>
                    <button class="ghost-btn" @click="nextVocabularyWord">下一个</button>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>错词本</h4>
                <div v-if="!vocabularyReviewWords.length" class="empty-state">暂无错词，继续保持</div>
                <div v-else class="simple-list vocabulary-review-list">
                  <div v-for="item in vocabularyReviewWords" :key="item.id" class="simple-list-item">
                    <h4>{{ item.word }}</h4>
                    <div class="muted">{{ item.meaning }}</div>
                    <div class="badge-row">
                      <span class="badge">不认识 {{ item.unknownCount || 0 }} 次</span>
                      <span class="badge" v-if="item.lastReviewedAt">最近：{{ formatTime(item.lastReviewedAt) }}</span>
                    </div>
                  </div>
                </div>
                <div class="action-row" style="margin-top: 16px;">
                  <button class="ghost-btn" @click="loadVocabulary">刷新</button>
                  <button class="danger-btn" @click="resetVocabularyRecords">重置学习记录</button>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'dictionary'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>AI 词典</h4>
                <form class="form-grid" @submit.prevent="queryDictionary">
                  <div class="field">
                    <label>输入英文单词、短语或句子</label>
                    <textarea v-model="dictionaryForm.query" placeholder="例如：improve / take part in / How can I improve my listening?"></textarea>
                  </div>
                  <button class="primary-btn" :disabled="state.loading">{{ state.loading ? '查询中...' : '发送到 Ollama' }}</button>
                </form>
              </div>
              <div class="panel-card">
                <h4>词典结果</h4>
                <div v-if="!state.dictionaryResult" class="empty-state">查询结果会显示在这里</div>
                <div v-else class="simple-list-item">
                  <h4>{{ state.dictionaryResult.query }}</h4>
                  <div style="white-space: pre-wrap; line-height: 1.8;">{{ state.dictionaryResult.answer }}</div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'student-notifications'">
            <div v-if="!state.notifications.length" class="empty-state">暂无通知</div>
            <div v-else class="card-list">
              <div v-for="item in state.notifications" :key="item.id" class="card-item">
                <h4>{{ item.title }}</h4>
                <div class="muted">{{ item.content }}</div>
                <div class="badge-row">
                  <span class="badge">面向：{{ item.targetRole }}</span>
                  <span class="badge">发布人：{{ item.createdByName }}</span>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'student-feedback'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>提交反馈</h4>
                <form class="form-grid" @submit.prevent="submitFeedback">
                  <div class="field">
                    <label>反馈内容</label>
                    <textarea v-model="feedbackForm.content" placeholder="请输入建议、问题或使用感受"></textarea>
                  </div>
                  <button class="primary-btn">提交反馈</button>
                </form>
              </div>
              <div class="panel-card">
                <h4>我的反馈记录</h4>
                <div v-if="!state.feedbackList.length" class="empty-state">暂无反馈记录</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.feedbackList" :key="item.id" class="simple-list-item">
                    <div>{{ item.content }}</div>
                    <div class="badge-row">
                      <span class="badge">{{ item.status }}</span>
                    </div>
                    <div class="muted" v-if="item.replyContent" style="margin-top: 10px;">管理员回复：{{ item.replyContent }}</div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'student-qa'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>发起答疑</h4>
                <form class="form-grid" @submit.prevent="submitQuestion">
                  <div class="field">
                    <label>学习资源</label>
                    <select v-model="qaForm.resourceId">
                      <option :value="null">请选择学习资源</option>
                      <option v-for="item in state.qaResources" :key="item.id" :value="item.id">{{ item.title }}</option>
                    </select>
                  </div>
                  <div class="field">
                    <label>问题标题</label>
                    <input v-model="qaForm.title" />
                  </div>
                  <div class="field">
                    <label>问题内容</label>
                    <textarea v-model="qaForm.content"></textarea>
                  </div>
                  <button class="primary-btn">提交问题</button>
                </form>
              </div>
              <div class="panel-card">
                <h4>我的答疑记录</h4>
                <div v-if="!state.qaList.length" class="empty-state">暂无答疑记录</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.qaList" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.content }}</div>
                    <div class="badge-row">
                      <span class="badge">{{ item.resourceTitle || item.courseTitle || '未关联资源' }}</span>
                      <span class="badge">{{ item.className || state.user.className }}</span>
                      <span class="badge">{{ item.status }}</span>
                    </div>
                    <div v-if="item.answers && item.answers.length" style="margin-top: 12px;">
                      <div v-for="answer in item.answers" :key="answer.id" class="simple-list-item" style="margin-top: 10px;">
                        <strong>{{ answer.teacherName }}</strong>
                        <div class="muted">{{ answer.content }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'student-forum'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>发布帖子</h4>
                <form class="form-grid" @submit.prevent="createForumPost">
                  <div class="field">
                    <label>帖子标题</label>
                    <input v-model="forumForm.title" placeholder="请输入想讨论的话题" />
                  </div>
                  <div class="field">
                    <label>帖子内容</label>
                    <textarea v-model="forumForm.content" placeholder="写下你的问题、经验或想法"></textarea>
                  </div>
                  <button class="primary-btn">发布帖子</button>
                </form>
                <div style="margin-top: 18px;">
                  <div class="field">
                    <label>搜索帖子</label>
                    <input v-model="state.forumKeyword" placeholder="按标题、内容或发帖人搜索" @change="loadForumPosts" />
                  </div>
                </div>
                <div v-if="!state.forumPosts.length" class="empty-state" style="margin-top: 16px;">暂无帖子</div>
                <div v-else class="simple-list" style="margin-top: 16px;">
                  <div
                    v-for="item in state.forumPosts"
                    :key="item.id"
                    class="simple-list-item"
                    :class="{ active: state.currentForumPost?.id === item.id }"
                    @click="openForumPost(item.id)"
                  >
                    <div class="badge-row">
                      <span class="badge">{{ item.studentName }}</span>
                      <span class="badge">{{ item.replyCount || 0 }} 回复</span>
                      <span class="badge" v-if="item.lastReplyAt">更新于 {{ formatTime(item.lastReplyAt) }}</span>
                    </div>
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.content }}</div>
                    <div class="action-row" style="margin-top: 12px;" @click.stop>
                      <button v-if="item.isMine" class="danger-btn" @click="deleteForumPost(item.id)">删除帖子</button>
                    </div>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>帖子详情</h4>
                <div v-if="!state.currentForumPost" class="empty-state">请选择一个帖子查看详情</div>
                <div v-else>
                  <h4>{{ state.currentForumPost.title }}</h4>
                  <div class="muted" style="margin-bottom: 12px;">{{ state.currentForumPost.content }}</div>
                  <div class="badge-row">
                    <span class="badge">{{ state.currentForumPost.studentName }}</span>
                    <span class="badge">{{ state.currentForumPost.className }}</span>
                    <span class="badge">{{ state.currentForumPost.replyCount || 0 }} 回复</span>
                  </div>

                  <div style="margin-top: 18px;">
                    <h4>回帖</h4>
                    <form class="form-grid" @submit.prevent="createForumReply">
                      <div class="field">
                        <textarea v-model="forumReplyForm.content" placeholder="回复同学的内容"></textarea>
                      </div>
                      <button class="primary-btn">发表回复</button>
                    </form>
                  </div>

                  <div style="margin-top: 18px;">
                    <h4>回复列表</h4>
                    <div v-if="!state.currentForumPost.replies || !state.currentForumPost.replies.length" class="empty-state">暂无回复</div>
                    <div v-else class="simple-list">
                      <div v-for="reply in state.currentForumPost.replies" :key="reply.id" class="simple-list-item">
                        <div class="badge-row">
                          <span class="badge">{{ reply.studentName }}</span>
                          <span class="badge">{{ formatTime(reply.createdAt) }}</span>
                        </div>
                        <div class="muted" style="margin-top: 8px;">{{ reply.content }}</div>
                        <div class="action-row" style="margin-top: 12px;">
                          <button v-if="reply.isMine" class="danger-btn" @click="deleteForumReply(reply.id)">删除回复</button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-dashboard'">
            <div class="analytics-grid">
              <div v-for="item in teacherDashboardCards" :key="item.label" class="metric-card metric-card--visual">
                <div class="metric-top">
                  <div class="metric-label">{{ item.label }}</div>
                  <span class="metric-dot" :style="{ backgroundColor: item.color }"></span>
                </div>
                <div class="metric-value">{{ item.value }}</div>
                <div class="metric-progress">
                  <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                </div>
                <div class="metric-hint">{{ item.hint }}</div>
              </div>
            </div>
            <div class="report-grid" style="margin-top: 18px;">
              <div class="panel-card">
                <h4>教学态势</h4>
                <div class="donut-layout">
                  <div class="donut-chart large" :style="{ background: teacherTeachingChart.gradient }">
                    <div class="donut-center">
                      <strong>{{ teacherTeachingChart.total }}</strong>
                      <span>教学量</span>
                    </div>
                  </div>
                  <div class="chart-legend">
                    <div v-for="item in teacherTeachingChart.items" :key="item.name" class="legend-row">
                      <span class="legend-left">
                        <i class="legend-swatch" :style="{ backgroundColor: item.color }"></i>
                        {{ item.name }}
                      </span>
                      <strong>{{ item.value }} / {{ item.percent }}%</strong>
                    </div>
                  </div>
                </div>
              </div>

              <div class="panel-card">
                <h4>班级完成情况</h4>
                <div class="bar-list">
                  <div v-for="item in teacherDashboardBars" :key="item.label" class="bar-row">
                    <div class="bar-meta">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                    </div>
                    <div class="bar-track">
                      <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-resources'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>发布 / 编辑资源</h4>
                <form class="form-grid" @submit.prevent="saveTeacherResource">
                  <div class="field">
                    <label>资源标题</label>
                    <el-input v-model="teacherResourceForm.title" clearable />
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>资源类型</label>
                      <el-select v-model="teacherResourceForm.type">
                        <el-option label="Word文档" value="文档" />
                        <el-option label="视频" value="视频" />
                      </el-select>
                    </div>
                    <div class="field">
                      <label>上传文件</label>
                      <input type="file" @change="handleTeacherFileChange" :accept="teacherFileAccept" />
                    </div>
                  </div>
                  <div class="field">
                    <label>资源描述</label>
                    <el-input v-model="teacherResourceForm.description" type="textarea" />
                  </div>
                  <div class="field">
                    <label>是否可下载</label>
                    <el-select v-model="teacherResourceForm.downloadable">
                      <el-option label="可下载" :value="true" />
                      <el-option label="仅在线访问" :value="false" />
                    </el-select>
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>可见范围</label>
                      <input value="本班资源" readonly />
                    </div>
                    <div class="field">
                      <label>可见班级</label>
                      <input :value="state.user.className || '未分配班级'" readonly />
                    </div>
                  </div>
                  <div class="muted" v-if="teacherResourceForm.fileName">当前文件：{{ teacherResourceForm.fileName }}</div>
                  <div class="action-row">
                    <el-button type="primary" native-type="submit">{{ teacherResourceForm.id ? '更新资源信息' : '上传并发布资源' }}</el-button>
                    <el-button type="button" plain @click="resetTeacherResourceForm">清空</el-button>
                  </div>
                </form>
              </div>
              <div class="panel-card">
                <h4>我发布的资源</h4>
                <div v-if="!state.teacherResources.length" class="empty-state">暂无资源</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.teacherResources" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.description || '暂无描述' }}</div>
                    <div class="badge-row">
                      <span class="badge">{{ item.type }}</span>
                      <span class="badge" v-if="item.fileName">{{ item.fileName }}</span>
                      <span class="badge">{{ resourceStatusLabel(item.visibility) }}</span>
                      <span class="badge" v-if="item.className">{{ item.className }}</span>
                      <span class="badge">{{ resourceStatusLabel(item.auditStatus) }}</span>
                      <span class="badge">{{ resourceStatusLabel(item.onlineStatus) }}</span>
                    </div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="mini-btn" @click="editTeacherResource(item)">编辑</button>
                      <button class="ghost-btn" @click="previewResource(item)">查看文件</button>
                      <button class="danger-btn" @click="deleteTeacherResource(item.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-homeworks'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>发布 / 编辑作业</h4>
                <form class="form-grid" @submit.prevent="saveTeacherHomework">
                  <div class="field">
                    <label>作业标题</label>
                    <input v-model="teacherHomeworkForm.title" />
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>班级</label>
                      <input :value="state.user.className || '未分配班级'" readonly />
                    </div>
                    <div class="field">
                      <label>截止时间</label>
                      <input v-model="teacherHomeworkForm.dueTime" type="datetime-local" />
                    </div>
                  </div>
                  <div class="field">
                    <label>作业说明</label>
                    <textarea v-model="teacherHomeworkForm.description"></textarea>
                  </div>
                  <div class="field">
                    <label>状态</label>
                    <select v-model="teacherHomeworkForm.status">
                      <option value="PUBLISHED">发布</option>
                      <option value="DRAFT">草稿</option>
                    </select>
                  </div>
                  <div class="action-row">
                    <button class="primary-btn">{{ teacherHomeworkForm.id ? '更新作业' : '发布作业' }}</button>
                    <button type="button" class="ghost-btn" @click="resetTeacherHomeworkForm">清空</button>
                  </div>
                </form>
              </div>
              <div class="panel-card">
                <h4>作业列表</h4>
                <div v-if="!state.teacherHomeworks.length" class="empty-state">暂无作业</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.teacherHomeworks" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.description || '暂无说明' }}</div>
                    <div class="badge-row">
                      <span class="badge">{{ item.className }}</span>
                      <span class="badge">{{ resourceStatusLabel(item.status) }}</span>
                      <span class="badge">提交 {{ item.submissionCount || 0 }}</span>
                      <span class="badge" v-if="item.dueTime">截止 {{ formatTime(item.dueTime) }}</span>
                    </div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="mini-btn" @click="editTeacherHomework(item)">编辑</button>
                      <button class="ghost-btn" @click="loadHomeworkSubmissions(item)">查看提交</button>
                      <button class="danger-btn" @click="deleteTeacherHomework(item.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="panel-card" style="margin-top: 18px;">
              <h4>提交与批改<span v-if="state.activeHomework">：{{ state.activeHomework.title }}</span></h4>
              <div v-if="!state.activeHomework" class="empty-state">请选择一个作业查看提交</div>
              <div v-else-if="!state.homeworkSubmissions.length" class="empty-state">暂无学生提交</div>
              <div v-else class="simple-list">
                <div v-for="item in state.homeworkSubmissions" :key="item.id" class="simple-list-item">
                  <h4>{{ item.studentName }}</h4>
                  <div class="muted">{{ item.content || '暂无提交内容' }}</div>
                  <div class="badge-row">
                    <span class="badge">{{ resourceStatusLabel(item.status) }}</span>
                    <span class="badge">提交时间：{{ formatTime(item.submittedAt) }}</span>
                  </div>
                  <div class="form-grid two-col" style="margin-top: 12px;">
                    <div class="field">
                      <label>分数</label>
                      <input v-model.number="teacherHomeworkGrade[item.id].score" type="number" min="0" max="100" />
                    </div>
                    <div class="field">
                      <label>批改反馈</label>
                      <input v-model="teacherHomeworkGrade[item.id].teacherFeedback" />
                    </div>
                  </div>
                  <button class="primary-btn" style="margin-top: 12px;" @click="gradeHomeworkSubmission(item.id)">保存批改</button>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-exams'">
            <div class="split-grid">
              <div class="panel-card">
                <h4>创建 / 编辑考试</h4>
                <form class="form-grid" @submit.prevent="saveTeacherExam">
                  <div class="field">
                    <label>考试标题</label>
                    <input v-model="teacherExamForm.title" />
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>发布班级</label>
                      <input :value="state.user.className || '未分配班级'" readonly />
                    </div>
                    <div class="field">
                      <label>试卷分类</label>
                      <select v-model="teacherExamForm.type">
                        <option v-for="type in examTypeOptions" :key="type" :value="type">{{ type }}</option>
                      </select>
                    </div>
                    <div class="field">
                      <label>考试时长（分钟）</label>
                      <input v-model.number="teacherExamForm.durationMinutes" type="number" min="1" />
                    </div>
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>随机抽题数量</label>
                      <input v-model.number="teacherExamForm.questionCount" type="number" min="1" />
                      <small class="muted">从同分类启用题库中随机抽取</small>
                    </div>
                    <div class="field">
                      <label>可用题目</label>
                      <input :value="availableQuestionCount(teacherExamForm.type) + ' 道'" readonly />
                    </div>
                  </div>
                  <div class="field">
                    <label>考试说明</label>
                    <textarea v-model="teacherExamForm.description"></textarea>
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>是否发布</label>
                      <select v-model="teacherExamForm.published">
                        <option :value="true">发布</option>
                        <option :value="false">不发布</option>
                      </select>
                    </div>
                    <div class="field">
                      <label>倒计时结束</label>
                      <select v-model="teacherExamForm.autoSubmit">
                        <option :value="true">自动交卷</option>
                        <option :value="false">仅提醒不交卷</option>
                      </select>
                    </div>
                  </div>
                  <div class="action-row">
                    <button class="primary-btn">{{ teacherExamForm.id ? '更新考试' : '创建考试' }}</button>
                    <button type="button" class="ghost-btn" @click="resetTeacherExamForm">清空</button>
                  </div>
                </form>
              </div>
              <div class="panel-card">
                <h4>我的考试</h4>
                <div v-if="!state.teacherExams.length" class="empty-state">暂无考试</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.teacherExams" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.description || '暂无说明' }}</div>
                    <div class="badge-row">
                      <span class="badge">{{ item.type }}</span>
                      <span class="badge">{{ item.className }}</span>
                      <span class="badge">试题 {{ item.questionCount || item.questions.length }} 道</span>
                      <span class="badge">总时长 {{ item.durationMinutes }} 分钟</span>
                      <span class="badge">{{ item.autoSubmit ? '自动交卷' : '手动交卷' }}</span>
                    </div>
                    <div class="muted" style="margin-top: 8px;">
                      已抽题：{{ (item.questions || []).map((question) => question.questionTypeLabel).join('、') || '暂无' }}
                    </div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="mini-btn" @click="editTeacherExam(item)">编辑</button>
                      <button class="danger-btn" @click="deleteTeacherExam(item.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-question-bank'">
            <div class="panel-card">
              <h4>题库维护</h4>
              <form class="form-grid" @submit.prevent="saveExamQuestion">
                <div class="form-grid three-col">
                  <div class="field">
                    <label>题目分类</label>
                    <select v-model="teacherQuestionForm.examType">
                      <option v-for="type in examTypeOptions" :key="type" :value="type">{{ type }}</option>
                    </select>
                  </div>
                  <div class="field">
                    <label>题型</label>
                    <select v-model="teacherQuestionForm.questionType">
                      <option v-for="item in questionTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
                    </select>
                  </div>
                  <div class="field">
                    <label>分值</label>
                    <input v-model.number="teacherQuestionForm.score" type="number" min="1" />
                  </div>
                </div>
                <div class="field">
                  <label>题干</label>
                  <textarea v-model="teacherQuestionForm.content"></textarea>
                </div>
                <div v-if="teacherQuestionForm.questionType === 'LISTENING_CHOICE'" class="field">
                  <label>听力音频</label>
                  <input type="file" accept=".mp3,.wav,.m4a,.ogg,audio/*" @change="handleQuestionAudioChange" />
                  <div class="audio-upload-state" v-if="teacherQuestionForm.audioFileName">
                    <span>{{ teacherQuestionForm.audioFileName }}</span>
                    <button type="button" class="mini-btn" @click="clearQuestionAudio">移除音频</button>
                  </div>
                  <small class="muted">支持 mp3、wav、m4a、ogg，学生端会在试题上方播放。</small>
                </div>
                <div v-if="questionNeedsOptions(teacherQuestionForm.questionType)" class="form-grid four-col">
                  <div class="field">
                    <label>A</label>
                    <input v-model="teacherQuestionForm.optionA" />
                  </div>
                  <div class="field">
                    <label>B</label>
                    <input v-model="teacherQuestionForm.optionB" />
                  </div>
                  <div class="field">
                    <label>C</label>
                    <input v-model="teacherQuestionForm.optionC" />
                  </div>
                  <div class="field">
                    <label>D</label>
                    <input v-model="teacherQuestionForm.optionD" />
                  </div>
                </div>
                <div class="form-grid two-col">
                  <div class="field">
                    <label>标准答案</label>
                    <input v-model="teacherQuestionForm.correctAnswer" :placeholder="questionAnswerPlaceholder" />
                  </div>
                  <div class="field">
                    <label>状态</label>
                    <select v-model="teacherQuestionForm.enabled">
                      <option :value="true">启用</option>
                      <option :value="false">停用</option>
                    </select>
                  </div>
                </div>
                <div class="field">
                  <label>解析</label>
                  <textarea v-model="teacherQuestionForm.analysis"></textarea>
                </div>
                <div class="action-row">
                  <button class="primary-btn">{{ teacherQuestionForm.id ? '更新题目' : '加入题库' }}</button>
                  <button type="button" class="ghost-btn" @click="resetTeacherQuestionForm">清空</button>
                </div>
              </form>
              <div v-if="!state.teacherExamQuestions.length" class="empty-state">暂无题库题目</div>
              <div v-else class="simple-list">
                <div v-for="question in state.teacherExamQuestions" :key="question.id" class="simple-list-item">
                  <h4>[{{ question.examType }} / {{ question.questionTypeLabel }}] {{ question.content }}</h4>
                  <div class="badge-row">
                    <span class="badge">分值 {{ question.score }}</span>
                    <span class="badge">{{ question.enabled ? '启用' : '停用' }}</span>
                    <span class="badge">答案 {{ question.correctAnswer }}</span>
                    <span v-if="question.hasAudio" class="badge">音频 {{ question.audioFileName || '已上传' }}</span>
                  </div>
                  <div class="action-row" style="margin-top: 12px;">
                    <button class="mini-btn" @click="editExamQuestion(question)">编辑</button>
                    <button class="danger-btn" @click="deleteExamQuestion(question.id)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-records'">
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>学生</th>
                    <th>考试</th>
                    <th>成绩</th>
                    <th>教师反馈</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in state.teacherRecords" :key="item.id">
                    <td>{{ item.studentName }}</td>
                    <td>{{ item.examTitle }}</td>
                    <td>{{ item.score }} / {{ item.totalScore }}</td>
                    <td>
                      <textarea v-model="teacherRecordFeedback[item.id]" placeholder="请输入反馈建议"></textarea>
                    </td>
                    <td>
                      <button class="primary-btn" @click="saveRecordFeedback(item.id)">保存反馈</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-ai-advice'">
            <div class="report-hero">
              <div>
                <div class="report-kicker">AI Teaching Insight</div>
                <h3>AI教学建议</h3>
                <p>系统会汇总本班学生、资源、作业、考试成绩和答疑情况，调用本地 Ollama 生成教学分析和后续改进建议。</p>
              </div>
              <div class="report-spark">
                <span v-for="item in teacherAiSparkBars" :key="item.label" :style="{ height: `${item.percent}%`, background: item.color }"></span>
              </div>
            </div>
            <div class="analytics-grid" style="margin-top: 18px;">
              <div v-for="item in teacherAiCards" :key="item.label" class="metric-card metric-card--visual">
                <div class="metric-top">
                  <div class="metric-label">{{ item.label }}</div>
                  <span class="metric-dot" :style="{ backgroundColor: item.color }"></span>
                </div>
                <div class="metric-value">{{ item.value }}</div>
                <div class="metric-progress">
                  <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                </div>
                <div class="metric-hint">{{ item.hint }}</div>
              </div>
            </div>
            <div class="dual-pane" style="margin-top: 18px;">
              <div class="panel-card">
                <h4>生成建议</h4>
                <div class="muted">点击后会调用本地 Ollama，请确认模型服务已启动。</div>
                <div class="action-row" style="margin-top: 16px;">
                  <button class="primary-btn" :disabled="state.loading" @click="generateTeacherAdvice">
                    {{ state.loading ? '生成中...' : '生成AI教学建议' }}
                  </button>
                  <button class="ghost-btn" @click="loadTeacherDashboard">刷新数据</button>
                </div>
                <div v-if="teacherAiDataRows.length" class="simple-list" style="margin-top: 18px;">
                  <div v-for="item in teacherAiDataRows" :key="item.label" class="simple-list-item">
                    <strong>{{ item.label }}</strong>
                    <div class="muted">{{ item.value }}</div>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>建议结果</h4>
                <div v-if="!state.teacherAiAdvice" class="empty-state">AI生成的教学建议会显示在这里</div>
                <div v-else class="simple-list-item">
                  <div class="badge-row">
                    <span class="badge">班级：{{ state.teacherAiAdvice.className }}</span>
                    <span class="badge" v-if="state.teacherAiAdvice.generatedAt">生成：{{ formatTime(state.teacherAiAdvice.generatedAt) }}</span>
                  </div>
                  <div class="ai-answer">{{ state.teacherAiAdvice.answer }}</div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-notifications'">
            <div v-if="!state.teacherNotifications.length" class="empty-state">暂无通知</div>
            <div v-else class="card-list">
              <div v-for="item in state.teacherNotifications" :key="item.id" class="card-item">
                <h4>{{ item.title }}</h4>
                <div class="muted">{{ item.content }}</div>
                <div class="badge-row">
                  <span class="badge">面向：{{ item.targetRole }}</span>
                  <span class="badge">发布人：{{ item.createdByName }}</span>
                  <span class="badge" v-if="item.createdAt">发布时间：{{ formatTime(item.createdAt) }}</span>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'teacher-qa'">
            <div v-if="!state.teacherQaList.length" class="empty-state">暂无学生提问</div>
            <div v-else class="card-list">
              <div v-for="item in state.teacherQaList" :key="item.id" class="card-item">
                <h4>{{ item.title }}</h4>
                <div class="muted">学生：{{ item.studentName }} / 班级：{{ item.className || state.user.className }} / 资源：{{ item.resourceTitle || item.courseTitle || '未关联资源' }}</div>
                <p>{{ item.content }}</p>
                <div v-if="item.answers && item.answers.length" class="simple-list">
                  <div v-for="answer in item.answers" :key="answer.id" class="simple-list-item">
                    <strong>{{ answer.teacherName }}</strong>
                    <div class="muted">{{ answer.content }}</div>
                  </div>
                </div>
                <div class="field" style="margin-top: 12px;">
                  <label>回复内容</label>
                  <textarea v-model="teacherQaAnswer[item.id]" placeholder="请输入答疑内容"></textarea>
                </div>
                <button class="primary-btn" @click="answerQuestion(item.id)">提交答疑</button>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-dashboard'">
            <div class="analytics-grid">
              <div v-for="item in adminDashboardCards" :key="item.label" class="metric-card metric-card--visual">
                <div class="metric-top">
                  <div class="metric-label">{{ item.label }}</div>
                  <span class="metric-dot" :style="{ backgroundColor: item.color }"></span>
                </div>
                <div class="metric-value">{{ item.value }}</div>
                <div class="metric-progress">
                  <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                </div>
                <div class="metric-hint">{{ item.hint }}</div>
              </div>
            </div>
            <div class="split-grid" style="margin-top: 18px;">
              <div class="panel-card">
                <h4>最近日志</h4>
                <div v-if="!(state.adminDashboard.recentLogs || []).length" class="empty-state">暂无日志</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.adminDashboard.recentLogs" :key="item.id" class="simple-list-item">
                    <strong>{{ item.module }} / {{ item.action }}</strong>
                    <div class="muted">{{ item.username || '系统' }} - {{ item.detail }}</div>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>运营概览</h4>
                <div class="donut-layout">
                  <div class="donut-chart" :style="{ background: dashboardRoleChart.gradient }">
                    <div class="donut-center">
                      <strong>{{ dashboardRoleChart.total }}</strong>
                      <span>用户</span>
                    </div>
                  </div>
                  <div class="chart-legend">
                    <div v-for="item in dashboardRoleChart.items" :key="item.name" class="legend-row">
                      <span class="legend-left">
                        <i class="legend-swatch" :style="{ backgroundColor: item.color }"></i>
                        {{ item.name }}
                      </span>
                      <strong>{{ item.value }} / {{ item.percent }}%</strong>
                    </div>
                  </div>
                </div>
                <div class="bar-list compact">
                  <div v-for="item in adminDashboardBars" :key="item.label" class="bar-row">
                    <div class="bar-meta">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                    </div>
                    <div class="bar-track">
                      <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-users'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>创建教师 / 学生账号</h4>
                <form class="form-grid" @submit.prevent="createAdminUser">
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>用户名</label>
                      <input v-model="adminUserForm.username" />
                    </div>
                    <div class="field">
                      <label>初始密码</label>
                      <input v-model="adminUserForm.password" type="password" />
                    </div>
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>姓名</label>
                      <input v-model="adminUserForm.realName" />
                    </div>
                    <div class="field">
                      <label>角色</label>
                      <select v-model="adminUserForm.role">
                        <option value="TEACHER">教师</option>
                        <option value="STUDENT">学生</option>
                        <option value="ADMIN">管理员</option>
                      </select>
                    </div>
                  </div>
                  <div class="form-grid two-col">
                    <div class="field">
                      <label>邮箱</label>
                      <input v-model="adminUserForm.email" />
                    </div>
                    <div class="field">
                      <label>班级</label>
                      <input v-model="adminUserForm.className" :disabled="adminUserForm.role === 'ADMIN'" placeholder="例如：英语2301班" />
                    </div>
                  </div>
                  <div class="field">
                    <label>简介</label>
                    <textarea v-model="adminUserForm.bio"></textarea>
                  </div>
                  <button class="primary-btn">创建账号</button>
                </form>
              </div>
              <div class="panel-card">
                <h4>用户列表</h4>
                <div v-if="!state.adminUsers.length" class="empty-state">暂无用户</div>
                <div v-else class="admin-user-list">
                  <div v-for="item in state.adminUsers" :key="item.id" class="simple-list-item">
                    <div class="form-grid three-col">
                      <div class="field">
                        <label>用户名</label>
                        <input :value="item.username" readonly />
                      </div>
                      <div class="field">
                        <label>姓名</label>
                        <input v-model="item.realName" />
                      </div>
                      <div class="field">
                        <label>角色</label>
                        <select v-model="item.role">
                          <option value="STUDENT">学生</option>
                          <option value="TEACHER">教师</option>
                          <option value="ADMIN">管理员</option>
                        </select>
                      </div>
                    </div>
                    <div class="form-grid three-col" style="margin-top: 12px;">
                      <div class="field">
                        <label>状态</label>
                        <select v-model="item.status">
                          <option value="ACTIVE">启用</option>
                          <option value="DISABLED">禁用</option>
                        </select>
                      </div>
                      <div class="field">
                        <label>邮箱</label>
                        <input v-model="item.email" />
                      </div>
                      <div class="field">
                        <label>班级</label>
                        <input v-model="item.className" :disabled="item.role === 'ADMIN'" />
                      </div>
                    </div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="mini-btn" @click="saveAdminUser(item)">保存</button>
                      <button class="danger-btn" @click="resetAdminUserPassword(item.id)">重置密码</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-resources'">
            <div v-if="!state.adminResources.length" class="empty-state">暂无资源</div>
            <div v-else class="card-list">
              <div v-for="item in state.adminResources" :key="item.id" class="card-item">
                <h4>{{ item.title }}</h4>
                <div class="muted">{{ item.description || '暂无描述' }}</div>
                <div class="badge-row">
                  <span class="badge">{{ item.type }}</span>
                  <span class="badge">发布人：{{ item.creatorName }}</span>
                  <span class="badge">{{ resourceStatusLabel(item.visibility) }}</span>
                  <span class="badge" v-if="item.className">{{ item.className }}</span>
                  <span class="badge">{{ resourceStatusLabel(item.auditStatus) }}</span>
                  <span class="badge">{{ resourceStatusLabel(item.onlineStatus) }}</span>
                  <span class="badge" v-if="item.fileName">{{ item.fileName }}</span>
                </div>
                <div class="action-row" style="margin-top: 12px;">
                  <button class="primary-btn" @click="auditAdminResource(item.id, 'APPROVED')">审核通过</button>
                  <button class="danger-btn" @click="auditAdminResource(item.id, 'REJECTED')">驳回</button>
                  <button class="ghost-btn" @click="updateAdminResourceOnlineStatus(item.id, 'PUBLISHED')">上架</button>
                  <button class="ghost-btn" @click="updateAdminResourceOnlineStatus(item.id, 'DRAFT')">下架</button>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-notifications'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>发布公告</h4>
                <form class="form-grid" @submit.prevent="saveNotification">
                  <div class="field">
                    <label>公告标题</label>
                    <input v-model="notificationForm.title" />
                  </div>
                  <div class="field">
                    <label>目标角色</label>
                    <select v-model="notificationForm.targetRole">
                      <option value="ALL">全平台</option>
                      <option value="STUDENT">学生</option>
                      <option value="TEACHER">教师</option>
                      <option value="ADMIN">管理员</option>
                    </select>
                  </div>
                  <div class="field">
                    <label>公告内容</label>
                    <textarea v-model="notificationForm.content"></textarea>
                  </div>
                  <button class="primary-btn">发布公告</button>
                </form>
              </div>
              <div class="panel-card">
                <h4>公告列表</h4>
                <div v-if="!state.adminNotifications.length" class="empty-state">暂无公告</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.adminNotifications" :key="item.id" class="simple-list-item">
                    <h4>{{ item.title }}</h4>
                    <div class="muted">{{ item.content }}</div>
                    <div class="action-row" style="margin-top: 12px;">
                      <button class="danger-btn" @click="deleteNotification(item.id)">删除</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-feedback'">
            <div v-if="!state.adminFeedback.length" class="empty-state">暂无反馈</div>
            <div v-else class="card-list">
              <div v-for="item in state.adminFeedback" :key="item.id" class="card-item">
                <h4>{{ item.username }}（{{ item.role }}）</h4>
                <div>{{ item.content }}</div>
                <div class="badge-row">
                  <span class="badge">{{ item.status }}</span>
                </div>
                <div class="field" style="margin-top: 12px;">
                  <label>回复内容</label>
                  <textarea v-model="adminFeedbackReply[item.id]" placeholder="请输入处理结果"></textarea>
                </div>
                <button class="primary-btn" @click="replyFeedback(item.id)">提交回复</button>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-logs'">
            <div class="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>用户</th>
                    <th>模块</th>
                    <th>动作</th>
                    <th>详情</th>
                    <th>时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in state.adminLogs" :key="item.id">
                    <td>{{ item.username || '系统' }}</td>
                    <td>{{ item.module }}</td>
                    <td>{{ item.action }}</td>
                    <td>{{ item.detail }}</td>
                    <td>{{ formatTime(item.createdAt) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-configs'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>系统参数</h4>
                <div v-if="!state.adminConfigs.length" class="empty-state">暂无参数</div>
                <div v-else class="simple-list">
                  <div v-for="item in state.adminConfigs" :key="item.id" class="simple-list-item">
                    <strong>{{ item.configKey }}</strong>
                    <div class="muted">{{ item.description }}</div>
                    <div class="field" style="margin-top: 10px;">
                      <input v-model="item.configValue" />
                    </div>
                    <button class="mini-btn" style="margin-top: 10px;" @click="saveConfig(item)">保存参数</button>
                  </div>
                </div>
              </div>
              <div class="panel-card">
                <h4>系统维护</h4>
                <div class="simple-list">
                  <div class="simple-list-item">这里的“备份”是演示型登记操作，会把最近一次备份时间写入系统参数表。</div>
                </div>
                <button class="primary-btn" style="margin-top: 12px;" @click="runBackup">登记一次备份</button>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'admin-report'">
            <div class="report-hero">
              <div>
                <div class="report-kicker">运营数据看板</div>
                <h3>平台运行概览</h3>
                <p>汇总用户、资源、考试、反馈和答疑数据，用图形化方式展示平台活跃度。</p>
              </div>
              <div class="report-spark">
                <span
                  v-for="item in adminReportBars"
                  :key="item.label"
                  :style="{ height: `${Math.max(item.percent, 10)}%`, background: item.color }"
                ></span>
              </div>
            </div>

            <div class="analytics-grid" style="margin-top: 18px;">
              <div v-for="item in adminReportCards" :key="item.label" class="metric-card metric-card--visual">
                <div class="metric-top">
                  <div class="metric-label">{{ item.label }}</div>
                  <span class="metric-dot" :style="{ backgroundColor: item.color }"></span>
                </div>
                <div class="metric-value">{{ item.value }}</div>
                <div class="metric-progress">
                  <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                </div>
                <div class="metric-hint">{{ item.hint }}</div>
              </div>
            </div>

            <div class="report-grid" style="margin-top: 18px;">
              <div class="panel-card">
                <h4>角色分布</h4>
                <div class="donut-layout">
                  <div class="donut-chart large" :style="{ background: reportRoleChart.gradient }">
                    <div class="donut-center">
                      <strong>{{ reportRoleChart.total }}</strong>
                      <span>账号</span>
                    </div>
                  </div>
                  <div class="chart-legend">
                    <div v-for="item in reportRoleChart.items" :key="item.name" class="legend-row">
                      <span class="legend-left">
                        <i class="legend-swatch" :style="{ backgroundColor: item.color }"></i>
                        {{ item.name }}
                      </span>
                      <strong>{{ item.value }} / {{ item.percent }}%</strong>
                    </div>
                  </div>
                </div>
              </div>

              <div class="panel-card">
                <h4>业务规模</h4>
                <div class="bar-list">
                  <div v-for="item in adminReportBars" :key="item.label" class="bar-row">
                    <div class="bar-meta">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                    </div>
                    <div class="bar-track">
                      <span :style="{ width: `${item.percent}%`, background: item.color }"></span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <template v-else-if="state.currentView === 'profile'">
            <div class="dual-pane">
              <div class="panel-card">
                <h4>个人资料</h4>
                <form class="form-grid" @submit.prevent="saveProfile">
                  <div class="field">
                    <label>姓名</label>
                    <input v-model="profileForm.realName" />
                  </div>
                  <div class="field">
                    <label>邮箱</label>
                    <input v-model="profileForm.email" />
                  </div>
                  <div class="field">
                    <label>班级</label>
                    <input v-model="profileForm.className" readonly />
                    <small class="muted">班级由管理员统一分配</small>
                  </div>
                  <div class="field">
                    <label>个人简介</label>
                    <textarea v-model="profileForm.bio"></textarea>
                  </div>
                  <button class="primary-btn">保存资料</button>
                </form>
              </div>
              <div class="panel-card">
                <h4>修改密码</h4>
                <form class="form-grid" @submit.prevent="savePassword">
                  <div class="field">
                    <label>旧密码</label>
                    <input v-model="passwordForm.oldPassword" type="password" />
                  </div>
                  <div class="field">
                    <label>新密码</label>
                    <input v-model="passwordForm.newPassword" type="password" />
                  </div>
                  <button class="primary-btn">修改密码</button>
                </form>
              </div>
            </div>
          </template>
        </section>

      </main>
    </div>

    <div v-if="state.preview.visible" class="preview-mask" @click.self="closePreview">
      <div class="preview-dialog">
        <div class="preview-head">
          <div>
            <h3>{{ state.preview.title }}</h3>
            <p>{{ state.preview.type }}</p>
          </div>
          <button class="danger-btn" @click="closePreview">关闭</button>
        </div>

        <div class="preview-body">
          <video
            v-if="state.preview.mode === 'video'"
            class="preview-video"
            :src="state.preview.src"
            controls
          ></video>

          <div
            v-else-if="state.preview.mode === 'docx'"
            ref="docxPreviewRef"
            class="preview-docx"
          ></div>

          <div v-else class="empty-state">当前资源暂不支持预览</div>
        </div>

        <div class="action-row" style="margin-top: 16px;">
          <button class="primary-btn" @click="downloadPreviewResource">下载当前资源</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import mammothBrowser from 'mammoth/mammoth.browser.js'
import { computed, nextTick, reactive, ref, watch } from 'vue'
import request from './api'

const menus = {
  STUDENT: [
    { key: 'student-dashboard', label: '学生首页', desc: '查看学习概览和通知' },
    { key: 'resources', label: '学习资源', desc: '浏览、搜索和打开资源' },
    { key: 'plans', label: '学习计划', desc: '管理个人学习计划和进度' },
    { key: 'student-homeworks', label: '班级作业', desc: '查看本班作业并提交完成情况' },
    { key: 'student-exams', label: '在线考试', desc: '参加入学测试和模拟考试' },
    { key: 'student-records', label: '成绩记录', desc: '查看历史成绩和教师反馈' },
    { key: 'vocabulary', label: '单词闯关', desc: '卡片式背词、记录掌握情况和错词复习' },
    { key: 'dictionary', label: 'AI词典', desc: '调用本地 Ollama 进行英文释义' },
    { key: 'student-notifications', label: '通知公告', desc: '查看平台通知' },
    { key: 'student-feedback', label: '意见反馈', desc: '提交平台反馈并查看处理结果' },
    { key: 'student-qa', label: '在线答疑', desc: '向教师发起课程问题' },
    { key: 'student-forum', label: '班级论坛', desc: '和同班同学发帖交流' },
    { key: 'profile', label: '个人资料', desc: '修改个人信息和密码' }
  ],
  TEACHER: [
    { key: 'teacher-dashboard', label: '教师首页', desc: '查看教学概览' },
    { key: 'teacher-resources', label: '资源管理', desc: '发布和维护教学资源' },
    { key: 'teacher-homeworks', label: '作业管理', desc: '发布班级作业并批改学生提交' },
    { key: 'teacher-exams', label: '考试管理', desc: '创建和维护本班考试' },
    { key: 'teacher-question-bank', label: '题库维护', desc: '维护考试题目和听力音频' },
    { key: 'teacher-records', label: '成绩反馈', desc: '查看学生成绩并给出反馈' },
    { key: 'teacher-ai-advice', label: 'AI教学建议', desc: '分析本班教学数据并生成改进建议' },
    { key: 'teacher-notifications', label: '通知公告', desc: '查看管理员发布的平台公告' },
    { key: 'teacher-qa', label: '在线答疑', desc: '回答学生问题' },
    { key: 'profile', label: '个人资料', desc: '修改个人信息和密码' }
  ],
  ADMIN: [
    { key: 'admin-dashboard', label: '管理首页', desc: '查看平台总体运行情况' },
    { key: 'admin-users', label: '用户权限', desc: '创建教师账号并维护用户班级' },
    { key: 'admin-resources', label: '资源审核', desc: '审核资源并控制上下架状态' },
    { key: 'admin-notifications', label: '公告管理', desc: '发布和删除全平台公告' },
    { key: 'admin-feedback', label: '反馈处理', desc: '处理用户反馈' },
    { key: 'admin-logs', label: '操作日志', desc: '查看最近操作记录' },
    { key: 'admin-configs', label: '系统参数', desc: '维护配置和备份时间' },
    { key: 'admin-report', label: '运营报表', desc: '查看平台统计数据' },
    { key: 'profile', label: '个人资料', desc: '修改个人信息和密码' }
  ]
}

const state = reactive({
  token: localStorage.getItem('token') || '',
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  authMode: 'login',
  currentView: 'student-dashboard',
  loading: false,
  error: '',
  message: '',
  resourceKeyword: '',
  forumKeyword: '',
  studentDashboard: {},
  teacherDashboard: {},
  adminDashboard: {},
  resources: [],
  qaResources: [],
  plans: [],
  studentHomeworks: [],
  studentExams: [],
  currentExam: null,
  examRemainingSeconds: 0,
  examSessionStatus: '',
  examAudioUrls: {},
  records: [],
  vocabularyWords: [],
  vocabularyAllWords: [],
  vocabularyStats: {},
  vocabularyMode: 'all',
  vocabularyIndex: 0,
  dictionaryResult: null,
  notifications: [],
  feedbackList: [],
  qaList: [],
  forumPosts: [],
  currentForumPost: null,
  forumLoading: false,
  teacherResources: [],
  teacherHomeworks: [],
  homeworkSubmissions: [],
  activeHomework: null,
  teacherExams: [],
  teacherExamQuestions: [],
  teacherRecords: [],
  teacherAiAdvice: null,
  teacherNotifications: [],
  teacherQaList: [],
  adminUsers: [],
  adminResources: [],
  adminNotifications: [],
  adminFeedback: [],
  adminLogs: [],
  adminConfigs: [],
  adminReport: {},
  preview: {
    visible: false,
    resourceId: null,
    title: '',
    type: '',
    mode: '',
    src: '',
    objectUrl: '',
    rolePath: ''
  }
})

const loginForm = reactive({ username: 'student1', password: '123456' })
const registerForm = reactive({ username: '', password: '', realName: '', email: '', className: '' })
const profileForm = reactive({ realName: '', email: '', className: '', bio: '' })
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const planForm = reactive({ id: null, title: '', targetContent: '', progress: 0, status: 'TODO', dueDate: '' })
const dictionaryForm = reactive({ query: '' })
const feedbackForm = reactive({ content: '' })
const qaForm = reactive({ resourceId: null, title: '', content: '' })
const forumForm = reactive({ title: '', content: '' })
const forumReplyForm = reactive({ content: '' })
const homeworkSubmitForm = reactive({ homeworkId: null, content: '' })
const teacherResourceForm = reactive({
  id: null,
  title: '',
  description: '',
  type: '文档',
  url: '',
  downloadable: true,
  visibility: 'PUBLIC',
  className: '',
  file: null,
  fileName: ''
})
const teacherHomeworkForm = reactive({ id: null, title: '', description: '', className: '', dueTime: '', status: 'PUBLISHED' })
const teacherHomeworkGrade = reactive({})
const teacherExamForm = reactive({
  id: null,
  title: '',
  description: '',
  type: '大学四级',
  questionCount: 5,
  durationMinutes: 30,
  autoSubmit: true,
  published: true
})
const teacherQuestionForm = reactive({
  id: null,
  examType: '大学四级',
  questionType: 'SINGLE_CHOICE',
  content: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctAnswer: '',
  analysis: '',
  audioFileName: '',
  audioStoredName: '',
  audioContentType: '',
  audioFileSize: null,
  score: 10,
  enabled: true
})
const notificationForm = reactive({ title: '', content: '', targetRole: 'ALL' })
const examAnswers = reactive({})
let examTimer = null
const teacherRecordFeedback = reactive({})
const teacherQaAnswer = reactive({})
const adminFeedbackReply = reactive({})
const adminUserForm = reactive({
  username: '',
  password: '123456',
  realName: '',
  role: 'TEACHER',
  email: '',
  className: '',
  bio: ''
})
const docxPreviewRef = ref(null)

const currentMenus = computed(() => menus[state.user?.role] || [])
const currentTitle = computed(() => currentMenus.value.find((item) => item.key === state.currentView)?.label || '功能页面')
const currentDescription = computed(() => currentMenus.value.find((item) => item.key === state.currentView)?.desc || '平台功能')
const nowText = computed(() => new Date().toLocaleString('zh-CN'))
const teacherFileAccept = computed(() => (teacherResourceForm.type === '视频' ? '.mp4,.avi,.mov,.wmv' : '.doc,.docx'))
const examTypeOptions = ['大学四级', '大学六级', '考研英语', '听力考试']
const questionTypeOptions = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'LISTENING_CHOICE', label: '听力题' },
  { value: 'TRUE_FALSE', label: '判断题' },
  { value: 'FILL_BLANK', label: '填空题' },
  { value: 'SHORT_ANSWER', label: '简答题' }
]
const questionAnswerPlaceholder = computed(() => {
  if (teacherQuestionForm.questionType === 'MULTIPLE_CHOICE') return '多个选项用逗号分隔，例如 A,C'
  if (teacherQuestionForm.questionType === 'LISTENING_CHOICE') return '填写正确选项，例如 A'
  if (teacherQuestionForm.questionType === 'TRUE_FALSE') return '正确填 A，错误填 B'
  if (teacherQuestionForm.questionType === 'FILL_BLANK') return '填写标准文本答案'
  if (teacherQuestionForm.questionType === 'SHORT_ANSWER') return '填写参考答案，当前按文本精确匹配'
  return '填写正确选项，例如 A'
})
const chartColors = ['#0f6f7f', '#e39f3a', '#2d8b6f', '#c8553d', '#62748a']
const studentDashboardCards = computed(() => {
  const dashboard = state.studentDashboard || {}
  return normalizeMetricCards([
    { label: '学习资源', value: dashboard.resourceCount, hint: '本班可学资源', color: chartColors[0] },
    { label: '学习计划', value: dashboard.planCount, hint: '个人计划数量', color: chartColors[1] },
    { label: '已做考试', value: dashboard.recordCount, hint: '累计考试记录', color: chartColors[2] },
    { label: '可参加考试', value: dashboard.examCount, hint: '当前开放考试', color: chartColors[3] }
  ])
})
const studentDashboardBars = computed(() => normalizeBarItems([
  { label: '学习资源', value: state.studentDashboard?.resourceCount, color: chartColors[0] },
  { label: '学习计划', value: state.studentDashboard?.planCount, color: chartColors[1] },
  { label: '已做考试', value: state.studentDashboard?.recordCount, color: chartColors[2] },
  { label: '可参加考试', value: state.studentDashboard?.examCount, color: chartColors[3] },
  { label: '近期通知', value: (state.studentDashboard?.notifications || []).length, color: chartColors[4] }
]))
const studentLearningChart = computed(() => buildRoleChart([
  { name: '学习资源', value: state.studentDashboard?.resourceCount },
  { name: '学习计划', value: state.studentDashboard?.planCount },
  { name: '考试记录', value: state.studentDashboard?.recordCount },
  { name: '开放考试', value: state.studentDashboard?.examCount }
]))
const availableStudentExams = computed(() => state.studentExams.filter((item) => !item.completed))
const completedStudentExams = computed(() => state.studentExams.filter((item) => item.completed))
const currentVocabularyWord = computed(() => state.vocabularyWords[state.vocabularyIndex] || null)
const vocabularyReviewWords = computed(() => state.vocabularyAllWords.filter((item) => item.status === 'REVIEW'))
const vocabularyProgressPercent = computed(() => {
  const total = state.vocabularyWords.length || 1
  return Math.round(((state.vocabularyIndex + 1) / total) * 100)
})
const vocabularyMetricCards = computed(() => {
  const stats = state.vocabularyStats || {}
  return normalizeMetricCards([
    { label: '词库总量', value: stats.totalCount, hint: '内置四级核心词', color: chartColors[0] },
    { label: '已学单词', value: stats.studiedCount, hint: '已有学习记录', color: chartColors[1] },
    { label: '已掌握', value: stats.masteredCount, hint: '最近标记认识', color: chartColors[2] },
    { label: '正确率', value: stats.accuracyRate, hint: '认识次数占比', color: chartColors[3], max: 100 }
  ])
})
const teacherDashboardCards = computed(() => {
  const dashboard = state.teacherDashboard || {}
  return normalizeMetricCards([
    { label: '我的资源', value: dashboard.resourceCount, hint: '已发布资源', color: chartColors[0] },
    { label: '我的考试', value: dashboard.examCount, hint: '已创建考试', color: chartColors[1] },
    { label: '待答疑问题', value: dashboard.qaCount, hint: '等待回复', color: chartColors[3] },
    { label: '平均分', value: Number(dashboard.averageScore || 0).toFixed(1), hint: '本班考试均分', color: chartColors[2], max: 100 }
  ])
})
const teacherDashboardBars = computed(() => normalizeBarItems([
  { label: '本班学生', value: state.teacherDashboard?.studentCount, color: chartColors[0] },
  { label: '作业总数', value: state.teacherDashboard?.homeworkCount, color: chartColors[1] },
  { label: '作业提交', value: state.teacherDashboard?.submittedHomeworkCount, color: chartColors[2] },
  { label: '已批改', value: state.teacherDashboard?.gradedHomeworkCount, color: chartColors[3] },
  { label: '考试参与', value: state.teacherDashboard?.examParticipation, color: chartColors[4] }
]))
const teacherTeachingChart = computed(() => buildRoleChart([
  { name: '教学资源', value: state.teacherDashboard?.resourceCount },
  { name: '考试安排', value: state.teacherDashboard?.examCount },
  { name: '班级作业', value: state.teacherDashboard?.homeworkCount },
  { name: '待答疑', value: state.teacherDashboard?.qaCount }
]))
const teacherAiCards = computed(() => normalizeMetricCards([
  { label: '本班学生', value: state.teacherDashboard?.studentCount, hint: '当前班级人数', color: chartColors[0] },
  { label: '考试参与', value: state.teacherDashboard?.examParticipation, hint: '累计交卷记录', color: chartColors[1] },
  { label: '作业提交', value: state.teacherDashboard?.submittedHomeworkCount, hint: '累计作业提交', color: chartColors[2] },
  { label: '待答疑', value: state.teacherDashboard?.qaCount, hint: '需要教师回复', color: chartColors[3] }
]))
const teacherAiSparkBars = computed(() => normalizeBarItems([
  { label: '资源', value: state.teacherDashboard?.resourceCount, color: chartColors[0] },
  { label: '考试', value: state.teacherDashboard?.examCount, color: chartColors[1] },
  { label: '作业', value: state.teacherDashboard?.homeworkCount, color: chartColors[2] },
  { label: '交卷', value: state.teacherDashboard?.examParticipation, color: chartColors[3] },
  { label: '答疑', value: state.teacherDashboard?.qaCount, color: chartColors[4] }
]))
const teacherAiDataRows = computed(() => {
  const data = state.teacherAiAdvice?.data || state.teacherDashboard || {}
  return [
    { label: '资源建设', value: `已发布 ${data.resourceCount || 0} 个资源` },
    { label: '考试情况', value: `考试 ${data.examCount || 0} 场，参与记录 ${data.examRecordCount ?? data.examParticipation ?? 0} 条，平均分 ${data.averageScore || 0}` },
    { label: '作业情况', value: `作业 ${data.homeworkCount || 0} 次，提交 ${data.homeworkSubmissionCount ?? data.submittedHomeworkCount ?? 0} 次，已批改 ${data.gradedHomeworkCount || 0} 次` },
    { label: '答疑情况', value: `待答疑 ${data.openQuestionCount ?? data.qaCount ?? 0} 个，已答疑 ${data.answeredQuestionCount || 0} 个` }
  ]
})
const adminDashboardCards = computed(() => {
  const dashboard = state.adminDashboard || {}
  return normalizeMetricCards([
    { label: '平台用户', value: dashboard.userCount, hint: '已创建账号', color: chartColors[0] },
    { label: '教学资源', value: dashboard.resourceCount, hint: '资源库规模', color: chartColors[1] },
    { label: '考试记录', value: dashboard.examRecordCount, hint: '累计作答记录', color: chartColors[2] },
    { label: '待处理反馈', value: dashboard.pendingFeedbackCount, hint: '需要管理员处理', color: chartColors[3] }
  ])
})
const adminDashboardBars = computed(() => normalizeBarItems([
  { label: '资源总数', value: state.adminDashboard?.resourceCount, color: chartColors[1] },
  { label: '考试总数', value: state.adminDashboard?.examCount, color: chartColors[0] },
  { label: '考试记录', value: state.adminDashboard?.examRecordCount, color: chartColors[2] },
  { label: '待处理反馈', value: state.adminDashboard?.pendingFeedbackCount, color: chartColors[3] },
  { label: '待答疑问题', value: state.adminDashboard?.openQuestionCount, color: chartColors[4] }
]))
const adminReportCards = computed(() => {
  const report = state.adminReport || {}
  return normalizeMetricCards([
    { label: '资源总数', value: report.resourceCount, hint: '教学内容沉淀', color: chartColors[1] },
    { label: '考试总数', value: report.examCount, hint: '已创建考试', color: chartColors[0] },
    { label: '考试记录', value: report.recordCount, hint: '学生参与记录', color: chartColors[2] },
    { label: '已答疑问题', value: report.answeredQuestionCount, hint: '教师已回复', color: chartColors[3] }
  ])
})
const adminReportBars = computed(() => normalizeBarItems([
  { label: '资源总数', value: state.adminReport?.resourceCount, color: chartColors[1] },
  { label: '考试总数', value: state.adminReport?.examCount, color: chartColors[0] },
  { label: '考试记录', value: state.adminReport?.recordCount, color: chartColors[2] },
  { label: '待处理反馈', value: state.adminReport?.pendingFeedbackCount, color: chartColors[3] },
  { label: '已答疑问题', value: state.adminReport?.answeredQuestionCount, color: chartColors[4] }
]))
const dashboardRoleChart = computed(() => buildRoleChart(state.adminDashboard?.report?.roleDistribution || []))
const reportRoleChart = computed(() => buildRoleChart(state.adminReport?.roleDistribution || []))
const examRemainingText = computed(() => {
  const seconds = Math.max(Number(state.examRemainingSeconds || 0), 0)
  const minutes = Math.floor(seconds / 60)
  const rest = String(seconds % 60).padStart(2, '0')
  return `${minutes}:${rest}`
})

function normalizeMetricCards(items) {
  const maxValue = Math.max(...items.map((item) => Number(item.value || 0)), 1)
  return items.map((item) => {
    const value = Number(item.value || 0)
    const percentBase = Number(item.max || maxValue)
    return {
      ...item,
      value,
      percent: Math.max(Math.round((value / percentBase) * 100), value > 0 ? 8 : 0)
    }
  })
}

function normalizeBarItems(items) {
  const maxValue = Math.max(...items.map((item) => Number(item.value || 0)), 1)
  return items.map((item) => {
    const value = Number(item.value || 0)
    return {
      ...item,
      value,
      percent: Math.max(Math.round((value / maxValue) * 100), value > 0 ? 6 : 0)
    }
  })
}

function buildRoleChart(items) {
  const rows = (items || []).map((item, index) => ({
    name: item.name,
    value: Number(item.value || 0),
    color: chartColors[index % chartColors.length]
  }))
  const total = rows.reduce((sum, item) => sum + item.value, 0)
  let cursor = 0
  const segments = rows.map((item) => {
    const start = total ? (cursor / total) * 100 : 0
    cursor += item.value
    const end = total ? (cursor / total) * 100 : 0
    return `${item.color} ${start}% ${end}%`
  })
  return {
    total,
    items: rows.map((item) => ({
      ...item,
      percent: total ? Math.round((item.value / total) * 100) : 0
    })),
    gradient: total ? `conic-gradient(${segments.join(', ')})` : 'conic-gradient(rgba(15, 111, 127, 0.14) 0 100%)'
  }
}

function roleLabel(role) {
  return { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[role] || role
}

function wordStatusLabel(status) {
  return { NEW: '未学习', MASTERED: '已掌握', REVIEW: '待复习' }[status] || status || '未学习'
}

function setError(error) {
  state.error = error?.message || '操作失败'
  ElMessage.error(state.error)
}

function setMessage(message) {
  state.message = message
  ElMessage.success(message)
  window.setTimeout(() => {
    state.message = ''
  }, 2500)
}

function persistLogin(data) {
  state.token = data.token
  state.user = data.user
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
  initProfileForm()
  state.currentView = menus[data.user.role][0].key
  if (data.user.role === 'TEACHER') {
    resetTeacherResourceForm()
    resetTeacherHomeworkForm()
  }
}

function logout() {
  stopExamTimer()
  revokeExamAudioUrls()
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  state.token = ''
  state.user = null
  state.currentView = 'student-dashboard'
  state.error = ''
  state.message = ''
  state.forumKeyword = ''
  state.forumPosts = []
  state.currentForumPost = null
  forumForm.title = ''
  forumForm.content = ''
  forumReplyForm.content = ''
}

async function handleLogin() {
  state.loading = true
  state.error = ''
  try {
    const data = await request.post('/api/auth/login', loginForm)
    persistLogin(data)
    await loadCurrentView()
  } catch (error) {
    setError(error)
  } finally {
    state.loading = false
  }
}

async function handleRegister() {
  state.loading = true
  state.error = ''
  try {
    await request.post('/api/auth/register', registerForm)
    setMessage('注册成功，请直接使用新账号登录')
    state.authMode = 'login'
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
    registerForm.username = ''
    registerForm.password = ''
    registerForm.realName = ''
    registerForm.email = ''
    registerForm.className = ''
  } catch (error) {
    setError(error)
  } finally {
    state.loading = false
  }
}

function initProfileForm() {
  profileForm.realName = state.user?.realName || ''
  profileForm.email = state.user?.email || ''
  profileForm.className = state.user?.className || ''
  profileForm.bio = state.user?.bio || ''
}

function resetPlanForm() {
  planForm.id = null
  planForm.title = ''
  planForm.targetContent = ''
  planForm.progress = 0
  planForm.status = 'TODO'
  planForm.dueDate = ''
}

function editPlan(item) {
  planForm.id = item.id
  planForm.title = item.title
  planForm.targetContent = item.targetContent || ''
  planForm.progress = item.progress
  planForm.status = item.status
  planForm.dueDate = item.dueDate || ''
}

function resetTeacherResourceForm() {
  teacherResourceForm.id = null
  teacherResourceForm.title = ''
  teacherResourceForm.description = ''
  teacherResourceForm.type = '文档'
  teacherResourceForm.url = ''
  teacherResourceForm.downloadable = true
  teacherResourceForm.visibility = 'CLASS'
  teacherResourceForm.className = state.user?.className || ''
  teacherResourceForm.file = null
  teacherResourceForm.fileName = ''
}

function editTeacherResource(item) {
  teacherResourceForm.id = item.id
  teacherResourceForm.title = item.title
  teacherResourceForm.description = item.description || ''
  teacherResourceForm.type = item.type
  teacherResourceForm.url = item.url
  teacherResourceForm.downloadable = item.downloadable
  teacherResourceForm.visibility = 'CLASS'
  teacherResourceForm.className = item.className || state.user?.className || ''
  teacherResourceForm.file = null
  teacherResourceForm.fileName = item.fileName || ''
}

function handleTeacherFileChange(event) {
  const file = event.target.files?.[0]
  teacherResourceForm.file = file || null
  teacherResourceForm.fileName = file ? file.name : ''
}

function resetHomeworkSubmitForm() {
  homeworkSubmitForm.homeworkId = null
  homeworkSubmitForm.content = ''
}

function editHomeworkSubmission(homework) {
  homeworkSubmitForm.homeworkId = homework.id
  homeworkSubmitForm.content = homework.mySubmission?.content || ''
}

function resetTeacherHomeworkForm() {
  teacherHomeworkForm.id = null
  teacherHomeworkForm.title = ''
  teacherHomeworkForm.description = ''
  teacherHomeworkForm.className = state.user?.className || ''
  teacherHomeworkForm.dueTime = ''
  teacherHomeworkForm.status = 'PUBLISHED'
}

function editTeacherHomework(item) {
  teacherHomeworkForm.id = item.id
  teacherHomeworkForm.title = item.title
  teacherHomeworkForm.description = item.description || ''
  teacherHomeworkForm.className = item.className || ''
  teacherHomeworkForm.dueTime = formatDateTimeInput(item.dueTime)
  teacherHomeworkForm.status = item.status || 'PUBLISHED'
}

function formatDateTimeInput(value) {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 16)
}

function resetTeacherExamForm() {
  teacherExamForm.id = null
  teacherExamForm.title = ''
  teacherExamForm.description = ''
  teacherExamForm.type = '大学四级'
  teacherExamForm.questionCount = 5
  teacherExamForm.durationMinutes = 30
  teacherExamForm.autoSubmit = true
  teacherExamForm.published = true
}

function editTeacherExam(item) {
  teacherExamForm.id = item.id
  teacherExamForm.title = item.title
  teacherExamForm.description = item.description || ''
  teacherExamForm.type = item.type
  teacherExamForm.questionCount = item.questionCount || item.questions?.length || 1
  teacherExamForm.durationMinutes = item.durationMinutes
  teacherExamForm.autoSubmit = item.autoSubmit !== false
  teacherExamForm.published = item.published
}

function resetTeacherQuestionForm() {
  teacherQuestionForm.id = null
  teacherQuestionForm.examType = '大学四级'
  teacherQuestionForm.questionType = 'SINGLE_CHOICE'
  teacherQuestionForm.content = ''
  teacherQuestionForm.optionA = ''
  teacherQuestionForm.optionB = ''
  teacherQuestionForm.optionC = ''
  teacherQuestionForm.optionD = ''
  teacherQuestionForm.correctAnswer = ''
  teacherQuestionForm.analysis = ''
  teacherQuestionForm.audioFileName = ''
  teacherQuestionForm.audioStoredName = ''
  teacherQuestionForm.audioContentType = ''
  teacherQuestionForm.audioFileSize = null
  teacherQuestionForm.score = 10
  teacherQuestionForm.enabled = true
}

function resetAdminUserForm() {
  adminUserForm.username = ''
  adminUserForm.password = '123456'
  adminUserForm.realName = ''
  adminUserForm.role = 'TEACHER'
  adminUserForm.email = ''
  adminUserForm.className = ''
  adminUserForm.bio = ''
}

function editExamQuestion(question) {
  teacherQuestionForm.id = question.id
  teacherQuestionForm.examType = question.examType || '大学四级'
  teacherQuestionForm.questionType = question.questionType || 'SINGLE_CHOICE'
  teacherQuestionForm.content = question.content || ''
  teacherQuestionForm.optionA = question.optionA || ''
  teacherQuestionForm.optionB = question.optionB || ''
  teacherQuestionForm.optionC = question.optionC || ''
  teacherQuestionForm.optionD = question.optionD || ''
  teacherQuestionForm.correctAnswer = question.correctAnswer || ''
  teacherQuestionForm.analysis = question.analysis || ''
  teacherQuestionForm.audioFileName = question.audioFileName || ''
  teacherQuestionForm.audioStoredName = question.audioStoredName || ''
  teacherQuestionForm.audioContentType = question.audioContentType || ''
  teacherQuestionForm.audioFileSize = question.audioFileSize || null
  teacherQuestionForm.score = question.score || 10
  teacherQuestionForm.enabled = question.enabled !== false
}

function questionNeedsOptions(questionType) {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'LISTENING_CHOICE', 'TRUE_FALSE'].includes(questionType)
}

watch(
  () => teacherQuestionForm.questionType,
  (questionType) => {
    if (questionType === 'TRUE_FALSE') {
      teacherQuestionForm.optionA = '正确'
      teacherQuestionForm.optionB = '错误'
      teacherQuestionForm.optionC = ''
      teacherQuestionForm.optionD = ''
    }
    if (questionType === 'LISTENING_CHOICE') {
      teacherQuestionForm.examType = '听力考试'
    }
    if (!questionNeedsOptions(questionType)) {
      teacherQuestionForm.optionA = ''
      teacherQuestionForm.optionB = ''
      teacherQuestionForm.optionC = ''
      teacherQuestionForm.optionD = ''
    }
    if (questionType !== 'LISTENING_CHOICE') {
      clearQuestionAudio()
    }
  }
)

watch(
  () => adminUserForm.role,
  (role) => {
    if (role === 'ADMIN') {
      adminUserForm.className = ''
    }
  }
)

function availableQuestionCount(type) {
  return state.teacherExamQuestions.filter((question) => question.enabled !== false && question.examType === type).length
}

function isChoiceQuestion(question) {
  return ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'LISTENING_CHOICE', 'TRUE_FALSE'].includes(question.questionType)
}

function examOptions(question) {
  return [
    { key: 'A', value: question.optionA },
    { key: 'B', value: question.optionB },
    { key: 'C', value: question.optionC },
    { key: 'D', value: question.optionD }
  ].filter((option) => option.value)
}

function multiAnswerSelected(questionId, optionKey) {
  return String(examAnswers[questionId] || '')
    .split(',')
    .map((item) => item.trim())
    .includes(optionKey)
}

function toggleMultiAnswer(questionId, optionKey) {
  const values = String(examAnswers[questionId] || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
  if (values.includes(optionKey)) {
    examAnswers[questionId] = values.filter((item) => item !== optionKey).join(',')
  } else {
    examAnswers[questionId] = [...values, optionKey].sort().join(',')
  }
  persistExamSessionLocal()
}

async function handleQuestionAudioChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  try {
    const formData = new FormData()
    formData.append('file', file)
    const data = await request.post('/api/teacher/exam-questions/audio', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    teacherQuestionForm.audioFileName = data.audioFileName || file.name
    teacherQuestionForm.audioStoredName = data.audioStoredName || ''
    teacherQuestionForm.audioContentType = data.audioContentType || file.type
    teacherQuestionForm.audioFileSize = data.audioFileSize || file.size
    setMessage('听力音频已上传')
  } catch (error) {
    setError(error)
  }
}

function clearQuestionAudio() {
  teacherQuestionForm.audioFileName = ''
  teacherQuestionForm.audioStoredName = ''
  teacherQuestionForm.audioContentType = ''
  teacherQuestionForm.audioFileSize = null
}

function resourceStatusLabel(status) {
  return {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    PUBLISHED: '已上架',
    DRAFT: '已下架',
    PUBLIC: '公开资源',
    CLASS: '班级资源',
    SUBMITTED: '已提交',
    GRADED: '已批改'
  }[status] || status || '-'
}

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

function getResourceFileExtension(item) {
  const fileName = String(item?.fileName || item?.title || '').toLowerCase()
  const index = fileName.lastIndexOf('.')
  return index >= 0 ? fileName.slice(index) : ''
}

function isVideoResource(item) {
  return item?.previewable || ['.mp4', '.avi', '.mov', '.wmv'].includes(getResourceFileExtension(item))
}

function isDocxResource(item) {
  return getResourceFileExtension(item) === '.docx'
}

function isDocResource(item) {
  return getResourceFileExtension(item) === '.doc'
}

function previewResource(item) {
  state.preview.visible = true
  state.preview.resourceId = item.id
  state.preview.title = item.title
  state.preview.type = item.type
  state.preview.rolePath = state.currentView?.startsWith('teacher-') ? 'teacher' : 'student'
  clearPreviewObjectUrl()

  if (isVideoResource(item)) {
    state.preview.mode = 'video'
    state.preview.src = ''
    renderVideoPreview(item.id)
    return
  }

  if (isDocxResource(item)) {
    state.preview.mode = 'docx'
    state.preview.src = ''
    renderDocxPreview(item.id)
    return
  }

  if (isDocResource(item)) {
    state.preview.mode = 'unsupported'
    state.preview.src = ''
    setMessage('当前本地预览仅支持 .docx，.doc 请先下载后查看')
    return
  }

  state.preview.mode = 'unsupported'
  state.preview.src = ''
}

function closePreview() {
  clearPreviewObjectUrl()
  state.preview.visible = false
  state.preview.resourceId = null
  state.preview.title = ''
  state.preview.type = ''
  state.preview.mode = ''
  state.preview.src = ''
  state.preview.rolePath = ''
  if (docxPreviewRef.value) {
    docxPreviewRef.value.innerHTML = ''
  }
}

function clearPreviewObjectUrl() {
  if (!state.preview.objectUrl) {
    return
  }
  window.URL.revokeObjectURL(state.preview.objectUrl)
  state.preview.objectUrl = ''
}

async function renderVideoPreview(resourceId) {
  try {
    const blob = await request.get(resourceFilePath(resourceId, 'file'), {
      responseType: 'blob'
    })
    const objectUrl = window.URL.createObjectURL(blob)
    state.preview.objectUrl = objectUrl
    state.preview.src = objectUrl
  } catch (error) {
    state.preview.mode = 'unsupported'
    state.preview.src = ''
    setError(error)
  }
}

async function renderDocxPreview(resourceId) {
  try {
    await nextTick()
    if (!docxPreviewRef.value) {
      return
    }
    docxPreviewRef.value.innerHTML = '<div class="preview-loading">文档加载中...</div>'
    const buffer = await request.get(resourceFilePath(resourceId, 'file'), {
      responseType: 'arraybuffer'
    })
    const mammoth = mammothBrowser?.default || mammothBrowser
    const result = await mammoth.convertToHtml({ arrayBuffer: buffer })
    docxPreviewRef.value.innerHTML = `<div class="docx-html">${result.value}</div>`
  } catch (error) {
    state.preview.mode = 'unsupported'
    state.preview.src = ''
    if (docxPreviewRef.value) {
      docxPreviewRef.value.innerHTML = ''
    }
    setError(error)
  }
}

async function downloadResource(item) {
  try {
    const blob = await request.get(resourceFilePath(item.id, 'download'), {
      responseType: 'blob'
    })
    triggerBlobDownload(blob, item.fileName || item.title)
  } catch (error) {
    setError(error)
  }
}

async function downloadPreviewResource() {
  if (!state.preview.resourceId) {
    return
  }
  try {
    const blob = await request.get(resourceFilePath(state.preview.resourceId, 'download'), {
      responseType: 'blob'
    })
    triggerBlobDownload(blob, state.preview.title)
  } catch (error) {
    setError(error)
  }
}

function resourceFilePath(resourceId, action) {
  const rolePath = state.preview.rolePath || (state.currentView?.startsWith('teacher-') ? 'teacher' : 'student')
  return `/api/${rolePath}/resources/${resourceId}/${action}`
}

function triggerBlobDownload(blob, fileName) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || 'resource'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

async function changeView(view) {
  if (state.currentView === 'student-exams' && view !== 'student-exams') {
    stopExamTimer()
    revokeExamAudioUrls()
    state.currentExam = null
    state.examRemainingSeconds = 0
    state.examSessionStatus = ''
  }
  state.currentView = view
  await loadCurrentView()
}

async function loadCurrentView() {
  if (!state.user) return
  state.error = ''
  try {
    switch (state.currentView) {
      case 'student-dashboard':
        state.studentDashboard = await request.get('/api/student/dashboard')
        break
      case 'resources':
        await loadStudentResources()
        break
      case 'plans':
        state.plans = await request.get('/api/student/plans')
        break
      case 'student-homeworks':
        state.studentHomeworks = await request.get('/api/student/homeworks')
        break
      case 'student-exams':
        state.studentExams = await request.get('/api/student/exams')
        break
      case 'student-records':
        state.records = await request.get('/api/student/records')
        break
      case 'vocabulary':
        await loadVocabulary()
        break
      case 'student-notifications':
        state.notifications = await request.get('/api/student/notifications')
        break
      case 'student-feedback':
        state.feedbackList = await request.get('/api/student/feedback')
        break
      case 'student-qa':
        await loadQaResources()
        state.qaList = await request.get('/api/student/qa')
        break
      case 'student-forum':
        await loadForumPosts()
        break
      case 'teacher-dashboard':
        await loadTeacherDashboard()
        break
      case 'teacher-resources':
        state.teacherResources = await request.get('/api/teacher/resources')
        break
      case 'teacher-homeworks':
        state.teacherHomeworks = await request.get('/api/teacher/homeworks')
        break
      case 'teacher-exams':
        state.teacherExams = await request.get('/api/teacher/exams')
        state.teacherExamQuestions = await request.get('/api/teacher/exam-questions')
        break
      case 'teacher-question-bank':
        state.teacherExamQuestions = await request.get('/api/teacher/exam-questions')
        break
      case 'teacher-records':
        state.teacherRecords = await request.get('/api/teacher/records')
        state.teacherRecords.forEach((item) => {
          teacherRecordFeedback[item.id] = item.teacherFeedback || ''
        })
        break
      case 'teacher-ai-advice':
        await loadTeacherDashboard()
        break
      case 'teacher-notifications':
        state.teacherNotifications = await request.get('/api/teacher/notifications')
        break
      case 'teacher-qa':
        state.teacherQaList = await request.get('/api/teacher/qa')
        break
      case 'admin-dashboard':
        state.adminDashboard = await request.get('/api/admin/dashboard')
        break
      case 'admin-users':
        state.adminUsers = await request.get('/api/admin/users')
        break
      case 'admin-resources':
        state.adminResources = await request.get('/api/admin/resources')
        break
      case 'admin-notifications':
        state.adminNotifications = await request.get('/api/admin/notifications')
        break
      case 'admin-feedback':
        state.adminFeedback = await request.get('/api/admin/feedback')
        break
      case 'admin-logs':
        state.adminLogs = await request.get('/api/admin/logs')
        break
      case 'admin-configs':
        state.adminConfigs = await request.get('/api/admin/configs')
        break
      case 'admin-report':
        state.adminReport = await request.get('/api/admin/report')
        break
      case 'profile': {
        const user = await request.get('/api/auth/me')
        state.user = user
        localStorage.setItem('user', JSON.stringify(user))
        initProfileForm()
        break
      }
    }
  } catch (error) {
    setError(error)
  }
}

async function loadStudentResources() {
  state.resources = await request.get('/api/student/resources', { params: { keyword: state.resourceKeyword } })
}

async function loadQaResources() {
  state.qaResources = await request.get('/api/student/resources', { params: { keyword: '' } })
}

async function loadForumPosts() {
  state.forumPosts = await request.get('/api/student/forum/posts', { params: { keyword: state.forumKeyword } })
  if (!state.currentForumPost && state.forumPosts.length) {
    await openForumPost(state.forumPosts[0].id)
  } else if (state.currentForumPost) {
    const exists = state.forumPosts.some((item) => item.id === state.currentForumPost.id)
    if (exists) {
      await openForumPost(state.currentForumPost.id)
    } else {
      state.currentForumPost = state.forumPosts.length ? await request.get(`/api/student/forum/posts/${state.forumPosts[0].id}`) : null
    }
  }
}

async function loadTeacherDashboard() {
  state.teacherDashboard = await request.get('/api/teacher/dashboard')
}

async function loadVocabulary() {
  const allData = await request.get('/api/student/vocabulary', { params: { mode: 'all' } })
  state.vocabularyAllWords = allData.words || []
  state.vocabularyStats = allData.stats || {}
  const activeData = state.vocabularyMode === 'review'
    ? await request.get('/api/student/vocabulary', { params: { mode: 'review' } })
    : allData
  state.vocabularyWords = activeData.words || []
  if (state.vocabularyIndex >= state.vocabularyWords.length) {
    state.vocabularyIndex = Math.max(state.vocabularyWords.length - 1, 0)
  }
}

async function setVocabularyMode(mode) {
  state.vocabularyMode = mode
  state.vocabularyIndex = 0
  await loadVocabulary()
}

function nextVocabularyWord() {
  if (!state.vocabularyWords.length) return
  state.vocabularyIndex = (state.vocabularyIndex + 1) % state.vocabularyWords.length
}

function previousVocabularyWord() {
  if (!state.vocabularyWords.length) return
  state.vocabularyIndex = state.vocabularyIndex === 0 ? state.vocabularyWords.length - 1 : state.vocabularyIndex - 1
}

async function reviewVocabulary(known) {
  const word = currentVocabularyWord.value
  if (!word) return
  try {
    await request.post('/api/student/vocabulary/review', {
      wordId: word.id,
      known
    })
    await loadVocabulary()
    if (state.vocabularyWords.length) {
      state.vocabularyIndex = Math.min(state.vocabularyIndex, state.vocabularyWords.length - 1)
      if (state.vocabularyMode === 'all') {
        nextVocabularyWord()
      }
    }
    setMessage(known ? '已标记为认识' : '已加入错词本')
  } catch (error) {
    setError(error)
  }
}

async function resetVocabularyRecords() {
  try {
    await request.delete('/api/student/vocabulary/records')
    state.vocabularyIndex = 0
    await loadVocabulary()
    setMessage('单词学习记录已重置')
  } catch (error) {
    setError(error)
  }
}

async function submitHomework() {
  if (!homeworkSubmitForm.homeworkId) {
    setError(new Error('请先选择要提交的作业'))
    return
  }
  try {
    await request.post(`/api/student/homeworks/${homeworkSubmitForm.homeworkId}/submit`, {
      content: homeworkSubmitForm.content
    })
    resetHomeworkSubmitForm()
    state.studentHomeworks = await request.get('/api/student/homeworks')
    setMessage('作业已提交')
  } catch (error) {
    setError(error)
  }
}

async function savePlan() {
  try {
    if (planForm.id) {
      await request.put(`/api/student/plans/${planForm.id}`, planForm)
      setMessage('学习计划已更新')
    } else {
      await request.post('/api/student/plans', planForm)
      setMessage('学习计划已新增')
    }
    resetPlanForm()
    state.plans = await request.get('/api/student/plans')
  } catch (error) {
    setError(error)
  }
}

async function deletePlan(id) {
  try {
    await request.delete(`/api/student/plans/${id}`)
    state.plans = await request.get('/api/student/plans')
    setMessage('学习计划已删除')
  } catch (error) {
    setError(error)
  }
}

async function openExam(id) {
  try {
    const exam = state.studentExams.find((item) => item.id === id)
    if (exam?.completed) {
      setError(new Error('该试卷已完成，不能重复考试'))
      return
    }
    stopExamTimer()
    revokeExamAudioUrls()
    state.currentExam = await request.get(`/api/student/exams/${id}`)
    await loadCurrentExamAudios()
    Object.keys(examAnswers).forEach((key) => delete examAnswers[key])
    const remoteSession = state.currentExam.session || await request.get(`/api/student/exams/${id}/session`)
    restoreExamSession(remoteSession)
    restoreExamSessionLocal(id)
    if (!state.examRemainingSeconds) {
      state.examRemainingSeconds = Number(state.currentExam.durationMinutes || 0) * 60
    }
    startExamTimer()
  } catch (error) {
    setError(error)
  }
}

async function submitExam() {
  if (!state.currentExam) return
  try {
    stopExamTimer()
    const answers = state.currentExam.questions.map((item) => ({
      questionId: item.id,
      answer: examAnswers[item.id] || ''
    }))
    const result = await request.post(`/api/student/exams/${state.currentExam.id}/submit`, { answers })
    setMessage(`交卷成功，得分 ${result.score} / ${result.totalScore}`)
    state.records = await request.get('/api/student/records')
    state.studentExams = await request.get('/api/student/exams')
    clearExamSessionLocal(state.currentExam.id)
    revokeExamAudioUrls()
    state.currentExam = null
    state.examRemainingSeconds = 0
    state.examSessionStatus = ''
  } catch (error) {
    setError(error)
  }
}

async function loadCurrentExamAudios() {
  const questions = state.currentExam?.questions || []
  await Promise.all(questions.filter((item) => item.hasAudio).map(async (question) => {
    try {
      const blob = await request.get(`/api/student/exam-questions/${question.id}/audio`, {
        responseType: 'blob'
      })
      const objectUrl = window.URL.createObjectURL(blob)
      state.examAudioUrls[question.id] = objectUrl
      question.audioSrc = objectUrl
    } catch (error) {
      question.audioSrc = ''
    }
  }))
}

function revokeExamAudioUrls() {
  Object.values(state.examAudioUrls).forEach((url) => {
    if (url) {
      window.URL.revokeObjectURL(url)
    }
  })
  state.examAudioUrls = {}
}

function buildExamAnswersPayload() {
  if (!state.currentExam) return []
  return state.currentExam.questions.map((item) => ({
    questionId: item.id,
    answer: examAnswers[item.id] || ''
  }))
}

function restoreExamSession(session) {
  state.examSessionStatus = session?.status || 'NEW'
  state.examRemainingSeconds = Number(session?.remainingSeconds || 0)
  ;(session?.answers || []).forEach((item) => {
    if (item.questionId && item.answer) {
      examAnswers[item.questionId] = item.answer
    }
  })
}

function examSessionStorageKey(id) {
  return `exam-session-${state.user?.id || 'guest'}-${id}`
}

function restoreExamSessionLocal(id) {
  const raw = sessionStorage.getItem(examSessionStorageKey(id))
  if (!raw) return
  try {
    const cached = JSON.parse(raw)
    if (Number(cached.remainingSeconds || 0) > 0) {
      state.examRemainingSeconds = cached.remainingSeconds
    }
    ;(cached.answers || []).forEach((item) => {
      if (item.questionId && item.answer) {
        examAnswers[item.questionId] = item.answer
      }
    })
  } catch (error) {
    sessionStorage.removeItem(examSessionStorageKey(id))
  }
}

function persistExamSessionLocal() {
  if (!state.currentExam) return
  sessionStorage.setItem(examSessionStorageKey(state.currentExam.id), JSON.stringify({
    answers: buildExamAnswersPayload(),
    remainingSeconds: state.examRemainingSeconds
  }))
}

function clearExamSessionLocal(id) {
  sessionStorage.removeItem(examSessionStorageKey(id))
}

function startExamTimer() {
  stopExamTimer()
  examTimer = window.setInterval(() => {
    if (!state.currentExam) {
      stopExamTimer()
      return
    }
    state.examRemainingSeconds = Math.max(Number(state.examRemainingSeconds || 0) - 1, 0)
    persistExamSessionLocal()
    if (state.examRemainingSeconds === 0) {
      if (state.currentExam.autoSubmit !== false) {
        submitExam()
      } else {
        stopExamTimer()
        setMessage('考试时间已结束，请尽快手动交卷')
      }
    }
  }, 1000)
}

function stopExamTimer() {
  if (examTimer) {
    window.clearInterval(examTimer)
    examTimer = null
  }
}

async function saveExamSessionRemote() {
  if (!state.currentExam) return
  try {
    const session = await request.post(`/api/student/exams/${state.currentExam.id}/session`, {
      answers: buildExamAnswersPayload(),
      remainingSeconds: state.examRemainingSeconds
    })
    restoreExamSession(session)
    persistExamSessionLocal()
    setMessage('考试进度已保存')
  } catch (error) {
    setError(error)
  }
}

async function queryDictionary() {
  state.loading = true
  try {
    state.dictionaryResult = await request.post('/api/student/ai-dictionary', dictionaryForm)
  } catch (error) {
    setError(error)
  } finally {
    state.loading = false
  }
}

async function submitFeedback() {
  try {
    await request.post('/api/student/feedback', feedbackForm)
    feedbackForm.content = ''
    state.feedbackList = await request.get('/api/student/feedback')
    setMessage('反馈已提交')
  } catch (error) {
    setError(error)
  }
}

async function submitQuestion() {
  if (!qaForm.resourceId) {
    setError(new Error('请先选择要答疑的学习资源'))
    return
  }
  try {
    await request.post('/api/student/qa', qaForm)
    qaForm.resourceId = null
    qaForm.title = ''
    qaForm.content = ''
    state.qaList = await request.get('/api/student/qa')
    setMessage('答疑问题已提交')
  } catch (error) {
    setError(error)
  }
}

async function openForumPost(id) {
  try {
    state.currentForumPost = await request.get(`/api/student/forum/posts/${id}`)
  } catch (error) {
    setError(error)
  }
}

async function createForumPost() {
  try {
    if (!forumForm.title.trim() || !forumForm.content.trim()) {
      setError(new Error('帖子标题和内容不能为空'))
      return
    }
    await request.post('/api/student/forum/posts', forumForm)
    forumForm.title = ''
    forumForm.content = ''
    await loadForumPosts()
    setMessage('帖子已发布')
  } catch (error) {
    setError(error)
  }
}

async function createForumReply() {
  if (!state.currentForumPost) {
    setError(new Error('请先选择一个帖子'))
    return
  }
  try {
    if (!forumReplyForm.content.trim()) {
      setError(new Error('回复内容不能为空'))
      return
    }
    await request.post(`/api/student/forum/posts/${state.currentForumPost.id}/replies`, forumReplyForm)
    forumReplyForm.content = ''
    await openForumPost(state.currentForumPost.id)
    await loadForumPosts()
    setMessage('回复已发表')
  } catch (error) {
    setError(error)
  }
}

async function deleteForumPost(id) {
  try {
    await request.delete(`/api/student/forum/posts/${id}`)
    if (state.currentForumPost?.id === id) {
      state.currentForumPost = null
    }
    await loadForumPosts()
    setMessage('帖子已删除')
  } catch (error) {
    setError(error)
  }
}

async function deleteForumReply(id) {
  try {
    await request.delete(`/api/student/forum/replies/${id}`)
    if (state.currentForumPost) {
      await openForumPost(state.currentForumPost.id)
      await loadForumPosts()
    }
    setMessage('回复已删除')
  } catch (error) {
    setError(error)
  }
}

async function saveTeacherResource() {
  try {
    teacherResourceForm.visibility = 'CLASS'
    teacherResourceForm.className = state.user?.className || ''
    const payload = {
      title: teacherResourceForm.title,
      description: teacherResourceForm.description,
      type: teacherResourceForm.type,
      url: teacherResourceForm.url,
      downloadable: teacherResourceForm.downloadable,
      visibility: teacherResourceForm.visibility,
      className: teacherResourceForm.className
    }
    if (teacherResourceForm.id) {
      await request.put(`/api/teacher/resources/${teacherResourceForm.id}`, payload)
      setMessage('资源已更新')
    } else if (teacherResourceForm.file) {
      const formData = new FormData()
      formData.append('file', teacherResourceForm.file)
      formData.append('title', teacherResourceForm.title)
      formData.append('description', teacherResourceForm.description)
      formData.append('type', teacherResourceForm.type)
      formData.append('visibility', 'CLASS')
      formData.append('className', state.user?.className || '')
      await request.post('/api/teacher/resources/upload', formData)
      setMessage('资源已上传')
    } else {
      setError(new Error('请先选择要上传的视频或Word文件'))
      return
    }
    resetTeacherResourceForm()
    state.teacherResources = await request.get('/api/teacher/resources')
  } catch (error) {
    setError(error)
  }
}

async function deleteTeacherResource(id) {
  try {
    await request.delete(`/api/teacher/resources/${id}`)
    state.teacherResources = await request.get('/api/teacher/resources')
    setMessage('资源已删除')
  } catch (error) {
    setError(error)
  }
}

async function saveTeacherHomework() {
  try {
    teacherHomeworkForm.className = state.user?.className || ''
    const payload = {
      title: teacherHomeworkForm.title,
      description: teacherHomeworkForm.description,
      className: teacherHomeworkForm.className,
      dueTime: teacherHomeworkForm.dueTime,
      status: teacherHomeworkForm.status
    }
    if (teacherHomeworkForm.id) {
      await request.put(`/api/teacher/homeworks/${teacherHomeworkForm.id}`, payload)
      setMessage('作业已更新')
    } else {
      await request.post('/api/teacher/homeworks', payload)
      setMessage('作业已发布')
    }
    resetTeacherHomeworkForm()
    state.teacherHomeworks = await request.get('/api/teacher/homeworks')
  } catch (error) {
    setError(error)
  }
}

async function deleteTeacherHomework(id) {
  try {
    await request.delete(`/api/teacher/homeworks/${id}`)
    state.teacherHomeworks = await request.get('/api/teacher/homeworks')
    if (state.activeHomework?.id === id) {
      state.activeHomework = null
      state.homeworkSubmissions = []
    }
    setMessage('作业已删除')
  } catch (error) {
    setError(error)
  }
}

async function loadHomeworkSubmissions(homework) {
  try {
    state.activeHomework = homework
    state.homeworkSubmissions = await request.get(`/api/teacher/homeworks/${homework.id}/submissions`)
    state.homeworkSubmissions.forEach((item) => {
      teacherHomeworkGrade[item.id] = {
        score: item.score ?? 0,
        teacherFeedback: item.teacherFeedback || ''
      }
    })
  } catch (error) {
    setError(error)
  }
}

async function gradeHomeworkSubmission(id) {
  try {
    await request.put(`/api/teacher/homework-submissions/${id}/grade`, teacherHomeworkGrade[id])
    if (state.activeHomework) {
      await loadHomeworkSubmissions(state.activeHomework)
    }
    setMessage('作业批改已保存')
  } catch (error) {
    setError(error)
  }
}

async function saveTeacherExam() {
  try {
    const availableCount = availableQuestionCount(teacherExamForm.type)
    if (availableCount < Number(teacherExamForm.questionCount || 1)) {
      setError(new Error(`当前分类题库只有 ${availableCount} 道启用题，不能抽取 ${teacherExamForm.questionCount} 道`))
      return
    }
    const payload = {
      title: teacherExamForm.title,
      description: teacherExamForm.description,
      type: teacherExamForm.type,
      questionCount: teacherExamForm.questionCount,
      durationMinutes: teacherExamForm.durationMinutes,
      autoSubmit: teacherExamForm.autoSubmit,
      published: teacherExamForm.published
    }
    if (teacherExamForm.id) {
      await request.put(`/api/teacher/exams/${teacherExamForm.id}`, payload)
      setMessage('考试已更新')
    } else {
      await request.post('/api/teacher/exams', payload)
      setMessage('考试已创建')
    }
    resetTeacherExamForm()
    state.teacherExams = await request.get('/api/teacher/exams')
  } catch (error) {
    setError(error)
  }
}

async function saveExamQuestion() {
  try {
    const payload = {
      examType: teacherQuestionForm.examType,
      questionType: teacherQuestionForm.questionType,
      content: teacherQuestionForm.content,
      optionA: teacherQuestionForm.optionA,
      optionB: teacherQuestionForm.optionB,
      optionC: teacherQuestionForm.optionC,
      optionD: teacherQuestionForm.optionD,
      correctAnswer: teacherQuestionForm.correctAnswer,
      analysis: teacherQuestionForm.analysis,
      audioFileName: teacherQuestionForm.audioFileName,
      audioStoredName: teacherQuestionForm.audioStoredName,
      audioContentType: teacherQuestionForm.audioContentType,
      audioFileSize: teacherQuestionForm.audioFileSize,
      score: teacherQuestionForm.score,
      enabled: teacherQuestionForm.enabled
    }
    if (teacherQuestionForm.id) {
      await request.put(`/api/teacher/exam-questions/${teacherQuestionForm.id}`, payload)
      setMessage('题目已更新')
    } else {
      await request.post('/api/teacher/exam-questions', payload)
      setMessage('题目已加入题库')
    }
    resetTeacherQuestionForm()
    state.teacherExamQuestions = await request.get('/api/teacher/exam-questions')
  } catch (error) {
    setError(error)
  }
}

async function deleteExamQuestion(id) {
  try {
    await request.delete(`/api/teacher/exam-questions/${id}`)
    state.teacherExamQuestions = await request.get('/api/teacher/exam-questions')
    setMessage('题目已删除')
  } catch (error) {
    setError(error)
  }
}

async function deleteTeacherExam(id) {
  try {
    await request.delete(`/api/teacher/exams/${id}`)
    state.teacherExams = await request.get('/api/teacher/exams')
    setMessage('考试已删除')
  } catch (error) {
    setError(error)
  }
}

async function saveRecordFeedback(id) {
  try {
    await request.put(`/api/teacher/records/${id}/feedback`, { teacherFeedback: teacherRecordFeedback[id] || '' })
    state.teacherRecords = await request.get('/api/teacher/records')
    setMessage('成绩反馈已保存')
  } catch (error) {
    setError(error)
  }
}

async function generateTeacherAdvice() {
  state.loading = true
  try {
    state.teacherAiAdvice = await request.post('/api/teacher/ai/advice')
    setMessage('AI教学建议已生成')
  } catch (error) {
    setError(error)
  } finally {
    state.loading = false
  }
}

async function answerQuestion(id) {
  try {
    await request.post(`/api/teacher/qa/${id}/answer`, { content: teacherQaAnswer[id] || '' })
    teacherQaAnswer[id] = ''
    state.teacherQaList = await request.get('/api/teacher/qa')
    setMessage('答疑已提交')
  } catch (error) {
    setError(error)
  }
}

async function saveAdminUser(item) {
  try {
    if (item.role === 'ADMIN') {
      item.className = ''
    }
    await request.put(`/api/admin/users/${item.id}`, {
      role: item.role,
      status: item.status,
      realName: item.realName,
      email: item.email,
      className: item.className
    })
    state.adminUsers = await request.get('/api/admin/users')
    setMessage('用户信息已更新')
  } catch (error) {
    setError(error)
  }
}

async function createAdminUser() {
  try {
    const payload = { ...adminUserForm }
    if (payload.role === 'ADMIN') {
      payload.className = ''
    }
    await request.post('/api/admin/users', payload)
    resetAdminUserForm()
    state.adminUsers = await request.get('/api/admin/users')
    setMessage('账号已创建')
  } catch (error) {
    setError(error)
  }
}

async function resetAdminUserPassword(id) {
  try {
    await request.put(`/api/admin/users/${id}/reset-password`)
    setMessage('密码已重置为 123456')
  } catch (error) {
    setError(error)
  }
}

async function auditAdminResource(id, auditStatus) {
  try {
    await request.put(`/api/admin/resources/${id}/audit`, { auditStatus })
    state.adminResources = await request.get('/api/admin/resources')
    setMessage('资源审核状态已更新')
  } catch (error) {
    setError(error)
  }
}

async function updateAdminResourceOnlineStatus(id, onlineStatus) {
  try {
    await request.put(`/api/admin/resources/${id}/online-status`, { onlineStatus })
    state.adminResources = await request.get('/api/admin/resources')
    setMessage('资源上下架状态已更新')
  } catch (error) {
    setError(error)
  }
}

async function saveNotification() {
  try {
    await request.post('/api/admin/notifications', notificationForm)
    notificationForm.title = ''
    notificationForm.content = ''
    notificationForm.targetRole = 'ALL'
    state.adminNotifications = await request.get('/api/admin/notifications')
    setMessage('公告已发布')
  } catch (error) {
    setError(error)
  }
}

async function deleteNotification(id) {
  try {
    await request.delete(`/api/admin/notifications/${id}`)
    state.adminNotifications = await request.get('/api/admin/notifications')
    setMessage('公告已删除')
  } catch (error) {
    setError(error)
  }
}

async function replyFeedback(id) {
  try {
    await request.put(`/api/admin/feedback/${id}/reply`, { replyContent: adminFeedbackReply[id] || '' })
    state.adminFeedback = await request.get('/api/admin/feedback')
    setMessage('反馈已处理')
  } catch (error) {
    setError(error)
  }
}

async function saveConfig(item) {
  try {
    await request.put(`/api/admin/configs/${item.id}`, { configValue: item.configValue })
    state.adminConfigs = await request.get('/api/admin/configs')
    setMessage('参数已更新')
  } catch (error) {
    setError(error)
  }
}

async function runBackup() {
  try {
    await request.post('/api/admin/backup')
    state.adminConfigs = await request.get('/api/admin/configs')
    setMessage('已登记一次备份时间')
  } catch (error) {
    setError(error)
  }
}

async function saveProfile() {
  try {
    const user = await request.put('/api/auth/profile', profileForm)
    state.user = user
    localStorage.setItem('user', JSON.stringify(user))
    initProfileForm()
    setMessage('个人资料已保存')
  } catch (error) {
    setError(error)
  }
}

async function savePassword() {
  try {
    await request.put('/api/auth/password', passwordForm)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    setMessage('密码修改成功')
  } catch (error) {
    setError(error)
  }
}

if (state.token && state.user) {
  initProfileForm()
  if (state.user.role === 'TEACHER') {
    resetTeacherResourceForm()
    resetTeacherHomeworkForm()
  }
  loadCurrentView()
}
</script>
