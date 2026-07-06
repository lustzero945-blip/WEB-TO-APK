from celery import Celery
from app.config import settings

celery_app = Celery(
    "lust_tasks",
    broker=settings.REDIS_URL,
    backend=settings.REDIS_URL
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="UTC",
    enable_utc=True,
    task_track_started=True,
    task_time_limit=1800, # Max timeout limit structure (30 minutes)
)

# Autodiscover background build tasks
celery_app.autodiscover_tasks(["app.tasks"])
