package com.github.taymindis.jdc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;

import static com.github.taymindis.jdc.Jdc.getClassPath;

class WiredClassLoader extends ClassLoader {

    protected Set<String> resolvedClass = new HashSet<>();

// it only reload default classpath
    protected WiredClassLoader() {
        super(WiredClassLoader.class.getClassLoader());
    }

    protected Class<?> load(Class<?> loadingClass) throws IOException, ClassNotFoundException {
        // Expect class full name are same
        String classFullName = loadingClass.getName();
        if(resolvedClass.contains(classFullName)) {
            return super.loadClass(classFullName); // Use default CL cache
        }

        String classPath = getClassPath(loadingClass);
        InputStream input = null;
        ByteArrayOutputStream buffer = null;
        try {
            URL myUrl = new URL("file:".concat(classPath));
            URLConnection connection = myUrl.openConnection();
            input = connection.getInputStream();
            buffer = new ByteArrayOutputStream();
            int data = input.read();

            while (data != -1) {
                buffer.write(data);
                data = input.read();
            }

            byte[] classData = buffer.toByteArray();
            Class<?> loadedClass;
            try {
                loadedClass = defineClass(classFullName, classData, 0, classData.length);
            } catch (ClassFormatError | NoClassDefFoundError e) {
                e.printStackTrace();
                loadedClass = defineClass(null,
                        classData, 0, classData.length);
            }
            if (loadedClass != null) {
                if (loadedClass.getPackage() == null) {
                    definePackage(classFullName.replaceAll("\\.\\w+$", ""), null, null, null, null, null, null, null);
                }
                resolveClass(loadedClass);
                resolvedClass.add(classFullName);
            }
            return loadedClass;
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
        } finally {
            if (buffer != null) {
                try {
                    buffer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}