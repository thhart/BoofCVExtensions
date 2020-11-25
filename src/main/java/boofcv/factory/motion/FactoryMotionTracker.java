package boofcv.factory.motion;
import boofcv.abst.motion.ConfigMotionSize;
import boofcv.alg.tracker.motion.MotionTracker;
/**
 Created by th on 11.08.16.
 */
public class FactoryMotionTracker {
   public static MotionTracker motionTracker(int percentage) {
      final ConfigMotionSize config = new ConfigMotionSize();
      config.percentage = percentage;
      return motionTracker(config);
   }
public static MotionTracker motionTracker(final ConfigMotionSize config) {
      return new MotionTracker(config);
   }
}
