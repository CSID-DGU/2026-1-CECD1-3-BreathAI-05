from fastapi import APIRouter, HTTPException

from app.schemas.retrieve import RetrieveRequest, RetrieveResponse


router = APIRouter()


@router.post("/retrieve", response_model=RetrieveResponse)
async def retrieve(req: RetrieveRequest):
    """
    실시간 FAQ 검색 API.

    추후 services/retriever.py를 서버용 함수로 감싼 뒤 연결한다.
    """

    if not req.query_text or not req.query_text.strip():
        raise HTTPException(
            status_code=400,
            detail="query_text가 필요합니다.",
        )

    return RetrieveResponse(
        returned_faq_seq_num=None,
        answer=None,
        rerank_score=None,
    )
