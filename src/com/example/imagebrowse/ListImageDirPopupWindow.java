package com.example.imagebrowse;

import java.util.List;

import com.example.imagebrowse.bean.FolderBean;
import com.example.imagebrowse.utils.ImageLoader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;


public class ListImageDirPopupWindow extends PopupWindow {

	private int mWidth ; 
	private int mHeight ;
	private View mConvertView ; 
	private ListView mListView ; 
	private List<FolderBean> mDatas;
	/**
	 * 文件夹选中的接口回调事件
	 * @author jiangqianghua
	 *
	 */
	public interface OnDirSelectedListener
	{
		void onSelect(FolderBean folderBean);
	}
	
	public OnDirSelectedListener mListener ;
	
	
	public OnDirSelectedListener getmListener() {
		return mListener;
	}


	public void setmListener(OnDirSelectedListener mListener) {
		this.mListener = mListener;
	}


	public ListImageDirPopupWindow(Context context , List<FolderBean> datas){
		calWidthAndHeight(context);
		mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);
		setContentView(mConvertView);
		setHeight(mHeight);
		setWidth(mWidth);
		
		setFocusable(true);
		setTouchable(true);
		setOutsideTouchable(true);  // 设置可以点击外部
		setBackgroundDrawable(new BitmapDrawable()); // 设置点击外部可以消失
		
		setTouchInterceptor(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getAction() == MotionEvent.ACTION_OUTSIDE)
				{
					dismiss(); 
					return true ;
				}
				return false;
			}
		});
		mDatas = datas ;
		initViews(context);
		initEvent();
		
		
	}


	private void initViews(Context context) {
		// TODO Auto-generated method stub
		mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
		mListView.setAdapter(new ListDirAdapter(context, mDatas));
	}
	
	private void initEvent() {
		// TODO Auto-generated method stub
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				if(mListener != null)
				{
					mListener.onSelect(mDatas.get(position));
				}
			}
		});
	}


	/**
	 * 计算popuWindow宽度和高度
	 * @param context
	 */
	private void calWidthAndHeight(Context context) {
		// TODO Auto-generated method stub
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics outMetrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(outMetrics);
		mWidth = outMetrics.widthPixels;
		mHeight = (int) (outMetrics.heightPixels *0.7) ;
		
	}
	
	private class ListDirAdapter extends ArrayAdapter<FolderBean>
	{

		private LayoutInflater mInflater ; 
		private List<FolderBean> mDatas ;
		
		public ListDirAdapter(Context context, List<FolderBean> objects) {
			super(context, 0,objects);
			// TODO Auto-generated constructor stub
			mDatas = objects ;
			mInflater = LayoutInflater.from(context);
		}
		
	    
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder holder = null ;
			if(convertView == null)
			{
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.item_popup_main, null);
				holder.mImg = (ImageView) convertView.findViewById(R.id.id_id_dir_item_image);
				holder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
				holder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) convertView.getTag() ;
			}
			
			FolderBean bean = getItem(position);
			// rerset image
			holder.mImg.setImageResource(R.drawable.pictures_no);
			ImageLoader.getInstance().loadImage(bean.getFirstImgPath(), holder.mImg);
			holder.mDirCount.setText(bean.getCount()+"");
			holder.mDirName.setText(bean.getName());
			return convertView;
		
		}
		
		private class ViewHolder
		{
			ImageView mImg ; 
			TextView mDirName ;
			TextView mDirCount ;
		}
	}
}
