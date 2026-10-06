package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.hid.HidConstants
import com.example.hid.HidReportSender
import com.example.hid.HidReports
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

/**
 * Registers the phone as a Bluetooth HID combo device and manages the host connection.
 *
 * All connection bookkeeping runs on the main thread (callbacks are re-posted there), so there
 * are no races between the Bluetooth binder callbacks and UI calls. Reports are sent by
 * [sender] on its own thread and only read the two volatile fields below.
 *
 * While an HID Device app is registered, Android pauses the phone's own HID Host role (its
 * Bluetooth keyboards, mice and game controllers). [suspend] therefore releases the registration
 * whenever the app is not in use, and [initialize] takes it again when the app comes back.
 */
@SuppressLint("MissingPermission") // Every entry point checks hasRequiredPermissions() first.
class BluetoothHidManager(private val context: Context) {

    companion object {
        private const val TAG = "BluetoothHidManager"

        /** Windows installs HID drivers on the first connection, which can take well over 10 s. */
        private const val CONNECT_TIMEOUT_MS = 30_000L
        private val RECONNECT_DELAYS_MS = longArrayOf(1_500, 3_000, 6_000, 12_000, 20_000)
        private const val REREGISTER_DELAY_MS = 1_500L
        private const val MAX_REREGISTER_ATTEMPTS = 3
        private const val PROXY_RETRY_DELAY_MS = 1_000L
        const val DISCOVERABLE_SECONDS = 300

        /**
         * Only the host-to-device (outgoing) QoS is specified; the incoming side is left to the
         * stack. This is the combination known to work with Windows, macOS, Linux, ChromeOS and
         * Android TV. A fixed incoming QoS makes some Windows Bluetooth drivers refuse the link.
         */
        private val QOS_OUT = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    @Volatile private var hidDevice: BluetoothHidDevice? = null
    @Volatile private var connectedDevice: BluetoothDevice? = null

    // Main-thread state
    private var isAppRegistered = false
    private var registerRequested = false
    private var proxyRequested = false
    private var shuttingDown = false
    private var suspended = false
    private var connectingDevice: BluetoothDevice? = null
    private var pendingConnect: BluetoothDevice? = null
    private var userDisconnecting = false
    private var reconnectTargetAddress: String? = null
    private var reconnectAttempt = 0
    private var isReconnectAttempt = false
    private var reregisterAttempts = 0

    val sender = HidReportSender(object : HidReportSender.Transport {
        override fun isReady(): Boolean = hidDevice != null && connectedDevice != null

        override fun send(reportId: Byte, data: ByteArray): Boolean {
            val proxy = hidDevice ?: return false
            val host = connectedDevice ?: return false
            return proxy.sendReport(host, reportId.toInt(), data)
        }
    })

    private val _connectionState = MutableStateFlow<HidConnectionState>(HidConnectionState.Initializing)
    val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = _pairedDevices.asStateFlow()

    private val _capsLockOn = MutableStateFlow(false)
    val capsLockOn: StateFlow<Boolean> = _capsLockOn.asStateFlow()

    /** One-shot, user-facing messages (shown as snackbars). */
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val reconnectRunnable = Runnable { attemptReconnect() }
    private val reregisterRunnable = Runnable { registerHidApp() }
    private val retryInitRunnable = Runnable { initialize() }
    private val connectTimeoutRunnable = Runnable {
        val pending = connectingDevice
        connectingDevice = null
        onConnectAttemptFailed(pending, timedOut = true)
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> onBluetoothOff()
                        BluetoothAdapter.STATE_ON -> {
                            // Never take the HID registration while the app is in the background.
                            if (!suspended) {
                                Log.d(TAG, "Bluetooth ON - re-initializing HID")
                                initialize()
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refreshPairedDevices()
            }
        }
    }

    private val serviceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            Log.d(TAG, "HID Device profile proxy connected")
            proxyRequested = false
            val hid = proxy as? BluetoothHidDevice ?: return
            if (suspended || shuttingDown) {
                // The app went to the background while the proxy was being bound.
                try {
                    bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid)
                } catch (_: Exception) {
                }
                return
            }
            hidDevice = hid
            registerHidApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            Log.d(TAG, "HID Device profile proxy disconnected")
            hidDevice = null
            connectedDevice = null
            connectingDevice = null
            isAppRegistered = false
            registerRequested = false
            proxyRequested = false
            if (shuttingDown || suspended) return
            if (bluetoothAdapter?.isEnabled == true) {
                // The Bluetooth service restarted; bind again once it is back.
                _connectionState.value = HidConnectionState.Initializing
                mainHandler.removeCallbacks(retryInitRunnable)
                mainHandler.postDelayed(retryInitRunnable, PROXY_RETRY_DELAY_MS)
            } else {
                _connectionState.value = HidConnectionState.BluetoothOff
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            mainHandler.post { handleAppStatusChanged(pluggedDevice, registered) }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            mainHandler.post { handleConnectionStateChanged(device, state) }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            val proxy = hidDevice ?: return
            val dev = device ?: return
            try {
                val report = HidReports.emptyReport(id)
                when {
                    type != BluetoothHidDevice.REPORT_TYPE_INPUT ->
                        proxy.reportError(dev, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
                    report.isEmpty() ->
                        proxy.reportError(dev, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                    else -> proxy.replyReport(dev, type, id, report)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in onGetReport", e)
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && id == HidConstants.REPORT_ID_KEYBOARD) {
                handleLedReport(id, data)
            }
            // SET_REPORT is the one control request the stack leaves for the app to acknowledge.
            val dev = device ?: return
            try {
                hidDevice?.reportError(dev, BluetoothHidDevice.ERROR_RSP_SUCCESS)
            } catch (e: Exception) {
                Log.e(TAG, "Error in onSetReport", e)
            }
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            if (reportId == HidConstants.REPORT_ID_KEYBOARD) handleLedReport(reportId, data)
        }

        // onSetProtocol is intentionally not overridden: the Bluetooth stack already answers
        // SET_PROTOCOL with a handshake. A second, unsolicited handshake from the app makes
        // strict hosts (several Windows drivers, some TVs) drop the connection.

        override fun onVirtualCableUnplug(device: BluetoothDevice?) {
            Log.d(TAG, "onVirtualCableUnplug from ${device?.address}")
            mainHandler.post {
                if (reconnectTargetAddress == device?.address) cancelReconnect()
                handleConnectionStateChanged(device, BluetoothProfile.STATE_DISCONNECTED)
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, bluetoothStateReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    fun initialize() {
        if (shuttingDown) return
        suspended = false
        mainHandler.removeCallbacks(retryInitRunnable)
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionState.value = HidConnectionState.NotSupported("This device has no Bluetooth hardware.")
            return
        }
        if (!adapter.isEnabled) {
            _connectionState.value = HidConnectionState.BluetoothOff
            return
        }
        if (!hasRequiredPermissions()) {
            _connectionState.value = HidConnectionState.PermissionsRequired
            return
        }

        refreshPairedDevices()

        val proxy = hidDevice
        if (proxy != null) {
            when {
                !isAppRegistered && !registerRequested -> registerHidApp()
                isAppRegistered && connectingDevice == null -> publishIdleOrConnectedState()
            }
            return
        }
        if (proxyRequested) return

        _connectionState.value = HidConnectionState.Initializing
        try {
            proxyRequested = adapter.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
            if (!proxyRequested) {
                _connectionState.value = HidConnectionState.NotSupported(
                    "The Bluetooth HID Device profile is not available on this phone. " +
                        "Some manufacturers disable it in firmware."
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in getProfileProxy", e)
            _connectionState.value = HidConnectionState.PermissionsRequired
        } catch (e: Exception) {
            Log.e(TAG, "Exception getting profile proxy", e)
            _connectionState.value = HidConnectionState.NotSupported("Unable to access HID Device profile: ${e.message}")
        }
    }

    private fun registerHidApp() {
        val proxy = hidDevice ?: return
        if (isAppRegistered || suspended || shuttingDown) return
        if (!hasRequiredPermissions()) {
            _connectionState.value = HidConnectionState.PermissionsRequired
            return
        }

        _connectionState.value = HidConnectionState.Registering

        val sdp = BluetoothHidDeviceAppSdpSettings(
            "HID Remote",
            "Bluetooth Mouse & Keyboard",
            "HID Remote",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidConstants.HID_REPORT_DESCRIPTOR
        )

        try {
            registerRequested = proxy.registerApp(sdp, null, QOS_OUT, callbackExecutor, hidCallback)
            if (!registerRequested) {
                Log.e(TAG, "registerApp returned false")
                scheduleReregister(
                    "Couldn't register as a keyboard/mouse. Another app may be using the Bluetooth HID profile. " +
                        "Close it, or toggle Bluetooth off and on, then retry."
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while registering HID App", e)
            _connectionState.value = HidConnectionState.PermissionsRequired
        } catch (e: Exception) {
            Log.e(TAG, "Exception registering HID app", e)
            _connectionState.value = HidConnectionState.Error("Error registering HID application: ${e.message}")
        }
    }

    /** Retries registration a few times (the stack is often briefly busy), then gives up with [failure]. */
    private fun scheduleReregister(failure: String) {
        mainHandler.removeCallbacks(reregisterRunnable)
        if (reregisterAttempts >= MAX_REREGISTER_ATTEMPTS) {
            reregisterAttempts = 0
            _connectionState.value = HidConnectionState.Error(failure)
            return
        }
        reregisterAttempts++
        _connectionState.value = HidConnectionState.Registering
        mainHandler.postDelayed(reregisterRunnable, REREGISTER_DELAY_MS * reregisterAttempts)
    }

    fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun refreshPairedDevices() {
        val adapter = bluetoothAdapter ?: return
        if (!hasRequiredPermissions()) return
        try {
            _pairedDevices.value = (adapter.bondedDevices ?: emptySet())
                .map { dev ->
                    BluetoothDeviceModel(
                        name = displayName(dev),
                        address = dev.address,
                        isBonded = true,
                        deviceType = classify(dev.bluetoothClass),
                        device = dev
                    )
                }
                .sortedWith(compareBy<BluetoothDeviceModel> { it.deviceType.ordinal }.thenBy { it.name.lowercase() })
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading bonded devices", e)
        }
    }

    private fun classify(btClass: BluetoothClass?): DeviceType = when (btClass?.majorDeviceClass) {
        BluetoothClass.Device.Major.COMPUTER -> DeviceType.COMPUTER
        BluetoothClass.Device.Major.PHONE -> DeviceType.PHONE
        BluetoothClass.Device.Major.AUDIO_VIDEO -> when (btClass.deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR,
            BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX,
            BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY -> DeviceType.TV
            else -> DeviceType.AUDIO
        }
        else -> DeviceType.UNKNOWN
    }

    /** Intent that makes this phone visible so a new host can find and pair with it. */
    fun discoverableIntent(): Intent =
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_SECONDS)

    // ------------------------------------------------------------------
    // Connection control
    // ------------------------------------------------------------------

    /** The host to reconnect to automatically, or null to disable auto-reconnect. */
    fun setReconnectTarget(address: String?) {
        if (reconnectTargetAddress == address) return
        reconnectTargetAddress = address
        cancelReconnect()
        if (address != null && isAppRegistered && connectedDevice == null && connectingDevice == null) {
            mainHandler.postDelayed(reconnectRunnable, RECONNECT_DELAYS_MS[0])
        }
    }

    fun connect(device: BluetoothDevice) {
        cancelReconnect()
        isReconnectAttempt = false
        startConnect(device)
    }

    private fun startConnect(device: BluetoothDevice) {
        if (!hasRequiredPermissions()) {
            _connectionState.value = HidConnectionState.PermissionsRequired
            return
        }
        val proxy = hidDevice
        if (proxy == null || !isAppRegistered) {
            pendingConnect = device
            initialize()
            return
        }

        val current = connectedDevice
        if (current != null) {
            if (current.address == device.address) return
            // One host at a time: drop the current host, connect when it reports DISCONNECTED.
            pendingConnect = device
            userDisconnecting = true
            _connectionState.value = HidConnectionState.Disconnecting
            try {
                proxy.disconnect(current)
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting before switch", e)
            }
            return
        }

        connectingDevice = device
        _connectionState.value = HidConnectionState.Connecting(displayName(device), device.address, reconnectAttempt)
        mainHandler.removeCallbacks(connectTimeoutRunnable)
        mainHandler.postDelayed(connectTimeoutRunnable, CONNECT_TIMEOUT_MS)
        try {
            if (!proxy.connect(device)) {
                Log.w(TAG, "BluetoothHidDevice.connect() returned false")
                connectingDevice = null
                onConnectAttemptFailed(device, timedOut = false)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException connecting", e)
            mainHandler.removeCallbacks(connectTimeoutRunnable)
            connectingDevice = null
            _connectionState.value = HidConnectionState.PermissionsRequired
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
            connectingDevice = null
            onConnectAttemptFailed(device, timedOut = false)
        }
    }

    fun disconnect() {
        cancelReconnect()
        pendingConnect = null
        val proxy = hidDevice ?: return
        val dev = connectedDevice ?: connectingDevice ?: return
        if (!hasRequiredPermissions()) return

        mainHandler.removeCallbacks(connectTimeoutRunnable)
        userDisconnecting = true
        sender.releaseAll()
        _connectionState.value = HidConnectionState.Disconnecting
        try {
            proxy.disconnect(dev)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }

    /**
     * Drops the host connection and the HID registration so nothing keeps running in the
     * background and the phone's own Bluetooth keyboards, mice and controllers work again.
     * [initialize] undoes this.
     */
    fun suspend() {
        if (suspended || shuttingDown) return
        Log.d(TAG, "Suspending HID registration")
        suspended = true
        cancelReconnect()
        mainHandler.removeCallbacks(connectTimeoutRunnable)
        mainHandler.removeCallbacks(reregisterRunnable)
        mainHandler.removeCallbacks(retryInitRunnable)
        pendingConnect = null
        reregisterAttempts = 0

        val proxy = hidDevice
        val dev = connectedDevice ?: connectingDevice
        if (proxy != null && hasRequiredPermissions()) {
            if (dev != null) {
                try {
                    proxy.disconnect(dev)
                } catch (e: Exception) {
                    Log.w(TAG, "disconnect during suspend failed", e)
                }
            }
            if (isAppRegistered || registerRequested) {
                try {
                    proxy.unregisterApp()
                } catch (e: Exception) {
                    Log.w(TAG, "unregisterApp during suspend failed", e)
                }
            }
        }
        connectedDevice = null
        connectingDevice = null
        isAppRegistered = false
        registerRequested = false
        userDisconnecting = false
        sender.releaseAll()
        closeProxy()
        _connectionState.value = HidConnectionState.Initializing
    }

    // ------------------------------------------------------------------
    // Callback handling (main thread)
    // ------------------------------------------------------------------

    private fun handleAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        Log.d(TAG, "onAppStatusChanged: registered=$registered, plugged=${pluggedDevice?.address}")
        if (suspended || shuttingDown) return
        isAppRegistered = registered
        registerRequested = registered
        if (!registered) {
            mainHandler.removeCallbacks(connectTimeoutRunnable)
            connectedDevice = null
            connectingDevice = null
            if (bluetoothAdapter?.isEnabled == true && hidDevice != null) {
                // Usually the stack restarting or another app briefly taking the profile.
                scheduleReregister("The HID keyboard/mouse service was unregistered by the system.")
            }
            return
        }

        reregisterAttempts = 0
        mainHandler.removeCallbacks(reregisterRunnable)
        val pending = pendingConnect
        pendingConnect = null
        val plugged = pluggedDevice?.takeIf { isConnected(it) }
        when {
            plugged != null -> handleConnectionStateChanged(plugged, BluetoothProfile.STATE_CONNECTED)
            pending != null -> startConnect(pending)
            else -> {
                _connectionState.value = HidConnectionState.ReadyToConnect
                if (reconnectTargetAddress != null) {
                    // Give the host a moment: many hosts reconnect to a known keyboard themselves,
                    // and paging each other at the same instant makes both attempts fail.
                    mainHandler.removeCallbacks(reconnectRunnable)
                    mainHandler.postDelayed(reconnectRunnable, RECONNECT_DELAYS_MS[0])
                }
            }
        }
    }

    private fun handleConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        if (suspended || shuttingDown) return
        val name = device?.let { displayName(it) } ?: "Unknown"
        val address = device?.address ?: ""
        Log.d(TAG, "onConnectionStateChanged: $name ($address) state=$state")

        when (state) {
            BluetoothProfile.STATE_CONNECTED -> {
                if (device == null) return
                mainHandler.removeCallbacks(connectTimeoutRunnable)
                cancelReconnect()
                connectingDevice = null
                connectedDevice = device
                userDisconnecting = false
                _capsLockOn.value = false
                sender.releaseAll() // start from a clean all-keys-up state
                _connectionState.value = HidConnectionState.Connected(device, name, address)
            }
            BluetoothProfile.STATE_CONNECTING -> {
                if (connectedDevice == null) {
                    mainHandler.removeCallbacks(reconnectRunnable)
                    _connectionState.value = HidConnectionState.Connecting(name, address, reconnectAttempt)
                }
            }
            BluetoothProfile.STATE_DISCONNECTING -> {
                if (connectedDevice?.address == address) {
                    _connectionState.value = HidConnectionState.Disconnecting
                }
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                val wasConnected = connectedDevice?.address == address
                val attempted = connectingDevice
                val wasConnecting = attempted?.address == address
                if (!wasConnected && !wasConnecting && connectedDevice != null) return // unrelated host

                mainHandler.removeCallbacks(connectTimeoutRunnable)
                connectedDevice = null
                connectingDevice = null
                sender.releaseAll()

                val next = pendingConnect
                if (next != null) {
                    pendingConnect = null
                    userDisconnecting = false
                    startConnect(next)
                    return
                }

                _connectionState.value = HidConnectionState.ReadyToConnect
                when {
                    userDisconnecting -> userDisconnecting = false
                    wasConnecting -> onConnectAttemptFailed(attempted, timedOut = false)
                    wasConnected && address == reconnectTargetAddress -> {
                        _events.tryEmit("Lost connection to $name. Reconnecting…")
                        reconnectAttempt = 0
                        isReconnectAttempt = true
                        scheduleReconnect()
                    }
                    wasConnected -> _events.tryEmit("Disconnected from $name")
                }
            }
        }
    }

    private fun onConnectAttemptFailed(failed: BluetoothDevice?, timedOut: Boolean) {
        mainHandler.removeCallbacks(connectTimeoutRunnable)
        if (connectedDevice != null) return

        // A timed-out attempt is left to the stack instead of being torn down: the host may still
        // be installing drivers, and a late CONNECTED is handled like any other.
        _connectionState.value = HidConnectionState.ReadyToConnect

        if (isReconnectAttempt && failed?.address == reconnectTargetAddress) {
            scheduleReconnect()
        } else if (failed != null) {
            val reason = if (timedOut) "is taking too long to answer" else "didn't accept the connection"
            _events.tryEmit(
                "${displayName(failed)} $reason. Keep it awake and nearby. If it's a Windows PC paired " +
                    "before, remove this phone there and pair again with this app open."
            )
        }
    }

    private fun scheduleReconnect() {
        mainHandler.removeCallbacks(reconnectRunnable)
        if (reconnectTargetAddress == null || shuttingDown || suspended) return
        if (reconnectAttempt >= RECONNECT_DELAYS_MS.size) {
            reconnectAttempt = 0
            isReconnectAttempt = false
            _events.tryEmit("Couldn't reach the host. Pick it from Devices to try again.")
            return
        }
        mainHandler.postDelayed(reconnectRunnable, RECONNECT_DELAYS_MS[reconnectAttempt++])
    }

    private fun attemptReconnect() {
        val target = reconnectTargetAddress ?: return
        if (suspended || !isAppRegistered || connectedDevice != null || connectingDevice != null) return
        val device = pairedDevices.value.firstOrNull { it.address == target }?.device ?: return
        isReconnectAttempt = true
        startConnect(device)
    }

    private fun cancelReconnect() {
        mainHandler.removeCallbacks(reconnectRunnable)
        reconnectAttempt = 0
        isReconnectAttempt = false
    }

    private fun publishIdleOrConnectedState() {
        val dev = connectedDevice
        _connectionState.value = if (dev != null) {
            HidConnectionState.Connected(dev, displayName(dev), dev.address)
        } else {
            HidConnectionState.ReadyToConnect
        }
    }

    private fun onBluetoothOff() {
        Log.d(TAG, "Bluetooth turning OFF")
        cancelReconnect()
        mainHandler.removeCallbacks(connectTimeoutRunnable)
        mainHandler.removeCallbacks(reregisterRunnable)
        mainHandler.removeCallbacks(retryInitRunnable)
        sender.releaseAll()
        connectedDevice = null
        connectingDevice = null
        isAppRegistered = false
        registerRequested = false
        closeProxy()
        _pairedDevices.value = emptyList()
        _connectionState.value = HidConnectionState.BluetoothOff
    }

    private fun handleLedReport(id: Byte, data: ByteArray?) {
        if (data == null || data.isEmpty()) return
        // Some stacks include the report ID as the first byte, others don't.
        val leds = if (data.size >= 2 && data[0] == id) data[1].toInt() else data[0].toInt()
        _capsLockOn.value = (leds and HidConstants.LED_CAPS_LOCK) != 0
    }

    private fun isConnected(device: BluetoothDevice): Boolean = try {
        hidDevice?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
    } catch (_: Exception) {
        false
    }

    private fun displayName(device: BluetoothDevice): String = try {
        device.name?.takeIf { it.isNotBlank() } ?: device.address
    } catch (_: SecurityException) {
        device.address
    }

    private fun closeProxy() {
        val proxy = hidDevice ?: return
        hidDevice = null
        proxyRequested = false
        try {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
        } catch (e: Exception) {
            Log.w(TAG, "closeProfileProxy failed", e)
        }
    }

    fun cleanup() {
        shuttingDown = true
        mainHandler.removeCallbacksAndMessages(null)
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
        } catch (_: Exception) {
        }
        try {
            if ((isAppRegistered || registerRequested) && hasRequiredPermissions()) hidDevice?.unregisterApp()
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering HID app", e)
        }
        closeProxy()
        connectedDevice = null
        sender.shutdown()
        callbackExecutor.shutdown()
    }
}
