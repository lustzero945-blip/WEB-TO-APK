import os
from pydantic_settings import BaseSettings
from pydantic import Field

class Settings(BaseSettings):
    # App General configurations
    PROJECT_NAME: str = "LUST Web APK Compiler Ecosystem"
    VERSION: str = "2.0.0"
    API_V1_STR: str = "/api"

    # Database parameters
    DATABASE_URL: str = Field(
        default="postgresql://lust_admin:lust_secure_password_102@localhost:5432/lust_web_apk_db",
        env="DATABASE_URL"
    )
    
    # Redis Queue parameters
    REDIS_URL: str = Field(
        default="redis://localhost:6379/0",
        env="REDIS_URL"
    )

    # Cryptographic authentication parameters
    JWT_SECRET: str = Field(
        default="09d25e094faa6ca2556c818166b7a9563b93f7099f6f0f4caa6cf63b88e8d3e7",
        env="JWT_SECRET"
    )
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7 # 1 week

    # Encryption key for signing keystore passwords
    # 32-byte key is required for Fernet encryption
    ENCRYPTION_KEY: str = Field(
        default="fR4q9Xm5p2Zt1v7xK8w4e6r8M0u2i1o3-abcdef=",
        env="ENCRYPTION_KEY"
    )

    # Local storage configurations
    STORAGE_DIR: str = "/storage"
    UPLOADED_ICONS_DIR: str = "/storage/uploaded_icons"
    APK_BUILDS_DIR: str = "/storage/apk_builds"
    KEYSTORES_DIR: str = "/storage/keystores"
    TEMPLATES_DIR: str = "/storage/templates"

    # User Storage limits (Default 100MB per user account)
    USER_STORAGE_QUOTA_BYTES: int = 100 * 1024 * 1024 

    # Rate limiting thresholds
    RATE_LIMIT_LOGIN: str = "5/minute"
    RATE_LIMIT_BUILDS: str = "30/hour"

    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()

# Ensure all physical directories exist in the running environment
for path in [settings.STORAGE_DIR, settings.UPLOADED_ICONS_DIR, settings.APK_BUILDS_DIR, settings.KEYSTORES_DIR, settings.TEMPLATES_DIR]:
    os.makedirs(path, exist_ok=True)
