from datetime import timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.user import User
from app.schemas.all_schemas import UserCreate, UserResponse, Token
from app.services.security import SecurityService
from app.config import settings

router = APIRouter(prefix="/auth", tags=["Identity & Authorization"])

oauth2_scheme = OAuth2PasswordBearer(tokenUrl=f"{settings.API_V1_STR}/auth/login")

# Shared credential dependencies
def get_current_user(db: Session = Depends(get_db), token: str = Depends(oauth2_scheme)) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Could not validate active identity credentials.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    username, user_id = SecurityService.verify_access_token(token)
    if username is None or user_id is None:
        raise credentials_exception
        
    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        raise credentials_exception
    return user


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_211_CREATED)
def register_user(payload: UserCreate, db: Session = Depends(get_db)):
    """Creates a new developer account, hashing the credentials safely inside the database."""
    # Check if duplicate username or email exists
    existing_username = db.query(User).filter(User.username == payload.username).first()
    if existing_username:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username already registered, please pick another name."
        )
        
    existing_email = db.query(User).filter(User.email == payload.email).first()
    if existing_email:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email address already registered, please log in."
        )

    hashed_pw = SecurityService.hash_password(payload.password)
    new_user = User(
        username=payload.username,
        email=payload.email,
        password_hash=hashed_pw
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return new_user


@router.post("/login", response_model=Token)
def login_user(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    """Verifies credentials and returns a secure JWT authentication token."""
    user = db.query(User).filter(User.username == form_data.username).first()
    if not user or not SecurityService.verify_password(form_data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password combination.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token_payload = {
        "sub": user.username,
        "id": user.id
    }
    jwt_token = SecurityService.create_access_token(data=token_payload)
    return {"access_token": jwt_token, "token_type": "bearer"}
