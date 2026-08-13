package com.example.tiffinmanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.launch

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val joiningDate: String,
    val rate: Double,
    val upiId: String,
    val active: Boolean = true
)

@Entity(tableName = "tiffin_entries")
data class TiffinEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val date: String,
    val quantity: Int,
    val rate: Double,
    val amount: Double,
    val status: String = "Delivered"
)

@Entity(tableName = "expenses")
data class ExpenseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val date: String,
    val amount: Double
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val date: String,
    val amount: Double,
    val method: String
)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name")
    suspend fun all(): List<Customer>
    @Insert suspend fun insert(customer: Customer)
}

@Dao
interface TiffinDao {
    @Query("SELECT * FROM tiffin_entries ORDER BY id DESC")
    suspend fun all(): List<TiffinEntry>
    @Insert suspend fun insert(entry: TiffinEntry)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY id DESC")
    suspend fun all(): List<ExpenseEntry>
    @Insert suspend fun insert(entry: ExpenseEntry)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY id DESC")
    suspend fun all(): List<Payment>
    @Insert suspend fun insert(payment: Payment)
}

@Database(
    entities = [Customer::class, TiffinEntry::class, ExpenseEntry::class, Payment::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun tiffinDao(): TiffinDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "tiffin_center.db"
                ).build().also { INSTANCE = it }
            }
    }
}

class TiffinViewModel(private val db: AppDatabase) : ViewModel() {
    var customers by mutableStateOf(listOf<Customer>())
    var tiffins by mutableStateOf(listOf<TiffinEntry>())
    var expenses by mutableStateOf(listOf<ExpenseEntry>())
    var payments by mutableStateOf(listOf<Payment>())
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    fun refresh() {
        scope.launch {
            customers = db.customerDao().all()
            tiffins = db.tiffinDao().all()
            expenses = db.expenseDao().all()
            payments = db.paymentDao().all()
        }
    }
    fun addCustomer(c: Customer) { scope.launch { db.customerDao().insert(c); refresh() } }
    fun addTiffin(e: TiffinEntry) { scope.launch { db.tiffinDao().insert(e); refresh() } }
    fun addExpense(e: ExpenseEntry) { scope.launch { db.expenseDao().insert(e); refresh() } }
    fun addPayment(p: Payment) { scope.launch { db.paymentDao().insert(p); refresh() } }
}

