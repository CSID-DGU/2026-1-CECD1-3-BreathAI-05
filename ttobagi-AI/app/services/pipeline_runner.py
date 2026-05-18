import json
import os
import shutil
import sys
import traceback
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional

import pandas as pd

from app.services.bronze_ingestor import ingest_unanswered_learning
from app.services.callback import send_callback, send_status_callback


# 현재 파일 위치:
# ttobagi-AI/app/services/pipeline_runner.py
#
# 프로젝트 루트:
# 2026-1-CECD1-4-BreathAI-15/
PROJECT_ROOT = Path(__file__).resolve().parents[3]

# 기존 분석 파이프라인이 위치한 루트 services 디렉터리를 import 경로에 추가
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


DATASET_DIR = PROJECT_ROOT / "dataset"

# 기존 루트 services/run_pipeline.py가 읽는 입력 파일명
PIPELINE_INPUT_FILE = DATASET_DIR / "tota24_unanswered_2026Q1.csv"

# 기존 루트 services/build_backend_payload.py가 생성하는 결과 파일
BACKEND_PAYLOAD_FILE = DATASET_DIR / "backend_payload.json"


def run_pipeline_job(
    analysis_id: int,
    upload_id: str,
    file_path: str,
    original_file_name: str,
    is_masking_enabled: bool,
    is_translation_enabled: bool,
    period_start_date: str,
    period_end_date: str,
) -> None:
    """
    FastAPI BackgroundTasks에서 실행되는 전체 AI 파이프라인 작업 함수.

    callback 정책:
    - PREPROCESSING: 상태만 callback
    - ANALYZING: 상태만 callback
    - COMPLETED: 최종 분석 payload 포함 callback
    - FAILED: 에러 정보 포함 callback
    """

    print("[PIPELINE] 백그라운드 전체 파이프라인 작업 시작", flush=True)
    print(f"[PIPELINE] analysis_id={analysis_id}", flush=True)
    print(f"[PIPELINE] upload_id={upload_id}", flush=True)
    print(f"[PIPELINE] original_file_name={original_file_name}", flush=True)
    print(f"[PIPELINE] file_path={file_path}", flush=True)
    print(f"[PIPELINE] is_masking_enabled={is_masking_enabled}", flush=True)
    print(f"[PIPELINE] is_translation_enabled={is_translation_enabled}", flush=True)
    print(f"[PIPELINE] period_start_date={period_start_date}", flush=True)
    print(f"[PIPELINE] period_end_date={period_end_date}", flush=True)

    ingest_batch_id: Optional[str] = None
    bronze_row_count: Optional[int] = None

    try:
        print("[PIPELINE] PREPROCESSING callback 전송", flush=True)
        send_status_callback(
            analysis_id=analysis_id,
            upload_id=upload_id,
            file_name=original_file_name,
            status="PREPROCESSING",
            message="AI 서버에서 업로드 파일 전처리를 시작했습니다.",
            extra={
                "periodStartDate": period_start_date,
                "periodEndDate": period_end_date,
                "options": {
                    "isMaskingEnabled": is_masking_enabled,
                    "isTranslationEnabled": is_translation_enabled,
                },
            },
        )

        print("[PIPELINE] Bronze DB 적재 시작", flush=True)
        ingest_result = ingest_unanswered_learning(
            file_path=file_path,
            original_file_name=original_file_name,
        )
        ingest_batch_id = ingest_result.ingest_batch_id
        bronze_row_count = ingest_result.row_count

        print(
            "[PIPELINE] Bronze DB 적재 완료 "
            f"ingest_batch_id={ingest_batch_id}, "
            f"row_count={bronze_row_count}",
            flush=True,
        )

        print("[PIPELINE] 입력 파일 준비 시작", flush=True)
        _prepare_input_file(file_path)
        print("[PIPELINE] 입력 파일 준비 완료", flush=True)

        print("[PIPELINE] 전처리 옵션 환경변수 설정", flush=True)
        _set_preprocess_options(
            is_masking_enabled=is_masking_enabled,
            is_translation_enabled=is_translation_enabled,
        )

        print("[PIPELINE] ANALYZING callback 전송", flush=True)
        send_status_callback(
            analysis_id=analysis_id,
            upload_id=upload_id,
            file_name=original_file_name,
            status="ANALYZING",
            message="AI 서버에서 분석 파이프라인을 실행 중입니다.",
            extra={
                "ingestBatchId": ingest_batch_id,
                "bronzeRowCount": bronze_row_count,
                "periodStartDate": period_start_date,
                "periodEndDate": period_end_date,
                "options": {
                    "isMaskingEnabled": is_masking_enabled,
                    "isTranslationEnabled": is_translation_enabled,
                },
            },
        )

        print("[PIPELINE] 기존 services 파이프라인 실행 시작", flush=True)
        _run_existing_pipeline()
        print("[PIPELINE] 기존 services 파이프라인 실행 완료", flush=True)

        print("[PIPELINE] backend_payload.json 로드 시작", flush=True)
        payload = _load_backend_payload()
        print("[PIPELINE] backend_payload.json 로드 완료", flush=True)

        payload = _patch_payload(
            payload=payload,
            analysis_id=analysis_id,
            upload_id=upload_id,
            file_name=original_file_name,
            status="COMPLETED",
            is_masking_enabled=is_masking_enabled,
            is_translation_enabled=is_translation_enabled,
            period_start_date=period_start_date,
            period_end_date=period_end_date,
            ingest_batch_id=ingest_batch_id,
            bronze_row_count=bronze_row_count,
        )

        print("[PIPELINE] COMPLETED callback 전송 시작", flush=True)
        send_callback(analysis_id=analysis_id, payload=payload)
        print("[PIPELINE] 전체 파이프라인 작업 완료", flush=True)

    except Exception as exc:
        print("[PIPELINE] 전체 파이프라인 작업 실패", flush=True)
        print(str(exc), flush=True)
        print(traceback.format_exc(), flush=True)

        fail_payload = _build_fail_payload(
            analysis_id=analysis_id,
            upload_id=upload_id,
            file_name=original_file_name,
            is_masking_enabled=is_masking_enabled,
            is_translation_enabled=is_translation_enabled,
            period_start_date=period_start_date,
            period_end_date=period_end_date,
            exc=exc,
            ingest_batch_id=ingest_batch_id,
            bronze_row_count=bronze_row_count,
        )

        try:
            send_callback(analysis_id=analysis_id, payload=fail_payload)
        except Exception as callback_exc:
            print("[PIPELINE] 실패 callback 전송 중 추가 오류 발생", flush=True)
            print(str(callback_exc), flush=True)
            print(traceback.format_exc(), flush=True)


