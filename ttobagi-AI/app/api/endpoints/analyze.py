from fastapi import APIRouter, BackgroundTasks, HTTPException

from app.schemas.analyze import AnalyzeRequest, AnalyzeResponse
from app.services.pipeline_runner import run_analysis_job


router = APIRouter()


@router.post("/analyze", response_model=AnalyzeResponse)
async def analyze(req: AnalyzeRequest, background_tasks: BackgroundTasks):
    print("[ANALYZE] /analyze 요청 수신", flush=True)
    print(f"[ANALYZE] analysis_id={req.analysis_id}", flush=True)
    print(f"[ANALYZE] upload_id={req.upload_id}", flush=True)
    print(f"[ANALYZE] file_name={req.file_name}", flush=True)
    print(f"[ANALYZE] ingest_batch_id={req.ingest_batch_id}", flush=True)
    print(f"[ANALYZE] file_path={req.file_path}", flush=True)
    print(f"[ANALYZE] callback_url={req.callback_url}", flush=True)

    if not req.ingest_batch_id and not req.file_path:
        raise HTTPException(
            status_code=400,
            detail="ingest_batch_id 또는 file_path 중 하나는 필요합니다.",
        )

    background_tasks.add_task(
        run_analysis_job,
        analysis_id=req.analysis_id,
        upload_id=req.upload_id,
        file_name=req.file_name,
        file_path=req.file_path,
        callback_url=req.callback_url,
    )

    print("[ANALYZE] BackgroundTask 등록 완료", flush=True)

    return AnalyzeResponse(
        analysis_id=req.analysis_id,
        status="ANALYZING",
        message="분석 파이프라인을 시작합니다.",
    )
