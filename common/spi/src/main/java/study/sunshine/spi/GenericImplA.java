package study.sunshine.spi;

/**
 * @Author: dongcx
 * @Description: SPI 实现类A
 * @Date: 2020-07-24
 **/
public class GenericImplA implements GenericSpi {
    @Override
    public void sayHello(String msg) {
        System.out.println("GenericImplB "+ msg);
    }
}
