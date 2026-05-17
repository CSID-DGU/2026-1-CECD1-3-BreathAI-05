"""또타24 KB 검색기 — BGE-M3 dense embedding + BGE-reranker 2-stage.

입력:  dataset/seoulmetro_kb_clean.jsonl (KB)
       dataset/faq_candidates.json       (질의 = standardQuestion)
출력:  dataset/kb_embeddings.npy         (KB 임베딩 캐시)
       dataset/faq_retrieval.json        (FAQ별 top-k 검색 결과)

설계:
  - 1차: BGE-M3 dense embedding cosine sim (top_k1=20)
  - 2차: BGE-reranker로 rerank (top_k2=5)
  - 표준질문이 비어있거나 FAQ 태그 아닌 클러스터는 스킵
"""
from __future__ import annotations

import json
import time
import unicodedata
from pathlib import Path

import numpy as np
import torch
from sentence_transformers import SentenceTransformer, CrossEncoder


def _nfd(p: Path) -> Path:
    return Path(unicodedata.normalize('NFD', str(p)))


# 경로
DATA_DIR = _nfd(Path(__file__).resolve().parent.parent / 'dataset')
KB_JSONL = DATA_DIR / 'seoulmetro_kb_clean.jsonl'
KB_EMB = DATA_DIR / 'kb_embeddings.npy'
FAQ_JSON = DATA_DIR / 'faq_candidates.json'
OUTPUT_JSON = DATA_DIR / 'faq_retrieval.json'

# 모델
DENSE_MODEL = 'BAAI/bge-m3'
RERANK_MODEL = 'BAAI/bge-reranker-v2-m3'
DEVICE = 'cuda' if torch.cuda.is_available() else 'cpu'

TOP_K1 = 20    # dense retrieval
TOP_K2 = 5     # after rerank
BATCH_SIZE = 32


def load_kb() -> list[dict]:
    return [json.loads(l) for l in KB_JSONL.open(encoding='utf-8')]


def build_or_load_index(kb: list[dict], force_rebuild: bool = False) -> np.ndarray:
    """KB 임베딩을 .npy로 캐시. force_rebuild=True면 재계산."""
    if KB_EMB.exists() and not force_rebuild:
        emb = np.load(KB_EMB)
        if emb.shape[0] == len(kb):
            print(f'KB 임베딩 캐시 hit: {KB_EMB.name} ({emb.shape})')
            return emb
        print(f'KB 변경 감지, 임베딩 재계산')

    print(f'KB 임베딩 계산 중 (n={len(kb)}, model={DENSE_MODEL})')
    enc = SentenceTransformer(DENSE_MODEL, device=DEVICE)
    texts = [c['text'] for c in kb]
    emb = enc.encode(
        texts, batch_size=BATCH_SIZE, normalize_embeddings=True,
        show_progress_bar=True, convert_to_numpy=True,
    )
    np.save(KB_EMB, emb)
    print(f'저장: {KB_EMB.name} ({emb.shape}, {KB_EMB.stat().st_size:,} bytes)')
    return emb


def search(query: str, enc: SentenceTransformer, kb: list[dict],
           kb_emb: np.ndarray, top_k: int = TOP_K1) -> list[dict]:
    """dense cosine top-k."""
    q_emb = enc.encode([query], normalize_embeddings=True, convert_to_numpy=True)[0]
    sims = kb_emb @ q_emb
    idx = np.argsort(-sims)[:top_k]
    return [{**kb[i], 'dense_score': float(sims[i])} for i in idx]


def rerank(query: str, candidates: list[dict], reranker: CrossEncoder,
           top_k: int = TOP_K2) -> list[dict]:
    """Cross-encoder rerank."""
    pairs = [(query, c['text']) for c in candidates]
    scores = reranker.predict(pairs, show_progress_bar=False)
    for c, s in zip(candidates, scores):
        c['rerank_score'] = float(s)
    candidates.sort(key=lambda c: -c['rerank_score'])
    return candidates[:top_k]


def run_for_faqs() -> None:
    kb = load_kb()
    print(f'KB chunks: {len(kb)}')

    kb_emb = build_or_load_index(kb)
    enc = SentenceTransformer(DENSE_MODEL, device=DEVICE)
    reranker = CrossEncoder(
        RERANK_MODEL, max_length=512, device=DEVICE,
        default_activation_function=torch.nn.Sigmoid(),
    )
    print(f'reranker loaded: {RERANK_MODEL}')

    candidates = json.loads(FAQ_JSON.read_text(encoding='utf-8'))['faqCandidates']
    faqs = [c for c in candidates if c.get('tag') == 'FAQ' and c.get('standardQuestion')]
    print(f'\n질의 (FAQ standardQuestion): {len(faqs)}개\n')

    results = []
    for i, c in enumerate(faqs, 1):
        q = c['standardQuestion']
        t0 = time.time()
        cands = search(q, enc, kb, kb_emb, top_k=TOP_K1)
        top = rerank(q, cands, reranker, top_k=TOP_K2)
        elapsed = time.time() - t0
        record = {
            'clusterLabel': c['clusterLabel'],
            'clusterSize': c['clusterSize'],
            'query': q,
            'retrieved': [
                {
                    'source_url': t['source_url'],
                    'menuIdx': t['menuIdx'],
                    'page_label': t['page_label'],
                    'chunk_id': t['chunk_id'],
                    'text': t['text'],
                    'dense_score': round(t['dense_score'], 4),
                    'rerank_score': round(t['rerank_score'], 4),
                }
                for t in top
            ],
        }
        results.append(record)
        best = top[0] if top else None
        if best:
            print(f'  [{i:>2}/{len(faqs)}] C{c["clusterLabel"]:>2}  '
                  f'top={best["rerank_score"]:.3f}  '
                  f'q="{q[:36]}" → "{best["page_label"][:20]}"  ({elapsed:.2f}s)')
        else:
            print(f'  [{i:>2}/{len(faqs)}] C{c["clusterLabel"]:>2}  no result')

    OUTPUT_JSON.write_text(
        json.dumps({'faqRetrieval': results}, ensure_ascii=False, indent=2),
        encoding='utf-8',
    )
    print(f'\n산출물: {OUTPUT_JSON.name} ({OUTPUT_JSON.stat().st_size:,} bytes, {len(results)}건)')


if __name__ == '__main__':
    run_for_faqs()
