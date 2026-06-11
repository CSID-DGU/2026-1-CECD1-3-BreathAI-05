import os
from typing import Any

import pymysql
from pymysql.connections import Connection
from pymysql.cursors import DictCursor


def get_db_connection() -> Connection:
    """
    AI 서버에서 MariaDB에 접속하기 위한 공통 connection 생성 함수.

    환경변수:
    - DB_HOST
    - DB_PORT
    - DB_NAME
    - DB_USER
    - DB_PASSWORD
    """

    host = os.getenv("DB_HOST", "127.0.0.1")
    port = int(os.getenv("DB_PORT", "3307"))
    database = os.getenv("DB_NAME", "ttobagi_db")
    user = os.getenv("DB_USER", "tbg_admin")
    password = os.getenv("DB_PASSWORD", "tbg_pw_260501")

    return pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        cursorclass=DictCursor,
        autocommit=False,
    )