from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker
from sqlalchemy.pool import QueuePool
from .config import settings

# Create engine with production-ready connection pooling
engine = create_engine(
    settings.DATABASE_URL,
    pool_class=QueuePool,
    pool_size=20,
    max_overflow=10,
    pool_timeout=30,
    pool_recycle=1800,
    echo=False
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()

# Request database context lifecycle utility
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
