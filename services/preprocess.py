"""또타24 미답변 로그 전처리 — 텍스트 정제 단일 책임.

기능:
  1. 언어 감지 + 외국어→한국어 번역 (옵션)
  2. PII 마스킹 (옵션)
  3. 열차번호 보호 토큰화 (마스킹 옵션에 포함)
  4. 이중 정규화 (클러스터링용 강함 / 생성용 오타 보존)

응답 라우팅·strategy 분류는 이 모듈 책임이 아님 — 클러스터링 + LLM이 담당.
라벨(`질문 유형`)은 평가(5단계) 단계에서 ground truth로 사용.
"""
from __future__ import annotations

import json
import os
import re
import unicodedata
from pathlib import Path

import pandas as pd
from deep_translator import GoogleTranslator
from deep_translator.exceptions import TranslationNotFound, NotValidPayload


def _nfd(p: Path) -> Path:
    """프로젝트 폴더명이 NFD(macOS-style)로 저장된 경우를 위한 정규화."""
    return Path(unicodedata.normalize('NFD', str(p)))


# 0. 경로/컬럼 설정
DATA_DIR = _nfd(Path(__file__).resolve().parent.parent / 'dataset')
INPUT_CSV = DATA_DIR / 'tota24_unanswered_2026Q1.csv'
OUTPUT_CSV = DATA_DIR / 'preprocessed_full.csv'

COL_ID = '번호'
COL_QUESTION = '질문'
COL_VALID = '질의 유효/무효(유효:1, 무효:0)'
COL_TYPE = '질문 유형'
COL_KEYWORDS = '질문 핵심 키워드'
COL_ANSWER_DIR = '답변 생성 방향 검토'
COL_PROB = '답변확률'
COL_DATE = '날짜'


def _env_bool(name: str, default: bool) -> bool:
    """
    환경변수 기반 boolean 옵션 파서.

    pipeline_runner.py에서 다음 환경변수를 설정하면 preprocess가 이를 반영한다.
    - TTOBAGI_IS_MASKING_ENABLED
    - TTOBAGI_IS_TRANSLATION_ENABLED
    """

    raw = os.getenv(name)

    if raw is None:
        return default

    return raw.strip().lower() in {'1', 'true', 'yes', 'y', 'on'}


# 1. 입력 정규화: int/float/NaN/공백 섞인 값을 깔끔한 str로
def to_text(q) -> str:
    if pd.isna(q):
        return ''
    if isinstance(q, (int, float)):
        return str(int(q)) if float(q).is_integer() else str(q)
    return str(q).strip()


# 2. 언어 감지 (한·러·일·중·영 순)
def detect_lang(text: str) -> str:
    if not text:
        return 'empty'
    if re.search(r'[가-힣]', text):
        return 'ko'
    if re.search(r'[а-яА-Я]', text):
        return 'ru'
    if re.search(r'[ぁ-んァ-ン]', text):
        return 'ja'
    if re.search(r'[一-龯]', text):
        return 'zh'
    if re.search(r'[a-zA-Z]', text):
        return 'en'
    return 'unknown'


_translate_cache: dict[str, str | None] = {}


def translate_to_ko(text: str, src_lang: str) -> str | None:
    """한국어 등록 원칙: 외국어로 감지되면 한국어로 번역."""
    if src_lang in ('ko', 'empty', 'unknown') or not text.strip():
        return None

    if text in _translate_cache:
        return _translate_cache[text]

    try:
        ko = GoogleTranslator(source='auto', target='ko').translate(text)
        if not ko or ko.strip() == text.strip():
            ko = None
    except (TranslationNotFound, NotValidPayload):
        ko = None
    except Exception:
        ko = None

    _translate_cache[text] = ko
    return ko


