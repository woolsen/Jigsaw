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

    private static final int NO_MOVE = -1;
    private static final int UP_MOVE = 1;
    private static final int DOWN_MOVE = 2;
    private static final int LEFT_MOVE = 3;
    private static final int RIGHT_MOVE = 4;


    private static final int EMPTY_ID = 0x111;

    private int mRowCount;
    private int mColumnCount;

    private Drawable mDrawable;

    private int[][] mOriginIdMap;
    private int[][] mIdMap;
    private int[][] mMoveMap;
    private View[][] mImageViewMap;

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
        generateImageView();
        initOriginIdMap();
        initIdMapAndMovementMap();
        for (View[] views : mImageViewMap) {
            for (View view : views) {
                addView(view);
            }
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

    /**
     * 通过Drawable生产相对应的ImageView;
     */
    private void generateImageView() {
        //切割图片
        Bitmap[][] bitmaps = new Bitmap[mRowCount][mColumnCount];
        final int w = mDrawable.getIntrinsicWidth();
        final int h = mDrawable.getIntrinsicHeight();
        final float perWidth = 1f * w / mColumnCount;
        final float perHeight = 1f * h / mRowCount;

        Bitmap.Config config = mDrawable.getOpacity() != PixelFormat.OPAQUE ?
                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap originBitmap = Bitmap.createBitmap(w, h, config);
        Canvas canvas = new Canvas(originBitmap);
        mDrawable.setBounds(0, 0, w, h);
        mDrawable.draw(canvas);

        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                Bitmap bitmap = Bitmap.createBitmap(originBitmap,
                        (int) (perWidth * j),
                        (int) (perHeight * i),
                        (int) (perWidth + 0.5f),
                        (int) (perHeight + 0.5f));
                bitmaps[i][j] = bitmap;
            }
        }

        //生成ImageView
        mImageViewMap = new ImageView[mRowCount][mColumnCount];
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                ImageView view = new ImageView(getContext());
                view.setId(generateViewId());
                view.setImageBitmap(bitmaps[i][j]);
                view.setScaleType(ImageView.ScaleType.FIT_XY);

                LayoutParams lp = new LayoutParams(i, j);
                view.setLayoutParams(lp);

                mImageViewMap[i][j] = view;
            }
        }
        mImageViewMap[mRowCount - 1][mColumnCount - 1].setId(EMPTY_ID);
    }

    private void initOriginIdMap() {
        mOriginIdMap = new int[mRowCount][mColumnCount];
        for (View[] views : mImageViewMap) {
            for (View view : views) {
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                mOriginIdMap[lp.i][lp.j] = view.getId();
            }
        }
    }

    private void setMovement(int i, int j, int movement) {
        if (checkPosition(i, j)) {
            mMoveMap[i][j] = movement;
        }
    }

    private void setId(int i, int j, int id) {
        if (checkPosition(i, j)) {
            mIdMap[i][j] = id;
        }
    }

    private int getMovement(View view) {
        final int id = view.getId();
        int position_i = -1;
        int position_j = -1;
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                if (mIdMap[i][j] == id) {
                    position_i = i;
                    position_j = j;
                    break;
                }
            }
            if (position_i != -1) {
                break;
            }
        }
        return mMoveMap[position_i][position_j];
    }

    private boolean checkPosition(int i, int j) {
        return 0 <= i && i < mRowCount && 0 <= j && j < mColumnCount;
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

    private void initIdMapAndMovementMap() {
        mIdMap = new int[mRowCount][mColumnCount];
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                final View view = mImageViewMap[i][j];
                LayoutParams lp = (LayoutParams) view.getLayoutParams();
                mIdMap[lp.i][lp.j] = view.getId();
            }
        }

        int position_i = 0;
        int position_j = 0;
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                if (mIdMap[i][j] == EMPTY_ID) {
                    position_i = i;
                    position_j = j;
                    break;
                }
            }
            if (position_i != 0) {
                break;
            }
        }

        mMoveMap = new int[mRowCount][mColumnCount];
        for (int[] ints : mMoveMap) {
            Arrays.fill(ints, NO_MOVE);
        }
        setMovement(position_i, position_j - 1, RIGHT_MOVE);
        setMovement(position_i, position_j + 1, LEFT_MOVE);
        setMovement(position_i - 1, position_j, DOWN_MOVE);
        setMovement(position_i + 1, position_j, UP_MOVE);
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

    /**
     * 对于m*n的拼图，从拼图板块中任取三块做轮换，通过[(m*n)/3]^2次轮换，即可实现相当“乱”的打乱效果。
     */
    public void start() {
        List<View> views = new ArrayList<>();
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                views.add(mImageViewMap[i][j]);
            }
        }
        views.remove(views.size() - 1);

        Random r = new Random();

        int swapCount = ((mColumnCount * mRowCount) / 3) * ((mColumnCount * mRowCount) / 3);

        while (swapCount-- != 0) {
            final View view1 = views.get(r.nextInt(views.size()));
            views.remove(view1);
            final View view2 = views.get(r.nextInt(views.size()));
            views.remove(view2);
            final View view3 = views.get(r.nextInt(views.size()));
            views.remove(view3);
            swapParamLayout(view1, view2, view3);
            views.add(view1);
            views.add(view2);
            views.add(view3);
        }

        initIdMapAndMovementMap();

        mImageViewMap[mRowCount - 1][mColumnCount - 1].setVisibility(GONE);
        requestLayout();
    }

    public void reset() {
        initJigsaw();
    }

    /**
     * 交换ImageView的LayoutParams
     */
    private void swapParamLayout(View view1, View view2, View view3) {
        ViewGroup.LayoutParams lp1 = view1.getLayoutParams();
        view1.setLayoutParams(view2.getLayoutParams());
        view2.setLayoutParams(view3.getLayoutParams());
        view3.setLayoutParams(lp1);
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
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                if (mOriginIdMap[i][j] != mIdMap[i][j]) {
                    return false;
                }
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
        final IJ position = findPositionById(id);
        final int origin_i = position.i;
        final int origin_j = position.j;
        int final_i = origin_i;
        int final_j = origin_j;

        final int movement = mMoveMap[origin_i][origin_j];
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

        setId(origin_i, origin_j, EMPTY_ID);
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

        System.out.println("Origin: ");
        printMap(mOriginIdMap);
        System.out.println("Current: ");
        printMap(mIdMap);

        if (isSuccess() && mOnSuccessListener != null) {
            mImageViewMap[mRowCount - 1][mColumnCount - 1].setVisibility(VISIBLE);
            requestLayout();
            mOnSuccessListener.onSuccess();
        }
    }

    private IJ findPositionById(int id) {
        for (int i = 0; i < mRowCount; i++) {
            for (int j = 0; j < mColumnCount; j++) {
                if (mIdMap[i][j] == id) {
                    return new IJ(i, j);
                }
            }
        }
        return new IJ(-1, -1);
    }

    private void printMap(int[][] map) {
        StringBuilder sb = new StringBuilder();
        for (int[] ints : map) {
            for (int a : ints) {
                sb.append(a).append("  ");
            }
            sb.append("\n");
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

    private static class IJ {
        int i;
        int j;

        public IJ(int i, int j) {
            this.i = i;
            this.j = j;
        }
    }
}
