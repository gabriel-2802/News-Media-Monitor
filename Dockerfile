FROM python:3.10-slim

WORKDIR /app

RUN apt-get update && apt-get install -y build-essential

COPY ai_models/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY ai_models/ .

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
