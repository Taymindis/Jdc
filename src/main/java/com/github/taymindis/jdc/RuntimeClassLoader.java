package com.github.taymindis.jdc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

class RuntimeClassLoader extends ClassLoader{
    private Class<?> thisClass;
    protected RuntimeClassLoader(Class<?> thisClass) {
        super(thisClass.getClassLoader());
        this.thisClass = thisClass;
    }

    protected Class<?> load() throws IOException {
        String fullPath = "file:".concat(thisClass.getProtectionDomain().getCodeSource().getLocation()
                .getPath()).concat(thisClass.getCanonicalName().replace(".", File.separator ).concat(".class"));
        return load(fullPath);
    }

    protected Class<?> load(String classPath) throws IOException {
        InputStream input = null;
        ByteArrayOutputStream buffer = null;
        try {
            URL myUrl = new URL(classPath);
            URLConnection connection = myUrl.openConnection();
            input = connection.getInputStream();
            buffer = new ByteArrayOutputStream();
            int data = input.read();

            while(data != -1){
                buffer.write(data);
                data = input.read();
            }

            byte[] classData = buffer.toByteArray();

            try {
                return defineClass(thisClass.getCanonicalName(), classData, 0, classData.length);
            } catch (ClassFormatError classFormatError) {
                classFormatError.printStackTrace();
            } catch (NoClassDefFoundError classFormatError) {
                classFormatError.printStackTrace();
            }
            return defineClass(null,
                    classData, 0, classData.length);

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