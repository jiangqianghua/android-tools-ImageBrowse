package com.example.imagebrowse;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.imagebrowse.ListImageDirPopupWindow.OnDirSelectedListener;
import com.example.imagebrowse.bean.FolderBean;
import com.example.imagebrowse.utils.ImageLoader;
import com.example.imagebrowse.utils.ImageLoader.Type;

import android.R.animator;
import android.support.v7.app.ActionBarActivity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	private GridView mGridView;
	private RelativeLayout mBottomLy;
	private TextView mDirName;
	private TextView mDirCount;
	/**
	 * ���imgs�б�
	 */
	private List<String> mImgs;
	
	private ImageAdapter mImgAdapter ;
	/**
	 * �ļ���
	 */
	private File mCurrentDir;
	/**
	 * ͼƬ������
	 */
	private int mMaxCount;

	private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

	private ProgressDialog mProgressDialog;
	
	private static final int DATA_LOADED = 0X110;
	
	private ListImageDirPopupWindow mDirPopupWindow ;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			
			if(msg.what == DATA_LOADED)
			{
				mProgressDialog.dismiss();
				// �����ݵ�View��
				data2View();
				
				initDirPopupWindow();
			}
		}


	};
	
	private void initDirPopupWindow() {
		// TODO Auto-generated method stub
		mDirPopupWindow = new ListImageDirPopupWindow(this, mFolderBeans);
		mDirPopupWindow.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss() {
				// TODO Auto-generated method stub
				lightOn() ;
			}

		});
		
		mDirPopupWindow.setmListener(new OnDirSelectedListener() {
			
			@Override
			public void onSelect(FolderBean folderBean) {
				// TODO Auto-generated method stub
				// �����ļ���
				mCurrentDir = new File(folderBean.getDir());
				//����ͼƬ
				mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
					
					@Override
					public boolean accept(File dir, String filename) {
						// TODO Auto-generated method stub
						if(filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
						{
							return true ;
						}
						return false;
					}
				}));
				
				mImgAdapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
				mGridView.setAdapter(mImgAdapter);
				
				mDirCount.setText(mImgs.size()+"");
				mDirName.setText(folderBean.getName());
				
				mDirPopupWindow.dismiss();
			}
		});
	}
	/**
	 * ���ݱ���
	 */
	private void lightOn() {
		// TODO Auto-generated method stub
		WindowManager.LayoutParams lp = getWindow().getAttributes() ;
		lp.alpha = 1.0f ; 
		getWindow().setAttributes(lp);
	}
	/**
	 * �����ݵ�View��
	 */
	private void data2View() {
		// TODO Auto-generated method stub
		if(mCurrentDir == null )
		{
			Toast.makeText(this, "δɨ�赽�κ�ͼƬ", Toast.LENGTH_LONG).show();
			return ;
		}
		/**
		 * ������ת��list
		 */
		mImgs = Arrays.asList(mCurrentDir.list());
		// ��ʼ��д������
		mImgAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
		mGridView.setAdapter(mImgAdapter);
		mDirCount.setText(mMaxCount+"");
		mDirName.setText(mCurrentDir.getName());
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initView();
		initDatas();
		initEvent();
	}

	/**
	 * ��ʼ���¼�
	 */
	private void initEvent() {
		// TODO Auto-generated method stub
		mBottomLy.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
				mDirPopupWindow.showAsDropDown(mBottomLy,0,0);
				lightOff();
			}

		});
	}

	/**
	 * ���ݱ������
	 */
	private void lightOff() {
		// TODO Auto-generated method stub
		WindowManager.LayoutParams lp = getWindow().getAttributes() ;
		lp.alpha = .3f ; 
		getWindow().setAttributes(lp);
		
	}
	/**
	 * �����洢��ͼƬ
	 */
	private void initDatas() {
		// TODO Auto-generated method stub
		// �жϵ�ǰ�洢���Ƿ����
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
		{
			Toast.makeText(this, "��ǰ�洢��������", Toast.LENGTH_LONG).show();
			return ;
		}
		
		mProgressDialog = ProgressDialog.show(this, "", "���ڼ���...");
		
		new Thread()
		{
			public void run() {
				
				Uri  mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI ;
				ContentResolver cr = MainActivity.this.getContentResolver() ;
				/**
				 * ����ͼƬ��������Ƭ�޸�����
				 */
				Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE+"=? or "+MediaStore.Images.Media.MIME_TYPE+"=?" , new String[]{"image/jpeg" ,"image/png"}, MediaStore.Images.Media.DATE_MODIFIED);
				
				// �洢�ļ���·������ֹ�ظ�����
				Set<String> mDirPaths = new HashSet<String>() ;
				while(cursor.moveToNext())
				{
					/**
					 * ��ȡͼƬ·��
					 */
					String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
					
					File parentFile = new File(path).getParentFile();
					if(parentFile == null)
						continue ;
					String dirPath = parentFile.getAbsolutePath() ;
					
					FolderBean folderBean = null ;
					System.out.println("dir:"+dirPath);
					if(mDirPaths.contains(dirPath))
					{
						continue ;
					}
					else
					{
						mDirPaths.add(dirPath);
						folderBean = new FolderBean();
						folderBean.setDir(dirPath);
						folderBean.setFirstImgPath(path);
						
					}
					
					if(parentFile.list() == null)
					{
						continue ;
					}
					
					int picSize = parentFile.list(new FilenameFilter() {
						
						@Override
						public boolean accept(File dir, String filename) {
							// TODO Auto-generated method stub
							if(filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
							{
								return true ;
							}
							return false;
						}
					}).length;
					
					folderBean.setCount(picSize);
					mFolderBeans.add(folderBean);
					if(picSize > mMaxCount)
					{
						mMaxCount = picSize ;
						mCurrentDir = parentFile ;
					}
				}
				
				cursor.close();
				
				// ֪ͨhandler��ɨ��ͼƬ���
				mHandler.sendEmptyMessage(DATA_LOADED);
			};
		}.start();;
	}

	/**
	 * ��ʼ���ؼ�
	 */
	private void initView() {
		// TODO Auto-generated method stub
		mGridView = (GridView) findViewById(R.id.id_gridview);
		mBottomLy = (RelativeLayout) findViewById(R.id.id_bottom_ly);
		mDirName = (TextView) findViewById(R.id.id_dir_name);
		mDirCount = (TextView) findViewById(R.id.id_dir_count);
	}

	
}
