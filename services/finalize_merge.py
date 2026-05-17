"""v3 base + v5 corporate_info OOS 부분 머지 → faq_candidates_v6.json (최종본).

입력:
  dataset/faq_candidates_v3.json   (refine v0.1 결과)
  dataset/faq_candidates_v5.json   (oos_refresher v0.2 결과 — case별 단일 가이드 적용)

출력:
  dataset/faq_candidates_v6.json   (81건 최종 — corporate 5건만 v5에서, 나머지 v3)
"""
from __future__ import annotations

import json
import unicodedata
from pathlib import Path


def _nfd(p):
    return Path(unicodedata.normalize('NFD', str(p)))


DATA_DIR = _nfd(Path(__file__).resolve().parent.parent / 'dataset')
V3_JSON = DATA_DIR / 'faq_candidates_v3.json'
V5_JSON = DATA_DIR / 'faq_candidates_v5.json'
V6_JSON = DATA_DIR / 'faq_candidates_v6.json'


def main() -> None:
    v3 = json.loads(V3_JSON.read_text(encoding='utf-8'))['faqCandidates']
    v5_map = {c['clusterLabel']: c for c in
              json.loads(V5_JSON.read_text(encoding='utf-8'))['faqCandidates']}

    corp_cids = {cid for cid, c in v5_map.items()
                 if c.get('oos_case') == 'corporate_info'}
    print(f'corporate_info 머지 대상: {len(corp_cids)}건 — {sorted(corp_cids)}')

    v6 = [v5_map[c['clusterLabel']] if c['clusterLabel'] in corp_cids else c
          for c in v3]
    V6_JSON.write_text(json.dumps({'faqCandidates': v6}, ensure_ascii=False,
                                  indent=2), encoding='utf-8')
    nr = sum(1 for c in v6 if c['evaluation'].get('needs_review_v2'))
    print(f'저장: {V6_JSON.name} ({V6_JSON.stat().st_size:,}B, {len(v6)}건)')
    print(f'전체 needsReview: {nr}/{len(v6)}건')


if __name__ == '__main__':
    main()
