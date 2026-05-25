import { useState } from "react";
import { dashApi } from "../api/index.js";

export default function AnalyzePage() {
  const [tab, setTab]               = useState("upload"); // upload | visual | recommend
  const [file, setFile]             = useState(null);
  const [drag, setDrag]             = useState(false);
  const [uploading, setUploading]   = useState(false);
  const [uploadResult, setUploadResult] = useState(null);
  const [running, setRunning]       = useState(false);
  const [step, setStep]             = useState(-1);
  const [done, setDone]             = useState(false);
  const [candidates, setCandidates] = useState([]);

  const STEPS = ["데이터 전처리", "문장 임베딩", "군집화 (HDBSCAN)", "FAQ 초안 생성", "검증 완료"];

  const handleFile = (f) => {
    if (!f.name.endsWith(".xlsx")) { alert(".xlsx 파일만 업로드 가능합니다."); return; }
    setFile(f);
  };

  const doUpload = async () => {
    if (!file) return;
    setUploading(true);
    const res = await dashApi.uploadExcel(file);
    setUploading(false);
    if (res.success) setUploadResult(res.data);
  };

  const doAnalysis = async () => {
    if (!uploadResult) return;
    setRunning(true);
    setDone(false);
    setStep(-1);
    for (let i = 0; i < STEPS.length; i++) {
      await new Promise((r) => setTimeout(r, 700));
      setStep(i);
    }
    await new Promise((r) => setTimeout(r, 500));
    setDone(true);
    setRunning(false);
    // mock FAQ 후보 세팅
    setCandidates([
      { id: "c1", question: "열차 내 냉난방 온도 조절 요청", answer: "열차 내 온도는 혼잡도에 따라 자동 조절되나, 불편 시 열차번호와 함께 민원을 접수해 주시면 즉시 조치하겠습니다.", synonyms: ["에어컨","에어콘","덥다","냉방조절"], gain: 18.4, count: 412, status: "pending" },
      { id: "c2", question: "에스컬레이터 고장 신고", answer: "시설물 고장 신고는 역무원에게 직접 전달하시거나, 고객센터(1577-1234)로 신고 부탁드립니다.", synonyms: ["에스컬레이터","엘리베이터","고장"], gain: 12.1, count: 271, status: "pending" },
      { id: "c3", question: "수유실 위치 안내", answer: "수유실은 서울역, 강남역, 잠실역 등 주요 역사에 설치되어 있습니다.", synonyms: ["수유실","수유칸","유아","아기"], gain: 8.7, count: 134, status: "pending" },
    ]);
    setTab("recommend");
  };

  const handleCandidate = (id, action) => {
    setCandidates((prev) =>
      prev.map((c) => c.id === id ? { ...c, status: action } : c)
    );
  };

  return (
    <div style={S.page}>
      <div style={S.pageTitle}>신규 데이터 분석</div>
      <div style={S.pageSub}>미답변 로그를 업로드하고 AI 분석을 실행합니다.</div>

      {/* 탭 */}
      <div style={S.tabs}>
        {[
          { key: "upload",    label: "① 업로드 · 분석" },
          { key: "visual",    label: "② 시각화 · 비교" },
          { key: "recommend", label: `③ FAQ 추천 ${candidates.length > 0 ? `(${candidates.filter(c => c.status === "pending").length})` : ""}` },
        ].map((t) => (
          <div key={t.key} style={{ ...S.tab, ...(tab === t.key ? S.tabActive : {}) }}
            onClick={() => setTab(t.key)}>{t.label}</div>
        ))}
      </div>

      {/* ── 탭 1: 업로드 · 분석 ── */}
      {tab === "upload" && (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: 16 }}>
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>

            {/* 업로드 카드 */}
            <div style={S.card}>
              <div style={S.cardHeader}>
                <span style={S.cardTitle}>① 신규 로그 엑셀 업로드</span>
                <span style={S.badge}>1달 분량</span>
              </div>
              <div style={S.cardBody}>
                <input type="file" accept=".xlsx" style={{ display: "none" }} id="fileInput"
                  onChange={(e) => handleFile(e.target.files[0])} />
                <div
                  style={{ ...S.dropzone, ...(drag ? S.dropzoneDrag : {}) }}
                  onClick={() => document.getElementById("fileInput").click()}
                  onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
                  onDragLeave={() => setDrag(false)}
                  onDrop={(e) => { e.preventDefault(); setDrag(false); handleFile(e.dataTransfer.files[0]); }}>
                  <div style={{ fontSize: 36, marginBottom: 10 }}>📂</div>
                  <div style={{ fontSize: 14, fontWeight: 500, color: "#374151", marginBottom: 4 }}>
                    {file ? file.name : "엑셀 파일을 드래그하거나 클릭하여 선택"}
                  </div>
                  <div style={{ fontSize: 12, color: "#9CA3AF" }}>.xlsx 형식, 최대 50MB</div>
                </div>

                {file && !uploadResult && (
                  <button style={{ ...S.btnPrimary, marginTop: 12 }}
                    onClick={doUpload} disabled={uploading}>
                    {uploading ? "업로드 중..." : "업로드 시작"}
                  </button>
                )}

                {uploadResult && (
                  <div style={S.successBox}>
                    📊 총 <strong>{uploadResult.totalRows.toLocaleString()}건</strong> 적재 완료
                    &nbsp;(중복 제거: {uploadResult.totalRows - uploadResult.insertedRows}건)
                  </div>
                )}
              </div>
            </div>

            {/* 분석 실행 카드 */}
            <div style={S.card}>
              <div style={S.cardHeader}>
                <span style={S.cardTitle}>② AI 분석 실행</span>
                {done && <span style={{ ...S.badge, background: "#D1FAE5", color: "#065F46" }}>완료</span>}
              </div>
              <div style={S.cardBody}>
                {!running && !done && (
                  <>
                    <p style={{ fontSize: 13, color: "#6B7280", marginBottom: 16 }}>
                      전처리 → 임베딩 → 군집화 → FAQ 생성 → 검증 순으로 진행됩니다.
                    </p>
                    <button style={{ ...S.btnPrimary, opacity: !uploadResult ? 0.5 : 1 }}
                      onClick={doAnalysis} disabled={!uploadResult}>
                      분석 시작
                    </button>
                  </>
                )}

                {(running || done) && (
                  <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                    {STEPS.map((s, i) => {
                      const isDone = step >= i;
                      const isRun  = step === i && running;
                      return (
                        <div key={s} style={{ display: "flex", alignItems: "center", gap: 10, fontSize: 13 }}>
                          <div style={{
                            width: 24, height: 24, borderRadius: "50%", display: "flex",
                            alignItems: "center", justifyContent: "center", fontSize: 11, fontWeight: 600,
                            background: isDone ? "#D1FAE5" : isRun ? "#DBEAFE" : "#F3F4F6",
                            color: isDone ? "#065F46" : isRun ? "#1D4ED8" : "#9CA3AF",
                          }}>
                            {isDone ? "✓" : i + 1}
                          </div>
                          <span style={{ color: isDone ? "#111827" : isRun ? "#1D4ED8" : "#9CA3AF", fontWeight: isDone || isRun ? 500 : 400 }}>
                            {s}
                          </span>
                          {isRun && <span style={{ fontSize: 11, color: "#1D4ED8" }}>처리 중...</span>}
                        </div>
                      );
                    })}
                    {done && (
                      <div style={{ ...S.successBox, marginTop: 8 }}>
                        🎉 분석 완료! <strong>FAQ 추천</strong> 탭에서 결과를 확인하세요.
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* 오른쪽 — 분석 이력 */}
          <div style={S.card}>
            <div style={S.cardHeader}><span style={S.cardTitle}>분석 이력</span></div>
            <div style={{ padding: 0 }}>
              {[
                { id: 1, status: "COMPLETED", fileName: "unanswered_log_202604.xlsx", rows: 8166, date: "2026-04-19" },
                { id: 2, status: "COMPLETED", fileName: "unanswered_log_202603.xlsx", rows: 7923, date: "2026-03-12" },
                { id: 3, status: "FAIL",      fileName: "unanswered_log_202602.xlsx", rows: 5210, date: "2026-02-08" },
                ].map((r, i) => (
                <div key={i} style={{ padding: "12px 16px", borderBottom: "1px solid #F3F4F6", fontSize: 12 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                      <span style={{ fontFamily: "monospace", color: "#374151" }}>analysis_id: {r.id}</span>
                   <span style={{
                        fontSize: 10, fontWeight: 500, padding: "2px 8px", borderRadius: 10,
                     background: r.status === "COMPLETED" ? "#D1FAE5" : "#FDECEA",
                      color: r.status === "COMPLETED" ? "#065F46" : "#B91C1C",
                    }}>{r.status}</span>
    </div>
    <div style={{ color: "#6B7280" }}>{r.fileName}</div>
    <div style={{ color: "#9CA3AF", marginTop: 2 }}>{r.rows.toLocaleString()}건 · {r.date}</div>
  </div>
))}
            </div>
          </div>
        </div>
      )}

      {/* ── 탭 2: 시각화 ── */}
      {tab === "visual" && (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
          {/* 기존 vs 신규 비교 */}
          <div style={S.card}>
            <div style={S.cardHeader}><span style={S.cardTitle}>응답 유형 비교</span></div>
            <div style={S.cardBody}>
              {[
                { label: "기존 데이터", answered: 71, unanswered: 29, color: "#1976D2" },
                { label: "신규 데이터", answered: 73.4, unanswered: 26.6, color: "#43A047" },
              ].map((d) => (
                <div key={d.label} style={{ marginBottom: 20 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginBottom: 6 }}>
                    <span style={{ fontWeight: 500 }}>{d.label}</span>
                    <span style={{ color: d.color, fontWeight: 600 }}>답변율 {d.answered}%</span>
                  </div>
                  <div style={{ height: 24, background: "#F3F4F6", borderRadius: 6, overflow: "hidden", display: "flex" }}>
                    <div style={{ width: `${d.answered}%`, background: d.color, borderRadius: "6px 0 0 6px", transition: "width .6s" }} />
                    <div style={{ width: `${d.unanswered}%`, background: "#EF9F27" }} />
                  </div>
                  <div style={{ display: "flex", gap: 12, fontSize: 11, color: "#6B7280", marginTop: 4 }}>
                    <span>🔵 답변 {d.answered}%</span>
                    <span>🟡 미답변 {d.unanswered}%</span>
                  </div>
                </div>
              ))}
              <div style={{ padding: "12px 14px", background: "#EFF8FF", borderRadius: 8, fontSize: 13 }}>
                미답변율 <strong style={{ color: "#1565C0" }}>29% → 26.6%</strong> 로 개선 예상
              </div>
            </div>
          </div>

          {/* 예상 효과 */}
          <div style={S.card}>
            <div style={S.cardHeader}><span style={S.cardTitle}>분석 결과 요약</span></div>
            <div style={S.cardBody}>
              {[
                { label: "총 미답변 로그",    value: "8,166건",  color: "#111827" },
                { label: "예상 해소 건수",    value: "1,200건",  color: "#43A047" },
                { label: "예상 해소율",       value: "14.7%",   color: "#43A047" },
                { label: "예상 정확도 향상",  value: "+12.4%",  color: "#1565C0" },
                { label: "FAQ 후보 생성 수",  value: "38개",    color: "#1565C0" },
              ].map((r) => (
                <div key={r.label} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "10px 0", borderBottom: "1px solid #F3F4F6", fontSize: 13 }}>
                  <span style={{ color: "#6B7280" }}>{r.label}</span>
                  <strong style={{ color: r.color }}>{r.value}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ── 탭 3: FAQ 추천 ── */}
      {tab === "recommend" && (
        <div>
          {candidates.length === 0 ? (
            <div style={{ ...S.card, ...S.cardBody, textAlign: "center", padding: 48, color: "#9CA3AF" }}>
              <div style={{ fontSize: 36, marginBottom: 10 }}>🔍</div>
              <div>분석을 먼저 실행해주세요.</div>
            </div>
          ) : (
            candidates.map((c) => (
              <div key={c.id} style={{ ...S.card, marginBottom: 12, padding: 18 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 8 }}>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 600, color: "#111827", marginBottom: 4 }}>{c.question}</div>
                    <div style={{ display: "flex", gap: 8, alignItems: "center", fontSize: 12 }}>
                      <span style={{ color: "#43A047", fontWeight: 600 }}>↑ {c.gain}% 정확도 향상</span>
                      <span style={{ color: "#9CA3AF" }}>매칭 {c.count}건</span>
                    </div>
                  </div>
                  {c.status === "pending" ? (
                    <div style={{ display: "flex", gap: 6 }}>
                      <button style={S.btnAccept} onClick={() => handleCandidate(c.id, "accepted")}>✓ 승인</button>
                      <button style={S.btnReject} onClick={() => handleCandidate(c.id, "rejected")}>✗ 반려</button>
                    </div>
                  ) : (
                    <span style={{
                      fontSize: 12, fontWeight: 500, padding: "4px 12px", borderRadius: 20,
                      background: c.status === "accepted" ? "#D1FAE5" : "#FDECEA",
                      color: c.status === "accepted" ? "#065F46" : "#B91C1C",
                    }}>
                      {c.status === "accepted" ? "✅ 승인됨" : "❌ 반려됨"}
                    </span>
                  )}
                </div>
                <div style={{ fontSize: 12, color: "#6B7280", lineHeight: 1.6, marginBottom: 10 }}>{c.answer}</div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: 5 }}>
                    {c.synonyms.map((s) => (
                     <span key={s.synonymId} style={{
                        fontSize: 11, padding: "2px 8px",
                        background: s.type === "TYPO" ? "#FEF3C7" : "#EFF8FF",
                        color: s.type === "TYPO" ? "#92400E" : "#1565C0",
                        borderRadius: 12,
                        border: `1px solid ${s.type === "TYPO" ? "#FDE68A" : "#BFDBFE"}`,
                        marginRight: 4, marginBottom: 4,
                        }}>
                            {s.text}
                            <span style={{ fontSize: 9, marginLeft: 3, opacity: 0.6 }}>
                                {s.type === "TYPO" ? "오타" : s.type === "ABBR" ? "약어" : "유사어"}
                        </span>
                    </span>
                    ))}
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

const S = {
  page:        { padding: 28 },
  pageTitle:   { fontSize: 20, fontWeight: 700, color: "#111827", marginBottom: 4 },
  pageSub:     { fontSize: 13, color: "#6B7280", marginBottom: 20 },
  tabs:        { display: "flex", gap: 2, background: "#F3F4F6", borderRadius: 10, padding: 4, width: "fit-content", marginBottom: 20 },
  tab:         { padding: "8px 20px", borderRadius: 8, fontSize: 13, cursor: "pointer", color: "#6B7280" },
  tabActive:   { background: "#fff", color: "#1565C0", fontWeight: 600, boxShadow: "0 1px 3px rgba(0,0,0,.08)" },
  card:        { background: "#fff", borderRadius: 12, boxShadow: "0 1px 3px rgba(0,0,0,.08)", border: "1px solid rgba(0,0,0,.06)" },
  cardHeader:  { padding: "14px 20px", borderBottom: "1px solid #F3F4F6", display: "flex", alignItems: "center", justifyContent: "space-between" },
  cardTitle:   { fontSize: 14, fontWeight: 600, color: "#111827" },
  cardBody:    { padding: 20 },
  badge:       { fontSize: 11, fontWeight: 500, padding: "2px 9px", borderRadius: 20, background: "#DBEAFE", color: "#1E40AF" },
  dropzone:    { border: "2px dashed #D1D5DB", borderRadius: 12, padding: "32px 20px", textAlign: "center", cursor: "pointer", transition: "all .18s", background: "#fff" },
  dropzoneDrag:{ borderColor: "#1565C0", background: "#EFF8FF" },
  btnPrimary:  { padding: "10px 20px", background: "#1565C0", color: "#fff", border: "none", borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" },
  btnAccept:   { padding: "6px 14px", background: "#D1FAE5", color: "#065F46", border: "none", borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" },
  btnReject:   { padding: "6px 14px", background: "#FDECEA", color: "#B91C1C", border: "none", borderRadius: 8, fontSize: 12, fontWeight: 600, cursor: "pointer", fontFamily: "inherit" },
  successBox:  { marginTop: 10, padding: "10px 14px", background: "#D1FAE5", borderRadius: 8, fontSize: 13, color: "#065F46" },
};