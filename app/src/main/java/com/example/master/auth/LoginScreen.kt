package com.example.master.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.master.R

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onGoogleSignIn: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    
    // Navigate on successful login
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }
    
    // Show error messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            // Error will be shown in UI
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Title
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.login_welcome_back),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1F2937)
                    )

                    Text(
                        text = stringResource(R.string.login_subhead),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6B7280)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Email Field
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.onEmailChanged(it) },
                        label = { Text(stringResource(R.string.login_email_label)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null)
                        },
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password Field
                    var passwordVisible by remember { mutableStateOf(false) }
                    
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        label = { Text(stringResource(R.string.login_password_label)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) 
                                        Icons.Filled.Visibility 
                                    else 
                                        Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) 
                                        stringResource(R.string.login_hide_password) 
                                    else 
                                        stringResource(R.string.login_show_password)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.signIn() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Error Message
                    if (uiState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Login Button
                    Button(
                        onClick = { viewModel.signIn() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1)
                        )
                    ) {
                        if (uiState.isLoading && uiState.loginInProgress == AuthFlow.EMAIL) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.login_sign_in),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Forgot Password
                    Text(
                        text = stringResource(R.string.login_forgot_password),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF6366F1),
                        modifier = Modifier.clickable {
                            // TODO: Navigate to forgot password
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = onGoogleSignIn,
                        enabled = !uiState.isLoading || (uiState.isLoading && uiState.loginInProgress == AuthFlow.GOOGLE),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading && uiState.loginInProgress == AuthFlow.GOOGLE) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.login_continue_google),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.login_or),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Sign Up Link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.login_no_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = stringResource(R.string.login_sign_up),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF6366F1),
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }
        }
    }
}
