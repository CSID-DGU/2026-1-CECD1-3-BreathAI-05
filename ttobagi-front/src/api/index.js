const mock = (data, ms = 600) =>
  new Promise((res) => setTimeout(() => res({ success: true, data }), ms));

// ── 회원 (gold_user) ──────────────────────────────────────
export const authApi = {
  // POST /api/v1/auth/login
  login: (email, password) =>
    mock({ accessToken: "mock-token", user: { email, name: "운영자", role: "USER" } }),

  // POST /api/v1/auth/signup
  signup: (email, password) => mock(null),

  // POST /api/v1/auth/password/reset
  resetPassword: (email) => mock(null),

  // GET /api/v1/auth/me
  getMe: (token) =>
    mock({ email: "admin@seoulmetro.co.kr", name: "운영자", role: "USER" }),

  // PATCH /api/v1/auth/me
  updateMe: (body, token) => mock(null),

  // DELETE /api/v1/auth/quit
  quit: (token) => mock(null),
};

// ── 대시보드 ──────────────────────────────────────────────
export const dashApi = {
  // GET /api/v1/dashboard/usage
  // → gold_usage_stat 기반
  // ai_percent: 75이상=정답 / 25~75=오답변 / 25미만=미답변
  getUsage: (token) =>
    mock({
      totalChats: 18432,
      answerCount: 11043,
      lowQualityCount: 2435,
      unansweredCount: 4954,
      answeredRate: 59.9,
      lowQualityRate: 13.2,
      unansweredRate: 26.9,
      monthly: [
        { month: "11월", answerCount: 1820, lowQualityCount: 380, unansweredCount: 800 },
        { month: "12월", answerCount: 1890, lowQualityCount: 350, unansweredCount: 760 },
        { month: "1월",  answerCount: 1850, lowQualityCount: 360, unansweredCount: 790 },
        { month: "2월",  answerCount: 1920, lowQualityCount: 340, unansweredCount: 740 },
        { month: "3월",  answerCount: 1960, lowQualityCount: 320, unansweredCount: 720 },
        { month: "4월",  answerCount: 1603, lowQualityCount: 685, unansweredCount: 144 },
      ],
      topKeywords: [
        { word: "에어컨",       count: 412 },
        { word: "냉방",         count: 388 },
        { word: "에스컬레이터", count: 271 },
        { word: "유실물",       count: 243 },
        { word: "환승",         count: 198 },
        { word: "열차 지연",    count: 187 },
        { word: "수유칸",       count: 134 },
        { word: "연착",         count: 122 },
      ],
      // gold_unanswer_stat — 미답변 사유별 세분화
      unansweredReasons: [
        { reason: "키워드 미등록",        count: 2100, rate: 42.4 },
        { reason: "외국어 질의",           count: 890,  rate: 18.0 },
        { reason: "보안/정책상 답변 불가", count: 650,  rate: 13.1 },
        { reason: "기타",                  count: 1314, rate: 26.5 },
      ],
      // gold_surge_detection — 급증 질문
      surges: [
        { keyword: "에스컬레이터", keywordCount: 210, increasedRate: 520.0, isNew: false },
        { keyword: "냉방",         keywordCount: 180, increasedRate: 280.0, isNew: false },
        { keyword: "수유실",       keywordCount: 95,  increasedRate: 150.0, isNew: true  },
      ],
      // gold_performance_comparison
      performance: {
        afterUnanswerRate: 11.8,
        accuracyGain: 12.4,
        falsePositiveRate: 0.032,
        evalThreshold: 0.75,
        resolvedCountByAi: 1200,
      },
    }),

  // POST /api/v1/dashboard/analyze/upload
  // → gold_upload_file
  uploadExcel: (file, token) =>
    mock({
      uploadId: "uuid-upload-001",    // gold_upload_file.upload_id (UUID)
      fileName: "unanswered_log.xlsx",
      totalRows: 8166,
      insertedRows: 8102,
      status: "UPLOADED",             // UPLOADED / INGESTED / FAIL
    }, 1200),

  // POST /api/v1/dashboard/analyze/new/:uploadId
  // → gold_analysis_job
  runAnalysis: (uploadId, token) =>
    mock({
      analysisId: 1,                  // gold_analysis_job.analysis_id (BIGINT)
      uploadId: uploadId,
      status: "PREPROCESSING",        // PREPROCESSING / ANALYZING / COMPLETED / FAIL
    }, 800),

  // GET /api/v1/dashboard/analyze/:analysisId
  // → gold_analysis_job.status
  getAnalysisStatus: (analysisId, token) =>
    mock({
      analysisId: analysisId,
      status: "COMPLETED",            // PREPROCESSING / ANALYZING / COMPLETED / FAIL
      startedAt: "2026-04-19T10:30:00",
      finishedAt: "2026-04-19T10:44:00",
    }),

  // GET /api/v1/dashboard/analyze/legacy
  // → gold_usage_stat + gold_performance_comparison
  getLegacyAnalysis: (token) =>
    mock({
      totalLogs: 8166,
      answerCount: 4920,              // ai_percent >= 75
      lowQualityCount: 1082,          // 25 <= ai_percent < 75
      unansweredCount: 2164,          // ai_percent < 25
      breakdown: [
        { label: "정답",  value: 60.2, color: "#1976D2" },  // ai_percent >= 75
        { label: "오답변", value: 13.2, color: "#64B5F6" }, // 25~75
        { label: "미답변", value: 26.6, color: "#EF9F27" }, // < 25
      ],
      // gold_performance_comparison
      afterUnansweredRate: 11.8,
      accuracyGain: 12.4,
      resolvedCountByAi: 1200,
    }),

  // GET /api/v1/dashboard/analyze/history
  // → gold_analysis_job 목록
  getAnalysisHistory: (token) =>
    mock([
      {
        analysisId: 1,
        uploadId: "uuid-upload-001",
        fileName: "unanswered_log_202604.xlsx",
        status: "COMPLETED",          // PREPROCESSING / ANALYZING / COMPLETED / FAIL
        startedAt: "2026-04-19T10:30:00",
        finishedAt: "2026-04-19T10:44:00",
      },
      {
        analysisId: 2,
        uploadId: "uuid-upload-002",
        fileName: "unanswered_log_202603.xlsx",
        status: "COMPLETED",
        startedAt: "2026-03-12T09:15:00",
        finishedAt: "2026-03-12T09:28:00",
      },
      {
        analysisId: 3,
        uploadId: "uuid-upload-003",
        fileName: "unanswered_log_202602.xlsx",
        status: "FAIL",
        startedAt: "2026-02-08T14:22:00",
        finishedAt: null,
      },
    ]),
};

