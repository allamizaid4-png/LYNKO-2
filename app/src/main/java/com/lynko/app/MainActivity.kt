
package com.lynko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

private val Ink = Color(0xFF101010)
private val Paper = Color(0xFFF7F7F7)
private val Muted = Color(0xFF777777)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LynkoApp() }
    }
}

private enum class Screen { Splash, Welcome, Auth, Home }

private enum class Tab(val title: String) {
    Chats("المحادثات"),
    Friends("الأصدقاء"),
    Groups("المجموعات"),
    Profile("حسابي")
}

private data class UserProfile(
    val uid: String,
    val username: String
)

private data class FriendRequest(
    val id: String,
    val senderId: String,
    val recipientId: String,
    val status: String
)

@Composable
private fun LynkoApp() {
    val auth = remember { FirebaseAuth.getInstance() }

    var screen by remember { mutableStateOf(Screen.Splash) }
    var selectedTab by remember { mutableStateOf(Tab.Chats) }
    var createAccount by remember { mutableStateOf(true) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            onPrimary = Color.White,
            background = Paper,
            surface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Paper
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "lynko-screen"
            ) { target ->
                when (target) {
                    Screen.Splash -> {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1200)
                            screen = if (auth.currentUser != null) {
                                Screen.Home
                            } else {
                                Screen.Welcome
                            }
                        }
                        SplashScreen()
                    }

                    Screen.Welcome -> WelcomeScreen(
                        onContinue = {
                            createAccount = true
                            screen = Screen.Auth
                        },
                        onExisting = {
                            createAccount = false
                            screen = Screen.Auth
                        }
                    )

                    Screen.Auth -> AuthScreen(
                        initialCreate = createAccount,
                        onBack = { screen = Screen.Welcome },
                        onSuccess = { screen = Screen.Home }
                    )

                    Screen.Home -> HomeScreen(
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                        onLogout = {
                            auth.signOut()
                            selectedTab = Tab.Chats
                            screen = Screen.Welcome
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(112.dp)
            .background(Ink, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.AllInclusive,
            null,
            tint = Color.LightGray,
            modifier = Modifier.fillMaxSize(0.6f)
        )
    }
}

@Composable
private fun SplashScreen() {
    Column(
        Modifier.fillMaxSize().background(Ink),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark()
        Spacer(Modifier.height(22.dp))
        Text(
            "LYNKO",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Connect Without Limits",
            color = Color.LightGray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun WelcomeScreen(
    onContinue: () -> Unit,
    onExisting: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Ink).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark()
        Spacer(Modifier.height(24.dp))
        Text("مرحباً بك في", color = Color.LightGray)
        Text(
            "LYNKO",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        Spacer(Modifier.height(40.dp))
        Text("اختر رحلتك", color = Color.White, fontSize = 23.sp)
        Spacer(Modifier.height(22.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Ink
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("إنشاء حساب جديد")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onExisting,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text("لدي حساب بالفعل")
        }
    }
}

private fun ensurePublicProfile(
    db: FirebaseFirestore,
    uid: String,
    callback: (String?) -> Unit
) {
    val publicRef = db.collection("publicProfiles").document(uid)

    publicRef.get().addOnSuccessListener { existing ->
        if (existing.exists()) {
            callback(null)
            return@addOnSuccessListener
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { user ->
                val username = user.getString("username")

                if (username.isNullOrBlank()) {
                    callback("لم يتم العثور على اسم المستخدم.")
                    return@addOnSuccessListener
                }

                val profile = hashMapOf(
                    "uid" to uid,
                    "username" to username,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                publicRef.set(profile)
                    .addOnSuccessListener { callback(null) }
                    .addOnFailureListener {
                        callback("تعذر إنشاء الملف العام: ${it.localizedMessage}")
                    }
            }
            .addOnFailureListener {
                callback("تعذر قراءة ملف الحساب: ${it.localizedMessage}")
            }
    }.addOnFailureListener {
        callback("تعذر التحقق من الملف العام: ${it.localizedMessage}")
    }
}

@Composable
private fun AuthScreen(
    initialCreate: Boolean,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(initialCreate) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, null)
            Spacer(Modifier.width(6.dp))
            Text("رجوع")
        }

        Text(
            if (create) "إنشاء حسابك" else "تسجيل الدخول",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))

        if (create) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("اسم المستخدم") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("البريد الإلكتروني") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("كلمة المرور") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                error = null

                if (email.isBlank() || password.isBlank()) {
                    error = "أدخل البريد الإلكتروني وكلمة المرور"
                    return@Button
                }

                if (create && username.trim().length !in 3..24) {
                    error = "اسم المستخدم يجب أن يكون بين 3 و24 حرفاً"
                    return@Button
                }

                if (create && password.length < 6) {
                    error = "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                    return@Button
                }

                loading = true

                if (create) {
                    auth.createUserWithEmailAndPassword(
                        email.trim(),
                        password
                    ).addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            loading = false
                            error = task.exception?.localizedMessage
                                ?: "تعذر إنشاء الحساب"
                            return@addOnCompleteListener
                        }

                        val user = auth.currentUser
                        if (user == null) {
                            loading = false
                            error = "تعذر الحصول على بيانات الحساب"
                            return@addOnCompleteListener
                        }

                        val profile = hashMapOf(
                            "uid" to user.uid,
                            "email" to email.trim(),
                            "username" to username.trim(),
                            "createdAt" to FieldValue.serverTimestamp()
                        )

                        db.collection("users").document(user.uid)
                            .set(profile)
                            .addOnSuccessListener {
                                ensurePublicProfile(db, user.uid) { problem ->
                                    loading = false
                                    if (problem == null) {
                                        onSuccess()
                                    } else {
                                        error = problem
                                    }
                                }
                            }
                            .addOnFailureListener {
                                loading = false
                                error = "تعذر حفظ الحساب: ${it.localizedMessage}"
                            }
                    }
                } else {
                    auth.signInWithEmailAndPassword(
                        email.trim(),
                        password
                    ).addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            loading = false
                            error = task.exception?.localizedMessage
                                ?: "البريد أو كلمة المرور غير صحيحة"
                            return@addOnCompleteListener
                        }

                        val uid = auth.currentUser?.uid
                        if (uid == null) {
                            loading = false
                            error = "تعذر تحديد الحساب"
                            return@addOnCompleteListener
                        }

                        ensurePublicProfile(db, uid) { problem ->
                            loading = false
                            if (problem == null) {
                                onSuccess()
                            } else {
                                error = problem
                            }
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White
                )
            } else {
                Text(if (create) "إنشاء الحساب" else "دخول")
            }
        }

        TextButton(
            onClick = {
                create = !create
                error = null
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (create) "لديك حساب؟ سجل الدخول"
                else "مستخدم جديد؟ أنشئ حساباً"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "LYNKO",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                },
                navigationIcon = {
                    BrandMark(
                        Modifier.padding(start = 12.dp).size(34.dp)
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, "تسجيل الخروج")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Paper
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.values().forEach { tab ->
                    val icon = when (tab) {
                        Tab.Chats -> Icons.Default.Chat
                        Tab.Friends -> Icons.Default.People
                        Tab.Groups -> Icons.Default.Groups
                        Tab.Profile -> Icons.Default.Person
                    }

                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = { onSelect(tab) },
                        icon = { Icon(icon, tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        containerColor = Paper
    ) { padding ->
        when (selected) {
            Tab.Friends -> FriendsScreen(
                Modifier.padding(padding)
            )

            Tab.Chats -> EmptyState(
                Modifier.padding(padding),
                Icons.Default.ChatBubbleOutline,
                "محادثاتك",
                "ستظهر محادثاتك هنا."
            )

            Tab.Groups -> EmptyState(
                Modifier.padding(padding),
                Icons.Default.Groups,
                "مجموعاتك",
                "تواصل مع أصدقائك في المجموعات."
            )

            Tab.Profile -> EmptyState(
                Modifier.padding(padding),
                Icons.Default.AccountCircle,
                "ملفك الشخصي",
                "إعدادات الحساب والصورة والحالة."
            )
        }
    }
}

@Composable
private fun FriendsScreen(modifier: Modifier = Modifier) {
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var searchText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var incoming by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var outgoing by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var friends by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var section by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        if (uid == null) return

        loading = true

        db.collection("friendRequests")
            .whereEqualTo("recipientId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                incoming = snapshot.documents.mapNotNull { doc ->
                    val sender = doc.getString("senderId") ?: return@mapNotNull null
                    val recipient = doc.getString("recipientId") ?: return@mapNotNull null
                    FriendRequest(
                        doc.id,
                        sender,
                        recipient,
                        doc.getString("status") ?: ""
                    )
                }.filter { it.status == "pending" }

                db.collection("friendRequests")
                    .whereEqualTo("senderId", uid)
                    .get()
                    .addOnSuccessListener { sent ->
                        outgoing = sent.documents.mapNotNull { doc ->
                            val sender = doc.getString("senderId") ?: return@mapNotNull null
                            val recipient = doc.getString("recipientId") ?: return@mapNotNull null
                            FriendRequest(
                                doc.id,
                                sender,
                                recipient,
                                doc.getString("status") ?: ""
                            )
                        }.filter { it.status == "pending" }

                        val sentFriends = sent.documents.mapNotNull { doc ->
                            if (doc.getString("status") == "accepted") {
                                doc.getString("recipientId")
                            } else null
                        }

                        db.collection("friendRequests")
                            .whereEqualTo("recipientId", uid)
                            .get()
                            .addOnSuccessListener { received ->
                                val receivedFriends = received.documents.mapNotNull { doc ->
                                    if (doc.getString("status") == "accepted") {
                                        doc.getString("senderId")
                                    } else null
                                }

                                val friendIds = (sentFriends + receivedFriends).distinct()

                                if (friendIds.isEmpty()) {
                                    friends = emptyList()
                                    loading = false
                                } else {
                                    val loaded = mutableListOf<UserProfile>()
                                    var remaining = friendIds.size

                                    friendIds.forEach { friendId ->
                                        db.collection("publicProfiles")
                                            .document(friendId)
                                            .get()
                                            .addOnSuccessListener { profile ->
                                                val name = profile.getString("username")
                                                    ?: "مستخدم LYNKO"
                                                loaded.add(UserProfile(friendId, name))
                                                remaining--

                                                if (remaining == 0) {
                                                    friends = loaded.sortedBy {
                                                        it.username.lowercase()
                                                    }
                                                    loading = false
                                                }
                                            }
                                            .addOnFailureListener {
                                                remaining--
                                                if (remaining == 0) {
                                                    friends = loaded
                                                    loading = false
                                                }
                                            }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                loading = false
                                message = "تعذر تحميل الأصدقاء: ${it.localizedMessage}"
                            }
                    }
                    .addOnFailureListener {
                        loading = false
                        message = "تعذر تحميل الطلبات المرسلة: ${it.localizedMessage}"
                    }
            }
            .addOnFailureListener {
                loading = false
                message = "تعذر تحميل الطلبات: ${it.localizedMessage}"
            }
    }

    LaunchedEffect(uid) {
        refresh()
    }

    Column(
        modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            "الأصدقاء",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("بحث", "الواردة", "المرسلة", "أصدقائي").forEachIndexed { index, title ->
                FilterChip(
                    selected = section == index,
                    onClick = { section = index },
                    label = { Text(title) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { message = null }) {
                Text("إغلاق")
            }
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        when (section) {
            0 -> {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم المستخدم الكامل") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val term = searchText.trim()
                                if (term.length < 3) {
                                    message = "أدخل 3 أحرف على الأقل."
                                } else {
                                    loading = true
                                    message = null

                                    db.collection("publicProfiles")
                                        .whereEqualTo("username", term)
                                        .limit(20)
                                        .get()
                                        .addOnSuccessListener { snapshot ->
                                            results = snapshot.documents.mapNotNull { doc ->
                                                val name = doc.getString("username")
                                                    ?: return@mapNotNull null
                                                if (doc.id == uid) null
                                                else UserProfile(doc.id, name)
                                            }
                                            loading = false
                                        }
                                        .addOnFailureListener {
                                            loading = false
                                            message = "فشل البحث: ${it.localizedMessage}"
                                        }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, "بحث")
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn {
                    items(results, key = { it.uid }) { user ->
                        UserRow(
                            user = user,
                            actionText = "إضافة",
                            icon = Icons.Default.PersonAdd,
                            onAction = {
                                if (uid == null) return@UserRow

                                loading = true
                                message = null

                                db.collection("friendRequests")
                                    .whereEqualTo("senderId", uid)
                                    .get()
                                    .addOnSuccessListener { sent ->
                                        val exists = sent.documents.any {
                                            it.getString("recipientId") == user.uid &&
                                                it.getString("status") in listOf("pending", "accepted")
                                        }

                                        if (exists) {
                                            loading = false
                                            message = "سبق إرسال طلب لهذا المستخدم أو أنكما صديقان."
                                        } else {
                                            val request = hashMapOf(
                                                "senderId" to uid,
                                                "recipientId" to user.uid,
                                                "status" to "pending",
                                                "createdAt" to FieldValue.serverTimestamp()
                                            )

                                            db.collection("friendRequests")
                                                .add(request)
                                                .addOnSuccessListener {
                                                    loading = false
                                                    message = "تم إرسال طلب الصداقة."
                                                    refresh()
                                                }
                                                .addOnFailureListener {
                                                    loading = false
                                                    message = "تعذر إرسال الطلب: ${it.localizedMessage}"
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        loading = false
                                        message = "تعذر التحقق من الطلبات السابقة."
                                    }
                            }
                        )
                    }
                }
            }

            1 -> {
                LazyColumn {
                    items(incoming, key = { it.id }) { request ->
                        RequestRow(
                            userId = request.senderId,
                            positiveAction = "قبول",
                            negativeAction = "رفض",
                            onPositive = {
                                db.collection("friendRequests")
                                    .document(request.id)
                                    .update("status", "accepted")
                                    .addOnSuccessListener {
                                        message = "تم قبول طلب الصداقة."
                                        refresh()
                                    }
                                    .addOnFailureListener {
                                        message = "تعذر قبول الطلب: ${it.localizedMessage}"
                                    }
                            },
                            onNegative = {
                                db.collection("friendRequests")
                                    .document(request.id)
                                    .update("status", "rejected")
                                    .addOnSuccessListener {
                                        message = "تم رفض الطلب."
                                        refresh()
                                    }
                                    .addOnFailureListener {
                                        message = "تعذر رفض الطلب: ${it.localizedMessage}"
                                    }
                            }
                        )
                    }
                }
                if (incoming.isEmpty()) {
                    EmptyMessage("لا توجد طلبات واردة حالياً.")
                }
            }

            2 -> {
                LazyColumn {
                    items(outgoing, key = { it.id }) { request ->
                        UserRow(
                            user = UserProfile(
                                request.recipientId,
                                "مستخدم ${request.recipientId.take(6)}"
                            ),
                            actionText = "قيد الانتظار",
                            icon = Icons.Default.Schedule,
                            onAction = {}
                        )
                    }
                }
                if (outgoing.isEmpty()) {
                    EmptyMessage("لا توجد طلبات مرسلة.")
                }
            }

            3 -> {
                LazyColumn {
                    items(friends, key = { it.uid }) { friend ->
                        UserRow(
                            user = friend,
                            actionText = "صديق",
                            icon = Icons.Default.CheckCircle,
                            onAction = {}
                        )
                    }
                }
                if (friends.isEmpty()) {
                    EmptyMessage("لم تضف أي أصدقاء بعد.")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = { refresh() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(6.dp))
            Text("تحديث البيانات")
        }
    }
}

@Composable
private fun UserRow(
    user: UserProfile,
    actionText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onAction: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Ink
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(user.username, fontWeight = FontWeight.SemiBold)
                Text(
                    user.uid.take(8),
                    fontSize = 11.sp,
                    color = Muted
                )
            }

            TextButton(onClick = onAction) {
                Icon(icon, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text(actionText)
            }
        }
    }
}

@Composable
private fun RequestRow(
    userId: String,
    positiveAction: String,
    negativeAction: String,
    onPositive: () -> Unit,
    onNegative: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "مستخدم ${userId.take(8)}",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPositive) {
                    Text(positiveAction)
                }
                OutlinedButton(onClick = onNegative) {
                    Text(negativeAction)
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(
        Modifier.fillMaxWidth().padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier.fillMaxWidth().padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            tint = Muted,
            modifier = Modifier.size(54.dp)
        )

        Spacer(Modifier.height(18.dp))

        Text(
            title,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            subtitle,
            color = Muted,
            textAlign = TextAlign.Center
        )
    }
}
