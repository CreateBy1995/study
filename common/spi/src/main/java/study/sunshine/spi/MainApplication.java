package study.sunshine.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-07-24
 **/
public class MainApplication {
    public static void main(String[] args) {
        ServiceLoader<GenericSpi> services = ServiceLoader.load(GenericSpi.class);
        Iterator<GenericSpi> iterator = services.iterator();
        while(iterator.hasNext()) {
            GenericSpi genericSpi = iterator.next();
            genericSpi.sayHello("hello");
        }
    }
}
