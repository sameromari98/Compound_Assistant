package com.samer.compoundassistant

/* ============================== Android ============================== */
import android.Manifest
import android.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import android.widget.Toast

/* ============================== Compose ============================== */
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

/* ============================== Images ============================== */
import coil.compose.rememberAsyncImagePainter

/* ============================== Data ============================== */
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/* ============================ Notifications ========================= */
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider

/* ================================ Java ============================== */
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/* ============================== Constants =========================== */
private const val SECURITY_EMAIL = "frcsecurity@falcompound.net"

/* ============================== Models ============================== */

data class Contact(
    val name: String,
    val phone: String,
    val hours: String = "",
    val category: String = "Other",
    val favorite: Boolean = false
)

data class Profile(
    val name: String = "",
    val apartment: String = "",
    val profilePhotoUri: String = "",
    val myIdUri: String = ""          // persisted My ID image (FileProvider URI after fix)
)

data class Visitor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val idImageUri: String,
    val arrival: String,              // stored "HH:mm"
    val departure: String,            // stored "HH:mm"
    val date: String = LocalDate.now().toString()
)

data class SavedPerson(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val idImageUri: String
)

/* ============================= DataStore ============================ */

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "compound_store")
private val KEY_PROFILE_JSON = stringPreferencesKey("profile_json")
private val KEY_CONTACTS_JSON = stringPreferencesKey("contacts_json")
private val KEY_VISITORS_JSON = stringPreferencesKey("visitors_json")
private val KEY_SAVED_PEOPLE_JSON = stringPreferencesKey("saved_people_json")

/* ============================ Built-in contacts ===================== */

private val builtInContacts: List<Contact> = listOf(
    Contact("Compound Manager – Ms. Hala Al Athel", "4037", "8:00 am – 5:00 pm", "Administration"),
    Contact("Deputy Manager – Mohammad Al-Alfan", "4019", "8:00 am – 5:00 pm", "Administration"),
    Contact("Secretary (C.M.) – Lourdes", "4037", "8:00 am – 5:00 pm", "Administration"),
    Contact("Lease Administrator – Mohd. Moklesur Rahman", "4036", "8:00 am – 5:00 pm", "Leasing"),
    Contact("Leasing Assistant – Danish", "4003", "8:00 am – 5:00 pm", "Leasing"),
    Contact("Leasing Assistant – Ms. Randa", "4005", "8:00 am – 5:00 pm", "Leasing"),
    Contact("Chief Accountant – Mohammed Nadeem Baig", "4039", "8:00 am – 5:00 pm", "Accounting"),
    Contact("Accountant – Sajid Ali", "4026", "8:00 am – 5:00 pm", "Accounting"),
    Contact("Cashier – Kumar", "4006", "8:00 am – 8:00 pm", "Accounting"),
    Contact("IT / HR – Mr. Fahad", "4045", "8:00 am – 11:00 pm", "IT / HR"),
    Contact("PABX Telephone – Augustin", "3999", "1:00 pm – 9:00 pm", "IT / HR"),
    Contact("Video Operator – Augustin", "4009", "3:00 pm – 11:00 pm", "IT / HR"),
    Contact("Housing Supervisor – Lourdes", "4004 / 1387", "8:00 am – 5:00 pm", "Housing"),
    Contact("Housing Assistant – Amina", "4004 / 1387", "8:00 am – 5:00 pm", "Housing"),
    Contact("Operator & Security (Emergency)", "0 / 4001 / 4024", "24 Hours", "Emergency"),
    Contact("Administration Reception – Jewel / Kamlesh", "0", "24 Hours", "Emergency"),
    Contact("Transport Supervisor – Najim Uddin", "0", "8:00 am – 5:00 pm", "Emergency"),
    Contact("Maintenance Manager – Mr. Chris", "4027", "8:00 am – 5:00 pm", "Maintenance"),
    Contact("Maintenance Coordinator – Mr. Dias", "1333", "8:00 am – 5:00 pm", "Maintenance"),
    Contact("Maintenance Office – Masoud", "4022", "8:00 am – 5:00 pm", "Maintenance"),
    Contact("Warehouse – Obaidullah", "4044", "8:00 am – 5:00 pm", "Maintenance"),
    Contact("Security Supervisor – Dosari", "4001 / 4024", "8:00 am – 5:00 pm", "Security"),
    Contact("Visitor Gate – Security", "4001", "24 Hours", "Security"),
    Contact("Main Gate – Security", "4042 / 1193", "24 Hours", "Security"),
    Contact("Arm Gate – Security", "4040", "24 Hours", "Security"),
    Contact("Housekeeping Supervisor – Masoud", "4022", "8:00 am – 5:00 pm", "Housekeeping"),
    Contact("Pest Control & Landscaping – Shabir", "4007", "8:00 am – 5:00 pm", "Housekeeping"),
    Contact("Recreation Supervisor – Roel", "4017", "9:00 am – 5:00 pm", "Recreation"),
    Contact("Attendant Rec. A", "4015", "3:00 pm – 11:00 pm", "Recreation"),
    Contact("Attendant Rec. B – Hasan", "4029", "8:00 am – 11:00 pm", "Recreation"),
    Contact("Coffee Shop Rec. A", "3215", "7:00 am – 12:00 am", "Food & Shops"),
    Contact("Sports Café Shop Rec. A", "3216", "1:00 pm – 12:00 am", "Food & Shops"),
    Contact("New Crystal Pizza Shop", "4008", "11:00 am – 12:00 am", "Food & Shops"),
    Contact("Restaurant Crystal", "4033", "12:00 pm – 12:00 am", "Food & Shops"),
    Contact("Pizza Shop – Bakery", "3233", "11:00 am – 12:00 am", "Food & Shops"),
    Contact("ZINC Café (Family S. Pool)", "4021", "12:00 pm – 12:00 am", "Food & Shops"),
    Contact("Laundry Shop", "4018", "12:00 pm – 10:00 pm", "Services"),
    Contact("Mini Market", "4020", "7:00 am – 12:00 am", "Services")
)

