package com.akuplatform.shared.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
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
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Minimal iOS Keychain wrapper used by [IosTokenStorage].
 *
 * Stores and retrieves UTF-8 encoded string values in the generic-password
 * Keychain class, keyed by [service] + [account].
 */
@OptIn(ExperimentalForeignApi::class)
internal class KeychainHelper(
    private val service: String = "com.akulearn.app",
    private val account: String = "aku_auth_token"
) {

    fun save(value: String): Boolean {
        val data = value.toNSData() ?: return false
        delete() // remove any existing item first

        memScoped {
            val query = buildQuery {
                addEntry(kSecClass, kSecClassGenericPassword)
                addEntry(kSecAttrService, CFBridgingRetain(service)!!)
                addEntry(kSecAttrAccount, CFBridgingRetain(account)!!)
                addEntry(kSecValueData, CFBridgingRetain(data)!!)
            }
            val status = SecItemAdd(query, null)
            return status == errSecSuccess || status == errSecDuplicateItem
        }
    }

    fun load(): String? {
        memScoped {
            val query = buildQuery {
                addEntry(kSecClass, kSecClassGenericPassword)
                addEntry(kSecAttrService, CFBridgingRetain(service)!!)
                addEntry(kSecAttrAccount, CFBridgingRetain(account)!!)
                addEntry(kSecReturnData, kCFBooleanTrue)
                addEntry(kSecMatchLimit, kSecMatchLimitOne)
            }
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return null
            val data = CFBridgingRelease(result.value) as? NSData ?: return null
            return data.toKotlinString()
        }
    }

    fun delete(): Boolean {
        memScoped {
            val query = buildQuery {
                addEntry(kSecClass, kSecClassGenericPassword)
                addEntry(kSecAttrService, CFBridgingRetain(service)!!)
                addEntry(kSecAttrAccount, CFBridgingRetain(account)!!)
            }
            val status = SecItemDelete(query)
            return status == errSecSuccess
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private fun buildQuery(block: QueryBuilder.() -> Unit): CFMutableDictionaryRef? {
        val dict = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        QueryBuilder(dict).block()
        return dict
    }

    private class QueryBuilder(private val dict: CFMutableDictionaryRef?) {
        fun addEntry(key: CFStringRef?, value: Any?) {
            platform.CoreFoundation.CFDictionaryAddValue(dict, key, value)
        }
    }

    private fun String.toNSData(): NSData? =
        (this as NSString).dataUsingEncoding(NSUTF8StringEncoding)

    private fun NSData.toKotlinString(): String? =
        NSString.create(this, NSUTF8StringEncoding) as? String
}
