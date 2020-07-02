package com.technodeveloper.sigsviewer;

import android.content.pm.Signature;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class SignatureInfo {

    private X509Certificate certificate;

    public SignatureInfo(Signature signature) {
        try {
            certificate = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(signature.toByteArray()));
        } catch (CertificateException e) {
            e.printStackTrace();
        }
    }

    public int getSerialNumber() {
        return certificate.getSerialNumber().intValue();
    }

    public String getIssuer() {
        return certificate.getIssuerDN().toString();
    }

    public String getSubject() {
        return certificate.getSubjectDN().toString();
    }
}
