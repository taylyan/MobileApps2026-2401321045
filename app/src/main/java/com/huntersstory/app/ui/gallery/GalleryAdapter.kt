
package com.huntersstory.app.ui.gallery

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.huntersstory.app.R
import com.huntersstory.app.data.local.entity.HuntingPhoto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

fun decodeSampledPhoto(file: File, maxSize: Int): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeFile(file.absolutePath, options)

    if (options.outWidth <= 0 || options.outHeight <= 0) {
        return null
    }

    var sampleSize = 1

    while (
        options.outWidth / sampleSize > maxSize * 2 ||
        options.outHeight / sampleSize > maxSize * 2
    ) {
        sampleSize *= 2
    }

    return BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
    )
}

class GalleryAdapter(
    private val photoDirectory: File,
    private val scope: CoroutineScope,
    private val onView: (HuntingPhoto) -> Unit,
    private val onDelete: (HuntingPhoto) -> Unit
) : ListAdapter<HuntingPhoto, GalleryAdapter.PhotoHolder>(
    PhotoDiff()
) {

    class PhotoHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val image: ImageView = view.findViewById(R.id.ivPhoto)
        val date: TextView = view.findViewById(R.id.tvPhotoDate)
        val viewButton: Button =
            view.findViewById(R.id.btnViewPhoto)
        val deleteButton: Button =
            view.findViewById(R.id.btnDeletePhoto)

        var imageJob: Job? = null
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PhotoHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoHolder(view)
    }

    override fun onBindViewHolder(
        holder: PhotoHolder,
        position: Int
    ) {
        val photo = getItem(position)
        val file = File(photoDirectory, photo.fileName)

        holder.imageJob?.cancel()
        holder.image.setImageDrawable(null)

        holder.date.text = DateFormat.getDateTimeInstance()
            .format(Date(photo.createdAt))

        holder.imageJob = scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                decodeSampledPhoto(file, 300)
            }

            holder.image.setImageBitmap(bitmap)
        }

        holder.image.setOnClickListener {
            onView(photo)
        }

        holder.viewButton.setOnClickListener {
            onView(photo)
        }

        holder.deleteButton.setOnClickListener {
            onDelete(photo)
        }
    }

    override fun onViewRecycled(holder: PhotoHolder) {
        holder.imageJob?.cancel()
        holder.image.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    class PhotoDiff : DiffUtil.ItemCallback<HuntingPhoto>() {

        override fun areItemsTheSame(
            oldItem: HuntingPhoto,
            newItem: HuntingPhoto
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: HuntingPhoto,
            newItem: HuntingPhoto
        ): Boolean = oldItem == newItem
    }
}