/* ============================== Colors / Theme ====================== */

// FAL logo palette
val FalBlue  = Color(0xFF3A7BD5)
val FalTeal  = Color(0xFF26C6DA)
val FalGreen = Color(0xFF43A047)
val FalDeep  = Color(0xFF2E7D32)
val FalSurface = Color(0xFFF7FAFC)

val TealAccent =  Color(0xFF00897B)
fun falLightColors() = lightColorScheme(
    primary = FalGreen,
    onPrimary = Color.White,
    secondary = FalTeal,
    onSecondary = Color.White,
    tertiary = FalBlue,
    onTertiary = Color.White,
    surface = FalSurface,
    background = Color.White
)

/* ============================== Activity ============================ */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)

        createReminderChannel()

        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }

        setContent {
            // Use FAL palette (you can switch to dynamicLightColorScheme if you ever want)
            MaterialTheme(colorScheme = falLightColors()) {
                App()
            }
        }
    }

    private fun createReminderChannel() {
        val channel = NotificationChannelCompat.Builder(
            "visitors", NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName("Visitor Reminders")
            .setDescription("Reminders for visitor departure times")
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }
}

/* ============================== UI Shell ============================ */

@Composable
fun App() {
    var tab by remember { mutableStateOf(0) } // 0:Dash 1:MyID 2:Directory 3:Visitors 4:Profile

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.CreditCard, null) }, label = { Text("My ID") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Security, null) }, label = { Text("Directory") }
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.PersonAdd, null) }, label = { Text("Visitors") }
                )
                NavigationBarItem(
                    selected = tab == 4, onClick = { tab = 4 },
                    icon = { Icon(Icons.Filled.AccountCircle, null) }, label = { Text("Profile") }
                )
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (tab) {
                0 -> DashboardScreen(onGoMyID = { tab = 1 }, onGoVisitors = { tab = 3 })
                1 -> MyIDScreen()
                2 -> DirectoryScreen()
                3 -> VisitorsScreen()
                4 -> ProfileScreen()
            }
        }
    }
}

/* ============================ Banner ================================ */

@Composable
fun Banner(
    title: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        color = color,
        contentColor = Color.White,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            trailing?.invoke()
        }
    }
}

/* ============================ Dashboard ============================= */

@Composable
fun DashboardScreen(onGoMyID: () -> Unit, onGoVisitors: () -> Unit) {
    val context = LocalContext.current
    var profile by remember { mutableStateOf(Profile()) }
    var visitors by remember { mutableStateOf(listOf<Visitor>()) }

    LaunchedEffect(Unit) {
        profile = loadProfile(context)
        visitors = loadVisitors(context)
            .filter { it.date == LocalDate.now().toString() }
            .sortedBy { it.arrival }
    }

    Column(Modifier.fillMaxSize()) {

        Banner(title = "Dashboard", color = FalGreen, icon = Icons.Filled.Home)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Show My ID", Icons.Filled.CreditCard, Modifier.weight(1f), onGoMyID)
                QuickAction("Add Visitor", Icons.Filled.PersonAdd, Modifier.weight(1f), onGoVisitors)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Call Security", Icons.Filled.Security, Modifier.weight(1f)) { dial(context, "4001") }
                QuickAction("Call Maintenance", Icons.Filled.Build, Modifier.weight(1f)) { dial(context, "4022") }
            }

            Banner(title = "Today's Visitors", color = FalBlue, icon = Icons.Filled.Schedule)

            if (visitors.isEmpty()) {
                Text("No visitors added for today.", modifier = Modifier.padding(16.dp))
            } else {
                visitors.forEach {
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(it.name, style = MaterialTheme.typography.titleMedium)
                                Text("${it.arrival} → ${it.departure}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Icon(imageVector = Icons.Filled.Schedule, contentDescription = null, tint = FalTeal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(modifier = modifier.height(80.dp).clickable { onClick() }) {
        Row(Modifier.fillMaxSize()) {
            // Color rail
            Box(Modifier.width(6.dp).fillMaxSize().background(FalTeal))
            Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(28.dp), tint = FalTeal)
                Spacer(Modifier.width(8.dp))
                Text(text)
            }
        }
    }
}

/* ============================== My ID =============================== */

@Composable
fun MyIDScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf(Profile()) }
    var myIdUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        profile = loadProfile(context)
        // migrate old external URIs to private copy once
        val current = profile.myIdUri
        val initial = current.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val migrated: String = if (initial != null && !isOurFileProviderUri(context, initial) && isUriReadable(context, initial)) {
            copyToPrivateFile(context, initial, subdir = "id_images")?.toString() ?: ""
        } else current
        if (migrated != current) {
            val updated = profile.copy(myIdUri = migrated)
            profile = updated
            scope.launch { saveProfile(context, updated) }
        }
        myIdUri = migrated.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Save our PRIVATE copy and store our FileProvider URI
            val privateUri = copyToPrivateFile(context, uri, subdir = "id_images")
            myIdUri = privateUri
            val updated = profile.copy(myIdUri = privateUri?.toString() ?: "")
            profile = updated
            scope.launch { saveProfile(context, updated) }
            Toast.makeText(context, "ID image saved privately.", Toast.LENGTH_SHORT).show()
        }
    }

    val idAvailable = isUriReadable(context, myIdUri)

    Column(Modifier.fillMaxSize()) {

        Banner(title = "My ID", color = FalBlue, icon = Icons.Filled.CreditCard)

        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (idAvailable && myIdUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(myIdUri),
                    contentDescription = "My ID",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No ID selected")
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickImage.launch(arrayOf("image/*")) }) {
                    Text(if (!idAvailable) "Pick My ID" else "Replace My ID")
                }
                Button(
                    onClick = {
                        if (idAvailable && myIdUri != null) {
                            // Share our own FileProvider URI directly
                            val shareUri = myIdUri!!
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = context.contentResolver.getType(shareUri) ?: "image/*"
                                putExtra(Intent.EXTRA_STREAM, shareUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                clipData = ClipData.newRawUri("id", shareUri)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share My ID"))
                        } else {
                            Toast.makeText(context, "ID image not found. Please replace it.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = idAvailable
                ) { Text("Share") }
            }
        }
    }
}