def run_analysis_job(
    analysis_id: int,
    upload_id: str,
    file_name: str,
    file_path: Optional[str] = None,
    callback_url: Optional[str] = None,
) -> None:
    """
    기존 /analyze endpoint 호환용 함수.

    새 연동 계약에서는 Spring Boot가 /analyze를 호출하지 않고 /pipeline을 호출한다.
    따라서 이 함수는 내부 테스트 또는 과거 코드 호환용으로만 유지한다.
    """

    if file_path is None:
        raise ValueError("file_path가 필요합니다.")

    print(
        "[PIPELINE] run_analysis_job은 legacy 함수입니다. run_pipeline_job으로 위임합니다.",
        flush=True,
    )

    run_pipeline_job(
        analysis_id=analysis_id,
        upload_id=upload_id,
        file_path=file_path,
        original_file_name=file_name,
        is_masking_enabled=True,
        is_translation_enabled=False,
        period_start_date="",
        period_end_date="",
    )


def _prepare_input_file(file_path: str) -> None:
    """
    /pipeline에서 저장한 업로드 파일을 기존 루트 services 파이프라인이 읽을 수 있는
    dataset/tota24_unanswered_2026Q1.csv 형태로 준비한다.

    기존 파이프라인이 CSV 입력을 기준으로 작성되어 있으므로,
    업로드 파일이 xlsx/xls이면 CSV로 변환하고,
    csv이면 그대로 복사한다.
    """

    source_path = Path(file_path)

    if not source_path.exists():
        raise FileNotFoundError(f"입력 파일을 찾을 수 없습니다: {source_path}")

    DATASET_DIR.mkdir(parents=True, exist_ok=True)

    file_ext = source_path.suffix.lower()

    if file_ext == ".csv":
        shutil.copyfile(source_path, PIPELINE_INPUT_FILE)
        return

    if file_ext in {".xlsx", ".xls"}:
        df = pd.read_excel(source_path)
        df.to_csv(PIPELINE_INPUT_FILE, index=False, encoding="utf-8-sig")
        return

    raise ValueError(f"지원하지 않는 입력 파일 형식입니다: {file_ext}")


