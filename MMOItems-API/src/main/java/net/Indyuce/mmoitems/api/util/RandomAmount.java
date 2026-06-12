package net.Indyuce.mmoitems.api.util;

import java.util.Random;

public class RandomAmount {
    private final int min, max;

    private static final Random RANDOM = new Random();

    public RandomAmount(int val) {
        this(val, val);
    }

    public RandomAmount(int min, int max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Reads random amount from a string formatted "{min}-{max}"
     *
     * @param str String to extract the random amount from
     */
    public RandomAmount(String str) {
        var split = str.split("-");
        min = Integer.parseInt(split[0]);
        max = split.length > 1 ? Integer.parseInt(split[1]) : min;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getRandomAmount() {
        if (min >= max) return min;
        return min + RANDOM.nextInt(max - min + 1);
    }

    public static RandomAmount one() {
        return new RandomAmount(1);
    }

    @Override
    public String toString() {
        return "RandomAmount{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }
}
