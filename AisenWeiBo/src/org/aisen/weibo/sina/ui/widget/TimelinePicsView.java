package org.aisen.weibo.sina.ui.widget;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.m.common.utils.KeyGenerator;
import com.m.common.utils.Logger;
import com.m.common.utils.SystemUtils;
import com.m.common.utils.SystemUtils.NetWorkType;
import com.m.common.utils.Utils;
import com.m.component.bitmaploader.BitmapLoader;
import com.m.component.bitmaploader.core.BitmapOwner;
import com.m.component.bitmaploader.core.ImageConfig;
import com.m.component.bitmaploader.core.LruMemoryCache;
import com.m.ui.fragment.ABaseFragment;

import org.aisen.weibo.sina.R;
import org.aisen.weibo.sina.base.AppSettings;
import org.aisen.weibo.sina.support.bean.PicSize;
import org.aisen.weibo.sina.support.compress.TimelineThumbBitmapCompress;
import org.aisen.weibo.sina.ui.fragment.basic.BizFragment;
import org.aisen.weibo.sina.ui.fragment.comment.TimelineCommentFragment;
import org.aisen.weibo.sina.ui.fragment.profile.PhotosFragment;
import org.aisen.weibo.sina.ui.fragment.timeline.TimelineRepostFragment;
import org.sina.android.bean.PicUrls;
import org.sina.android.bean.StatusContent;

import java.io.File;


/**
 * timeline的图片容器，根据图片个数动态布局ImageView
 * 
 * @author wangdan
 *
 */
public class TimelinePicsView extends ViewGroup implements BitmapOwner {

	public static final String TAG = TimelinePicsView.class.getSimpleName();

	private int mWidth;
	
	private int gap;
	
	private PicUrls[] picUrls;
	
	private Rect[] picRects;
	
	private StatusContent mStatusContent;
	
	private boolean large = true;
	
	private BizFragment bizFragment;
	private ABaseFragment ownerFragment;

    private PicSize picSize;// 当只显示了一个图片时有效

    private static LruMemoryCache<String, PicSize> picSizeCache;

    static {
        picSizeCache = new LruMemoryCache<String, PicSize>(100) {

        };
    }

