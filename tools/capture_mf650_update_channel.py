"""Capture one approved GET; no retries, redirects, authentication or update actions.

Raw bodies/headers stay in gitignored test-results/update-channel. API requests
require a separately reviewed source gate bound to the saved system.html hash.
"""
import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import urllib.error
import urllib.request


PATHS = {
    "system-page": "/html/system.html",
    "check-update": "/api/system/check-update",
    "update-log": "/api/system/update-log",
}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def capture(kind, root):
    private = root / "test-results/update-channel"
    destination = private / kind
    destination.mkdir(parents=True, exist_ok=True)
    reservation = destination / "attempt.json"
    if reservation.exists():
        raise RuntimeError(f"{kind}: already attempted; no second request allowed")
    if kind != "system-page":
        gate = json.loads((private / "READ_ONLY_GATE.json").read_text(encoding="utf-8"))
        source = private / "system-page/body.bin"
        if hashlib.sha256(source.read_bytes()).hexdigest() != gate["source_sha256"]:
            raise RuntimeError("source hash differs from the reviewed GET gate")
        if gate["endpoints"][kind]["decision"] != "SAFE_GET" or gate["endpoints"][kind]["path"] != PATHS[kind]:
            raise RuntimeError("endpoint has no reviewed SAFE_GET decision")
    now = datetime.now(timezone.utc)
    record = {"url": "http://192.168.100.1:8081" + PATHS[kind], "method": "GET",
              "time_utc": now.isoformat(), "time_shanghai": now.astimezone(timezone(timedelta(hours=8))).isoformat(),
              "automatic_redirects": False, "retries": 0, "request_body": None}
    with reservation.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(record, indent=2) + "\n")
    request = urllib.request.Request(record["url"], method="GET", headers={"Accept": "*/*"})
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    try:
        try:
            response = opener.open(request, timeout=25)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read()
            headers = list(response.headers.raw_items())
            record.update({"status": response.code, "reason": response.reason,
                           "response_url": response.geturl(), "headers": headers,
                           "bytes": len(body), "body_sha256": hashlib.sha256(body).hexdigest()})
            (destination / "body.bin").write_bytes(body)
            (destination / "headers.txt").write_bytes(response.headers.as_bytes())
            try:
                parsed = json.loads(body)
                (destination / "parsed.json").write_text(json.dumps(parsed, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
                record["parsed_json"] = True
            except (ValueError, UnicodeDecodeError):
                record["parsed_json"] = False
    except (OSError, urllib.error.URLError) as error:
        record["error"] = str(error)
    (destination / "response.json").write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k:record[k] for k in ("url", "method", "status", "bytes", "body_sha256", "parsed_json", "error") if k in record}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("kind", choices=PATHS)
    args = parser.parse_args()
    capture(args.kind, Path(__file__).resolve().parents[1])
