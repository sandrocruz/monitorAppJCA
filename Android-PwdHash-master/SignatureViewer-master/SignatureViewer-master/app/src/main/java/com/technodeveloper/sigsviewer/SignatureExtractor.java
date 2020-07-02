package com.technodeveloper.sigsviewer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;

public class SignatureExtractor {

    private PackageManager packageManager;

    public SignatureExtractor(Context context) {
        packageManager = context.getPackageManager();
    }

    public Signature[] getSignatures(String packageName) {
        Signature[] signatures = null;

        try {
            signatures = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return signatures;
    }

    public Signature getSignature(String packageName) {
        return getSignatures(packageName)[0];
    }

    public String getSignatureString(Signature signature) {
        return Integer.toHexString(signature.hashCode());
    }
}
