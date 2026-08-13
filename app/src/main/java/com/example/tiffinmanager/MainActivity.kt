package com.example.tiffinmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mobile: String = "",
    val address: String = "",
    val rate: Double = 0.0,
    val lunch: Boolean = true,
    val dinner: Boolean = false,
    val joiningDate: String = "",
    val active: Boolean = true
)

@Entity(tableName = "tiffin_entries")
data class TiffinEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val date: String,
    val lunchQty: Int = 0,
    val dinnerQty: Int = 0,
    val amount: Double = 0.0
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val date: String,
    val amount: Double,
    val mode: String
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val category: String,
    val amount: Double,
    val description: String = ""
)

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name") fun all(): kotlinx.coroutines.flow.Flow<List<Customer>>
    @Insert fun insert(c: Customer)
    @Update fun update(c: Customer)
    @Delete fun delete(c: Customer)
}

@Dao
interface TiffinDao {
    @Query("SELECT * FROM tiffin_entries ORDER BY date DESC") fun all(): kotlinx.coroutines.flow.Flow<List<TiffinEntry>>
    @Insert fun insert(e: TiffinEntry)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC") fun all(): kotlinx.coroutines.flow.Flow<List<Payment>>
    @Insert fun insert(p: Payment)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC") fun all(): kotlinx.coroutines.flow.Flow<List<Expense>>
    @Insert fun insert(e: Expense)
}

@Database(entities=[Customer::class,TiffinEntry::class,Payment::class,Expense::class], version=1, exportSchema=false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun tiffinDao(): TiffinDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "tiffin.db").build().also { INSTANCE=it }
            }
    }
}

@Composable
fun App() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val scope = rememberCoroutineScope()
    var screen by remember { mutableStateOf("Dashboard") }
    var showCustomer by remember { mutableStateOf(false) }
    var showExpense by remember { mutableStateOf(false) }
    var showTiffin by remember { mutableStateOf(false) }

    val customers by db.customerDao().all().collectAsState(initial = emptyList())
    val expenses by db.expenseDao().all().collectAsState(initial = emptyList())
    val tiffins by db.tiffinDao().all().collectAsState(initial = emptyList())

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title={ Text("🍱 Tiffin Center Manager") }) },
            bottomBar = {
                NavigationBar {
                    listOf("Dashboard","Customers","Tiffin","Payments","Reports").forEach {
                        NavigationBarItem(selected=screen==it,onClick={screen=it},
                            icon={Text(when(it){"Dashboard"->"🏠";"Customers"->"👥";"Tiffin"->"🍱";"Payments"->"💰";else->"📊"})},
                            label={Text(it)})
                    }
                }
            },
            floatingActionButton = {
                if(screen=="Customers") FloatingActionButton(onClick={showCustomer=true}){Text("+")}
                else if(screen=="Tiffin") FloatingActionButton(onClick={showTiffin=true}){Text("+")}
                else if(screen=="Reports") FloatingActionButton(onClick={showExpense=true}){Text("+")}
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when(screen) {
                    "Dashboard" -> Dashboard(customers,tiffins,expenses)
                    "Customers" -> CustomerList(customers,onDelete={scope.launch{db.customerDao().delete(it)}})
                    "Tiffin" -> TiffinList(tiffins,customers)
                    "Payments" -> PaymentScreen(customers,db,scope)
                    "Reports" -> ExpenseList(expenses)
                }
            }
        }
    }

    if(showCustomer) CustomerDialog(
        onDismiss={showCustomer=false},
        onSave={c -> scope.launch{db.customerDao().insert(c);showCustomer=false}}
    )
    if(showTiffin) TiffinDialog(customers,{e->scope.launch{db.tiffinDao().insert(e);showTiffin=false}},{showTiffin=false})
    if(showExpense) ExpenseDialog({e->scope.launch{db.expenseDao().insert(e);showExpense=false}},{showExpense=false})
}

@Composable fun Dashboard(c:List<Customer>, t:List<TiffinEntry>, e:List<Expense>) {
    val income=t.sumOf{it.amount}; val expense=e.sumOf{it.amount}
    LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{Text("Dashboard",style=MaterialTheme.typography.headlineMedium)}
        item{StatCard("🍱 Today's Tiffins",t.filter{it.date==today()}.sumOf{it.lunchQty+it.dinnerQty}.toString())}
        item{StatCard("💰 Total Income","₹%.2f".format(income))}
        item{StatCard("💸 Total Expenses","₹%.2f".format(expense))}
        item{StatCard("📈 Net Profit","₹%.2f".format(income-expense))}
        item{StatCard("👥 Active Customers",c.count{it.active}.toString())}
    }
}
@Composable fun StatCard(title:String,value:String){Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Text(title);Text(value,style=MaterialTheme.typography.headlineSmall)}}}

