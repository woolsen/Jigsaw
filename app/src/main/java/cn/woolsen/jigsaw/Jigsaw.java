package cn.woolsen.jigsaw;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.customview.widget.ViewDragHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Jigsaw extends ViewGroup {

    private static final boolean DEBUG = false;

    public static final int NO_MOVE = -1;
    public static final int UP_MOVE = 1;
    public static final int DOWN_MOVE = 2;
    public static final int LEFT_MOVE = 3;
    public static final int RIGHT_MOVE = 4;

    private int mRowCount;
    private int mColumnCount;

    private Drawable mDrawable;

    private int[] mOriginIdMap;
    private int[] mIdMap;
    private int[] mMoveMap;
    private View[] mImageViewMap;

    private Paint mLinePaint;

    private ViewDragHelper mViewDragHelper;

    private float mPerWidth = 0;
    private float mPerHeight = 0;

    private OnSuccessListener mOnSuccessListener;

    public Jigsaw(Context context) {
        this(context, null);
    }

    public Jigsaw(Context context, AttributeSet attrs) {
        this(context, attrs, R.style.jigsawStyle);
    }

    public Jigsaw(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Jigsaw, defStyleAttr, R.style.jigsawStyle);

        mRowCount = a.getInt(R.styleable.Jigsaw_rowCount, 3);
        mColumnCount = a.getInt(R.styleable.Jigsaw_columnCount, 3);

        mDrawable = a.getDrawable(R.styleable.Jigsaw_image);
        if (mDrawable == null) {
            mDrawable = ContextCompat.getDrawable(context, R.drawable.android);
        }

        mLinePaint = new Paint();
        mLinePaint.setColor(Color.LTGRAY);
        mLinePaint.setStrokeWidth(2f);
        mLinePaint.setStyle(Paint.Style.STROKE);

        DragCallback callback = new DragCallback();
        mViewDragHelper = ViewDragHelper.create(this, callback);
        callback.setDrager(mViewDragHelper);

        a.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initJigsaw();
    }

    private void initJigsaw() {
        removeAllViews();
        View[] views = generateImageView(mDrawable, mRowCount, mColumnCount);
        for (View view : views) {
            addView(view);
        }
    }

    public void setOnSuccessListener(OnSuccessListener listener) {
        mOnSuccessListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;
        mPerWidth = 1f * width / mColumnCount;
        mPerHeight = 1f * height / mRowCount;

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                child.layout(0, 0, 0, 0);
                return;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            final float top = 1.0f * lp.i * mPerHeight;
            final float left = 1.0f * lp.j * mPerWidth;
//            System.out.println("left: " + (int) (left + 0.5f) +
//                    ", top: " + (int) (top + 0.5f) +
//                    ", right: " + (int) (left + mPerWidth + 0.5f) +
//                    ", bottom: " + (int) (top + mPerHeight + 0.5f));
            child.layout((int) (left + 0.5f),
                    (int) (top + 0.5f),
                    (int) (left + mPerWidth + 0.5f),
                    (int) (top + mPerHeight + 0.5f));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();
        final int height = getHeight();
        mPerWidth = 1f * width / mColumnCount;
        mPerHeight = 1f * height / mRowCount;

        canvas.save();
        for (int i = 1; i < mRowCount; i++) {
            canvas.translate(0, mPerHeight);
            canvas.drawLine(0, 0, width, 0, mLinePaint);
        }
        canvas.restore();

        canvas.save();
        for (int i = 1; i < mColumnCount; i++) {
            canvas.translate(mPerWidth, 0);
            canvas.drawLine(0, 0, 0, height, mLinePaint);
        }
        canvas.restore();
    }

    private View[] generateImageView(Drawable drawable, int rowCount, int columnCount) {
        //切割图片
        Bitmap[] bitmaps = new Bitmap[rowCount * columnCount];
        final int w = drawable.getIntrinsicWidth();
        final int h = drawable.getIntrinsicHeight();
        final float perWidth = 1f * w / columnCount;
        final float perHeight = 1f * h / rowCount;

        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ?
                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap originBitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(originBitmap);
        drawable.setBounds(0, 0, w, h);
        System.out.println("w = " + w + ", h = " + h);
        drawable.draw(canvas);

        int index = 0;
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
//                System.out.println("x = " + (int) (perWidth * j + 0.5f) +
//                        ", y = " + (int) (perHeight * i + 0.5f) +
//                        ", width = " + (int) (perWidth + 0.5f) +
//                        ", height = " + (int) (perHeight + 0.5f));
                Bitmap bitmap = Bitmap.createBitmap(originBitmap,
                        (int) (perWidth * j),
                        (int) (perHeight * i),
                        (int) (perWidth + 0.5f),
                        (int) (perHeight + 0.5f));
                bitmaps[index++] = bitmap;
            }
        }

        //生成ImageView
        mImageViewMap = new ImageView[rowCount * columnCount];
        mIdMap = new int[rowCount * columnCount];
        mOriginIdMap = new int[rowCount * columnCount];
        index = 0;
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {
                ImageView view = new ImageView(getContext());
                view.setId(generateViewId());
                view.setImageBitmap(bitmaps[index]);
                view.setScaleType(ImageView.ScaleType.FIT_XY);

                LayoutParams lp = new LayoutParams(i, j);
                view.setLayoutParams(lp);

                mIdMap[index] = view.getId();
                mOriginIdMap[index] = view.getId();
                mImageViewMap[index] = view;

                index++;
            }
        }

        mMoveMap = new int[rowCount * columnCount];
        for (int i = 0, max = rowCount * columnCount; i < max; i++) {
            mMoveMap[i] = NO_MOVE;
        }
        setMovement(rowCount - 2, columnCount - 1, DOWN_MOVE);
        setMovement(rowCount - 1, columnCount - 2, RIGHT_MOVE);

        return mImageViewMap;
    }

    private void setMovement(int i, int j, int movement) {
        if (0 <= i && i < mRowCount && 0 <= j && j < mColumnCount) {
            mMoveMap[i * mColumnCount + j] = movement;
        }
    }

    private void setId(int i, int j, int id) {
        if (0 <= i && i < mRowCount && 0 <= j && j < mColumnCount) {
            mIdMap[i * mColumnCount + j] = id;
        }
    }

    private int getMovement(View view) {
        int position = findPositionById(view.getId());
        return mMoveMap[position];
    }

    public void setRowCount(int rowCount) throws IllegalAccessException {
        if (rowCount < 3) {
            throw new IllegalAccessException("RowCount can't less than 3");
        }
        mRowCount = rowCount;
        initJigsaw();
    }

    public int getRowCount() {
        return mRowCount;
    }

    public void setColumnCount(int columnCount) throws IllegalAccessException {
        if (columnCount < 3) {
            throw new IllegalAccessException("ColumnCount can't less than 3");
        }
        mColumnCount = columnCount;
        initJigsaw();
    }

    public int getColumnCount() {
        return mColumnCount;
    }

    public void start() {
        if (DEBUG) {
            debugStart();
            return;
        }
        int max = mColumnCount * mRowCount - 1;
        List<View> views = new ArrayList<>(Arrays.asList(mImageViewMap).subList(0, max));
        Random r = new Random();
        int index = 0;
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                final int rInt = r.nextInt(views.size());
                final View view = views.get(rInt);
                views.remove(view);
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                lp.i = i;
                lp.j = j;
                mIdMap[index] = view.getId();
                index++;
                if (index == max) {
                    break;
                }
            }
        }
        mImageViewMap[max].setVisibility(GONE);
        requestLayout();
    }

    private void debugStart() {
        int max = mColumnCount * mRowCount - 1;
        int index = 0;
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                final View view = mImageViewMap[index];
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                lp.i = i;
                lp.j = j;
                mIdMap[index] = view.getId();
                index++;
                if (index == max) {
                    break;
                }
            }
        }
        mImageViewMap[max].setVisibility(GONE);
        requestLayout();
        resolveMovement(mImageViewMap[max-1]);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    public boolean isSuccess() {
        for (int i = 0, max = mRowCount * mColumnCount - 1; i < max; i++) {
            if (mOriginIdMap[i] != mIdMap[i]) {
                return false;
            }
        }
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isSuccess()) {
            mViewDragHelper.processTouchEvent(event);
        }
        return true;
    }

    private void resolveMovement(View child) {

        final int id = child.getId();
        final int position = findPositionById(id);
        final int origin_i = position / mColumnCount;
        final int origin_j = position % mColumnCount;
        int final_i = origin_i;
        int final_j = origin_j;

        final int movement = mMoveMap[position];
        switch (movement) {
            case LEFT_MOVE: {
                final_j = origin_j - 1;
                break;
            }
            case RIGHT_MOVE: {
                final_j = origin_j + 1;
                break;
            }
            case UP_MOVE: {
                final_i = origin_i - 1;
                break;
            }
            case DOWN_MOVE: {
                final_i = origin_i + 1;
                break;
            }
            default:
                System.out.println("illegal movement in position [" + origin_i + ", " + origin_j + "]");
        }

        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.i = final_i;
        lp.j = final_j;

        setId(origin_i, origin_j, -1);
        setId(final_i, final_j, id);

        setMovement(final_i - 1, final_j, NO_MOVE);
        setMovement(final_i + 1, final_j, NO_MOVE);
        setMovement(final_i, final_j - 1, NO_MOVE);
        setMovement(final_i, final_j + 1, NO_MOVE);
        setMovement(origin_i, origin_j, NO_MOVE);

        setMovement(origin_i - 1, origin_j, DOWN_MOVE);
        setMovement(origin_i + 1, origin_j, UP_MOVE);
        setMovement(origin_i, origin_j - 1, RIGHT_MOVE);
        setMovement(origin_i, origin_j + 1, LEFT_MOVE);

        if (isSuccess() && mOnSuccessListener != null) {
            mImageViewMap[mImageViewMap.length - 1].setVisibility(VISIBLE);
            requestLayout();
            mOnSuccessListener.onSuccess();
        }
    }

    private int findPositionById(int id) {
        for (int i = 0, max = mRowCount * mColumnCount; i < max; i++) {
            if (mIdMap[i] == id) {
                return i;
            }
        }
        return -1;
    }

    private void printMoveMap() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mColumnCount * mRowCount; i++) {
            sb.append(mMoveMap[i]).append("  ");
            if ((i + 1) % mColumnCount == 3) {
                sb.append("\n");
            }
        }
        System.out.println("\n" + sb);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    public class DragCallback extends ViewDragHelper.Callback {

        private static final float MIN_VELOCITY = 0.f;

        private Rect rect = new Rect();
        private int mOriginLeft = 0;
        private int mOriginTop = 0;

        private ViewDragHelper mDragger;

        public void setDrager(ViewDragHelper drager) {
            mDragger = drager;
        }

        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            final int movement = getMovement(child);
            mOriginLeft = child.getLeft();
            mOriginTop = child.getTop();
            switch (movement) {
                case LEFT_MOVE: {
                    rect.set((int) (child.getLeft() - mPerWidth + 0.5f),
                            child.getTop(),
                            child.getLeft(),
                            child.getTop());
                    return true;
                }
                case RIGHT_MOVE: {
                    rect.set(child.getLeft(),
                            child.getTop(),
                            (int) (child.getLeft() + mPerWidth + 0.5f),
                            child.getTop());
                    return true;
                }
                case UP_MOVE: {
                    rect.set(child.getLeft(),
                            (int) (child.getTop() - mPerHeight + 0.5f),
                            child.getLeft(),
                            child.getTop());
                    return true;
                }
                case DOWN_MOVE: {
                    rect.set(child.getLeft(),
                            child.getTop(),
                            child.getLeft(),
                            (int) (child.getTop() + mPerHeight + 0.5f));
                    return true;
                }
                default: {
                    return false;
                }
            }
        }

        @Override
        public int clampViewPositionHorizontal(@NotNull View child, int left, int dx) {
            return left < rect.left ? rect.left : Math.min(rect.right, left);
        }

        @Override
        public int clampViewPositionVertical(@NotNull View child, int top, int dy) {
            return top < rect.top ? rect.top : Math.min(rect.bottom, top);
        }

        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);

            final int movement = getMovement(releasedChild);
            if (movement == LEFT_MOVE && (releasedChild.getLeft() < ((rect.left + rect.right) >> 1) || xvel < -MIN_VELOCITY) ||
                    (movement == RIGHT_MOVE && (releasedChild.getLeft() > ((rect.left + rect.right) >> 1) || xvel > MIN_VELOCITY)) ||
                    (movement == UP_MOVE && (releasedChild.getTop() < ((rect.top + rect.bottom) >> 1) || yvel < -MIN_VELOCITY)) ||
                    (movement == DOWN_MOVE && (releasedChild.getTop() > ((rect.top + rect.bottom) >> 1) || yvel > MIN_VELOCITY))) {
                resolveMovement(releasedChild);
                LayoutParams lp = (LayoutParams) releasedChild.getLayoutParams();
                mDragger.settleCapturedViewAt((int) (lp.j * mPerWidth + 0.5f), (int) (lp.i * mPerHeight + 0.5f));
            } else {
                mDragger.settleCapturedViewAt(mOriginLeft, mOriginTop);
            }
            invalidate();
        }
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        public static final int NO_POSITION = -1;

        public int i = NO_POSITION;
        public int j = NO_POSITION;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams() {
            super(WRAP_CONTENT, WRAP_CONTENT);
        }

        public LayoutParams(int i, int j) {
            super(WRAP_CONTENT, WRAP_CONTENT);
            this.i = i;
            this.j = j;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    public interface OnSuccessListener {
        void onSuccess();
    }
}
