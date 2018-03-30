package com.example.imagebrowse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.imagebrowse.utils.ImageLoader;
import com.example.imagebrowse.utils.ImageLoader.Type;

public class ImageAdapter extends BaseAdapter
{
	private static Set<String> mSelectImg = new HashSet<String>();
	private String mDirPath ;  // 存放文件夹路径
	private List<String> mImgPaths ;   // 存放图片名称
	private LayoutInflater mInflater ; // 用于布局使用
	

	public ImageAdapter(Context context , List<String> mDatas,String dirPath) 
	{
		// TODO Auto-generated constructor stub
		this.mDirPath = dirPath ; 
		this.mImgPaths = mDatas ; 
		mInflater = LayoutInflater.from(context);
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mImgPaths.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mImgPaths.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		final ViewHolder viewHolder ;
		
		if(convertView == null)
		{
			convertView = mInflater.inflate(R.layout.item_gridview, parent,false);
			viewHolder = new ViewHolder();
			viewHolder.mImg = (ImageView) convertView.findViewById(R.id.id_item_image);
			viewHolder.mSelect = (ImageButton) convertView.findViewById(R.id.id_item_select);
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		// 重置状态
		viewHolder.mImg.setImageResource(R.drawable.pictures_no);
		viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
		viewHolder.mImg.setColorFilter(null);
		ImageLoader.getInstance(1,Type.LIFO).loadImage(mDirPath+"/"+mImgPaths.get(position), viewHolder.mImg);
		final String filePath = mDirPath+"/"+mImgPaths.get(position) ;
		// 添加点击事件
		viewHolder.mImg.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				if(mSelectImg.contains(filePath)){
					// 已经被选择
					mSelectImg.remove(filePath);
					viewHolder.mSelect.setImageResource(R.drawable.picture_unselected);
					viewHolder.mImg.setColorFilter(null);
				}
				else
				{
					mSelectImg.add(filePath);
					viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
					viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
				}
			//	notifyDataSetChanged();  防止闪屏，不需要用该方式刷新
			}
		});
		
		if(mSelectImg.contains(filePath)){
			viewHolder.mImg.setColorFilter(Color.parseColor("#77000000"));
			viewHolder.mSelect.setImageResource(R.drawable.pictures_selected);
		}
		return convertView;
	}
	
	private class ViewHolder
	{
		ImageView mImg ; 
		ImageButton mSelect ;
	}
	
}