/* ============================ Directory ============================= */

@Composable
fun DirectoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var contacts by remember { mutableStateOf(listOf<Contact>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val saved: List<Contact> = loadContacts(context)
            contacts = if (saved.isEmpty()) {
                saveContacts(context, builtInContacts)
                builtInContacts
            } else saved
            loading = false
        } catch (t: Throwable) {
            error = t.message ?: "Failed to load directory"
            loading = false
        }
    }

    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("All") }
    var editing by remember { mutableStateOf<Contact?>(null) }
    var confirmDeleteFor by remember { mutableStateOf<Contact?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    val categories: List<String> = remember(contacts) {
        (listOf("All") + contacts.map { it.category.ifBlank { "Other" } }.distinct().sorted())
    }

    val visible: List<Contact> = remember(contacts, query, categoryFilter) {
        val q = query.trim().lowercase()
        contacts
            .sortedWith(
                compareByDescending<Contact> { it.favorite }
                    .thenBy { it.category }
                    .thenBy { it.name }
            )
            .filter { c ->
                val qMatch = q.isBlank() ||
                        c.name.lowercase().contains(q) ||
                        c.phone.lowercase().contains(q) ||
                        c.hours.lowercase().contains(q)
                val catMatch = categoryFilter == "All" ||
                        c.category.ifBlank { "Other" } == categoryFilter
                qMatch && catMatch
            }
    }

    val grouped: Map<String, List<Contact>> = remember(visible) {
        visible.groupBy { it.category.ifBlank { "Other" } }
            .toSortedMap(compareBy<String> { it != "Emergency" }.thenBy { it })
    }

    Column(Modifier.fillMaxSize()) {

        Banner(
            title = "Phone Directory",
            color = FalBlue,
            icon = Icons.Filled.Security,
            trailing = {
                Button(
                    onClick = { showAdd = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = FalBlue
                    )
                ) { Text("Add") }
            }
        )

        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search (name / number / hours)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { categoryFilter = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            when {
                loading -> Text("Loading…", modifier = Modifier.padding(8.dp))
                error != null -> Text("Error: $error", color = Color.Red, modifier = Modifier.padding(8.dp))
                else -> {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        grouped.forEach { (cat: String, itemsInCat: List<Contact>) ->
                            item {
                                Surface(color = MaterialTheme.colorScheme.surface) {
                                    Row(
                                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("${itemsInCat.size}", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Divider()
                                }
                            }
                            items(itemsInCat, key = { it.name + it.phone }) { c ->
                                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f).clickable { dial(context, c.phone) }) {
                                            Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                            if (c.hours.isNotBlank()) {
                                                Spacer(Modifier.height(2.dp))
                                                Text(c.hours, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            IconButton(onClick = {
                                                val updated: List<Contact> = contacts.map { if (it == c) it.copy(favorite = !it.favorite) else it }
                                                contacts = updated
                                                scope.launch { saveContacts(context, updated) }
                                            }) { Icon(if (c.favorite) Icons.Filled.Star else Icons.Filled.StarBorder, null, tint = FalTeal) }
                                            IconButton(onClick = { clipboard.setText(AnnotatedString(c.phone)) }) {
                                                Icon(Icons.Filled.ContentCopy, null)
                                            }
                                            IconButton(onClick = { editing = c }) { Icon(Icons.Filled.Edit, null) }
                                            IconButton(onClick = { confirmDeleteFor = c }) { Icon(Icons.Filled.Delete, null) }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        Modifier.fillMaxWidth().clickable { dial(context, c.phone) },
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(c.category, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                                        Text(c.phone, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Divider()
                            }
                        }
                        if (grouped.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No contacts match your filters.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add contact dialog
    if (showAdd) {
        AddContactDialog(
            onDismiss = { showAdd = false },
            onSave = { name: String, phone: String, hours: String, category: String ->
                val updated: List<Contact> = contacts + Contact(
                    name = name.trim(),
                    phone = phone.trim(),
                    hours = hours.trim(),
                    category = category.trim().ifBlank { "Other" }
                )
                contacts = updated
                scope.launch { saveContacts(context, updated) }
                showAdd = false
            }
        )
    }

    // Edit dialog
    editing?.let { original ->
        var n by remember { mutableStateOf(original.name) }
        var p by remember { mutableStateOf(original.phone) }
        var h by remember { mutableStateOf(original.hours) }
        var cat by remember { mutableStateOf(original.category) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit Contact") },
            text = {
                Column {
                    OutlinedTextField(n, { n = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(p, { p = it }, label = { Text("Phone / Ext") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(h, { h = it }, label = { Text("Hours") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(cat, { cat = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (n.isNotBlank() && p.isNotBlank()) {
                        val updated: List<Contact> = contacts.map {
                            if (it == original) it.copy(
                                name = n.trim(), phone = p.trim(),
                                hours = h.trim(), category = cat.trim().ifBlank { "Other" }
                            ) else it
                        }
                        contacts = updated
                        scope.launch { saveContacts(context, updated) }
                        editing = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { Button(onClick = { editing = null }) { Text("Cancel") } }
        )
    }

    // Delete confirm
    confirmDeleteFor?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteFor = null },
            title = { Text("Delete contact?") },
            text = { Text("Remove “${target.name}” from your directory?") },
            confirmButton = {
                Button(onClick = {
                    val newList: List<Contact> = contacts.filterNot { it == target }
                    contacts = newList
                    scope.launch { saveContacts(context, newList) }
                    confirmDeleteFor = null
                }) { Text("Delete") }
            },
            dismissButton = { Button(onClick = { confirmDeleteFor = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, hours: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Other") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone / Ext") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(hours, { hours = it }, label = { Text("Hours") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && phone.isNotBlank(),
                onClick = { onSave(name, phone, hours, category) }
            ) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

/* ============================ Visitors ============================== */

@Composable
fun VisitorsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var visitors by remember { mutableStateOf(listOf<Visitor>()) }
    var savedPeople by remember { mutableStateOf(listOf<SavedPerson>()) }
    var profile by remember { mutableStateOf(Profile()) }

    // Dialog states
    var showAdd by remember { mutableStateOf(false) }
    var managePeople by remember { mutableStateOf(false) }
    var editVisit by remember { mutableStateOf<Visitor?>(null) }

    // Add dialog fields
    var visitorName by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var arrival by remember { mutableStateOf<LocalTime?>(null) }
    var departure by remember { mutableStateOf<LocalTime?>(null) }
    var saveForReuse by remember { mutableStateOf(true) }

    // Pick → copy to private storage immediately
    val pickVisitorId = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pickedUri = ensurePrivateImage(context, uri, subdir = "visitor_images")
            if (pickedUri == null) {
                Toast.makeText(context, "Couldn't save the ID image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        profile = loadProfile(context)

        // Load then migrate any non-private URIs to private copies
        val loadedVisitors = loadVisitors(context)
        val migratedVisitors = loadedVisitors.map { v ->
            val src = Uri.parse(v.idImageUri)
            val fixed = if (isOurFileProviderUri(context, src)) src
            else if (isUriReadable(context, src)) ensurePrivateImage(context, src, "visitor_images")
            else src
            if (fixed != null && fixed.toString() != v.idImageUri) v.copy(idImageUri = fixed.toString()) else v
        }

        val loadedSaved = loadSavedPeople(context)
        val migratedSaved = loadedSaved.map { p ->
            val src = Uri.parse(p.idImageUri)
            val fixed = if (isOurFileProviderUri(context, src)) src
            else if (isUriReadable(context, src)) ensurePrivateImage(context, src, "visitor_images")
            else src
            if (fixed != null && fixed.toString() != p.idImageUri) p.copy(idImageUri = fixed.toString()) else p
        }

        // Persist migrations if anything changed
        if (migratedVisitors != loadedVisitors) saveVisitors(context, migratedVisitors)
        if (migratedSaved != loadedSaved) saveSavedPeople(context, migratedSaved)

        visitors = migratedVisitors.sortedWith(compareBy<Visitor> { it.date }.thenBy { it.arrival })
        savedPeople = migratedSaved
    }

    val fmt24 = remember { DateTimeFormatter.ofPattern("HH:mm") }   // stored
    val fmt12 = remember { DateTimeFormatter.ofPattern("hh:mm a") } // shown

    val today = LocalDate.now().toString()
    val todays = visitors.filter { it.date == today }
    val upcoming = visitors.filter { it.date > today }
    val past = visitors.filter { it.date < today }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Banner(
                title = "Visit",
                color = FalTeal,
                icon = Icons.Filled.PersonAdd,
                trailing = {
                    Button(
                        onClick = {
                            visitorName = ""
                            pickedUri = null
                            arrival = null
                            departure = null
                            saveForReuse = true
                            showAdd = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = FalTeal
                        )
                    ) { Text("Add") }
                }
            )
        }

        item {
            // ==================== SAVED VISITORS SECTION ====================
            var showManageDialog by remember { mutableStateOf(false) }
            var showEditDialog by remember { mutableStateOf(false) }
            var selectedPerson by remember { mutableStateOf<SavedPerson?>(null) }

            Banner(
                "Saved Visitors",
                color = Color(0xFF7E57C2),
                icon = Icons.Filled.Person,
                trailing = {
                    if (savedPeople.isNotEmpty()) {
                        Button(
                            onClick = { showManageDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF7E57C2)
                            )
                        ) { Text("Manage") }
                    }
                }
            )

            if (savedPeople.isEmpty()) {
                Text(
                    "No saved visitors.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    savedPeople.forEach { person ->
                        ElevatedCard(
                            modifier = Modifier
                                .widthIn(min = 140.dp)
                                .clickable {
                                    // Prefill Add Visitor form with saved person's data
                                    visitorName = person.name
                                    pickedUri = Uri.parse(person.idImageUri)
                                    arrival = null
                                    departure = null
                                    saveForReuse = false // hide checkbox
                                    showAdd = true
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }

//
// MANAGE SAVED VISITORS DIALOG
//
            if (showManageDialog) {
                AlertDialog(
                    onDismissRequest = { showManageDialog = false },
                    title = { Text("Manage Saved Visitors") },
                    text = {
                        Column {
                            if (savedPeople.isEmpty()) {
                                Text("No saved visitors.", modifier = Modifier.padding(8.dp))
                            } else {
                                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                                    items(savedPeople, key = { it.id }) { person ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                person.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        selectedPerson = person
                                                        showEditDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = FalTeal,
                                                        contentColor = Color.White
                                                    )
                                                ) { Text("Edit") }

                                                Button(
                                                    onClick = {
                                                        val updated = savedPeople.filterNot { it.id == person.id }
                                                        savedPeople = updated
                                                        scope.launch { saveSavedPeople(context, updated) }
                                                        Toast.makeText(context, "${person.name} deleted", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color.Red,
                                                        contentColor = Color.White
                                                    )
                                                ) { Text("Delete") }
                                            }
                                        }
                                        Divider()
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = { showManageDialog = false }) { Text("Close") }
                    }
                )
            }

//
// EDIT SAVED PERSON DIALOG
//
            if (showEditDialog && selectedPerson != null) {
                var newName by remember { mutableStateOf(selectedPerson!!.name) }
                var newIdUri by remember { mutableStateOf(Uri.parse(selectedPerson!!.idImageUri)) }

                val pickNewId = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        newIdUri = ensurePrivateImage(context, uri, "visitor_images")
                        if (newIdUri == null) {
                            Toast.makeText(context, "Couldn't save the ID image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Edit Saved Visitor") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (newIdUri != null) "ID image selected" else "No ID selected",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(onClick = { pickNewId.launch(arrayOf("image/*")) }) {
                                    Text("Change ID")
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val updatedList = savedPeople.map {
                                    if (it.id == selectedPerson!!.id)
                                        it.copy(
                                            name = newName.trim(),
                                            idImageUri = newIdUri?.toString() ?: it.idImageUri
                                        )
                                    else it
                                }
                                savedPeople = updatedList
                                scope.launch { saveSavedPeople(context, updatedList) }
                                Toast.makeText(context, "Visitor updated", Toast.LENGTH_SHORT).show()
                                showEditDialog = false
                                showManageDialog = false
                            }
                        ) { Text("Save") }
                    },
                    dismissButton = {
                        Button(onClick = { showEditDialog = false }) { Text("Cancel") }
                    }
                )
            }

        }

        item {
            Banner("Today", color = FalGreen, icon = Icons.Filled.Schedule)
            if (todays.isEmpty()) {
                Text("No visitors today.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            } else {
                todays.forEach { v ->
                    VisitorRow(
                        context = context,
                        profile = profile,
                        v = v,
                        fmt12 = fmt12,
                        onEdit = { editVisit = it },
                        onDelete = { del ->
                            val updated = visitors.filterNot { it.id == del.id }
                            visitors = updated
                            scope.launch { saveVisitors(context, updated) }
                        },
                        onDuplicateTomorrow = { base ->
                            val newV = base.copy(
                                id = UUID.randomUUID().toString(),
                                date = LocalDate.parse(base.date).plusDays(1).toString()
                            )
                            val updated: List<Visitor> = visitors + newV
                            visitors = updated
                            scope.launch { saveVisitors(context, updated) }
                            runCatching { scheduleReminder(context, newV) }
                        }
                    )
                }
            }
        }

        item {
            Banner("Upcoming", color = TealAccent, icon = Icons.Filled.Schedule)
            if (upcoming.isEmpty()) {
                Text("No upcoming visitors.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            } else {
                upcoming.take(20).forEach { v ->
                    VisitorRow(
                        context = context,
                        profile = profile,
                        v = v,
                        fmt12 = fmt12,
                        onEdit = { editVisit = it },
                        onDelete = { del ->
                            val updated = visitors.filterNot { it.id == del.id }
                            visitors = updated
                            scope.launch { saveVisitors(context, updated) }
                        },
                        onDuplicateTomorrow = { base ->
                            val newV = base.copy(
                                id = UUID.randomUUID().toString(),
                                date = LocalDate.parse(base.date).plusDays(1).toString()
                            )
                            val updated: List<Visitor> = visitors + newV
                            visitors = updated
                            scope.launch { saveVisitors(context, updated) }
                            runCatching { scheduleReminder(context, newV) }
                        }
                    )
                }
            }
        }

        item {
            Banner("Previous", color = FalDeep, icon = Icons.Filled.Schedule)
            if (past.isEmpty()) {
                Text("No previous visitors.", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            } else {
                past.takeLast(20).forEach { v ->
                    VisitorRow(
                        context = context,
                        profile = profile,
                        v = v,
                        fmt12 = fmt12,
                        onEdit = { editVisit = it },
                        onDelete = { del ->
                            val updated = visitors.filterNot { it.id == del.id }
                            visitors = updated
                            scope.launch { saveVisitors(context, updated) }
                        },
                        onDuplicateTomorrow = { base ->
                            val newV = base.copy(
                                id = UUID.randomUUID().toString(),
                                date = LocalDate.parse(base.date).plusDays(1).toString()
                            )
                            val updated: List<Visitor> = visitors + newV
                            visitors = updated
                            scope.launch { saveVisitors(context, updated) }
                            runCatching { scheduleReminder(context, newV) }
                        }
                    )
                }
            }
        }
    }

    // Add Visit dialog
    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add Visit") },
            text = {
                Column {
                    OutlinedTextField(
                        value = visitorName,
                        onValueChange = { visitorName = it },
                        label = { Text("Visitor Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    // ID picker row
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (pickedUri != null) "ID Selected" else "No ID image selected")
                        Button(onClick = { pickVisitorId.launch(arrayOf("image/*")) }) {
                            Text("Pick ID")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Arrival field
                    OutlinedButton(
                        onClick = {
                            val now = LocalTime.now()
                            TimePickerDialog(
                                context,
                                { _, h, m -> arrival = LocalTime.of(h, m) },
                                now.hour,
                                now.minute,
                                false
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Arrival: " + (arrival?.format(fmt12) ?: "--:--"))
                    }

                    Spacer(Modifier.height(8.dp))

                    // Departure field
                    OutlinedButton(
                        onClick = {
                            val now = LocalTime.now().plusHours(2)
                            TimePickerDialog(
                                context,
                                { _, h, m -> departure = LocalTime.of(h, m) },
                                now.hour,
                                now.minute,
                                false
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Departure: " + (departure?.format(fmt12) ?: "--:--"))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Show the checkbox only when adding manually
                    if (saveForReuse) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = saveForReuse, onCheckedChange = { saveForReuse = it })
                            Spacer(Modifier.width(8.dp))
                            Text("Save person for reuse")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val n = visitorName.trim()
                    val src = pickedUri
                    val a = arrival
                    val d = departure
                    if (n.isBlank() || src == null || a == null || d == null) return@Button

                    // Guarantee a private copy
                    val priv = ensurePrivateImage(context, src, "visitor_images")
                    if (priv == null) {
                        Toast.makeText(context, "Couldn't save the ID image", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val v = Visitor(
                        name = n,
                        idImageUri = priv.toString(),
                        arrival = a.format(fmt24),
                        departure = d.format(fmt24),
                        date = LocalDate.now().toString()
                    )

                    val newList: List<Visitor> = visitors + v
                    visitors = newList
                    scope.launch { saveVisitors(context, newList) }

                    if (saveForReuse) {
                        val exists = savedPeople.any { it.name.equals(n, true) && it.idImageUri == priv.toString() }
                        if (!exists) {
                            val sp = savedPeople + SavedPerson(name = n, idImageUri = priv.toString())
                            savedPeople = sp
                            scope.launch { saveSavedPeople(context, sp) }
                        }
                    }

                    runCatching { scheduleReminder(context, v) }

                    showAdd = false
                    visitorName = ""; pickedUri = null; arrival = null; departure = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }


    // Edit visit dialog
    editVisit?.let { toEdit ->
        EditVisitDialog(
            context = context,
            visitor = toEdit,
            onSave = { updated ->
                val newList: List<Visitor> = visitors.map { if (it.id == updated.id) updated else it }
                visitors = newList
                scope.launch { saveVisitors(context, newList) }
                runCatching { scheduleReminder(context, updated) }
                editVisit = null
            },
            onDelete = { del ->
                val newList: List<Visitor> = visitors.filterNot { it.id == del.id }
                visitors = newList
                scope.launch { saveVisitors(context, newList) }
                editVisit = null
            },
            onDismiss = { editVisit = null }
        )
    }

    // Manage saved people dialog
    if (managePeople) {
        ManageSavedPeopleDialog(
            context = context,
            people = savedPeople,
            onSaveAll = { newList ->
                savedPeople = newList
                scope.launch { saveSavedPeople(context, newList) }
            },
            onDismiss = { managePeople = false }
        )
    }
}

@Composable
private fun VisitorRow(
    context: Context,
    profile: Profile,
    v: Visitor,
    fmt12: DateTimeFormatter,
    onEdit: (Visitor) -> Unit,
    onDelete: (Visitor) -> Unit,
    onDuplicateTomorrow: (Visitor) -> Unit
) {
    val times: Pair<String, String> = runCatching {
        val a = LocalTime.parse(v.arrival, DateTimeFormatter.ofPattern("HH:mm")).format(fmt12)
        val d = LocalTime.parse(v.departure, DateTimeFormatter.ofPattern("HH:mm")).format(fmt12)
        a to d
    }.getOrElse { "--:--" to "--:--" }

    val fmtDate = LocalDate.parse(v.date).format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    var menuOpen by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(v.name, style = MaterialTheme.typography.titleMedium)
                Text("$fmtDate   ${times.first} → ${times.second}", style = MaterialTheme.typography.bodyMedium)
            }
            // Email + More side-by-side (not stacked)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val src = Uri.parse(v.idImageUri)
                val readable = isUriReadable(context, src)

                IconButton(onClick = {
                    if (!readable) {
                        Toast.makeText(
                            context,
                            "Visitor ID image not found. Please update it.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@IconButton
                    }
                    val attachment = copyToCacheForEmail(context, src) ?: run {
                        Toast.makeText(context, "Couldn’t attach the ID image.", Toast.LENGTH_LONG).show()
                        return@IconButton
                    }
                    val subject = "Visitor"
                    val body = """
                        Dear all,
                        I have a visitor coming to ${profile.apartment} at ${times.first} and will be leaving at ${times.second}

                        Thanks,
                        ${profile.name}
                    """.trimIndent()

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = context.contentResolver.getType(attachment) ?: "application/octet-stream"
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                        putExtra(Intent.EXTRA_STREAM, attachment)
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(SECURITY_EMAIL))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = ClipData.newRawUri("attachment", attachment)
                    }
                    context.startActivity(Intent.createChooser(intent, "Send email"))
                }, enabled = readable) {
                    Icon(Icons.Filled.Email, contentDescription = "Email", tint = if (readable) FalTeal else Color.Gray)
                }

                // Box only for the dropdown anchor; button sits in the same row
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = {
                            menuOpen = false
                            onEdit(v)
                        })
                        DropdownMenuItem(text = { Text("Duplicate for tomorrow") }, onClick = {
                            menuOpen = false
                            onDuplicateTomorrow(v)
                        })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = {
                            menuOpen = false
                            onDelete(v)
                        })
                    }
                }
            }
        }
    }
}


@Composable
private fun EditVisitDialog(
    context: Context,
    visitor: Visitor,
    onSave: (Visitor) -> Unit,
    onDelete: (Visitor) -> Unit,
    onDismiss: () -> Unit
) {
    val fmt24 = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val fmt12 = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    var name by remember { mutableStateOf(visitor.name) }
    var arrival by remember { mutableStateOf(runCatching { LocalTime.parse(visitor.arrival, fmt24) }.getOrNull()) }
    var departure by remember { mutableStateOf(runCatching { LocalTime.parse(visitor.departure, fmt24) }.getOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Visit") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val now = arrival ?: LocalTime.now()
                        TimePickerDialog(context, { _, h, m -> arrival = LocalTime.of(h, m) }, now.hour, now.minute, false).show()
                    }) { Text("Arrival: " + (arrival?.format(fmt12) ?: "--:--")) }
                    OutlinedButton(onClick = {
                        val now = departure ?: LocalTime.now().plusHours(1)
                        TimePickerDialog(context, { _, h, m -> departure = LocalTime.of(h, m) }, now.hour, now.minute, false).show()
                    }) { Text("Departure: " + (departure?.format(fmt12) ?: "--:--")) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val a = arrival; val d = departure
                if (name.isBlank() || a == null || d == null) return@Button
                val updated = visitor.copy(
                    name = name.trim(),
                    arrival = a.format(fmt24),
                    departure = d.format(fmt24)
                )
                onSave(updated)
            }) { Text("Save") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onDelete(visitor) }) { Text("Delete") }
                Button(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/* ===================== Manage Saved People dialog ==================== */

@Composable
private fun ManageSavedPeopleDialog(
    context: Context,
    people: List<SavedPerson>,
    onSaveAll: (List<SavedPerson>) -> Unit,
    onDismiss: () -> Unit
) {
    var list by remember { mutableStateOf(people) }
    var editing by remember { mutableStateOf<SavedPerson?>(null) }

    val pickIdForEdit = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && editing != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val e = editing!!
            val priv = ensurePrivateImage(context, uri, "visitor_images") ?: return@rememberLauncherForActivityResult
            list = list.map { if (it.id == e.id) it.copy(idImageUri = priv.toString()) else it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Saved People") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (list.isEmpty()) {
                    Text("No saved people.", modifier = Modifier.padding(8.dp))
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(list, key = { it.id }) { person ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(person.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Row {
                                    IconButton(onClick = { editing = person }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                                    IconButton(onClick = { list = list.filterNot { it.id == person.id } }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSaveAll(list); onDismiss() }) { Text("Done") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )

    editing?.let { person ->
        var name by remember { mutableStateOf(person.name) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit Saved Person") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { pickIdForEdit.launch(arrayOf("image/*")) }) { Text("Change ID Image") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val n = name.trim()
                    if (n.isBlank()) return@Button
                    val updated = list.map { if (it.id == person.id) it.copy(name = n) else it }
                    list = updated
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { Button(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

/* ============================ Profile =============================== */

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf(Profile()) }
    var editing by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var apartment by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(Unit) {
        profile = loadProfile(context)
        name = profile.name
        apartment = profile.apartment
        photoUri = profile.profilePhotoUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoUri = uri
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) exportAll(context, uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            importAll(context, uri)
            scope.launch {
                val fresh = loadProfile(context)
                profile = fresh
                name = fresh.name
                apartment = fresh.apartment
                photoUri = fresh.profilePhotoUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                Toast.makeText(context, "Import complete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {

        Banner(title = "Profile", color = FalGreen, icon = Icons.Filled.AccountCircle)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Center avatar
            ElevatedCard(
                modifier = Modifier
                    .size(112.dp)
                    .clickable(enabled = editing) { pickPhoto.launch(arrayOf("image/*")) }
            ) {
                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(56.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Name") }, singleLine = true, enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apartment, onValueChange = { apartment = it },
                label = { Text("Apartment (e.g., A3 115)") }, singleLine = true, enabled = editing,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (!editing) {
                Row(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { editing = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Edit") }
                }
            } else {
                // Row 1: Save / Cancel
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val updated = Profile(
                                name = name.trim(),
                                apartment = apartment.trim(),
                                profilePhotoUri = photoUri?.toString() ?: "",
                                myIdUri = profile.myIdUri
                            )
                            profile = updated
                            scope.launch {
                                saveProfile(context, updated)
                                Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
                            }
                            editing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }

                    OutlinedButton(
                        onClick = {
                            // revert
                            name = profile.name
                            apartment = profile.apartment
                            photoUri = profile.profilePhotoUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                            editing = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }

                Spacer(Modifier.height(8.dp))

                // Row 2: Export / Import
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("compound_backup.json") },
                        modifier = Modifier.weight(1f)
                    ) { Text("Export") }

                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Import") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Backup includes Profile, Directory, Saved People, and Visitors.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

/* ========================= Persistence utils ======================== */

private suspend fun loadProfile(context: Context): Profile {
    val prefs = context.dataStore.data.first()
    val json = prefs[KEY_PROFILE_JSON] ?: return Profile()
    return runCatching { Gson().fromJson(json, Profile::class.java) }.getOrDefault(Profile())
}
private suspend fun saveProfile(context: Context, profile: Profile) {
    context.dataStore.edit { it[KEY_PROFILE_JSON] = Gson().toJson(profile) }
}

private suspend fun loadContacts(context: Context): List<Contact> {
    val prefs = context.dataStore.data.first()
    val json = prefs[KEY_CONTACTS_JSON] ?: return emptyList()
    val type = object : TypeToken<List<Contact>>() {}.type
    return runCatching { Gson().fromJson<List<Contact>>(json, type) }.getOrDefault(emptyList())
}
private suspend fun saveContacts(context: Context, contacts: List<Contact>) {
    context.dataStore.edit { it[KEY_CONTACTS_JSON] = Gson().toJson(contacts) }
}

private suspend fun loadVisitors(context: Context): List<Visitor> {
    val prefs = context.dataStore.data.first()
    val json = prefs[KEY_VISITORS_JSON] ?: return emptyList()
    val type = object : TypeToken<List<Visitor>>() {}.type
    return runCatching { Gson().fromJson<List<Visitor>>(json, type) }.getOrDefault(emptyList())
}
private suspend fun saveVisitors(context: Context, visitors: List<Visitor>) {
    context.dataStore.edit { it[KEY_VISITORS_JSON] = Gson().toJson(visitors) }
}

private suspend fun loadSavedPeople(context: Context): List<SavedPerson> {
    val prefs = context.dataStore.data.first()
    val json = prefs[KEY_SAVED_PEOPLE_JSON] ?: return emptyList()
    val type = object : TypeToken<List<SavedPerson>>() {}.type
    return runCatching { Gson().fromJson<List<SavedPerson>>(json, type) }.getOrDefault(emptyList())
}
private suspend fun saveSavedPeople(context: Context, people: List<SavedPerson>) {
    context.dataStore.edit { it[KEY_SAVED_PEOPLE_JSON] = Gson().toJson(people) }
}

/* ========================= Email / phone helpers ==================== */

private fun copyToCacheForEmail(context: Context, source: Uri): Uri? {
    return try {
        val dir = File(context.cacheDir, "attachments").apply { mkdirs() }
        val fileName = resolveDisplayName(context.contentResolver, source) ?: "visitor_id.jpg"
        val outFile = File(dir, fileName)
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    } catch (_: Exception) {
        null
    }
}

private fun resolveDisplayName(resolver: ContentResolver, uri: Uri): String? {
    resolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx != -1 && c.moveToFirst()) return c.getString(idx)
    }
    return null
}

private fun isOurFileProviderUri(context: Context, uri: Uri?): Boolean {
    if (uri == null) return false
    return uri.authority == "${context.packageName}.fileprovider"
}

private fun dial(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$number") }
    context.startActivity(intent)
}

/* ============================== Reminders =========================== */

private fun scheduleReminder(context: Context, v: Visitor) {
    val parts = v.departure.split(":")
    if (parts.size != 2) return
    val h = parts[0].toIntOrNull() ?: return
    val m = parts[1].toIntOrNull() ?: return

    val date = runCatching { LocalDate.parse(v.date) }.getOrNull() ?: LocalDate.now()
    val depTime = LocalDateTime.of(date, LocalTime.of(h, m))
    val trigger = depTime.minusMinutes(10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val intent = Intent(context, ReminderReceiver::class.java).apply {
        putExtra("name", v.name)
        putExtra("time", v.departure)
    }
    val pi = PendingIntent.getBroadcast(
        context, v.id.hashCode(), intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    runCatching { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi) }
}

/* ============================ Backup / Restore ====================== */

private fun exportAll(context: Context, dest: Uri) {
    val gson = Gson()
    val data: Map<String, Any> = runBlocking {
        val p: Profile = loadProfile(context)
        val c: List<Contact> = loadContacts(context)
        val v: List<Visitor> = loadVisitors(context)
        val sp: List<SavedPerson> = loadSavedPeople(context)
        mapOf("profile" to p, "contacts" to c, "visitors" to v, "saved_people" to sp)
    }
    context.contentResolver.openOutputStream(dest)?.use { out -> out.write(gson.toJson(data).toByteArray()) }
}

private fun importAll(context: Context, src: Uri) {
    val json = context.contentResolver.openInputStream(src)?.use { it.readBytes().decodeToString() } ?: return
    val gson = Gson()

    val rootMapType = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = runCatching { gson.fromJson<Map<String, Any>>(json, rootMapType) }.getOrNull() ?: return

    runBlocking {
        root["profile"]?.let {
            val profile: Profile = gson.fromJson(gson.toJson(it), Profile::class.java)
            saveProfile(context, profile)
        }
        root["contacts"]?.let {
            val listType = object : TypeToken<List<Contact>>() {}.type
            val contacts: List<Contact> = gson.fromJson(gson.toJson(it), listType)
            saveContacts(context, contacts)
        }
        root["visitors"]?.let {
            val listType = object : TypeToken<List<Visitor>>() {}.type
            val visitors: List<Visitor> = gson.fromJson(gson.toJson(it), listType)
            saveVisitors(context, visitors)
        }
        root["saved_people"]?.let {
            val listType = object : TypeToken<List<SavedPerson>>() {}.type
            val people: List<SavedPerson> = gson.fromJson(gson.toJson(it), listType)
            saveSavedPeople(context, people)
        }
    }
}

/* ===================== Reminder notification receiver =============== */

class ReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: "Visitor"
        val time = intent.getStringExtra("time") ?: ""

        val notif = NotificationCompat.Builder(context, "visitors")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("Visitor departing soon")
            .setContentText("$name is scheduled to leave at $time")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify((name + time).hashCode(), notif)
    }
}

/* ======================= Image/URI safety helpers =================== */

private fun isUriReadable(context: Context, uri: Uri?): Boolean {
    if (uri == null) return false
    return try {
        context.contentResolver.openInputStream(uri).use { it != null }
    } catch (_: Exception) { false }
}

/** Copy a content Uri to app-private filesDir/<subdir>/ and return a FileProvider Uri. */
private fun copyToPrivateFile(
    context: Context,
    src: Uri,
    subdir: String,
    suggestedName: String = "image_${System.currentTimeMillis()}.jpg"
): Uri? {
    return try {
        val resolver = context.contentResolver
        val inS = resolver.openInputStream(src) ?: return null
        val dir = java.io.File(context.filesDir, subdir).apply { mkdirs() }
        val name = resolveDisplayName(resolver, src) ?: suggestedName
        val outFile = java.io.File(dir, name)
        inS.use { input ->
            java.io.FileOutputStream(outFile).use { out -> input.copyTo(out) }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    } catch (_: Exception) { null }
}

/** If already a FileProvider Uri -> return it, else copy to private and return that. */
private fun ensurePrivateImage(context: Context, uri: Uri?, subdir: String): Uri? {
    if (uri == null) return null
    return if (isOurFileProviderUri(context, uri)) uri
    else copyToPrivateFile(context, uri, subdir)
}
