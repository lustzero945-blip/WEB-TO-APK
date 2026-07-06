from pydantic import BaseModel, Field, EmailStr, field_validator
from typing import Optional, Dict, Any, List
from datetime import datetime
import re

# ================= AUTH SCHEMAS =================

class UserBase(BaseModel):
    username: str = Field(..., min_length=3, max_length=50, examples=["lust_developer"])
    email: EmailStr = Field(..., examples=["valerylusty@gmail.com"])

class UserCreate(UserBase):
    password: str = Field(..., min_length=6, max_length=100, examples=["strongSuperPasscode@"])

class UserResponse(UserBase):
    id: int
    created_at: datetime

    class Config:
        from_attributes = True

class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"

class TokenData(BaseModel):
    username: Optional[str] = None
    user_id: Optional[int] = None


# ================= PROJECT SCHEMAS =================

class ProjectBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=100, examples=["Lust Shopping"])
    website_url: str = Field(..., examples=["https://example.com"])
    package_name: str = Field(..., examples=["com.lust.shopping"])
    version: str = Field(default="1.0.0", examples=["1.0.0"])
    configuration_json: Dict[str, Any] = Field(
        default_factory=lambda: {
            "enableJs": True,
            "enableZoom": False,
            "domStorage": True,
            "orientation": "UNSPECIFIED", # PORTRAIT, LANDSCAPE, UNSPECIFIED
            "displayMode": "NORMAL",      # NORMAL, FULLSCREEN
            "themeColor": "#1E293B",
            "minSdkVersion": 24,
            "targetSdkVersion": 34
        }
    )

    @field_validator("website_url")
    @classmethod
    def validate_url(cls, v: str) -> str:
        url_regex = re.compile(
            r'^(?:http|https)://' # http:// or https://
            r'(?:(?:[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?\.)+(?:[A-Z]{2,6}\.?|[A-Z0-9-]{2,}\.?)|' # domain...
            r'localhost|' # localhost...
            r'\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})' # ...or ip
            r'(?::\d+)?' # optional port
            r'(?:/?|[/?]\S+)$', re.IGNORECASE
        )
        if not url_regex.match(v):
            raise ValueError("Invalid target website URL format. Must start with http or https.")
        return v

    @field_validator("package_name")
    @classmethod
    def validate_package_name(cls, v: str) -> str:
        pkg_regex = re.compile(r'^[a-z][a-z0-9_]*(\.[a-z0-9_]+)+[0-9a-z_]$')
        if not pkg_regex.match(v):
            raise ValueError("Invalid Android Package Name. Must follow reverse-DNS notation (e.g. com.example.app).")
        return v

class ProjectCreate(ProjectBase):
    pass

class ProjectUpdate(BaseModel):
    name: Optional[str] = None
    website_url: Optional[str] = None
    package_name: Optional[str] = None
    version: Optional[str] = None
    configuration_json: Optional[Dict[str, Any]] = None

class ProjectResponse(ProjectBase):
    id: int
    user_id: int
    icon_path: Optional[str] = None
    icon_original_path: Optional[str] = None
    icon_processed_path: Optional[str] = None
    icon_uploaded_at: Optional[datetime] = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


# ================= KEYSTORE SCHEMAS =================

class KeystoreCreate(BaseModel):
    alias: str = Field(..., min_length=2, max_length=50, examples=["release-key"])
    password: str = Field(..., min_length=6, max_length=100, examples=["secStorePass123"])

class KeystoreResponse(BaseModel):
    id: int
    user_id: int
    alias: str
    created_at: datetime

    class Config:
        from_attributes = True


# ================= BUILD SCHEMAS =================

class BuildTrigger(BaseModel):
    keystore_id: Optional[int] = Field(None, description="Optional custom user keystore ID. If omitted, standard debug key is applied.")

class BuildResponse(BaseModel):
    id: int
    project_id: int
    status: str
    progress_percentage: int
    apk_path: Optional[str] = None
    error_message: Optional[str] = None
    started_at: datetime
    finished_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class BuildLogResponse(BaseModel):
    build_id: int
    status: str
    progress_percentage: int
    logs: str

    class Config:
        from_attributes = True
