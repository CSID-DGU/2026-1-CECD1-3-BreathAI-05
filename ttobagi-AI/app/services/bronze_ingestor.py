import hashlib
import json
import math
import uuid
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional

import pandas as pd

from app.config.database import get_db_connection


@dataclass
class BronzeIngestResult:
    ingest_batch_id: str
    row_count: int


REQUIRED_COLUMNS = [
    "번호",
    "도메인",
    "질문",
    "질의 유효/무효(유효:1, 무효:0)",
    "질문 유형",
    "답변확률",
    "상태",
    "날짜",
]


def ingest_unanswered_learning(
    file_path: str,
    original_file_name: str,
) -> BronzeIngestResult:
    """
    업로드된 또타24 미답변 분석 엑셀/CSV 파일을 Bronze DB에 적재한다.

    적재 대상:
    - bronze_ingest_batch
    - bronze_unanswered_learning
    """

    source_path = Path(file_path)

    if not source_path.exists():
        raise FileNotFoundError(f"Bronze 적재 대상 파일을 찾을 수 없습니다: {source_path}")

    ingest_batch_id = str(uuid.uuid4())
    df = _load_source_file(source_path)
    df = _normalize_columns(df)
    _validate_required_columns(df)

    row_count = len(df)

    conn = get_db_connection()

    try:
        with conn.cursor() as cursor:
            _insert_ingest_batch(
                cursor=cursor,
                ingest_batch_id=ingest_batch_id,
                original_file_name=original_file_name,
                row_count=row_count,
            )

            for _, row in df.iterrows():
                record = _build_unanswered_learning_record(
                    row=row,
                    ingest_batch_id=ingest_batch_id,
                    source_file=original_file_name,
                )
                _insert_unanswered_learning(cursor=cursor, record=record)

            _update_ingest_batch_status(
                cursor=cursor,
                ingest_batch_id=ingest_batch_id,
                batch_status="SUCCESS",
                row_count=row_count,
            )

        conn.commit()

    except Exception:
        conn.rollback()

        try:
            with conn.cursor() as cursor:
                _update_ingest_batch_status(
                    cursor=cursor,
                    ingest_batch_id=ingest_batch_id,
                    batch_status="FAIL",
                    row_count=row_count,
                )
            conn.commit()
        except Exception:
            conn.rollback()

        raise

    finally:
        conn.close()

    return BronzeIngestResult(
        ingest_batch_id=ingest_batch_id,
        row_count=row_count,
    )


def _load_source_file(source_path: Path) -> pd.DataFrame:
    """
    xlsx/xls/csv 파일을 pandas DataFrame으로 로드한다.
    """

    file_ext = source_path.suffix.lower()

    if file_ext in {".xlsx", ".xls"}:
        return pd.read_excel(source_path)

    if file_ext == ".csv":
        return pd.read_csv(source_path)

    raise ValueError(f"지원하지 않는 Bronze 적재 파일 형식입니다: {file_ext}")


def _normalize_columns(df: pd.DataFrame) -> pd.DataFrame:
    """
    엑셀 컬럼명의 줄바꿈과 앞뒤 공백을 제거한다.

    예:
    '질의 유효/무효\\n(유효:1, 무효:0)'
    → '질의 유효/무효(유효:1, 무효:0)'
    """

    copied = df.copy()
    copied.columns = [
        str(column).replace("\n", "").replace("\r", "").strip()
        for column in copied.columns
    ]
    return copied


def _validate_required_columns(df: pd.DataFrame) -> None:
    """
    Bronze 적재에 필요한 필수 컬럼이 모두 존재하는지 검증한다.
    """

    missing_columns = [column for column in REQUIRED_COLUMNS if column not in df.columns]

    if missing_columns:
        raise ValueError(
            "미응답 엑셀 필수 컬럼이 누락되었습니다: "
            + ", ".join(missing_columns)
        )


