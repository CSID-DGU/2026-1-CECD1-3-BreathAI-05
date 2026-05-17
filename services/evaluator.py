"""또타24 FAQ 후보 품질 평가 — BGE-reranker-v2-m3 (Cross-Encoder).

입력:  dataset/faq_candidates.json (faq_generator.py 산출물)
       dataset/clusters.csv         (클러스터별 원본 샘플)
출력:  dataset/faq_quality.json     (클러스터별 점수 + 검토 플래그)

설계:
  - FAQ 태그 클러스터만 평가 (OOS/EMERGENCY/REJECT는 평가 대상 아님)
  - standardQuestion ↔ 각 원본 샘플 N개 pair score (sigmoid 적용한 0~1)
  - 통계: mean / median / min / max + 낮은 점수 pair 목록
  - mean < REVIEW_THRESHOLD 이면 needsReview=True
"""
from __future__ import annotations

import json
import time
import unicodedata
from pathlib import Path

import numpy as np
import pandas as pd
import torch
from sentence_transformers import CrossEncoder


def _nfd(p: Path) -> Path:
    return Path(unicodedata.normalize('NFD', str(p)))


# 0. 경로/모델/하이퍼파라미터
DATA_DIR = _nfd(Path(__file__).resolve().parent.parent / 'dataset')
FAQ_JSON = DATA_DIR / 'faq_candidates.json'
CLUSTERS_CSV = DATA_DIR / 'clusters.csv'
OUTPUT_JSON = DATA_DIR / 'faq_quality.json'

MODEL_NAME = 'BAAI/bge-reranker-v2-m3'
MAX_LENGTH = 512
DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'

MAX_SAMPLES_PER_CLUSTER = 15   # 클러스터당 평가할 샘플 수
REVIEW_THRESHOLD = 0.1         # mean 점수 이하 → needsReview (분포 기반: 하위 ~50%)
LOW_PAIR_THRESHOLD = 0.4       # pair score 이하 → lowScorePairs


# 1. 모델 로드
def load_model() -> CrossEncoder:
    print(f'모델 로드: {MODEL_NAME}')
    model = CrossEncoder(
        MODEL_NAME,
        max_length=MAX_LENGTH,
        device=DEVICE,
        default_activation_function=torch.nn.Sigmoid(),
    )
    return model


# 2. 데이터 로드
def load_inputs() -> tuple[list[dict], dict[int, list[str]]]:
    payload = json.loads(FAQ_JSON.read_text(encoding='utf-8'))
    candidates = payload['faqCandidates']

    df = pd.read_csv(CLUSTERS_CSV, encoding='utf-8-sig')
    samples_by_cid: dict[int, list[str]] = {}
    for _, row in df.iterrows():
        cid = int(row['clusterId'])
        samples_by_cid.setdefault(cid, []).append(str(row['text']))
    return candidates, samples_by_cid


# 3. 평가
def evaluate_cluster(model: CrossEncoder, standard_q: str,
                     samples: list[str]) -> dict:
    samples = samples[:MAX_SAMPLES_PER_CLUSTER]
    pairs = [(standard_q, s) for s in samples]
    scores = model.predict(pairs, show_progress_bar=False).astype(float)

    low_pairs = [
        {'sample': s, 'score': round(float(sc), 4)}
        for s, sc in zip(samples, scores) if sc < LOW_PAIR_THRESHOLD
    ]
    return {
        'evaluationScore': round(float(scores.mean()), 4),
        'evaluationMedian': round(float(np.median(scores)), 4),
        'evaluationMin': round(float(scores.min()), 4),
        'evaluationMax': round(float(scores.max()), 4),
        'samplesEvaluated': len(samples),
        'lowScorePairs': low_pairs,
        'needsReview': float(scores.mean()) < REVIEW_THRESHOLD,
    }


# 4. 산출물 저장
def save(results: list[dict]) -> None:
    payload = {'qualityScores': results}
    OUTPUT_JSON.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8'
    )
    print(f'산출물: {OUTPUT_JSON.name} ({OUTPUT_JSON.stat().st_size:,} bytes, {len(results)}건)')


# 5. 메인
def main() -> None:
    candidates, samples_by_cid = load_inputs()
    faq_clusters = [c for c in candidates if c.get('tag') == 'FAQ']
    print(f'평가 대상: FAQ 클러스터 {len(faq_clusters)}개 (전체 {len(candidates)}개 중)\n')

    model = load_model()
    print(f'device: {DEVICE}\n')

    results = []
    for i, c in enumerate(faq_clusters, 1):
        cid = c['clusterLabel']
        std_q = c.get('standardQuestion', '').strip()
        samples = samples_by_cid.get(cid, [])
        if not std_q or not samples:
            print(f'  [{i:>2}/{len(faq_clusters)}] C{cid:>2}  skip (no std_q or samples)')
            continue

        t0 = time.time()
        metrics = evaluate_cluster(model, std_q, samples)
        elapsed = time.time() - t0

        record = {
            'clusterLabel': cid,
            'clusterSize': c['clusterSize'],
            'standardQuestion': std_q,
            **metrics,
        }
        results.append(record)
        flag = '⚠️' if metrics['needsReview'] else '  '
        print(f'  [{i:>2}/{len(faq_clusters)}] C{cid:>2} {flag} '
              f'mean={metrics["evaluationScore"]:.3f} '
              f'(min={metrics["evaluationMin"]:.3f} max={metrics["evaluationMax"]:.3f})  '
              f'n={metrics["samplesEvaluated"]:>2}  ({elapsed:.1f}s)')

    print()
    n_review = sum(1 for r in results if r['needsReview'])
    means = [r['evaluationScore'] for r in results]
    print(f'점수 통계:')
    print(f'  전체 mean 평균   : {np.mean(means):.3f}')
    print(f'  전체 mean median : {np.median(means):.3f}')
    print(f'  needsReview      : {n_review}/{len(results)}건')

    save(results)


if __name__ == '__main__':
    main()
