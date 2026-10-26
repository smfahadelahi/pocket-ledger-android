package com.fahad.pocketledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.fahad.pocketledger.data.AppDatabase
import com.fahad.pocketledger.data.TransactionDao
import com.fahad.pocketledger.data.TransactionEntity
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val Green = Color(0xFF087F5B)
private val Ink = Color(0xFF172B4D)
private val Canvas = Color(0xFFF4F7F8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.get(this).transactionDao()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Green, background = Canvas)) {
                HomeScreen(
                    dao = dao,
                    add = { amount, type, category, note, currency ->
                        lifecycleScope.launch {
                            dao.insert(TransactionEntity(amountMinor = amount, type = type, category = category, note = note, currency = currency))
                        }
                    },
                    delete = { item -> lifecycleScope.launch { dao.delete(item) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    dao: TransactionDao,
    add: (Long, String, String, String, String) -> Unit,
    delete: (TransactionEntity) -> Unit
) {
    val rows by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Dining") }
    var note by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("KRW") }
    var pendingDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Totals are deliberately scoped to one currency; exchange rates are not guessed.
    val selected = rows.filter { it.currency == currency }
    val income = selected.filter { it.type == "INCOME" }.sumOf { it.amountMinor }
    val expense = selected.filter { it.type == "EXPENSE" }.sumOf { it.amountMinor }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pocket Ledger", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Your money, made clearer", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    }
                },
                actions = { TextButton(onClick = { showAdd = true }) { Text("+ Add") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = Green, contentColor = Color.White) { Text("+") }
        },
        bottomBar = {
            NavigationBar {
                listOf("Home", "History", "Budgets", "Goals", "Settings").forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = i == 0,
                        onClick = { /* Other destinations are not implemented in this MVP yet. */ },
                        icon = { Text(listOf("⌂", "≡", "◷", "☆", "⚙")[i]) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().background(Canvas).padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TOTAL BALANCE", color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.weight(1f))
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) { Text("$currency ▾", color = Color.White) }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("KRW", "BDT", "USD").forEach { code ->
                                        DropdownMenuItem(text = { Text(code) }, onClick = { currency = code; expanded = false })
                                    }
                                }
                            }
                        }
                        Text(formatAmount(income - expense, currency), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Metric("Income", formatAmount(income, currency), Color(0xFFE1F6EA), Modifier.weight(1f))
                            Metric("Expenses", formatAmount(expense, currency), Color(0xFFFFE8E8), Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                Text("Quick actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { type = "INCOME"; showAdd = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1F6EA), contentColor = Ink)) { Text("+ Income") }
                    Button(onClick = { type = "EXPENSE"; showAdd = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE8E8), contentColor = Ink)) { Text("− Expense") }
                }
            }
            item { Text("Recent transactions · $currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink) }
            if (selected.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Your money story starts here", fontWeight = FontWeight.Bold)
                            Text("Add your first income or expense to see your dashboard.", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(selected.take(50), key = { it.id }) { row ->
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(row.category, fontWeight = FontWeight.SemiBold)
                                Text(row.note.ifBlank { row.type.lowercase().replaceFirstChar { it.uppercase() } }, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                (if (row.type == "INCOME") "+" else "−") + formatAmount(row.amountMinor, row.currency),
                                color = if (row.type == "INCOME") Green else Color(0xFFD94B4B),
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { pendingDelete = row }) { Text("Delete") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("Income") })
                        FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("Expense") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("KRW", "BDT", "USD").forEach { code ->
                            FilterChip(
                                selected = currency == code,
                                onClick = { currency = code; amount = normalizeAmountInput(amount, code) },
                                label = { Text(code) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = normalizeAmountInput(it, currency) },
                        label = { Text("Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(value = category, onValueChange = { category = it.take(60) }, label = { Text("Category") }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it.take(160) }, label = { Text("Note (optional)") }, singleLine = true)
                    Text(
                        if (currency == "KRW") "KRW uses whole won. BDT and USD support two decimal places."
                        else "Enter an amount with up to two decimal places.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val value = amount.toBigDecimalOrNull()
                    if (value != null && value > BigDecimal.ZERO) {
                        runCatching {
                            value.movePointRight(currencyScale(currency)).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
                        }.getOrNull()?.let { minorUnits ->
                            add(minorUnits, type, category.trim().ifBlank { "Other" }, note.trim(), currency)
                            amount = ""
                            note = ""
                            showAdd = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("This permanently deletes " + item.category + " (" + formatAmount(item.amountMinor, item.currency) + ").") },
            confirmButton = {
                Button(
                    onClick = { delete(item); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD94B4B))
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun Metric(label: String, value: String, background: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(background, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label, color = Ink.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Ink, fontWeight = FontWeight.Bold)
    }
}

private fun currencyScale(code: String): Int = if (code == "KRW") 0 else 2

private fun formatAmount(amountMinor: Long, currency: String): String {
    val scale = currencyScale(currency)
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = scale
        maximumFractionDigits = scale
    }
    val major = BigDecimal.valueOf(amountMinor).movePointLeft(scale)
    return symbol(currency) + formatter.format(major)
}

private fun normalizeAmountInput(input: String, currency: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    if (currency == "KRW") return filtered.filter { it.isDigit() }.take(14)
    val dot = filtered.indexOf('.')
    if (dot < 0) return filtered.take(14)
    val integer = filtered.substring(0, dot).take(12)
    val decimals = filtered.substring(dot + 1).filter { it.isDigit() }.take(2)
    return integer + "." + decimals
}

private fun symbol(code: String) = when (code) {
    "BDT" -> "৳"
    "USD" -> "$"
    else -> "₩"
}
