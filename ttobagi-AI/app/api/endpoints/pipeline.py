import shutil
from pathlib import Path
from uuid import uuid4

from fastapi import APIRouter, BackgroundTasks, File, Form, HTTPException, UploadFile

from app.schemas.pipeline import PipelineResponse
from app.services.pipeline_runner import run_pipeline_job


router = APIRouter()


# 현재 파일 위치:
# ttobagi-AI/app/api/endpoints/pipeline.py
#
# 업로드 임시 저장 위치:
# ttobagi-AI/uploads/
UPLOAD_DIR = Path(__file__).resolve().parents[3] / "uploads"


@router.post("/pipeline", response_model=PipelineResponse)
async def pipeline(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    analysisId: int = Form(...),
    uploadId: str = Form(...),
    isMaskingEnabled: bool = Form(True),
    isTranslationEnabled: bool = Form(False),
    periodStartDate: str = Form(...),
    periodEndDate: str = Form(...),
):
    """
    Spring Boot가 호출하는 AI 서버의 단일 외부 endpoint.

    요청 형식:
    multipart/form-data

    요청 필드:
    - file: 업로드된 미답변 엑셀/CSV 파일
    - analysisId: Spring Boot 분석 ID
    - uploadId: Spring Boot 업로드 파일 ID
    - isMaskingEnabled: 비식별화 적용 여부
    - isTranslationEnabled: 번역 적용 여부
    - periodStartDate: 분석 시작일
    - periodEndDate: 분석 종료일

    내부 처리 흐름:
    /pipeline
    → 업로드 파일 임시 저장
    → BackgroundTask 등록
    → pipeline_runner.run_pipeline_job()
    → PREPROCESSING callback
    → Bronze DB raw 적재
    → ANALYZING callback
    → 기존 services 파이프라인 실행
    → COMPLETED callback
    """

    print("[PIPELINE API] /pipeline 요청 수신", flush=True)
    print(f"[PIPELINE API] analysisId={analysisId}", flush=True)
    print(f"[PIPELINE API] uploadId={uploadId}", flush=True)
    print(f"[PIPELINE API] filename={file.filename}", flush=True)
    print(f"[PIPELINE API] isMaskingEnabled={isMaskingEnabled}", flush=True)
    print(f"[PIPELINE API] isTranslationEnabled={isTranslationEnabled}", flush=True)
    print(f"[PIPELINE API] periodStartDate={periodStartDate}", flush=True)
    print(f"[PIPELINE API] periodEndDate={periodEndDate}", flush=True)

    if not file.filename:
        raise HTTPException(
            status_code=400,
            detail="업로드 파일명이 비어 있습니다.",
        )

    file_ext = Path(file.filename).suffix.lower()

    if file_ext not in {".xlsx", ".xls", ".csv"}:
        raise HTTPException(
            status_code=400,
            detail="지원하지 않는 파일 형식입니다. xlsx, xls, csv만 업로드할 수 있습니다.",
        )

    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

    saved_file_name = f"{analysisId}_{uuid4().hex}{file_ext}"
    saved_file_path = UPLOAD_DIR / saved_file_name

    try:
        with saved_file_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=f"업로드 파일 저장 중 오류가 발생했습니다: {exc}",
        ) from exc

    finally:
        await file.close()

    print(f"[PIPELINE API] 업로드 파일 저장 완료: {saved_file_path}", flush=True)

    background_tasks.add_task(
        run_pipeline_job,
        analysis_id=analysisId,
        upload_id=uploadId,
        file_path=str(saved_file_path),
        original_file_name=file.filename,
        is_masking_enabled=isMaskingEnabled,
        is_translation_enabled=isTranslationEnabled,
        period_start_date=periodStartDate,
        period_end_date=periodEndDate,
    )

    print("[PIPELINE API] BackgroundTask 등록 완료", flush=True)

    return PipelineResponse(
        analysisId=analysisId,
        uploadId=uploadId,
        status="ACCEPTED",
        message="AI 파이프라인 요청을 수신했습니다. 분석은 백그라운드에서 실행됩니다.",
    )