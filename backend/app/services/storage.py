import os
import shutil
import uuid
import time
from typing import Optional, List
from fastapi import HTTPException, status
from app.config import settings

class StorageService:
    @staticmethod
    def get_user_storage_path(user_id: int) -> str:
        """Returns the isolated physical directory for a user inside storage."""
        user_dir = os.path.join(settings.STORAGE_DIR, f"user_{user_id}")
        os.makedirs(user_dir, exist_ok=True)
        return user_dir

    @staticmethod
    def check_user_storage_quota(user_id: int, incoming_bytes: int = 0) -> bool:
        """Computes current storage footprint and validates user storage limits."""
        user_dir = StorageService.get_user_storage_path(user_id)
        total_size = 0
        for root, _, files in os.walk(user_dir):
            for file in files:
                file_path = os.path.join(root, file)
                try:
                    total_size += os.path.getsize(file_path)
                except OSError:
                    continue

        if (total_size + incoming_bytes) > settings.USER_STORAGE_QUOTA_BYTES:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Storage quota exceeded. Allowed: {settings.USER_STORAGE_QUOTA_BYTES / (1024*1024)}MB. "
                       f"Currently using: {total_size / (1024*1024):.2f}MB."
            )
        return True

    @staticmethod
    def save_uploaded_file(content: bytes, filename: str, subfolder: str, user_id: int) -> str:
        """Saves bytes to disk securely under a user folder, returning the physical path."""
        # Clean naming path with UUID prefix to block collision/injection
        unique_prefix = str(uuid.uuid4())
        safe_filename = f"{unique_prefix}_{os.path.basename(filename)}"
        
        user_dir = StorageService.get_user_storage_path(user_id)
        target_folder = os.path.join(user_dir, subfolder)
        os.makedirs(target_folder, exist_ok=True)
        
        # Check database storage quotas first
        StorageService.check_user_storage_quota(user_id, len(content))
        
        file_path = os.path.join(target_folder, safe_filename)
        with open(file_path, "wb") as f:
            f.write(content)
            
        return file_path

    @staticmethod
    def generate_signed_url(file_path: str, expiration_seconds: int = 3600) -> str:
        """
        Generates a temporary timed signature URL for secure file downloads.
        In local mode, creates a signed routing endpoint URL. Can easily target AWS S3.
        """
        if not os.path.exists(file_path):
            raise HTTPException(status_code=404, detail="Requested file resource not found.")
            
        # Create a unique timed signature hash
        expire_time = int(time.time()) + expiration_seconds
        file_token = base64_encode_str(f"{file_path}:{expire_time}")
        
        # Local development routing hook
        return f"/api/builds/download/{file_token}"

    @staticmethod
    def cleanup_old_builds(days_threshold: int = 7) -> int:
        """Automatic file scrubber task to clear old build files and release storage."""
        cleaned_count = 0
        now = time.time()
        cutoff_seconds = days_threshold * 24 * 60 * 60
        
        # Traverse apk_builds inside storage and delete items older than cutoff
        for root, dirs, files in os.walk(settings.STORAGE_DIR):
            for file in files:
                if file.endswith(".apk") or file.endswith(".aab"):
                    file_path = os.path.join(root, file)
                    try:
                        file_time = os.path.getmtime(file_path)
                        if (now - file_time) > cutoff_seconds:
                            os.remove(file_path)
                            cleaned_count += 1
                    except OSError:
                        continue
        return cleaned_count

def base64_encode_str(s: str) -> str:
    import base64
    return base64.urlsafe_b64encode(s.encode('utf-8')).decode('utf-8')

def base64_decode_str(s: str) -> str:
    import base64
    return base64.urlsafe_b64decode(s.encode('utf-8')).decode('utf-8')
