package com.manuelalejandro.delfos.mobile.session

/**
 * Where the session lives between launches: the Android keystore encrypts it, the iOS keychain
 * holds it. One opaque value in, the same value out, or nothing.
 */
public interface TokenStore {
    public fun read(): String?

    public fun write(value: String)

    public fun clear()
}
