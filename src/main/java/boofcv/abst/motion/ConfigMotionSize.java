package boofcv.abst.motion;
/**
 Created by th on 11.08.16.
 */
public class ConfigMotionSize {
   /**
    Radius of blur for the difference of the input images. Default is 4.
    */
   public int blurRadius = 4;
   /**
    Sigma of blur for the difference of the input images. Default is -1.
    */
   public double blurSigma = -1;
   /**
    Canny edge detection radius of the difference image. Default is 4.
    */
   public int cannyRadius = 4;
   /**
    Canny edge threshold minimum of the difference image. Default is 2.
    */
   public float cannyMin = 2;
   /**
    Canny edge threshold maximum of the difference image. Default is 10.
    */
   public float cannyMax = 10;
   /**
    Percentage of input image size to trigger the motion detected to ignore small object triggers.  Default is 25.
    */
   public int percentage = 25;
public ConfigMotionSize(int blurRadius, double blurSigma, int cannyRadius, float cannyMin, float cannyMax, int percentage) {
      this.blurRadius = blurRadius;
      this.blurSigma = blurSigma;
      this.cannyRadius = cannyRadius;
      this.cannyMin = cannyMin;
      this.cannyMax = cannyMax;
      this.percentage = percentage;
   }
public ConfigMotionSize() {
}
}
