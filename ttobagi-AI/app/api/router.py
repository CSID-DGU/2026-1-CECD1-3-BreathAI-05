from fastapi import APIRouter

from app.api.endpoints import analyze, pipeline, retrieve


router = APIRouter()

router.include_router(pipeline.router, tags=["pipeline"])
router.include_router(analyze.router, tags=["analyze"])
router.include_router(retrieve.router, tags=["retrieve"])