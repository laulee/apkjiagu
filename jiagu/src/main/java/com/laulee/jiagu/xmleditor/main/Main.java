//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
package com.laulee.jiagu.xmleditor.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {
    private static final String CMD_TXT = "[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]";

    public Main() {
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("参数有误...");
            System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
        } else {
            String inputfile = args[args.length - 2];
            String outputfile = args[args.length - 1];
            File inputFile = new File(inputfile);
            File outputFile = new File(outputfile);
            if (!inputFile.exists()) {
                System.out.println("输入文件不存在...");
            } else {
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

                doCommand(args);
                if (!outputFile.exists()) {
                    outputFile.delete();
                }

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
    }

    public static void testDemo() {
    }

    public static void doCommand(String[] args) {
        String tag;
        String tagName;
        if ("-tag".equals(args[0])) {
            if (args.length < 2) {
                System.out.println("缺少参数...");
                System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
            } else if ("-i".equals(args[1])) {
                if (args.length < 3) {
                    System.out.println("缺少参数...");
                    System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
                } else {
                    tag = args[2];
                    File file = new File(tag);
                    if (!file.exists()) {
                        System.out.println("插入标签xml文件不存在...");
                    } else {
                        XmlEditor.addTag(tag);
                        System.out.println("插入标签完成...");
                    }
                }
            } else if ("-r".equals(args[1])) {
                if (args.length < 4) {
                    System.out.println("缺少参数...");
                    System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
                } else {
                    tag = args[2];
                    tagName = args[3];
                    XmlEditor.removeTag(tag, tagName);
                    System.out.println("删除标签完成...");
                }
            } else {
                System.out.println("操作标签参数有误...");
                System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
            }
        } else {
            if ("-attr".equals(args[0])) {
                if (args.length < 2) {
                    System.out.println("缺少参数...");
                    System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
                    return;
                }

                String attr;
                String value;
                if ("-i".equals(args[1])) {
                    if (args.length < 6) {
                        System.out.println("缺少参数...");
                        System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
                        return;
                    }

                    tag = args[2];
                    tagName = args[3];
                    attr = args[4];
                    value = args[5];
                    XmlEditor.addAttr(tag, tagName, attr, value);
                    System.out.println("插入属性完成...");
                    return;
                }

                if ("-r".equals(args[1])) {
                    if (args.length < 5) {
                        System.out.println("缺少参数...");
                        System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
                        return;
                    }

                    tag = args[2];
                    tagName = args[3];
                    attr = args[4];
                    XmlEditor.removeAttr(tag, tagName, attr);
                    System.out.println("删除属性完成...");
                    return;
                }

                if (!"-m".equals(args[1])) {
                    System.out.println("操作属性参数有误...");
                    System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
                    return;
                }

                if (args.length < 6) {
                    System.out.println("缺少参数...");
                    System.out.println("[usage java -jar AXMLEditor.jar [-tag|-attr] [-i|-r|-m] [标签名|标签唯一ID|属性名|属性值] [输入文件|输出文件]");
                    return;
                }

                tag = args[2];
                tagName = args[3];
                attr = args[4];
                value = args[5];
                XmlEditor.modifyAttr(tag, tagName, attr, value);
                System.out.println("修改属性完成...");
            }

        }
    }
}