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
import com.fahad.pocketledger.data.TransactionEntity
import kotlinx.coroutines.launch
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
                HomeScreen(dao) { amount, type, category, note, currency ->
                    lifecycleScope.launch {
                        dao.insert(TransactionEntity(amountMinor = amount, type = type, category = category, note = note, currency = currency))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    dao: com.fahad.pocketledger.data.TransactionDao,
    add: (Long, String, String, String, String) -> Unit
) {
    val rows by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    var showAdd by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("EXPENSE") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Dining") }
    var note by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("KRW") }
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }
    // Never add different currencies together. Dashboard shows totals for selected currency only.
    val selected = rows.filter { it.currency == currency }
    val income = selected.filter { it.type == "INCOME" }.sumOf { it.amountMinor }
    val expense = selected.filter { it.type == "EXPENSE" }.sumOf { it.amountMinor }
    Scaffold(
        topBar = { TopAppBar(title = { Column {
            Text("Pocket Ledger", color = Ink, fontWeight = FontWeight.Bold)
            Text("Your money, made clearer", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        } }, actions = { TextButton(onClick = { showAdd = true }) { Text("+ Add") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }, containerColor = Green, contentColor = Color.White) { Text("+") } },
        bottomBar = { NavigationBar { listOf("Home", "History", "Budgets", "Goals", "Settings").forEachIndexed { i, label ->
            NavigationBarItem(selected = i == 0, onClick = {}, icon = { Text(listOf("⌂", "≡", "◷", "☆", "⚙")[i]) }, label = { Text(label) })
        } } }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().background(Canvas).padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        Text("${symbol(currency)}${fmt.format(income - expense)}", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Metric("Income", "${symbol(currency)}${fmt.format(income)}", Color(0xFFE1F6EA), Modifier.weight(1f))
                            Metric("Expenses", "${symbol(currency)}${fmt.format(expense)}", Color(0xFFFFE8E8), Modifier.weight(1f))
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
            if (selected.isEmpty()) item {
                Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your money story starts here", fontWeight = FontWeight.Bold)
                    Text("Add your first income or expense to see your dashboard.", color = Color.Gray)
                } }
            } else items(selected.take(50), key = { it.id }) { row ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.category, fontWeight = FontWeight.SemiBold)
                            Text(row.note.ifBlank { row.type.lowercase().replaceFirstChar { it.uppercase() } }, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Text((if (row.type == "INCOME") "+" else "−") + symbol(row.currency) + fmt.format(row.amountMinor),
                            color = if (row.type == "INCOME") Green else Color(0xFFD94B4B), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
    if (showAdd) AlertDialog(
        onDismissRequest = { showAdd = false },
        title = { Text("Add transaction") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("Income") })
                FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("Expense") })
            }
            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }.take(14) }, label = { Text("Amount ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(value = category, onValueChange = { category = it.take(60) }, label = { Text("Category") }, singleLine = true)
            OutlinedTextField(value = note, onValueChange = { note = it.take(160) }, label = { Text("Note (optional)") }, singleLine = true)
            Text("Amounts are recorded in whole currency units in this starter.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        } },
        confirmButton = { Button(onClick = {
            val value = amount.toBigDecimalOrNull()
            if (value != null && value > java.math.BigDecimal.ZERO) {
                runCatching { value.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrNull()?.let {
                    add(it, type, category.ifBlank { "Other" }, note, currency)
                    amount = ""; note = ""; showAdd = false
                }
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
    )
}

@Composable
private fun Metric(label: String, value: String, background: Color, modifier: Modifier = Modifier) {
    Column(modifier.background(background, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label, color = Ink.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Ink, fontWeight = FontWeight.Bold)
    }
}
private fun symbol(code: String) = when (code) { "BDT" -> "৳"; "USD" -> "$"; else -> "₩" }