	public TimelinePicsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		init();
	}

	public TimelinePicsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init();
	}

	public TimelinePicsView(Context context) {
		super(context);
		
		init();
	}
	
	private void init() {
		gap = getResources().getDimensionPixelSize(R.dimen.gap_pics);
		
		Logger.v(TAG, String.format("screenWidth = %d", SystemUtils.getScreenWidth()));
		Logger.v(TAG, String.format("gap = %d, width = %d", gap, mWidth));
	}
	
	private void recyle() {
		for (int i = 0; i < getChildCount(); i++) {
			ImageView imgView = (ImageView) getChildAt(i);
			
			imgView.setImageDrawable(BitmapLoader.getLoadingDrawable(imgView));
		}
	}
	
	private void setMobileTimelinePicsView() {
        int maxWidth = SystemUtils.getScreenWidth() - Utils.dip2px(18 * 2);
        mWidth = Math.round(maxWidth * 1.0f * 4 / 5);

		picRects = null;
		
		int size = picUrls.length;
		
		int imgW = Math.round((mWidth - 2 * gap) * 1.0f / 3.0f);
		int imgH = imgW;
		LinearLayout.LayoutParams layoutParams = null;
		
		// 4个特殊情况，上2个下2个
		if (size == 4) {
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, imgH * 2 + gap);
			
			picRects = new Rect[4];
			
			Rect rect = new Rect(0, 0, imgW, imgH);
			picRects[0] = rect;
			rect = new Rect(imgW + gap, 0, imgW * 2 + gap, imgH);
			picRects[1] = rect;
			rect = new Rect(0, imgH + gap, imgW, imgH * 2 + gap);
			picRects[2] = rect;
			rect = new Rect(imgW + gap, imgH + gap, imgW * 2 + gap, imgH * 2 + gap);
			picRects[3] = rect;
		}
		else {
            int oneWidth = 0;
            if (large)
                oneWidth = mWidth;
            else
                oneWidth = maxWidth / 3;
			int height = 0;
			switch (size) {
			case 1:
                float maxRadio = 13 * 1.0f / 16;
                // 初始状态
                if (picSize == null) {
                    height = oneWidth;
                }
                else {
                    if (large) {
                        // 宽度跟高度大小近似
                        if (picSize.getWidth() * 1.0f / picSize.getHeight() >= maxRadio) {
                            // 根据宽度计算高度
                            height = Math.round(oneWidth *
                                    // 原图的比例
                                    (picSize.getHeight() * 1.0f / picSize.getWidth()));
                        }
                        // 高度比较大于宽度，取屏幕一半为最大宽度
                        else {
                            height = mWidth;
                            // 高度比较大时，截图一部分
                            if (picSize.getHeight() > TimelineThumbBitmapCompress.maxHeight
                                    && picSize.getWidth() <= 440) {
                                oneWidth = TimelineThumbBitmapCompress.cutWidth;// 发现440是新浪截取的宽度
                                height = TimelineThumbBitmapCompress.cutHeight;
                            }
                            else {
                                oneWidth = Math.round(height / (picSize.getHeight() * 1.0f / picSize.getWidth()));
                            }
                        }
                    }
                    else {
                        height = Math.round(oneWidth *
                                // 原图的比例
                                (picSize.getHeight() * 1.0f / picSize.getWidth()));
                        if (height > oneWidth) {
                            height = oneWidth;
                            oneWidth = Math.round(height / (picSize.getHeight() * 1.0f / picSize.getWidth()));
                        }
                    }
                }
				break;
			case 2:
			case 3:
				height = imgH;
				break;
			case 5:
			case 6:
				height = imgH * 2 + gap;
				break;
			case 7:
			case 8:
			case 9:
				height = imgH * 3 + gap * 2;
				break;
			}
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, height);

            // 当只有一个图片的时候，特殊处理
            if (size == 1) {
                Rect oneRect = new Rect();
                oneRect.left = 0;
                oneRect.top = 0;
                oneRect.right = oneWidth;
                oneRect.bottom = height;

                int imgSize = oneRect.right - oneRect.left;
                if (large) {
                    if (imgSize < Utils.dip2px(100))
                        oneRect.left = (maxWidth - imgSize) / 2;
                }
                else {
                    oneRect.left = Utils.dip2px(32);
                }
                oneRect.right += oneRect.left;

                picRects = new Rect[]{ oneRect };
            }
            else {
                picRects = new Rect[size];
                for (int i = 0; i < size; i++)
                    picRects[i] = getSmallRectArr()[i];
            }
		}
		
		setLayoutParams(layoutParams);
		
		displayPics();
		
		// 重新绘制
		requestLayout();
	}

    private static Rect[] small9ggRectArr = null;
	private Rect[] getSmallRectArr() {
        if (small9ggRectArr != null)
            return small9ggRectArr;

		int imgW = Math.round((mWidth - 2 * gap) * 1.0f / 3.0f);
		int imgH = imgW;
		
		Rect[] tempRects = new Rect[9];
		
		Rect rect = new Rect(0, 0, imgW, imgH);
		tempRects[0] = rect;
		rect = new Rect(imgW + gap, 0, imgW * 2 + gap, imgH);
		tempRects[1] = rect;
		rect = new Rect(mWidth - imgW, 0, mWidth, imgH);
		tempRects[2] = rect;
		
		rect = new Rect(0, imgH + gap, imgW, imgH * 2 + gap);
		tempRects[3] = rect;
		rect = new Rect(imgW + gap, imgH + gap, imgW * 2 + gap, imgH * 2 + gap);
		tempRects[4] = rect;
		rect = new Rect(mWidth - imgW, imgH + gap, mWidth, imgH * 2 + gap);
		tempRects[5] = rect;
		
		rect = new Rect(0, imgH * 2 + gap * 2, imgW, imgH * 3 + gap * 2);
		tempRects[6] = rect;
		rect = new Rect(imgW + gap, imgH * 2 + gap * 2, imgW * 2 + gap, imgH * 3 + gap * 2);
		tempRects[7] = rect;
		rect = new Rect(mWidth - imgW, imgH * 2 + gap * 2, mWidth, imgH * 3 + gap * 2);
		tempRects[8] = rect;
		
        small9ggRectArr = tempRects;
		return small9ggRectArr;
	}
	
	private void setWifiTimelinePicsView() {
		picRects = null;

        int gap = getResources().getDimensionPixelSize(R.dimen.gap_pics);
        mWidth = SystemUtils.getScreenWidth() - 2 * getResources().getDimensionPixelSize(R.dimen.comm_h_gap);

		int size = picUrls.length;
		int random = 0;
		
		LinearLayout.LayoutParams layoutParams = null;
		Rect[] tempRects = new Rect[size];
		switch (size) {
		case 1:
            int imgW = mWidth;
            int imgH = Math.round(imgW * 4.0f / 3.0f);

            String url = getStatusMulImage(picUrls[0].getThumbnail_pic());
            File file = BitmapLoader.getInstance().getCacheFile(url);
            if (file != null && file.exists()) {
                long time = System.currentTimeMillis();
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                Logger.w(String.format("耗时%s毫秒", String.valueOf(System.currentTimeMillis() - time)));
                imgH = Math.round(imgW * opts.outHeight * 1.0f / opts.outWidth);
                if (imgH > SystemUtils.getScreenHeight())
                    imgH = imgW;
            }

			Rect rect = new Rect(0, 0, imgW, imgH);
			tempRects[0] = rect;
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, imgH);
			break;
		case 2:
			imgW = (mWidth - gap) / 2;
			imgH = Math.round(imgW * 4.0f / 3.0f);
			
			rect = new Rect(0, 0, imgW, imgH);
			tempRects[0] = rect;
			rect = new Rect(imgW + gap, 0, mWidth, imgH);
			tempRects[1] = rect;
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, imgH);
			break;
		case 3:
			int imgW02 = Math.round((mWidth - gap) * 3.0f / 7.0f);
			int imgH02 = imgW02;
			int imgW01 = Math.round((mWidth - gap) * 4.0f / 7.0f);
			int imgH01 = imgH02 * 2 + gap;
			
			try {
				random = (int) (mStatusContent.getId() % 2);
			} catch (Exception e) {
			}
			// 见/doc/3_0.png
			if (random == 0) {
				rect = new Rect(0, 0, imgW01, imgH01);
				tempRects[0] = rect;
				rect = new Rect(gap + imgW01, 0, mWidth, imgH02);
				tempRects[1] = rect;
				rect = new Rect(gap + imgW01, imgH02 + gap, mWidth, imgH01);
				tempRects[2] = rect;
			}
			// 见/doc/3_1.png
			else if (random == 1) {
				rect = new Rect(0, 0, imgW02, imgH02);
				tempRects[0] = rect;
				rect = new Rect(0, imgH02 + gap, imgW02, imgH01);
				tempRects[1] = rect;
				rect = new Rect(gap + imgW02, 0, mWidth, imgH01);
				tempRects[2] = rect;
			}
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, imgH01);
			break;
		case 4:
			imgW = Math.round((mWidth - gap) * 1.0f / 2);
			imgH = Math.round(imgW * 4.0f / 3.0f);
			
			rect = new Rect(0, 0, imgW, imgH);
			tempRects[0] = rect;
			rect = new Rect(gap + imgW, 0, mWidth, imgH);
			tempRects[1] = rect;
			rect = new Rect(0, imgH + gap, imgW, imgH * 2 + gap);
			tempRects[2] = rect;
			rect = new Rect(gap + imgW, imgH + gap, mWidth, imgH * 2 + gap);
			tempRects[3] = rect;
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, imgH * 2 + gap);
			break;
		case 5:
			imgW01 = Math.round((mWidth - gap) * 1.0f / 2);
			imgH01 = Math.round(imgW01 * 4.0f / 3.0f);
			
			imgW02 = Math.round((mWidth - gap * 2) * 1.0f / 3);
			imgH02 = Math.round(imgW02 * 4.0f / 3.0f);
			
			try {
				random = (int) (mStatusContent.getId() % 2);
			} catch (Exception e) {
			}
			int height = imgH01 + imgH02 + gap;
			// 见/doc/5_0.png
			if (random == 0) {
				rect = new Rect(0, 0, imgW01, imgH01);
				tempRects[0] = rect;
				rect = new Rect(gap + imgW01, 0, mWidth, imgH01);
				tempRects[1] = rect;
				rect = new Rect(0, imgH01 + gap, imgW02, height);
				tempRects[2] = rect;
				rect = new Rect(imgW02 + gap, imgH01 + gap, imgW02 * 2 + gap, height);
				tempRects[3] = rect;
				rect = new Rect(mWidth - imgW02, imgH01 + gap, mWidth, height);
				tempRects[4] = rect;
			}
			// 见/doc/5_1.png
			else if (random == 1) {
				rect = new Rect(0, 0, imgW02, imgH02);
				tempRects[0] = rect;
				rect = new Rect(imgW02 + gap, 0, imgW02 * 2 + gap, imgH02);
				tempRects[1] = rect;
				rect = new Rect(mWidth - imgW02, 0, mWidth, imgH02);
				tempRects[2] = rect;
				rect = new Rect(0, imgH02 + gap, imgW01, height);
				tempRects[3] = rect;
				rect = new Rect(gap + imgW01, imgH02 + gap, mWidth, height);
				tempRects[4] = rect;
			}
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, imgH01 + imgH02 + gap);
			break;
		case 6:
			imgW01 = Math.round((mWidth - 2 * gap) * 1.0f / 3.0f);
			imgH01 = Math.round(imgW01 * 4.0f / 3.0f);
			
			imgW02 = imgW01 * 2 + gap;
			imgH02 = imgH01 * 2 + gap;
			
			height = imgH01 + imgH02 + gap;
			
			try {
				random = (int) (mStatusContent.getId() % 3);
			} catch (Exception e) {
			}
			// 见/doc/6_0.png
			if (random == 0) {
				rect = new Rect(0, 0, imgW01, imgH01);
				tempRects[0] = rect;
				rect = new Rect(gap + imgW01, 0, imgW01 * 2 + gap, imgH01);
				tempRects[1] = rect;
				rect = new Rect(mWidth - imgW01, 0, mWidth, imgH01);
				tempRects[2] = rect;
				rect = new Rect(0, imgH01 + gap, imgW02, height);
				tempRects[3] = rect;
				rect = new Rect(imgW02 + gap, imgH01 + gap, mWidth, height - imgH01 - gap);
				tempRects[4] = rect;
				rect = new Rect(imgW02 + gap, height - imgH01, mWidth, height);
				tempRects[5] = rect;
			}
			// 见/doc/6_1.png
			else if (random == 1) {
				rect = new Rect(0, 0, imgW01, imgH01);
				tempRects[0] = rect;
				rect = new Rect(0, imgH01 + gap, imgW01, imgH01 * 2 + gap);
				tempRects[1] = rect;
				rect = new Rect(gap + imgW01, 0, mWidth, imgH02);
				tempRects[2] = rect;
				rect = new Rect(0, height - imgH01, imgW01, height);
				tempRects[3] = rect;
				rect = new Rect(gap + imgW01, height - imgH01, gap + imgW01 * 2, height);
				tempRects[4] = rect;
				rect = new Rect(imgW02 + gap, height - imgH01, mWidth, height);
				tempRects[5] = rect;
			}
			// 见/doc/6_2.png
			else if (random == 2) {
				rect = new Rect(0, 0, imgW02, imgH02);
				tempRects[0] = rect;
				rect = new Rect(imgW02 + gap, 0, mWidth, imgH01);
				tempRects[1] = rect;
				rect = new Rect(gap + imgW02, imgH01 + gap, mWidth, imgH01 * 2 + gap);
				tempRects[2] = rect;
				rect = new Rect(0, height - imgH01, imgW01, height);
				tempRects[3] = rect;
				rect = new Rect(gap + imgW01, height - imgH01, gap + imgW01 * 2, height);
				tempRects[4] = rect;
				rect = new Rect(imgW02 + gap, height - imgH01, mWidth, height);
				tempRects[5] = rect;
			}
			// 见/doc/6_3.png
			else if (random == 3) {
				rect = new Rect(0, 0, imgW01, imgH01);
				tempRects[0] = rect;
				rect = new Rect(gap + imgW01, 0, imgW01 * 2 + gap, imgH01);
				tempRects[1] = rect;
				rect = new Rect(mWidth - imgW01, 0, mWidth, imgH01);
				tempRects[2] = rect;
				rect = new Rect(0, imgH01 + gap, imgW01, imgH01 * 2 + gap);
				tempRects[3] = rect;
				rect = new Rect(0, height - imgH01, imgW01, height);
				tempRects[4] = rect;
				rect = new Rect(imgW01 + gap, imgH01 + gap, mWidth, height);
				tempRects[5] = rect;
			}
			layoutParams = new LinearLayout.LayoutParams(mWidth, height);
			break;
		case 7:
			imgW01 = Math.round((mWidth - 2 * gap) * 1.0f / 3.0f);
			imgH01 = Math.round(imgW01 * 4.0f / 3.0f);
			
			imgW02 = mWidth;
			imgH02 = Math.round(imgW02 * 3.0f / 4.0f);
			
			height = imgH01 * 2 + imgH02 + gap * 2;
			
			rect = new Rect(0, 0, imgW01, imgH01);
			tempRects[0] = rect;
			rect = new Rect(gap + imgW01, 0, imgW01 * 2 + gap, imgH01);
			tempRects[1] = rect;
			rect = new Rect(mWidth - imgW01, 0, mWidth, imgH01);
			tempRects[2] = rect;
			
			rect = new Rect(0, imgH01 + gap, imgW02, imgH01 + gap + imgH02);
			tempRects[3] = rect;
			
			rect = new Rect(0, imgH01 + gap * 2 + imgH02, imgW01, height);
			tempRects[4] = rect;
			rect = new Rect(gap + imgW01, imgH01 + gap * 2 + imgH02, imgW01 * 2 + gap, height);
			tempRects[5] = rect;
			rect = new Rect(mWidth - imgW01, imgH01 + gap * 2 + imgH02, mWidth, height);
			tempRects[6] = rect;
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, height);
			break;
		case 8:
			imgW01 = Math.round((mWidth - 2 * gap) * 1.0f / 3.0f);
			imgH01 = Math.round(imgW01 * 4.0f / 3.0f);
			
			imgW02 = Math.round((mWidth - gap) * 1.0f / 2.0f);
			imgH02 = Math.round(imgW02 * 4.0f / 3.0f);
			
			height = imgH01 * 2 + imgH02 + gap * 2;
			
			rect = new Rect(0, 0, imgW01, imgH01);
			tempRects[0] = rect;
			rect = new Rect(gap + imgW01, 0, imgW01 * 2 + gap, imgH01);
			tempRects[1] = rect;
			rect = new Rect(mWidth - imgW01, 0, mWidth, imgH01);
			tempRects[2] = rect;
			
			rect = new Rect(0, imgH01 + gap, imgW02, imgH01 + gap + imgH02);
			tempRects[3] = rect;
			rect = new Rect(imgW02 + gap, imgH01 + gap, mWidth, imgH01 + gap + imgH02);
			tempRects[4] = rect;
			
			rect = new Rect(0, imgH01 + gap * 2 + imgH02, imgW01, height);
			tempRects[5] = rect;
			rect = new Rect(gap + imgW01, imgH01 + gap * 2 + imgH02, imgW01 * 2 + gap, height);
			tempRects[6] = rect;
			rect = new Rect(mWidth - imgW01, imgH01 + gap * 2 + imgH02, mWidth, height);
			tempRects[7] = rect;
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, height);
			break;
		case 9:
			imgW01 = Math.round((mWidth - 2 * gap) * 1.0f / 3.0f);
			imgH01 = Math.round(imgW01 * 4.0f / 3.0f);
			
			height = imgH01 * 2 + imgH01 + gap * 2;
			
			rect = new Rect(0, 0, imgW01, imgH01);
			tempRects[0] = rect;
			rect = new Rect(gap + imgW01, 0, imgW01 * 2 + gap, imgH01);
			tempRects[1] = rect;
			rect = new Rect(mWidth - imgW01, 0, mWidth, imgH01);
			tempRects[2] = rect;
			
			rect = new Rect(0, imgH01 + gap, imgW01, imgH01 * 2 + gap);
			tempRects[3] = rect;
			rect = new Rect(gap + imgW01, imgH01 + gap, imgW01 * 2 + gap, imgH01 * 2 + gap);
			tempRects[4] = rect;
			rect = new Rect(mWidth - imgW01, imgH01 + gap, mWidth, imgH01 * 2 + gap);
			tempRects[5] = rect;
			
			rect = new Rect(0, imgH01 + gap * 2 + imgH01, imgW01, height);
			tempRects[6] = rect;
			rect = new Rect(gap + imgW01, imgH01 + gap * 2 + imgH01, imgW01 * 2 + gap, height);
			tempRects[7] = rect;
			rect = new Rect(mWidth - imgW01, imgH01 + gap * 2 + imgH01, mWidth, height);
			tempRects[8] = rect;
			
			layoutParams = new LinearLayout.LayoutParams(mWidth, height);
			break;
		}

		setLayoutParams(layoutParams);
		
		picRects = tempRects;
		
		displayPics();
		
		// 重新绘制
		requestLayout();
	}
	
	public void displayPics() {
		if (picRects == null || picUrls == null || picUrls.length == 0)
			return;
		
		for (int i = 0; i < getChildCount(); i++) {
			ImageView imgView = (ImageView) getChildAt(i);

			// 隐藏多余的View
			if (i >= picRects.length) {
				getChildAt(i).setVisibility(View.GONE);
				
				imgView.setImageDrawable(BitmapLoader.getLoadingDrawable(imgView));
			}
			else {
				Rect imgRect = picRects[i];
				
				imgView.setVisibility(View.VISIBLE);
				// 如果是一个图片，就显示大一点
				int size = picUrls.length;
				if (size == 1) {
                    if (isWifiTimeline())
					    imgView.setScaleType(ScaleType.FIT_CENTER);
                    else
                        imgView.setScaleType(ScaleType.FIT_XY);
				}
				else {
					imgView.setScaleType(ScaleType.CENTER_CROP);
				}
                imgView.setLayoutParams(new LayoutParams(imgRect.right - imgRect.left, imgRect.bottom - imgRect.top));

				String url = getStatusMulImage(picUrls[i].getThumbnail_pic());

                TimelineImageConfig config = new TimelineImageConfig();
                config.setShowWidth(imgRect.right - imgRect.left);
                config.setShowHeight(imgRect.bottom - imgRect.top);
                config.setSize(picUrls.length);
				if (large) {
                    config.setId("status_large");
                }
				else
					config.setId("status_thumb");
				config.setLoadfaildRes(R.drawable.bg_timeline_loading);
				config.setLoadingRes(R.drawable.bg_timeline_loading);
				if (large) {
					config.setMaxWidth(imgRect.right - imgRect.left);
					config.setMaxHeight(imgRect.bottom - imgRect.top);
				}
//				else {
//					config.setMaxWidth(imgRect.right - imgRect.left);
//					config.setMaxHeight(0);
//				}

                config.setBitmapCompress(TimelineThumbBitmapCompress.class);
//				else if (AppSettings.getPicLargeMode() == 1)
//					config.setBitmapCompress(Timeline9ggBitmapCompress.class);
//				else
//					config.setBitmapCompress(TimelineBitmapCompress.class);

                GifHintImageView gifView = (GifHintImageView) imgView;
				// gif图片
                gifView.setHint(url);
                // 是否截取了
                if (picUrls.length == 1 && large &&
                                    config.getShowWidth() == TimelineThumbBitmapCompress.cutWidth &&
                                    config.getShowHeight() == TimelineThumbBitmapCompress.cutHeight) {
                    gifView.setCut(true);
                }
                else {
                    gifView.setCut(false);
                }
				BitmapLoader.getInstance().display(this, url, imgView, config);
				
				if (bizFragment != null) {
					bizFragment.previousPics(imgView, mStatusContent, i);
				}
			}
		}
	}
	
	private String getStatusMulImage(String thumbImage) {
		if (large) {
            if (AppSettings.isLoadOrigPic())
                return thumbImage.replace("thumbnail", "large");

			return thumbImage.replace("thumbnail", "bmiddle");
		}
		else {
			return thumbImage;
		}
	}
	
    private PicSize getPicSize(PicUrls picUrls) {
        String url = getStatusMulImage(picUrls.getThumbnail_pic());

        return picSizeCache.get(KeyGenerator.generateMD5(url));
    }

    public void checkPicSize() {
        if (picUrls != null && picUrls.length == 1 && picSize == null) {
            String url = getStatusMulImage(picUrls[0].getThumbnail_pic());

            String key = KeyGenerator.generateMD5(url);

            Logger.v(TAG, "checkPicSize()--- " + key);
            File file = BitmapLoader.getInstance().getCacheFile(url);
            if (file.exists()) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(file.getAbsolutePath(), opts);

                picSize = new PicSize();
                picSize.setKey(key);
                picSize.setWidth(opts.outWidth);
                picSize.setHeight(opts.outHeight);

                // 放入内存
                picSizeCache.put(picSize.getKey(), picSize);

                // 重新规划尺寸
                calculatePicSize();
            }
        }
    }

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (picRects == null)
			return;
		
		for (int i = 0; i < getChildCount(); i++) {
			// 隐藏多余的View
			if (i < picRects.length) {
				Rect imgRect = picRects[i];
				
				ImageView childView = (ImageView) getChildAt(i);
				
//				int size = picUrls.length;
//                if (large && ownerFragment != null && (ownerFragment instanceof TimelineCommentsFragment || ownerFragment instanceof TimelineRepostFragment)) {
//                    childView.layout(imgRect.left, imgRect.top, imgRect.right, imgRect.bottom);
//                }
//				else if (size == 1) {
//                    int height = imgRect.bottom - imgRect.top;
//                    height = getOneHeight(height);
//                    childView.layout(imgRect.left, imgRect.top, height, height);
//				}
//				else {
//					childView.layout(imgRect.left, imgRect.top, imgRect.right, imgRect.bottom);
//				}

                childView.layout(imgRect.left, imgRect.top, imgRect.right, imgRect.bottom);
			}
			else {
				break;
			}
		}
	}
	
	public void release() {
		Logger.v(TAG, "释放资源");
		
		mStatusContent = null;
		
		for (int i = 0; i < getChildCount(); i++) {
			ImageView imgView = (ImageView) getChildAt(i);
			imgView.setImageDrawable(BitmapLoader.getLoadingDrawable(imgView));
		}
	}
	
	public void setPics(StatusContent status, BizFragment bizFragment, ABaseFragment ownerFragment) {
		this.bizFragment = bizFragment;
		this.ownerFragment = ownerFragment;
		
		boolean replace = true;
		// 如果内容发送了改变
		if (mStatusContent != null && String.valueOf(mStatusContent.getId()).equals(String.valueOf(status.getId())))
			replace = false;
		
		// 如果图片模式是小图
		if (AppSettings.getPictureMode() == 0) {
			if (large)
				replace = true;
			
			large = false;
		}
		// 如果图片模式是大图
		else if (AppSettings.getPictureMode() == 1) {
			if (!large)
				replace = true;
			
			large = true;
		}
		// 图片模式是自动，且当前是WIFI网络
		else if (AppSettings.getPictureMode() == 2 && SystemUtils.getNetworkType() == NetWorkType.wifi) {
			// 如果当前不是large
			if (!large) 
				replace = true;
			
			large = true;
		}
		// 图片模式是自动，且当前是Mobile网络
		else if (AppSettings.getPictureMode() == 2 && SystemUtils.getNetworkType() != NetWorkType.wifi) {
			// 如果当前不是large
			if (large) 
				replace = true;
			
			large = false;
		}

//        if (!replace) return;
		
		mStatusContent = status;

		PicUrls[] picUrls = mStatusContent.getPic_urls();

		this.picUrls = picUrls;
		
		if (ownerFragment instanceof PhotosFragment) {
			mWidth = SystemUtils.getScreenWidth();
			large = true;
		}
		
		if (picUrls == null || picUrls.length == 0) {
			recyle();
			
			setVisibility(View.GONE);
		}
		else {
            if (picUrls.length == 1)
                picSize = getPicSize(picUrls[0]);

			setVisibility(View.VISIBLE);
			
			calculatePicSize();
		}
	}

    // 根据图片尺寸
    private void calculatePicSize() {
        if (isWifiTimeline()) {
            setWifiTimelinePicsView();
        }
        else {
            setMobileTimelinePicsView();
        }
    }

    // 是否是绘制WIFI模式
    private boolean isWifiTimeline() {
        if (ownerFragment == null)
            return large;

        return large && (ownerFragment instanceof TimelineCommentFragment ||
                ownerFragment instanceof TimelineRepostFragment);
    }

	@Override
	public boolean canDisplay() {
		if (ownerFragment != null)
			return ownerFragment.canDisplay();
		
		return true;
	}

    public static class TimelineImageConfig extends ImageConfig {

        private int size;

        private int showWidth;

        private int showHeight;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getShowWidth() {
            return showWidth;
        }

        public void setShowWidth(int showWidth) {
            this.showWidth = showWidth;
        }

        public int getShowHeight() {
            return showHeight;
        }

        public void setShowHeight(int showHeight) {
            this.showHeight = showHeight;
        }
    }

}
