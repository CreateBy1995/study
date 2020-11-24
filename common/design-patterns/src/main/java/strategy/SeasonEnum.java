package strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum SeasonEnum {
    SPRING(1, "春天") {
        @Override
        public String doSomething() {
            return "SPRING";
        }
    },
    SUMMER(2, "夏天") {
        @Override
        public String doSomething() {
            return "SUMMER";
        }
    },
    AUTUMN(3, "秋天") {
        @Override
        public String doSomething() {
            return "AUTUMN";
        }
    },
    WINTER(4, "冬天") {
        @Override
        public String doSomething() {
            return "WINTER";
        }
    };
    private Integer key;
    private String description;
    private static Map<Integer, SeasonEnum> map = new HashMap<>();

    static {
        for (SeasonEnum seasonEnum : SeasonEnum.values()) {
            map.put(seasonEnum.getKey(), seasonEnum);
        }
    }

    public abstract String doSomething();

    public static SeasonEnum toSeasonEnum(Integer key) {
        return map.get(key);
    }
}
