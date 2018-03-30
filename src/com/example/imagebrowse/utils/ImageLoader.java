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
 * ͼƬ������
 * @author jiangqianghua
 *
 */
public class ImageLoader {

	private static ImageLoader mInstance ;
	/**
	 * ͼƬ����ĺ��Ķ���
	 */
	private LruCache<String,Bitmap> mLruCache ;
	
	/**
	 * �̴߳ʳ�
	 */
	private ExecutorService mThreadPool ;
	/**
	 * Ĭ���߳�����
	 */
	private static final int DEFAULT_THREAD_COUNT = 1 ;
	/**
	 * ���е��ȷ�ʽ
	 */
	private Type mType = Type.LIFO ; 
	/**
	 * �������
	 */
	private LinkedList<Runnable> mTaskQueue ;
	/**
	 * ��̨��ѯ�߳�
	 */
	private Thread mPoolThread ; 
	private Handler mPoolThreadHandler ;
	/**
	 * UI�߳�Handler
	 */
	private Handler mUIHandler ;
	/**
	 * ����ͬ�������̵߳��ź�������
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
	 * ��ʼ��ͼƬ������
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
						//�̳߳�ȡ��һ���������ִ��
						mThreadPool.execute(getTask());
						try {
							mSemaphoreThreadPool.acquire();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
							
					}

				};
				mSemaphorePoolThreadHandler.release();// �ͷ��ź���
				Looper.loop(); 
			}
		};
		// ��ʼִ���߳�
		mPoolThread.start();
		// ��ȡ�ڴ�������ֵ
		
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
		
		
		// �����̳߳�
		mThreadPool = Executors.newFixedThreadPool(threadCount);
		mTaskQueue = new LinkedList<Runnable>();
		mSemaphoreThreadPool = new Semaphore(threadCount);
	}
	
	/**
	 * ���������ȡ��һ������
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
	 * ʵ����������ʵ��������
	 * @return
	 */
	public static ImageLoader getInstance()
	{
		// ���Ч�ʣ����˵��󲿷ִ���
		if(mInstance == null)
		{
			// ͬ���̣߳�����Ч��
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
	 * ʵ����������ʵ��������
	 * @return
	 */
	public static ImageLoader getInstance(int threadCount , Type type)
	{
		// ���Ч�ʣ����˵��󲿷ִ���
		if(mInstance == null)
		{
			// ͬ���̣߳�����Ч��
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
	 * ����pathΪImageView����ͼƬ
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
					// ��ȡ�õ�ͼƬ��ΪImageView�ص�����ͼƬ
					ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
					Bitmap bm = holder.bitmap ; 
					ImageView imageView = holder.imageView ;
					String path = holder.path ;
					//��path��getTag�洢·�����бȶ�
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
			// ��������в�����
			addTask(new Runnable() {
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					// ����ͼƬ
					//ͼƬѹ��
					//1. ��ȡͼƬ��Ҫ��ʾ�Ĵ�С
					ImageSize imageSize = getImageViewSize(imageView);
					//2.ѹ��ͼƬ
					Bitmap bm = decodeSampleFromPath(path , imageSize.width, imageSize.height );
					//3. ��ͼƬ���뵽����
					addBitmapToLruCache(path,bm);
					refreashBitmap(path, imageView, bm);
					
					mSemaphoreThreadPool.release(); 
				}
				
			});
		}
	}
	/**
	 * ˢ��bitmap
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
		// ���������ڣ�
		Message message = Message.obtain();
		message.obj = holder ;
		mUIHandler.sendMessage(message);
	}
	
	/**
	 * ��ͼƬ���뵽LruCache����
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
	 * ��ͼƬҪ��ʾ�Ŀ�͸߶�ͼƬ����ѹ��
	 * @param path
	 * @param width
	 * @param height
	 * @return
	 */
	private Bitmap decodeSampleFromPath(String path, int width,
			int height) {
		// ��ȡͼƬ�Ŀ�ߣ�������ͼƬ���ص��ڴ���
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		BitmapFactory.decodeFile(path,options);
		options.inSampleSize = caculateSampleSize(options,width , height);
		// ʹ�û�ȡ��InSampleSize�ٴν���ͼƬ
		options.inJustDecodeBounds = false ;
		Bitmap bitmap = BitmapFactory.decodeFile(path, options);
		return bitmap;
	}
	
	/**
	 * ��������Ŀ�͸ߺ�ʵ�ʵĿ�͸߼���SampleSize
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
	 * ����ImageView��ȡ���
	 * @param imageView
	 * @return
	 */
	@SuppressLint("NewApi")
	private ImageSize getImageViewSize(ImageView imageView) {
		// TODO Auto-generated method stub
		ImageSize imageSize = new ImageSize();
		LayoutParams lp = imageView.getLayoutParams();
		DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
		int width = imageView.getWidth() ; // ��ȡimageview�Ŀ��
		//(lp.width == LayoutParams.WRAP_CONTENT?0:imageView.getWidth()) ;
		if(width <= 0)
		{
			width = lp.width; // ��ȡimageview��layout�Ŀ��
		}
		
		if(width <= 0)
		{
			width = imageView.getMaxWidth() ; // ��������  
		}
		
		if(width <= 0)
		{
			// �������û�õ���������Ļ���
			width = displayMetrics.widthPixels ;
		}
		
		int height = imageView.getHeight() ; // ��ȡimageview�ĸ߶�  �÷����������¼��ݣ������÷�����getObjectViewFieldValue
	//	int height = getObjectViewFieldValue(imageView,"mMaxHeight");
		//(lp.width == LayoutParams.WRAP_CONTENT?0:imageView.getWidth()) ;
		if(height <= 0)
		{
			height = lp.height; // ��ȡimageview��layout�ĸ߶�
		}
		
		if(height <= 0)
		{
			height = imageView.getMaxHeight() ; // ������߶�
		}
		
		if(height <= 0)
		{
			// �������û�õ���������Ļ���
			height = displayMetrics.heightPixels ;
		}
		
		imageSize.width = width ; 
		imageSize.height = height ;
		return imageSize ;
	}
	
	/**
	 * ��ȡִ��ĳ�������ĳ��ֵ
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
			// �÷�����ȴ�mSemaphorePoolThreadHandler.realse�ͷţ��Ž�����һ��
			if(mPoolThreadHandler == null)
			mSemaphorePoolThreadHandler.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mPoolThreadHandler.sendEmptyMessage(0x11);
	}
	/**
	 * �ӻ����л�ȡbitmap
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
