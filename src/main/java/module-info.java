module BoofCV.Extensions {
	requires java.desktop;
	requires optimizerHelper;
	requires boofcv.uber;
	requires forms.rt;
	requires org.apache.logging.log4j;
	exports boofcv.helper.visualize;
	exports boofcv.helper.visualize.control;
}