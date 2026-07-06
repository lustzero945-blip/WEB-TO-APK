from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base

class Keystore(Base):
    __tablename__ = "keystores"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    
    # Path inside /storage/keystores/ where the actual binary sits
    encrypted_file_path = Column(String(512), nullable=False)
    alias = Column(String(100), nullable=False)
    
    # Encrypted passcode payload (using cryptography library)
    encrypted_password = Column(String(512), nullable=False)
    
    created_at = Column(DateTime(timezone=True), server_default=func.now(), nullable=False)

    # Relationships
    user = relationship("User", back_populates="keystores")
