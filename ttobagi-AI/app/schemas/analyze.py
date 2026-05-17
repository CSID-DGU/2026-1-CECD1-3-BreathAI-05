from datetime import date
from typing import Optional

from pydantic import BaseModel


class AnalyzeRequest(BaseModel):
    analysis_id: int
    upload_id: str
    file_name: str

    # 백엔드 최종 구조에서는 ingest_batch_id 기반 분석을 사용할 수 있음
    ingest_batch_id: Optional[str] = None

    # 현재 1차 구현에서는 기존 services 파이프라인이 파일 기반이므로 file_path도 허용
    file_path: Optional[str] = None

    period_start_date: Optional[date] = None
    period_end_date: Optional[date] = None
    is_masking_enabled: Optional[bool] = True
    is_translation_enabled: Optional[bool] = True

    # Spring callback URL이 확정되기 전까지 테스트용으로 직접 받을 수 있게 둠
    callback_url: Optional[str] = None


class AnalyzeResponse(BaseModel):
    analysis_id: int
    status: str
    message: str
