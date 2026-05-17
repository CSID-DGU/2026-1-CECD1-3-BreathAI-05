from typing import Optional

import requests


def send_callback(callback_url: Optional[str], payload: dict) -> None:
    """
    분석 완료 후 Spring Boot 서버로 결과를 전송한다.
    callback_url이 없으면 전송하지 않고 종료한다.
    """

    if not callback_url:
        return

    response = requests.post(
        callback_url,
        json=payload,
        timeout=30,
    )
    response.raise_for_status()