def _insert_ingest_batch(
    cursor: Any,
    ingest_batch_id: str,
    original_file_name: str,
    row_count: int,
) -> None:
    """
    bronze_ingest_batch에 이번 적재 배치 이력을 생성한다.
    """

    sql = """
        INSERT INTO bronze_ingest_batch (
            ingest_batch_id,
            source_system,
            source_table,
            source_object,
            extracted_at,
            load_type,
            retention_days,
            row_count,
            batch_status
        )
        VALUES (
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s
        )
    """

    cursor.execute(
        sql,
        (
            ingest_batch_id,
            "seoulmetro",
            "unanswered_learning",
            original_file_name,
            datetime.now(),
            "FULL",
            365,
            row_count,
            "RUNNING",
        ),
    )


def _update_ingest_batch_status(
    cursor: Any,
    ingest_batch_id: str,
    batch_status: str,
    row_count: int,
) -> None:
    """
    bronze_ingest_batch의 적재 상태를 갱신한다.
    """

    sql = """
        UPDATE bronze_ingest_batch
        SET batch_status = %s,
            row_count = %s
        WHERE ingest_batch_id = %s
    """

    cursor.execute(sql, (batch_status, row_count, ingest_batch_id))


def _build_unanswered_learning_record(
    row: pd.Series,
    ingest_batch_id: str,
    source_file: str,
) -> Dict[str, Any]:
    """
    엑셀 1행을 bronze_unanswered_learning insert용 dict로 변환한다.
    """

    seq_num = _to_int(_get_value(row, "번호"))
    domain = _to_str(_get_value(row, "도메인")) or "seoulmetro"
    answer_request = _to_nullable_str(_get_value(row, "답변요청"))
    user_question = _to_str(_get_value(row, "질문"))

    is_valid = _to_int(_get_value(row, "질의 유효/무효(유효:1, 무효:0)"))
    question_type = _to_nullable_str(_get_value(row, "질문 유형"))
    core_keywords = _to_nullable_str(_get_value(row, "질문 핵심 키워드"))
    answer_direction = _to_nullable_str(_get_value(row, "답변 생성 방향 검토"))

    match_probability = _to_float(_get_value(row, "답변확률"), default=0.0)
    learn_state = _to_nullable_str(_get_value(row, "상태"))
    occurred_at = _to_datetime(_get_value(row, "날짜"))

    raw_json = _build_raw_json(row)

    row_hash = _build_row_hash(
        seq_num=seq_num,
        domain=domain,
        user_question=user_question,
        is_valid=is_valid,
        question_type=question_type,
        core_keywords=core_keywords,
        answer_direction=answer_direction,
        match_probability=match_probability,
        learn_state=learn_state,
        occurred_at=occurred_at,
    )

    return {
        "seq_num": seq_num,
        "domain": domain,
        "answer_request": answer_request,
        "user_question": user_question,
        "is_valid": is_valid,
        "question_type": question_type,
        "core_keywords": core_keywords,
        "answer_direction": answer_direction,
        "match_probability": match_probability,
        "learn_state": learn_state,
        "occurred_at": occurred_at,
        "raw_json": raw_json,
        "_ingest_batch_id": ingest_batch_id,
        "_source_file": source_file,
        "_row_hash": row_hash,
    }


def _insert_unanswered_learning(cursor: Any, record: Dict[str, Any]) -> None:
    """
    bronze_unanswered_learning에 미답변 엑셀 row를 insert한다.
    """

    sql = """
        INSERT INTO bronze_unanswered_learning (
            seq_num,
            domain,
            answer_request,
            user_question,
            is_valid,
            question_type,
            core_keywords,
            answer_direction,
            match_probability,
            learn_state,
            occurred_at,
            raw_json,
            _ingest_batch_id,
            _source_table,
            _source_file,
            _row_hash
        )
        VALUES (
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s,
            %s
        )
    """

    cursor.execute(
        sql,
        (
            record["seq_num"],
            record["domain"],
            record["answer_request"],
            record["user_question"],
            record["is_valid"],
            record["question_type"],
            record["core_keywords"],
            record["answer_direction"],
            record["match_probability"],
            record["learn_state"],
            record["occurred_at"],
            record["raw_json"],
            record["_ingest_batch_id"],
            "unanswered_learning",
            record["_source_file"],
            record["_row_hash"],
        ),
    )


