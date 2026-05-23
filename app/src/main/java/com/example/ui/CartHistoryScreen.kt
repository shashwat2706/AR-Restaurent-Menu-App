package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CartItem
import com.example.data.SavedCapture
import com.example.ui.theme.*
import com.example.viewmodel.MenuViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CartHistoryScreen(
    viewModel: MenuViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cartItems by viewModel.cartItems.collectAsState()
    val rawTotalPrice by viewModel.totalCartPrice.collectAsState()
    val allCaptures by viewModel.allCaptures.collectAsState()

    var showOrderReceiptDialog by remember { mutableStateOf(false) }
    var activeSubTab by remember { mutableStateOf("Cart") } // "Cart" or "Gallery"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateGreyDark)
    ) {
        // Toggle Switch Top header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SlateGreyDark, SlateMediumSurface.copy(alpha = 0.5f))
                    )
                )
                .padding(vertical = 12.dp)
        ) {
            TabRow(
                selectedTabIndex = if (activeSubTab == "Cart") 0 else 1,
                containerColor = Color.Transparent,
                contentColor = ClayPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[if (activeSubTab == "Cart") 0 else 1]),
                        color = ClayPrimary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .align(Alignment.Center)
                    .testTag("checkout_history_tabs")
            ) {
                Tab(
                    selected = activeSubTab == "Cart",
                    onClick = { activeSubTab = "Cart" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Order list", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Basket (${cartItems.sumOf { it.quantity }})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ClayPrimary,
                    unselectedContentColor = TextMutedSlate
                )
                Tab(
                    selected = activeSubTab == "Gallery",
                    onClick = { activeSubTab = "Gallery" },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "AR captures", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Captures (${allCaptures.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ClayPrimary,
                    unselectedContentColor = TextMutedSlate
                )
            }
        }

        // Subtabs routing
        if (activeSubTab == "Cart") {
            // Cart Section
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Empty Basket",
                            tint = TextMutedSlate,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Your Basket is Empty",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BoneWhite
                        )
                        Text(
                            text = "Check our Gourmet selection, launch the AR lens to experience portion sizes, then place orders directly from active lens.",
                            fontSize = 12.sp,
                            color = TextMutedSlate,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .testTag("cart_items_list"),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cartItems, key = { it.dish.id }) { item ->
                            CartItemRow(
                                item = item,
                                onAdd = { viewModel.updateCartQuantity(item.dish, item.quantity + 1) },
                                onMinus = { viewModel.updateCartQuantity(item.dish, item.quantity - 1) },
                                onDelete = { viewModel.removeFromCart(item.dish) }
                            )
                        }
                    }

                    // Bottom Order Invoice pricing panel
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(20.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        colors = CardDefaults.cardColors(containerColor = SlateMediumSurface),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(1.dp, SlateBorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Order Estimation",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = ClayPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("A la Carte Subtotal", color = TextMutedSlate, fontSize = 13.sp)
                                Text("$${String.format("%.2f", rawTotalPrice)}", color = BoneWhite, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Chef's Service Tax (10%)", color = TextMutedSlate, fontSize = 13.sp)
                                Text("$${String.format("%.2f", rawTotalPrice * 0.10)}", color = BoneWhite, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Table Setup Fee", color = TextMutedSlate, fontSize = 13.sp)
                                Text("FREE in AR", color = SageGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = SlateBorderLight)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Gross Grand Total", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("$${String.format("%.2f", rawTotalPrice * 1.10)}", color = WarmGoldAccent, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // checkout action button
                            Button(
                                onClick = { showOrderReceiptDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("checkout_order_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ClayPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payment, contentDescription = "Checkout", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pay & Place Chef Order", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Gallery / Saved Snapshot Moments from AR Section
            if (allCaptures.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Empty Gallery",
                            tint = TextMutedSlate,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Captured Moments Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BoneWhite
                        )
                        Text(
                            text = "Launch AR, align custom plates, and tap the white camera shutter to capture breathtaking snapshots of high-fidelity dining scenes.",
                            fontSize = 12.sp,
                            color = TextMutedSlate,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .testTag("ar_captures_grid"),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(allCaptures) { capture ->
                        CaptureMomentsCard(
                            capture = capture,
                            onRemoveClick = {
                                viewModel.deleteCapture(capture.id)
                                Toast.makeText(context, "Snapshot deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Success Invoice Order Receipt Dialog
    if (showOrderReceiptDialog) {
        val totalAmount = rawTotalPrice * 1.10
        AlertDialog(
            onDismissRequest = {
                viewModel.clearCart()
                showOrderReceiptDialog = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCart()
                        showOrderReceiptDialog = false
                        Toast.makeText(context, "Order sent successfully!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClayPrimary)
                ) {
                    Text("Pay & Print Receipt", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOrderReceiptDialog = false }) {
                    Text("Cancel", color = TextMutedSlate)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = "Reciept", tint = SageGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gourmet Receipt Invoice", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BoneWhite)
                }
            },
            text = {
                Column {
                    Text("Chef Kitchen: Ready to Serve!", color = SageGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    cartItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${item.quantity}x ${item.dish.name}", fontSize = 12.sp, color = BoneWhite, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("$${String.format("%.2f", item.dish.price * item.quantity)}", fontSize = 12.sp, color = BoneWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp), color = SlateBorderLight)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 12.sp, color = TextMutedSlate)
                        Text("$${String.format("%.2f", rawTotalPrice)}", fontSize = 12.sp, color = BoneWhite)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tax & Service (10%)", fontSize = 12.sp, color = TextMutedSlate)
                        Text("$${String.format("%.2f", rawTotalPrice * 0.10)}", fontSize = 12.sp, color = BoneWhite)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount Paid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BoneWhite)
                        Text("$${String.format("%.2f", totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = WarmGoldAccent)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Transaction ID: TXN-${UUID.randomUUID().toString().uppercase().take(10)}",
                        fontSize = 9.sp,
                        color = TextMutedSlate
                    )
                }
            },
            containerColor = SlateMediumSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onAdd: () -> Unit,
    onMinus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateMediumSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Little miniature drawing model representing contents
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SlateGreyDark),
                contentAlignment = Alignment.Center
            ) {
                Food3DModelVisualizer(
                    dish = item.dish,
                    scale = 0.28f,
                    rotationX = 15f,
                    rotationY = 0f,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Label Titles
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.dish.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BoneWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$${String.format("%.2f", item.dish.price)} per portion",
                    fontSize = 11.sp,
                    color = TextMutedSlate
                )
            }

            // Price Item total
            Text(
                text = "$${String.format("%.2f", item.dish.price * item.quantity)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = WarmGoldAccent,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Quantity adjust counters
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(26.dp)
                        .background(SlateGreyDark, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Reduce", tint = BoneWhite, modifier = Modifier.size(14.dp))
                }

                Text(
                    text = item.quantity.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BoneWhite,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .size(26.dp)
                        .background(SlateGreyDark, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = BoneWhite, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/**
 * Historical Moments captured by AR cameras. Since physical screenshot capture uses complex bitmap
 * utilities, we simulate dynamic vector snapshots of plates inside framed Polaroid plates!
 */
@Composable
fun CaptureMomentsCard(
    capture: SavedCapture,
    onRemoveClick: () -> Unit
) {
    val date = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(capture.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .border(1.dp, SlateBorderLight, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateMediumSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // Simulated snapshot: Dark container with nice plate vectors inside representing captured dish
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(SlateBorderLight, SlateGreyDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Renders snapshot lines and flash elements
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = drawContext.size.width
                    val h = drawContext.size.height
                    // Draw digital scanner framing guides
                    drawCircle(
                        color = Color(0x2210B981),
                        radius = w * 0.35f
                    )
                    // Draw outer border guides representation
                    drawRoundRect(
                        color = Color(0x3310B981),
                        size = drawContext.size,
                        style = Stroke(width = 2f)
                    )
                }

                // Visual anchor icon representing photo camera target
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Photo captured",
                    tint = TextMutedSlate.copy(alpha = 0.25f),
                    modifier = Modifier.size(54.dp)
                )

                // Flash badge info
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color(0xFF10B981), CircleShape)
                        .size(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Label detailing capture
            Text(
                text = capture.dishName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = BoneWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = date,
                fontSize = 9.sp,
                color = TextMutedSlate,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // Option to delete
            Button(
                onClick = onRemoveClick,
                colors = ButtonDefaults.buttonColors(containerColor = SlateGreyDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete snapshot", tint = WarmRedMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 10.sp, color = WarmRedMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
