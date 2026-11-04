FROM python:3.12-slim
WORKDIR /migration
COPY requirements.txt .
RUN --mount=type=secret,id=proxy_ca \
    if [ -s /run/secrets/proxy_ca ]; then export PIP_CERT=/run/secrets/proxy_ca; fi; \
    pip install --no-cache-dir -r requirements.txt
COPY migrate.py schema.sql ./
ENTRYPOINT ["python", "-u", "migrate.py"]
