package com.alijaya.genieacs.data

data class DeviceSummary(
    val id: String,
    val serialNumber: String,
    val modelName: String,
    val customerName: String,
    val pppoeUsername: String,
    val rxPower: String,
    val rxPowerVal: Double?,
    val isOnline: Boolean,
    val ipAddress: String,
    val wifiSsid: String,
    val wifiPass: String = "alijaya2026",
    val customerPhone: String = "",
    val odpName: String = "",
    val vlanId: String = "-",
    val lastInform: String = "-"
)

data class DeviceDetail(
    val id: String,
    val serialNumber: String,
    val productClass: String,
    val manufacturer: String,
    val softwareVersion: String,
    val hardwareVersion: String,
    val customerName: String,
    val customerPhone: String,
    val isOnline: Boolean,
    val uptime: String,
    val cpuTemp: String,
    val lastInform: String,
    
    // WAN
    val pppoeUsername: String,
    val pppoeStatus: String,
    val ipAddress: String,
    val macAddress: String,
    val defaultGateway: String,
    val dnsServers: String,
    val vlanId: String = "-",
    
    // Optic
    val rxPower: String,
    val txPower: String,
    val oltPon: String,
    val oltName: String,
    
    // WLAN
    val wifiSsid: String,
    val wifiPass: String,
    val wifiEnabled: Boolean,
    val wifiClientsCount: Int
)

data class DashboardStats(
    val totalDevices: Int,
    val onlineCount: Int,
    val offlineCount: Int,
    val criticalCount: Int
)
