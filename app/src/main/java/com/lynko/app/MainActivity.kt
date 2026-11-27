
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

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

private data class ChatTarget(
    val friend: UserProfile,
    val conversationId: String
)

private data class ConversationPreview(
    val id: String,
    val friend: UserProfile,
    val lastMessage: String,
    val lastMessageAt: Timestamp?
)

private data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val createdAt: Timestamp?
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
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var activeChat by remember { mutableStateOf<ChatTarget?>(null) }
    var openingChat by remember { mutableStateOf(false) }
    var openError by remember { mutableStateOf<String?>(null) }

    fun openConversation(friend: UserProfile) {
        if (uid == null || uid == friend.uid || openingChat) return

        openingChat = true
        openError = null

        val participants = listOf(uid, friend.uid).sorted()
        val conversationId = participants.joinToString("_")
        val conversationRef = db.collection("conversations")
            .document(conversationId)

        conversationRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    activeChat = ChatTarget(friend, conversationId)
                    openingChat = false
                } else {
                    val conversation = hashMapOf(
                        "participants" to participants,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastMessage" to "",
                        "lastMessageAt" to FieldValue.serverTimestamp()
                    )

                    conversationRef.set(conversation)
                        .addOnSuccessListener {
                            activeChat = ChatTarget(friend, conversationId)
                            openingChat = false
                        }
                        .addOnFailureListener {
                            openingChat = false
                            openError = "تعذر إنشاء المحادثة: ${it.localizedMessage}"
                        }
                }
            }
            .addOnFailureListener {
                openingChat = false
                openError = "تعذر فتح المحادثة: ${it.localizedMessage}"
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        activeChat?.friend?.username ?: "LYNKO",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (activeChat == null) 3.sp else 0.sp
                    )
                },
                navigationIcon = {
                    if (activeChat != null) {
                        IconButton(onClick = { activeChat = null }) {
                            Icon(Icons.Default.ArrowBack, "رجوع")
                        }
                    } else {
                        BrandMark(
                            Modifier.padding(start = 12.dp).size(34.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        activeChat = null
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, "تسجيل الخروج")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Paper
                )
            )
        },
        bottomBar = {
            if (activeChat == null) {
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
            }
        },
        containerColor = Paper
    ) { padding ->
        when {
            activeChat != null -> {
                ChatScreen(
                    target = activeChat!!,
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                Column(Modifier.fillMaxSize()) {
                    openError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (openingChat) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }

                    when (selected) {
                        Tab.Friends -> FriendsScreen(
                            Modifier.padding(padding),
                            onOpenChat = { openConversation(it) }
                        )

                        Tab.Chats -> ChatsScreen(
                            modifier = Modifier.padding(padding),
                            onOpenChat = { openConversation(it) }
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
        }
    }
}

@Composable
private fun ChatsScreen(
    modifier: Modifier = Modifier,
    onOpenChat: (UserProfile) -> Unit
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var conversations by remember {
        mutableStateOf<List<ConversationPreview>>(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uid) {
        if (uid == null) {
            loading = false
            onDispose {}
        } else {
            val registration = db.collection("conversations")
                .whereArrayContains("participants", uid)
                .addSnapshotListener { snapshot, exception ->
                    if (exception != null) {
                        error = "تعذر تحميل المحادثات: ${exception.localizedMessage}"
                        loading = false
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        loading = false
                        return@addSnapshotListener
                    }

                    val documents = snapshot.documents

                    if (documents.isEmpty()) {
                        conversations = emptyList()
                        loading = false
                        return@addSnapshotListener
                    }

                    val loaded = mutableListOf<ConversationPreview>()
                    var remaining = documents.size

                    documents.forEach { document ->
                        val participants = document.get("participants") as? List<*>
                        val friendId = participants
                            ?.filterIsInstance<String>()
                            ?.firstOrNull { it != uid }

                        if (friendId == null) {
                            remaining--
                            if (remaining == 0) {
                                conversations = loaded.sortedByDescending {
                                    it.lastMessageAt?.toDate()?.time ?: 0L
                                }
                                loading = false
                            }
                        } else {
                            db.collection("publicProfiles")
                                .document(friendId)
                                .get()
                                .addOnSuccessListener { profile ->
                                    val username = profile.getString("username")
                                        ?: "مستخدم LYNKO"

                                    loaded.add(
                                        ConversationPreview(
                                            id = document.id,
                                            friend = UserProfile(friendId, username),
                                            lastMessage = document.getString("lastMessage") ?: "",
                                            lastMessageAt = document.getTimestamp("lastMessageAt")
                                        )
                                    )

                                    remaining--

                                    if (remaining == 0) {
                                        conversations = loaded.sortedByDescending {
                                            it.lastMessageAt?.toDate()?.time ?: 0L
                                        }
                                        loading = false
                                        error = null
                                    }
                                }
                                .addOnFailureListener {
                                    remaining--
                                    if (remaining == 0) {
                                        conversations = loaded.sortedByDescending {
                                            it.lastMessageAt?.toDate()?.time ?: 0L
                                        }
                                        loading = false
                                    }
                                }
                        }
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    Column(
        modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            "المحادثات",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(14.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }

        if (!loading && conversations.isEmpty()) {
            EmptyState(
                Modifier.fillMaxWidth(),
                Icons.Default.ChatBubbleOutline,
                "لا توجد محادثات بعد",
                "اذهب إلى الأصدقاء واضغط مراسلة لبدء محادثتك الأولى."
            )
        } else {
            LazyColumn {
                items(conversations, key = { it.id }) { conversation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        onClick = {
                            onOpenChat(conversation.friend)
                        }
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
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

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    conversation.friend.username,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    conversation.lastMessage.ifBlank {
                                        "ابدأ المحادثة الآن"
                                    },
                                    color = Muted,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = Muted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen(
    target: ChatTarget,
    modifier: Modifier = Modifier
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var messages by remember {
        mutableStateOf<List<ChatMessage>>(emptyList())
    }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    DisposableEffect(target.conversationId) {
        val registration = db.collection("conversations")
            .document(target.conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    error = "تعذر تحميل الرسائل: ${exception.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    messages = snapshot.documents.mapNotNull { document ->
                        val sender = document.getString("senderId")
                            ?: return@mapNotNull null
                        val messageText = document.getString("text")
                            ?: return@mapNotNull null

                        ChatMessage(
                            id = document.id,
                            senderId = sender,
                            text = messageText,
                            createdAt = document.getTimestamp("createdAt")
                        )
                    }
                }
            }

        onDispose {
            registration.remove()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendMessage() {
        val messageText = text.trim()

        if (messageText.isEmpty() || sending || uid.isEmpty()) return

        sending = true
        error = null

        val conversationRef = db.collection("conversations")
            .document(target.conversationId)

        val message = hashMapOf(
            "senderId" to uid,
            "text" to messageText,
            "createdAt" to FieldValue.serverTimestamp()
        )

        conversationRef.collection("messages")
            .add(message)
            .addOnSuccessListener {
                text = ""

                conversationRef.update(
                    mapOf(
                        "lastMessage" to messageText,
                        "lastMessageAt" to FieldValue.serverTimestamp()
                    )
                ).addOnSuccessListener {
                    sending = false
                }.addOnFailureListener { exception ->
                    sending = false
                    error = "أُرسلت الرسالة، لكن تعذر تحديث آخر رسالة: ${exception.localizedMessage}"
                }
            }
            .addOnFailureListener { exception ->
                sending = false
                error = "تعذر إرسال الرسالة: ${exception.localizedMessage}"
            }
    }

    Column(
        modifier = modifier.fillMaxSize().background(Paper)
    ) {
        Row(
            Modifier.fillMaxWidth().background(Color.White).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
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

            Column {
                Text(
                    target.friend.username,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "محادثة خاصة",
                    color = Muted,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = Color(0xFFEAEAEA))

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(10.dp),
                fontSize = 12.sp
            )
        }

        if (messages.isEmpty()) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        null,
                        tint = Muted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "ابدأ المحادثة",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "أرسل أول رسالة إلى ${target.friend.username}",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val mine = message.senderId == uid

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) {
                            Arrangement.End
                        } else {
                            Arrangement.Start
                        }
                    ) {
                        Column(
                            horizontalAlignment = if (mine) {
                                Alignment.End
                            } else {
                                Alignment.Start
                            }
                        ) {
                            Surface(
                                color = if (mine) Ink else Color.White,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (mine) 16.dp else 4.dp,
                                    bottomEnd = if (mine) 4.dp else 16.dp
                                ),
                                shadowElevation = 1.dp
                            ) {
                                Text(
                                    message.text,
                                    color = if (mine) Color.White else Ink,
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),
                                    fontSize = 15.sp
                                )
                            }

                            message.createdAt?.let { timestamp ->
                                Text(
                                    SimpleDateFormat(
                                        "hh:mm a",
                                        Locale.getDefault()
                                    ).format(timestamp.toDate()),
                                    color = Muted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(
                                        top = 3.dp,
                                        start = 4.dp,
                                        end = 4.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFEAEAEA))

        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("اكتب رسالة...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = !sending
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = { sendMessage() },
                enabled = text.isNotBlank() && !sending,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (text.isNotBlank() && !sending) Ink else Muted,
                        CircleShape
                    )
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        "إرسال",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendsScreen(
    modifier: Modifier = Modifier,
    onOpenChat: (UserProfile) -> Unit
) {
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
                    val sender = doc.getString("senderId")
                        ?: return@mapNotNull null
                    val recipient = doc.getString("recipientId")
                        ?: return@mapNotNull null

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
                            val sender = doc.getString("senderId")
                                ?: return@mapNotNull null
                            val recipient = doc.getString("recipientId")
                                ?: return@mapNotNull null

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
            listOf("بحث", "الواردة", "المرسلة", "أصدقائي")
                .forEachIndexed { index, title ->
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
                                if (uid != null) {
                                    loading = true
                                    message = null

                                    db.collection("friendRequests")
                                        .whereEqualTo("senderId", uid)
                                        .get()
                                        .addOnSuccessListener { sent ->
                                            val exists = sent.documents.any {
                                                it.getString("recipientId") == user.uid &&
                                                    it.getString("status") in
                                                    listOf("pending", "accepted")
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
                            actionText = "مراسلة",
                            icon = Icons.Default.Chat,
                            onAction = {
                                onOpenChat(friend)
                            }
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
