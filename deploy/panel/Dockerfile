FROM python:3.12-slim
WORKDIR /panel
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
COPY templates templates
USER nobody
# Exactly 1 worker: the login rate limiter is in-process memory.
CMD ["gunicorn", "-w", "1", "-b", "0.0.0.0:8080", "app:app"]
