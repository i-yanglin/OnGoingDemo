
package com.example.yanglin.ongoingdemo1.Utils;

import com.example.yanglin.ongoingdemo1.Generic.GenericGF;
import com.example.yanglin.ongoingdemo1.Generic.GenericGFPoly;
import com.example.yanglin.ongoingdemo1.Generic.ReedSolomonDecoder;

/**
 * Created by yanglin on 2019/2/23.
 */

public class Utils2 {

    private static String rawKey;   //进行 0,1 处理后的数据
    private static String syndromes;  //校正子

   private static String  rawKeyGyr;
  private  static String syndromesGyr;
    public static void setRawKeyGyr(String data){
        rawKeyGyr = data;
    }
public static String getRawKeyGyr(){
    return rawKeyGyr;
}
    public static void setRawKey(String data){
        rawKey = data;
    }

    public static void setSyndromesGyr(String data){
        syndromesGyr = data;
    }
    public static String getSyndromesGyr(){
        return syndromesGyr;
    }
    public static  String getRawKey(){
        return rawKey;
    }


    public static void setSyndromes(String data){
        syndromes = data;
    }
    public static String getSyndromes(){
        return syndromes;
    }





    public static GenericGFPoly[] getSyndromePoly(String rawKey)
    {
        int[] rawKeyInt = new int[45];
        for(int i=0;i<32;i++)
        {
            String tmpStr = rawKey.substring(i*4,(i+1)*4);
            rawKeyInt[i] = Integer.parseInt(tmpStr,2);
        }
        for(int i=32;i<45;i++)
        {
            rawKeyInt[i] = 0;
        }
        int[] rawKey1 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey1[i] = rawKeyInt[i];
        }
        int[] rawKey2 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey2[i] = rawKeyInt[15+i];
        }
        int[] rawKey3 = new int[15];
        for(int i=0;i<15;i++)
        {
            rawKey3[i] = rawKeyInt[30+i];
        }

        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_PARAM);
        GenericGFPoly[] syndromes = new GenericGFPoly[3];
        syndromes[0] = decoder.getSyndromes(rawKey1,12);
        syndromes[1] = decoder.getSyndromes(rawKey2,12);
        syndromes[2] = decoder.getSyndromes(rawKey3,12);

        return syndromes;
    }

    public static String int2String(int[] data)
    {
        StringBuffer result=new StringBuffer("");
        for(int i=0;i<data.length;i++)
        {
            String tmp = Integer.toBinaryString(data[i]);
            int tmpLength = tmp.length();
            if(tmpLength < 4)
            {
                StringBuffer sb = new StringBuffer("");
                for(int j=0;j<4-tmpLength;j++)
                {
                    sb.append("0");
                }
                result.append(sb.toString()+tmp);
            }
            else {
                result.append(tmp);
            }
        }

        return result.toString();
    }

    public static int[] string2Ints(String syndrome)
    {
        int strLength = syndrome.length();
        int count = strLength/4;
        int[] syndromeBytes = new int[count];
        for(int i=0;i<count;i++)
        {
            String tmp = syndrome.substring(i*4,(i+1)*4);
            syndromeBytes[i] = Integer.parseInt(tmp,2);
        }
        return syndromeBytes;
    }


}
