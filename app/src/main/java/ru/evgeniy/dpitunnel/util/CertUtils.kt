package ru.evgeniy.dpitunnel.util

import android.content.Context
import org.spongycastle.asn1.ASN1ObjectIdentifier
import org.spongycastle.asn1.x500.X500NameBuilder
import org.spongycastle.asn1.x500.style.BCStyle
import org.spongycastle.asn1.x509.BasicConstraints
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo
import org.spongycastle.cert.X509v3CertificateBuilder
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.openssl.jcajce.JcaPEMWriter
import org.spongycastle.operator.ContentSigner
import org.spongycastle.operator.OperatorCreationException
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.FileOutputStream
import java.io.PrintWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.util.*


class CertUtils {
    companion object {
        val VALIDITY_IN_DAYS: Long = 3650;

        fun generateRootCA(context: Context) {
            var kpg: KeyPairGenerator?
            try {
                kpg = KeyPairGenerator.getInstance("RSA")
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                return;
            }
            kpg.initialize(2048)
            val keyPair = kpg.genKeyPair()

            val startDate = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val endDate = Date(System.currentTimeMillis() + VALIDITY_IN_DAYS * 24 * 60 * 60 * 1000)

            val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
            nameBuilder.addRDN(BCStyle.O, "DPITunnel")
            nameBuilder.addRDN(BCStyle.OU, "Research")
            nameBuilder.addRDN(BCStyle.L, "RU")

            val x500Name = nameBuilder.build()
            val random = SecureRandom()

            val subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
            val v3CertGen = X509v3CertificateBuilder(x500Name, BigInteger.valueOf(random.nextLong() and Long.MAX_VALUE),
                    startDate, endDate, x500Name, subjectPublicKeyInfo)

            val basicConstraints = BasicConstraints(true) // <-- true for CA, false for EndEntity

            v3CertGen.addExtension(ASN1ObjectIdentifier("2.5.29.19"), true, basicConstraints) // Basic Constraints is usually marked as critical.

            // Prepare Signature
            var sigGen: ContentSigner? = null
            try {
                Security.addProvider(BouncyCastleProvider())
                sigGen = JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("SC").build(keyPair.private)
            } catch (e: OperatorCreationException) {
                e.printStackTrace()
                return
            }

            // Self sign
            val x509CertificateHolder = v3CertGen.build(sigGen)
            var rootCert: X509Certificate
            try {
                rootCert = JcaX509CertificateConverter().getCertificate(x509CertificateHolder)
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                return
            }

            // Save to file
            val filesDir = context.getFilesDir().toString()
            // Cert
            var writer = JcaPEMWriter(PrintWriter(FileOutputStream(filesDir + "/rootCA.crt")))
            writer.writeObject(rootCert)
            writer.flush()
            writer.close()
            // Private key
            writer = JcaPEMWriter(PrintWriter(FileOutputStream(filesDir + "/rootCA.key")))
            writer.writeObject(keyPair.private)
            writer.flush()
            writer.close()
        }
    }
}