# 3. PII 마스킹
RE_MOBILE = re.compile(r'(?<!\d)01[016789][-.\s]?\d{3,4}[-.\s]?\d{4}(?!\d)')
RE_TEL = re.compile(r'(?<!\d)0\d{1,2}[-.\s]?\d{3,4}[-.\s]?\d{4}(?!\d)')
RE_EMAIL = re.compile(r'[\w.+-]+@[\w-]+\.[\w.-]+')
RE_RRN = re.compile(r'(?<!\d)\d{6}-?[1-4]\d{6}(?!\d)')
RE_CARD = re.compile(r'(?<!\d)\d{4}[-\s]?\d{4}[-\s]?\d{4}[-\s]?\d{4}(?!\d)')
RE_URL = re.compile(r'https?://\S+|www\.\S+')


def mask_pii(text: str) -> tuple[str, dict]:
    ent: dict[str, list[str]] = {}

    if m := RE_MOBILE.findall(text):
        ent['mobile'] = m
    text = RE_MOBILE.sub('[TEL]', text)

    if m := RE_TEL.findall(text):
        ent['tel'] = m
    text = RE_TEL.sub('[TEL]', text)

    if m := RE_EMAIL.findall(text):
        ent['email'] = m
    text = RE_EMAIL.sub('[EMAIL]', text)

    text = RE_RRN.sub('[RRN]', text)
    text = RE_CARD.sub('[CARD]', text)
    text = RE_URL.sub('[URL]', text)

    return text, ent


# 4. 열차번호 토큰화 (4자리=서울메트로, 6자리=코레일, 5/7+=폐기)
RE_NUM = re.compile(r'(?<!\d)\d+(?!\d)')


def tag_numbers(text: str) -> tuple[str, dict]:
    ent: dict[str, list[str]] = {}

    def repl(m: re.Match) -> str:
        n = m.group()
        length = len(n)

        if length == 4:
            ent.setdefault('metro_train', []).append(n)
            return '[METRO_TRAIN]'

        if length == 6:
            ent.setdefault('korail_train', []).append(n)
            return '[KORAIL_TRAIN]'

        if length == 5 or length >= 7:
            ent.setdefault('discarded_num', []).append(n)
            return ' '

        return n

    return RE_NUM.sub(repl, text), ent


# 5. 이중 정규화
RE_REPEAT_KOR = re.compile(r'([ㅋㅎㅠㅜㅡ])\1{2,}')
RE_REPEAT_CHAR = re.compile(r'(.)\1{3,}')
RE_EMOJI = re.compile(r'[\U0001F300-\U0001F9FF\U0001FA00-\U0001FAFF☀-➿]+')
RE_HANGUL_JAMO = re.compile(r'[ᄀ-ᇿ㄰-㆏]')
RE_WS = re.compile(r'\s+')


def normalize_for_clustering(text: str) -> str:
    """클러스터링용: 자모/이모지/반복 제거. 의미 단위 유지."""
    text = RE_EMOJI.sub(' ', text)
    text = RE_REPEAT_KOR.sub(r'\1\1', text)
    text = RE_REPEAT_CHAR.sub(r'\1\1', text)
    text = RE_HANGUL_JAMO.sub(' ', text)
    return RE_WS.sub(' ', text).strip()


def normalize_for_generation(text: str) -> str:
    """생성용: 오타 보존 (유사어 생성용)."""
    text = RE_EMOJI.sub(' ', text)
    text = RE_REPEAT_CHAR.sub(r'\1\1', text)
    return RE_WS.sub(' ', text).strip()


# 6. 입력 검증
def validate_input_columns(df: pd.DataFrame) -> None:
    """
    기존 파이프라인이 요구하는 입력 컬럼이 존재하는지 확인한다.
    """

    required_columns = [
        COL_ID,
        COL_QUESTION,
        COL_VALID,
        COL_TYPE,
        COL_KEYWORDS,
        COL_ANSWER_DIR,
        COL_PROB,
        COL_DATE,
    ]

    missing_columns = [
        col for col in required_columns
        if col not in df.columns
    ]

    if missing_columns:
        raise ValueError(
            '입력 파일에 필수 컬럼이 없습니다: '
            + ', '.join(missing_columns)
        )


# 7. 파이프라인
def load() -> pd.DataFrame:
    df = pd.read_csv(INPUT_CSV, encoding='utf-8-sig')
    df.columns = [c.replace('\n', '') for c in df.columns]
    validate_input_columns(df)
    return df


