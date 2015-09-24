package crs_svr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class PTUntil {
    public static void putShort(byte b[], short s, int index) {
        b[index + 1] = (byte) (s >> 8);
        b[index + 0] = (byte) (s >> 0);
    }

    public static void putShortlh(byte b[], short s, int index) {
        b[index + 0] = (byte) (s >> 8);
        b[index + 1] = (byte) (s >> 0);
    }

    public static short getShort(byte[] b, int index) {
        return (short) (((b[index + 1] << 8) | b[index + 0] & 0xff));
    }

    public static short getShortlh(byte[] b, int index) {
        return (short) (((b[index + 0] << 8) | b[index + 1] & 0xff));
    }

    public static void putInt(byte[] bb, int x, int index) {
        bb[index + 3] = (byte) (x >> 24);
        bb[index + 2] = (byte) (x >> 16);
        bb[index + 1] = (byte) (x >> 8);
        bb[index + 0] = (byte) (x >> 0);
    }

    public static void putIntlh(byte[] bb, int x, int index) {
        bb[index + 0] = (byte) (x >> 24);
        bb[index + 1] = (byte) (x >> 16);
        bb[index + 2] = (byte) (x >> 8);
        bb[index + 3] = (byte) (x >> 0);
    }

    public static int getInt(byte[] bb, int index) {
        return (int) ((((bb[index + 3] & 0xff) << 24)
                | ((bb[index + 2] & 0xff) << 16)
                | ((bb[index + 1] & 0xff) << 8) | ((bb[index + 0] & 0xff) << 0)));
    }

    public static int getIntlh(byte[] bb, int index) {
        return (int) ((((bb[index + 0] & 0xff) << 24)
                | ((bb[index + 1] & 0xff) << 16)
                | ((bb[index + 2] & 0xff) << 8) | ((bb[index + 3] & 0xff) << 0)));
    }

    public static void putLong(byte[] bb, long x, int index) {
        bb[index + 7] = (byte) (x >> 56);
        bb[index + 6] = (byte) (x >> 48);
        bb[index + 5] = (byte) (x >> 40);
        bb[index + 4] = (byte) (x >> 32);
        bb[index + 3] = (byte) (x >> 24);
        bb[index + 2] = (byte) (x >> 16);
        bb[index + 1] = (byte) (x >> 8);
        bb[index + 0] = (byte) (x >> 0);
    }

    public static void putLonglh(byte[] bb, long x, int index) {
        bb[index + 0] = (byte) (x >> 56);
        bb[index + 1] = (byte) (x >> 48);
        bb[index + 2] = (byte) (x >> 40);
        bb[index + 3] = (byte) (x >> 32);
        bb[index + 4] = (byte) (x >> 24);
        bb[index + 5] = (byte) (x >> 16);
        bb[index + 6] = (byte) (x >> 8);
        bb[index + 7] = (byte) (x >> 0);
    }


    public static long getLong(byte[] bb, int index) {
        return ((((long) bb[index + 7] & 0xff) << 56)
                | (((long) bb[index + 6] & 0xff) << 48)
                | (((long) bb[index + 5] & 0xff) << 40)
                | (((long) bb[index + 4] & 0xff) << 32)
                | (((long) bb[index + 3] & 0xff) << 24)
                | (((long) bb[index + 2] & 0xff) << 16)
                | (((long) bb[index + 1] & 0xff) << 8) | (((long) bb[index + 0] & 0xff) << 0));
    }

    public static long getLonglh(byte[] bb, int index) {
        return ((((long) bb[index + 0] & 0xff) << 56)
                | (((long) bb[index + 1] & 0xff) << 48)
                | (((long) bb[index + 2] & 0xff) << 40)
                | (((long) bb[index + 3] & 0xff) << 32)
                | (((long) bb[index + 4] & 0xff) << 24)
                | (((long) bb[index + 5] & 0xff) << 16)
                | (((long) bb[index + 6] & 0xff) << 8) | (((long) bb[index + 7] & 0xff) << 0));
    }


    public static void putChar(byte[] bb, char ch, int index) {
        int temp = (int) ch;
        // byte[] b = new byte[2];
        for (int i = 0; i < 2; i++) {
            bb[index + i] = new Integer(temp & 0xff).byteValue();
            temp = temp >> 8;
        }
    }

    public static char getChar(byte[] b, int index) {
        int s = 0;
        if (b[index + 1] > 0)
            s += b[index + 1];
        else
            s += 256 + b[index + 0];
        s *= 256;
        if (b[index + 0] > 0)
            s += b[index + 1];
        else
            s += 256 + b[index + 0];
        char ch = (char) s;
        return ch;
    }

    public static void putFloat(byte[] bb, float x, int index) {
        // byte[] b = new byte[4];
        int l = Float.floatToIntBits(x);
        for (int i = 0; i < 4; i++) {
            bb[index + i] = new Integer(l).byteValue();
            l = l >> 8;
        }
    }

    public static float getFloat(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }

    public static void putDouble(byte[] bb, double x, int index) {
        // byte[] b = new byte[8];
        long l = Double.doubleToLongBits(x);
        for (int i = 0; i < 8; i++) {
            bb[index + i] = new Long(l).byteValue();
            l = l >> 8;
        }
    }

    public static double getDouble(byte[] b, int index) {
        long l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        l &= 0xffffffffl;
        l |= ((long) b[index + 4] << 32);
        l &= 0xffffffffffl;
        l |= ((long) b[index + 5] << 40);
        l &= 0xffffffffffffl;
        l |= ((long) b[index + 6] << 48);
        l &= 0xffffffffffffffl;
        l |= ((long) b[index + 7] << 56);
        return Double.longBitsToDouble(l);
    }

    public static int putShortString(byte[] bb, String info, int index) {
        try {
            if (bb.length < (info.length() + index + 1))
                return -1;
            short ilen = (short) info.length();
            bb[index] = (byte) ilen;
            if (0 < ilen) {
                System.arraycopy(info.getBytes("UTF-8"), 0, bb, index + 1, ilen);
            }
            return (ilen + 1);
        } catch (UnsupportedEncodingException e) {
            return -2;
        }
    }

    public static int getShortString(byte[] bb, int index, StringBuilder sb) {
        try {
            if (bb.length < 1 + index) return -1;
            short ilen = (short) bb[index];
            if (bb.length < (1 + ilen + index)) return -2;
            if (0 < ilen) {
                byte[] bTmp = new byte[ilen];
                System.arraycopy(bb, index + 1, bTmp, 0, ilen);
                String str = new String(bTmp, "UTF-8");
                sb.append(str);
            }

            return (ilen + 1);
        } catch (UnsupportedEncodingException e) {
            return -3;
        }
    }

    public static int putShortString4Align(byte[] bb, String info, int index) {
        try {
            int ilen = putShortString(bb, info, index);
            if (0 > ilen) return ilen;
            if (0 != (ilen % 4)) ilen += (4 - (ilen % 4));
            return ilen;
        } catch (Exception e) {
            return -2;
        }
    }

    public static int getShortString4Align(byte[] bb, int index, StringBuilder sb) {
        try {
            int ilen = getShortString(bb, index, sb);
            if (0 > ilen) return ilen;
            if (0 != (ilen % 4)) ilen += (4 - (ilen % 4));
            return ilen;
        } catch (Exception e) {
            return -3;
        }
    }

    public static int putLongString(byte[] bb, String info, int index) {
        try {
            if (bb.length < (info.length() + index + 2))
                return -1;
            short ilen = (short) info.length();
            putShortlh(bb, ilen, index);
            if (0 < ilen) {
                System.arraycopy(info.getBytes("UTF-8"), 0, bb, index + 2, ilen);
            }
            return (ilen + 2);
        } catch (UnsupportedEncodingException e) {
            return -2;
        }
    }

    public static int getLongString(byte[] b, int index, StringBuilder sb) {
        try {
            if (b.length < 2 + index) return -1;
            short ilen = getShortlh(b, index);
            if (b.length < (2 + index + ilen)) return -2;
            if (0 < ilen) {
                byte[] bTmp = new byte[ilen];
                System.arraycopy(b, index + 1, bTmp, 0, ilen);
                String str = new String(bTmp, "UTF-8");
                sb.append(str);
            }
            return (ilen + 2);
        } catch (UnsupportedEncodingException e) {
            return -3;
        }
    }

    public static int putLongString4Align(byte[] bb, String info, int index) {
        try {
            int ilen = putLongString(bb, info, index);
            if (0 > ilen) return ilen;
            if (0 != (ilen % 4)) ilen += (4 - (ilen % 4));
            return ilen;
        } catch (Exception e) {
            return -2;
        }
    }

    public static int getLongString4Align(byte[] b, int index, StringBuilder sb) {
        try {
            int ilen = getLongString(b, index, sb);
            if (0 > ilen) return ilen;
            if (0 != (ilen % 4)) ilen += (4 - (ilen % 4));
            return ilen;
        } catch (Exception e) {
            return -3;
        }
    }

    public static int hBytesToInt(byte[] b) {
        int s = 0;
        for (int i = 0; i < 3; i++) {
            if (b[i] >= 0) {
                s = s + b[i];
            } else {
                s = s + 256 + b[i];
            }
            s = s * 256;
        }
        if (b[3] >= 0) {
            s = s + b[3];
        } else {
            s = s + 256 + b[3];
        }
        return s;
    }

    public static int lBytesToInt(byte[] b) {
        int s = 0;
        for (int i = 0; i < 3; i++) {
            if (b[3 - i] >= 0) {
                s = s + b[3 - i];
            } else {
                s = s + 256 + b[3 - i];
            }
            s = s * 256;
        }
        if (b[0] >= 0) {
            s = s + b[0];
        } else {
            s = s + 256 + b[0];
        }
        return s;
    }


    private static final int MAX_ENCRYPT_BLOCK = 16;

    public static byte[] encrypt(byte[] content, int iOffset, int len, String password) throws IOException {
        try {
           /*
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
	        kgen.init(128, new SecureRandom(password.getBytes()));  
	        SecretKey secretKey = kgen.generateKey();  
	        byte[] enCodeFormat = secretKey.getEncoded();  
	        SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");  
	        */

            //KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            //keyGen.init(256, new SecureRandom(password.getBytes("UTF-8")) );
            //Key key = keyGen.generateKey();

            SecretKeySpec key = new SecretKeySpec(password.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");// ����������
            // byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, key);// ��ʼ��


            int inputLen = len;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = iOffset;
            byte[] cache = null;
            int i = 0;
            // ����ݷֶν���
            while (inputLen - offSet > 0) {
                if (inputLen - offSet >= MAX_ENCRYPT_BLOCK) {
                    cache = cipher.doFinal(content, offSet, MAX_ENCRYPT_BLOCK);
                } else {
                    byte[] bTmp = new byte[MAX_ENCRYPT_BLOCK];
                    System.arraycopy(content, offSet, bTmp, 0, inputLen - offSet);
                    cache = cipher.doFinal(bTmp, 0, MAX_ENCRYPT_BLOCK);
                }
                out.write(cache, 0, cache.length);
                i++;
                offSet += MAX_ENCRYPT_BLOCK;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();


            //byte[] result = cipher.doFinal(byteContent);
            return decryptedData; // ����
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static final int MAX_DECRYPT_BLOCK = 16;

    public static byte[] decrypt(byte[] content, int iOffset, int len, String password) throws IOException {
        try {
            //�õ�һ��ʹ��AES�㷨��KeyGenerator��ʵ��
            //KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            //SecureRandom random = new SecureRandom();
            //keyGen.init(128,random);
            //ͨ��KeyGenerator����һ��key(��Կ�㷨�Ѷ��壬ΪAES)
            //Key key = keyGen.generateKey();

            //KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            //keyGen.init(256, new SecureRandom(password.getBytes("UTF-8")) );
            //Key key = keyGen.generateKey();

            SecretKeySpec key = new SecretKeySpec(password.getBytes("UTF-8"), "AES");


            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");// ����������
            cipher.init(Cipher.DECRYPT_MODE, key);// ��ʼ��
            //byte[] result = cipher.doFinal(content);


            int inputLen = len;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int offSet = iOffset;
            byte[] cache = null;
            int i = 0;
            // ����ݷֶν���
            while (inputLen - offSet > 0) {
                if (inputLen - offSet >= MAX_DECRYPT_BLOCK) {
                    cache = cipher.doFinal(content, offSet, MAX_DECRYPT_BLOCK);
                } else {
                    byte[] bTmp = new byte[MAX_DECRYPT_BLOCK];
                    System.arraycopy(content, offSet, bTmp, 0, inputLen - offSet);
                    cache = cipher.doFinal(bTmp, 0, MAX_DECRYPT_BLOCK);
                }
                out.write(cache, 0, cache.length);
                i++;
                //offSet = i * MAX_DECRYPT_BLOCK ;
                offSet += MAX_DECRYPT_BLOCK;
            }
            byte[] decryptedData = out.toByteArray();
            out.close();

            return decryptedData; // ����
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String str2HexStr(String str) {

        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;

        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            sb.append(' ');
        }
        return sb.toString().trim();
    }


    public static byte[] str2HexStr(byte[] str) throws UnsupportedEncodingException {

        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str;
        int bit;

        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0xf0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            //sb.append(' ');
        }
        return sb.toString().trim().getBytes("utf-8");
    }

    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;

        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    public static byte[] hexStr2Str(byte[] hexStr) {
        String str = "0123456789ABCDEF";
        byte[] hexs = hexStr;
        byte[] bytes = new byte[hexStr.length / 2];
        int n;

        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return (bytes);
    }

    public static byte[] hexStr2Str(byte[] hexStr, int iLen) {
        String str = "0123456789ABCDEF";
        byte[] hexs = hexStr;
        byte[] bytes = new byte[iLen / 2];
        int n;

        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return (bytes);
    }

    public static String byte2HexStr(byte[] b) {
        String stmp = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

    public static byte[] byte2HexStr(byte[] b, int iLen) {
        String stmp = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim().getBytes();
    }

    /**
     * delete file
     *
     * @return suc true; other false
     * @parram String file_path
     */
    public static boolean deleteFile(String file_path) {
        try {
            File file = new File(file_path);
            if (file.isFile() && file.exists()) {
                file.delete();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

