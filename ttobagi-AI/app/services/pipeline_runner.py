import json
import shutil
import sys
import traceback
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

from app.services.callback import send_callback


PROJECT_ROOT = Path(__file__).resolve().parents[3]

ROOT_SERVICES_DIR = PROJECT_ROOT / "services"
DATASET_DIR = PROJECT_ROOT / "dataset"


if str(PROJECT_ROOT) not in sys.path:
    sys.path.append(str(PROJECT_ROOT))


def _now_iso() -> str:
    return (
        datetime.now(timezone.utc)
        .replace(microsecond=0)
        .isoformat()
        .replace("+00:00", "Z")
    )


def _prepare_input_file(file_path: Optional[str]) -> None:
    print("[PIPELINE] _prepare_input_file 진입", flush=True)
    print(f"[PIPELINE] PROJECT_ROOT={PROJECT_ROOT}", flush=True)
    print(f"[PIPELINE] DATASET_DIR={DATASET_DIR}", flush=True)

    DATASET_DIR.mkdir(parents=True, exist_ok=True)

    if not file_path:
        print("[PIPELINE] file_path 없음. 기존 dataset 입력 파일을 사용합니다.", flush=True)
        return

    src = Path(file_path).resolve()
    print(f"[PIPELINE] 입력 파일 src={src}", flush=True)

    if not src.exists():
        raise FileNotFoundError(f"분석 대상 파일을 찾을 수 없습니다: {src}")

    dst = (DATASET_DIR / "tota24_unanswered_2026Q1.csv").resolve()
    print(f"[PIPELINE] 표준 입력 파일 dst={dst}", flush=True)

    if src == dst:
        print("[PIPELINE] src와 dst가 동일하여 복사하지 않습니다.", flush=True)
        return

    shutil.copyfile(src, dst)
    print("[PIPELINE] 입력 파일 복사 완료", flush=True)


def _run_existing_pipeline() -> None:
    print("[PIPELINE] 루트 services import 시작", flush=True)

    import services.run_pipeline as run_pipeline
    import services.build_backend_payload as build_backend_payload

    print("[PIPELINE] services.run_pipeline import 완료", flush=True)
    print("[PIPELINE] services.build_backend_payload import 완료", flush=True)

    if hasattr(run_pipeline, "run_full_pipeline"):
        print("[PIPELINE] run_pipeline.run_full_pipeline 실행", flush=True)
        run_pipeline.run_full_pipeline()
    elif hasattr(run_pipeline, "main"):
        print("[PIPELINE] run_pipeline.main 실행", flush=True)
        run_pipeline.main()
    else:
        raise AttributeError(
            "services.run_pipeline에 run_full_pipeline 또는 main 함수가 없습니다."
        )

    if hasattr(build_backend_payload, "main"):
        print("[PIPELINE] build_backend_payload.main 실행", flush=True)
        build_backend_payload.main()

    print("[PIPELINE] 기존 services 파이프라인 실행 완료", flush=True)


def _load_backend_payload() -> dict:
    print("[PIPELINE] backend_payload.json 탐색 시작", flush=True)

    candidates = [
        DATASET_DIR / "backend_payload.json",
        ROOT_SERVICES_DIR / "backend_payload.json",
        PROJECT_ROOT / "backend_payload.json",
    ]

    for path in candidates:
        print(f"[PIPELINE] payload 후보 확인: {path}", flush=True)
        if path.exists():
            print(f"[PIPELINE] payload 발견: {path}", flush=True)
            return json.loads(path.read_text(encoding="utf-8"))

    raise FileNotFoundError(
        "backend_payload.json 파일을 찾을 수 없습니다. "
        "services.build_backend_payload 실행 결과를 확인하세요."
    )


def _patch_payload(
    payload: dict,
    analysis_id: int,
    upload_id: str,
    file_name: str,
    status: str,
) -> dict:
    now = _now_iso()

    payload["analysisId"] = analysis_id
    payload["uploadId"] = upload_id
    payload["fileName"] = file_name
    payload["status"] = status

    payload.setdefault("startedAt", now)
    payload["finishedAt"] = now

    return payload


def _build_fail_payload(
    analysis_id: int,
    upload_id: str,
    file_name: str,
    exc: Exception,
) -> dict:
    return {
        "analysisId": analysis_id,
        "uploadId": upload_id,
        "fileName": file_name,
        "status": "FAIL",
        "finishedAt": _now_iso(),
        "errorMessage": str(exc),
        "trace": traceback.format_exc(),
    }


def run_analysis_job(
    analysis_id: int,
    upload_id: str,
    file_name: str,
    file_path: Optional[str] = None,
    callback_url: Optional[str] = None,
) -> None:
    print("[PIPELINE] 백그라운드 분석 작업 시작", flush=True)
    print(f"[PIPELINE] analysis_id={analysis_id}", flush=True)
    print(f"[PIPELINE] upload_id={upload_id}", flush=True)
    print(f"[PIPELINE] file_name={file_name}", flush=True)
    print(f"[PIPELINE] file_path={file_path}", flush=True)
    print(f"[PIPELINE] callback_url={callback_url}", flush=True)

    try:
        print("[PIPELINE] 입력 파일 준비 시작", flush=True)
        _prepare_input_file(file_path)
        print("[PIPELINE] 입력 파일 준비 완료", flush=True)

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
            file_name=file_name,
            status="COMPLETED",
        )

        print("[PIPELINE] callback 전송 시작", flush=True)
        send_callback(callback_url, payload)
        print("[PIPELINE] 분석 작업 완료", flush=True)

    except Exception as exc:
        print("[PIPELINE] 분석 작업 실패", flush=True)
        print(str(exc), flush=True)
        print(traceback.format_exc(), flush=True)

        fail_payload = _build_fail_payload(
            analysis_id=analysis_id,
            upload_id=upload_id,
            file_name=file_name,
            exc=exc,
        )

        send_callback(callback_url, fail_payload)

        if not callback_url:
            raise
