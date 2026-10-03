package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.MediaResult
import com.example.data.model.MediaType
import com.example.data.repository.SaveTrickRepository
import com.example.util.UrlValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingDownloaderService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: SaveTrickRepository

    private var isExpanded = true
    private var resolvedMedia: MediaResult? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = SaveTrickRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForegroundNotification()
        createFloatingWindow()
    }

    private fun startForegroundNotification() {
        val channelId = "savetrick_floating_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SaveTrick Floating Window",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating downloader active over other apps"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("SaveTrick Floating Downloader Active")
            .setContentText("Tap to open full app or drag the floating popup")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            try {
                startForeground(1001, notification)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingWindow() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val initialWidth = (displayMetrics.widthPixels * 0.88f).toInt().coerceIn(320, 420)

        params = WindowManager.LayoutParams(
            initialWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }

        val rootView = buildFloatingLayout(initialWidth)
        floatingView = rootView

        try {
            windowManager.addView(rootView, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to display overlay: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun buildFloatingLayout(widthPx: Int): View {
        val density = resources.displayMetrics.density

        // Root container
        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        // 1. Minimized Bubble View (shown when collapsed)
        val bubbleView = FrameLayout(this).apply {
            val size = (54 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = createBubbleDrawable()
            elevation = 16f
            visibility = View.GONE

            val logoImg = ImageView(context).apply {
                setImageResource(R.drawable.savetrick_logo)
                val pad = (8 * density).toInt()
                setPadding(pad, pad, pad, pad)
                layoutParams = FrameLayout.LayoutParams(size, size)
            }
            addView(logoImg)

            setOnClickListener {
                expandWindow()
            }
        }

        // 2. Expanded Card View
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable()
            val pad = (14 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 20f
            layoutParams = LinearLayout.LayoutParams(
                widthPx,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Header (Title, Drag, Minimize, Close)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(this).apply {
            text = "⚡ SaveTrick Pop-up"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnMinimize = TextView(this).apply {
            text = "–"
            setTextColor(android.graphics.Color.parseColor("#00E5FF"))
            textSize = 20f
            val p = (8 * density).toInt()
            setPadding(p, 0, p, 0)
            setOnClickListener { collapseWindow() }
        }

        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(android.graphics.Color.parseColor("#EF4444"))
            textSize = 16f
            val p = (8 * density).toInt()
            setPadding(p, 0, p, 0)
            setOnClickListener { stopSelf() }
        }

        header.addView(titleView)
        header.addView(btnMinimize)
        header.addView(btnClose)
        cardLayout.addView(header)

        // URL input row
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val topMargin = (10 * density).toInt()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = topMargin
            layoutParams = lp
        }

        val editUrl = EditText(this).apply {
            hint = "Paste TikTok link..."
            setHintTextColor(android.graphics.Color.parseColor("#888888"))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 13f
            maxLines = 1
            background = createInputDrawable()
            val padH = (10 * density).toInt()
            val padV = (8 * density).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnTouchListener { _, _ ->
                params?.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                safeUpdateLayout()
                false
            }
        }

        val btnPaste = Button(this).apply {
            text = "Paste"
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            background = createPrimaryButtonDrawable()
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            )
            lp.marginStart = (8 * density).toInt()
            layoutParams = lp

            setOnClickListener {
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clipData = clipboard?.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString().orEmpty()
                        val sanitized = UrlValidator.sanitizeUrl(text)
                        editUrl.setText(sanitized)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@FloatingDownloaderService, "Could not access clipboard", Toast.LENGTH_SHORT).show()
                }
            }
        }

        inputRow.addView(editUrl)
        inputRow.addView(btnPaste)
        cardLayout.addView(inputRow)

        // Download status text & Progress bar
        val statusText = TextView(this).apply {
            text = "Ready to download from TikTok"
            setTextColor(android.graphics.Color.parseColor("#94A3B8"))
            textSize = 11.5f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = (8 * density).toInt()
            layoutParams = lp
        }
        cardLayout.addView(statusText)

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (6 * density).toInt()
            )
            lp.topMargin = (6 * density).toInt()
            layoutParams = lp
        }
        cardLayout.addView(progressBar)

        // Action Buttons Row (Download Video, Audio, All)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = (10 * density).toInt()
            layoutParams = lp
        }

        val btnDownload = Button(this).apply {
            text = "Download Video"
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            background = createPrimaryButtonDrawable()
            layoutParams = LinearLayout.LayoutParams(0, (40 * density).toInt(), 1f)

            setOnClickListener {
                val input = editUrl.text.toString().trim()
                if (input.isBlank()) {
                    Toast.makeText(this@FloatingDownloaderService, "Paste a TikTok link first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                statusText.text = "Resolving TikTok media..."
                progressBar.visibility = View.VISIBLE
                progressBar.isIndeterminate = true
                isEnabled = false

                serviceScope.launch {
                    val result = repository.resolveUrl(input)
                    withContext(Dispatchers.Main) {
                        progressBar.isIndeterminate = false
                        isEnabled = true

                        result.onSuccess { resolved ->
                            resolvedMedia = resolved
                            statusText.text = "Resolved: ${resolved.title.take(30)}..."

                            when (resolved.type) {
                                MediaType.VIDEO -> {
                                    resolved.videoUrl?.let { vUrl ->
                                        repository.startDownload(
                                            sourceUrl = resolved.sourceUrl,
                                            mediaUrl = vUrl,
                                            title = resolved.title,
                                            thumbnail = resolved.coverUrl.orEmpty(),
                                            mediaType = MediaType.VIDEO
                                        )
                                        statusText.text = "Video download started!"
                                        Toast.makeText(this@FloatingDownloaderService, "Video download started", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                MediaType.PHOTO_SLIDESHOW, MediaType.IMAGE -> {
                                    resolved.images.forEachIndexed { idx, imgUrl ->
                                        repository.startDownload(
                                            sourceUrl = "${resolved.sourceUrl}#img_${idx + 1}",
                                            mediaUrl = imgUrl,
                                            title = "${resolved.title}_Image_${idx + 1}",
                                            thumbnail = imgUrl,
                                            mediaType = MediaType.IMAGE
                                        )
                                    }
                                    statusText.text = "Saved all ${resolved.images.size} photos!"
                                    Toast.makeText(this@FloatingDownloaderService, "Downloading ${resolved.images.size} photos", Toast.LENGTH_SHORT).show()
                                }
                                MediaType.AUDIO -> {
                                    resolved.audioUrl?.let { aUrl ->
                                        repository.startDownload(
                                            sourceUrl = resolved.sourceUrl,
                                            mediaUrl = aUrl,
                                            title = "${resolved.title} (Audio)",
                                            thumbnail = resolved.coverUrl.orEmpty(),
                                            mediaType = MediaType.AUDIO
                                        )
                                        statusText.text = "Audio download started!"
                                    }
                                }
                            }
                        }.onFailure { err ->
                            statusText.text = "Error: ${err.message ?: "Failed to resolve link"}"
                        }
                    }
                }
            }
        }

        actionRow.addView(btnDownload)
        cardLayout.addView(actionRow)

        // Observe active downloads to show real-time speed in popup
        serviceScope.launch {
            repository.activeDownloads.collectLatest { list ->
                val active = list.firstOrNull()
                if (active != null) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = active.progress
                    statusText.text = "${active.progress}% • ${active.downloadSpeed} • ${active.title.take(20)}..."
                } else {
                    if (statusText.text.contains("%")) {
                        statusText.text = "Download complete!"
                        progressBar.visibility = View.GONE
                    }
                }
            }
        }

        // Drag listener for moving the window across the screen
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val dragTouchListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    safeUpdateLayout()
                    true
                }
                else -> false
            }
        }

        header.setOnTouchListener(dragTouchListener)
        bubbleView.setOnTouchListener(dragTouchListener)

        root.addView(cardLayout)
        root.addView(bubbleView)
        return root
    }

    private fun safeUpdateLayout() {
        floatingView?.let { view ->
            if (view.isAttachedToWindow) {
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }
    }

    private fun collapseWindow() {
        val root = floatingView as? FrameLayout ?: return
        val cardLayout = root.getChildAt(0)
        val bubbleView = root.getChildAt(1)

        cardLayout.visibility = View.GONE
        bubbleView.visibility = View.VISIBLE
        isExpanded = false

        params?.width = WindowManager.LayoutParams.WRAP_CONTENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        safeUpdateLayout()
    }

    private fun expandWindow() {
        val root = floatingView as? FrameLayout ?: return
        val cardLayout = root.getChildAt(0)
        val bubbleView = root.getChildAt(1)

        val displayMetrics = resources.displayMetrics
        val targetWidth = (displayMetrics.widthPixels * 0.88f).toInt().coerceIn(320, 420)

        cardLayout.visibility = View.VISIBLE
        bubbleView.visibility = View.GONE
        isExpanded = true

        params?.width = targetWidth
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        safeUpdateLayout()
    }

    private fun createCardDrawable(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 28f
            setColor(android.graphics.Color.parseColor("#F00A111E"))
            setStroke(2, android.graphics.Color.parseColor("#00E5FF"))
        }
    }

    private fun createBubbleDrawable(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor("#007BFF"))
            setStroke(3, android.graphics.Color.parseColor("#00E5FF"))
        }
    }

    private fun createInputDrawable(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(android.graphics.Color.parseColor("#1E293B"))
            setStroke(1, android.graphics.Color.parseColor("#334155"))
        }
    }

    private fun createPrimaryButtonDrawable(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(android.graphics.Color.parseColor("#007BFF"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        floatingView?.let { view ->
            if (view.isAttachedToWindow) {
                try {
                    windowManager.removeView(view)
                } catch (_: Exception) {}
            }
        }
    }
}
