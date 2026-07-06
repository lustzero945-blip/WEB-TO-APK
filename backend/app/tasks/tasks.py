# tasks.py - Celery tasks for modern high-performance Android compilation
import logging
from .build_tasks import run_asynchronous_compile

logger = logging.getLogger("LustTasks")

# Re-expose run_asynchronous_compile so Celery autodiscover finds it in app.tasks.tasks module
__all__ = ["run_asynchronous_compile"]
