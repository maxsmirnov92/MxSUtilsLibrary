package net.maxsmr.commonutils.android.certs;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class SignatureUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(SignatureUtils.class);

    public SignatureUtils() {
        throw new AssertionError("no instances.");
    }

    @Nullable
    public static PackageSignatureInfo getSelfPackageSignatureInfo(@NonNull Context context) {
        return getInstalledPackageSignatureInfo(context, context.getPackageName());
    }

    @Nullable
    public static PackageSignatureInfo getInstalledPackageSignatureInfo(@NonNull Context context, String packageName) {
        try {
            return getPackageSignatureInfo(context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static PackageSignatureInfo getArchivePackageSignatureInfo(@NonNull Context context, File file) {

        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }

        return getPackageSignatureInfo(context.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), PackageManager.GET_SIGNATURES));
    }

    @Nullable
    public static PackageSignatureInfo getPackageSignatureInfo(PackageInfo info) {

        if (info == null) {
            return null;
        }

        final List<PackageSignatureInfo.SignatureInfo> signatureInfos = new ArrayList<>();

        final Signature[] arrSignatures = info.signatures;
        for (final Signature sig : arrSignatures) {
        /*
        * Get the X.509 certificate.
        */
            final byte[] rawCert = sig.toByteArray();
            InputStream certStream = new ByteArrayInputStream(rawCert);

            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X509");
                X509Certificate x509Cert = (X509Certificate) certFactory.generateCertificate(certStream);

                if (x509Cert != null) {
                    signatureInfos.add(new PackageSignatureInfo.SignatureInfo(x509Cert.getSubjectDN().toString(), x509Cert.getIssuerDN().toString(), x509Cert.getSerialNumber().toString(), sig.hashCode()));
                }

            } catch (CertificateException e) {
               logger.e("a " + CertificateException.class.getSimpleName() + " occurred: " + e.getMessage());
            }
        }

        return new PackageSignatureInfo(info.packageName, signatureInfos);
    }

    public static class PackageSignatureInfo {

        @NonNull
        private final String packageName;

        @NonNull
        private final List<SignatureInfo> signatureInfos;

        public PackageSignatureInfo(@NonNull String packageName, @NonNull List<SignatureInfo> signatureInfos) {
            this.packageName = packageName;
            this.signatureInfos = Collections.unmodifiableList(signatureInfos);
        }

        @NonNull
        public String getPackageName() {
            return packageName;
        }

        @NonNull
        public List<SignatureInfo> getSignatureInfos() {
            return signatureInfos;
        }

        @Override
        public String toString() {
            return "PackageSignatureInfo{" +
                    "packageName='" + packageName + '\'' +
                    ", signatureInfos=" + signatureInfos +
                    '}';
        }

        public static boolean containsDebug(List<SignatureInfo> infos) {
            boolean has = false;
            if (infos != null) {
                for (SignatureInfo info : infos) {
                    if (SignatureInfo.containsDebug(info)) {
                        has = true;
                        break;
                    }
                }
            }
            return has;
        }

        public static class SignatureInfo {

            private final String subjectDN;

            private final String issuerDN;

            private final String serialNumber;

            private final long hashCode;

            public SignatureInfo(String subjectDN, String issuerDN, String serialNumber, long hashCode) {
                this.subjectDN = subjectDN;
                this.issuerDN = issuerDN;
                this.serialNumber = serialNumber;
                this.hashCode = hashCode;
            }

            public String getSubjectDN() {
                return subjectDN;
            }

            public String getIssuerDN() {
                return issuerDN;
            }

            public String getSerialNumber() {
                return serialNumber;
            }

            public long getHashCode() {
                return hashCode;
            }

            @Override
            public String toString() {
                return "SignatureInfo{" +
                        "subjectDN='" + subjectDN + '\'' +
                        ", issuerDN='" + issuerDN + '\'' +
                        ", serialNumber='" + serialNumber + '\'' +
                        ", hashCode=" + hashCode +
                        '}';
            }

            public static boolean containsDebug(SignatureInfo info) {
                return info != null && !TextUtils.isEmpty(info.subjectDN) && info.subjectDN.contains("Debug");
            }
        }
    }
}
