package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.CartItem
import com.example.data.ExportedDocumentRecord
import com.example.data.PosRepository
import com.example.data.Product
import com.example.data.SaleItemRecord
import com.example.data.SaleRecord
import com.example.data.UserAccount
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportService(
    private val context: Context,
    private val repository: PosRepository
) {
    private val exportDir: File
        get() {
            val dir = File(context.filesDir, "exports")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * Loads the official DCM logo bitmap from application resources.
     */
    private fun loadLogoBitmap(): Bitmap? {
        return try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_donsael_logo)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.ic_donsael_logo)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a thermal receipt PDF tailored to the selected paper width.
     * Paper width is in mm (e.g., 58mm, 80mm).
     */
    suspend fun generateSaleReceipt(
        sale: SaleRecord,
        items: List<SaleItemRecord>,
        paperWidthMm: Int,
        currentUser: UserAccount
    ): File {
        // 1 mm = approx 2.8346 points
        val widthPoints = (paperWidthMm * 2.8346).toInt()
        val textMargin = (widthPoints * 0.05f).toInt()
        val printableWidth = widthPoints - (textMargin * 2)

        val logo = loadLogoBitmap()

        // Measure content height dynamically
        var estimatedHeight = if (logo != null) 580 else 520
        estimatedHeight += items.size * 32
        if (sale.paymentType == "CREDIT") estimatedHeight += 120

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(widthPoints, estimatedHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        // Paints
        val pTitle = Paint().apply {
            color = Color.rgb(0, 74, 173) // DCM Royal Blue
            textSize = if (paperWidthMm <= 58) 10f else 12f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val pSubtitle = Paint().apply {
            color = Color.DKGRAY
            textSize = if (paperWidthMm <= 58) 7f else 8.5f
            textAlign = Paint.Align.CENTER
        }
        val pText = Paint().apply {
            color = Color.BLACK
            textSize = if (paperWidthMm <= 58) 7.5f else 9f
        }
        val pBold = Paint().apply {
            color = Color.BLACK
            textSize = if (paperWidthMm <= 58) 8f else 9.5f
            isFakeBoldText = true
        }
        val pRight = Paint().apply {
            color = Color.BLACK
            textSize = if (paperWidthMm <= 58) 7.5f else 9f
            textAlign = Paint.Align.RIGHT
        }
        val pRightBold = Paint().apply {
            color = Color.BLACK
            textSize = if (paperWidthMm <= 58) 8f else 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        val pLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val pDcmLine = Paint().apply {
            color = Color.rgb(0, 74, 173) // DCM Accent line
            strokeWidth = 1.5f
        }

        var y = 14f
        val centerX = widthPoints / 2f
        val rightX = widthPoints - textMargin.toFloat()
        val leftX = textMargin.toFloat()

        // Draw DCM Logo on Receipt
        if (logo != null) {
            val logoW = printableWidth * 0.70f
            val ratio = logo.height.toFloat() / logo.width.toFloat()
            val logoH = (logoW * ratio).coerceAtMost(55f)
            val logoX = (widthPoints - logoW) / 2f
            canvas.drawBitmap(logo, null, RectF(logoX, y, logoX + logoW, y + logoH), null)
            y += logoH + 8f
        }

        // Header: DONSAEL
        canvas.drawText("DONSAEL COMMUNICATION", centerX, y, pTitle)
        y += 13f
        canvas.drawText("& MULTI-SERVICES", centerX, y, pTitle)
        y += 12f
        canvas.drawText("Vente d'Appareils Électroménagers & Divers", centerX, y, pSubtitle)
        y += 10f
        canvas.drawText("Belle-Vue, L'Asile, Nippes, Haïti", centerX, y, pSubtitle)
        y += 10f
        canvas.drawText("Tél: 3753-2670 / 3224-4430", centerX, y, pSubtitle)
        y += 10f
        canvas.drawText("Email: donsaelmondelice08@gmail.com", centerX, y, pSubtitle)
        y += 12f

        canvas.drawLine(leftX, y, rightX, y, pDcmLine)
        y += 12f

        // Sale info
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date(sale.timestamp))
        canvas.drawText("Réf: ${sale.reference}", leftX, y, pBold)
        y += 11f
        canvas.drawText("Date: $dateStr", leftX, y, pText)
        y += 11f
        canvas.drawText("Opérateur: ${sale.sellerFullName}", leftX, y, pText)
        y += 11f
        canvas.drawText("Type: ${if (sale.paymentType == "CREDIT") "VENTE À CRÉDIT" else "VENTE AU COMPTANT"}", leftX, y, pBold)
        y += 11f
        canvas.drawText("Taux USD/HTG: 1 USD = ${sale.exchangeRateApplied} HTG", leftX, y, pText)
        y += 12f

        if (!sale.customerName.isNullOrBlank()) {
            canvas.drawText("Client: ${sale.customerName}", leftX, y, pBold)
            y += 10f
            if (!sale.customerPhone.isNullOrBlank()) {
                canvas.drawText("Tél 1: ${sale.customerPhone}", leftX, y, pText)
                y += 10f
            }
            if (!sale.customerPhone2.isNullOrBlank()) {
                canvas.drawText("Tél 2: ${sale.customerPhone2}", leftX, y, pText)
                y += 10f
            }
            if (!sale.customerAddress.isNullOrBlank()) {
                canvas.drawText("Adresse: ${sale.customerAddress}", leftX, y, pText)
                y += 10f
            }
        }

        canvas.drawLine(leftX, y, rightX, y, pLine)
        y += 12f

        // Table header
        canvas.drawText("ARTICLE", leftX, y, pBold)
        canvas.drawText("TOTAL", rightX, y, pRightBold)
        y += 12f

        // Items
        for (item in items) {
            val nameDisplay = if (item.productName.length > 22) item.productName.take(20) + ".." else item.productName
            canvas.drawText("${item.quantity}x $nameDisplay", leftX, y, pText)
            val lineTotal = item.quantity * item.unitPriceConverted
            canvas.drawText(String.format(Locale.US, "%.2f %s", lineTotal, sale.paymentCurrency), rightX, y, pRight)
            y += 10f
            canvas.drawText(String.format(Locale.US, "  @ %.2f %s", item.unitPriceConverted, sale.paymentCurrency), leftX, y, pSubtitle)
            y += 11f
        }

        canvas.drawLine(leftX, y, rightX, y, pLine)
        y += 13f

        // Totals
        canvas.drawText("TOTAL:", leftX, y, pBold)
        canvas.drawText(String.format(Locale.US, "%.2f %s", sale.totalAmount, sale.paymentCurrency), rightX, y, pRightBold)
        y += 13f

        if (sale.paymentType == "COMPTANT") {
            canvas.drawText("Mode paiement:", leftX, y, pText)
            canvas.drawText(sale.paymentMethod, rightX, y, pRight)
            y += 11f
            canvas.drawText("Montant remis:", leftX, y, pText)
            canvas.drawText(String.format(Locale.US, "%.2f %s", sale.amountTendered, sale.paymentCurrency), rightX, y, pRight)
            y += 11f
            canvas.drawText("Monnaie à rendre:", leftX, y, pBold)
            canvas.drawText(String.format(Locale.US, "%.2f %s", sale.changeGiven, sale.paymentCurrency), rightX, y, pRightBold)
            y += 13f
        } else {
            // Credit
            canvas.drawText("Acompte versé:", leftX, y, pText)
            canvas.drawText(String.format(Locale.US, "%.2f %s (%s)", sale.creditDepositAmount, sale.paymentCurrency, sale.creditDepositMethod ?: "Espèces"), rightX, y, pRight)
            y += 11f
            canvas.drawText("Solde restant:", leftX, y, pBold)
            canvas.drawText(String.format(Locale.US, "%.2f %s", sale.creditRemainingBalance, sale.paymentCurrency), rightX, y, pRightBold)
            y += 11f
            if (sale.creditDueDate != null && sale.creditDueDate > 0) {
                val dueStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(sale.creditDueDate))
                canvas.drawText("Échéance prévue:", leftX, y, pText)
                canvas.drawText(dueStr, rightX, y, pRight)
                y += 11f
            }
            canvas.drawText("Statut crédit:", leftX, y, pBold)
            val stTxt = when (sale.creditStatus) {
                "PAID" -> "DÉJÀ PAYÉ"
                "PARTIAL" -> "ACOMPTE VERSÉ"
                else -> "NON PAYÉ"
            }
            canvas.drawText(stTxt, rightX, y, pRightBold)
            y += 13f
        }

        canvas.drawLine(leftX, y, rightX, y, pLine)
        y += 14f

        // Footer: NexGen Spark (MANDATORY REQUIREMENT)
        val pFooterTitle = Paint().apply {
            color = Color.DKGRAY
            textSize = if (paperWidthMm <= 58) 6.5f else 7.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val pFooterText = Paint().apply {
            color = Color.GRAY
            textSize = if (paperWidthMm <= 58) 5.5f else 6.5f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("Merci de votre fidélité !", centerX, y, pFooterTitle)
        y += 11f
        canvas.drawText("Conçu et développé par NexGen Spark", centerX, y, pFooterTitle)
        y += 9f
        canvas.drawText("Belle-Vue, L'Asile, Nippes, Haïti", centerX, y, pFooterText)
        y += 8f
        canvas.drawText("support@nexgns.net | (+509) 42 00-0315 / 36 44-4675", centerX, y, pFooterText)
        y += 8f
        canvas.drawText("© 2026 NexGen Spark – Tous droits réservés", centerX, y, pFooterText)

        doc.finishPage(page)

        val file = File(exportDir, "Ticket_${sale.reference}.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        // Register document
        repository.insertDocument(
            ExportedDocumentRecord(
                fileName = file.name,
                docType = "TICKET_VENTE",
                userName = currentUser.fullName,
                filePath = file.absolutePath,
                summary = "Ticket de caisse pour la vente ${sale.reference} (${sale.totalAmount} ${sale.paymentCurrency})"
            )
        )

        return file
    }

    /**
     * Generates standard A4 Inventory PDF export.
     */
    suspend fun generateInventoryPdf(
        products: List<Product>,
        exchangeRate: Double,
        currentUser: UserAccount
    ): File {
        val doc = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val margin = 36f

        val logo = loadLogoBitmap()

        val pTitle = Paint().apply {
            color = Color.rgb(0, 74, 173) // DCM Royal Blue
            textSize = 13f
            isFakeBoldText = true
        }
        val pSubtitle = Paint().apply {
            color = Color.DKGRAY
            textSize = 8.5f
        }
        val pAccentBlue = Paint().apply {
            color = Color.rgb(0, 74, 173)
            strokeWidth = 2f
        }
        val pAccentCyan = Paint().apply {
            color = Color.rgb(0, 210, 255)
            strokeWidth = 1.2f
        }
        val pHeaderBg = Paint().apply {
            color = Color.rgb(238, 246, 255) // Ice blue header row background
            style = Paint.Style.FILL
        }
        val pHeader = Paint().apply {
            color = Color.rgb(0, 41, 102) // DCM Navy
            textSize = 8.5f
            isFakeBoldText = true
        }
        val pText = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
        }
        val pAlert = Paint().apply {
            color = Color.RED
            textSize = 8f
            isFakeBoldText = true
        }
        val pLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.8f
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            val textStartX: Float
            if (logo != null) {
                val logoW = 90f
                val ratio = logo.height.toFloat() / logo.width.toFloat()
                val logoH = (logoW * ratio).coerceAtMost(46f)
                c.drawBitmap(logo, null, RectF(margin, 20f, margin + logoW, 20f + logoH), null)
                textStartX = margin + logoW + 14f
            } else {
                textStartX = margin
            }

            // Header
            c.drawText("DONSAEL COMMUNICATION ET MULTI-SERVICES", textStartX, 34f, pTitle)
            c.drawText("RAPPORT D'INVENTAIRE OFFICIEL DES STOCKS", textStartX, 47f, pSubtitle)
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            c.drawText("Généré le: $dateStr | Utilisateur: ${currentUser.fullName} | Taux: 1 USD = $exchangeRate HTG", textStartX, 59f, pSubtitle)

            // Dual accent lines in DCM colors
            c.drawLine(margin, 68f, pageWidth - margin, 68f, pAccentBlue)
            c.drawLine(margin, 71f, pageWidth - margin, 71f, pAccentCyan)

            // Table headers with ice-blue background fill
            val yTh = 86f
            c.drawRect(RectF(margin, yTh - 10f, pageWidth - margin, yTh + 6f), pHeaderBg)
            c.drawText("PRODUIT", margin + 6f, yTh, pHeader)
            c.drawText("CATÉGORIE", margin + 170, yTh, pHeader)
            c.drawText("QTÉ", margin + 280, yTh, pHeader)
            c.drawText("PRIX ACHAT", margin + 315, yTh, pHeader)
            c.drawText("PRIX VENTE", margin + 385, yTh, pHeader)
            c.drawText("STATUT", margin + 455, yTh, pHeader)
            c.drawLine(margin, yTh + 6f, pageWidth - margin, yTh + 6f, pLine)

            // Mandatory NexGen Spark Footer on every page
            val yFoot = pageHeight - 32f
            c.drawLine(margin, yFoot - 8f, pageWidth - margin, yFoot - 8f, pLine)
            val pFoot = Paint().apply {
                color = Color.GRAY
                textSize = 7f
                textAlign = Paint.Align.CENTER
            }
            c.drawText("Système conçu et développé par NexGen Spark | Belle-Vue, L'Asile, Nippes, Haïti | support@nexgns.net | (+509) 42 00-0315 / 36 44-4675", pageWidth / 2f, yFoot, pFoot)
            c.drawText("© 2026 NexGen Spark – Tous droits réservés. Page $pNum", pageWidth / 2f, yFoot + 9f, pFoot)
        }

        drawHeaderAndFooter(canvas, pageNum)

        var y = 104f
        var totalStockHtg = 0.0
        var totalStockUsd = 0.0

        for (prod in products) {
            if (y > pageHeight - 65f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                drawHeaderAndFooter(canvas, pageNum)
                y = 104f
            }

            // Calc stock values
            val valHtg = if (prod.currency == "HTG") prod.sellingPrice * prod.quantity else prod.sellingPrice * prod.quantity * exchangeRate
            val valUsd = if (prod.currency == "USD") prod.sellingPrice * prod.quantity else (prod.sellingPrice * prod.quantity) / exchangeRate
            totalStockHtg += valHtg
            totalStockUsd += valUsd

            val isLow = prod.quantity <= prod.alertThreshold
            val nameDisplay = if (prod.name.length > 32) prod.name.take(30) + ".." else prod.name
            val catDisplay = if (prod.category.length > 20) prod.category.take(18) + ".." else prod.category

            canvas.drawText(nameDisplay, margin + 6f, y, if (isLow) pAlert else pText)
            canvas.drawText(catDisplay, margin + 170, y, pText)
            canvas.drawText("${prod.quantity}", margin + 280, y, if (isLow) pAlert else pText)
            canvas.drawText("${prod.purchasePrice} ${prod.currency}", margin + 315, y, pText)
            canvas.drawText("${prod.sellingPrice} ${prod.currency}", margin + 385, y, pText)

            val statusStr = if (isLow) "ALERTE (<=4)" else "OK"
            canvas.drawText(statusStr, margin + 455, y, if (isLow) pAlert else pText)

            y += 14f
        }

        // Summary box
        if (y > pageHeight - 120f) {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            drawHeaderAndFooter(canvas, pageNum)
            y = 104f
        }

        y += 10f
        canvas.drawLine(margin, y, pageWidth - margin, y, pAccentBlue)
        y += 16f
        canvas.drawText("RÉCAPITULATIF DE LA VALEUR DU STOCK:", margin, y, pHeader)
        y += 13f
        canvas.drawText(String.format(Locale.US, "• Valeur Totale Estimée en Gourdes (HTG): %.2f HTG", totalStockHtg), margin + 10, y, pText)
        y += 11f
        canvas.drawText(String.format(Locale.US, "• Valeur Totale Estimée en Dollars (USD): %.2f USD", totalStockUsd), margin + 10, y, pText)
        y += 11f
        canvas.drawText("• Total Références en Inventaire: ${products.size} articles", margin + 10, y, pText)

        doc.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "Inventaire_DONSAEL_$timestamp.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        repository.insertDocument(
            ExportedDocumentRecord(
                fileName = file.name,
                docType = "INVENTAIRE_PDF",
                userName = currentUser.fullName,
                filePath = file.absolutePath,
                summary = "Export inventaire (${products.size} produits) - Valeur stock: ${String.format(Locale.US, "%.2f HTG", totalStockHtg)}"
            )
        )

        return file
    }

    /**
     * Generates A4 Financial Performance PDF export.
     */
    suspend fun generateFinancialReportPdf(
        sales: List<SaleRecord>,
        periodLabel: String,
        currencyFilter: String,
        exchangeRate: Double,
        currentUser: UserAccount
    ): File {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val logo = loadLogoBitmap()

        val pTitle = Paint().apply {
            color = Color.rgb(0, 74, 173) // DCM Royal Blue
            textSize = 13f
            isFakeBoldText = true
        }
        val pSubtitle = Paint().apply {
            color = Color.DKGRAY
            textSize = 8.5f
        }
        val pAccentBlue = Paint().apply {
            color = Color.rgb(0, 74, 173)
            strokeWidth = 2f
        }
        val pAccentCyan = Paint().apply {
            color = Color.rgb(0, 210, 255)
            strokeWidth = 1.2f
        }
        val pHeaderBg = Paint().apply {
            color = Color.rgb(238, 246, 255)
            style = Paint.Style.FILL
        }
        val pHeader = Paint().apply {
            color = Color.rgb(0, 41, 102)
            textSize = 8.5f
            isFakeBoldText = true
        }
        val pText = Paint().apply {
            color = Color.DKGRAY
            textSize = 8f
        }
        val pLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.8f
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        fun drawHeaderAndFooter(c: Canvas, pNum: Int) {
            val textStartX: Float
            if (logo != null) {
                val logoW = 90f
                val ratio = logo.height.toFloat() / logo.width.toFloat()
                val logoH = (logoW * ratio).coerceAtMost(46f)
                c.drawBitmap(logo, null, RectF(margin, 20f, margin + logoW, 20f + logoH), null)
                textStartX = margin + logoW + 14f
            } else {
                textStartX = margin
            }

            c.drawText("DONSAEL COMMUNICATION ET MULTI-SERVICES", textStartX, 34f, pTitle)
            c.drawText("RAPPORT DES PERFORMANCES COMMERCIALES & FINANCIÈRES", textStartX, 47f, pSubtitle)
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            c.drawText("Période: $periodLabel | Généré le: $dateStr par ${currentUser.fullName}", textStartX, 59f, pSubtitle)

            // Dual accent lines in DCM colors
            c.drawLine(margin, 68f, pageWidth - margin, 68f, pAccentBlue)
            c.drawLine(margin, 71f, pageWidth - margin, 71f, pAccentCyan)

            val yFoot = pageHeight - 32f
            c.drawLine(margin, yFoot - 8f, pageWidth - margin, yFoot - 8f, pLine)
            val pFoot = Paint().apply {
                color = Color.GRAY
                textSize = 7f
                textAlign = Paint.Align.CENTER
            }
            c.drawText("Système conçu et développé par NexGen Spark | Belle-Vue, L'Asile, Nippes, Haïti | support@nexgns.net | (+509) 42 00-0315 / 36 44-4675", pageWidth / 2f, yFoot, pFoot)
            c.drawText("© 2026 NexGen Spark – Tous droits réservés. Page $pNum", pageWidth / 2f, yFoot + 9f, pFoot)
        }

        drawHeaderAndFooter(canvas, pageNum)

        // Filtered finalized sales: Comptant + fully paid credit
        val finalizedSales = sales.filter { it.paymentType == "COMPTANT" || it.creditStatus == "PAID" }
        var totalRevHtg = 0.0
        var totalCostHtg = 0.0

        for (s in finalizedSales) {
            val revInHtg = if (s.paymentCurrency == "HTG") s.totalAmount else s.totalAmount * exchangeRate
            val costInHtg = if (s.paymentCurrency == "HTG") s.purchaseCostTotal else s.purchaseCostTotal * exchangeRate
            totalRevHtg += revInHtg
            totalCostHtg += costInHtg
        }
        val totalProfitHtg = totalRevHtg - totalCostHtg
        val avgBasketHtg = if (finalizedSales.isNotEmpty()) totalRevHtg / finalizedSales.size else 0.0

        var y = 92f
        // Summary KPI Box
        canvas.drawRect(RectF(margin, y - 10f, pageWidth - margin, y + 80f), pHeaderBg)
        canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, pAccentBlue)

        canvas.drawText("SYNTHÈSE FINANCIÈRE DE LA PÉRIODE:", margin + 8f, y, pHeader)
        y += 14f
        canvas.drawText(String.format(Locale.US, "• Chiffre d'affaires encaissé: %.2f HTG (équiv. %.2f USD)", totalRevHtg, totalRevHtg / exchangeRate), margin + 14f, y, pText)
        y += 12f
        canvas.drawText(String.format(Locale.US, "• Coût total d'achat: %.2f HTG (équiv. %.2f USD)", totalCostHtg, totalCostHtg / exchangeRate), margin + 14f, y, pText)
        y += 12f
        canvas.drawText(String.format(Locale.US, "• Bénéfice net réalisé: %.2f HTG (équiv. %.2f USD)", totalProfitHtg, totalProfitHtg / exchangeRate), margin + 14f, y, pHeader)
        y += 12f
        canvas.drawText(String.format(Locale.US, "• Panier moyen: %.2f HTG", avgBasketHtg), margin + 14f, y, pText)
        y += 12f
        canvas.drawText("• Nombre total de ventes finalisées: ${finalizedSales.size}", margin + 14f, y, pText)
        y += 24f

        // Table of sales with Ice Blue header
        canvas.drawRect(RectF(margin, y - 10f, pageWidth - margin, y + 6f), pHeaderBg)
        canvas.drawText("RÉFÉRENCE", margin + 6f, y, pHeader)
        canvas.drawText("DATE & HEURE", margin + 100, y, pHeader)
        canvas.drawText("TYPE", margin + 210, y, pHeader)
        canvas.drawText("MONTANT", margin + 280, y, pHeader)
        canvas.drawText("OPÉRATEUR", margin + 370, y, pHeader)
        canvas.drawText("CLIENT", margin + 460, y, pHeader)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, pLine)
        y += 14f

        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        for (s in finalizedSales) {
            if (y > pageHeight - 65f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                canvas = page.canvas
                drawHeaderAndFooter(canvas, pageNum)
                y = 96f
            }

            canvas.drawText(s.reference, margin + 6f, y, pText)
            canvas.drawText(sdf.format(Date(s.timestamp)), margin + 100, y, pText)
            canvas.drawText(s.paymentType, margin + 210, y, pText)
            canvas.drawText(String.format(Locale.US, "%.2f %s", s.totalAmount, s.paymentCurrency), margin + 280, y, pText)
            canvas.drawText(s.sellerFullName.take(15), margin + 370, y, pText)
            canvas.drawText((s.customerName ?: "-").take(15), margin + 460, y, pText)

            y += 13f
        }

        doc.finishPage(page)

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(exportDir, "Rapport_Financier_$timestamp.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        repository.insertDocument(
            ExportedDocumentRecord(
                fileName = file.name,
                docType = "RAPPORT_FINANCIER_PDF",
                userName = currentUser.fullName,
                filePath = file.absolutePath,
                summary = "Rapport financier ($periodLabel) - CA: ${String.format(Locale.US, "%.2f HTG", totalRevHtg)}, Bénéfice: ${String.format(Locale.US, "%.2f HTG", totalProfitHtg)}"
            )
        )

        return file
    }

    /**
     * Helper to open or share a generated PDF file via Android intent.
     */
    fun openOrSharePdf(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Ouvrir ou imprimer le document PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager le PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
