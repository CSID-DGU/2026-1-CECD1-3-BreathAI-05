"""백엔드 페이로드 빌더 — Readme/AI_to_backend.md 스펙 전체 산출.

입력 (모두 dataset/):
  tota24_unanswered_2026Q1.csv   (원본)
  preprocessed_full.csv          (전처리: date/labelValid/labelMatchProbability)
  clusters.csv                   (UMAP 좌표 + clusterId)
  cluster_summary.csv            (size, isNoise, topKeywords)
  faq_candidates_final.json      (run_pipeline 산출 — 답변+평가)

출력:
  dataset/backend_payload.json   (백엔드가 그대로 소비할 페이로드)

설계:
  - 13개 top-level 필드 모두 채움
  - burstKeywords/surgeDetection: 분기 1개라 빈 배열 (직전 분기 데이터 도착 시 채움)
  - heuristic 정의는 코드 상단 상수로 분리 → 백엔드/멘토 합의 시 즉시 수정 가능
  - 내부 메타(tag/answerMeta/evaluation/refinement/oos_case)는 페이로드에서 제거
"""
from __future__ import annotations

import json
import unicodedata
import uuid
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

import pandas as pd


def _nfd(p):
    return Path(unicodedata.normalize('NFD', str(p)))


# ---------------------------------------------------------------------------
# 경로 / 상수
# ---------------------------------------------------------------------------
DATA_DIR = _nfd(Path(__file__).resolve().parent.parent / 'dataset')
INPUT_CSV = DATA_DIR / 'tota24_unanswered_2026Q1.csv'
PREPROC_CSV = DATA_DIR / 'preprocessed_full.csv'
CLUSTERS_CSV = DATA_DIR / 'clusters.csv'
SUMMARY_CSV = DATA_DIR / 'cluster_summary.csv'
FAQ_JSON = DATA_DIR / 'faq_candidates_final.json'
OUTPUT_JSON = DATA_DIR / 'backend_payload.json'

# heuristic 임계값 — 백엔드/멘토 합의로 조정 가능
CORRECT_THRESHOLD = 0.1       # labelValid=1 AND prob >= 이면 CORRECT, 아니면 LOW_QUALITY (B안)
FALSE_POSITIVE_RATE = 0.05    # 임시값 — 실제 측정값 도착 시 교체
EVAL_THRESHOLD = 0.75         # 평가 임계값 — 향후 모델 임계값과 일치시킬 것

# 옵션 (preprocess.py 정책 반영)
OPTIONS = {'isMaskingEnabled': True, 'isTranslationEnabled': False}

# tag → 미답변 사유 매핑
TAG_TO_REASON = {
    'FAQ': '키워드 미등록 (FAQ 신설 대상)',
    'OUT_OF_SCOPE': '소관 외 (타 기관/회사 정보 등)',
    'EMERGENCY': '긴급 상황 안내 필요',
    'REJECT': '보안/정책상 답변 불가',
}


# ---------------------------------------------------------------------------
# 분류 유틸
# ---------------------------------------------------------------------------
def classify_log_type(label_valid: int, prob: float) -> str:
    """logType: CORRECT / LOW_QUALITY / UNANSWER"""
    if label_valid == 0:
        return 'UNANSWER'
    if prob >= CORRECT_THRESHOLD:
        return 'CORRECT'
    return 'LOW_QUALITY'


def _safe_int(v, default=0):
    try:
        return int(float(v))
    except Exception:
        return default


def _safe_float(v, default=0.0):
    try:
        return float(v)
    except Exception:
        return default


# ---------------------------------------------------------------------------
# 빌더 — 페이로드 섹션별
# ---------------------------------------------------------------------------
def build_system_status(preproc: pd.DataFrame) -> dict:
    correct = low = unanswer = 0
    for _, row in preproc.iterrows():
        lv = _safe_int(row.get('labelValid'))
        prob = _safe_float(row.get('labelMatchProbability'))
        kind = classify_log_type(lv, prob)
        if kind == 'UNANSWER': unanswer += 1
        elif kind == 'CORRECT': correct += 1
        else: low += 1
    return {
        'totalLogCount': len(preproc),
        'samplingStatus': {
            'correct': correct,
            'lowQuality': low,
            'unanswer': unanswer,
        },
    }


