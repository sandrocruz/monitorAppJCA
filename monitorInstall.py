import sys, os, shutil

'''
INSTALAÇÃO DA MONITORIZAÇÃO DE APLICAÇÕES ANDROID

O PROCESSO É SIMPLES, BASICAMENTE TEM DE SE CORRER: python3 monitorInstall.py PASTA_APLICAÇÃO_ANDROID
A PARTIR DAQUI É TUDO FEITO AUTOMATICAMENTE.
O PROVIDER TEM DE ESTAR PRESENTE JUNTO COM O FICHEIRO PYTHON COM O NOME "bcprov-jdk15on-166b02.jar"
'''



IMPORT = ["import org.bouncycastle.Monitor;", "import java.security.Provider;", "import java.security.Security;", "import org.bouncycastle.jce.provider.BouncyCastleProvider;",
"import java.text.DecimalFormat;", "import android.Manifest;", "import android.app.Activity;", "import android.support.v4.app.ActivityCompat;", "import android.os.Build;"]

CODE = """        public void requestPermissionForReadExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void requestPermissionForWriteExternalStorage() throws Exception {
        try {
            ActivityCompat.requestPermissions((Activity) this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public boolean checkPermissionForReadExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public boolean checkPermissionForWriteExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    public static void setupBouncyCastle() {

        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        float statusMemory;
        float statusAppTime;
        sum = Monitor.writeTimes("", "SUM");
        statusMemory = endMemory - startMemory;
        statusAppTime = endAppTime - startAppTime;
         

        if (sum > 15000000) {
            System.out.println("[Monitor] Tempo de execução: O sistema preparou a build...");
        }
        else {
        sum = sum/1000000;
        statusAppTime = statusAppTime/1000000;
        statusMemory = Math.abs(statusMemory/1000);
        DecimalFormat df = new DecimalFormat("#.####");
        DecimalFormat df2 = new DecimalFormat("#.####");
        DecimalFormat df3 = new DecimalFormat("#.####");
        String iString = df.format(sum);
        String iString2 = df2.format(statusMemory);
        String iString3 = df3.format(statusAppTime);
        System.out.println("[Monitor] Tempo de execução (Total): " + iString3 + " ms\\n");
        System.out.println("[Monitor] Tempo de execução (Segurança): " + iString + " ms\\n");
        System.out.println("[Monitor] Memória: " + iString2 + " KB\\n");
        }

        Monitor.writeTimes("", "CLEAN");
    }

}
"""

VARS = ["    private float sum;", "    private float startMemory;", "    private float endMemory;", "    private float startAppTime; ", "    private float endAppTime;" ,"    private boolean mSaveStateOnExit = true;", 
"    private static final int WRITE_STORAGE_PERMISSION_REQUEST_CODE = 1;", "    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 2;\n"]

def copyLibJar(argv):
    directory = os.listdir(argv)
    for fname in directory:
        caminho = argv + os.sep + fname
        if "app" == fname:
            try:
                os.mkdir(caminho+"/libs")
            except FileExistsError:
                print("Folder libs already created!")

            shutil.copy2("bcprov-jdk15on-166b02.jar", caminho+"/libs")




def findAndroidManifest(argv):
    directory = os.listdir(argv)
    for fname in directory:
        caminho = argv + os.sep + fname
        if "main" in caminho:
            if "AndroidManifest.xml" in fname and os.path.isfile(caminho):
                f = open(caminho, "r")
                if ("<?xml version=\"1.0\" encoding=\"utf-8\"?>") in f.read():
                    f2 = open(caminho, "r")
                    f2Read = f2.read()
                    f.close()
                    f2.close()
                    if ("<uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" />" not in f2Read and "<uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\" />" not in f2Read):
                        newFile = f2Read.replace("    <application", "    <uses-permission android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\" />\n    <uses-permission android:name=\"android.permission.READ_EXTERNAL_STORAGE\" />\n\n    <application")
                        f3 = open(caminho, "w", encoding='utf8')
                        f3.write(newFile)
                        f3.close()


def findMainActivity(argv):
    imports = []
    forImport = []
    onCreate = []
    directory = os.listdir(argv)
    for fname in directory:
        caminho = argv + os.sep + fname
        if ".java" in fname and os.path.isfile(caminho):
            f = open(caminho, "r")
            if ("onCreate" and "savedInstanceState") in f.read():
                f2 = open(caminho, "r")
                f2Read = f2.read()
                f.close()
                f2.close()

                if "setupBouncyCastle()" not in f2Read:
                    for line in f2Read.splitlines():
                        if "import" in line:
                            imports.append(line)
                    for i in IMPORT:
                        if i not in imports:
                            forImport.append(i)
                    forImport.append(imports[-1])

                    methods = find_between(f2Read, "onCreate(Bundle savedInstanceState) {", "@Override")
                    if "private" in methods:
                        methods = find_between(f2Read, "onCreate(Bundle savedInstanceState) {", "private")

                    activity = find_between(f2Read, "extends", "{")
                    activitystrip = activity.strip()

                    
                    if "Activity" != activitystrip:
                        f2Read = f2Read.replace("extends " + activitystrip, "extends Activity")

                    f2Read = f2Read.replace("extends Activity {", "extends Activity {\n\n" + "\n".join(VARS))

                    f = -1
                    while (f2Read[f] != '}'):
                        f -= 1

                    f2Read = f2Read[:f] + "\n" + CODE

                    i = -1
                    while (methods[i] != '}'):
                        i -= 1
                        
                    newMethods = "\n        startAppTime = System.nanoTime();\n        startMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();\n" + methods[:i] + "\n" + """        if (checkPermissionForWriteExternalStorage() == false || checkPermissionForReadExternalStorage() == false) {
            try {
                requestPermissionForWriteExternalStorage();
                requestPermissionForReadExternalStorage();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        setupBouncyCastle();
        endMemory = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
        endAppTime = System.nanoTime();
    }""" + "\n\n    "


                    f2Read = f2Read.replace(imports[-1], "\n".join(forImport))
                    f2Read = f2Read.replace(methods, newMethods)

                    f3 = open(caminho, "w", encoding='utf8')
                    f3.write(f2Read)
                    f3.close()
                    

def find_between( s, first, last ):
    try:
        start = s.index( first ) + len( first )
        end = s.index( last, start )
        return s[start:end]
    except ValueError:
        return ""


def main(argv):
    print("0%... A inicializar o processo")
    print("20%... A adicionar as definições de storage do Android")
    for x in os.walk(argv):
        findAndroidManifest(x[0])
    print("50%... A adicionar a monitorização na aplicação Android")
    for x in os.walk(argv):
        findMainActivity(x[0])
    print("70%... A adicionar o Provider")
    for x in os.walk(argv):
        copyLibJar(x[0])
    print("100%... Concluído com sucesso!")


if __name__ == "__main__":
    main(sys.argv[1])
