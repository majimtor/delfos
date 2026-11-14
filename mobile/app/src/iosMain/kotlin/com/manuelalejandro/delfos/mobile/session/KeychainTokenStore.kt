package com.manuelalejandro.delfos.mobile.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CFTypeRefVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Keeps the session as a generic password item of the keychain, on this device only. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class KeychainTokenStore : TokenStore {
    override fun read(): String? = memScoped {
        val query = query(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        if (status != errSecSuccess) return null
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
    }

    override fun write(value: String) {
        clear()
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val retained = CFBridgingRetain(data)
        val item = query(kSecValueData to retained, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        SecItemAdd(item, null)
        CFRelease(item)
        CFRelease(retained)
    }

    override fun clear() {
        val query = query()
        SecItemDelete(query)
        CFRelease(query)
    }

    private fun query(vararg extra: Pair<CFTypeRef?, CFTypeRef?>): CFMutableDictionaryRef? {
        val dictionary = CFDictionaryCreateMutable(null, 0, null, null)
        val service = CFBridgingRetain(SERVICE as NSString)
        val account = CFBridgingRetain(ACCOUNT as NSString)
        CFDictionaryAddValue(dictionary, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(dictionary, kSecAttrService, service)
        CFDictionaryAddValue(dictionary, kSecAttrAccount, account)
        extra.forEach { (key, value) -> CFDictionaryAddValue(dictionary, key, value) }
        return dictionary
    }

    private companion object {
        const val SERVICE = "com.manuelalejandro.delfos"
        const val ACCOUNT = "session"
    }
}
