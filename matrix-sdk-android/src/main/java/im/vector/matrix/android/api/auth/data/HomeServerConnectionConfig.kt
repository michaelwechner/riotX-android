/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.api.auth.data

import android.net.Uri
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.auth.data.HomeServerConnectionConfig.Builder
import im.vector.matrix.android.internal.network.ssl.Fingerprint
import okhttp3.CipherSuite
import okhttp3.TlsVersion

/**
 * This data class holds how to connect to a specific Homeserver.
 * It's used with [im.vector.matrix.android.api.auth.Authenticator] class.
 * You should use the [Builder] to create one.
 */
@JsonClass(generateAdapter = true)
data class HomeServerConnectionConfig(
        val homeServerUri: Uri,
        val identityServerUri: Uri? = null,
        val antiVirusServerUri: Uri? = null,
        val allowedFingerprints: MutableList<Fingerprint> = ArrayList(),
        val shouldPin: Boolean = false,
        val tlsVersions: MutableList<TlsVersion>? = null,
        val tlsCipherSuites: MutableList<CipherSuite>? = null,
        val shouldAcceptTlsExtensions: Boolean = true,
        val allowHttpExtension: Boolean = false,
        val forceUsageTlsVersions: Boolean = false
) {

    /**
     * This builder should be use to create a [HomeServerConnectionConfig] instance.
     */
    class Builder {

        private lateinit var homeServerUri: Uri
        private var identityServerUri: Uri? = null
        private var antiVirusServerUri: Uri? = null
        private val allowedFingerprints: MutableList<Fingerprint> = ArrayList()
        private var shouldPin: Boolean = false
        private val tlsVersions: MutableList<TlsVersion> = ArrayList()
        private val tlsCipherSuites: MutableList<CipherSuite> = ArrayList()
        private var shouldAcceptTlsExtensions: Boolean = true
        private var allowHttpExtension: Boolean = false
        private var forceUsageTlsVersions: Boolean = false

        fun withHomeServerUri(hsUriString: String): Builder {
            return withHomeServerUri(Uri.parse(hsUriString))
        }

        /**
         * @param hsUri The URI to use to connect to the homeserver.
         * @return this builder
         */
        fun withHomeServerUri(hsUri: Uri): Builder {
            if (hsUri.scheme != "http" && hsUri.scheme != "https") {
                throw RuntimeException("Invalid home server URI: " + hsUri)
            }
            // ensure trailing /
            homeServerUri = if (!hsUri.toString().endsWith("/")) {
                try {
                    val url = hsUri.toString()
                    Uri.parse("$url/")
                } catch (e: Exception) {
                    throw RuntimeException("Invalid home server URI: $hsUri")
                }
            } else {
                hsUri
            }
            return this
        }

        fun withIdentityServerUri(identityServerUriString: String): Builder {
            return withIdentityServerUri(Uri.parse(identityServerUriString))
        }

        /**
         * @param identityServerUri The URI to use to manage identity.
         * @return this builder
         */
        fun withIdentityServerUri(identityServerUri: Uri): Builder {
            if (identityServerUri.scheme != "http" && identityServerUri.scheme != "https") {
                throw RuntimeException("Invalid identity server URI: $identityServerUri")
            }
            // ensure trailing /
            if (!identityServerUri.toString().endsWith("/")) {
                try {
                    val url = identityServerUri.toString()
                    this.identityServerUri = Uri.parse("$url/")
                } catch (e: Exception) {
                    throw RuntimeException("Invalid identity server URI: $identityServerUri")
                }
            } else {
                this.identityServerUri = identityServerUri
            }
            return this
        }

        /**
         * @param allowedFingerprints If using SSL, allow server certs that match these fingerprints.
         * @return this builder
         */
        fun withAllowedFingerPrints(allowedFingerprints: List<Fingerprint>?): Builder {
            if (allowedFingerprints != null) {
                this.allowedFingerprints.addAll(allowedFingerprints)
            }
            return this
        }

        /**
         * @param pin If true only allow certs matching given fingerprints, otherwise fallback to
         * standard X509 checks.
         * @return this builder
         */
        fun withPin(pin: Boolean): Builder {
            this.shouldPin = pin
            return this
        }

        /**
         * @param shouldAcceptTlsExtension
         * @return this builder
         */
        fun withShouldAcceptTlsExtensions(shouldAcceptTlsExtension: Boolean): Builder {
            this.shouldAcceptTlsExtensions = shouldAcceptTlsExtension
            return this
        }

        /**
         * Add an accepted TLS version for TLS connections with the home server.
         *
         * @param tlsVersion the tls version to add to the set of TLS versions accepted.
         * @return this builder
         */
        fun addAcceptedTlsVersion(tlsVersion: TlsVersion): Builder {
            this.tlsVersions.add(tlsVersion)
            return this
        }

        /**
         * Force the usage of TlsVersion. This can be usefull for device on Android version < 20
         *
         * @param forceUsageOfTlsVersions set to true to force the usage of specified TlsVersions (with [.addAcceptedTlsVersion]
         * @return this builder
         */
        fun forceUsageOfTlsVersions(forceUsageOfTlsVersions: Boolean): Builder {
            this.forceUsageTlsVersions = forceUsageOfTlsVersions
            return this
        }

        /**
         * Add a TLS cipher suite to the list of accepted TLS connections with the home server.
         *
         * @param tlsCipherSuite the tls cipher suite to add.
         * @return this builder
         */
        fun addAcceptedTlsCipherSuite(tlsCipherSuite: CipherSuite): Builder {
            this.tlsCipherSuites.add(tlsCipherSuite)
            return this
        }

        fun withAntiVirusServerUri(antivirusServerUriString: String?): Builder {
            return withAntiVirusServerUri(antivirusServerUriString?.let { Uri.parse(it) })
        }

        /**
         * Update the anti-virus server URI.
         *
         * @param antivirusServerUri the new anti-virus uri. Can be null
         * @return this builder
         */
        fun withAntiVirusServerUri(antivirusServerUri: Uri?): Builder {
            if (null != antivirusServerUri && "http" != antivirusServerUri.scheme && "https" != antivirusServerUri.scheme) {
                throw RuntimeException("Invalid antivirus server URI: $antivirusServerUri")
            }
            this.antiVirusServerUri = antivirusServerUri
            return this
        }

        /**
         * Convenient method to limit the TLS versions and cipher suites for this Builder
         * Ref:
         * - https://www.ssi.gouv.fr/uploads/2017/02/security-recommendations-for-tls_v1.1.pdf
         * - https://developer.android.com/reference/javax/net/ssl/SSLEngine
         *
         * @param tlsLimitations         true to use Tls limitations
         * @param enableCompatibilityMode set to true for Android < 20
         * @return this builder
         */
        fun withTlsLimitations(tlsLimitations: Boolean, enableCompatibilityMode: Boolean): Builder {
            if (tlsLimitations) {
                withShouldAcceptTlsExtensions(false)

                // Tls versions
                addAcceptedTlsVersion(TlsVersion.TLS_1_2)
                addAcceptedTlsVersion(TlsVersion.TLS_1_3)

                forceUsageOfTlsVersions(enableCompatibilityMode)

                // Cipher suites
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256)
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256)
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256)
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256)
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384)
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384)
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256)
                addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256)

                if (enableCompatibilityMode) {
                    // Adopt some preceding cipher suites for Android < 20 to be able to negotiate
                    // a TLS session.
                    addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA)
                    addAcceptedTlsCipherSuite(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA)
                }
            }
            return this
        }

        fun withAllowHttpConnection(allowHttpExtension: Boolean): Builder {
            this.allowHttpExtension = allowHttpExtension
            return this
        }

        /**
         * @return the [HomeServerConnectionConfig]
         */
        fun build(): HomeServerConnectionConfig {
            return HomeServerConnectionConfig(
                    homeServerUri,
                    identityServerUri,
                    antiVirusServerUri,
                    allowedFingerprints,
                    shouldPin,
                    tlsVersions,
                    tlsCipherSuites,
                    shouldAcceptTlsExtensions,
                    allowHttpExtension,
                    forceUsageTlsVersions
            )
        }
    }
}
