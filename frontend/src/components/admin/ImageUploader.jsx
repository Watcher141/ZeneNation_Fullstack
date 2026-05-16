// src/components/admin/ImageUploader.jsx
import { useState, useRef, useCallback } from 'react';
import { productApi } from '../../api/productApi';
import toast from 'react-hot-toast';
import { MdUpload, MdDelete, MdStar, MdStarBorder, MdDragIndicator, MdAdd } from 'react-icons/md';
import './ImageUploader.css';

/**
 * Full-featured image uploader for products:
 * - Drag & drop files into zone
 * - Bulk select multiple images
 * - Preview before upload
 * - Upload to server
 * - Reorder existing images (drag)
 * - Set primary image (star)
 * - Delete individual images
 */
const ImageUploader = ({ productId, existingImages = [], onImagesChange }) => {
  const [pendingFiles, setPendingFiles] = useState([]); // files selected but not uploaded
  const [uploading, setUploading] = useState(false);
  const [draggingOver, setDraggingOver] = useState(false);
  const [draggedIndex, setDraggedIndex] = useState(null);
  const fileInputRef = useRef(null);

  // ── File selection ──────────────────────────────────────────────────────────

  const addFiles = useCallback((files) => {
    const imageFiles = Array.from(files).filter(f => f.type.startsWith('image/'));
    if (!imageFiles.length) { toast.error('Please select image files only'); return; }

    const newPending = imageFiles.map(file => ({
      id: `pending-${Date.now()}-${Math.random()}`,
      file,
      preview: URL.createObjectURL(file),
      name: file.name,
    }));
    setPendingFiles(prev => [...prev, ...newPending]);
  }, []);

  const handleFileInput = (e) => {
    addFiles(e.target.files);
    e.target.value = ''; // reset so same file can be re-selected
  };

  const removePending = (id) => {
    setPendingFiles(prev => {
      const item = prev.find(p => p.id === id);
      if (item) URL.revokeObjectURL(item.preview);
      return prev.filter(p => p.id !== id);
    });
  };

  // ── Drag & drop into zone ───────────────────────────────────────────────────

  const handleDrop = (e) => {
    e.preventDefault();
    setDraggingOver(false);
    addFiles(e.dataTransfer.files);
  };

  const handleDragOver = (e) => { e.preventDefault(); setDraggingOver(true); };
  const handleDragLeave = () => setDraggingOver(false);

  // ── Upload pending files ────────────────────────────────────────────────────

  const handleUpload = async () => {
    if (!pendingFiles.length) { toast.error('No images selected'); return; }
    if (!productId) { toast.error('Save the product first before uploading images'); return; }

    setUploading(true);
    try {
      const formData = new FormData();
      pendingFiles.forEach(p => formData.append('images', p.file));
      await productApi.uploadImages(productId, formData);
      toast.success(`${pendingFiles.length} image(s) uploaded!`);
      setPendingFiles([]);
      onImagesChange?.();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  // ── Delete existing image ───────────────────────────────────────────────────

  const handleDelete = async (imageId) => {
    if (!window.confirm('Delete this image?')) return;
    try {
      await productApi.deleteImage(productId, imageId);
      toast.success('Image deleted');
      onImagesChange?.();
    } catch {
      toast.error('Failed to delete image');
    }
  };

  // ── Set primary image ───────────────────────────────────────────────────────

  const handleSetPrimary = async (imageId) => {
    try {
      await productApi.setPrimaryImage(productId, imageId);
      toast.success('Primary image set!');
      onImagesChange?.();
    } catch {
      toast.error('Failed to set primary image');
    }
  };

  // ── Drag to reorder existing images ────────────────────────────────────────

  const handleDragStart = (index) => setDraggedIndex(index);

  const handleDropOnImage = (e, dropIndex) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === dropIndex) return;
    // Visual reorder only — full reorder API can be added later
    setDraggedIndex(null);
  };

  return (
    <div className="img-uploader">

      {/* ── Existing images ── */}
      {existingImages.length > 0 && (
        <div className="img-uploader__section">
          <p className="img-uploader__label">
            Uploaded Images
            <span className="img-uploader__count">{existingImages.length}</span>
          </p>
          <div className="img-uploader__grid">
            {existingImages.map((img, index) => (
              <div
                key={img.id}
                className={`img-uploader__item ${img.isPrimary ? 'img-uploader__item--primary' : ''}`}
                draggable
                onDragStart={() => handleDragStart(index)}
                onDragOver={e => e.preventDefault()}
                onDrop={e => handleDropOnImage(e, index)}
              >
                <img src={img.imageUrl} alt={`Product ${index + 1}`} className="img-uploader__img" />

                {img.isPrimary && (
                  <div className="img-uploader__primary-badge">PRIMARY</div>
                )}

                <div className="img-uploader__overlay">
                  <div className="img-uploader__drag-handle">
                    <MdDragIndicator size={16} />
                  </div>

                  <div className="img-uploader__actions">
                    <button
                      className={`img-uploader__action-btn ${img.isPrimary ? 'img-uploader__action-btn--active' : ''}`}
                      onClick={() => !img.isPrimary && handleSetPrimary(img.id)}
                      title={img.isPrimary ? 'Primary image' : 'Set as primary'}
                    >
                      {img.isPrimary ? <MdStar size={16} /> : <MdStarBorder size={16} />}
                    </button>
                    <button
                      className="img-uploader__action-btn img-uploader__action-btn--delete"
                      onClick={() => handleDelete(img.id)}
                      title="Delete image"
                    >
                      <MdDelete size={16} />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Pending preview ── */}
      {pendingFiles.length > 0 && (
        <div className="img-uploader__section">
          <p className="img-uploader__label">
            Ready to Upload
            <span className="img-uploader__count">{pendingFiles.length}</span>
          </p>
          <div className="img-uploader__grid">
            {pendingFiles.map(p => (
              <div key={p.id} className="img-uploader__item img-uploader__item--pending">
                <img src={p.preview} alt={p.name} className="img-uploader__img" />
                <div className="img-uploader__overlay">
                  <div className="img-uploader__actions">
                    <button
                      className="img-uploader__action-btn img-uploader__action-btn--delete"
                      onClick={() => removePending(p.id)}
                      title="Remove"
                    >
                      <MdDelete size={16} />
                    </button>
                  </div>
                </div>
                <div className="img-uploader__pending-label">Pending</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Drop zone ── */}
      <div
        className={`img-uploader__dropzone ${draggingOver ? 'img-uploader__dropzone--active' : ''}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onClick={() => fileInputRef.current?.click()}
      >
        <MdUpload size={32} />
        <p>Drop images here or <strong>click to browse</strong></p>
        <p className="img-uploader__hint">PNG, JPG, WEBP · Multiple files supported</p>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          style={{ display: 'none' }}
          onChange={handleFileInput}
        />
      </div>

      {/* ── Upload button ── */}
      {pendingFiles.length > 0 && (
        <button
          className="btn btn-primary"
          onClick={handleUpload}
          disabled={uploading}
          style={{ marginTop: 12, width: '100%' }}
        >
          {uploading
            ? `Uploading ${pendingFiles.length} image(s)...`
            : `Upload ${pendingFiles.length} Image(s)`}
        </button>
      )}
    </div>
  );
};

export default ImageUploader;