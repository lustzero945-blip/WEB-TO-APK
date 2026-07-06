from fastapi import APIRouter, Depends, HTTPException, status, UploadFile, File
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from typing import List, Optional
import os
from datetime import datetime
from app.database import get_db
from app.models.project import Project
from app.models.user import User
from app.schemas.all_schemas import ProjectCreate, ProjectResponse, ProjectUpdate
from app.routers.auth import get_current_user
from app.services.storage import StorageService
from app.services.security import SecurityService
from app.services import ImageProcessorService

router = APIRouter(prefix="/projects", tags=["Web Configurations & Projects"])

@router.post("", response_model=ProjectResponse, status_code=status.HTTP_201_CREATED)
def create_project(payload: ProjectCreate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Creates a new Web-to-APK customization project blueprint."""
    new_project = Project(
        user_id=current_user.id,
        name=payload.name,
        website_url=payload.website_url,
        package_name=payload.package_name,
        version=payload.version,
        configuration_json=payload.configuration_json
    )
    db.add(new_project)
    db.commit()
    db.refresh(new_project)
    return new_project


@router.get("", response_model=List[ProjectResponse])
def list_user_projects(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Retrieves all project profiles associated with the authenticated account."""
    projects = db.query(Project).filter(Project.user_id == current_user.id).all()
    return projects


@router.get("/{project_id}", response_model=ProjectResponse)
def get_project_details(project_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Retrieves detailed attributes of a specific project profile."""
    project = db.query(Project).filter(Project.id == project_id, Project.user_id == current_user.id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Target APK configuration project not located.")
    return project


@router.put("/{project_id}", response_model=ProjectResponse)
def update_project(project_id: int, payload: ProjectUpdate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Modifies custom compiler inputs for a specified project configuration."""
    project = db.query(Project).filter(Project.id == project_id, Project.user_id == current_user.id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Target APK configuration project not located.")

    # Apply updates dynamically
    if payload.name is not None:
        project.name = payload.name
    if payload.website_url is not None:
        project.website_url = payload.website_url
    if payload.package_name is not None:
        project.package_name = payload.package_name
    if payload.version is not None:
        project.version = payload.version
    if payload.configuration_json is not None:
        project.configuration_json = payload.configuration_json

    db.commit()
    db.refresh(project)
    return project


@router.delete("/{project_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_project(project_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Deletes the project profile and purges the associated resource uploads from database."""
    project = db.query(Project).filter(Project.id == project_id, Project.user_id == current_user.id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Target APK configuration project not located.")
        
    # Clean up physical icon upload from storage first
    if project.icon_path and os.path.exists(project.icon_path):
        try:
            os.remove(project.icon_path)
        except OSError:
            pass

    db.delete(project)
    db.commit()
    return None


@router.post("/{project_id}/icon", response_model=ProjectResponse)
async def upload_project_launcher_icon(
    project_id: int, 
    file: UploadFile = File(...), 
    current_user: User = Depends(get_current_user), 
    db: Session = Depends(get_db)
):
    """Uploads, validates, and resizes a launcher icon for Android mapping densities (mdpi-xxxhdpi)."""
    project = db.query(Project).filter(Project.id == project_id, Project.user_id == current_user.id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Target APK configuration project not located.")

    # Read bytes to check contents securely
    content = await file.read()
    content_length = len(content)

    # Validate size and extension formats
    SecurityService.validate_uploaded_icon(file.filename, content_length)

    # Cleanup any old icons to enforce storage space discipline
    for p in [project.icon_path, project.icon_original_path]:
        if p and os.path.exists(p):
            try:
                os.remove(p)
            except OSError:
                pass
                
    if project.icon_processed_path and os.path.exists(project.icon_processed_path):
        import shutil
        try:
            shutil.rmtree(project.icon_processed_path)
        except OSError:
            pass

    # Save and Process images using Pillow Service
    result = ImageProcessorService.process_and_save_icons(
        content=content,
        filename=file.filename,
        user_id=current_user.id,
        project_id=project_id
    )

    # Register into project record
    project.icon_path = result["preview_path"]
    project.icon_original_path = result["original_path"]
    project.icon_processed_path = result["processed_dir"]
    project.icon_uploaded_at = datetime.utcnow()
    
    db.commit()
    db.refresh(project)
    return project


@router.get("/{project_id}/icon")
def preview_project_launcher_icon(
    project_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Retrieve/Stream the uploaded icon file for previewing in the mobile application framework."""
    project = db.query(Project).filter(Project.id == project_id, Project.user_id == current_user.id).first()
    if not project:
        raise HTTPException(status_code=404, detail="Target APK configuration project not located.")

    icon_to_serve = project.icon_path or project.icon_original_path
    if not icon_to_serve or not os.path.exists(icon_to_serve):
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Launcher icon not uploaded for this project yet."
        )

    return FileResponse(
        path=icon_to_serve,
        media_type="image/png"
    )

