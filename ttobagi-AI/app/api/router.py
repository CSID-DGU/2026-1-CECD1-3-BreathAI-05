from fastapi import APIRouter

from app.api.endpoints import analyze, retrieve


router = APIRouter()

router.include_router(analyze.router, tags=["analyze"])
router.include_router(retrieve.router, tags=["retrieve"])
