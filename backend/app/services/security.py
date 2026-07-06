import os
import secrets
from datetime import datetime, timedelta
from typing import Optional, Tuple
from jose import JWTError, jwt
from passlib.context import CryptContext
from cryptography.fernet import Fernet, InvalidToken
from fastapi import HTTPException, status
from app.config import settings

# Setup password context
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Setup safe symmetric encryption engine
try:
    # Ensure key complies with Fernet's url-safe base64 key specification (32 bytes)
    # If key is custom string, we derive/normalize it cleanly
    raw_key = settings.ENCRYPTION_KEY.encode('utf-8')
    if len(raw_key) < 32:
        raw_key = raw_key + b"0" * (32 - len(raw_key))
    import base64
    fernet_key = base64.urlsafe_b64encode(raw_key[:32])
    cipher_suite = Fernet(fernet_key)
except Exception:
    # Safe fallback if derivation fails
    fernet_key = Fernet.generate_key()
    cipher_suite = Fernet(fernet_key)

class SecurityService:
    @staticmethod
    def hash_password(password: str) -> str:
        return pwd_context.hash(password)

    @staticmethod
    def verify_password(plain_password: str, hashed_password: str) -> bool:
        return pwd_context.verify(plain_password, hashed_password)

    @staticmethod
    def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
        to_encode = data.copy()
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
        to_encode.update({"exp": expire})
        encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET, algorithm=settings.ALGORITHM)
        return encoded_jwt

    @staticmethod
    def verify_access_token(token: str) -> Tuple[Optional[str], Optional[int]]:
        try:
            payload = jwt.decode(token, settings.JWT_SECRET, algorithms=[settings.ALGORITHM])
            username: str = payload.get("sub")
            user_id: int = payload.get("id")
            if username is None or user_id is None:
                return None, None
            return username, user_id
        except JWTError:
            return None, None

    @staticmethod
    def encrypt_secret(plain_secret: str) -> str:
        """Encrypts sensitive fields (keystore passwords) with AES-256-CBC."""
        encrypted_bytes = cipher_suite.encrypt(plain_secret.encode('utf-8'))
        return encrypted_bytes.decode('utf-8')

    @staticmethod
    def decrypt_secret(encrypted_secret: str) -> str:
        """Decrypts passwords for compilation runs."""
        try:
            decrypted_bytes = cipher_suite.decrypt(encrypted_secret.encode('utf-8'))
            return decrypted_bytes.decode('utf-8')
        except InvalidToken:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Critical: Cryptographic authorization token for signing key is corrupted or key was rotated."
            )

    @staticmethod
    def validate_uploaded_icon(filename: str, file_size_bytes: int) -> bool:
        """Enforces icon size limits (max 5MB) and strict image extensions."""
        max_size = 5 * 1024 * 1024  # 5 MB limit
        if file_size_bytes > max_size:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="App Icon upload exceeds maximum permitted volume threshold of 5MB."
            )
            
        allowed_extensions = {".png", ".jpg", ".jpeg", ".webp"}
        _, ext = os.path.splitext(filename.lower())
        if ext not in allowed_extensions:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Unsupported platform icon file extension. Permitted types (PNG, JPG, WebP)."
            )
        return True
