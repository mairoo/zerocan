#!/usr/bin/env python3
"""
reCAPTCHA 테스트용 간단한 Python 웹 서버
포트 3000에서 HTML 파일을 서빙합니다.
"""

import http.server
import socketserver
import os
import sys
from pathlib import Path

PORT = 3000  # reCAPTCHA 도메인 설정을 위해 80 포트 사용
DIRECTORY = "."

class CustomHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        # CORS 헤더 추가 (개발 환경용)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        super().end_headers()

    def do_OPTIONS(self):
        # CORS preflight 요청 처리
        self.send_response(200)
        self.end_headers()

    def log_message(self, format, *args):
        # 로그 포맷 개선
        print(f"[{self.log_date_time_string()}] {format % args}")

def main():
    # 현재 디렉토리에 HTML 파일이 있는지 확인
    html_files = ["index.html", "recaptcha-v2-test.html", "recaptcha-v3-test.html", "recaptcha-status-test.html"]
    missing_files = [f for f in html_files if not Path(f).exists()]

    if missing_files:
        print("❌ 오류: 다음 HTML 파일들을 찾을 수 없습니다:")
        for f in missing_files:
            print(f"   - {f}")
        print("이 스크립트와 같은 디렉토리에 모든 HTML 파일을 배치해주세요.")
        sys.exit(1)

    try:
        with socketserver.TCPServer(("", PORT), CustomHTTPRequestHandler) as httpd:
            print(f"🚀 서버 시작: http://localhost:{PORT}")
            print(f"📁 서빙 디렉토리: {os.path.abspath(DIRECTORY)}")
            print(f"🌐 메인 페이지: http://localhost:{PORT}")
            print(f"🌐 테스트 페이지들:")
            print(f"   - v2 테스트: http://localhost:{PORT}/recaptcha-v2-test.html")
            print(f"   - v3 테스트: http://localhost:{PORT}/recaptcha-v3-test.html")
            print(f"   - 상태 확인: http://localhost:{PORT}/recaptcha-status-test.html")
            print("📋 사용법:")
            print("   1. 브라우저에서 위 URL에 접속")
            print("   2. reCAPTCHA Site Key 입력")
            print("   3. API 서버 URL 확인 (기본: http://localhost:8080)")
            print("   4. v2/v3 테스트 실행")
            print("\n⏹️  서버 종료: Ctrl+C")
            print("-" * 60)

            httpd.serve_forever()

    except KeyboardInterrupt:
        print("\n\n🛑 서버가 중단되었습니다.")
    except OSError as e:
        if e.errno == 48:  # Address already in use
            print(f"❌ 오류: 포트 {PORT}이 이미 사용 중입니다.")
            print("다른 프로세스를 종료하거나 다른 포트를 사용해주세요.")
        else:
            print(f"❌ 서버 시작 오류: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()