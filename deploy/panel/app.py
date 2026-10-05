"""Airline Club ops panel.

Password-gated control surface for the docker compose stack. Talks to the
Docker Engine API only through the socket proxy (DOCKER_API), which limits
the API surface to container list/inspect/logs/start/stop/restart.

Run with exactly one gunicorn worker: sessions are signed cookies, but the
login rate limiter is in-process memory.
"""

import json
import os
import struct
import time
from functools import wraps

import requests
from flask import (Flask, Response, abort, jsonify, redirect, render_template,
                   request, session, url_for)
from werkzeug.security import check_password_hash

DOCKER_API = os.environ["DOCKER_API"].rstrip("/")
COMPOSE_PROJECT = os.environ.get("COMPOSE_PROJECT", "airline")
ALLOWED_SERVICES = [s.strip() for s in os.environ.get("ALLOWED_SERVICES", "sim,web,es").split(",") if s.strip()]
PASSWORD_HASH = os.environ["PANEL_PASSWORD_HASH"]

app = Flask(__name__)
app.secret_key = os.environ["PANEL_SECRET_KEY"]
app.config.update(SESSION_COOKIE_HTTPONLY=True, SESSION_COOKIE_SAMESITE="Lax")

REQUEST_TIMEOUT = (3, 10)

# --- login rate limiting (per source IP, in-process) ---
FAIL_LIMIT = 5
LOCK_BASE_SECONDS = 30
LOCK_CAP_SECONDS = 15 * 60
_failures = {}  # ip -> {"count": int, "locked_until": float}


def _locked_for(ip):
    entry = _failures.get(ip)
    if not entry:
        return 0
    return max(0, entry["locked_until"] - time.time())


def _record_failure(ip):
    entry = _failures.setdefault(ip, {"count": 0, "locked_until": 0})
    entry["count"] += 1
    if entry["count"] >= FAIL_LIMIT:
        lockouts = entry["count"] - FAIL_LIMIT
        lock = min(LOCK_BASE_SECONDS * (2 ** lockouts), LOCK_CAP_SECONDS)
        entry["locked_until"] = time.time() + lock


def require_auth(fn):
    @wraps(fn)
    def wrapper(*args, **kwargs):
        if not session.get("auth"):
            if request.path.startswith("/api/"):
                abort(401)
            return redirect(url_for("login"))
        return fn(*args, **kwargs)
    return wrapper


# --- Docker API helpers ---

class DockerError(Exception):
    def __init__(self, message, status=502):
        super().__init__(message)
        self.status = status


def docker_get(path, params=None):
    return _docker_call("GET", path, params=params)


def docker_post(path, params=None):
    return _docker_call("POST", path, params=params)


def _docker_call(method, path, params=None):
    try:
        resp = requests.request(method, DOCKER_API + path, params=params, timeout=REQUEST_TIMEOUT)
    except requests.RequestException as e:
        raise DockerError("docker API unreachable: %s" % e.__class__.__name__)
    if resp.status_code == 403:
        raise DockerError("action not permitted by socket proxy", status=502)
    if resp.status_code >= 400:
        raise DockerError("docker API error %d: %s" % (resp.status_code, resp.text[:200]),
                          status=502)
    return resp


@app.errorhandler(DockerError)
def handle_docker_error(e):
    return jsonify({"error": str(e)}), e.status


def project_containers():
    filters = json.dumps({"label": ["com.docker.compose.project=%s" % COMPOSE_PROJECT]})
    resp = docker_get("/v1.41/containers/json", params={"all": "true", "filters": filters})
    return resp.json()


def find_container(service):
    for c in project_containers():
        if c.get("Labels", {}).get("com.docker.compose.service") == service:
            return c
    raise DockerError("no container found for service %r" % service, status=404)


def demux_logs(raw):
    """Strip the 8-byte frame headers of Docker's multiplexed log stream."""
    out = []
    i = 0
    while i + 8 <= len(raw):
        _stream, length = struct.unpack(">BxxxI", raw[i:i + 8])
        out.append(raw[i + 8:i + 8 + length])
        i += 8 + length
    if not out:  # TTY container: stream is not multiplexed
        return raw
    return b"".join(out)


# --- routes ---

@app.route("/healthz")
def healthz():
    return "ok"


@app.route("/login", methods=["GET", "POST"])
def login():
    ip = request.remote_addr or "?"
    if request.method == "POST":
        wait = _locked_for(ip)
        if wait > 0:
            return render_template("login.html", error="Too many attempts. Locked for %d seconds." % int(wait)), 429
        if check_password_hash(PASSWORD_HASH, request.form.get("password", "")):
            _failures.pop(ip, None)
            session["auth"] = True
            return redirect(url_for("index"))
        _record_failure(ip)
        return render_template("login.html", error="Wrong password."), 401
    return render_template("login.html", error=None)


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))


@app.route("/")
@require_auth
def index():
    return render_template("index.html", allowed=ALLOWED_SERVICES)


@app.route("/api/status")
@require_auth
def api_status():
    services = []
    for c in project_containers():
        labels = c.get("Labels", {})
        service = labels.get("com.docker.compose.service", "?")
        status_text = c.get("Status", "")  # e.g. "Up 3 hours (healthy)"
        health = "none"
        if "(healthy)" in status_text:
            health = "healthy"
        elif "(unhealthy)" in status_text:
            health = "unhealthy"
        elif "(health: starting)" in status_text:
            health = "starting"
        services.append({
            "service": service,
            "state": c.get("State", "?"),   # running / exited / ...
            "status": status_text,
            "health": health,
            "image": c.get("Image", "?"),
            "controllable": service in ALLOWED_SERVICES,
        })
    services.sort(key=lambda s: s["service"])
    return jsonify({"services": services})


def _act(service, verb, params=None):
    if service not in ALLOWED_SERVICES:
        abort(404)
    container = find_container(service)
    docker_post("/v1.41/containers/%s/%s" % (container["Id"], verb), params=params)
    return jsonify({"ok": True, "service": service, "action": verb})


@app.route("/api/service/<service>/restart", methods=["POST"])
@require_auth
def api_restart(service):
    return _act(service, "restart", params={"t": 10})


@app.route("/api/service/<service>/stop", methods=["POST"])
@require_auth
def api_stop(service):
    # 30 s grace so a sim cycle can wind down before SIGKILL.
    return _act(service, "stop", params={"t": 30})


@app.route("/api/service/<service>/start", methods=["POST"])
@require_auth
def api_start(service):
    return _act(service, "start")


@app.route("/api/service/<service>/logs")
@require_auth
def api_logs(service):
    # Logs are read-only: every project service is viewable, including db.
    container = find_container(service)
    tail = request.args.get("tail", "200")
    if not tail.isdigit():
        tail = "200"
    resp = docker_get("/v1.41/containers/%s/logs" % container["Id"],
                      params={"stdout": "1", "stderr": "1", "tail": tail, "timestamps": "1"})
    text = demux_logs(resp.content).decode("utf-8", errors="replace")
    return Response(text, mimetype="text/plain")


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