@Composable fun CustomerList(list:List<Customer>,onDelete:(Customer)->Unit)=LazyColumn(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    items(list){c->Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(c.name,style=MaterialTheme.typography.titleMedium);Text("${c.mobile}  •  ₹${c.rate}/tiffin");Text(if(c.active)"Active" else "Inactive")}Button(onClick={ {onDelete(c)} }){Text("Delete")}}}}
}
@Composable fun TiffinList(list:List<TiffinEntry>,customers:List<Customer>)=LazyColumn(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(list){e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(customers.find{it.id==e.customerId}?.name ?: "Customer #${e.customerId}");Text("${e.date}  •  Lunch ${e.lunchQty}  •  Dinner ${e.dinnerQty}  •  ₹${e.amount}")}}}}
@Composable fun ExpenseList(list:List<Expense>)=LazyColumn(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(list){e->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Text(e.category,style=MaterialTheme.typography.titleMedium);Text("${e.date}  •  ₹${e.amount}");if(e.description.isNotBlank())Text(e.description)}}}}
@Composable fun PaymentScreen(customers:List<Customer>,db:AppDatabase,scope:kotlinx.coroutines.CoroutineScope){
    var selected by remember{mutableStateOf<Customer?>(null)}; var amount by remember{mutableStateOf("")}
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Record Payment",style=MaterialTheme.typography.headlineSmall)
        customers.forEach{c->Button(onClick={selected=c},Modifier.fillMaxWidth()){Text("${c.name}  •  ₹${c.rate}/tiffin")}}
        if(selected!=null){OutlinedTextField(amount,{amount=it},label={Text("Amount")},modifier=Modifier.fillMaxWidth());Button(onClick={scope.launch{db.paymentDao().insert(Payment(customerId=selected!!.id,date=today(),amount=amount.toDoubleOrNull()?:0.0,mode="Cash"));amount="";selected=null}},Modifier.fillMaxWidth()){Text("Save Payment")}}
    }
}
@Composable fun CustomerDialog(onDismiss:()->Unit,onSave:(Customer)->Unit){
    var name by remember{mutableStateOf("")};var mobile by remember{mutableStateOf("")};var rate by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add Customer")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(name,{name=it},label={Text("Name")});OutlinedTextField(mobile,{mobile=it},label={Text("Mobile")});OutlinedTextField(rate,{rate=it},label={Text("Tiffin Rate")})}},confirmButton={Button(onClick={if(name.isNotBlank())onSave(Customer(name=name,mobile=mobile,rate=rate.toDoubleOrNull()?:0.0,joiningDate=today()))}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable fun TiffinDialog(customers:List<Customer>,onSave:(TiffinEntry)->Unit,onDismiss:()->Unit){
    var cid by remember{mutableStateOf(customers.firstOrNull()?.id?:0)};var lunch by remember{mutableStateOf("1")};var dinner by remember{mutableStateOf("0")}
    val c=customers.find{it.id==cid}; AlertDialog(onDismissRequest=onDismiss,title={Text("Daily Tiffin Entry")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){customers.forEach{Button(onClick={cid=it.id}){Text(it.name)}};OutlinedTextField(lunch,{lunch=it},label={Text("Lunch Qty")});OutlinedTextField(dinner,{dinner=it},label={Text("Dinner Qty")})}},confirmButton={Button(onClick={onSave(TiffinEntry(customerId=cid,date=today(),lunchQty=lunch.toIntOrNull()?:0,dinnerQty=dinner.toIntOrNull()?:0,amount=((lunch.toDoubleOrNull()?:0.0)+(dinner.toDoubleOrNull()?:0.0))*(c?.rate?:0.0)))}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable fun ExpenseDialog(onSave:(Expense)->Unit,onDismiss:()->Unit){
    var cat by remember{mutableStateOf("")};var amt by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Add Expense")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(cat,{cat=it},label={Text("Category")});OutlinedTextField(amt,{amt=it},label={Text("Amount")});OutlinedTextField(desc,{desc=it},label={Text("Description")})}},confirmButton={Button(onClick={onSave(Expense(date=today(),category=cat,amount=amt.toDoubleOrNull()?:0.0,description=desc))}){Text("Save")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(Date())
