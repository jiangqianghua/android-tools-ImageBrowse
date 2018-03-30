package com.example.imagebrowse.utils;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

/**
 * 图片加载类
 * @author jiangqianghua
 *
 */
public class ImageLoader {

	private static ImageLoader mInstance ;
	/**
	 * 图片缓存的核心对象
	 */
	private LruCache<String,Bitmap> mLruCache ;
	
	/**
	 * 线程词池
	 */
	private ExecutorService mThreadPool ;
	/**
	 * 默认线程数量
	 */
	private static final int DEFAULT_THREAD_COUNT = 1 ;
	/**
	 * 队列调度方式
	 */
	private Type mType = Type.LIFO ; 
	/**
	 * 任务队列
	 */
	private LinkedList<Runnable> mTaskQueue ;
	/**
	 * 后台轮询线程
	 */
	private Thread mPoolThread ; 
	private Handler mPoolThreadHandler ;
	/**
	 * UI线程Handler
	 */
	private Handler mUIHandler ;
	/**
	 * 用于同步两个线程的信号量机制
	 */
	private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
	
	private Semaphore mSemaphoreThreadPool ;
	public enum Type
	{
		FIFO,LIFO;
	}
	private ImageLoader(int threadCount , Type type)
	{
		init(threadCount , type);
	}
	/**
	 * 初始化图片加载器
	 * @param hreadCount
	 * @param type
	 */
	private void init(int threadCount , Type type)
	{
		mType = type ;
		mPoolThread = new Thread()
		{
			@Override
			public void run()
			{
				Looper.prepare(); 
				mPoolThreadHandler = new Handler()
				{
					@Override
					public void handleMessage(android.os.Message msg) 
					{
						//线程池取出一个任务进行执行
						mThreadPool.execute(getTask());
						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
							
					}

				};
				mSemaphorePoolThreadHandler.release();// 释放信号量
				Looper.loop(); 
			}
		};
		// 开始执行线程
		mPoolThread.start();
		// 获取内存最大可用值
		
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheMemory = maxMemory / 8 ;
		
		mLruCache = new LruCache<String, Bitmap>(cacheMemory)
		{
			@Override
			protected int sizeOf(String key, Bitmap value)
			{
				return value.getRowBytes()*value.getHeight() ;
			};
		};
		
		
		// 创建线程池
		mThreadPool = Executors.newFixedThreadPool(threadCount);
		mTaskQueue = new LinkedList<Runnable>();
		mSemaphoreThreadPool = new Semaphore(threadCount);
	}
	
	/**
	 * 从任务队列取出一个方法
	 * @return
	 */
	private Runnable getTask() {
		// TODO Auto-generated method stub
		if(mType == Type.FIFO)
		{
			return mTaskQueue.removeFirst();
		}
		else if(mType == Type.LIFO)
		{
			return mTaskQueue.removeLast();
		}
		return null;
	}
	
	/**
	 * 实例单利对象，实行懒加载
	 * @return
	 */
	public static ImageLoader getInstance()
	{
		// 提高效率，过滤掉大部分代码
		if(mInstance == null)
		{
			// 同步线程，提升效率
			synchronized (ImageLoader.class) 
			{
				if(mInstance == null)
				{
					mInstance = new ImageLoader(DEFAULT_THREAD_COUNT , Type.FIFO);
				}
			}
		}
		return mInstance ;
	}
	
	/**
	 * 实例单利对象，实行懒加载
	 * @return
	 */
	public static ImageLoader getInstance(int threadCount , Type type)
	{
		// 提高效率，过滤掉大部分代码
		if(mInstance == null)
		{
			// 同步线程，提升效率
			synchronized (ImageLoader.class) 
			{
				if(mInstance == null)
				{
					mInstance = new ImageLoader(threadCount , type);
				}
			}
		}
		return mInstance ;
	}
	
