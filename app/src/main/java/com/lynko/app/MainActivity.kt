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
import kotlinx.coroutines.delay

private val Ink = Color(0xFF101010)
private val Paper = Color(0xFFF7F7F7)
private val Muted = Color(0xFF777777)
private val Gold = Color(0xFFC7A65A)

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
    var screen by remember { mutableStateOf(Screen.Splash) }
    var isFirstRun by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(Tab.Chats) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(1500)
        // TODO: Replace with FirebaseAuth.currentUser + persistent onboarding state.
        screen = if (isFirstRun) Screen.Welcome else Screen.Home
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
            Modifier.fillMaxSize(),
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
                        onContinue = { screen = Screen.Auth },
                        onExisting = {
                            isFirstRun = false
                            screen = Screen.Auth
                        }
                    )

                    Screen.Auth -> AuthScreen(
                        onBack = { screen = Screen.Welcome },
                        onContinue = {
                            notice = "اربط Firebase Auth لإكمال تسجيل الدخول الحقيقي."
                        }
                    )

                    Screen.Home -> HomeScreen(
                        selectedTab,
                        { selectedTab = it }
                    )
                }
            }

            notice?.let { message ->
                AlertDialog(
                    onDismissRequest = { notice = null },
                    confirmButton = {
                        TextButton(onClick = { notice = null }) {
                            Text("حسناً")
                        }
                    },
                    title = { Text("إعداد مطلوب") },
                    text = { Text(message) }
                )
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
        // Minimal monochrome emblem placeholder.
        Box(
            Modifier
                .size(66.dp)
                .background(Color.Transparent, CircleShape)
        ) {
            Icon(
                Icons.Default.AllInclusive,
                null,
                tint = Color(0xFFE5E5E5),
                modifier = Modifier.fillMaxSize()
            )
        }
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
            color = Color(0xFFBDBDBD),
            fontSize = 12.sp,
            letterSpacing = 1.sp
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
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark()

        Spacer(Modifier.height(24.dp))

        Text(
            "مرحباً بك في",
            color = Color.LightGray,
            fontSize = 16.sp
        )

        Text(
            "LYNKO",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "Connect Without Limits",
            color = Color(0xFFBDBDBD),
            fontSize = 13.sp
        )

        Spacer(Modifier.height(54.dp))

        Text(
            "اختر رحلتك",
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold
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
            Text(
                "إنشاء حساب جديد",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onExisting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("لدي حساب بالفعل")
        }
    }
}

@Composable
private fun AuthScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var create by remember { mutableStateOf(true) }

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

        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            username,
            { username = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("اسم المستخدم أو البريد الإلكتروني")
            },
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            password,
            { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("كلمة المرور") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            )
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(if (create) "إنشاء الحساب" else "دخول")
        }

        TextButton(
            onClick = { create = !create },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                if (create)
                    "لديك حساب؟ سجل الدخول"
                else
                    "مستخدم جديد؟ أنشئ حساباً"
            )
        }

        Text(
            "تسجيل الدخول الفعلي يتطلب إعداد Firebase قبل نشر التطبيق.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        )
    }
}

@Composable
private fun HomeScreen(
    selected: Tab,
    onSelect: (Tab) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBarCompat()
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
                        icon = {
                            Icon(
                                icon,
                                contentDescription = tab.title
                            )
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
                    "ابدأ محادثة بعد ربط الحسابات والخادم."
                )

                Tab.Friends -> EmptyState(
                    Icons.Default.PersonAdd,
                    "قائمة الأصدقاء",
                    "ابحث عن المستخدمين وأرسل طلبات الصداقة."
                )

                Tab.Groups -> EmptyState(
                    Icons.Default.Groups,
                    "مجموعاتك",
                    "أنشئ مجموعات وتواصل مع أصدقائك."
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
private fun TopAppBarCompat() {
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
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.NotificationsNone,
                    "الإشعارات"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
