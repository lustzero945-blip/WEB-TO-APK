from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base

class Build(Base):
    __tablename__ = "builds"

    id = Column(Integer, primary_key=True, index=True)
    project_id = Column(Integer, ForeignKey("projects.id", ondelete="CASCADE"), nullable=False)
    
    # Compilation Lifecycle Status:
    # QUEUED, PREPARING, GENERATING_PROJECT, CONFIGURING_GRADLE, BUILDING_APK, SIGNING_APK, COMPLETED, FAILED
    status = Column(String(50), nullable=False, default="QUEUED")
    progress_percentage = Column(Integer, nullable=False, default=0)
    
    build_logs = Column(Text, nullable=True, default="")
    apk_path = Column(String(512), nullable=True)
    error_message = Column(Text, nullable=True)
    
    started_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    finished_at = Column(DateTime(timezone=True), nullable=True)

    # Relationships
    project = relationship("Project", back_populates="builds")
