from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from typing import List, Optional
from datetime import datetime
from app.database import get_db
from app.models.project import Project
from app.models.build import Build
from app.models.user import User
from app.schemas.all_schemas import BuildTrigger, BuildResponse, BuildLogResponse
from app.routers.auth import get_current_user
from app.services.storage import StorageService, base64_decode_str
from app.tasks.build_tasks import run_asynchronous_compile

router = APIRouter(tags=["Compiler & Async Build System"])

@router.post("/projects/{project_id}/build", response_model=BuildResponse, status_code=status.HTTP_202_ACCEPTED)
def trigger_project_compilation(
    project_id: int, 
    payload: BuildTrigger,
    current_user: User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    """Adds a web-to-APK compilations request into the asynchronous Celery-Redis build queue."""
    project = db.query(Project).filter(Project.id == project_id, Project.user_id == current_user.id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Parent project profile not found.")

    # Instantiate Build record in database
    new_build = Build(
        project_id=project.id,
        status="QUEUED",
        progress_percentage=0,
        build_logs="[LUST COMPILER QUEUE]: Job accepted, standing by for first available worker node...\r\n"
    )
    db.add(new_build)
    db.commit()
    db.refresh(new_build)

    # Dispatch to Celery worker asynchronously
    run_asynchronous_compile.delay(build_id=new_build.id, keystore_id=payload.keystore_id)

    return new_build


@router.get("/builds/{build_id}/status", response_model=BuildResponse)
def get_build_status(build_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Provides current state parameters (e.g., Progress metrics, Execution State)."""
    build = db.query(Build).join(Project).filter(Build.id == build_id, Project.user_id == current_user.id).first()
    if not build:
        raise HTTPException(status_code=404, detail="Asynchronous compile task record not found.")
    return build


@router.get("/builds/{build_id}/logs", response_model=BuildLogResponse)
def get_build_logs(build_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Streams live console log strings captured during Gradle build actions."""
    build = db.query(Build).join(Project).filter(Build.id == build_id, Project.user_id == current_user.id).first()
    if not build:
        raise HTTPException(status_code=404, detail="Asynchronous compile task record not found.")
    
    return {
        "build_id": build.id,
        "status": build.status,
        "progress_percentage": build.progress_percentage,
        "logs": build.build_logs or ""
    }


@router.get("/builds/{build_id}/download-url")
def get_secure_download_url(build_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Generates a secure temporary download link URL with signature tokens that expires in 1 hour."""
    build = db.query(Build).join(Project).filter(Build.id == build_id, Project.user_id == current_user.id).first()
    if not build:
        raise HTTPException(status_code=404, detail="Asynchronous compile task record not found.")

    if build.status != "COMPLETED" or not build.apk_path:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="APK binary files not assembled or compilation failed."
        )

    signed_url = StorageService.generate_signed_url(build.apk_path)
    return {"download_url": signed_url}


@router.get("/builds/{build_id}/download")
def download_apk_file_directly(
    build_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Directly stream download the assembled APK file for authorized users."""
    import os
    build = db.query(Build).join(Project).filter(Build.id == build_id, Project.user_id == current_user.id).first()
    if not build:
        raise HTTPException(status_code=404, detail="Asynchronous compile task record not found.")

    if build.status != "COMPLETED" or not build.apk_path:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="APK binary is not fully compiled or signed yet."
        )

    if not os.path.exists(build.apk_path):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Physical APK file could not be found or has expired."
        )

    return FileResponse(
        path=build.apk_path,
        media_type="application/vnd.android.package-archive",
        filename=os.path.basename(build.apk_path)
    )


# Secure physical download stream route matching base64 ticket token keys
@router.get("/builds/download/{token}")
def download_apk_file(token: str):
    """Securely streams the physical compiled binary package back using token validation."""
    try:
        decoded = base64_decode_str(token)
        file_path, expire = decoded.split(":")
        expire_time = int(expire)
    except Exception:
        raise HTTPException(status_code=400, detail="Malformed or modified security download ticket.")

    # Enforce time limits
    if datetime.utcnow().timestamp() > expire_time:
        raise HTTPException(status_code=403, detail="Temporary security download link ticket has expired.")

    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Physical binary file has expired or was removed by storage quotas.")

    # Determine app naming on save
    base_name = os.path.basename(file_path)
    return FileResponse(
        path=file_path,
        media_type="application/vnd.android.package-archive",
        filename=base_name
    )
