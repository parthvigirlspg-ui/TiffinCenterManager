package com.example.tiffinmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TiffinEntry(
    val customer: String,
    val date: String,
    val quantity: Int,
    val amount: Double
)

data class ExpenseEntry(
    val title: String,
    val amount: Double
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                TiffinCenterApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiffinCenterApp() {
    var selectedScreen by remember { mutableStateOf("Dashboard") }

    val tiffins = remember { mutableStateListOf<TiffinEntry>() }
    val expenses = remember { mutableStateListOf<ExpenseEntry>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tiffin Center Manager") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedScreen) {
                "Dashboard" -> DashboardScreen(
                    tiffins = tiffins,
                    expenses = expenses
                )

                "Tiffin" -> TiffinEntryScreen(
                    onAdd = { entry -> tiffins.add(entry) }
                )

                "Expense" -> ExpenseScreen(
                    onAdd = { entry -> expenses.add(entry) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { selectedScreen = "Dashboard" }
                ) {
                    Text("Dashboard")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { selectedScreen = "Tiffin" }
                ) {
                    Text("Tiffin")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { selectedScreen = "Expense" }
                ) {
                    Text("Expense")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DashboardScreen(
    tiffins: List<TiffinEntry>,
    expenses: List<ExpenseEntry>
) {
    val income = tiffins.sumOf { it.amount }
    val totalExpense = expenses.sumOf { it.amount }
    val profit = income - totalExpense
    val totalTiffins = tiffins.sumOf { it.quantity }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            SummaryCard("Total Tiffins", totalTiffins.toString())
        }

        item {
            SummaryCard("Total Income", "₹%.2f".format(income))
        }

        item {
            SummaryCard("Total Expenses", "₹%.2f".format(totalExpense))
        }

        item {
            SummaryCard("Profit", "₹%.2f".format(profit))
        }

        item {
            Text(
                text = "Recent Tiffin Entries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(tiffins.takeLast(10).reversed()) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(entry.customer, fontWeight = FontWeight.Bold)
                    Text("Date: ${entry.date}")
                    Text("Tiffins: ${entry.quantity}")
                    Text("Amount: ₹%.2f".format(entry.amount))
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TiffinEntryScreen(
    onAdd: (TiffinEntry) -> Unit
) {
    var customer by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Daily Tiffin Entry",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = customer,
            onValueChange = { customer = it },
            label = { Text("Customer Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (DD-MM-YYYY)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it.filter(Char::isDigit) },
            label = { Text("Tiffin Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (₹)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val qty = quantity.toIntOrNull()
                val price = amount.toDoubleOrNull()

                if (
                    customer.isNotBlank() &&
                    date.isNotBlank() &&
                    qty != null &&
                    price != null
                ) {
                    onAdd(
                        TiffinEntry(
                            customer = customer.trim(),
                            date = date.trim(),
                            quantity = qty,
                            amount = price
                        )
                    )

                    customer = ""
                    date = ""
                    quantity = ""
                    amount = ""
                }
            }
        ) {
            Text("Save Tiffin Entry")
        }
    }
}

@Composable
fun ExpenseScreen(
    onAdd: (ExpenseEntry) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Add Expense",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Expense Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount (₹)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val value = amount.toDoubleOrNull()

                if (title.isNotBlank() && value != null) {
                    onAdd(
                        ExpenseEntry(
                            title = title.trim(),
                            amount = value
                        )
                    )

                    title = ""
                    amount = ""
                }
            }
        ) {
            Text("Save Expense")
        }
    }
}
