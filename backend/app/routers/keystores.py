from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File, Form
from sqlalchemy.orm import Session
from typing import List
import os
from app.database import get_db
from app.models.keystore import Keystore
from app.models.user import User
from app.schemas.all_schemas import KeystoreResponse
from app.routers.auth import get_current_user
from app.services.storage import StorageService
from app.services.security import SecurityService

router = APIRouter(prefix="/keystores", tags=["Security & Release Signing Keys"])

@router.post("", response_model=KeystoreResponse, status_code=status.HTTP_201_CREATED)
async def register_signing_keystore(
    alias: str = Form(...),
    password: str = Form(...),
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Registers an authentic Android .jks or .keystore certificate, encrypting
    passphrases with 256-bit keys before persisting.
    """
    # Restrict file type uploads to signing files
    _, ext = os.path.splitext(file.filename.lower())
    if ext not in {".jks", ".keystore"}:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Unsupported signature certificate type. Please upload standardized format (.jks or .keystore)."
        )

    content = await file.read()
    content_length = len(content)

    # Check database storage quotas
    StorageService.check_user_storage_quota(current_user.id, content_length)

    # Save to user storage folder
    saved_path = StorageService.save_uploaded_file(
        content=content,
        filename=file.filename,
        subfolder="keystores",
        user_id=current_user.id
    )

    # Encrypt raw password via security helper
    encrypted_pw = SecurityService.encrypt_secret(password)

    new_keystore = Keystore(
        user_id=current_user.id,
        encrypted_file_path=saved_path,
        alias=alias,
        encrypted_password=encrypted_pw
    )
    
    db.add(new_keystore)
    db.commit()
    db.refresh(new_keystore)
    return new_keystore


@router.get("", response_model=List[KeystoreResponse])
def list_registered_keystores(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Exposes all credential profiles associated with current developer identity."""
    keystores = db.query(Keystore).filter(Keystore.user_id == current_user.id).all()
    return keystores


@router.delete("/{keystore_id}", status_code=status.HTTP_204_NO_CONTENT)
def unregister_keystore(keystore_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Revokes and safely deletes a customized signing certificate file asset."""
    keystore = db.query(Keystore).filter(Keystore.id == keystore_id, Keystore.user_id == current_user.id).first()
    if not keystore:
        raise HTTPException(status_code=404, detail="Target signature keystore details not found.")

    # Remove physical key bytes from disk directory first
    if keystore.encrypted_file_path and os.path.exists(keystore.encrypted_file_path):
        try:
            os.remove(keystore.encrypted_file_path)
        except OSError:
            pass

    db.delete(keystore)
    db.commit()
    return None
