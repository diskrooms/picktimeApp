package tech.startech.picktime;

import android.graphics.Bitmap;
import android.util.Log;

import com.apkfuns.logutils.LogUtils;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * @see JavaBlurProcess
 * Blur using the NDK and native code.
 */
class NativeBlurProcess implements BlurProcess{
	private static native void functionToGray(Bitmap bitmapOut);		//去色
	private static native void functionToReserve(Bitmap bitmapOut);		//反相
	private static native void functionToBlur(Bitmap bitmapOut, int radius, int threadCount, int threadIndex, int round);//模糊
	private static native int[] functionToDescolor(int[] prevPixels,int[] nextPixels,int w,int h);		//反相
	static {
		System.loadLibrary("blur");
	}

	/*public Bitmap gray(Bitmap original){
		Bitmap bitmapOut = original.copy(Bitmap.Config.ARGB_8888, true);
		//去色
		int width = bitmapOut.getWidth();
		int height = bitmapOut.getHeight();
		functionToGray(bitmapOut,width,height);
		return original;
	}*/

	@Override
	public Bitmap blur(Bitmap original, float radius, int w,int h) {
		Bitmap bitmapOut = original.copy(Bitmap.Config.ARGB_8888, true);
		int cores = StackBlurManager.EXECUTOR_THREADS;

			functionToGray(bitmapOut);
			int[] grayPixels = new int[w*h];
			bitmapOut.getPixels(grayPixels,0,w,0,0,w,h);
			functionToReserve(bitmapOut);

			/*functionToBlur(bitmapOut, (int) radius, 1, 0, 1);
			return bitmapOut;*/

		ArrayList<NativeTask> horizontal = new ArrayList<NativeTask>(cores);
		ArrayList<NativeTask> vertical = new ArrayList<NativeTask>(cores);
		for (int i = 0; i < cores; i++) {
			horizontal.add(new NativeTask(bitmapOut, (int) radius, cores, i, 1));
			vertical.add(new NativeTask(bitmapOut, (int) radius, cores, i, 2));
		}
		try {
			StackBlurManager.EXECUTOR.invokeAll(horizontal);
		} catch (InterruptedException e) {
			return bitmapOut;
		}
		try {
			StackBlurManager.EXECUTOR.invokeAll(vertical);
		} catch (InterruptedException e) {
			return bitmapOut;
		}
			int[] blurPixels = new int[w*h];
			bitmapOut.getPixels(blurPixels,0,w,0,0,w,h);
			int[] sketchPixels = new int[w*h];
			sketchPixels = functionToDescolor(grayPixels,blurPixels,w,h);
			//Bitmap sketchBitmap = Bitmap.createBitmap(sketchPixels,w,h, Bitmap.Config.RGB_565);
		return bitmapOut;
	}

	private static class NativeTask implements Callable<Void> {
		private final Bitmap _bitmapOut;
		private final int _radius;
		private final int _totalCores;
		private final int _coreIndex;
		private final int _round;

		public NativeTask(Bitmap bitmapOut, int radius, int totalCores, int coreIndex, int round) {
			_bitmapOut = bitmapOut;
			_radius = radius;
			_totalCores = totalCores;
			_coreIndex = coreIndex;
			_round = round;
		}

		@Override public Void call() throws Exception {
			functionToBlur(_bitmapOut, _radius, _totalCores, _coreIndex, _round);
			return null;
		}

	}
}
