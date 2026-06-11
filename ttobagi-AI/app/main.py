from fastapi import FastAPI

from app.api.router import router


app = FastAPI(
    title="Ttobagi AI Server",
    description="AI 기반 미답변 분석 및 FAQ 후보 생성 서버",
    version="0.1.0",
)


@app.get("/")
def read_root():
    return {
        "message": "Ttobagi AI Server is running"
    }


@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "ttobagi-ai"
    }


app.include_router(router)