	/**
	 * 根据path为ImageView设置图片
	 * @param path
	 * @param imageView
	 */
	public void loadImage(final String path, final ImageView imageView)
	{
		imageView.setTag(path);
		if(mUIHandler == null)
		{
			mUIHandler = new Handler()
			{
				public void handleMessage(android.os.Message msg)
				{
					// 获取得到图片，为ImageView回调设置图片
					ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
					Bitmap bm = holder.bitmap ; 
					ImageView imageView = holder.imageView ;
					String path = holder.path ;
					//将path与getTag存储路径进行比对
					if(imageView.getTag().toString().equals(path))
					{
						imageView.setImageBitmap(bm);
					}
					
				};
			};
		}
		Bitmap bm = getBitMapFromLruCache(path);
		if(bm != null)
		{
			refreashBitmap(path, imageView, bm);
		}
		else
		{
			// 如果缓存中不存在
			addTask(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					// 加载图片
					//图片压缩
					//1. 获取图片需要显示的大小
					ImageSize imageSize = getImageViewSize(imageView);
					//2.压缩图片
					Bitmap bm = decodeSampleFromPath(path , imageSize.width, imageSize.height );
					//3. 把图片加入到缓存
					addBitmapToLruCache(path,bm);
					refreashBitmap(path, imageView, bm);
					
					mSemaphoreThreadPool.release(); 
				}
				
			});
		}
	}
	/**
	 * 刷新bitmap
	 * @param path
	 * @param imageView
	 * @param bm
	 */
	private void refreashBitmap(final String path, final ImageView imageView,
			Bitmap bm) {
		ImageBeanHolder holder = new ImageBeanHolder();
		holder.bitmap = bm ; 
		holder.imageView = imageView ; 
		holder.path = path ;
		// 如果缓存存在，
		Message message = Message.obtain();
		message.obj = holder ;
		mUIHandler.sendMessage(message);
	}
	
	/**
	 * 将图片加入到LruCache缓存
	 * @param path
	 * @param bm
	 */
	private void addBitmapToLruCache(String path, Bitmap bm) {
		// TODO Auto-generated method stub
		if(getBitMapFromLruCache(path) == null)
		{
			if(bm != null)
			{
				mLruCache.put(path, bm);
			}
		}
	}

	/**
	 * 对图片要显示的宽和高对图片进行压缩
	 * @param path
	 * @param width
	 * @param height
	 * @return
	 */
	private Bitmap decodeSampleFromPath(String path, int width,
			int height) {
		// 获取图片的宽高，但不把图片加载到内存中
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		BitmapFactory.decodeFile(path,options);
		options.inSampleSize = caculateSampleSize(options,width , height);
		// 使用获取的InSampleSize再次解析图片
		options.inJustDecodeBounds = false ;
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);
		return bitmap;
	}
	
	/**
	 * 根据需求的宽和高和实际的宽和高计算SampleSize
	 * @param options
	 * @param width
	 * @param height
	 * @return
	 */
	private int caculateSampleSize(Options options, int reqWidth, int reqHeight) {
		// TODO Auto-generated method stub
		int width = options.outWidth ;
		int height = options.outHeight ;
		
		int inSampleSize = 1 ;
		if(width > reqWidth || height > reqHeight)
		{
			int widthRadio = Math.round(width*0.1f/reqWidth);
			int heightRadio = Math.round(height*0.1f/reqHeight); 
			inSampleSize = Math.max(widthRadio, heightRadio);
		}
		return inSampleSize;
	}

	private class ImageSize
	{
		public int width ;
		public int height ;
	}
	/**
	 * 根据ImageView获取宽高
	 * @param imageView
	 * @return
	 */
	@SuppressLint("NewApi")
	private ImageSize getImageViewSize(ImageView imageView) {
		// TODO Auto-generated method stub
		ImageSize imageSize = new ImageSize();
		LayoutParams lp = imageView.getLayoutParams();
		DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
		int width = imageView.getWidth() ; // 获取imageview的宽度
		//(lp.width == LayoutParams.WRAP_CONTENT?0:imageView.getWidth()) ;
		if(width <= 0)
		{
			width = lp.width; // 获取imageview在layout的宽度
		}
		
		if(width <= 0)
		{
			width = imageView.getMaxWidth() ; // 检查最大宽度  
		}
		
		if(width <= 0)
		{
			// 如果还是没拿到，复制屏幕宽度
			width = displayMetrics.widthPixels ;
		}
		
		int height = imageView.getHeight() ; // 获取imageview的高度  该方法不能向下兼容，可以用反射解决getObjectViewFieldValue
	//	int height = getObjectViewFieldValue(imageView,"mMaxHeight");
		//(lp.width == LayoutParams.WRAP_CONTENT?0:imageView.getWidth()) ;
		if(height <= 0)
		{
			height = lp.height; // 获取imageview在layout的高度
		}
		
		if(height <= 0)
		{
			height = imageView.getMaxHeight() ; // 检查最大高度
		}
		
		if(height <= 0)
		{
			// 如果还是没拿到，复制屏幕宽度
			height = displayMetrics.heightPixels ;
		}
		
		imageSize.width = width ; 
		imageSize.height = height ;
		return imageSize ;
	}
	
	/**
	 * 获取执行某个对象的某个值
	 * @param object
	 * @param fieldName
	 * @return
	 */
	private  static int getObjectViewFieldValue(Object object , String fieldName)
	{
		int value = 0 ;
		Field field = null;
		try {
			field = ImageView.class.getDeclaredField(fieldName);
		} catch (NoSuchFieldException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		field.setAccessible(true);
		
		try {
			int fieldValue = field.getInt(object);
			if(fieldValue >0 && fieldValue < Integer.MAX_VALUE)
			{
				value = fieldValue ;
			}
		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return value;
		
	}
	private synchronized void addTask(Runnable runnable) {
		// TODO Auto-generated method stub
		mTaskQueue.add(runnable);
		
		try {
			// 该方法会等待mSemaphorePoolThreadHandler.realse释放，才进行下一步
			if(mPoolThreadHandler == null)
			mSemaphorePoolThreadHandler.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mPoolThreadHandler.sendEmptyMessage(0x11);
	}
	/**
	 * 从缓存中获取bitmap
	 * @param key
	 * @return
	 */
	private Bitmap getBitMapFromLruCache(String key)
	{
		return mLruCache.get(key);
	}
	
	
	private class ImageBeanHolder
	{
		Bitmap bitmap ;
		ImageView imageView ;
		String path ;
	}
	
}
