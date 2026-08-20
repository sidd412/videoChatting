package com.videoChatting.echat.presentation.wallet

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.videoChatting.echat.data.remote.ApiService
import com.videoChatting.echat.data.remote.model.InvoiceData
import com.videoChatting.echat.presentation.theme.CyberMidnight
import com.videoChatting.echat.presentation.theme.ElectricIndigo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

// ---------- ViewModel ----------

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    private val _invoice = MutableStateFlow<InvoiceData?>(null)
    val invoice: StateFlow<InvoiceData?> = _invoice

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadInvoice(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = apiService.getInvoice(orderId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _invoice.value = response.body()!!.invoice
                } else {
                    _error.value = "Could not load invoice. Please try again."
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// ---------- Screen ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    orderId: String,
    navController: androidx.navigation.NavController,
    viewModel: InvoiceViewModel = hiltViewModel()
) {
    val invoice   by viewModel.invoice.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()
    val context   = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) { viewModel.loadInvoice(orderId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (invoice != null) {
                        IconButton(
                            onClick = {
                                isDownloading = true
                                generateAndSavePdf(context, invoice!!) { success ->
                                    isDownloading = false
                                    if (success)
                                        Toast.makeText(context, "Invoice saved to Downloads ✅", Toast.LENGTH_LONG).show()
                                    else
                                        Toast.makeText(context, "Failed to save invoice ❌", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            if (isDownloading)
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            else
                                Icon(Icons.Default.Download, contentDescription = "Download Invoice", tint = Color(0xFFFFD700))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberMidnight)
            )
        },
        containerColor = CyberMidnight
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                        Text(error ?: "", color = Color.White.copy(0.7f), textAlign = TextAlign.Center)
                    }
                }
                invoice != null -> {
                    InvoiceCard(invoice!!, onDownload = {
                        isDownloading = true
                        generateAndSavePdf(context, invoice!!) { success ->
                            isDownloading = false
                            if (success)
                                Toast.makeText(context, "Invoice saved to Downloads ✅", Toast.LENGTH_LONG).show()
                            else
                                Toast.makeText(context, "Failed to save invoice ❌", Toast.LENGTH_SHORT).show()
                        }
                    }, isDownloading = isDownloading)
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(inv: InvoiceData, onDownload: () -> Unit, isDownloading: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Receipt card
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E143E), Color(0xFF140D2B))))
                .border(1.dp, Brush.linearGradient(listOf(Color(0xFFFFD700).copy(0.5f), Color(0xFF7C3AED).copy(0.3f))), RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column {
                // Header
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                    Column {
                        Text("🪙 TALKSY", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD700))
                        Text("Official Receipt", fontSize = 12.sp, color = Color.White.copy(0.55f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF065F46).copy(0.4f)) {
                            Text("✅ PAID", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(inv.invoiceNumber, fontSize = 10.sp, color = Color.White.copy(0.4f))
                    }
                }

                Spacer(Modifier.height(20.dp))
                Divider(color = Color.White.copy(0.1f))
                Spacer(Modifier.height(20.dp))

                // Buyer Info
                InvoiceSection("BILL TO")
                InvoiceRow("Name", inv.buyerName)
                if (inv.buyerEmail.isNotBlank()) InvoiceRow("Email", inv.buyerEmail)

                Spacer(Modifier.height(16.dp))
                Divider(color = Color.White.copy(0.08f))
                Spacer(Modifier.height(16.dp))

                // Item
                InvoiceSection("ITEM PURCHASED")
                InvoiceRow("Product", inv.productLabel)
                InvoiceRow("Coins Credited", "+${inv.coins} 🪙")
                InvoiceRow("Payment Method", inv.paymentMethod)

                Spacer(Modifier.height(16.dp))
                Divider(color = Color.White.copy(0.08f))
                Spacer(Modifier.height(16.dp))

                // Transaction Meta
                InvoiceSection("TRANSACTION DETAILS")
                InvoiceRow("Order ID", inv.orderId.take(20) + if (inv.orderId.length > 20) "…" else "")
                InvoiceRow("Date", formatDate(inv.issuedAt))
                InvoiceRow("Status", inv.status)

                Spacer(Modifier.height(20.dp))
                Divider(color = Color(0xFFFFD700).copy(0.3f))
                Spacer(Modifier.height(16.dp))

                // Total
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("TOTAL PAID", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(0.7f))
                    Text("₹${inv.amountInr}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD700))
                }

                Spacer(Modifier.height(20.dp))
                Divider(color = Color.White.copy(0.06f))
                Spacer(Modifier.height(12.dp))

                Text(
                    "Thank you for recharging! Coins are credited instantly.\n${inv.appName} v${inv.appVersion}",
                    fontSize = 11.sp,
                    color = Color.White.copy(0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Download Button
        Button(
            onClick = onDownload,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricIndigo),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            enabled = !isDownloading
        ) {
            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Saving...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Download Invoice PDF", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InvoiceSection(title: String) {
    Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = Color.White.copy(0.45f))
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun InvoiceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), Arrangement.SpaceBetween, Alignment.Top) {
        Text(label, fontSize = 13.sp, color = Color.White.copy(0.55f), modifier = Modifier.weight(0.45f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White,
            modifier = Modifier.weight(0.55f), textAlign = TextAlign.End)
    }
}

// ---------- PDF Generation ----------

fun generateAndSavePdf(context: Context, inv: InvoiceData, onResult: (Boolean) -> Unit) {
    try {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdf.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val bgPaint = Paint().apply { color = AndroidColor.parseColor("#0F0B1E"); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        fun textPaint(size: Float, bold: Boolean = false, alpha: Int = 255): Paint = Paint().apply {
            color = AndroidColor.WHITE
            this.alpha = alpha
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            isAntiAlias = true
        }

        fun goldPaint(size: Float, bold: Boolean = true): Paint = Paint().apply {
            color = AndroidColor.parseColor("#FFD700")
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            isAntiAlias = true
        }

        var y = 60f
        // App Name
        canvas.drawText("🪙 TALKSY", 40f, y, goldPaint(24f)); y += 10f
        canvas.drawText("Official Receipt", 40f, y + 16f, textPaint(11f, alpha = 140)); y += 40f
        // Invoice #
        canvas.drawText(inv.invoiceNumber, 40f, y, textPaint(10f, alpha = 100)); y += 30f
        // Divider
        val divPaint = Paint().apply { color = AndroidColor.WHITE; alpha = 25 }
        canvas.drawLine(40f, y, 555f, y, divPaint); y += 20f

        // PAID badge
        val badgePaint = Paint().apply { color = AndroidColor.parseColor("#065F46"); alpha = 180; style = Paint.Style.FILL }
        canvas.drawRoundRect(40f, y, 110f, y + 20f, 6f, 6f, badgePaint)
        canvas.drawText("✅ PAID", 46f, y + 14f, textPaint(10f, bold = true).apply { color = AndroidColor.parseColor("#34D399") })
        y += 36f

        fun section(title: String) { canvas.drawText(title, 40f, y, textPaint(9f, alpha = 100)); y += 18f }
        fun row(label: String, value: String) {
            canvas.drawText(label, 40f, y, textPaint(11f, alpha = 150))
            canvas.drawText(value, 320f, y, textPaint(11f, bold = true))
            y += 18f
        }

        section("BILL TO")
        row("Name", inv.buyerName)
        if (inv.buyerEmail.isNotBlank()) row("Email", inv.buyerEmail)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, divPaint); y += 16f

        section("ITEM PURCHASED")
        row("Product", inv.productLabel.take(38))
        row("Coins Credited", "+${inv.coins} Coins")
        row("Payment Method", inv.paymentMethod)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, divPaint); y += 16f

        section("TRANSACTION")
        row("Order ID", inv.orderId.take(32))
        row("Date", formatDate(inv.issuedAt))
        row("Status", inv.status)
        y += 10f
        canvas.drawLine(40f, y, 555f, y, Paint().apply { color = AndroidColor.parseColor("#FFD700"); alpha = 80 }); y += 18f

        // Total
        canvas.drawText("TOTAL PAID", 40f, y + 4f, textPaint(11f, alpha = 150))
        canvas.drawText("₹${inv.amountInr}", 450f, y + 4f, goldPaint(20f)); y += 36f

        canvas.drawLine(40f, y, 555f, y, divPaint); y += 20f
        canvas.drawText("Thank you for recharging on Talksy!", 40f, y, textPaint(10f, alpha = 80))
        y += 14f
        canvas.drawText("${inv.appName} v${inv.appVersion}", 40f, y, textPaint(9f, alpha = 60))

        pdf.finishPage(page)

        val fileName = "Talksy_Invoice_${inv.invoiceNumber}.pdf"
        var outputStream: OutputStream? = null
        var savedUri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            savedUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = savedUri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            @Suppress("DEPRECATION")
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            outputStream = FileOutputStream(file)
            savedUri = Uri.fromFile(file)
        }

        if (outputStream != null) {
            pdf.writeTo(outputStream)
            outputStream.close()
            pdf.close()
            // Open the file
            savedUri?.let { uri ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                try { context.startActivity(intent) } catch (_: Exception) { }
            }
            onResult(true)
        } else {
            pdf.close()
            onResult(false)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false)
    }
}
