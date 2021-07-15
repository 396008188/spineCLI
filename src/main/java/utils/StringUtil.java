package utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtil {

    /**
     * 删除字符串中指定字符
     * @param str
     * @param delChar
     * @return
     */
    public static String deleteString(String str, char delChar) {
        StringBuffer stringBuffer = new StringBuffer("");
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != delChar) {
                stringBuffer.append(str.charAt(i));
            }
        }
        return stringBuffer.toString();
    }

    public static String getStringBetweenSingleQuotes(String str) {

        List<Integer> list = new ArrayList<>();
        for(int i = 0; i < str.length(); i++) {
            String str_1 = str.substring(i, i + 1);
            if ("\'".equals(str_1)) {
                list.add(i);//list记录单引号索引
            }
        }
        int before = list.get(0);//前一个单引号索引
        int rear = list.get(1);//后一个单引号索引
        String pathWithTitle = str.substring(before+1, rear);
        String path = pathWithTitle.substring(7);
        return path;
    }
}
