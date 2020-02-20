package com.laulee.jiagu.xmleditor.main;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by laulee on 2020-02-20.
 */
public class Test {

    public static void run(String inputFile, String outputFile, String[] args) {

        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;

        try {
            fis = new FileInputStream(inputFile);
            bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            boolean var8 = false;

            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            ParserChunkUtils.xmlStruct.byteSrc = bos.toByteArray();
        } catch (Exception var33) {
            System.out.println("parse xml error:" + var33.toString());
        } finally {
            try {
                fis.close();
                bos.close();
            } catch (Exception var30) {
            }

        }
        String tag = args[2];
        String tagName = args[3];
        String attr = args[4];
        String value = args[5];
        XmlEditor.modifyAttr(tag, tagName, attr, value);

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(outputFile);
            fos.write(ParserChunkUtils.xmlStruct.byteSrc);
            fos.close();
        } catch (Exception var31) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException var29) {
                    var29.printStackTrace();
                }
            }

        }
    }
}
