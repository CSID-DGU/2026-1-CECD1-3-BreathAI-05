import { useEffect, useState } from "react";
import { dashApi } from "../api/index.js";

export default function DashboardPage() {
  const [data, setData] = useState(null);

  useEffect(() => {
    dashApi.getUsage().then((r) => setData(r.data));
  }, []);

  if (!data) return <div style={{ padding: 40, color: "#6B7280" }}>불러오는 중...</div>;

  const maxKw = data.topKeywords[0]?.count || 1;
  const maxMonthly = Math.max(...data.monthly.map(
    (m) => m.answerCount + m.lowQualityCount + m.unansweredCount
  ));

  return (
    <div style={S.page}>
      <div style={S.pageTitle}>대시보드</div>
      <div style={S.pageSub}>또타24 챗봇 응답 현황을 확인합니다.</div>

      {/* 통계 카드 */}
      <div style={S.statGrid}>
        {[
          { label: "총 대화 수",      value: data.totalChats.toLocaleString(),           sub: "이번 달 누적",                            color: "#111827" },
          { label: "정답 (≥75%)",     value: `${data.answeredRate}%`,                    sub: `${data.answerCount.toLocaleString()}건`,    color: "#1565C0" },
          { label: "오답변 (25~75%)", value: `${data.lowQualityRate}%`,                  sub: `${data.lowQualityCount.toLocaleString()}건`, color: "#7C3AED" },
          { label: "미답변 (<25%)",   value: `${data.unansweredRate}%`,                  sub: `${data.unansweredCount.toLocaleString()}건`, color: "#EF9F27" },
        ].map((s) => (
          <div key={s.label} style={S.statCard}>
            <div style={S.statLabel}>{s.label}</div>
            <div style={{ ...S.statValue, color: s.color }}>{s.value}</div>
            <div style={S.statSub}>{s.sub}</div>
          </div>
        ))}
      </div>

      {/* 월별 + 키워드 */}
      <div style={S.row2}>
        <div style={S.card}>
          <div style={S.cardHeader}>
            <span style={S.cardTitle}>월별 응답 현황</span>
            <div style={{ display: "flex", gap: 10, fontSize: 11 }}>
              <span>🔵 정답</span><span>🟣 오답변</span><span>🟡 미답변</span>
            </div>
          </div>
          <div style={{ padding: 20, display: "flex", alignItems: "flex-end", gap: 10, height: 140 }}>
            {data.monthly.map((m) => {
              const scale = 100 / maxMonthly;
              return (
                <div key={m.month} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 4 }}>
                  <div style={{ width: "100%", display: "flex", flexDirection: "column", height: 100, justifyContent: "flex-end" }}>
                    <div style={{ background: "#EF9F27", height: `${m.unansweredCount * scale}px`, borderRadius: "3px 3px 0 0" }} />
                    <div style={{ background: "#7C3AED", height: `${m.lowQualityCount * scale}px` }} />
                    <div style={{ background: "#1976D2", height: `${m.answerCount * scale}px`, borderRadius: "0 0 3px 3px" }} />
                  </div>
                  <span style={{ fontSize: 10, color: "#6B7280" }}>{m.month}</span>
                </div>
              );
            })}
          </div>
        </div>

        <div style={S.card}>
          <div style={S.cardHeader}><span style={S.cardTitle}>미답변 상위 키워드</span></div>
          <div style={{ padding: 20 }}>
            {data.topKeywords.map((k) => (
              <div key={k.word} style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10, fontSize: 12 }}>
                <span style={{ width: 80, color: "#374151" }}>{k.word}</span>
                <div style={{ flex: 1, height: 6, background: "#F3F4F6", borderRadius: 3 }}>
                  <div style={{ width: `${(k.count / maxKw) * 100}%`, height: "100%", background: "#1976D2", borderRadius: 3 }} />
                </div>
                <span style={{ width: 36, textAlign: "right", color: "#6B7280" }}>{k.count}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 미답변 사유 세분화 + 급증 질문 */}
      <div style={{ ...S.row2, marginTop: 16 }}>

        {/* gold_unanswer_stat */}
        <div style={S.card}>
          <div style={S.cardHeader}>
            <span style={S.cardTitle}>미답변 사유 분석</span>
            <span style={S.badge}>gold_unanswer_stat</span>
          </div>
          <div style={{ padding: 20 }}>
            {data.unansweredReasons.map((r) => (
              <div key={r.reason} style={{ marginBottom: 14 }}>
                <div style={{ display: "flex", justifyContent: "space-between", fontSize: 12, marginBottom: 5 }}>
                  <span style={{ color: "#374151", fontWeight: 500 }}>{r.reason}</span>
                  <span style={{ color: "#6B7280" }}>{r.count.toLocaleString()}건 ({r.rate}%)</span>
                </div>
                <div style={{ height: 6, background: "#F3F4F6", borderRadius: 3 }}>
                  <div style={{
                    width: `${r.rate}%`, height: "100%", borderRadius: 3,
                    background: r.reason === "키워드 미등록" ? "#1976D2"
                      : r.reason === "외국어 질의" ? "#7C3AED"
                      : r.reason === "보안/정책상 답변 불가" ? "#E53935"
                      : "#EF9F27",
                  }} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* gold_surge_detection */}
        <div style={S.card}>
          <div style={S.cardHeader}>
            <span style={S.cardTitle}>⚡ 급증 질문 탐지</span>
            <span style={S.badge}>gold_surge_detection</span>
          </div>
          <div style={{ padding: 0 }}>
            {data.surges.map((s, i) => (
              <div key={s.keyword} style={{
                display: "flex", alignItems: "center", justifyContent: "space-between",
                padding: "12px 20px",
                borderBottom: i < data.surges.length - 1 ? "1px solid #F3F4F6" : "none",
              }}>
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  {s.isNew && (
                    <span style={{ fontSize: 10, fontWeight: 600, padding: "1px 6px", borderRadius: 8, background: "#FDECEA", color: "#B91C1C" }}>NEW</span>
                  )}
                  <span style={{ fontSize: 13, fontWeight: 500, color: "#111827" }}>{s.keyword}</span>
                </div>
                <div style={{ display: "flex", gap: 12, alignItems: "center", fontSize: 12 }}>
                  <span style={{ color: "#6B7280" }}>{s.keywordCount}건</span>
                  <span style={{ color: "#E53935", fontWeight: 600 }}>↑ {s.increasedRate}%</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* gold_performance_comparison */}
      <div style={{ ...S.card, marginTop: 16 }}>
        <div style={S.cardHeader}>
          <span style={S.cardTitle}>개선 전·후 미답변율 비교</span>
          <span style={S.badge}>gold_performance_comparison</span>
        </div>
        <div style={{ padding: 20, display: "grid", gridTemplateColumns: "repeat(5, 1fr)", gap: 16 }}>
          {[
            { label: "예상 미답변율",    value: `${data.performance.afterUnanswerRate}%`, color: "#43A047" },
            { label: "정확도 향상",      value: `+${data.performance.accuracyGain}%`,     color: "#1565C0" },
            { label: "오답 발생률",      value: `${data.performance.falsePositiveRate}`,   color: "#EF9F27" },
            { label: "적용 임계값",      value: data.performance.evalThreshold,            color: "#7C3AED" },
            { label: "AI 해결 건수",     value: `${data.performance.resolvedCountByAi.toLocaleString()}건`, color: "#43A047" },
          ].map((p) => (
            <div key={p.label} style={{ textAlign: "center", padding: "14px", background: "#F9FAFB", borderRadius: 10 }}>
              <div style={{ fontSize: 11, color: "#6B7280", marginBottom: 6 }}>{p.label}</div>
              <div style={{ fontSize: 22, fontWeight: 700, color: p.color }}>{p.value}</div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}

const S = {
  page:       { padding: 28 },
  pageTitle:  { fontSize: 20, fontWeight: 700, color: "#111827", marginBottom: 4 },
  pageSub:    { fontSize: 13, color: "#6B7280", marginBottom: 24 },
  statGrid:   { display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 14, marginBottom: 20 },
  statCard:   { background: "#fff", borderRadius: 12, padding: "18px 20px", boxShadow: "0 1px 3px rgba(0,0,0,.08)", border: "1px solid rgba(0,0,0,.06)" },
  statLabel:  { fontSize: 12, color: "#6B7280", marginBottom: 6 },
  statValue:  { fontSize: 26, fontWeight: 700, lineHeight: 1 },
  statSub:    { fontSize: 11, color: "#6B7280", marginTop: 4 },
  row2:       { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 },
  card:       { background: "#fff", borderRadius: 12, boxShadow: "0 1px 3px rgba(0,0,0,.08)", border: "1px solid rgba(0,0,0,.06)" },
  cardHeader: { padding: "14px 20px", borderBottom: "1px solid #F3F4F6", display: "flex", alignItems: "center", justifyContent: "space-between" },
  cardTitle:  { fontSize: 14, fontWeight: 600, color: "#111827" },
  badge:      { fontSize: 10, color: "#9CA3AF", fontFamily: "monospace" },
};