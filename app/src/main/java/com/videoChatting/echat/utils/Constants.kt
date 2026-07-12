package com.videoChatting.echat.utils

object Constants {
    // Development (Local) Server IP
    // Jab aap mobile aur laptop dono ko same WiFi par rakhenge, toh laptop ka IP yahan daalna hai.
    // Example: "192.168.31.117"
    private const val LOCAL_IP = "192.168.31.117"
    
    // Server URLs
    const val SOCKET_URL = "http://$LOCAL_IP:5000"
    const val BASE_URL = "$SOCKET_URL/api/"

    // Jab app Production (Live) mein jayega, toh hum LOCAL_IP ko apne 
    // Live Server/Domain ke link se replace kar denge, jaise:
    // const val SOCKET_URL = "https://api.echatapp.com"
}
