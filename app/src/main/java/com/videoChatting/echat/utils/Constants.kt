package com.videoChatting.echat.utils

object Constants {
    // Development (Local) Server IP
    // Jab local par test karna ho toh is IP ka use karein
    private const val LOCAL_IP = "192.168.31.117"
    
    // Server URLs (Live Render Server)
    const val SOCKET_URL = "https://echat-backend-uj78.onrender.com"
    const val BASE_URL = "$SOCKET_URL/api/"

    // Third-party SDK Keys
    // Agar future mein alag se Dev aur Prod ke liye Agora account banana ho, 
    // toh aap easily yahan change kar sakte hain.
    const val AGORA_APP_ID = "021fba375e4b4a73828c10923e12627c"
}
