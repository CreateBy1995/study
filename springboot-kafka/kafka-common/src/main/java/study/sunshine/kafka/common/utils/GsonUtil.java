package study.sunshine.kafka.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-04-23
 **/
public class GsonUtil {
    private static Gson gson ;
    static {
        gson = new GsonBuilder().create();
    }
    public static String toGson(Object object){
        return gson.toJson(object) ;
    }
    public static <T> T fromGson(String json, Class<T> clazz){
        return gson.fromJson(json,clazz) ;
    }
}