// ── FAQ ───────────────────────────────────────────────────
export const faqApi = {
  // GET /api/v1/faq
  // → bronze_counselling_info (현행 FAQ 목록)
  getList: (token) =>
    mock({
      faqs: [
        {
          faqId: 101,                 // counselling_info.seq_num
          question: "열차 내 냉난방 온도 조절 요청",
          answer: "열차 내 온도는 혼잡도에 따라 자동 조절됩니다.",
          keywords: ["에어컨", "냉방", "온도"],
          qaCnt: 1240,               // counselling_info.qa_cnt
        },
        {
          faqId: 102,
          question: "유실물 신고 방법",
          answer: "유실물센터(02-6110-1122)로 연락하시거나 홈페이지를 이용해 주세요.",
          keywords: ["유실물", "분실"],
          qaCnt: 843,
        },
        {
          faqId: 103,
          question: "에스컬레이터 고장 신고",
          answer: "시설물 고장 신고는 역무원에게 직접 전달하거나 고객센터(1577-1234)로 신고해 주세요.",
          keywords: ["에스컬레이터", "고장"],
          qaCnt: 521,
        },
      ],
    }),

  // GET /api/v1/faq/recommendations/:analysisId
  // → gold_faq_candidate + gold_synonym_candidate
  getCandidates: (analysisId, token) =>
    mock({
      candidates: [
        {
          candidateId: 1,             // gold_faq_candidate.candidate_id
          analysisId: analysisId,
          clusterId: 5,               // gold_cluster.cluster_id
          candidateType: "NEW",       // NEW / EXPAND
          standardQuestion: "열차 내 냉난방 온도 조절 요청",
          answerDraft: "열차 내 온도는 혼잡도에 따라 자동 조절되나, 불편 시 열차번호와 함께 민원을 접수해 주시면 즉시 조치하겠습니다.",
          reviewStatus: "PENDING",    // PENDING / ACCEPTED / REJECTED
          occurrenceCount: 412,       // gold_faq_candidate.occurrence_count
          representativeKeywords: ["에어컨", "온도", "덥다"], // gold_faq_candidate.representative_keywords
          synonyms: [                 // gold_synonym_candidate
            { synonymId: 1, text: "에어컨",  type: "SYNONYM" },
            { synonymId: 2, text: "에어콘",  type: "TYPO" },
            { synonymId: 3, text: "덥다",    type: "SYNONYM" },
            { synonymId: 4, text: "더워요",  type: "SYNONYM" },
            { synonymId: 5, text: "냉방조절", type: "ABBR" },
          ],
          accuracyGain: 18.4,         // gold_performance_comparison.accuracy_gain
          matchScore: 0.91,           // gold_faq_candidate_match.match_score
        },
        {
          candidateId: 2,
          analysisId: analysisId,
          clusterId: 8,
          candidateType: "NEW",
          standardQuestion: "에스컬레이터 고장 신고",
          answerDraft: "시설물 고장 신고는 역무원에게 직접 전달하시거나, 고객센터(1577-1234)로 신고 부탁드립니다.",
          reviewStatus: "PENDING",
          occurrenceCount: 271,
          representativeKeywords: ["에스컬레이터", "고장", "작동"],
          synonyms: [
            { synonymId: 6, text: "에스컬레이터", type: "SYNONYM" },
            { synonymId: 7, text: "엘리베이터",   type: "SYNONYM" },
            { synonymId: 8, text: "고장",         type: "SYNONYM" },
            { synonymId: 9, text: "작동안함",      type: "TYPO" },
          ],
          accuracyGain: 12.1,
          matchScore: 0.83,
        },
        {
          candidateId: 3,
          analysisId: analysisId,
          clusterId: 11,
          candidateType: "EXPAND",    // 기존 FAQ 키워드 확장
          standardQuestion: "수유실 위치 안내",
          answerDraft: "수유실은 서울역, 강남역, 잠실역 등 주요 역사에 설치되어 있습니다. 역 안내소에 문의하시면 정확한 위치를 알려드립니다.",
          reviewStatus: "PENDING",
          occurrenceCount: 134,
          representativeKeywords: ["수유실", "수유칸", "아기"],
          synonyms: [
            { synonymId: 10, text: "수유실", type: "SYNONYM" },
            { synonymId: 11, text: "수유칸", type: "SYNONYM" },
            { synonymId: 12, text: "유아",   type: "SYNONYM" },
            { synonymId: 13, text: "아기",   type: "SYNONYM" },
          ],
          accuracyGain: 8.7,
          matchScore: 0.78,
        },
      ],
    }),

  // POST /api/v1/faq/apply
  // → gold_faq_action_log (ACCEPT / REJECT)
  apply: (body, token) =>
    mock(null),
  // body 예시: { candidateId: 1, action: "ACCEPT", note: "승인 사유" }

  // PATCH /api/v1/faq/:faqId
  // → gold_faq_edit_history
  update: (faqId, body, token) => mock(null),

  // DELETE /api/v1/faq/:faqId
  remove: (faqId, token) => mock(null),
};