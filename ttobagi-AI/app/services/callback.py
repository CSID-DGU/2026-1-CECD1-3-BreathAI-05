import os
from datetime import datetime
from typing import Any, Dict, Optional

import requests


DEFAULT_BACKEND_BASE_URL = "http://host.docker.internal:8080"


def _get_backend_base_url() -> str:
    """
    Spring Boot 서버 기본 URL을 가져온다.

    우선순위:
    1. BACKEND_BASE_URL
    2. SPRING_CALLBACK_URL
    3. 기본값 http://localhost:8080
    """

    return (
        os.getenv("BACKEND_BASE_URL")
        or os.getenv("SPRING_CALLBACK_URL")
        or DEFAULT_BACKEND_BASE_URL
    ).rstrip("/")


def _build_callback_url(analysis_id: int) -> str:
    backend_base_url = _get_backend_base_url()
    return f"{backend_base_url}/api/v1/dashboard/analyze/callback/{analysis_id}"


def send_status_callback(
    analysis_id: int,
    upload_id: str,
    status: str,
    message: str,
    file_name: Optional[str] = None,
    extra: Optional[Dict[str, Any]] = None,
) -> None:
    """
    PREPROCESSING / ANALYZING 같은 중간 상태 callback 전송.

    중간 상태에서는 최종 분석 payload를 포함하지 않는다.
    Spring Boot는 이 callback을 받아 gold_analysis_job.status만 갱신하면 된다.
    """

    now = datetime.now().isoformat(timespec="seconds")

    payload: Dict[str, Any] = {
        "analysisId": analysis_id,
        "uploadId": upload_id,
        "status": status,
        "message": message,
        "updatedAt": now,
    }

    if file_name:
        payload["fileName"] = file_name

    if extra:
        payload.update(extra)

    send_callback(
        analysis_id=analysis_id,
        payload=payload,
    )


def send_callback(analysis_id: int, payload: dict) -> None:
    """
    Spring Boot callback endpoint로 payload를 전송한다.

    endpoint:
    POST /api/v1/dashboard/analyze/callback/{analysisId}
    """

    callback_url = _build_callback_url(analysis_id)

    print(f"[CALLBACK] callback_url={callback_url}", flush=True)
    print(f"[CALLBACK] status={payload.get('status')}", flush=True)

    response = requests.post(
        callback_url,
        json=payload,
        timeout=30,
    )

    response.raise_for_status()

    print(
        f"[CALLBACK] callback 전송 완료 status_code={response.status_code}",
        flush=True,
    )