@Composable
fun rememberTiffinViewModel(): TiffinViewModel {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm = remember { TiffinViewModel(AppDatabase.get(context)) }
    LaunchedEffect(Unit) { vm.refresh() }
    return vm
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TiffinCenterApp() {
    val vm = rememberTiffinViewModel()
    var screen by remember { mutableStateOf("Dashboard") }
    Scaffold(topBar = { TopAppBar(title = { Text("Tiffin Center Manager V1.1") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f)) {
                when (screen) {
                    "Dashboard" -> Dashboard(vm)
                    "Customers" -> Customers(vm)
                    "Tiffin" -> TiffinEntryScreen(vm)
                    "Payments" -> PaymentScreen(vm)
                    "Expenses" -> ExpenseScreen(vm)
                }
            }
            NavigationBar {
                listOf("Dashboard","Customers","Tiffin","Payments","Expenses").forEach {
                    NavigationBarItem(
                        selected = screen == it, onClick = { screen = it },
                        icon = { Text(it.take(1)) }, label = { Text(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun Dashboard(vm: TiffinViewModel) {
    val income = vm.tiffins.sumOf { it.amount }
    val expense = vm.expenses.sumOf { it.amount }
    val paid = vm.payments.sumOf { it.amount }
    val pending = (income - paid).coerceAtLeast(0.0)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Metric("Customers", vm.customers.size.toString()) }
        item { Metric("Tiffins", vm.tiffins.sumOf { it.quantity }.toString()) }
        item { Metric("Income", "₹%.2f".format(income)) }
        item { Metric("Expenses", "₹%.2f".format(expense)) }
        item { Metric("Profit", "₹%.2f".format(income - expense)) }
        item { Metric("Paid", "₹%.2f".format(paid)) }
        item { Metric("Pending", "₹%.2f".format(pending)) }
    }
}
@Composable fun Metric(t:String,v:String)=Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(t);Text(v,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}}

@Composable
fun Customers(vm: TiffinViewModel) {
    var name by remember{mutableStateOf("")}; var phone by remember{mutableStateOf("")}
    var joining by remember{mutableStateOf("")}; var rate by remember{mutableStateOf("")}; var upi by remember{mutableStateOf("")}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{
            Text("Customers",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
            OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("Name")})
            OutlinedTextField(phone,{phone=it},Modifier.fillMaxWidth(),label={Text("Mobile")})
            OutlinedTextField(joining,{joining=it},Modifier.fillMaxWidth(),label={Text("Joining Date")})
            OutlinedTextField(rate,{rate=it},Modifier.fillMaxWidth(),label={Text("Tiffin Rate ₹")})
            OutlinedTextField(upi,{upi=it},Modifier.fillMaxWidth(),label={Text("UPI ID (optional)")})
            Button(onClick={vm.addCustomer(Customer(name=name,phone=phone,joiningDate=joining,rate=rate.toDoubleOrNull()?:0.0,upiId=upi));name="";phone="";joining="";rate="";upi=""},Modifier.fillMaxWidth()){Text("Add Customer")}
        }
        items(vm.customers){c->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(c.name,fontWeight=FontWeight.Bold);Text("${c.phone} • ₹${c.rate}");Text("Joining: ${c.joiningDate}");if(c.upiId.isNotBlank())Text("UPI: ${c.upiId}")}}}
    }
}

@Composable
fun TiffinEntryScreen(vm:TiffinViewModel){
    var customer by remember{mutableStateOf("")};var date by remember{mutableStateOf("")};var qty by remember{mutableStateOf("")};var rate by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Daily Tiffin",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(customer,{customer=it},Modifier.fillMaxWidth(),label={Text("Customer Name")})
        OutlinedTextField(date,{date=it},Modifier.fillMaxWidth(),label={Text("Date")})
        OutlinedTextField(qty,{qty=it.filter(Char::isDigit)},Modifier.fillMaxWidth(),label={Text("Quantity")})
        OutlinedTextField(rate,{rate=it},Modifier.fillMaxWidth(),label={Text("Rate ₹")})
        Button(onClick={val q=qty.toIntOrNull()?:0;val r=rate.toDoubleOrNull()?:0.0;vm.addTiffin(TiffinEntry(customerName=customer,date=date,quantity=q,rate=r,amount=q*r,customerId=0));customer="";date="";qty="";rate=""},Modifier.fillMaxWidth()){Text("Save Tiffin")}
    }
}

@Composable
fun PaymentScreen(vm:TiffinViewModel){
    var customer by remember{mutableStateOf("")};var date by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")};var method by remember{mutableStateOf("UPI")}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Payment",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(customer,{customer=it},Modifier.fillMaxWidth(),label={Text("Customer")})
        OutlinedTextField(date,{date=it},Modifier.fillMaxWidth(),label={Text("Payment Date")})
        OutlinedTextField(amount,{amount=it},Modifier.fillMaxWidth(),label={Text("Amount ₹")})
        OutlinedTextField(method,{method=it},Modifier.fillMaxWidth(),label={Text("Method: Cash / UPI / Bank")})
        Button(onClick={vm.addPayment(Payment(customerId=0,customerName=customer,date=date,amount=amount.toDoubleOrNull()?:0.0,method=method));customer="";date="";amount=""},Modifier.fillMaxWidth()){Text("Save Payment")}
        Spacer(Modifier.height(12.dp))
        Text("Payment History",fontWeight=FontWeight.Bold)
        vm.payments.take(10).forEach{Text("${it.customerName} • ₹${it.amount} • ${it.method} • ${it.date}")}
    }
}

@Composable
fun ExpenseScreen(vm:TiffinViewModel){
    var title by remember{mutableStateOf("")};var date by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Expenses",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Expense")})
        OutlinedTextField(date,{date=it},Modifier.fillMaxWidth(),label={Text("Date")})
        OutlinedTextField(amount,{amount=it},Modifier.fillMaxWidth(),label={Text("Amount ₹")})
        Button(onClick={vm.addExpense(ExpenseEntry(title=title,date=date,amount=amount.toDoubleOrNull()?:0.0));title="";date="";amount=""},Modifier.fillMaxWidth()){Text("Save Expense")}
    }
}

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme{TiffinCenterApp()}}}
}
