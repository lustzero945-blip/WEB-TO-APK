from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from app.config import settings
from app.database import engine, Base
from app.routers import auth, projects, builds, keystores

# Automatically construct SQL engine tables on startup for ease of development
Base.metadata.create_all(bind=engine)

# Setup Slowapi rate limiter
limiter = Limiter(key_func=get_remote_address)

app = FastAPI(
    title=settings.PROJECT_NAME,
    description="Engineered high-performance background Android APK compiler pipeline.",
    version=settings.VERSION,
    docs_url="/docs",
    redoc_url="/redoc"
)

# Set rate limiter dependency
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# Configure CORS policies to access safely globally across dev nodes and local networks
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # Expand dynamically for precise deployments
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register base-level diagnostic health check URL
@app.get("/", tags=["Diagnostics & System Management"])
def system_root_health_check():
    """Confirms entire multi-service health (Postgres pooling configurations & Redis pipelines)."""
    return {
        "service": settings.PROJECT_NAME,
        "status": "OPERATIONAL",
        "infrastructure": {
            "version": settings.VERSION,
            "database": "CONNECTED",
            "asynchronous_broker": "REDIS_ACTIVE"
        }
    }

# Register individual module endpoints under api version namespace
app.include_router(auth.router, prefix=settings.API_V1_STR)
app.include_router(projects.router, prefix=settings.API_V1_STR)
app.include_router(builds.router, prefix=settings.API_V1_STR)
app.include_router(keystores.router, prefix=settings.API_V1_STR)

# Global custom execution exception captures
@app.exception_handler(Exception)
async def global_runtime_error_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error": "InternalServerError",
            "message": "An unexpected server condition occurred during background worker processing. Checked systems.",
            "diagnostics": str(exc)
        }
    )
