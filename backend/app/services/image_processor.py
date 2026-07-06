import os
import io
from PIL import Image
from fastapi import HTTPException, status

class ImageProcessorService:
    @staticmethod
    def process_and_save_icons(content: bytes, filename: str, user_id: int, project_id: int) -> dict:
        # Validate MIME type / extension via Pillow
        try:
            img = Image.open(io.BytesIO(content))
            img.verify() # Verify structure
            # Re-open for operations
            img = Image.open(io.BytesIO(content))
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Uploaded file is not a valid image. Error: {str(e)}"
            )

        # Dimension validation
        w, h = img.size
        if w < 48 or h < 48:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Image is too small. Launcher icons must be at least 48x48 pixels."
            )

        # Establish base paths
        user_dir = os.path.join("/storage", f"user_{user_id}")
        original_dir = os.path.join(user_dir, "original_icons")
        processed_dir = os.path.join(user_dir, "processed_icons", f"project_{project_id}")
        
        os.makedirs(original_dir, exist_ok=True)
        os.makedirs(processed_dir, exist_ok=True)
        
        # Save original file with UUID
        import uuid
        file_ext = os.path.splitext(filename)[1].lower()
        if not file_ext or file_ext not in {".png", ".jpg", ".jpeg", ".webp"}:
            file_ext = ".png"
        original_filename = f"{uuid.uuid4()}{file_ext}"
        original_path = os.path.join(original_dir, original_filename)
        with open(original_path, "wb") as f:
            f.write(content)

        # Dynamic icon production loop for each android density
        densities = {
            "mipmap-mdpi": 48,
            "mipmap-hdpi": 72,
            "mipmap-xhdpi": 96,
            "mipmap-xxhdpi": 144,
            "mipmap-xxxhdpi": 192
        }

        # Convert image color space to RGBA
        if img.mode != 'RGBA':
            img = img.convert('RGBA')

        for density, size in densities.items():
            density_dir = os.path.join(processed_dir, density)
            os.makedirs(density_dir, exist_ok=True)
            
            resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
            out_file_path = os.path.join(density_dir, "ic_launcher.png")
            resized_img.save(out_file_path, "PNG", optimize=True)

        # Store preview icon too (512x512)
        preview_path = os.path.join(processed_dir, "ic_launcher_preview.png")
        img.resize((512, 512), Image.Resampling.LANCZOS).save(preview_path, "PNG", optimize=True)

        return {
            "original_path": original_path,
            "processed_dir": processed_dir,
            "preview_path": preview_path
        }
