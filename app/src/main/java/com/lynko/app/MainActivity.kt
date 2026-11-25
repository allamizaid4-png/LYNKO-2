
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

@Composable
private fun LynkoApp() {
    val auth = remember { FirebaseAuth.getInstance() }

    var screen by remember {
        mutableStateOf(Screen.Splash)
    }

    var selectedTab by remember {
        mutableStateOf(Tab.Chats)
    }

    var notice by remember {
        mutableStateOf<String?>(null)
    }

    var createAccount by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        screen = if (auth.currentUser != null) {
            Screen.Home
        } else {
            Screen.Welcome
        }
    }

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
                    Screen.Splash -> SplashScreen()

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
                        onBack = {
                            screen = Screen.Welcome
                        },
                        onSuccess = {
                            screen = Screen.Home
                        }
                    )

                    Screen.Home -> HomeScreen(
                        selected = selectedTab,
                        onSelect = { selectedTab = it },
                        onLogout = {
                            auth.signOut()
                            screen = Screen.Welcome
                        }
                    )
                }
            }

            notice?.let { message ->
                AlertDialog(
                    onDismissRequest = { notice = null },
                    confirmButton = {
                        TextButton(onClick = {
                            notice = null
                        }) {
                            Text("حسناً")
                        }
                    },
                    title = { Text("LYNKO") },
                    text = { Text(message) }
                )
            }
        }
    }
}

@Composable
private fun BrandMark(
    modifier: Modifier = Modifier
) {
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
        Modifier
            .fillMaxSize()
            .background(Ink),
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
        Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(28.dp),
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

        Text(
            "اختر رحلتك",
            color = Color.White,
            fontSize = 23.sp
        )

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text("لدي حساب بالفعل")
        }
    }
}

@Composable
private fun AuthScreen(
    initialCreate: Boolean,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    var create by remember {
        mutableStateOf(initialCreate)
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
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
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "ابدأ رحلتك مع LYNKO",
            color = Muted
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
            visualTransformation =
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                error = null

                if (email.isBlank() ||
                    password.isBlank()
                ) {
                    error = "أدخل البريد الإلكتروني وكلمة المرور"
                    return@Button
                }

                if (create && username.isBlank()) {
                    error = "أدخل اسم المستخدم"
                    return@Button
                }

                if (create && password.length < 6) {
                    error = "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
                    return@Button
                }

                loading = true

                if (create) {
                    auth.createUserWithEmailAndPassword(
                        email,
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
                            "email" to email,
                            "username" to username.trim(),
                            "createdAt" to
                                com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )

                        db.collection("users")
                            .document(user.uid)
                            .set(profile)
                            .addOnCompleteListener { saveTask ->
                                loading = false

                                if (!saveTask.isSuccessful) {
                                    error = "تم إنشاء الحساب، لكن تعذر حفظ الملف الشخصي. راجع قواعد Firestore."
                                }

                                onSuccess()
                            }
                    }
                } else {
                    auth.signInWithEmailAndPassword(
                        email,
                        password
                    ).addOnCompleteListener { task ->
                        loading = false

                        if (task.isSuccessful) {
                            onSuccess()
                        } else {
                            error = task.exception?.localizedMessage
                                ?: "البريد أو كلمة المرور غير صحيحة"
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White
                )
            } else {
                Text(
                    if (create) "إنشاء الحساب" else "دخول"
                )
            }
        }

        TextButton(
            onClick = {
                create = !create
                error = null
            },
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        ) {
            Text(
                if (create)
                    "لديك حساب؟ سجل الدخول"
                else
                    "مستخدم جديد؟ أنشئ حساباً"
            )
        }
    }
}

@Composable
private fun HomeScreen(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBarCompat(onLogout)
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
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
                        icon = {
                            Icon(icon, tab.title)
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        },
        containerColor = Paper
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(22.dp)
        ) {
            Text(
                selected.title,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(Modifier.height(10.dp))

            when (selected) {
                Tab.Chats -> EmptyState(
                    Icons.Default.ChatBubbleOutline,
                    "محادثاتك ستظهر هنا",
                    "قريباً ستتمكن من مراسلة أصدقائك."
                )

                Tab.Friends -> EmptyState(
                    Icons.Default.PersonAdd,
                    "قائمة الأصدقاء",
                    "ابحث عن المستخدمين وأرسل طلبات الصداقة."
                )

                Tab.Groups -> EmptyState(
                    Icons.Default.Groups,
                    "مجموعاتك",
                    "تواصل مع أصدقائك في المجموعات."
                )

                Tab.Profile -> EmptyState(
                    Icons.Default.AccountCircle,
                    "ملفك الشخصي",
                    "إعدادات الحساب والصورة والحالة."
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarCompat(
    onLogout: () -> Unit
) {
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
                Modifier
                    .padding(start = 12.dp)
                    .size(34.dp)
            )
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(
                    Icons.Default.Logout,
                    "تسجيل الخروج"
                )
            }
        },
        colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Paper
            )
    )
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
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