def run(
    df: pd.DataFrame,
    is_masking_enabled: bool = True,
    is_translation_enabled: bool = False,
) -> pd.DataFrame:
    """
    원문 → 선택적 번역 → 선택적 PII/열차번호 마스킹 → 이중 정규화.
    """

    rows = []

    for _, row in df.iterrows():
        raw = to_text(row[COL_QUESTION])
        lang = detect_lang(raw)

        if is_translation_enabled:
            translated = translate_to_ko(raw, lang)
        else:
            translated = None

        working = translated if translated else raw

        entities = {}

        if is_masking_enabled:
            masked, pii_ent = mask_pii(working)
            tagged, num_ent = tag_numbers(masked)
            entities.update(pii_ent)
            entities.update(num_ent)
        else:
            tagged = working

        if translated:
            entities['translated_from'] = lang

        clu = normalize_for_clustering(tagged)
        gen = normalize_for_generation(tagged)

        rows.append({
            'logId': row[COL_ID],
            'date': row[COL_DATE],
            'originalText': raw,
            'lang': lang,
            'normalizedForClustering': clu,
            'normalizedForGeneration': gen,
            'extractedEntities': json.dumps(entities, ensure_ascii=False) if entities else '',
            'labelValid': row.get(COL_VALID),
            'labelType': row.get(COL_TYPE),
            'labelKeywords': row.get(COL_KEYWORDS),
            'labelAnswerDirection': row.get(COL_ANSWER_DIR),
            'labelMatchProbability': row.get(COL_PROB),
        })

    return pd.DataFrame(rows)


def save(out: pd.DataFrame) -> None:
    out.to_csv(OUTPUT_CSV, index=False, encoding='utf-8-sig')
    print(f'  {OUTPUT_CSV.stat().st_size:>9,} bytes  {len(out):>5} rows  {OUTPUT_CSV.name}')


def report(
    out: pd.DataFrame,
    is_masking_enabled: bool,
    is_translation_enabled: bool,
) -> None:
    print('전처리 옵션:')
    print(f'  isMaskingEnabled     = {is_masking_enabled}')
    print(f'  isTranslationEnabled = {is_translation_enabled}')
    print()

    print('언어 분포:')
    print(out['lang'].value_counts().to_string())
    print()

    translated = out['extractedEntities'].astype(str).str.contains('translated_from').sum()
    print(f'번역된 행: {translated}건')

    print(
        f'PII 토큰 [TEL] 포함: '
        f'{out["normalizedForClustering"].str.contains("[TEL]", regex=False, na=False).sum()}건'
    )
    print(
        f'열차번호 [METRO_TRAIN]: '
        f'{out["normalizedForClustering"].str.contains("[METRO_TRAIN]", regex=False, na=False).sum()}건'
    )
    print(
        f'열차번호 [KORAIL_TRAIN]: '
        f'{out["normalizedForClustering"].str.contains("[KORAIL_TRAIN]", regex=False, na=False).sum()}건'
    )


def main(
    is_masking_enabled: bool | None = None,
    is_translation_enabled: bool | None = None,
) -> None:
    """
    전처리 실행 진입점.

    직접 실행 시:
      python -m services.preprocess

    /pipeline에서 실행 시:
      pipeline_runner.py가 환경변수로 옵션을 설정한 뒤 run_full_pipeline()을 호출한다.
    """

    if is_masking_enabled is None:
        is_masking_enabled = _env_bool(
            'TTOBAGI_IS_MASKING_ENABLED',
            True,
        )

    if is_translation_enabled is None:
        is_translation_enabled = _env_bool(
            'TTOBAGI_IS_TRANSLATION_ENABLED',
            False,
        )

    df = load()
    print(f'입력: {INPUT_CSV.name} ({len(df)}행)\n')

    out = run(
        df,
        is_masking_enabled=is_masking_enabled,
        is_translation_enabled=is_translation_enabled,
    )

    print('산출물:')
    save(out)
    print()

    report(
        out,
        is_masking_enabled=is_masking_enabled,
        is_translation_enabled=is_translation_enabled,
    )


if __name__ == '__main__':
    main()