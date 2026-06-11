from typing import Optional

from pydantic import BaseModel


class RetrieveRequest(BaseModel):
    query_text: str
    analysis_id: Optional[int] = None
    user_id: Optional[int] = None


class RetrieveResponse(BaseModel):
    returned_faq_seq_num: Optional[int] = None
    answer: Optional[str] = None
    rerank_score: Optional[float] = None
