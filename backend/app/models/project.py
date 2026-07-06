from sqlalchemy import Column, Integer, String, DateTime, ForeignKey, JSON
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base

class Project(Base):
    __tablename__ = "projects"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    name = Column(String(100), nullable=False)
    website_url = Column(String(512), nullable=False)
    package_name = Column(String(150), nullable=False)
    version = Column(String(50), nullable=False, default="1.0.0")
    icon_path = Column(String(512), nullable=True)
    icon_original_path = Column(String(512), nullable=True)
    icon_processed_path = Column(String(512), nullable=True)
    icon_uploaded_at = Column(DateTime(timezone=True), nullable=True)
    configuration_json = Column(JSON, nullable=False, default=dict)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)

    # Relationships
    user = relationship("User", back_populates="projects")
    builds = relationship("Build", back_populates="project", cascade="all, delete-orphan")