def _build_row_hash(
    seq_num: int,
    domain: str,
    user_question: str,
    is_valid: int,
    question_type: Optional[str],
    core_keywords: Optional[str],
    answer_direction: Optional[str],
    match_probability: float,
    learn_state: Optional[str],
    occurred_at: datetime,
) -> str:
    """
    중복 적재 방지용 SHA-256 row hash를 생성한다.
    """

    hash_source = "|".join(
        [
            str(seq_num),
            str(domain),
            str(user_question),
            str(is_valid),
            str(question_type),
            str(core_keywords),
            str(answer_direction),
            f"{match_probability:.6f}",
            str(learn_state),
            occurred_at.isoformat(sep=" ", timespec="seconds"),
        ]
    )

    return hashlib.sha256(hash_source.encode("utf-8")).hexdigest()


def _build_raw_json(row: pd.Series) -> str:
    """
    엑셀 원본 row 전체를 JSON 문자열로 변환한다.
    """

    raw_dict: Dict[str, Any] = {}

    for key, value in row.to_dict().items():
        normalized_key = str(key).replace("\n", "").replace("\r", "").strip()
        raw_dict[normalized_key] = _to_json_safe_value(value)

    return json.dumps(raw_dict, ensure_ascii=False, default=str)


def _get_value(row: pd.Series, column_name: str) -> Any:
    """
    pandas Series에서 값을 안전하게 꺼낸다.
    """

    if column_name not in row:
        return None

    return row[column_name]


def _to_int(value: Any, default: int = 0) -> int:
    """
    값을 int로 변환한다.
    """

    if _is_null(value):
        return default

    try:
        return int(float(value))
    except Exception:
        return default


def _to_float(value: Any, default: float = 0.0) -> float:
    """
    값을 float로 변환한다.
    """

    if _is_null(value):
        return default

    try:
        return float(value)
    except Exception:
        return default


def _to_str(value: Any) -> str:
    """
    값을 문자열로 변환한다.
    """

    if _is_null(value):
        return ""

    return str(value).strip()


def _to_nullable_str(value: Any) -> Optional[str]:
    """
    값을 nullable 문자열로 변환한다.
    """

    if _is_null(value):
        return None

    text = str(value).strip()

    if text == "":
        return None

    return text


def _to_datetime(value: Any) -> datetime:
    """
    값을 datetime으로 변환한다.
    """

    if _is_null(value):
        raise ValueError("날짜 컬럼 값이 비어 있습니다.")

    if isinstance(value, datetime):
        return value

    parsed = pd.to_datetime(value, errors="coerce")

    if pd.isna(parsed):
        raise ValueError(f"날짜 값을 datetime으로 변환할 수 없습니다: {value}")

    return parsed.to_pydatetime()


def _to_json_safe_value(value: Any) -> Any:
    """
    JSON 직렬화 가능한 값으로 변환한다.
    """

    if _is_null(value):
        return None

    if isinstance(value, datetime):
        return value.isoformat(sep=" ", timespec="seconds")

    if hasattr(value, "isoformat"):
        try:
            return value.isoformat()
        except Exception:
            pass

    if isinstance(value, float) and math.isnan(value):
        return None

    return value


def _is_null(value: Any) -> bool:
    """
    pandas/numpy NaN, None 여부를 확인한다.
    """

    if value is None:
        return True

    try:
        return bool(pd.isna(value))
    except Exception:
        return False