def build_performance(faq_records: list, system_status: dict,
                      after_unanswer_count: int) -> dict:
    total = system_status['totalLogCount']
    unanswer = system_status['samplingStatus']['unanswer']
    # resolvedCountByAI: tag=FAQ + needsReview=False 클러스터의 size 합
    resolved = sum(
        r.get('clusterSize', 0) for r in faq_records
        if r.get('tag') == 'FAQ'
        and not (r.get('evaluation') or {}).get('needs_review_v2', True)
    )
    current_rate = round(unanswer / total * 100, 2) if total else 0.0
    after_rate = round(after_unanswer_count / total * 100, 2) if total else 0.0
    # predictedAccuracyGain = 미답변율 감소량 (현재 - AI 적용 후, 백분위 차)
    gain = round(current_rate - after_rate, 2)
    return {
        'currentUnanswerRate': current_rate,
        'predictedAccuracyGain': gain,
        'resolvedCountByAI': resolved,
    }


def build_trend(preproc: pd.DataFrame) -> list:
    counts = defaultdict(lambda: {'unanswerCount': 0, 'lowQualityCount': 0})
    for _, row in preproc.iterrows():
        date_str = str(row.get('date', '')).strip()
        if not date_str or date_str == 'nan':
            continue
        try:
            dt = datetime.fromisoformat(date_str.split(' ')[0])
            mmdd = dt.strftime('%m-%d')
        except Exception:
            continue
        lv = _safe_int(row.get('labelValid'))
        prob = _safe_float(row.get('labelMatchProbability'))
        kind = classify_log_type(lv, prob)
        if kind == 'UNANSWER': counts[mmdd]['unanswerCount'] += 1
        elif kind == 'LOW_QUALITY': counts[mmdd]['lowQualityCount'] += 1
    return [
        {'date': d, 'unanswerCount': v['unanswerCount'],
         'lowQualityCount': v['lowQualityCount']}
        for d, v in sorted(counts.items())
    ]


def build_unanswer_analysis(faq_records: list) -> dict:
    cnt = Counter()
    for r in faq_records:
        reason = TAG_TO_REASON.get(r.get('tag', ''), '기타')
        cnt[reason] += r.get('clusterSize', 0)
    return {
        'byReason': [{'reason': k, 'count': v} for k, v in cnt.most_common()],
    }


def build_clustering_points(clusters: pd.DataFrame, preproc: pd.DataFrame) -> list:
    # logId → logType 매핑
    label_map = {}
    for _, row in preproc.iterrows():
        lv = _safe_int(row.get('labelValid'))
        prob = _safe_float(row.get('labelMatchProbability'))
        label_map[str(row['logId'])] = classify_log_type(lv, prob)
    points = []
    for _, row in clusters.iterrows():
        log_id = str(row['logId'])
        points.append({
            'x': round(_safe_float(row['umap_x']), 4),
            'y': round(_safe_float(row['umap_y']), 4),
            'clusterLabel': _safe_int(row['clusterId']),
            'text': str(row['text']),
            'sourceLogSeqNum': _safe_int(log_id),
            'logType': label_map.get(log_id, 'UNANSWER'),
        })
    return points


def build_cluster_names(summary: pd.DataFrame, faq_records: list) -> list:
    sq_map = {r['clusterLabel']: r.get('standardQuestion', '').strip()
              for r in faq_records}
    out = []
    for _, row in summary.iterrows():
        cid = _safe_int(row['clusterId'])
        kw_str = str(row.get('topKeywords', ''))
        kws = [k.strip() for k in kw_str.split(',') if k.strip()]
        # 이름: standardQuestion 우선, 없으면 첫 키워드 / 노이즈는 고정
        if cid == -1:
            name = '기타/노이즈'
        else:
            name = sq_map.get(cid) or (kws[0] if kws else f'클러스터 {cid}')
        out.append({
            'clusterLabel': cid,
            'name': name,
            'topKeywords': kws,
            'size': _safe_int(row['size']),
        })
    return out


def transform_faq_candidates(records: list) -> list:
    """AI 내부 키 → 백엔드 스펙. 내부 메타(tag/answerMeta/evaluation 등) 제거."""
    out = []
    for i, r in enumerate(records, 1):
        # synonyms: 이미 [{text, type}] 형식이지만 안전하게 변환
        synonyms = r.get('synonyms', [])
        if synonyms and isinstance(synonyms[0], str):
            synonyms = [{'text': s, 'type': 'SYNONYM'} for s in synonyms]
        out.append({
            'candidateId': 100 + i,                                  # 자동 부여
            'clusterLabel': r.get('clusterLabel'),
            'candidateType': r.get('candidateType', 'NEW'),
            'standardQuestion': r.get('standardQuestion', ''),
            'representativeKeywords': r.get('topKeywords', []),       # ← 이름 변환
            'answerDraft': r.get('answerDraft', ''),
            'synonyms': synonyms,
            'occurrenceCount': r.get('clusterSize', 0),               # ← 이름 변환
            'matchedFaqs': r.get('matchedFaqs', []),
        })
    return out


