package study.sunshine.utils;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Charsets.UTF_8;



/**
 * @Author: dongcx
 * @Description:
 * @Date: 2020-05-27
 **/
public class MainApplication {
    public static void main(String[] args) {
//        testSet();
        testBloomFilter();
    }
    public static void testSet(){
        Set<String> set1 = new HashSet<>();
        Set<String> set2 = new HashSet<>();
        for (int i = 0; i <= 1000000; i++) {
            set1.add(i+"str");
        }
        for (int i = 999998; i < 1005000; i++) {
            set2.add(i+"str");
        }
        long now = System.currentTimeMillis();
        System.out.println(now);
        set2.retainAll(set1);
        System.out.println(set2.size());
        System.out.println(System.currentTimeMillis()-now);
    }
    public static void testBloomFilter(){
        BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(UTF_8),1000000,0.001F);
        for (int i = 0; i <= 1000000; i++) {
            bloomFilter.put(i+"str");
        }
        List<String> set2 = new ArrayList<>();
        for (int i = 999998; i < 1005000; i++) {
            set2.add(i+"str");
        }
        long now = System.currentTimeMillis();
        System.out.println(set2.stream().filter(bloomFilter::mightContain).collect(Collectors.toList()).size());
        System.out.println(System.currentTimeMillis()-now);
    }
}
