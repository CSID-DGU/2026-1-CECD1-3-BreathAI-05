from pydantic import BaseModel


class PipelineResponse(BaseModel):
    """
    /pipeline 요청 수신 직후 반환하는 응답 모델.

    실제 분석 결과는 이 응답에 포함하지 않는다.
    분석은 BackgroundTask에서 수행되고,
    결과는 Spring Boot callback endpoint로 전송한다.
    """

    analysisId: int
    uploadId: str
    status: str
    message: str