def build_evaluation_section(faq_records: list, system_status: dict,
                             performance: dict, period_end: str,
                             after_unanswer_count: int) -> dict:
    total = system_status['totalLogCount']
    after_rate = round(after_unanswer_count / total * 100, 2) if total else 0.0
    return {
        'beforeStatDate': period_end,
        'afterUnanswerCount': after_unanswer_count,
        'afterUnanswerRate': after_rate,
        'accuracyGain': performance['predictedAccuracyGain'],
        'resolvedCountByAI': performance['resolvedCountByAI'],
        'falsePositiveRate': FALSE_POSITIVE_RATE,
        'evalThreshold': EVAL_THRESHOLD,
    }


# ---------------------------------------------------------------------------
# 메인
# ---------------------------------------------------------------------------
def main() -> None:
    # 입력 로드
    preproc = pd.read_csv(PREPROC_CSV, encoding='utf-8-sig')
    clusters = pd.read_csv(CLUSTERS_CSV, encoding='utf-8-sig')
    summary = pd.read_csv(SUMMARY_CSV, encoding='utf-8-sig')
    faq_records = json.loads(FAQ_JSON.read_text(encoding='utf-8'))['faqCandidates']
    print(f'입력: {len(preproc)}건 로그, {len(clusters)}건 클러스터링, '
          f'{len(summary)}개 클러스터, {len(faq_records)}개 FAQ 후보')

    # 기간 계산
    dates = pd.to_datetime(preproc['date'], errors='coerce').dropna()
    period_start = dates.min().strftime('%Y-%m-%d')
    period_end = dates.max().strftime('%Y-%m-%d')

    # 빌더 호출
    system_status = build_system_status(preproc)
    # 잔존 미답변 (needsReview=True인 클러스터의 size 합) — 두 빌더가 공유
    after_unanswer_count = sum(
        r.get('clusterSize', 0) for r in faq_records
        if (r.get('evaluation') or {}).get('needs_review_v2', False)
    )
    performance = build_performance(faq_records, system_status, after_unanswer_count)
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace('+00:00', 'Z')

    payload = {
        'analysisId': int(datetime.now().strftime('%Y%m%d%H%M%S')),
        'uploadId': str(uuid.uuid4()),
        'fileName': INPUT_CSV.name,
        'status': 'COMPLETED',
        'submittedAt': now,
        'startedAt': now,
        'finishedAt': now,
        'periodStartDate': period_start,
        'periodEndDate': period_end,
        'options': OPTIONS,
        'systemStatus': system_status,
        'performanceMetrics': performance,
        'trend': build_trend(preproc),
        # 분기 1개라 비교 불가 — 직전 분기 데이터 도착 시 채울 것
        'burstKeywords': [],
        'unanswerAnalysis': build_unanswer_analysis(faq_records),
        'clusteringView': {
            'points': build_clustering_points(clusters, preproc),
            'clusterNames': build_cluster_names(summary, faq_records),
        },
        'faqCandidates': transform_faq_candidates(faq_records),
        'evaluation': build_evaluation_section(faq_records, system_status,
                                               performance, period_end,
                                               after_unanswer_count),
        # 분기 1개라 비교 불가 — 직전 분기 데이터 도착 시 채울 것
        'surgeDetection': [],
    }

    OUTPUT_JSON.write_text(json.dumps(payload, ensure_ascii=False, indent=2),
                           encoding='utf-8')
    size = OUTPUT_JSON.stat().st_size
    print(f'저장: {OUTPUT_JSON.name} ({size:,}B)')
    print()
    print('=== 페이로드 요약 ===')
    print(f'  analysisId        : {payload["analysisId"]}')
    print(f'  기간              : {period_start} ~ {period_end}')
    print(f'  systemStatus      : '
          f'total={system_status["totalLogCount"]}, '
          f'unanswer={system_status["samplingStatus"]["unanswer"]}, '
          f'correct={system_status["samplingStatus"]["correct"]}, '
          f'lowQuality={system_status["samplingStatus"]["lowQuality"]}')
    print(f'  performanceMetrics: rate={performance["currentUnanswerRate"]}%, '
          f'resolved={performance["resolvedCountByAI"]}, '
          f'gain={performance["predictedAccuracyGain"]}%')
    print(f'  trend             : {len(payload["trend"])}일치 데이터')
    print(f'  clusteringView    : points={len(payload["clusteringView"]["points"])}, '
          f'clusterNames={len(payload["clusteringView"]["clusterNames"])}')
    print(f'  faqCandidates     : {len(payload["faqCandidates"])}건')
    print(f'  unanswerAnalysis  : {len(payload["unanswerAnalysis"]["byReason"])}개 사유')
    print(f'  burstKeywords     : [] (직전 분기 데이터 필요)')
    print(f'  surgeDetection    : [] (직전 분기 데이터 필요)')


if __name__ == '__main__':
    main()