def _set_preprocess_options(
    is_masking_enabled: bool,
    is_translation_enabled: bool,
) -> None:
    """
    services/preprocess.py가 읽을 전처리 옵션을 환경변수로 설정한다.
    """

    os.environ["TTOBAGI_IS_MASKING_ENABLED"] = str(is_masking_enabled).lower()
    os.environ["TTOBAGI_IS_TRANSLATION_ENABLED"] = str(is_translation_enabled).lower()

    print(
        "[PIPELINE] TTOBAGI_IS_MASKING_ENABLED="
        f"{os.environ['TTOBAGI_IS_MASKING_ENABLED']}",
        flush=True,
    )
    print(
        "[PIPELINE] TTOBAGI_IS_TRANSLATION_ENABLED="
        f"{os.environ['TTOBAGI_IS_TRANSLATION_ENABLED']}",
        flush=True,
    )


def _run_existing_pipeline() -> None:
    """
    기존 루트 services 파이프라인을 실행한다.
    """

    from services.run_pipeline import run_full_pipeline
    from services.build_backend_payload import main as build_backend_payload_main

    run_full_pipeline()
    build_backend_payload_main()


def _load_backend_payload() -> Dict[str, Any]:
    """
    기존 build_backend_payload.py가 생성한 backend_payload.json을 로드한다.
    """

    if not BACKEND_PAYLOAD_FILE.exists():
        raise FileNotFoundError(
            f"backend_payload.json 파일을 찾을 수 없습니다: {BACKEND_PAYLOAD_FILE}"
        )

    with BACKEND_PAYLOAD_FILE.open("r", encoding="utf-8") as f:
        return json.load(f)


def _patch_payload(
    payload: Dict[str, Any],
    analysis_id: int,
    upload_id: str,
    file_name: str,
    status: str,
    is_masking_enabled: bool,
    is_translation_enabled: bool,
    period_start_date: str,
    period_end_date: str,
    ingest_batch_id: Optional[str],
    bronze_row_count: Optional[int],
) -> Dict[str, Any]:
    """
    기존 services/build_backend_payload.py가 만든 payload에
    Spring Boot 연동에 필요한 메타데이터를 보정한다.

    callback payload는 AI_to_backend.md 기준에 맞춰 camelCase를 유지한다.
    """

    now = datetime.now().isoformat(timespec="seconds")

    payload["analysisId"] = analysis_id
    payload["uploadId"] = upload_id
    payload["fileName"] = file_name
    payload["status"] = status

    payload["periodStartDate"] = period_start_date
    payload["periodEndDate"] = period_end_date

    payload["finishedAt"] = payload.get("finishedAt") or now
    payload["submittedAt"] = payload.get("submittedAt") or now
    payload["startedAt"] = payload.get("startedAt") or now

    if ingest_batch_id is not None:
        payload["ingestBatchId"] = ingest_batch_id

    if bronze_row_count is not None:
        payload["bronzeRowCount"] = bronze_row_count

    existing_options = payload.get("options") or {}
    existing_options["isMaskingEnabled"] = is_masking_enabled
    existing_options["isTranslationEnabled"] = is_translation_enabled
    payload["options"] = existing_options

    return payload


def _build_fail_payload(
    analysis_id: int,
    upload_id: str,
    file_name: str,
    is_masking_enabled: bool,
    is_translation_enabled: bool,
    period_start_date: str,
    period_end_date: str,
    exc: Exception,
    ingest_batch_id: Optional[str],
    bronze_row_count: Optional[int],
) -> Dict[str, Any]:
    """
    파이프라인 실행 실패 시 Spring Boot로 보낼 실패 callback payload를 생성한다.
    """

    now = datetime.now().isoformat(timespec="seconds")

    payload = {
        "analysisId": analysis_id,
        "uploadId": upload_id,
        "fileName": file_name,
        "status": "FAILED",
        "message": "AI 서버 파이프라인 실행 중 오류가 발생했습니다.",
        "submittedAt": now,
        "startedAt": now,
        "finishedAt": now,
        "periodStartDate": period_start_date,
        "periodEndDate": period_end_date,
        "options": {
            "isMaskingEnabled": is_masking_enabled,
            "isTranslationEnabled": is_translation_enabled,
        },
        "error": {
            "message": str(exc),
            "type": exc.__class__.__name__,
        },
    }

    if ingest_batch_id is not None:
        payload["ingestBatchId"] = ingest_batch_id

    if bronze_row_count is not None:
        payload["bronzeRowCount"] = bronze_row_count

    return payload