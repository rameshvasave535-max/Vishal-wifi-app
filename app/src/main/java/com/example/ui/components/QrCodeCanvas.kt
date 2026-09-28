package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.random.Random

@Composable
fun QrCodeView(
    qrImageUrl: String?,
    seedString: String = "9545362903@upi",
    modifier: Modifier = Modifier.size(170.dp)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!qrImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = qrImageUrl,
                contentDescription = "UPI Payment QR Code",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // High fidelity procedural QR Code generator canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasSize = size.minDimension
                val moduleCount = 21 // Standard Version 1 QR code grid
                val cellSize = canvasSize / moduleCount

                // Function to draw finder pattern
                fun drawFinderPattern(xIdx: Int, yIdx: Int) {
                    val x = xIdx * cellSize
                    val y = yIdx * cellSize
                    val size7 = 7 * cellSize

                    // Outer black square
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(x, y),
                        size = Size(size7, size7),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Inner white border
                    val size5 = 5 * cellSize
                    val pad1 = cellSize
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x + pad1, y + pad1),
                        size = Size(size5, size5),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Center black core
                    val size3 = 3 * cellSize
                    val pad2 = 2 * cellSize
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(x + pad2, y + pad2),
                        size = Size(size3, size3),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // 3 Finder patterns
                drawFinderPattern(0, 0)
                drawFinderPattern(moduleCount - 7, 0)
                drawFinderPattern(0, moduleCount - 7)

                // Procedural QR data matrix based on seed
                val rng = Random(seedString.hashCode())
                for (row in 0 until moduleCount) {
                    for (col in 0 until moduleCount) {
                        // Skip finder patterns areas (7x7 corners)
                        val inTopLeft = row < 8 && col < 8
                        val inTopRight = row < 8 && col >= moduleCount - 8
                        val inBottomLeft = row >= moduleCount - 8 && col < 8
                        if (inTopLeft || inTopRight || inBottomLeft) continue

                        // Timing lines
                        if (row == 6 || col == 6) {
                            if ((row + col) % 2 == 0) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(col * cellSize, row * cellSize),
                                    size = Size(cellSize * 0.95f, cellSize * 0.95f)
                                )
                            }
                            continue
                        }

                        // Data modules
                        if (rng.nextBoolean()) {
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(col * cellSize + (cellSize * 0.05f), row * cellSize + (cellSize * 0.05f)),
                                size = Size(cellSize * 0.9f, cellSize * 0.9f),
                                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
