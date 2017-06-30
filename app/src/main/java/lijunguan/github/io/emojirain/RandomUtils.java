package lijunguan.github.io.emojirain;

import java.util.Random;

public class RandomUtils {

    private static Random random = new Random();

    public static void setSeed(long seed) {
        random.setSeed(seed);
    }

    public static float floatAround(float mean, float delta) {
        return floatInRange(mean - delta, mean + delta);
    }

    public static float floatInRange(float left, float right) {
        return left + (right - left) * random.nextFloat();
    }

    public static double positiveGaussian() {
        return Math.abs(random.nextGaussian());
    }
}