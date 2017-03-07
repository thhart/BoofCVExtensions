package personal.thhart;

import java.util.*;
import java.util.concurrent.*;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.*;
import boofcv.abst.feature.orientation.*;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.*;
import boofcv.factory.feature.orientation.*;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 Created by th on 27.07.16.

 A matching extractor with some base algorithms
 */
@SuppressWarnings("WeakerAccess")
public class MatchingExtractor {
   private static InterestPointDetector<GrayU8> createDetector() {
      GeneralFeatureDetector<GrayU8,GrayU8> alg = FactoryDetectPoint.createFast(new ConfigFast(4, 12), new ConfigGeneralDetector(200, 4, 0.25f), GrayU8.class);
      return FactoryInterestPoint.wrapPoint(alg, 1, GrayU8.class, GrayU8.class);
   }


   private static OrientationImage<GrayU8> createOrientation() {
      Class<GrayU8> integralType = GIntegralImageOps.getIntegralType(GrayU8.class);
      OrientationIntegral<GrayU8> orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
      return FactoryOrientation.convertImage(orientationII, integralType);
   }

   private static DescribeRegionPoint<GrayU8, NccFeature> createDescriptor() {
      return FactoryDescribeRegionPoint.pixelNCC(24, 24, GrayU8.class);
   }

   final static ExecutorService service = Executors.newCachedThreadPool();

   protected static List<Match> extractMatches(GrayU8 previous, GrayU8 next) {
      final List<Match> matches = new ArrayList<>();
      try {
         final ScoreAssociation<NccFeature> scorer = FactoryAssociation.defaultScore(createDescriptor().getDescriptionType());
         final Future<List<Feature>> future = service.submit(() -> {
            try {
               return createFeatures(previous);
            } catch (Throwable throwable) {
               throwable.printStackTrace();
            }
            return null;
         });
         final List<Feature> fN = service.submit(() -> {
            try {
               return createFeatures(next);
            } catch (Throwable throwable) {
               throwable.printStackTrace();
            }
            return null;
         }).get();
         final List<Feature> fP = future.get();
         for (Feature feature : fP) {
            double score = Double.MAX_VALUE;
            Feature featureBest = null;
            for (Feature fx : fN) {
               final double v = scorer.score(feature.desc, fx.desc);
               if (score > v) {
                  score = v;
                  featureBest = fx;
               }
            }
            if(featureBest != null && score <= -0.95) {
               matches.add(new Match(feature, featureBest, score));
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return matches;
   }

   private static List<Feature> createFeatures(GrayU8 gray) {
      final ArrayList<Feature> features = new ArrayList<>();
      final DescribeRegionPoint<GrayU8, NccFeature> describe = createDescriptor();
      final FastQueue<NccFeature> queue = UtilFeature.createQueue(describe, 2);
      final OrientationImage<GrayU8> orientation = createOrientation();
      final InterestPointDetector<GrayU8> detector = createDetector();
      describe.setImage(gray);
      orientation.setImage(gray);
      detector.detect(gray);
      double yaw;
      final ArrayList<Integer> f64List = new ArrayList<>();
      final int size = detector.getNumberOfFeatures();
         for (int i = 0; i < size; i++) {
            f64List.add(i);
         }
      for (Integer i : f64List) {
         final Point2D_F64 pt = detector.getLocation(i);
         orientation.setImage(gray);
         yaw = orientation.compute(pt.x, pt.y);
         final Feature feature = new Feature();
         if (describe.process(pt.x, pt.y, yaw, 4, queue.grow())) {
            feature.point = pt.copy();
            feature.desc = queue.getTail();
            features.add(feature);
         }
      }
      return features;
   }

   @SuppressWarnings("unused")
   public static class Feature {
      public Point2D_F64 getPoint() {
         return point;
      }

      public void setPoint(Point2D_F64 point) {
         this.point = point;
      }

      public NccFeature getDesc() {
         return desc;
      }

      public void setDesc(NccFeature desc) {
         this.desc = desc;
      }

      NccFeature desc;
      Point2D_F64 point;
   }

   @SuppressWarnings("unused")
   public static class Match {

      public Feature getF1() {
         return f1;
      }

      private final Feature f1;

      public Feature getF2() {
         return f2;
      }

      private final Feature f2;
      private final double score;

      private Match(Feature f1, Feature f2, double score) {
         this.f1 = f1;
         this.f2 = f2;
         this.score = score;
      }

      public double getScore() {
         return score;
      }

   }
}
