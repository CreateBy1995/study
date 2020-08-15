package study.sunshine.dubbo.commonapi.api;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-08-13
 **/
public interface SyncApi {
    String getSyncResult(String result, Integer version);
}
